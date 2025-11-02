package com.david.open.dashboard;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.david.open.dashboard.mapper")
public class PublicDashboardApplication {
  public static void main(String[] args) {
    SpringApplication.run(PublicDashboardApplication.class, args);
  }
}
