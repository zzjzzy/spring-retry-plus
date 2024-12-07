package com.github.gitcat.spring.retryplus.persistent;

import lombok.Data;

/**
 * 异常重试信息
 */
@Data
public class BeanRetryInfo {

    /**
     * id
     */
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
     * 重试需要的参数值，会保存成json list格式，list每项对应方法的参数
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
     * 下次重试间隔
     */
    private Long nextRetryInterval;

}