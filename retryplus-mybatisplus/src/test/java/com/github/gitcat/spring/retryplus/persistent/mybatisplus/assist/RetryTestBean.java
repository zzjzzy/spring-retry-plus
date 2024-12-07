package com.github.gitcat.spring.retryplus.persistent.mybatisplus.assist;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.gitcat.spring.retryplus.persistent.mybatisplus.mapper.BeanRetryRecordMapper;
import com.github.gitcat.spring.retryplus.annotation.RetryablePlus;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
public class RetryTestBean {

    public static AtomicInteger counter = new AtomicInteger(0);
    @Autowired
    private BeanRetryRecordMapper beanRetryRecordMapper;

    /**
     * 测试正常流程
     */
    @RetryablePlus
    public void testCall(Object param)  {
        counter.getAndIncrement();
        System.out.println(System.currentTimeMillis() + ": testCall请求入参：" + param);
    }

    /**
     * 测试异常流程
     */
    @RetryablePlus(dbRetryInterval = @Backoff(delay = 3000, multiplier = 2), dbRetryTimes = 4)
    public void testExpCall(Object param)  {
        counter.getAndIncrement();
        System.out.println(System.currentTimeMillis() + ": testExpCall请求入参：" + param);
        if (counter.get() < 5) {
            int div = 1/0;
        }
    }

    public void clearInfo() {
        counter.set(0);
        beanRetryRecordMapper.delete(new LambdaQueryWrapper<>());
    }
}
