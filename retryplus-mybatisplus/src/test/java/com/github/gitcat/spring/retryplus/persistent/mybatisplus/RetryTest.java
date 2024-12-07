package com.github.gitcat.spring.retryplus.persistent.mybatisplus;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.gitcat.spring.retryplus.persistent.mybatisplus.assist.RetryTestBean;
import com.github.gitcat.spring.retryplus.persistent.mybatisplus.entity.BeanRetryRecord;
import com.github.gitcat.spring.retryplus.persistent.mybatisplus.mapper.BeanRetryRecordMapper;
import com.github.gitcat.spring.retryplus.persistent.mybatisplus.repository.BeanRetryRecordRepository;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfo;
import com.github.gitcat.spring.retryplus.service.RetryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

@SpringBootTest(properties = "spring.retryplus.enable=true")
public class RetryTest {

    @Autowired
    RetryTestBean retryTestBean;
    @Autowired
    private BeanRetryRecordRepository beanRetryRecordRepository;
    @Autowired
    private RetryService retryService;
    @Autowired
    private BeanRetryRecordMapper beanRetryRecordMapper;

    /**
     * 测试BeanRetryRecordRepository
     */
    @Test
    public void testDb() throws Exception {
        retryTestBean.clearInfo();
        BeanRetryInfo beanRetryInfo = new BeanRetryInfo();
        beanRetryInfo.setMaxRetryTimes(3);
        beanRetryInfo.setBeanClass("com.example.Test");
        beanRetryInfo.setBeanMethod("testMethod");
        beanRetryInfo.setParamValues("[\"paramDemo\"]");
        beanRetryInfo.setMethodParamTypes("[\"java.lang.String\"]");
        beanRetryInfo.setExceptionMsg("java.lang.Exception: test");
        beanRetryInfo.setNextRetryInterval(5000L);
        beanRetryRecordRepository.saveRetryRecord(beanRetryInfo);

        // 因为NextRetryInterval设置的是5000L，所以queryRetrablePageList方法5秒后才能查询到数据
        List<BeanRetryInfo> recordList = beanRetryRecordRepository.queryRetrablePageList(0L, 10);
        Assert.isTrue(recordList.isEmpty(), "当前应该查询不到数据");

        Thread.sleep(6000);

        recordList = beanRetryRecordRepository.queryRetrablePageList(0L, 10);
        Assert.isTrue(recordList.size() == 1, "当前应该查询到一条数据");
        BeanRetryInfo record = recordList.get(0);
        Assert.isTrue(record.getBeanClass().equals(beanRetryInfo.getBeanClass()), "当前beanClass应该等于beanRetryInfo.getBeanClass()");
    }

    /**
     * 测试正常调用
     */
    @Test
    public void testNormalCall() throws Exception {
        retryTestBean.clearInfo();
        retryTestBean.testCall("testParam");
        Assert.isTrue(RetryTestBean.counter.get() == 1, "当前应该调用一次testCall方法");

        LambdaQueryWrapper<BeanRetryRecord> queryWrapper = new LambdaQueryWrapper<>();
        List<BeanRetryRecord> recordList = beanRetryRecordMapper.selectList(queryWrapper);
        Assert.isTrue(recordList.isEmpty(), "当前应该查询不到数据");
    }

    /**
     * 测试异常调用
     */
    @Test
    public void testExpCall() throws Exception {
        retryTestBean.clearInfo();
        try {
            retryTestBean.testExpCall("testParam");
        } catch (Exception ex) {
            System.out.println("异常信息：" + ex.getMessage());
            Assert.isTrue(ex instanceof ArithmeticException, "当前应该抛出ArithmeticException异常");
        }
        Assert.isTrue(RetryTestBean.counter.get() == 3, "当前应该调用三次testExpCall方法");

        LambdaQueryWrapper<BeanRetryRecord> queryWrapper = new LambdaQueryWrapper<>();
        List<BeanRetryRecord> recordList = beanRetryRecordMapper.selectList(queryWrapper);
        Assert.isTrue(recordList.size() == 1, "异常数据未正确保存");

        // testExpCall配置的是在3、6、12秒后重试，所以马上查询数据应该是查询不到，因为只会查询到了重试时间的数据
        List<BeanRetryInfo> retryInfoList = beanRetryRecordRepository.queryRetrablePageList(0L, 10);
        Assert.isTrue(retryInfoList.isEmpty(), "当前应该查询到0条数据");

        Thread.sleep(3010);

        // testExpCall配置的是在3、6、12秒后重试，所以马上查询数据应该是查询不到，因为只会查询到了重试时间的数据
        retryInfoList = beanRetryRecordRepository.queryRetrablePageList(0L, 10);
        Assert.isTrue(retryInfoList.size() == 1, "当前应该查询到1条数据");
        BeanRetryInfo beanRetryInfo = retryInfoList.get(0);
        Assert.isTrue(beanRetryInfo.getBeanClass().equals(RetryTestBean.class.getName()), "当前beanClass应该等于RetryTestBean.class.getName()");
        Assert.isTrue(beanRetryInfo.getBeanMethod().equals("testExpCall"), "当前beanMethod应该等于testExpCall");
        Assert.isTrue(beanRetryInfo.getRetryTimes() == 0, "当前retryTimes应该等于0");
        Assert.isTrue(beanRetryInfo.getMaxRetryTimes() == 4, "当前maxRetryTimes应该等于4");
    }

    /**
     * 测试异常调用后，数据库继续重试
     */
    @Test
    public void testExpCallAndDbRetry() throws Exception {
        retryTestBean.clearInfo();
        try {
            retryTestBean.testExpCall("testParam");
        } catch (Exception ex) {
            System.out.println("异常信息：" + ex.getMessage());
            Assert.isTrue(ex instanceof ArithmeticException, "当前应该抛出ArithmeticException异常");
        }
        Assert.isTrue(RetryTestBean.counter.get() == 3, "当前应该调用三次testExpCall方法");

        long startTime = System.currentTimeMillis();
        LambdaQueryWrapper<BeanRetryRecord> queryWrapper = new LambdaQueryWrapper<>();
        List<BeanRetryRecord> list = beanRetryRecordMapper.selectList(queryWrapper);
        Assert.isTrue(list.size() == 1, "当前应该查询到1条数据");
        BeanRetryRecord beanRetryRecord = list.get(0);
        System.out.println(System.currentTimeMillis() + " - " + beanRetryRecord);
        // 下一次重试时间应该在3秒后
        long retryInterval = beanRetryRecord.getNextRetryTimestamp() - startTime;
        System.out.println(System.currentTimeMillis() + " - retryInterval: " + retryInterval);
        // 误差在100ms以内
        Assert.isTrue(retryInterval - 3000 < 100, "下一次重试时间应该在3秒后");

        // testEpCall配置的是在3、6、12秒后重试，所以马上查询数据应该是查询不到，因为只会查询到了重试时间的数据
        Thread.sleep(3010);
        retryService.retryAll(10, 10);

        // 检查重试后数据更新是否正确
        Assert.isTrue(RetryTestBean.counter.get() == 4, "testExpCall方法应对已经调用4次");
        list = beanRetryRecordMapper.selectList(queryWrapper);
        Assert.isTrue(list.size() == 1, "当前应该查询到1条数据");
        beanRetryRecord = list.get(0);
        System.out.println(System.currentTimeMillis() + " - " + beanRetryRecord);
        Assert.isTrue(beanRetryRecord.getRetryResult() == 0, "当前retryResult应该等于0");
        Assert.isTrue(beanRetryRecord.getRetryTimes() == 1, "当前retryTimes应该等于1");
        // 第一次重试3s后，第二次重试6s后，所以NextRetryTimestamp距离启动时间应该在9s后
        retryInterval = beanRetryRecord.getNextRetryTimestamp() - startTime;
        System.out.println(System.currentTimeMillis() + " - retryInterval: " + retryInterval);
        // 误差在100ms以内
        Assert.isTrue(retryInterval - 9000 < 100, "下一次重试时间应该在9秒后");
    }

}
