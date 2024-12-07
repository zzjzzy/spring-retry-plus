package com.github.gitcat.spring.retryplus.persistent;

import java.util.List;

/**
 * BeanRetryInfo信息保存策略类，基于不同实现可将信息保存在不同位置
 */
public interface BeanRetryInfoRepository {

    /**
     * 保存异常调用信息
     * @return 是否保存成功
     */
    boolean saveRetryRecord(BeanRetryInfo beanRetryInfo);

    /**
     * 分页查询可重试的调用记录
     * 可重试是指：
     * 1. 达到下次重试时间
     * 2. 未达到最大重试次数
     * 3. 上次重试未成功
     * @param minId 查询大于minId的数据，通过minId来实现分页，避免深度分页，查询时需要根据id排序返回数据
     * @param pageSize 分页大小
     */
    List<BeanRetryInfo> queryRetrablePageList(long minId, int pageSize);

    /**
     * 更新重试记录重试成功
     * @param retryId 重试记录id
     */
    void updateRetryRecordSuccess(Long retryId);

    /**
     * 更新重试记录重试失败
     * @param retryId 重试记录id
     * @param errorMsg 异常信息
     * @param nextRetryInterval 下次重试间隔时间，实现类可根据此值和上次重试时间，计算下次重试时间，此时间间隔单位由实现类自行定义
     */
    void updateRetryFail(Long retryId, String errorMsg, long nextRetryInterval);
}
