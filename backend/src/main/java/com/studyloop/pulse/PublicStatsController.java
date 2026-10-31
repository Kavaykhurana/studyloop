package com.studyloop.pulse;

import com.studyloop.pulse.dto.PublicStats;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Public")
public class PublicStatsController {

    private final PulseService pulse;

    public PublicStatsController(PulseService pulse) {
        this.pulse = pulse;
    }

    @GetMapping("/stats")
    public PublicStats stats() {
        return pulse.publicStats();
    }
}
