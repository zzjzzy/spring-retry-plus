package com.github.gitcat.spring.retryplus.test.assist;

import com.github.gitcat.spring.retryplus.annotation.RetryablePlus;
import com.github.gitcat.spring.retryplus.util.ProtobufUtil;
import com.github.gitcat.spring.retryplus.test.stub.Hello.BaseRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

@Service
public class RetryTestBean {

    public static AtomicInteger counter = new AtomicInteger(0);

    public static void clearInfo() {
        counter.set(0);
        BeanRetryInfoTestRepository.clearInfo();
    }

    /**
     * 测试正常流程
     */
    @RetryablePlus
    public void testCall(int a, Integer b, boolean c, Boolean d,
            Map e, List<String> f, List<TestEntity> g,
            TestEntity h, TestGenericEntity<TestEntity> i,
            BaseRequest j, TestEntity nullTest, Object objTest)  {
        if (nullTest != null) {
            throw new RuntimeException("nullTest is not null");
        }
        System.out.println("========== testCall start ==========");
        System.out.println("请求入参：" + a + "," + b + "," + c + "," + d + "," +
                e + "," + f + "," + g + "," + h + "," + i + "," + ProtobufUtil.toJson(j) +
                "," + nullTest + "," + objTest);
        String f0 = f.get(0);
        System.out.println(f0);
        TestEntity g0 = g.get(0);
        System.out.println(g0);
        TestEntity ia = i.getA();
        System.out.println(ia);
        System.out.println(j.getTraceId());
        System.out.println("========== testCall end ==========");
        System.out.println();
        counter.getAndIncrement();
    }

    /**
     * 测试异常流程
     */
    @RetryablePlus
    public void testExpCall(int a, Integer b, boolean c, Boolean d,
            Map e, List<String> f, List<TestEntity> g,
            TestEntity h, TestGenericEntity<TestEntity> i,
            BaseRequest j, TestEntity nullTest, Object objTest)  {
        this.testCall(a, b, c, d, e, f, g, h, i, j, nullTest, objTest);
        if (counter.get() < 5) {
            int div = 1/0;
        }
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestEntity {
        private Integer a;
        private String b;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TestGenericEntity<T> {
        private T a;
        private String b;
    }
}
