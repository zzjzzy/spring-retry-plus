package com.github.gitcat.spring.retryplus.serializer.impl;

import com.github.gitcat.spring.retryplus.constant.NULL_VALUE;
import com.github.gitcat.spring.retryplus.serializer.ParamSerializer;
import java.lang.reflect.Type;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * null值序列化
 */
@Service
public class NullParamSerializer implements ParamSerializer {

    @Override
    public boolean supportSerialize(Object paramValue, Type methodDefType) {
        return paramValue == null;
    }

    @Override
    public boolean supportDeserialize(String paramValueStr, Class paramType, Type methodDefType) {
        return Objects.equals(paramType.getName(), NULL_VALUE.class.getName());
    }

    @Override
    public String serialize(Object paramValue, Type methodDefType) {
        return NULL_VALUE.class.getName();
    }

    @Override
    public Object deserialize(String paramValueStr, Class paramType, Type methodDefType) {
        return null;
    }

    @Override
    public int getOrder() {
        return ORDER_1000;
    }
}
