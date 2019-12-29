package com.zhou.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.zhou.seckill.dao")
public class MainApplication{
	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}

}