CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE campuses (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    short_name    VARCHAR(20)  NOT NULL,
    email_domain  CITEXT UNIQUE,                       -- NULL = Open Campus (catch-all)
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    campus_id       BIGINT       NOT NULL REFERENCES campuses(id),
    name            VARCHAR(80)  NOT NULL CHECK (char_length(name) >= 2),
    email           CITEXT       NOT NULL UNIQUE,
    password_hash   VARCHAR(100),                      -- NULL only for demo personas
    role            VARCHAR(16)  NOT NULL DEFAULT 'STUDENT' CHECK (role IN ('STUDENT','ADMIN')),
    year_of_study   SMALLINT     CHECK (year_of_study BETWEEN 1 AND 5),
    branch          VARCHAR(40),
    bio             VARCHAR(280),
    reputation      INT          NOT NULL DEFAULT 0 CHECK (reputation >= 0),
    is_demo         BOOLEAN      NOT NULL DEFAULT false,
    onboarded       BOOLEAN      NOT NULL DEFAULT false,
    last_active_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT password_required CHECK (is_demo OR password_hash IS NOT NULL)
);

CREATE TABLE subjects (
    id         BIGSERIAL PRIMARY KEY,
    code       VARCHAR(12) NOT NULL UNIQUE,
    name       VARCHAR(80) NOT NULL,
    color_hex  CHAR(7)     NOT NULL CHECK (color_hex ~ '^#[0-9A-Fa-f]{6}$'),
    sort_order SMALLINT    NOT NULL
);

CREATE TABLE topics (
    id          BIGSERIAL PRIMARY KEY,
    subject_id  BIGINT      NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    name        VARCHAR(80) NOT NULL,
    sort_order  SMALLINT    NOT NULL,
    UNIQUE (subject_id, name)
);

-- A user rates a subject either as a strength or as needing help, never both.
CREATE TABLE user_subjects (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id  BIGINT      NOT NULL REFERENCES subjects(id),
    kind        VARCHAR(12) NOT NULL CHECK (kind IN ('STRONG','NEEDS_HELP')),
    level       SMALLINT    NOT NULL CHECK (level BETWEEN 1 AND 5),
    UNIQUE (user_id, subject_id)
);

CREATE TABLE doubts (
    id               BIGSERIAL PRIMARY KEY,
    campus_id        BIGINT       NOT NULL REFERENCES campuses(id),
    author_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id       BIGINT       NOT NULL REFERENCES subjects(id),
    topic_id         BIGINT       REFERENCES topics(id),
    title            VARCHAR(160) NOT NULL CHECK (char_length(title) >= 10),
    body_md          TEXT         NOT NULL CHECK (char_length(body_md) BETWEEN 20 AND 8000),
    difficulty       SMALLINT     CHECK (difficulty BETWEEN 1 AND 3),
    status           VARCHAR(10)  NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','SOLVED')),
    resolution       VARCHAR(12)  CHECK (resolution IN ('PEER_ANSWER','AI_HINT')),
    source           VARCHAR(8)   NOT NULL DEFAULT 'TEXT' CHECK (source IN ('TEXT','SNAP')),
    answer_count     INT          NOT NULL DEFAULT 0 CHECK (answer_count >= 0),
    view_count       INT          NOT NULL DEFAULT 0 CHECK (view_count >= 0),
    embedding        vector(768),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    first_answer_at  TIMESTAMPTZ,
    solved_at        TIMESTAMPTZ,
    CONSTRAINT solved_consistency CHECK (
        (status = 'OPEN'   AND resolution IS NULL     AND solved_at IS NULL) OR
        (status = 'SOLVED' AND resolution IS NOT NULL AND solved_at IS NOT NULL)
    )
);

CREATE TABLE answers (
    id              BIGSERIAL PRIMARY KEY,
    doubt_id        BIGINT      NOT NULL REFERENCES doubts(id) ON DELETE CASCADE,
    author_id       BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body_md         TEXT        NOT NULL CHECK (char_length(body_md) BETWEEN 20 AND 10000),
    is_accepted     BOOLEAN     NOT NULL DEFAULT false,
    helpful_rating  SMALLINT    CHECK (helpful_rating BETWEEN 1 AND 5),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at     TIMESTAMPTZ,
    CONSTRAINT accepted_consistency CHECK (is_accepted = (accepted_at IS NOT NULL))
);

CREATE TABLE doubt_matches (
    id          BIGSERIAL PRIMARY KEY,
    doubt_id    BIGINT      NOT NULL REFERENCES doubts(id) ON DELETE CASCADE,
    helper_id   BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rank        SMALLINT    NOT NULL CHECK (rank BETWEEN 1 AND 5),
    score       SMALLINT    NOT NULL CHECK (score BETWEEN 0 AND 100),
    breakdown   JSONB       NOT NULL,
    reasons     JSONB       NOT NULL,
    notified    BOOLEAN     NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (doubt_id, helper_id)
);

CREATE TABLE ai_hints (
    id               BIGSERIAL PRIMARY KEY,
    doubt_id         BIGINT      NOT NULL REFERENCES doubts(id) ON DELETE CASCADE,
    level            SMALLINT    NOT NULL CHECK (level BETWEEN 1 AND 4),
    content_md       TEXT        NOT NULL,
    cited_doubt_ids  BIGINT[]    NOT NULL DEFAULT '{}',
    confidence       VARCHAR(6)  NOT NULL CHECK (confidence IN ('HIGH','MEDIUM','LOW')),
    model            VARCHAR(60) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (doubt_id, level)
);

CREATE TABLE hint_progress (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    doubt_id        BIGINT      NOT NULL REFERENCES doubts(id) ON DELETE CASCADE,
    level_unlocked  SMALLINT    NOT NULL CHECK (level_unlocked BETWEEN 0 AND 4),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, doubt_id)
);

CREATE TABLE deflection_events (
    id               BIGSERIAL PRIMARY KEY,
    campus_id        BIGINT       NOT NULL REFERENCES campuses(id),
    user_id          BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source_doubt_id  BIGINT       NOT NULL REFERENCES doubts(id) ON DELETE CASCADE,
    query_text       VARCHAR(500) NOT NULL,
    similarity       REAL         NOT NULL CHECK (similarity BETWEEN 0 AND 1),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, source_doubt_id)
);

-- Append-only ledger. users.reputation is a denormalised sum updated in the same transaction.
CREATE TABLE reputation_events (
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id        BIGINT      NOT NULL REFERENCES subjects(id),
    delta             SMALLINT    NOT NULL CHECK (delta > 0),
    reason            VARCHAR(24) NOT NULL CHECK (reason IN ('ANSWER_ACCEPTED','FAST_ANSWER','HELPFUL_RATING','LOOP_CLOSED')),
    source_answer_id  BIGINT      NOT NULL REFERENCES answers(id) ON DELETE CASCADE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, reason, source_answer_id)
);

CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(20)  NOT NULL CHECK (type IN ('MATCHED','NEW_ANSWER','ANSWER_ACCEPTED')),
    doubt_id    BIGINT       NOT NULL REFERENCES doubts(id) ON DELETE CASCADE,
    actor_id    BIGINT       REFERENCES users(id) ON DELETE SET NULL,
    message     VARCHAR(200) NOT NULL,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE refresh_tokens (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      CHAR(64)    NOT NULL UNIQUE,      -- SHA-256 hex of the opaque token
    family_id       UUID        NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    replaced_by_id  BIGINT      REFERENCES refresh_tokens(id),
    user_agent      VARCHAR(200),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE study_plans (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_name      VARCHAR(100) NOT NULL,
    exam_date      DATE         NOT NULL,
    hours_per_day  NUMERIC(3,1) NOT NULL CHECK (hours_per_day BETWEEN 0.5 AND 12),
    summary        VARCHAR(400) NOT NULL,
    model          VARCHAR(60)  NOT NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE plan_tasks (
    id          BIGSERIAL PRIMARY KEY,
    plan_id     BIGINT       NOT NULL REFERENCES study_plans(id) ON DELETE CASCADE,
    day_index   SMALLINT     NOT NULL CHECK (day_index BETWEEN 0 AND 6),
    plan_date   DATE         NOT NULL,
    day_focus   VARCHAR(80)  NOT NULL,
    position    SMALLINT     NOT NULL,
    title       VARCHAR(140) NOT NULL,
    minutes     SMALLINT     NOT NULL CHECK (minutes BETWEEN 10 AND 240),
    kind        VARCHAR(10)  NOT NULL CHECK (kind IN ('LEARN','PRACTICE','REVISE','ASK_PEER')),
    subject_id  BIGINT       NOT NULL REFERENCES subjects(id),
    topic_id    BIGINT       REFERENCES topics(id),
    done_at     TIMESTAMPTZ,
    UNIQUE (plan_id, day_index, position)
);

CREATE TABLE ai_calls (
    id             BIGSERIAL PRIMARY KEY,
    feature        VARCHAR(16) NOT NULL CHECK (feature IN ('CLASSIFY','SNAP','HINT','PLAN','EMBED')),
    model          VARCHAR(60) NOT NULL,
    latency_ms     INT         NOT NULL,
    success        BOOLEAN     NOT NULL,
    error_code     VARCHAR(40),
    input_tokens   INT,
    output_tokens  INT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
