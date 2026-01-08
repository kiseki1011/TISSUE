package com.tissue;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer(
            @Value("${tissue.test.db-image:postgres:16-alpine}") String dbImageName) {
        return new PostgreSQLContainer<>(DockerImageName.parse(dbImageName));
    }

    @Bean
    @ServiceConnection(name = "redis")
    public GenericContainer<?> redisContainer(
            @Value("${tissue.test.redis-image:redis:alpine}") String redisImageName,
            @Value("${tissue.test.redis-port:6379}") int redisExposedPort) {
        return new GenericContainer<>(DockerImageName.parse(redisImageName)).withExposedPorts(redisExposedPort);
    }
}
