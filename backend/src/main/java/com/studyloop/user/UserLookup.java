package com.studyloop.user;

import com.studyloop.user.dto.UserBrief;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Batch resolution of public user summaries, so list endpoints never issue per-row user queries. */
@Component
public class UserLookup {

    private final UserRepository users;

    public UserLookup(UserRepository users) {
        this.users = users;
    }

    public static UserBrief brief(User u) {
        return new UserBrief(u.getId(), u.getName(), u.getYearOfStudy(), u.getBranch(), u.getReputation());
    }

    @Transactional(readOnly = true)
    public Map<Long, UserBrief> briefs(Collection<Long> ids) {
        Set<Long> unique = ids.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, UserBrief> result = new HashMap<>();
        if (unique.isEmpty()) {
            return result;
        }
        users.findAllById(unique).forEach(u -> result.put(u.getId(), brief(u)));
        return result;
    }
}
