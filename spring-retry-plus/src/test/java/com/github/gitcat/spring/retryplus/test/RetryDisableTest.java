package com.github.gitcat.spring.retryplus.test;

import com.github.gitcat.spring.retryplus.test.assist.BeanRetryInfoTestRepository;
import com.github.gitcat.spring.retryplus.test.assist.RetryTestBean;
import com.github.gitcat.spring.retryplus.test.assist.RetryTestBean.TestEntity;
import com.github.gitcat.spring.retryplus.test.assist.RetryTestBean.TestGenericEntity;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfo;
import com.github.gitcat.spring.retryplus.test.stub.Hello.BaseRequest;
import com.github.gitcat.spring.retryplus.test.stub.Hello.BaseRequest.Builder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.RetryListener;
import org.springframework.util.Assert;

@SpringBootTest(properties = "spring.retryplus.enable=false")
public class RetryDisableTest {

    @Autowired
    private RetryTestBean retryTestBean;
    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 测试spring.retryplus.enable=false的情况
     */
    @Test
    public void testConfigEnable() {
        try {
            RetryListener bean = applicationContext.getBean(RetryListener.class);
            Assert.isNull(bean, "RetryListener生效，期望未生效");
        } catch (Exception e) {
            // spring.retryplus.enable配置为false，预期会抛出NoSuchBeanDefinitionException异常
            Assert.isTrue(e instanceof NoSuchBeanDefinitionException, "期望NoSuchBeanDefinitionException异常");
        }
    }

    /**
     * 测试异常调用后，异常信息不会保存到数据库（因为spring.retryplus.enable=false）
     */
    @Test
    public void testExpCall() {
        RetryTestBean.clearInfo();
        System.out.println("testRetryExp start");

        HashMap map = new HashMap();
        map.put("k1", "v1");
        Builder builder = BaseRequest.newBuilder();
        builder.setTraceId("traceId001");
        builder.setUserId("userId001");
        builder.setOrderId("orderId001");
        try {
            List<String> f = new LinkedList<>();
            f.add("1");
            f.add("2");
            retryTestBean.testExpCall(1, 2, true, false, map,
                    f, Arrays.asList(new TestEntity(11, "str2")),
                    new TestEntity(22, "str2"),
                    new TestGenericEntity<TestEntity>(new TestEntity(33, "str3"), "str4"),
                    builder.build(), null, "objTestVal");
        } catch (Exception ex) {
            System.out.println("异常信息：" + ex.getMessage());
        }
        Assert.isTrue(RetryTestBean.counter.get() == 3, "重试次数不正确");
        BeanRetryInfo savedInfo = BeanRetryInfoTestRepository.savedBeanRetryInfo;
        Assert.isNull(savedInfo, "spring.retryplus.enable=false，预期保存的重试信息为null");

        System.out.println("testRetryExp end");
    }

}
