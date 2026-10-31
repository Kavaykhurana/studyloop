# AI usage disclosure

NexHack 3.0 allows AI tools and asks for significant use to be disclosed. This file is that disclosure.

## Tools used

**To build the project**

- Claude Code (Anthropic), running Claude Sonnet 5.5, generated the backend, frontend, tests, configuration, seed
  content and documentation from the team's build specification (`STUDYLOOP_BUILD_PROMPT`) and plan.

- Claude Code (Anthropic) was also used for a post-build audit and deployment session on 31 Oct 2026 (see the "Audit"
  row of the build log): it ran the full test suite and a scripted browser walkthrough of the demo path, fixed the bugs
  that surfaced, deployed the app to Render with a Supabase database, and updated these docs.

**Inside the product (Gemini models, all configured by environment variable)**

| Role | Model (default) | Used for |
|---|---|---|
| Chat | `gemini-3.5-flash` | Hint ladder rungs, "Snap a doubt" (vision), study planner |
| Fast / fallback | `gemini-3.1-flash-lite` | Doubt classification; fallback when the chat model is rate-limited |
| Embeddings | `gemini-embedding-2` (768 dimensions) | Duplicate radar, expertise routing, hint grounding |

Without `GEMINI_API_KEY` the app runs with deterministic fake clients (development and tests only). Uploaded photos
are sent to Gemini for transcription and are never stored by StudyLoop.

## What AI generated

Almost all source code in `backend/` and `frontend/`, the SQL migrations, the test suites, the Docker, Render, Vercel
and GitHub Actions configuration, the demo seed content (60 doubts and 75 answers, including the worked numerical
examples, which were computed with scripts rather than by hand), and the documentation in this repository.

## What humans designed and decided

The product concept and the rubric mapping, the loop (radar, hint ladder, routing, accepted answers feeding the next
student), the matching formula and its weights, the database schema and its invariants, the security model, the
Gemini prompts and JSON schemas, the UI copy and design tokens, and the choice of stack. These come from the team's
plan and build specification; the AI implemented them.

## How the generated work was checked

- Backend: 72 automated tests (`./mvnw verify`) against a real PostgreSQL with pgvector, including the concurrent
  accept race, refresh-token reuse detection and its concurrency grace, campus isolation, embedding-space re-embedding
  and the Gemini client's retry and fallback policy (run against a local stand-in server).
- Frontend: `npm run typecheck`, `npm run lint` (zero warnings) and `npm run build`.
- The core loop was clicked through in a browser at desktop and 375 px widths, then re-run as a scripted Playwright
  walkthrough (post, radar, hints, switch persona, answer, accept, rate, Pulse, plan, sign-up on both campuses, rapid
  reloads) with zero console errors, locally and against the deployed site.

**Not verified against the live Gemini API** (no key was available during the build): real model output quality, the
calibration of the similarity thresholds, and photo transcription. See `docs/ARCHITECTURE.md` section 8.

## Build log

One line per phase. "Human review" is for the team to complete as they review each area; it is intentionally left for
real names and real changes rather than filled in by the tool.

| Phase | What the AI coding tool generated | Human review |
|---|---|---|
| P0 | Spring Boot 4.1.2 scaffold, Flyway V1 to V3, error model with trace ids, `/api/health`, Vite + Tailwind v4 shell, design tokens | _team to record_ |
| P1 | Auth (access JWT, rotating refresh with reuse detection, demo login), campus resolution, profile and subjects, rate limiter; auth UI, onboarding, route guards | _team to record_ |
| P2 | Doubts, feed with filters and search, answers, accept with pessimistic lock, idempotent reputation ledger, notifications with polling; feed, detail, ask and bell UI | _team to record_ |
| P3 | `LlmClient` / `EmbeddingClient` with Gemini and fake implementations, classification, embeddings and backfill, duplicate radar, deflections, snap; radar and snap UI | _team to record_ |
| P4 | Hybrid peer matching with reasons, re-match job, hint ladder with grounding and citation validation, resolve-by-hint; hint ladder and match UI | _team to record_ |
| P5 | Campus Pulse SQL and public stats, study planner with output validation; Pulse, Plan and dashboard UI | _team to record_ |
| P6 | 60 doubts and 75 answers of seed content, demo seeder (ledger-derived reputation), admin reset endpoint | _team to record_ |
| P7 | Error code mapping, input hygiene, loading, empty and error states, 375 px pass, accessibility pass | _team to record_ |
| P8 | `ci.yml`, `keepalive.yml`, Dockerfile, `render.yaml`, `vercel.json`, README and the docs in this folder | _team to record_ |
| P9 | End-to-end verification run on a local stack; fixes found during it | _team to record_ |
| Audit | Fixes from a full audit: out-of-sync `package-lock.json` that broke `npm ci` in CI; Swagger UI at `/api/docs` returning 401; quick reloads logging users out (15 s refresh reuse grace); vectors from different embedding models being compared after a key is added (V4 `embedding_model` + startup re-embed); Gemini model ids resolved against the live list; demo-ready offline AI (keyword classifier, cited hints, separate thresholds); health gated on seeding; sign-out redirect race; router hydrate warning. Single-service Docker image (Spring Boot serves the SPA), Render + Supabase deployment, docs, submission pack and demo script | _team to record_ |
