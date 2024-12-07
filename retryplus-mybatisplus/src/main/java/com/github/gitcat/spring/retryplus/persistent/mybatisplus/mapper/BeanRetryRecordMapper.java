package com.github.gitcat.spring.retryplus.persistent.mybatisplus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.gitcat.spring.retryplus.persistent.mybatisplus.entity.BeanRetryRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 异常重试记录(exception_retry)数据Mapper
 */
@Mapper
public interface BeanRetryRecordMapper extends BaseMapper<BeanRetryRecord> {

}
