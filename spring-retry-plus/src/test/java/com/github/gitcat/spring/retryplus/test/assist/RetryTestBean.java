package com.github.gitcat.spring.retryplus.test.assist;

import com.github.gitcat.spring.retryplus.annotation.RetryablePlus;
import com.github.gitcat.spring.retryplus.test.stub.Hello.BaseRequest;
import com.github.gitcat.spring.retryplus.util.ProtobufUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class RetryTestBean {

    public static int paramA = 1;
    public static Integer paramB = 2;
    public static boolean paramC = true;
    public static Boolean paramD = false;
    public static Map paramE = null;
    public static List<String> paramF = null;
    public static List<TestEntity> paramG = null;
    public static TestEntity paramH = null;
    public static TestGenericEntity<TestEntity> paramI = null;
    public static BaseRequest paramJ = null;
    public static TestEntity paramNullTest = null;
    public static Object paramObjTest = null;
    static {
        paramE = new HashMap();
        paramE.put("mapKey", "mapVal");
        paramF = new LinkedList<>();
        paramF.add("list01");
        paramF.add("list02");
        paramG = new ArrayList<>();
        paramG.add(new TestEntity(1, "a"));
        paramG.add(new TestEntity(2, "b"));
        paramH = new TestEntity(3, "c");
        paramI = new TestGenericEntity<>(new TestEntity(4, "d"), "e");
        paramJ = BaseRequest.newBuilder()
                .setTraceId("traceId001")
                .setUserId("userId001")
                .setOrderId("orderId001")
                .build();
        paramNullTest = null;
        paramObjTest = "objTest";
    }

    public static AtomicInteger counter = new AtomicInteger(0);

    public static void clearInfo() {
        counter.set(0);
        BeanRetryInfoTestRepository.clearInfo();
    }

    /**
     * 测试正常流程
     */
    @RetryablePlus(idempotent = true)
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
        Assert.isTrue(Objects.equals(a, paramA), "a is not equal");
        Assert.isTrue(Objects.equals(b, paramB), "b is not equal");
        Assert.isTrue(Objects.equals(c, paramC), "c is not equal");
        Assert.isTrue(Objects.equals(d, paramD), "d is not equal");
        Assert.isTrue(Objects.equals(e, paramE), "e is not equal");
        Assert.isTrue(Objects.equals(f, paramF), "f is not equal");
        Assert.isTrue(Objects.equals(g, paramG), "g is not equal");
        Assert.isTrue(Objects.equals(h, paramH), "h is not equal");
        Assert.isTrue(Objects.equals(i, paramI), "i is not equal");
        Assert.isTrue(Objects.equals(j, paramJ), "j is not equal");
        Assert.isTrue(Objects.equals(nullTest, paramNullTest), "nullTest is not equal");
        Assert.isTrue(Objects.equals(objTest, paramObjTest), "objTest is not equal");
        System.out.println("========== testCall end ==========");
        System.out.println();
        counter.getAndIncrement();
    }

    /**
     * 测试异常流程
     */
    @RetryablePlus(idempotent = true)
    public void testExpCall(int a, Integer b, boolean c, Boolean d,
            Map e, List<String> f, List<TestEntity> g,
            TestEntity h, TestGenericEntity<TestEntity> i,
            BaseRequest j, TestEntity nullTest, Object objTest)  {
        this.testCall(a, b, c, d, e, f, g, h, i, j, nullTest, objTest);
        if (counter.get() < 5) {
            int div = 1/0;
        }
    }

    @RetryablePlus(value = NullPointerException.class, idempotent = true)
    public void testNoRetryCall(int a, String b)  {
        System.out.println("请求入参：" + a + "," + b);
        counter.getAndIncrement();
        throw new RuntimeException("testNoRetryCall");
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
