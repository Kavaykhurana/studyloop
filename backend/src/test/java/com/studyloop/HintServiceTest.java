package com.studyloop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyloop.ai.AiRequest;
import com.studyloop.ai.AiResult;
import com.studyloop.ai.AiUnavailableException;
import com.studyloop.ai.LlmClient;
import com.studyloop.ai.dto.HintResult;
import com.studyloop.common.error.ApiException;
import com.studyloop.common.error.ErrorCode;
import com.studyloop.hint.HintDao;
import com.studyloop.hint.HintService;
import com.studyloop.hint.dto.HintNextResponse;
import com.studyloop.support.IntegrationTestBase;
import com.studyloop.user.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class HintServiceTest extends IntegrationTestBase {

    private static final String TITLE = "Why does my BST insert become O(n) on sorted input?";
    private static final String BODY = "I insert 1 to 1000 in order into my binary search tree and the tree becomes "
            + "a chain, so every insert walks the whole height of the tree.";

    @MockitoBean
    LlmClient llm;
    @Autowired
    HintService hints;
    @Autowired
    HintDao hintDao;

    private void llmReturns(HintResult result) {
        when(llm.generateJson(any(AiRequest.class), eq(HintResult.class)))
                .thenReturn(new AiResult<>(result, "test-model", 12));
    }

    @Test
    void levelAdvancesPerUserAndTheSecondViewerReadsTheCache() {
        User author = fx.tiet("Asha Author");
        User peer = fx.tiet("Pia Peer");
        long doubt = fx.doubt(author, "DSA", "Trees & BST", TITLE, BODY);
        llmReturns(new HintResult("**Hint** Think about the shape of the tree.", List.of(), "HIGH"));

        HintNextResponse first = hints.next(fx.as(author), doubt);
        assertThat(first.levelUnlocked()).isEqualTo(1);
        assertThat(first.fromCache()).isFalse();

        HintNextResponse cached = hints.next(fx.as(peer), doubt);
        assertThat(cached.levelUnlocked()).isEqualTo(1);
        assertThat(cached.fromCache()).isTrue();
        verify(llm, times(1)).generateJson(any(AiRequest.class), eq(HintResult.class));

        HintNextResponse second = hints.next(fx.as(author), doubt);
        assertThat(second.levelUnlocked()).isEqualTo(2);
        assertThat(second.fromCache()).isFalse();
        verify(llm, times(2)).generateJson(any(AiRequest.class), eq(HintResult.class));
    }

    @Test
    void citationsOfIdsThatWereNotProvidedAreDroppedAndTheirMarkersRemoved() {
        User author = fx.tiet("Asha Author");
        User helper = fx.strongHelper("Hari Helper", "DSA", 4);
        long grounding = fx.solvedDoubt(fx.tiet("Earlier Asker"), helper, "DSA", "Trees & BST", TITLE + " (earlier)",
                BODY, "The tree degenerates into a chain because sorted keys always go right.");
        long doubt = fx.doubt(author, "DSA", "Trees & BST", TITLE, BODY);
        llmReturns(new HintResult("Compare with [#" + grounding + "] and also [#99999] for the idea.",
                List.of(grounding, 99999L), "HIGH"));

        HintNextResponse response = hints.next(fx.as(author), doubt);

        assertThat(response.hint().citedDoubts()).extracting(c -> c.id()).containsExactly(grounding);
        assertThat(response.hint().contentMd()).contains("[#" + grounding + "]").doesNotContain("99999");
    }

    @Test
    void whenTheModelIsUnavailableNothingIsStoredAndTheLevelDoesNotAdvance() {
        User author = fx.tiet("Asha Author");
        long doubt = fx.doubt(author, "DSA", "Trees & BST", TITLE, BODY);
        when(llm.generateJson(any(AiRequest.class), eq(HintResult.class)))
                .thenThrow(new AiUnavailableException("TIMEOUT"));

        assertThatThrownBy(() -> hints.next(fx.as(author), doubt)).isInstanceOfSatisfying(ApiException.class, e -> {
            assertThat(e.code()).isEqualTo(ErrorCode.AI_UNAVAILABLE);
            assertThat(e.getMessage()).contains("hint tutor is busy");
        });
        assertThat(hintDao.levelUnlocked(author.getId(), doubt)).isZero();
        assertThat(hintDao.find(doubt, 1)).isEmpty();
    }

    @Test
    void afterFourRungsNoMoreCanBeUnlocked() {
        User author = fx.tiet("Asha Author");
        long doubt = fx.doubt(author, "DSA", "Trees & BST", TITLE, BODY);
        llmReturns(new HintResult("A rung of the ladder.", List.of(), "MEDIUM"));
        for (int i = 0; i < 4; i++) {
            hints.next(fx.as(author), doubt);
        }
        assertThatThrownBy(() -> hints.next(fx.as(author), doubt)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.ALL_HINTS_UNLOCKED));
    }
}
