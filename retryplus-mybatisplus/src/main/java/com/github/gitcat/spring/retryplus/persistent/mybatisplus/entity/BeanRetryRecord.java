package com.github.gitcat.spring.retryplus.persistent.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

@Data
@TableName("bean_retry_record")
public class BeanRetryRecord {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 已重试次数
     */
    private Integer retryTimes;
    /**
     * 允许的最大重试次数
     */
    private Integer maxRetryTimes;
    /**
     * 需要重试的beanClass
     */
    private String beanClass;
    /**
     * 需要重试的bean方法
     */
    private String beanMethod;
    /**
     * 重试需要的参数，会保存成json格式，所以方法参数必须支持json序列化
     */
    private String paramValues;
    /**
     * 方法定义的参数类型
     */
    private String methodParamTypes;
    /**
     * 实际传入的参数类型
     */
    private String realParamTypes;
    /**
     * 异常信息
     */
    private String exceptionMsg;
    /**
     * 重试结果
     */
    private Integer retryResult;
    /**
     * 下次重试时间，毫秒时间戳
     */
    private Long nextRetryTimestamp;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;

}