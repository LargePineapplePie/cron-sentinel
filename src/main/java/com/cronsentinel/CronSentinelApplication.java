package com.cronsentinel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Cron Sentinel 启动类。
 * 开启 @EnableScheduling 以支持超时扫描定时任务。
 */
@EnableScheduling
@MapperScan("com.cronsentinel.mapper")
@SpringBootApplication
public class CronSentinelApplication {

    public static void main(String[] args) {
        SpringApplication.run(CronSentinelApplication.class, args);
    }
}
