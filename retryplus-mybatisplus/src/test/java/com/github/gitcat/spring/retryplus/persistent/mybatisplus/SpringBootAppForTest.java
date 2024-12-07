package com.github.gitcat.spring.retryplus.persistent.mybatisplus;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = {"com.github.gitcat.spring.retryplus"})
@EnableRetry
public class SpringBootAppForTest {

}
