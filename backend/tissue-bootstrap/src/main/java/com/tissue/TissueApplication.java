package com.tissue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TissueApplication {

    public static void main(String[] args) {
        SpringApplication.run(TissueApplication.class, args);
    }
}
