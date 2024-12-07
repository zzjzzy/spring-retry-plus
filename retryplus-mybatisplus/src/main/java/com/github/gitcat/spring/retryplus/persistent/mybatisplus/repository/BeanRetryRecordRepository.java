package com.github.gitcat.spring.retryplus.persistent.mybatisplus.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.gitcat.spring.retryplus.persistent.mybatisplus.entity.BeanRetryRecord;
import com.github.gitcat.spring.retryplus.persistent.mybatisplus.mapper.BeanRetryRecordMapper;
import com.github.gitcat.spring.retryplus.enums.YesNoEnum;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfo;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfoRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;

/**
 * 异常重试记录(exception_retry)数据
 */
@AllArgsConstructor
public class BeanRetryRecordRepository implements BeanRetryInfoRepository {

    private BeanRetryRecordMapper beanRetryRecordMapper;

    @Override
    public boolean saveRetryRecord(BeanRetryInfo beanRetryInfo) {
        BeanRetryRecord beanRetryRecord = new BeanRetryRecord();
        beanRetryRecord.setMaxRetryTimes(beanRetryInfo.getMaxRetryTimes());
        beanRetryRecord.setBeanClass(beanRetryInfo.getBeanClass());
        beanRetryRecord.setBeanMethod(beanRetryInfo.getBeanMethod());
        beanRetryRecord.setParamValues(beanRetryInfo.getParamValues());
        beanRetryRecord.setMethodParamTypes(beanRetryInfo.getMethodParamTypes());
        beanRetryRecord.setRealParamTypes(beanRetryInfo.getRealParamTypes());
        beanRetryRecord.setExceptionMsg(beanRetryInfo.getExceptionMsg());
        beanRetryRecord.setNextRetryTimestamp(System.currentTimeMillis() + beanRetryInfo.getNextRetryInterval());
        return beanRetryRecordMapper.insert(beanRetryRecord) > 0;
    }


    @Override
    public List<BeanRetryInfo> queryRetrablePageList(long minId, int pageSize) {
        LambdaQueryWrapper<BeanRetryRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.apply("retry_times < max_retry_times");
        queryWrapper.eq(BeanRetryRecord::getRetryResult, YesNoEnum.NO.getValue());
        queryWrapper.gt(BeanRetryRecord::getId, minId);
        queryWrapper.le(BeanRetryRecord::getNextRetryTimestamp, System.currentTimeMillis());
        queryWrapper.orderByAsc(BeanRetryRecord::getId);
        queryWrapper.last("limit " + pageSize);
        List<BeanRetryRecord> records = beanRetryRecordMapper.selectList(queryWrapper);
        List<BeanRetryInfo> res = new ArrayList<>();
        for (BeanRetryRecord record : records) {
            BeanRetryInfo beanRetryInfo = new BeanRetryInfo();
            beanRetryInfo.setId(record.getId());
            beanRetryInfo.setRetryTimes(record.getRetryTimes());
            beanRetryInfo.setMaxRetryTimes(record.getMaxRetryTimes());
            beanRetryInfo.setBeanClass(record.getBeanClass());
            beanRetryInfo.setBeanMethod(record.getBeanMethod());
            beanRetryInfo.setParamValues(record.getParamValues());
            beanRetryInfo.setMethodParamTypes(record.getMethodParamTypes());
            beanRetryInfo.setRealParamTypes(record.getRealParamTypes());
            beanRetryInfo.setExceptionMsg(record.getExceptionMsg());
            res.add(beanRetryInfo);
        }
        return res;
    }

    @Override
    public void updateRetryRecordSuccess(Long retryId) {
        LambdaUpdateWrapper<BeanRetryRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BeanRetryRecord::getId, retryId);
        updateWrapper.set(BeanRetryRecord::getRetryResult, YesNoEnum.YES.getValue());
        updateWrapper.setSql("retry_times = retry_times + 1");
        beanRetryRecordMapper.update(null, updateWrapper);
    }

    @Override
    public void updateRetryFail(Long retryId, String errorMsg, long nextRetryInterval) {
        LambdaUpdateWrapper<BeanRetryRecord> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BeanRetryRecord::getId, retryId);
        updateWrapper.set(BeanRetryRecord::getRetryResult, YesNoEnum.NO.getValue());
        updateWrapper.set(BeanRetryRecord::getExceptionMsg, errorMsg);
        updateWrapper.setSql("retry_times = retry_times + 1");
        updateWrapper.setSql("next_retry_timestamp = next_retry_timestamp + " + nextRetryInterval);
        beanRetryRecordMapper.update(null, updateWrapper);
    }
}