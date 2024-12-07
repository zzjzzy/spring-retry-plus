package com.github.gitcat.spring.retryplus.serializer;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ParamSerializerHolder {

    @Autowired
    private List<ParamSerializer> paramSerializers;

    private List<ParamSerializer> sortedParamSerializers;

    public List<ParamSerializer> getParamSerializers() {
        return sortedParamSerializers;
    }

    @PostConstruct
    public void init() {
        sortedParamSerializers = paramSerializers
                .stream()
                .sorted(Comparator.comparingInt(ParamSerializer::getOrder))
                .collect(Collectors.toList());
    }

}
