package com.studyloop;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyloop.embedding.EmbeddingSpaceGuard;
import com.studyloop.support.IntegrationTestBase;
import com.studyloop.user.User;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EmbeddingSpaceGuardTest extends IntegrationTestBase {

    @Autowired
    EmbeddingSpaceGuard guard;

    @Test
    void vectorsFromAnotherModelAreReEmbeddedWithTheCurrentOneAndCurrentVectorsAreKept() {
        User author = fx.tiet("Asha Author");
        long current = fx.doubt(author, "DSA", "Trees & BST", "Why does my BST insert become O(n) on sorted input?",
                "I insert 1 to 1000 in order and the tree becomes a chain.");
        long foreign = fx.doubt(author, "OS", "Deadlocks", "Why does my producer consumer code deadlock?",
                "Two semaphores, waits ordered mutex then empty, and the program hangs.");
        String currentVector = vector(current);
        jdbc.update("UPDATE doubts SET embedding_model = 'some-older-model' WHERE id = ?",
                foreign);

        guard.run(null);

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT embedding_model, embedding IS NOT NULL AS has_vector FROM doubts WHERE id = ?", foreign);
        assertThat(row.get("embedding_model")).isEqualTo("fake-bow-768");
        assertThat(row.get("has_vector")).isEqualTo(true);
        assertThat(vector(current)).isEqualTo(currentVector);
        assertThat(guard.isReady()).isTrue();
    }

    @Test
    void doubtsWithoutAVectorAreEmbeddedAtStartup() {
        User author = fx.tiet("Asha Author");
        long id = fx.doubt(author, "CN", "Error Control & CRC", "CRC remainder for message 1101011011",
                "Generator is 10011; I keep getting a different remainder from the book.");
        jdbc.update("UPDATE doubts SET embedding = NULL, embedding_model = NULL WHERE id = ?", id);

        guard.run(null);

        assertThat(jdbc.queryForObject("SELECT embedding_model FROM doubts WHERE id = ?", String.class, id))
                .isEqualTo("fake-bow-768");
    }

    private String vector(long id) {
        return jdbc.queryForObject("SELECT embedding::text FROM doubts WHERE id = ?", String.class, id);
    }
}
