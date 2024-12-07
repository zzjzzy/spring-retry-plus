package com.github.gitcat.spring.retryplus.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import java.lang.reflect.Method;

public class ProtobufUtil {

    public static Message messageFromJson(String json, Class messageClass) throws Exception {
        Method method = messageClass.getMethod("newBuilder");
        Builder builder = (Message.Builder) method.invoke(null);
        JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
        return builder.build();
    }

    public static String toJson(MessageOrBuilder message) {
        try {
            return JsonFormat.printer().omittingInsignificantWhitespace().print(message);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

}
