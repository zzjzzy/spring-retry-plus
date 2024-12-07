package com.github.gitcat.spring.retryplus.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.gitcat.spring.retryplus.constant.Constants;
import com.github.gitcat.spring.retryplus.listener.RetryPlusListener;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfo;
import com.github.gitcat.spring.retryplus.service.RetryService;
import com.github.gitcat.spring.retryplus.test.assist.BeanRetryInfoTestRepository;
import com.github.gitcat.spring.retryplus.test.assist.RetryTestBean;
import com.github.gitcat.spring.retryplus.test.assist.RetryTestBean.TestEntity;
import com.github.gitcat.spring.retryplus.test.assist.RetryTestBean.TestGenericEntity;
import com.github.gitcat.spring.retryplus.test.stub.Hello.BaseRequest;
import com.github.gitcat.spring.retryplus.util.JsonUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.retry.RetryListener;
import org.springframework.util.Assert;

@SpringBootTest(properties = "spring.retryplus.enable=true")
public class RetryTest {

    @Autowired
    private RetryTestBean retryTestBean;
    @Autowired
    private RetryService retryService;
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void tempTest() throws Exception {
        System.out.println("临时功能测试");
    }

    /**
     * 测试spring.retryplus.enable配置是否生效
     */
    @Test
    public void testConfigEnable() {
        RetryListener bean = applicationContext.getBean(RetryListener.class);
        Assert.notNull(bean, "RetryListener未生效");
        Assert.isTrue(bean instanceof RetryPlusListener, "RetryListener未生效");
    }

    /**
     * 正常调用测试
     */
    @Test
    public void testNoExpCall() {
        RetryTestBean.clearInfo();
        System.out.println("testRetry start");
        retryTestBean.testCall(RetryTestBean.paramA, RetryTestBean.paramB, RetryTestBean.paramC, RetryTestBean.paramD,
                RetryTestBean.paramE, RetryTestBean.paramF, RetryTestBean.paramG, RetryTestBean.paramH,
                RetryTestBean.paramI, RetryTestBean.paramJ, RetryTestBean.paramNullTest, RetryTestBean.paramObjTest);
        Assert.isTrue(RetryTestBean.counter.get() == 1, "重试次数不正确");
        Assert.isNull(BeanRetryInfoTestRepository.savedBeanRetryInfo, "重试记录不正确");
        System.out.println("testRetry end");
    }

    /**
     * 测试异常调用后，异常信息是否正确保存数据库
     */
    @Test
    public void testExpCall() throws JsonProcessingException {
        RetryTestBean.clearInfo();
        System.out.println("testRetryExp start");
        try {
            retryTestBean.testExpCall(RetryTestBean.paramA, RetryTestBean.paramB, RetryTestBean.paramC, RetryTestBean.paramD,
                    RetryTestBean.paramE, RetryTestBean.paramF, RetryTestBean.paramG, RetryTestBean.paramH,
                    RetryTestBean.paramI, RetryTestBean.paramJ, RetryTestBean.paramNullTest, RetryTestBean.paramObjTest);
        } catch (Exception ex) {
            System.out.println("异常信息：" + ex.getMessage());
        }
        Assert.isTrue(RetryTestBean.counter.get() == 3, "重试次数不正确");
        BeanRetryInfo savedInfo = BeanRetryInfoTestRepository.savedBeanRetryInfo;
        Assert.notNull(savedInfo, "重试记录未保存");
        Assert.isTrue(savedInfo.getBeanClass().equals(RetryTestBean.class.getName()), "保存的bean类名不正确");
        Assert.isTrue(savedInfo.getBeanMethod().equals("testExpCall"), "保存的方法名不正确");
        Assert.isTrue(savedInfo.getExceptionMsg().equals("/ by zero"), "保存的异常信息不正确");

        List<String> methodParamTypes = Arrays.asList(
                Constants.PRIMITIVE_PREFIX + "int", Integer.class.getName(),
                Constants.PRIMITIVE_PREFIX + "boolean", Boolean.class.getName(), Map.class.getName(),
                List.class.getName(), List.class.getName(), TestEntity.class.getName(),
                TestGenericEntity.class.getName(),
                BaseRequest.class.getName()
        );
        List<String> realParamTypes = Arrays.asList(
                Integer.class.getName(), Integer.class.getName(),
                Boolean.class.getName(), Boolean.class.getName(), HashMap.class.getName(),
                LinkedList.class.getName(), ArrayList.class.getName(), TestEntity.class.getName(),
                TestGenericEntity.class.getName(),
                BaseRequest.class.getName()
        );
        List<String> savedMethodParamTypes = JsonUtil.parseObj(savedInfo.getMethodParamTypes(),
                new TypeReference<List<String>>() {});
        List<String> savedRealParamTypes = JsonUtil.parseObj(savedInfo.getRealParamTypes(),
                new TypeReference<List<String>>() {});
        for (int i = 0; i < methodParamTypes.size(); i++) {
            Assert.isTrue(methodParamTypes.get(i).equals(savedMethodParamTypes.get(i)), "保存的参数类型不正确"
                    + "方法参数类型：" + methodParamTypes.get(i) + "，保存的参数类型：" + savedMethodParamTypes.get(i));
            Assert.isTrue(realParamTypes.get(i).equals(savedRealParamTypes.get(i)), "保存的真实参数类型不正确, "
                    + "真实参数类型：" + realParamTypes.get(i) + "，保存的参数类型：" + savedRealParamTypes.get(i));
        }
        System.out.println("testRetryExp end");
    }

    /**
     * 测试异常调用后保存到数据库，然后从数据库读取重试记录进行重试
     */
    @Test
    public void testDbRetry() {
        RetryTestBean.clearInfo();
        System.out.println("testRetryCall start");

        // 模拟重试记录保存数据库
        try {
            retryTestBean.testExpCall(RetryTestBean.paramA, RetryTestBean.paramB, RetryTestBean.paramC, RetryTestBean.paramD,
                    RetryTestBean.paramE, RetryTestBean.paramF, RetryTestBean.paramG, RetryTestBean.paramH,
                    RetryTestBean.paramI, RetryTestBean.paramJ, RetryTestBean.paramNullTest, RetryTestBean.paramObjTest);
        } catch (Exception ex) {
            System.out.println("异常信息：" + ex.getMessage());
        }
        // 模拟从数据库读取重试记录进行重试
        // 这里retryAll因为会重试失败，会打印错误日志，忽略
        retryService.retryAll(1, 100);
        Assert.isTrue(RetryTestBean.counter.get() == 4, "重试次数不正确");
        Assert.isTrue(BeanRetryInfoTestRepository.updatedBeanRetryInfo != null
                            && BeanRetryInfoTestRepository.updatedBeanRetryInfo.getRetryTimes() == 1,
                "重试记录更新错误");

        System.out.println("=======================");

        // 再次重试，模拟重试成功
        // com.github.gitcat.spring.retryplus.assist.retryplus.RetryTestBean.testExpCall发现重试次数>=5会模拟成功，不再抛异常
        retryService.retryAll(1, 100);
        Assert.isTrue(RetryTestBean.counter.get() == 5, "重试次数不正确");
        Assert.isTrue(BeanRetryInfoTestRepository.updatedBeanRetryInfo != null
                        && BeanRetryInfoTestRepository.updatedBeanRetryInfo.getRetryTimes() == 2,
                "重试记录跟新错误");
        Assert.isTrue(BeanRetryInfoTestRepository.successRetryId != null, "重试成功记录未更新");

        System.out.println("testRetryCall end");
    }

    /**
     * 测试非重试异常不进行重试，且不保存到数据库
     */
    @Test
    public void testNotRetryExp() {
        RetryTestBean.clearInfo();
        System.out.println("testNotRetryExp start");
        try {
            retryTestBean.testNoRetryCall(1, "2");
        } catch (Exception ex) {
            System.out.println("异常信息：" + ex.getMessage());
        }
        Assert.isTrue(RetryTestBean.counter.get() == 1, "调用次数预期为1");
        Assert.isNull(BeanRetryInfoTestRepository.savedBeanRetryInfo, "重试记录预期不保存");
    }
}
