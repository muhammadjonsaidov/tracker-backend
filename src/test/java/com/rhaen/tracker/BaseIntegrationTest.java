package com.rhaen.tracker;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

        static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
                        org.testcontainers.utility.DockerImageName.parse("postgis/postgis:16-3.4")
                                        .asCompatibleSubstituteFor("postgres"))
                        .withDatabaseName("tracker_test")
                        .withUsername("test")
                        .withPassword("test");

        static {
                postgres.start();
        }

        @org.springframework.test.context.DynamicPropertySource
        static void configureProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", postgres::getJdbcUrl);
                registry.add("spring.datasource.username", postgres::getUsername);
                registry.add("spring.datasource.password", postgres::getPassword);
        }

}
