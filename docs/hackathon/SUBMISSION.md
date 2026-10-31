# NexHack 3.0 submission pack

Paste-ready text for every Unstop field for **Team AlphaFire**. Everything below is final.

## Project name

StudyLoop

## Track

Education and Student Innovation

## Problem statement

Engineering students get stuck on doubts late at night before mid-sems and lab evaluations. Batch WhatsApp groups bury
a question within minutes, the same doubts are asked every semester with nothing searchable, seniors who have already
solved them are hard to reach, and general chatbots hand over final answers instead of teaching. The knowledge seniors
shared once is lost every year.

## Proposed solution

StudyLoop is a campus knowledge loop. Before a student posts, a duplicate radar semantically searches doubts already
solved on their campus and shows the accepted senior answer. If nothing fits, the doubt is posted and a four-rung AI hint
ladder (nudge, approach, first step, full solution) makes the student think before seeing the answer; the hints are
grounded in seniors' accepted answers and cite them. At the same time, smart routing ranks seniors by what they have
actually solved, not just what they claim, and shows the reasons. When a senior's answer is accepted, reputation is
awarded through an idempotent ledger and the answer joins the campus knowledge base that powers the next student's radar
and hints. Campus Pulse shows faculty and class representatives which topics the batch is stuck on this week.

## Description and key features

- **Duplicate radar:** semantic search (pgvector, 768-d Gemini embeddings) over doubts already solved on the same campus;
  "This solved my doubt" is logged as a deflection.
- **Socratic hint ladder:** four rungs: Nudge, Approach, First step, Full solution. Rungs 1 to 3 never contain the
  answer, every rung is grounded in seniors' accepted answers with validated `[#id]` citations, and rungs are cached per
  doubt.
- **Explainable expertise routing:** hybrid score from semantic similarity to the helper's accepted answers (40%),
  declared strength, subject reputation and recent activity (20% each), minus current load. The top three are notified,
  and every match shows its reasons and a score breakdown.
- **Snap a doubt:** photograph a notebook page; Gemini vision transcribes it into an editable doubt, and the image is
  never stored.
- **Reputation ledger:** append-only and idempotent, protected by a pessimistic lock on accept and a partial unique index.
- **In-app notifications:** 15-second polling.
- **Campus Pulse:** doubts posted, solved rate, share solved without waiting for a peer, median time to first answer,
  a confusion map by subject and topic, top helpers and AI health.
- **AI study planner:** a dated plan to an exam built from weak subjects and the student's own open doubts.
- **One-click demo:** "Try as a learner" and "Try as a helper" personas on a seeded campus (25 users, 60 doubts, 75
  answers), labelled as seeded data everywhere.
- **Multi-campus by email domain:** content never crosses campuses.

## Technology stack

Java 21, Spring Boot 4.1, Spring Security 7 (JWT access tokens and rotating refresh tokens with reuse detection),
Spring Data JPA (Hibernate 7), Flyway, PostgreSQL with pgvector (HNSW) and citext (Supabase), Gemini API (3.5 Flash-Lite, 3 Flash and
Embedding 2 via RestClient with structured JSON output, per-tier thinking levels and cross-model fallback), React 19, TypeScript, Vite, Tailwind CSS v4, TanStack
Query, React Router 7, Recharts, JUnit 5 with Testcontainers (75 tests), GitHub Actions, Docker on Render.

## GitHub repository

https://github.com/Kavaykhurana/studyloop

The README has the setup instructions and tech stack. AI usage is disclosed in `docs/AI_USAGE.md` and each member's
contribution is in `docs/CONTRIBUTIONS.md`.

## Demo

- Live: https://studyloop-bi2z.onrender.com. Click "Try as a learner"; no signup needed. On free hosting the first load
  can take up to two minutes while the server wakes.
- API docs: https://studyloop-bi2z.onrender.com/api/docs
- Video (3 minutes, narrated): the unlisted YouTube URL of the narrated demo

## Team contributions

- **Kavay Khurana (team leader):** Backend core and backend AI: product plan and build spec; auth, doubts, answers,
  reputation ledger and notifications; Gemini layer, duplicate radar, expertise matching, hint ladder, study planner,
  Campus Pulse and the seeded campus; deployment on Render with Supabase.
- **Gurliv Singh:** Frontend: the React app: landing page and demo personas, Ask flow with radar and Snap, doubt page
  with hint ladder and matched seniors, feed, Pulse, planner and the design system.
- **Harshit Atal:** Backend AI and frontend: the AI features end to end: duplicate radar, matching with its "Why this
  match?" breakdown, hint ladder, study planner and Campus Pulse.
- **Tanveer Singh:** Design, content and pitch: documentation, seeded campus content (60 doubts, 75 answers), demo
  video and the Unstop submission.

## Requirement checklist

Every item NexHack 3.0 asks for, and where it lives:

| Unstop requirement | Where |
|---|---|
| Project name and selected track | This file, top: StudyLoop, Education and Student Innovation |
| Problem statement and proposed solution | This file; README "The problem" and "How it works" |
| Project description and key features | This file; README "Features" (with how each is built) |
| GitHub repository | https://github.com/Kavaykhurana/studyloop |
| README with setup instructions and technology stack | README "Getting started", "Configuration", "Tech stack" |
| Demo video or working demo link | Both. Live: https://studyloop-bi2z.onrender.com (one-click demo). Video: YouTube link |
| Each team member's contribution | This file, "Team contributions"; `docs/CONTRIBUTIONS.md` |

| Rule | How the project complies |
|---|---|
| Libraries, APIs, frameworks and open-source components used with appropriate attribution | `docs/ATTRIBUTION.md`, linked from the README |
| Significant AI assistance disclosed | `docs/AI_USAGE.md`, linked from the README |
| No misrepresentation | Demo data is labelled as seeded on every metric screen; no invented statistics; the narrated video discloses its AI voice |

## Before pressing submit

1. Open the live link in an incognito window on mobile data. Wait for the landing page, then click "Try as a learner"
   and check that the dashboard loads.
2. Open `/api/docs` and `/api/health` (it should say `"status":"UP"`).
3. Open the video link in a logged-out window and check that it plays.
4. Check that the repository is public, the default branch has the latest code, and the CI badge is green.
5. Take a screenshot of the Unstop confirmation.
