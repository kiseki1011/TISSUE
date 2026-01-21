package com.tissue.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.TestcontainersConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected EntityManager em;

    @Autowired
    protected DatabaseCleanup databaseCleanup;

    @BeforeEach
    void setUp() {
        databaseCleanup.execute();
    }
}
