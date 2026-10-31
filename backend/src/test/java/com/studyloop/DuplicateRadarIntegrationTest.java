package com.studyloop;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyloop.doubt.DuplicateRadarService;
import com.studyloop.doubt.dto.SimilarRequest;
import com.studyloop.doubt.dto.SimilarResponse;
import com.studyloop.support.IntegrationTestBase;
import com.studyloop.user.User;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DuplicateRadarIntegrationTest extends IntegrationTestBase {

    private static final String TITLE = "Why does my BST insert become O(n) on sorted input?";
    private static final String BODY = "I insert 1 to 1000 in order into my binary search tree and the tree becomes "
            + "a chain, so every insert walks the whole height of the tree.";

    @Autowired
    DuplicateRadarService radar;

    @Test
    void aNearIdenticalQuestionFindsTheSolvedDoubtAndAnUnrelatedOneDoesNot() {
        User author = fx.tiet("Asha Learner");
        User helper = fx.strongHelper("Hari Helper", "DSA", 4);
        long solved = fx.solvedDoubt(author, helper, "DSA", "Trees & BST", TITLE, BODY,
                "Sorted inserts always go to the right, so the tree degenerates into a chain of height n.");
        User asker = fx.tiet("Nina Asker");

        SimilarResponse near = radar.similar(fx.as(asker), new SimilarRequest(TITLE, BODY, null));
        assertThat(near.items()).hasSize(1);
        assertThat(near.items().get(0).doubtId()).isEqualTo(solved);
        assertThat(near.items().get(0).similarity()).isGreaterThanOrEqualTo(0.65);
        assertThat(near.items().get(0).acceptedAnswerExcerpt()).startsWith("Sorted inserts");
        assertThat(near.items().get(0).acceptedAnswerAuthor()).isEqualTo("Hari Helper");

        SimilarResponse unrelated = radar.similar(fx.as(asker), new SimilarRequest(
                "Subnet mask for a class C network with six subnets", "How many hosts fit in each subnet of the network?", null));
        assertThat(unrelated.items()).isEmpty();
    }

    @Test
    void doubtsSolvedByHintsAreNeverOfferedAsDuplicates() {
        User author = fx.tiet("Asha Learner");
        long doubt = fx.doubtAt(author, "DSA", "Trees & BST", TITLE, BODY, clock.instant().minus(Duration.ofDays(3)));
        jdbc.update("UPDATE doubts SET status = 'SOLVED', resolution = 'AI_HINT', solved_at = now() WHERE id = ?", doubt);
        User asker = fx.tiet("Nina Asker");

        assertThat(radar.similar(fx.as(asker), new SimilarRequest(TITLE, BODY, null)).items()).isEmpty();
    }

    @Test
    void openDoubtsAreNotOfferedEither() {
        User author = fx.tiet("Asha Learner");
        fx.doubt(author, "DSA", "Trees & BST", TITLE, BODY);
        User asker = fx.tiet("Nina Asker");

        assertThat(radar.similar(fx.as(asker), new SimilarRequest(TITLE, BODY, null)).items()).isEmpty();
    }
}
