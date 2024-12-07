package com.github.gitcat.spring.retryplus.serializer.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.gitcat.spring.retryplus.serializer.ParamSerializer;
import com.github.gitcat.spring.retryplus.util.JsonUtil;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.springframework.stereotype.Service;

/**
 * 普通java bean json序列化
 * 最为兜底序列化方案，如果其他序列化方案都失败，则使用此方案
 */
@Service
public class JsonParamSerializer implements ParamSerializer {


    @Override
    public boolean supportSerialize(Object paramValue, Type methodDefType) {
        return true;
    }

    @Override
    public boolean supportDeserialize(String paramValueStr, Class paramType, Type methodDefType) {
        return true;
    }

    @Override
    public String serialize(Object paramValue, Type methodDefType) {
        try {
            return JsonUtil.toJsonStr(paramValue);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object deserialize(String paramValueStr, Class paramType, Type methodDefType) {
        try {
            // 如果方法定义的是泛型类型，则使用方法的类型进行反序列化，否则使用paramType进行反序列化
            // 因为方法定义的类型可能是比较宽泛的父类型，paramType的类型更精确
            Object value = null;
            if (methodDefType instanceof ParameterizedType) {
                value = JsonUtil.parseObj(paramValueStr, new TypeReference<Object>() {
                    @Override
                    public Type getType() {
                        return methodDefType;
                    }
                });
            } else {
                value = JsonUtil.parseObj(paramValueStr, paramType);
            }
            return value;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getOrder() {
        return ORDER_LARGE_1;
    }
}
