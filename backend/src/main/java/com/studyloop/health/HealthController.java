package com.studyloop.health;

import com.studyloop.config.AppProperties;
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

    public HealthController(JdbcTemplate jdbc, AppProperties props) {
        this.jdbc = jdbc;
        this.props = props;
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
        body.put("status", dbUp ? "UP" : "DOWN");
        body.put("db", dbUp ? "UP" : "DOWN");
        body.put("ai", props.ai().enabled() ? "GEMINI" : "FAKE");
        body.put("version", props.version().gitSha());
        return ResponseEntity.status(dbUp ? 200 : 503).body(body);
    }
}
