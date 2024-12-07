package com.github.gitcat.spring.retryplus.context;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class RetryInfoHolder {

    private static final ThreadLocal<RetryInfo> threadLocal = new ThreadLocal<>();

    public static void set(RetryInfo scene) {
        threadLocal.set(scene);
    }

    public static RetryInfo get() {
        return threadLocal.get();
    }

    public static boolean isFromDb() {
        RetryInfo retryInfo = get();
        return retryInfo != null && RetryFrom.DB.getFrom().equals(retryInfo.getFrom());
    }

    public static Long getRetryId() {
        RetryInfo retryInfo = get();
        return retryInfo != null ? retryInfo.getRetryId() : null;
    }

    public static void remove() {
        threadLocal.remove();
    }

    public static int getRetryTimes() {
        return get().getRetryTimes();
    }

    @Getter
    @AllArgsConstructor
    public static class RetryInfo {
        // retry数据来源
        private String from;
        // 重试记录数据id
        private Long retryId;
        // 重试记录已经重试次数
        private int retryTimes;
    }
}
