package com.studyloop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studyloop.support.Fixtures;
import com.studyloop.support.IntegrationTestBase;
import com.studyloop.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CampusIsolationTest extends IntegrationTestBase {

    @Test
    void anotherCampusNeverSeesOrTouchesTietContent() throws Exception {
        User tietAuthor = fx.tiet("Tiet Author");
        User tietHelper = fx.strongHelper("Tiet Helper", "DSA", 4);
        User outsider = fx.user("Open Outsider", Fixtures.OPEN_CAMPUS, 2);
        String title = "Why does my BST insert become O(n) on sorted input?";
        String body = "Inserting 1 to 1000 in order makes my binary search tree behave like a linked list.";
        long solved = fx.solvedDoubt(tietAuthor, tietHelper, "DSA", "Trees & BST", title, body,
                "A sorted insert always goes right so the tree becomes a chain of height n.");
        long open = fx.doubt(tietAuthor, "DSA", "Trees & BST", "Another open doubt about trees", body);
        String auth = fx.bearer(outsider);

        mvc.perform(get("/api/doubts/" + solved).header("Authorization", auth)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(get("/api/doubts").header("Authorization", auth)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(post("/api/doubts/similar").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"bodyMd\":\"" + body + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(post("/api/doubts/" + open + "/answers").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"bodyMd\":\"An answer from outside the campus that is long enough.\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/doubts/" + open + "/hints/next").header("Authorization", auth))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/users/" + tietHelper.getId()).header("Authorization", auth))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM answers WHERE author_id = ?", Integer.class,
                outsider.getId())).isZero();
    }

    @Test
    void sameCampusUserCanSeeTheDoubt() throws Exception {
        User author = fx.tiet("Tiet Author");
        User peer = fx.tiet("Tiet Peer");
        long id = fx.doubt(author, "DSA", "Trees & BST", "Visible doubt for campus peers",
                "A body that is long enough to be valid for this test case.");
        mvc.perform(get("/api/doubts/" + id).header("Authorization", fx.bearer(peer))).andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.canAnswer").value(true));
    }
}
