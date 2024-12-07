package com.github.gitcat.spring.retryplus.persistent.mybatisplus.configuration;

import com.github.gitcat.spring.retryplus.persistent.mybatisplus.mapper.BeanRetryRecordMapper;
import com.github.gitcat.spring.retryplus.persistent.mybatisplus.repository.BeanRetryRecordRepository;
import com.github.gitcat.spring.retryplus.persistent.BeanRetryInfoRepository;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "com.github.gitcat.spring.retryplus.persistent.mybatisplus.mapper")
public class BeanConfiguration {

    @Bean
    @ConditionalOnMissingBean(BeanRetryInfoRepository.class)
    public BeanRetryInfoRepository beanRetryInfoRepository(BeanRetryRecordMapper beanRetryRecordMapper) {
        return new BeanRetryRecordRepository(beanRetryRecordMapper);
    }

}
