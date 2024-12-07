package com.github.gitcat.spring.retryplus.serializer.impl;

import com.github.gitcat.spring.retryplus.serializer.ParamSerializer;
import com.github.gitcat.spring.retryplus.util.ClassUtil;
import java.lang.reflect.Type;
import org.springframework.stereotype.Service;

/**
 * 可以直接把参数值通过toString转换的值
 */
@Service
public class StringableParamSerializer implements ParamSerializer {

    @Override
    public boolean supportSerialize(Object paramValue, Type methodDefType) {
        return ClassUtil.isToStringType(paramValue.getClass());
    }

    @Override
    public boolean supportDeserialize(String paramValueStr, Class paramType, Type methodDefType) {
        return ClassUtil.isToStringType(paramType);
    }

    @Override
    public String serialize(Object paramValue, Type methodDefType) {
        return String.valueOf(paramValue);
    }

    @Override
    public Object deserialize(String paramValueStr, Class paramType, Type methodDefType) {
        return convertBasicType(paramValueStr, paramType);
    }

    @Override
    public int getOrder() {
        return ORDER_2000;
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
}
