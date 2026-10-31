package com.studyloop.user;

import com.studyloop.common.web.CurrentUser;
import com.studyloop.user.dto.UpdateMeRequest;
import com.studyloop.user.dto.UpdateSubjectsRequest;
import com.studyloop.user.dto.UserMe;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Profile")
public class MeController {

    private final UserService users;

    public MeController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public UserMe me(CurrentUser me) {
        return users.me(me.userId());
    }

    @PutMapping
    public UserMe update(CurrentUser me, @Valid @RequestBody UpdateMeRequest req) {
        return users.update(me.userId(), req);
    }

    @PutMapping("/subjects")
    public UserMe updateSubjects(CurrentUser me, @Valid @RequestBody UpdateSubjectsRequest req) {
        return users.updateSubjects(me.userId(), req);
    }
}
