package com.github.gitcat.spring.retryplus.test.assist;

import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfo;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfoRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class BeanRetryInfoTestRepository implements BeanRetryInfoRepository {

    public static BeanRetryInfo savedBeanRetryInfo = null;

    public static BeanRetryInfo updatedBeanRetryInfo = null;

    public static Long successRetryId = null;

    public static void clearInfo() {
        savedBeanRetryInfo = null;
        updatedBeanRetryInfo = null;
        successRetryId = null;
    }

    @Override
    public boolean saveRetryRecord(BeanRetryInfo beanRetryInfo) {
        System.out.println("saveRetryRecord:" + beanRetryInfo);
        savedBeanRetryInfo = beanRetryInfo;
        savedBeanRetryInfo.setId(1L);
        return true;
    }

    @Override
    public List<BeanRetryInfo> queryRetrablePageList(long minId, int pageSize) {
        System.out.println("queryRetrablePageList:" + minId + "," + pageSize);
        if (minId == 0L) {
            return Arrays.asList(savedBeanRetryInfo);
        }
        return Collections.emptyList();
    }

    @Override
    public void updateRetryRecordSuccess(Long retryId) {
        successRetryId = retryId;
        savedBeanRetryInfo.setRetryTimes(savedBeanRetryInfo.getRetryTimes() + 1);
        System.out.println("updateRetryRecordSuccess:" + retryId);
    }

    @Override
    public void updateRetryFail(Long retryId, String errorMsg, long nextRetryInterval) {
        savedBeanRetryInfo.setExceptionMsg(errorMsg);
        savedBeanRetryInfo.setNextRetryInterval(nextRetryInterval);
        savedBeanRetryInfo.setRetryTimes(savedBeanRetryInfo.getRetryTimes() + 1);
        updatedBeanRetryInfo = savedBeanRetryInfo;
        System.out.println("updateRetryFail:" + retryId + "," + errorMsg + "," + nextRetryInterval);
    }
}
