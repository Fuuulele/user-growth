package com.usergrowth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.usergrowth.infrastructure.mapper")
public class UserGrowthApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserGrowthApplication.class, args);
    }
}