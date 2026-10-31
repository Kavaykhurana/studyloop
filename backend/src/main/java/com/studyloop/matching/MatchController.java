package com.studyloop.matching;

import com.studyloop.common.web.CurrentUser;
import com.studyloop.matching.dto.MatchDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doubts/{id}/matches")
@Tag(name = "Matching")
public class MatchController {

    private final PeerMatchingService matching;

    public MatchController(PeerMatchingService matching) {
        this.matching = matching;
    }

    @PostMapping("/refresh")
    public List<MatchDto> refresh(CurrentUser me, @PathVariable long id) {
        return matching.refresh(me, id);
    }
}
