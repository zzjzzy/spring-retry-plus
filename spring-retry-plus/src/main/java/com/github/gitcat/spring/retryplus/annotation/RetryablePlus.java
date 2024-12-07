package com.github.gitcat.spring.retryplus.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Retryable
public @interface RetryablePlus {

    @AliasFor(annotation = Retryable.class, attribute = "recover")
    String recover() default "";

    @AliasFor(annotation = Retryable.class, attribute = "value")
    Class<? extends Throwable>[] value() default { Exception.class };

    @AliasFor(annotation = Retryable.class, attribute = "include")
    Class<? extends Throwable>[] include() default {};

    @AliasFor(annotation = Retryable.class, attribute = "exclude")
    Class<? extends Throwable>[] exclude() default {};

    @AliasFor(annotation = Retryable.class, attribute = "label")
    String label() default "";

    @AliasFor(annotation = Retryable.class, attribute = "maxAttempts")
    int maxAttempts() default 3;

    @AliasFor(annotation = Retryable.class, attribute = "backoff")
    Backoff backoff() default @Backoff(delay = 1000, multiplier = 1.5);;

    @AliasFor(annotation = Retryable.class, attribute = "listeners")
    String[] listeners() default {};

    /**
     * 保存在db中的记录最大重试次数
     */
    int dbRetryTimes() default 3;

    /**
     * db中记录重试间隔配置，单位：毫秒
     * 复用spring的Backoff注解，但是只支持配置delay和multiplier配置
     */
    Backoff dbRetryInterval() default @Backoff(delay = 30000, multiplier = 1.5);

    /**
     * 是否支持幂等，需要重试的方法必须支持幂等，避免多次调用造成数据错误
     * 此参数必须为true，设计此参数只是为了提示使用者需要注意方法幂等
     */
    boolean idempotent();

}