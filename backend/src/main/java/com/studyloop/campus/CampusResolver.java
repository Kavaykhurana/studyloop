package com.studyloop.campus;

import java.util.Locale;
import org.springframework.stereotype.Component;

/** Maps an email domain (or any subdomain of it) to a campus; unknown domains join the Open Campus. */
@Component
public class CampusResolver {

    public static final long OPEN_CAMPUS_ID = 2L;

    private final CampusRepository campuses;

    public CampusResolver(CampusRepository campuses) {
        this.campuses = campuses;
    }

    public long resolve(String email) {
        int at = email.lastIndexOf('@');
        String domain = at < 0 ? "" : email.substring(at + 1).toLowerCase(Locale.ROOT);
        for (Campus campus : campuses.findByEmailDomainIsNotNull()) {
            String registered = campus.getEmailDomain().toLowerCase(Locale.ROOT);
            if (domain.equals(registered) || domain.endsWith("." + registered)) {
                return campus.getId();
            }
        }
        return OPEN_CAMPUS_ID;
    }
}
