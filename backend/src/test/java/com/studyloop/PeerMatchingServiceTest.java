package com.studyloop;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyloop.doubt.Doubt;
import com.studyloop.doubt.DoubtRepository;
import com.studyloop.matching.PeerMatchingService;
import com.studyloop.matching.dto.MatchDto;
import com.studyloop.support.IntegrationTestBase;
import com.studyloop.user.User;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PeerMatchingServiceTest extends IntegrationTestBase {

    private static final String TITLE = "Why does my BST insert become O(n) on sorted input?";
    private static final String BODY = "I insert 1 to 1000 in order into my binary search tree and the tree becomes "
            + "a chain, so every insert walks the whole height of the tree.";

    @Autowired
    PeerMatchingService matching;
    @Autowired
    DoubtRepository doubts;

    private Doubt newDoubt(User author) {
        long id = fx.doubt(author, "DSA", "Trees & BST", TITLE, BODY);
        return doubts.findById(id).orElseThrow();
    }

    /** Gives the helper one solved doubt that is near-identical to the new one. */
    private void solvedSimilar(User helper) {
        User someone = fx.tiet("Earlier Asker " + helper.getName());
        fx.solvedDoubt(someone, helper, "DSA", "Trees & BST", TITLE + " (earlier)", BODY,
                "The tree degenerates into a chain because sorted keys always go right.");
    }

    @Test
    void theAuthorIsNeverARecommendedHelper() {
        User author = fx.strongHelper("Asha Author", "DSA", 5);
        fx.strongHelper("Hari Helper", "DSA", 3);
        List<MatchDto> matches = matching.matchAndNotify(newDoubt(author));
        assertThat(matches).extracting(m -> m.helper().id()).doesNotContain(author.getId());
        assertThat(matches).hasSize(1);
    }

    @Test
    void proofOfSolvingSimilarDoubtsOutranksAHigherSelfDeclaredLevel() {
        User proven = fx.strongHelper("Proven Pat", "DSA", 3);
        User claimed = fx.strongHelper("Claimed Chris", "DSA", 5);
        solvedSimilar(proven);
        User author = fx.tiet("Asha Author");

        List<MatchDto> matches = matching.matchAndNotify(newDoubt(author));

        assertThat(matches.get(0).helper().id()).isEqualTo(proven.getId());
        assertThat(matches.get(0).breakdown().get("semantic")).isGreaterThan(0.5);
        assertThat(matches.get(0).reasons().get(0)).startsWith("Solved 1 similar");
        assertThat(matches.get(1).helper().id()).isEqualTo(claimed.getId());
        assertThat(matches.get(1).breakdown().get("semantic")).isZero();
    }

    @Test
    void currentLoadLowersTheScore() {
        User busy = fx.strongHelper("Busy Bee", "DSA", 4);
        User free = fx.strongHelper("Free Fran", "DSA", 4);
        User author = fx.tiet("Asha Author");
        for (int i = 0; i < 3; i++) {
            long open = fx.doubt(author, "DSA", "Graphs", "Another unanswered graph doubt number " + i, BODY);
            jdbc.update("INSERT INTO doubt_matches (doubt_id, helper_id, rank, score, breakdown, reasons) "
                    + "VALUES (?, ?, 1, 50, '{}'::jsonb, '[]'::jsonb)", open, busy.getId());
        }

        List<MatchDto> matches = matching.matchAndNotify(newDoubt(author));

        MatchDto busyMatch = matches.stream().filter(m -> m.helper().id().equals(busy.getId())).findFirst().orElseThrow();
        MatchDto freeMatch = matches.stream().filter(m -> m.helper().id().equals(free.getId())).findFirst().orElseThrow();
        assertThat(busyMatch.breakdown().get("load")).isEqualTo(1.0);
        assertThat(busyMatch.score()).isLessThan(freeMatch.score());
        assertThat(busyMatch.reasons()).contains("Already helping with 3 open doubts");
    }

    @Test
    void reasonsFollowTheSpecifiedRulesAndAreCappedAtThree() {
        User star = fx.strongHelper("Star Sam", "DSA", 5);
        solvedSimilar(star);
        User author = fx.tiet("Asha Author");

        MatchDto match = matching.matchAndNotify(newDoubt(author)).get(0);

        assertThat(match.reasons()).hasSizeLessThanOrEqualTo(3);
        assertThat(match.reasons()).contains("Solved 1 similar Trees & BST doubt", "Rates DSA 5/5");
        assertThat(match.reasons()).anyMatch(r -> r.contains("reputation in DSA"));
    }

    @Test
    void onlyTheTopThreeQualifyingHelpersAreNotifiedAndNeverTwice() {
        User author = fx.tiet("Asha Author");
        for (int i = 1; i <= 5; i++) {
            fx.strongHelper("Helper " + i, "DSA", i);
        }
        Doubt doubt = newDoubt(author);

        matching.matchAndNotify(doubt);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE type = 'MATCHED'", Integer.class))
                .isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM doubt_matches WHERE doubt_id = ?", Integer.class,
                doubt.getId())).isEqualTo(5);

        matching.matchAndNotify(doubt);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE type = 'MATCHED'", Integer.class))
                .isEqualTo(3);
    }

    @Test
    void helpersBelowTheMinimumScoreAreNotNotified() {
        User author = fx.tiet("Asha Author");
        User sleepy = fx.strongHelper("Sleepy Sid", "DSA", 1);
        fx.touch(sleepy, Duration.ofDays(30));

        List<MatchDto> matches = matching.matchAndNotify(newDoubt(author));

        assertThat(matches.get(0).score()).isLessThan(25);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE type = 'MATCHED'", Integer.class))
                .isZero();
    }
}
