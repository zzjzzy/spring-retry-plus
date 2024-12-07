package com.github.gitcat.spring.retryplus.serializer.impl;

import com.github.gitcat.spring.retryplus.serializer.ParamSerializer;
import com.github.gitcat.spring.retryplus.util.ProtobufUtil;
import com.google.protobuf.Message;
import java.lang.reflect.Type;
import org.springframework.stereotype.Service;

/**
 * google protobuf协议序列化
 */
@Service
public class ProtobufParamSerializer implements ParamSerializer {

    @Override
    public boolean supportSerialize(Object paramValue, Type methodDefType) {
        return paramValue instanceof Message;
    }

    @Override
    public boolean supportDeserialize(String paramValueStr, Class paramType, Type methodDefType) {
        return Message.class.isAssignableFrom(paramType);
    }

    @Override
    public String serialize(Object paramValue, Type methodDefType) {
        return ProtobufUtil.toJson(((Message) paramValue));
    }

    @Override
    public Object deserialize(String paramValueStr, Class paramType, Type methodDefType) {
        try {
            return ProtobufUtil.messageFromJson(paramValueStr, paramType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getOrder() {
        return ORDER_3000;
    }
}
