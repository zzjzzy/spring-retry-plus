package com.github.gitcat.spring.retryplus.listener;

import com.github.gitcat.spring.retryplus.annotation.RetryablePlus;
import com.github.gitcat.spring.retryplus.constant.Constants;
import com.github.gitcat.spring.retryplus.constant.NULL_VALUE;
import com.github.gitcat.spring.retryplus.context.RetryInfoHolder;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfo;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfoRepository;
import com.github.gitcat.spring.retryplus.serializer.ParamSerializer;
import com.github.gitcat.spring.retryplus.serializer.ParamSerializerHolder;
import com.github.gitcat.spring.retryplus.util.JsonUtil;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
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
    @Autowired
    private ParamSerializerHolder paramSerializerHolder;

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
                    beanRetryInfo.setExceptionMsg(throwable.getMessage());
                    long nextRetryInterval = calNextRetryInterval(0, mergedAnnotation);
                    beanRetryInfo.setNextRetryInterval(nextRetryInterval);

                    // 需要保存的序列化后的参数值、参数类型、方法定义的参数类型
                    List<String> serializedParamValues = new ArrayList<>();
                    List<String> realParamStrTypes = new ArrayList<>();
                    List<String> methodDefStrTypes = new ArrayList<>();

                    Object[] paramValues = invocation.getArguments();
                    // 方法定义的参数类型
                    Class<?>[] methodDefClassTypes = method.getParameterTypes();
                    // 方法定义的参数类型，包含泛型信息
                    Type[] methodDefTypes = method.getGenericParameterTypes();
                    for (int i = 0; i < paramValues.length; i++) {
                        Object paramValue = paramValues[i];
                        Class<?> paramClass = paramValue == null ? NULL_VALUE.class : paramValue.getClass();
                        Class<?> methodDefClassType = methodDefClassTypes[i];
                        Type methodDefType = methodDefTypes[i];

                        realParamStrTypes.add(paramClass.getName());
                        String methodDefStrType = methodDefClassType.getName();
                        if (methodDefClassType.isPrimitive()) {
                            // 反序列化的时候需要通过Class.forName获取class对象，然后找到对应的方法
                            // 原始类型通过Class.forName获取class对象会报错
                            // 这里特殊处理一下
                            methodDefStrType = Constants.PRIMITIVE_PREFIX + methodDefStrType;
                        }
                        methodDefStrTypes.add(methodDefStrType);
                        String serializedParamValue = null;
                        for (ParamSerializer serializer : paramSerializerHolder.getParamSerializers()) {
                            if (serializer.supportSerialize(paramValue, methodDefType)) {
                                serializedParamValue = serializer.serialize(paramValue, methodDefType);
                                break;
                            }
                        }
                        if (serializedParamValue == null) {
                            throw new RuntimeException("未找到可用的ParamSerializer");
                        }
                        serializedParamValues.add(serializedParamValue);
                    }

                    beanRetryInfo.setParamValues(JsonUtil.toJsonStr(serializedParamValues));
                    beanRetryInfo.setMethodParamTypes(JsonUtil.toJsonStr(methodDefStrTypes));
                    beanRetryInfo.setRealParamTypes(JsonUtil.toJsonStr(realParamStrTypes));
                    // 保存失败不影响主流程
                    try {
                        beanRetryInfoRepository.saveRetryRecord(beanRetryInfo);
                    } catch (Throwable e) {
                        log.error("saveRetryRecord error", e);
                    }
                }
            }
        } catch (Throwable e) {
            // 有异常直接抛出，这样可以在开发阶段就发现问题
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
