package com.studyloop.pulse;

import com.studyloop.common.web.CurrentUser;
import com.studyloop.pulse.dto.PulseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pulse")
@Tag(name = "Campus Pulse")
public class PulseController {

    private final PulseService pulse;

    public PulseController(PulseService pulse) {
        this.pulse = pulse;
    }

    @GetMapping
    public PulseDto pulse(CurrentUser me, @RequestParam(defaultValue = "7") int days) {
        return pulse.pulse(me.campusId(), days);
    }
}
