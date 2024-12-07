package com.github.gitcat.spring.retryplus.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;

public class JsonUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJsonStr(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsString(obj);
    }

    public static <T> T parseObj(String retryParam, Class<T> valueType) throws JsonProcessingException {
        return objectMapper.readValue(retryParam, valueType);
    }

    public static <T> T parseObj(String retryParam, TypeReference<T> typeReference) throws JsonProcessingException {
        return objectMapper.readValue(retryParam, typeReference);
    }
}
