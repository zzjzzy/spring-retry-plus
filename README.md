基于spring-retry框架扩展的retry实现，在spring-retry基础上，如果重试失败，会将失败信息保存到数据库，用于后续定时任务扫描重试。

## 模块说明
### retryplus-demo
使用@RetryablePlus示例，使用时可参考此示例。

### spring-retry-plus
实现对RetryListener的扩展，重试记录的保存基于com.github.gitcat.spring.retryplus.persistent.BeanRetryInfoRepository接口实现，
默认实现在retryplus-mybatisplus模块中，基于mybatisplus保存到mysql，如果业务有自己的保存方式，可自行实现BeanRetryInfoRepository接口，并将实现类注册为spring bean。

### retryplus-mybatisplus
基于mybatisplus的重试记录保存实现

## 用法
为避免和业务项目中依赖冲突，本项目大部分maven依赖配置的都是`<optional>true</optional>`，如果发现java.lang.ClassNotFoundException异常，可自行引入相关jar包
大部分情况下，如果你使用的是spring框架，业务项目依赖的jar包已经可以满足要求。

### 引入依赖
```xml
<dependencies>
    <dependency>
        <groupId>com.github.gitcat</groupId>
        <artifactId>spring-retry-plus</artifactId>
        <version>最新版本号</version>
    </dependency>
    <dependency>
        <groupId>com.github.gitcat</groupId>
        <artifactId>retryplus-mybatisplus</artifactId>
        <version>最新版本号</version>
    </dependency>
</dependencies>
```

### 新建数据库
如果不使用默认实现的retryplus-mybatisplus，这一步可以不做。
异常调用信息会保存到数据库，用于后续定时任务扫描重试
```sql
CREATE TABLE `bean_retry_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `retry_times` int(11) DEFAULT '0' COMMENT '已重试次数',
  `max_retry_times` int(11) DEFAULT '0' COMMENT '允许的最大重试次数',
  `bean_class` varchar(255) DEFAULT '' COMMENT '需要重试的bean类',
  `bean_method` varchar(255) DEFAULT '' COMMENT '需要重试的bean方法',
  `param_values` text COMMENT '重试需要的参数',
  `method_param_types` text COMMENT '方法定义的参数类型',
  `real_param_types` text COMMENT '实际传入的参数类型',
  `exception_msg` text  COMMENT '异常信息',
  `retry_result` int(11) DEFAULT '0' COMMENT '重试结果',
  `next_retry_timestamp` bigint(20) DEFAULT NULL COMMENT '下次重试时间，毫秒时间戳',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Bean重试记录表';
```
### 配置定时任务
定时任务可以根据情况自行配置，通过定时任务调用com.github.gitcat.spring.retryplus.service.RetryService.retryAll来实现重试。
如果业务对于重试有自己的个性化需求（比如执行重试前设置traceId），可通过覆写RetryService.retryAll，或者自行实现重试方式。

### 代码中使用
#### 配置
- 配置文件中开启功能 spring.retryplus.enable=true
- 启动类上加@EnableRetry

#### 在需要重试的方法上添加@RetryablePlus注解
该注解继承了spring的@Retryable注解，用法和spring的@Retryable一致。

使用@RetryablePlus注解后，表现如下
- 如果在maxAttempts次数内重试失败，则会将失败信息及调用方法等信息保存到数据库，用于后续定时任务扫描重试。
- 如果在maxAttempts次数内重试成功，则不会保存失败信息
- 定时任务重试时，会再次调用@RetryablePlus注解的方法，如果重试成功，标记数据库记录重试成功。如果重试失败，计算下次重试时间并将重试次数加1，更新重试记录。
- 当定时任务重试次数达到RetryablePlus.dbRetryTimes次数后或者重试成功后，不会再进行重试。

#### 使用限制
因为要通过json反序列化方法参数，所以注解的方法参数类型必须有无参构造函数<br/>
不支持@Async和@RetryablePlus同时使用，原因请查看com.github.gitcat.spring.retryplus.listener.RetryPlusListener.close注释<br/>

