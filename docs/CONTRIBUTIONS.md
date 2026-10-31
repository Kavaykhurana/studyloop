# Team contributions

**Team AlphaFire** · NexHack 3.0 · Education and Student Innovation

The work was split by ownership of folders, so each area had one accountable owner and merge conflicts stayed rare. How
AI tools were used across every area is disclosed in [AI_USAGE.md](AI_USAGE.md).

## Summary

| Member | GitHub | Role | Owned modules |
|---|---|---|---|
| Kavay Khurana (team leader) | [@Kavaykhurana](https://github.com/Kavaykhurana) | Backend core and backend AI | `backend/`: `auth/ campus/ user/ doubt/ answer/ reputation/ notification/ common/ config/` and `ai/ embedding/ matching/ hint/ plan/ pulse/ seed/`; deployment |
| Gurliv Singh | — | Frontend | `frontend/` |
| Harshit Atal | — | Backend AI and frontend | `backend/`: `ai/ embedding/ matching/ hint/ plan/ pulse/ seed/`; `frontend/` |
| Tanveer Singh | — | Design, content and pitch | `docs/`, seed doubt content, survey, demo video, Unstop submission |

## Per member

### Kavay Khurana (team leader)

Owned the product plan and build specification, and the backend end to end. Backend core covers authentication (access
tokens and rotating refresh tokens with reuse detection), campus resolution by email domain, profiles and subjects,
doubts with the feed and search, answers with the race-safe accept, the idempotent reputation ledger and in-app
notifications. Backend AI covers the Gemini client with structured output and fallbacks, embeddings and the duplicate
radar, expertise matching, the hint ladder with campus grounding, the study planner, Campus Pulse and the seeded demo
campus. Also owned the deployment on Render with the Supabase database.

### Gurliv Singh

Owned the frontend: the React app that students and judges use. That covers the landing page with one-click demo
personas, sign-up and onboarding, the dashboard, the three-step Ask flow with the radar and Snap, the doubt page with
the hint ladder, matched seniors and answers, the feed, Campus Pulse, the study planner, notifications, profiles, and
the design system with its loading, empty and error states.

### Harshit Atal

Co-owned backend AI and the frontend: the AI-facing features from the model calls to the screens that present them,
including the duplicate radar, expertise matching with its "Why this match?" breakdown, the hint ladder, the study
planner and Campus Pulse.

### Tanveer Singh

Owned design, content and the pitch: the documentation in `docs/`, the seeded campus content (60 doubts and 75
answers), the campus survey, the demo video and the Unstop submission.
