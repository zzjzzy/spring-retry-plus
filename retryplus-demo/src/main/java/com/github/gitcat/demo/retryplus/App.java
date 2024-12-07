package com.github.gitcat.demo.retryplus;

import com.github.gitcat.demo.retryplus.dao.mapper.OrderUserMapper;
import com.github.gitcat.demo.retryplus.service.DemoService;
import com.github.gitcat.spring.retryplus.service.RetryService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class App {

    /**
     * 启动时修改数据库链接信息
     * -Dspring.datasource.url=jdbc:mysql://127.0.0.1:3306/test
     * -Dspring.datasource.username=<USERNAME>
     * -Dspring.datasource.password=<PASSWORD>
     */
    public static void main(String[] args) throws Exception {
        ConfigurableApplicationContext context = new SpringApplicationBuilder()
                .sources(App.class)
                .run(args);

        try {
            DemoService demoService = context.getBean(DemoService.class);
            // 执行完这个方法，由于没有异常，数据表中预期无数据插入
//            demoService.testNoExpCall("noExpCallParam");
            // 执行完这个方法，数据表中预期插入一条数据
            demoService.testExpCall("expCallParam");
        } catch (Exception ex) {
            System.out.println("异常信息：" + ex.getMessage());
        }

        System.out.println();
        System.out.println();
        System.out.println("======================");
        // 因为下次重试时间为30s后，所以这里sleep 31s
        Thread.sleep(31000);
        RetryService retryService = context.getBean(RetryService.class);
        retryService.retryAll(10, 10);
        // 执行到这里，预期数据表中的数据retry_times字段值为1

        OrderUserMapper bean = context.getBean(OrderUserMapper.class);
        System.out.println(bean);
    }
}
