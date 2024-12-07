package com.github.gitcat.spring.retryplus.listener;

import com.github.gitcat.spring.retryplus.annotation.RetryablePlus;
import com.github.gitcat.spring.retryplus.constant.Constants;
import com.github.gitcat.spring.retryplus.constant.NULL_VALUE;
import com.google.protobuf.Message;
import com.github.gitcat.spring.retryplus.context.RetryInfoHolder;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfo;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfoRepository;
import com.github.gitcat.spring.retryplus.util.ClassUtil;
import com.github.gitcat.spring.retryplus.util.JsonUtil;
import com.github.gitcat.spring.retryplus.util.ProtobufUtil;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.interceptor.MethodInvocationRetryCallback;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "spring.retryplus.enable", havingValue = "true")
@Slf4j
public class RetryPlusListener implements RetryListener {

    @Autowired
    private BeanRetryInfoRepository beanRetryInfoRepository;

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        return true;
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
            Throwable throwable) {
        log.debug("RetryPlusListener close");
        try {
            MethodInvocationRetryCallback cb = (MethodInvocationRetryCallback) callback;
            MethodInvocation invocation = cb.getInvocation();
            Method method = invocation.getMethod();
            RetryablePlus mergedAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, RetryablePlus.class);
            if (mergedAnnotation == null) {
                // 只有DurableRetryable才需要持久化处理
                return;
            }
            Async asyncAnno = AnnotatedElementUtils.findMergedAnnotation(method, Async.class);
            if (asyncAnno != null) {
                // 异步方法不支持数据库重试，因为需要通过ThreadLocal获取重试调用来源
                return;
            }
            boolean isRetryExcepton = false;
            if (throwable != null) {
                for (Class<? extends Throwable> retryException : mergedAnnotation.value()) {
                    if (retryException.isAssignableFrom(throwable.getClass())) {
                        isRetryExcepton = true;
                        break;
                    }
                }
            }
            // 如果是来自定时任务扫描db的重试，会设置RetryInfoHolder值
            if (RetryInfoHolder.isFromDb()) {
                if (!isRetryExcepton) {
                    // 不是需要重试的异常，则认为重试成功
                    beanRetryInfoRepository.updateRetryRecordSuccess(RetryInfoHolder.getRetryId());
                } else {
                    // 是重试的异常，更新重试次数和重试结果
                    long nextRetryInterval = calNextRetryInterval(RetryInfoHolder.getRetryTimes()+1, mergedAnnotation);
                    beanRetryInfoRepository.updateRetryFail(RetryInfoHolder.getRetryId(),
                            throwable.getMessage(), nextRetryInterval);
                }
            } else {
                if (!isRetryExcepton) {
                    // 不是需要重试的异常，不需要保存
                    return;
                } else {
                    // 是需要重试的异常，保存到mysql
                    BeanRetryInfo beanRetryInfo = new BeanRetryInfo();
                    beanRetryInfo.setRetryTimes(0);
                    beanRetryInfo.setMaxRetryTimes(mergedAnnotation.dbRetryTimes());
                    beanRetryInfo.setBeanClass(invocation.getThis().getClass().getName());
                    beanRetryInfo.setBeanMethod(method.getName());
                    List<String> realParamTypes = new ArrayList<>();
                    List<String> paramValues = new ArrayList<>();
                    for (Object param : invocation.getArguments()) {
                        if (param == null) {
                            paramValues.add(NULL_VALUE.class.getName());
                            realParamTypes.add(NULL_VALUE.class.getName());
                            continue;
                        }
                        Class<?> paramType = param.getClass();
                        realParamTypes.add(paramType.getName());
                        if (param instanceof Message) {
                            paramValues.add(ProtobufUtil.toJson((Message) param));
                        } else if (ClassUtil.isToStringType(paramType)) {
                            paramValues.add(String.valueOf(param));
                        } else {
                            paramValues.add(JsonUtil.toJsonStr(param));
                        }
                    }
                    Class<?>[] methodParamClasses = method.getParameterTypes();
                    List<String> methodParamTypes = new ArrayList<>();
                    for (int i = 0; i < methodParamClasses.length; i++) {
                        Class<?> methodParamClass = methodParamClasses[i];
                        if (methodParamClass.isPrimitive()) {
                            methodParamTypes.add(Constants.PRIMITIVE_PREFIX + methodParamClass.getName());
                        } else {
                            methodParamTypes.add(methodParamClass.getName());
                        }
                    }
                    beanRetryInfo.setParamValues(JsonUtil.toJsonStr(paramValues));
                    beanRetryInfo.setMethodParamTypes(JsonUtil.toJsonStr(methodParamTypes));
                    beanRetryInfo.setRealParamTypes(JsonUtil.toJsonStr(realParamTypes));
                    beanRetryInfo.setExceptionMsg(throwable.getMessage());
                    long nextRetryInterval = calNextRetryInterval(0, mergedAnnotation);
                    beanRetryInfo.setNextRetryInterval(nextRetryInterval);
                    // 保存失败不影响主流程
                    try {
                        beanRetryInfoRepository.saveRetryRecord(beanRetryInfo);
                    } catch (Throwable e) {
                        log.error("saveRetryRecord error", e);
                    }
                }
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
            Throwable throwable) {
        if (RetryInfoHolder.isFromDb()) {
            // setExhaustedOnly后，有异常也不会再重试
            // 来自db的重试，只需要调用一次即可
            context.setExhaustedOnly();
        }
    }

    // 根据已重试次数计算下次重试时间间隔
    private long calNextRetryInterval(int retryTimes, RetryablePlus durableRetryable) {
        Backoff backoff = durableRetryable.dbRetryInterval();
        long delay = backoff.delay();
        double multiplier = backoff.multiplier();
        return (long) (delay * Math.pow(multiplier, retryTimes));
    }
}
