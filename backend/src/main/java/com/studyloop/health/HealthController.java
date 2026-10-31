package com.studyloop.health;

import com.studyloop.config.AppProperties;
import com.studyloop.embedding.EmbeddingSpaceGuard;
import com.studyloop.seed.DemoSeeder;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health")
public class HealthController {

    private final JdbcTemplate jdbc;
    private final AppProperties props;
    private final DemoSeeder seeder;
    private final EmbeddingSpaceGuard embeddingGuard;

    public HealthController(JdbcTemplate jdbc, AppProperties props, DemoSeeder seeder,
                            EmbeddingSpaceGuard embeddingGuard) {
        this.jdbc = jdbc;
        this.props = props;
        this.seeder = seeder;
        this.embeddingGuard = embeddingGuard;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        boolean dbUp;
        try {
            Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
            dbUp = one != null && one == 1;
        } catch (RuntimeException e) {
            dbUp = false;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        boolean up = dbUp && seeder.isReady() && embeddingGuard.isReady();
        body.put("status", up ? "UP" : dbUp ? "STARTING" : "DOWN");
        body.put("db", dbUp ? "UP" : "DOWN");
        body.put("ai", props.ai().enabled() ? "GEMINI" : "FAKE");
        body.put("version", props.version().gitSha());
        return ResponseEntity.status(up ? 200 : 503).body(body);
    }
}
