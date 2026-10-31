package com.studyloop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studyloop.answer.AnswerService;
import com.studyloop.answer.dto.AcceptResult;
import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.support.IntegrationTestBase;
import com.studyloop.user.User;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AcceptAnswerIntegrationTest extends IntegrationTestBase {

    private static final String BODY = "A long enough body for the doubt used in this accept answer test.";
    private static final String ANSWER = "An answer that is comfortably long enough to be accepted by validation.";

    @Autowired
    AnswerService answers;

    @Test
    void acceptingAFastAnswerAwardsHelperAndAuthorAndClosesTheDoubt() {
        User author = fx.tiet("Asha Learner");
        User helper = fx.strongHelper("Hari Helper", "DSA", 4);
        long doubt = fx.doubt(author, "DSA", "Trees & BST", "Why is my BST slow on sorted input?", BODY);
        clock.advance(Duration.ofMinutes(10));
        long answerId = answers.post(fx.as(helper), doubt, ANSWER).id();

        AcceptResult result = answers.accept(fx.as(author), answerId);

        assertThat(result.awarded().helper()).isEqualTo(20);
        assertThat(result.awarded().author()).isEqualTo(2);
        assertThat(fx.reputation(helper)).isEqualTo(20);
        assertThat(fx.reputation(author)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT status || '/' || resolution FROM doubts WHERE id = ?", String.class,
                doubt)).isEqualTo("SOLVED/PEER_ANSWER");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'ANSWER_ACCEPTED'",
                Integer.class, helper.getId())).isEqualTo(1);
    }

    @Test
    void aSlowAnswerEarnsNoFastBonus() {
        User author = fx.tiet("Asha Learner");
        User helper = fx.strongHelper("Hari Helper", "DSA", 4);
        long doubt = fx.doubt(author, "DSA", "Trees & BST", "Why is my BST slow on sorted input?", BODY);
        clock.advance(Duration.ofMinutes(45));
        long answerId = answers.post(fx.as(helper), doubt, ANSWER).id();

        assertThat(answers.accept(fx.as(author), answerId).awarded().helper()).isEqualTo(15);
    }

    @Test
    void secondAcceptAndNonAuthorAcceptAreRejected() {
        User author = fx.tiet("Asha Learner");
        User helper = fx.strongHelper("Hari Helper", "DSA", 4);
        User other = fx.strongHelper("Ola Other", "DSA", 3);
        long doubt = fx.doubt(author, "DSA", "Trees & BST", "Why is my BST slow on sorted input?", BODY);
        long first = answers.post(fx.as(helper), doubt, ANSWER).id();
        long second = answers.post(fx.as(other), doubt, ANSWER + " Second answer.").id();

        assertThatThrownBy(() -> answers.accept(fx.as(other), first)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.NOT_DOUBT_OWNER));
        answers.accept(fx.as(author), first);
        assertThatThrownBy(() -> answers.accept(fx.as(author), second)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.DOUBT_SOLVED));
    }

    @Test
    void authorCannotAnswerTheirOwnDoubt() {
        User author = fx.tiet("Asha Learner");
        long doubt = fx.doubt(author, "DSA", "Trees & BST", "Why is my BST slow on sorted input?", BODY);
        assertThatThrownBy(() -> answers.post(fx.as(author), doubt, ANSWER)).isInstanceOfSatisfying(
                ApiException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.CANNOT_ANSWER_OWN));
    }

    @Test
    void twoConcurrentAcceptsOfDifferentAnswersLetExactlyOneWin() throws Exception {
        User author = fx.tiet("Asha Learner");
        User h1 = fx.strongHelper("Hari Helper", "DSA", 4);
        User h2 = fx.strongHelper("Ola Other", "DSA", 3);
        long doubt = fx.doubt(author, "DSA", "Trees & BST", "Why is my BST slow on sorted input?", BODY);
        long a1 = answers.post(fx.as(h1), doubt, ANSWER).id();
        long a2 = answers.post(fx.as(h2), doubt, ANSWER + " Another view.").id();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (long answerId : new long[] {a1, a2}) {
            results.add(pool.submit(() -> {
                ready.countDown();
                go.await();
                try {
                    answers.accept(fx.as(author), answerId);
                    return true;
                } catch (ApiException e) {
                    assertThat(e.code()).isIn(ErrorCode.DOUBT_SOLVED, ErrorCode.ALREADY_ACCEPTED);
                    return false;
                }
            }));
        }
        ready.await();
        go.countDown();
        int wins = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) {
                wins++;
            }
        }
        pool.shutdown();

        assertThat(wins).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM answers WHERE doubt_id = ? AND is_accepted",
                Integer.class, doubt)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reputation_events WHERE reason = 'ANSWER_ACCEPTED'",
                Integer.class)).isEqualTo(1);
        Integer ledger = jdbc.queryForObject("SELECT COALESCE(SUM(delta), 0) FROM reputation_events", Integer.class);
        Integer stored = jdbc.queryForObject("SELECT COALESCE(SUM(reputation), 0) FROM users", Integer.class);
        assertThat(stored).isEqualTo(ledger);
    }
}
