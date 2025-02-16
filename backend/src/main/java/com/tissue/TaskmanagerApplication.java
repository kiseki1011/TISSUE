package com.tissue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.tissue.api.issue.config.properties.IssueProperties;

@SpringBootApplication
@EnableConfigurationProperties(IssueProperties.class)
public class TaskmanagerApplication {
	public static void main(String[] args) {
		SpringApplication.run(TaskmanagerApplication.class, args);
	}
}
