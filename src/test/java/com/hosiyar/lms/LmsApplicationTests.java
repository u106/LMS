package com.hosiyar.lms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Requires docker-compose (Postgres) running locally - Testcontainers-backed
 * integration tests are introduced in the Testing episode later in the series.
 */
@SpringBootTest
@ActiveProfiles("test")
class LmsApplicationTests {

    @Test
    void contextLoads() {
    }
}
