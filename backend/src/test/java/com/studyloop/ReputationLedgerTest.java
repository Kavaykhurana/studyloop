package com.studyloop;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyloop.reputation.ReputationReason;
import com.studyloop.reputation.ReputationService;
import com.studyloop.support.IntegrationTestBase;
import com.studyloop.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

class ReputationLedgerTest extends IntegrationTestBase {

    @Autowired
    ReputationService reputation;
    @Autowired
    TransactionTemplate tx;

    @Test
    void awardingTheSameUserReasonAndAnswerTwiceChangesReputationOnce() {
        User author = fx.tiet("Asha Learner");
        User helper = fx.strongHelper("Hari Helper", "DSA", 4);
        long doubt = fx.doubt(author, "DSA", "Trees & BST", "Ledger idempotency check doubt", "A body that is long enough.");
        long answer = fx.answer(doubt, helper, "An answer body that is long enough to store.", clock.instant());
        long subject = fx.subjectId("DSA");

        int first = tx.execute(s -> reputation.award(helper.getId(), subject, ReputationReason.ANSWER_ACCEPTED, answer));
        int second = tx.execute(s -> reputation.award(helper.getId(), subject, ReputationReason.ANSWER_ACCEPTED, answer));

        assertThat(first).isEqualTo(15);
        assertThat(second).isZero();
        assertThat(fx.reputation(helper)).isEqualTo(15);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reputation_events WHERE user_id = ?", Integer.class,
                helper.getId())).isEqualTo(1);
    }

    @Test
    void differentReasonsForTheSameAnswerAreDistinctLedgerRows() {
        User author = fx.tiet("Asha Learner");
        User helper = fx.strongHelper("Hari Helper", "DSA", 4);
        long doubt = fx.doubt(author, "DSA", "Trees & BST", "Ledger reasons check doubt", "A body that is long enough.");
        long answer = fx.answer(doubt, helper, "An answer body that is long enough to store.", clock.instant());
        long subject = fx.subjectId("DSA");

        tx.executeWithoutResult(s -> {
            reputation.award(helper.getId(), subject, ReputationReason.ANSWER_ACCEPTED, answer);
            reputation.award(helper.getId(), subject, ReputationReason.FAST_ANSWER, answer);
            reputation.award(helper.getId(), subject, ReputationReason.HELPFUL_RATING, answer);
        });

        assertThat(fx.reputation(helper)).isEqualTo(25);
    }
}
