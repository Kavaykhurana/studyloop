package com.studyloop.hint;

import com.studyloop.common.web.CurrentUser;
import com.studyloop.hint.dto.HintNextResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doubts/{id}/hints")
@Tag(name = "Hints")
public class HintController {

    private final HintService hints;

    public HintController(HintService hints) {
        this.hints = hints;
    }

    @PostMapping("/next")
    public HintNextResponse next(CurrentUser me, @PathVariable long id) {
        return hints.next(me, id);
    }
}
