package com.studyloop.user;

import com.studyloop.campus.Campus;
import com.studyloop.campus.CampusRepository;
import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.common.web.CurrentUser;
import com.studyloop.doubt.DoubtMapper;
import com.studyloop.doubt.dto.DoubtSummary;
import com.studyloop.subject.SubjectCatalog;
import com.studyloop.subject.SubjectInfo;
import com.studyloop.user.dto.PublicProfile;
import com.studyloop.user.dto.UpdateMeRequest;
import com.studyloop.user.dto.UpdateSubjectsRequest;
import com.studyloop.user.dto.UserMe;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;
    private final UserSubjectRepository userSubjects;
    private final CampusRepository campuses;
    private final SubjectCatalog catalog;
    private final JdbcTemplate jdbc;
    private final DoubtMapper doubtMapper;

    public UserService(UserRepository users, UserSubjectRepository userSubjects, CampusRepository campuses,
                       SubjectCatalog catalog, JdbcTemplate jdbc, DoubtMapper doubtMapper) {
        this.users = users;
        this.userSubjects = userSubjects;
        this.campuses = campuses;
        this.catalog = catalog;
        this.jdbc = jdbc;
        this.doubtMapper = doubtMapper;
    }

    @Transactional(readOnly = true)
    public UserMe me(long userId) {
        return toMe(users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED)));
    }

    @Transactional(readOnly = true)
    public UserMe toMe(User u) {
        Campus campus = campuses.findById(u.getCampusId()).orElseThrow();
        List<UserMe.MySubject> subjects = userSubjects.findByUserId(u.getId()).stream()
                .map(us -> catalog.byId(us.getSubjectId()).map(s -> new UserMe.MySubject(s.id(), s.code(), s.name(),
                        us.getKind().name(), us.getLevel())).orElse(null))
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(UserMe.MySubject::subjectId))
                .toList();
        return new UserMe(u.getId(), u.getName(), u.getEmail(),
                new UserMe.CampusRef(campus.getId(), campus.getName(), campus.getShortName()),
                u.getYearOfStudy(), u.getBranch(), u.getBio(), u.getReputation(), u.isOnboarded(), u.isDemo(),
                subjects);
    }

    @Transactional
    public UserMe update(long userId, UpdateMeRequest req) {
        User u = users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
        String name = req.name().trim();
        if (u.isDemo() && !name.equals(u.getName())) {
            throw new ApiException(ErrorCode.DEMO_READ_ONLY);
        }
        u.setName(name);
        u.setYearOfStudy(req.yearOfStudy());
        u.setBranch(blankToNull(req.branch()));
        u.setBio(blankToNull(req.bio()));
        return toMe(users.save(u));
    }

    @Transactional
    public UserMe updateSubjects(long userId, UpdateSubjectsRequest req) {
        User u = users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
        Set<Long> seen = new HashSet<>();
        for (UpdateSubjectsRequest.Entry e : req.subjects()) {
            if (catalog.byId(e.subjectId()).isEmpty() || !seen.add(e.subjectId())) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Each subject can be picked once.");
            }
        }
        userSubjects.deleteAllForUser(userId);
        req.subjects().forEach(e -> userSubjects.save(new UserSubject(userId, e.subjectId(),
                UserSubject.Kind.valueOf(e.kind()), e.level())));
        u.setOnboarded(true);
        users.save(u);
        return toMe(u);
    }

    @Transactional(readOnly = true)
    public PublicProfile profile(CurrentUser me, long id) {
        User u = users.findById(id).filter(x -> x.getCampusId().equals(me.campusId()))
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
        List<PublicProfile.StrongSubject> strong = userSubjects.findByUserId(id).stream()
                .filter(us -> us.getKind() == UserSubject.Kind.STRONG)
                .map(us -> catalog.byId(us.getSubjectId()).map(s -> new PublicProfile.StrongSubject(s.code(),
                        us.getLevel())).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        List<PublicProfile.SubjectReputation> reputation = jdbc.query(
                "SELECT subject_id, SUM(delta) AS rep FROM reputation_events WHERE user_id = ? "
                        + "GROUP BY subject_id ORDER BY rep DESC", (rs, i) -> {
                    SubjectInfo s = catalog.byId(rs.getLong("subject_id")).orElseThrow();
                    return new PublicProfile.SubjectReputation(s.code(), rs.getInt("rep"));
                }, id);
        PublicProfile.Stats stats = jdbc.queryForObject(
                "SELECT COUNT(*) AS answers, COUNT(*) FILTER (WHERE is_accepted) AS accepted, "
                        + "AVG(helpful_rating) AS avg_helpful FROM answers WHERE author_id = ?",
                (rs, i) -> new PublicProfile.Stats(rs.getLong("answers"), rs.getLong("accepted"),
                        rs.getObject("avg_helpful") == null ? null
                                : Math.round(rs.getDouble("avg_helpful") * 10.0) / 10.0), id);
        List<Long> recentIds = jdbc.queryForList(
                "SELECT d.id FROM answers a JOIN doubts d ON d.id = a.doubt_id "
                        + "WHERE a.author_id = ? AND a.is_accepted AND d.campus_id = ? "
                        + "ORDER BY a.accepted_at DESC LIMIT 5", Long.class, id, me.campusId());
        List<DoubtSummary> recent = doubtMapper.summariesInOrder(recentIds);
        return new PublicProfile(UserLookup.brief(u), u.getBio(), strong, reputation, stats, recent);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
