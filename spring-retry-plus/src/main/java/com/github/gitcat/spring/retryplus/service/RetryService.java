package com.github.gitcat.spring.retryplus.service;

import com.github.gitcat.spring.retryplus.constant.Constants;
import com.github.gitcat.spring.retryplus.context.RetryFrom;
import com.github.gitcat.spring.retryplus.context.RetryInfoHolder;
import com.github.gitcat.spring.retryplus.context.RetryInfoHolder.RetryInfo;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfo;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfoRepository;
import com.github.gitcat.spring.retryplus.serializer.ParamSerializer;
import com.github.gitcat.spring.retryplus.serializer.ParamSerializerHolder;
import com.github.gitcat.spring.retryplus.util.ClassUtil;
import com.github.gitcat.spring.retryplus.util.JsonUtil;
import com.github.gitcat.spring.retryplus.util.RetryPlusSpringUtil;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RetryService {

    @Autowired
    private BeanRetryInfoRepository beanRetryInfoRepository;
    @Autowired
    private ParamSerializerHolder paramSerializerHolder;

    public void retryAll(int pageSize, int maxLoopCount) {
        try {
            long minId = 0L;  // 通过minId来控制查询数据范围，避免深度分页查询
            for (int i = 1; i <= maxLoopCount; i++) {
                List<BeanRetryInfo> retryList = beanRetryInfoRepository.queryRetrablePageList(minId, pageSize);
                if (retryList == null || retryList.isEmpty()) {
                    break;
                }
                long recordsMaxId = 0L;
                for (BeanRetryInfo record : retryList) {
                    log.debug("retry record:{}", record);
                    try {
                        RetryInfoHolder.set(new RetryInfo(RetryFrom.DB.getFrom(),
                                record.getId(), record.getRetryTimes()));
                        beforeDoRetry(record);
                        doRetry(record);
                        afterDoRetry(record);
                    } catch (Throwable ex) {
                        log.error("retry exception:{}", record, ex);
                        expDoRetry(record, ex);
                    } finally {
                        RetryInfoHolder.remove();
                        finallyDoRetry(record);
                    }
                    recordsMaxId = Math.max(recordsMaxId, record.getId());
                }
                minId = recordsMaxId;

            }
        } finally {
            RetryInfoHolder.remove();
        }
    }


    private void doRetry(BeanRetryInfo retryInfo) throws Exception {
        Class<?> beanClass = Class.forName(retryInfo.getBeanClass());
        Object bean = RetryPlusSpringUtil.getBean(beanClass);
        List methodParamTypes = JsonUtil.parseObj(retryInfo.getMethodParamTypes(), List.class);
        Class[] methodParamClasses = new Class[methodParamTypes.size()];
        for (int i = 0; i < methodParamTypes.size(); i++) {
            String paramTypeStr = methodParamTypes.get(i).toString();
            if (paramTypeStr.startsWith(Constants.PRIMITIVE_PREFIX)) {
                methodParamClasses[i] = ClassUtil.getPrimitiveClassType(paramTypeStr.replace(Constants.PRIMITIVE_PREFIX, ""));
            } else {
                methodParamClasses[i] = Class.forName(paramTypeStr);
            }
        }
        String beanMethod = retryInfo.getBeanMethod();
        Method method = beanClass.getMethod(beanMethod, methodParamClasses);

        Type[] methodDefTypes = method.getGenericParameterTypes();
        List realParamTypes = JsonUtil.parseObj(retryInfo.getRealParamTypes(), List.class);
        List paramValues = JsonUtil.parseObj(retryInfo.getParamValues(), List.class);
        Object[] args = new Object[paramValues.size()];
        for (int i = 0; i < paramValues.size(); i++) {
            String paramValueStr = paramValues.get(i).toString();
            Class realParamType = Class.forName(realParamTypes.get(i).toString());
            Type methodDefType = methodDefTypes[i];
            Object paramValue = null;
            for (ParamSerializer serializer : paramSerializerHolder.getParamSerializers()) {
                if (serializer.supportDeserialize(paramValueStr, realParamType, methodDefType)) {
                    paramValue = serializer.deserialize(paramValueStr, realParamType, methodDefType);
                    break;
                }
            }
            // 对参数做后处理
            paramValue = postProcessParam(paramValueStr, realParamType, methodDefType, i, paramValue);
            args[i] = paramValue;
        }
        method.invoke(bean, args);
    }


    /**
     * 扩展接口，业务可以覆写此方法，在doRetry执行前后做些处理
     */
    public void beforeDoRetry(BeanRetryInfo record) {

    }

    /**
     * 扩展接口，业务可以覆写此方法，在doRetry执行前后做些处理
     */
    public void afterDoRetry(BeanRetryInfo record) {

    }

    /**
     * 扩展接口，业务可以覆写此方法，在doRetry执行前后做些处理
     */
    public void expDoRetry(BeanRetryInfo record, Throwable ex) {

    }

    /**
     * 扩展接口，业务可以覆写此方法，在doRetry执行前后做些处理
     */
    public void finallyDoRetry(BeanRetryInfo record) {

    }

    /**
     * 扩展接口，业务可以覆写此方法，对参数做后处理
     * @param paramValueStr 参数值序列化后的字符串值
     * @param realParamType 参数的真实类型
     * @param methodDefType 方法定义的参数类型
     * @param paramIdx 参数在方法定义中的位置
     * @param paramValue 参数值
     */
    public Object postProcessParam(String paramValueStr, Class realParamType, Type methodDefType,
            int paramIdx, Object paramValue) {
        return paramValue;
    }
}
