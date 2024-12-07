package com.github.gitcat.spring.retryplus.serializer;

import java.lang.reflect.Type;
import org.springframework.core.Ordered;

/**
 * 参数序列化反序列化接口
 */
public interface ParamSerializer extends Ordered {

    int ORDER_1000 = 1000;
    int ORDER_2000 = 2000;
    int ORDER_3000 = 3000;
    int ORDER_LARGE_1 = 1000000;

    /**
     * 是否支持序列化
     * @param paramValue 参数值
     * @param methodDefType 方法定义的参数类型
     */
    boolean supportSerialize(Object paramValue, Type methodDefType);

    /**
     * 是否支持反序列化
     * @param paramValueStr 参数序列化后的值
     * @param paramType 实际传给方法的参数类型，通过param.getClass()获取，无法保留泛型信息
     * @param methodDefType 方法定义的参数类型，通过method.getGenericParameterTypes()获取，可以保留泛型信息
     */
    boolean supportDeserialize(String paramValueStr, Class paramType, Type methodDefType);

    /**
     * 序列化
     * @param paramValue 参数值
     * @param methodDefType 方法定义的参数类型
     */
    String serialize(Object paramValue, Type methodDefType);

    /**
     * 反序列化
     * @param paramValueStr 参数序列化后的值
     * @param paramType 实际传给方法的参数类型，通过param.getClass()获取，无法保留泛型信息
     * @param methodDefType 方法定义的参数类型，通过method.getGenericParameterTypes()获取，可以保留泛型信息
     */
    Object deserialize(String paramValueStr, Class paramType, Type methodDefType);
}
