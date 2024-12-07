package com.github.gitcat.demo.retryplus.service;

import com.github.gitcat.spring.retryplus.annotation.RetryablePlus;
import org.springframework.stereotype.Service;

@Service
public class DemoService {

    @RetryablePlus
    public void testNoExpCall(String param) {
        System.out.println("param = " + param);
    }

    @RetryablePlus
    public void testExpCall(String param) {
        System.out.println("param = " + param);
        throw new RuntimeException("测试异常");
    }
}
