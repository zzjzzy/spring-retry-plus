package com.github.gitcat.spring.retryplus.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.gitcat.spring.retryplus.constant.Constants;
import com.github.gitcat.spring.retryplus.constant.NULL_VALUE;
import com.google.protobuf.Message;
import com.github.gitcat.spring.retryplus.context.RetryFrom;
import com.github.gitcat.spring.retryplus.context.RetryInfoHolder;
import com.github.gitcat.spring.retryplus.context.RetryInfoHolder.RetryInfo;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfo;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfoRepository;
import com.github.gitcat.spring.retryplus.util.ClassUtil;
import com.github.gitcat.spring.retryplus.util.JsonUtil;
import com.github.gitcat.spring.retryplus.util.ProtobufUtil;
import com.github.gitcat.spring.retryplus.util.RetryPlusSpringUtil;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
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

        Type[] methodGenericParamTypes = method.getGenericParameterTypes();
        List realParamTypes = JsonUtil.parseObj(retryInfo.getRealParamTypes(), List.class);
        List paramValues = JsonUtil.parseObj(retryInfo.getParamValues(), List.class);
        Object[] args = new Object[paramValues.size()];
        for (int i = 0; i < paramValues.size(); i++) {
            String paramValueStr = paramValues.get(i).toString();
            if (realParamTypes.get(i).toString().equals(NULL_VALUE.class.getName())) {
                // null值
                args[i] = null;
                continue;
            }
            Type genericParamType = methodGenericParamTypes[i];
            // 由于实际参数类型无法保存泛型信息或基本数据类型信息
            // 所以，如果方法的参数类型是泛型或者基本类型，则以方法的参数类型进行参数值转换
            // 否则以实际参数类型进行转换
            if (genericParamType instanceof ParameterizedType) {
                args[i] = JsonUtil.parseObj(paramValueStr, new TypeReference<Object>() {
                    @Override
                    public Type getType() {
                        return genericParamType;
                    }
                });
            } else if (genericParamType instanceof Class) {
                Class<?> paramType = (Class<?>) genericParamType;
                if (!paramType.isPrimitive()) {
                    paramType = Class.forName(realParamTypes.get(i).toString());
                }
                if (ClassUtil.isToStringType(paramType)) {
                    args[i] = convertBasicType(paramValueStr, paramType);
                } else if (Message.class.isAssignableFrom(paramType)) {
                    args[i] = ProtobufUtil.messageFromJson(paramValueStr, paramType);
                } else {
                    args[i] = JsonUtil.parseObj(paramValueStr, paramType);
                }
            } else {
                log.error("不支持的方法参数类型:{}", genericParamType);
                throw new RuntimeException("不支持的方法参数类型:" + genericParamType);
            }
        }
        method.invoke(bean, args);
    }

    private Object convertBasicType(String valStr, Class paramType) {
        if (paramType == String.class) {
            return valStr;
        } else if (paramType == Integer.class || paramType == int.class) {
            return Integer.valueOf(valStr);
        } else if (paramType == Long.class || paramType == long.class) {
            return Long.valueOf(valStr);
        } else if (paramType == Boolean.class || paramType == boolean.class) {
            return Boolean.valueOf(valStr);
        } else if (paramType == Double.class || paramType == double.class) {
            return Double.valueOf(valStr);
        } else if (paramType == Float.class || paramType == float.class) {
            return Float.valueOf(valStr);
        } else if (paramType == Short.class || paramType == short.class) {
            return Short.valueOf(valStr);
        } else if (paramType == Byte.class || paramType == byte.class) {
            return Byte.valueOf(valStr);
        } else if (paramType == Character.class || paramType == char.class) {
            return valStr.charAt(0);
        } else {
            throw new IllegalArgumentException("unsupported type:" + paramType);
        }
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
}
