package com.tissue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = "com.tissue")
@EnableJpaRepositories(basePackages = "com.tissue")
@SpringBootApplication
public class TissueApplication {
    public static void main(String[] args) {
        SpringApplication.run(TissueApplication.class, args);
    }
}
