package com.studyloop.support;

import com.studyloop.common.ratelimit.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real PostgreSQL + pgvector for every integration test. CI uses Testcontainers; set TEST_DATABASE_URL (plus
 * TEST_DATABASE_USER / TEST_DATABASE_PASSWORD) to point at an existing database when Docker is unavailable.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import({TestClockConfig.class, Fixtures.class})
public abstract class IntegrationTestBase {

    private static final PostgreSQLContainer CONTAINER;

    static {
        if (System.getenv("TEST_DATABASE_URL") == null) {
            CONTAINER = new PostgreSQLContainer(
                    DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));
            CONTAINER.start();
        } else {
            CONTAINER = null;
        }
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        if (CONTAINER != null) {
            registry.add("spring.datasource.url", CONTAINER::getJdbcUrl);
            registry.add("spring.datasource.username", CONTAINER::getUsername);
            registry.add("spring.datasource.password", CONTAINER::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> System.getenv("TEST_DATABASE_URL"));
            registry.add("spring.datasource.username",
                    () -> System.getenv().getOrDefault("TEST_DATABASE_USER", "studyloop"));
            registry.add("spring.datasource.password",
                    () -> System.getenv().getOrDefault("TEST_DATABASE_PASSWORD", "studyloop"));
        }
    }

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected MutableClock clock;
    @Autowired
    protected Fixtures fx;
    @Autowired
    protected RateLimiter rateLimiter;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("""
                TRUNCATE users, doubts, answers, doubt_matches, ai_hints, hint_progress, deflection_events,
                         reputation_events, notifications, refresh_tokens, study_plans, plan_tasks, ai_calls,
                         user_subjects RESTART IDENTITY CASCADE""");
        clock.reset();
        rateLimiter.reset();
    }
}
