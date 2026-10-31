package com.studyloop.ai;

import com.studyloop.ai.dto.SnapResponse;
import com.studyloop.common.web.CurrentUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI")
public class SnapController {

    private final SnapService snap;

    public SnapController(SnapService snap) {
        this.snap = snap;
    }

    @PostMapping(value = "/snap", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SnapResponse snap(CurrentUser me, @RequestParam("image") MultipartFile image) {
        return snap.snap(me, image);
    }
}
