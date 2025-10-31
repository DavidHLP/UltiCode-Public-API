package com.david.open.problem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.david.open.problem.mapper")
public class PublicProblemApplication {
    public static void main(String[] args) {
        SpringApplication.run(PublicProblemApplication.class, args);
    }
}
