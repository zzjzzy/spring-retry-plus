package com.github.gitcat.spring.retryplus.context;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 重试数据来源
 */
@AllArgsConstructor
@Getter
public enum RetryFrom {

    DB("db");

    private final String from;

}
