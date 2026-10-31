package com.studyloop;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyloop.common.web.PageResponse;
import com.studyloop.doubt.DoubtQueryService;
import com.studyloop.doubt.dto.DoubtSummary;
import com.studyloop.support.IntegrationTestBase;
import com.studyloop.user.User;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FeedQueryTest extends IntegrationTestBase {

    @Autowired
    DoubtQueryService queries;
    @Autowired
    EntityManagerFactory emf;

    @Test
    void aFeedOfTwentyDoubtsRunsAtMostThreeStatements() {
        User viewer = fx.tiet("Vera Viewer");
        for (int i = 0; i < 20; i++) {
            User author = fx.tiet("Author Number " + i);
            fx.doubt(author, "DSA", "Trees & BST", "Feed doubt number " + i + " about binary trees",
                    "A body that is long enough to be stored for the feed query test " + i);
        }
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        PageResponse<DoubtSummary> page = queries.feed(fx.as(viewer),
                new DoubtQueryService.FeedFilter(null, null, null, false, false, null), 0, 20);

        assertThat(page.items()).hasSize(20);
        assertThat(page.totalItems()).isEqualTo(20);
        assertThat(page.items()).allSatisfy(d -> assertThat(d.author().name()).startsWith("Author Number"));
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    void searchEscapesLikeWildcards() {
        User viewer = fx.tiet("Vera Viewer");
        fx.doubt(viewer, "DSA", "Trees & BST", "How much is 100% of a tree height", "A body that is long enough here.");
        fx.doubt(viewer, "DSA", "Trees & BST", "Plain title about linked lists here", "A body that is long enough here.");

        PageResponse<DoubtSummary> percent = queries.feed(fx.as(viewer),
                new DoubtQueryService.FeedFilter(null, null, null, false, false, "%"), 0, 20);
        assertThat(percent.totalItems()).isEqualTo(1);
    }
}
