package com.studyloop.user;

import com.studyloop.common.web.CurrentUser;
import com.studyloop.user.dto.PublicProfile;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Profile")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/{id}")
    public PublicProfile profile(CurrentUser me, @PathVariable long id) {
        return users.profile(me, id);
    }
}
