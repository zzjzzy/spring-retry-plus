package com.github.gitcat.spring.retryplus.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClassUtil {
    /** 包装类型为Key，原始类型为Value，例如： Integer.class =》 int.class. */
    public static final Map<Class<?>, Class<?>> WRAPPER_PRIMITIVE_MAP = new ConcurrentHashMap<>(8);
    /** 原始类型为Key，包装类型为Value，例如： int.class =》 Integer.class. */
    public static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = new ConcurrentHashMap<>(8);
    /** 原始类型名称为Key，原生类型为Value，例如： int =》 int.class. */
    public static final Map<String, Class<?>> PRIMITIVE_NAME_TYPE_MAP = new ConcurrentHashMap<>(8);

    static {
        WRAPPER_PRIMITIVE_MAP.put(Boolean.class, boolean.class);
        WRAPPER_PRIMITIVE_MAP.put(Byte.class, byte.class);
        WRAPPER_PRIMITIVE_MAP.put(Character.class, char.class);
        WRAPPER_PRIMITIVE_MAP.put(Double.class, double.class);
        WRAPPER_PRIMITIVE_MAP.put(Float.class, float.class);
        WRAPPER_PRIMITIVE_MAP.put(Integer.class, int.class);
        WRAPPER_PRIMITIVE_MAP.put(Long.class, long.class);
        WRAPPER_PRIMITIVE_MAP.put(Short.class, short.class);

        for (Map.Entry<Class<?>, Class<?>> entry : WRAPPER_PRIMITIVE_MAP.entrySet()) {
            PRIMITIVE_WRAPPER_MAP.put(entry.getValue(), entry.getKey());
            PRIMITIVE_NAME_TYPE_MAP.put(entry.getValue().getName(), entry.getValue());
        }
    }

    /**
     * 可以直接转成toString的类型
     */
    public static boolean isToStringType(Class<?> clazz) {
        if (null == clazz) {
            return false;
        }
        return clazz.isPrimitive()
                || WRAPPER_PRIMITIVE_MAP.containsKey(clazz)
                || clazz == String.class;
    }


    public static Class<?> getPrimitiveClassType(String primitiveTypeStr) {
        return PRIMITIVE_NAME_TYPE_MAP.get(primitiveTypeStr);
    }
}
