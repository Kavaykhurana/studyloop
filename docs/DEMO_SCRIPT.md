# Demo video script (3:00) and live pitch

Record on the deployed site, not localhost, so the video also proves the link works. Use 1920×1080 and a voice-over,
record each block separately, then cut. **Warm the server first**: open https://studyloop-bi2z.onrender.com/api/health and
wait for `"status":"UP"`.

Only quote survey numbers that are filled in `docs/SURVEY.md`. If the survey is not done, use the bracketed fallback
line and keep the story.

## Shot list

| Time | On screen | Voice-over |
|---|---|---|
| 0:00–0:15 | A batch WhatsApp group scrolling past an unanswered doubt (blur names), then the survey chart | "It's 11 PM before a DBMS mid-sem. Your doubt sinks in the batch group in minutes. In our survey of {n} TIET students, {x}% said their doubts rarely get answered within an hour." [No survey: "And the senior who solved this exact doubt last year? You can't find them."] |
| 0:15–0:30 | Landing page with the live stats strip, then click **Try as a learner** | "StudyLoop makes every doubt solved once, solved for everyone. No signup, one click." |
| 0:30–0:55 | **Ask a doubt**. Type the BCNF doubt below and click **Continue**. The radar shows the senior's solved BCNF doubt and its match percentage | "Before you post, the radar checks what seniors on your campus already solved. It's semantic search over accepted answers, not keyword matching." |
| 0:55–1:20 | Click **None of these, post my doubt**, then **Post doubt**. The detail page shows "We've notified k seniors" and the match cards; open **Why this match?** on Rohan | "If it's new, StudyLoop routes it to seniors by what they've actually solved, not what they claim, and shows why. Rohan has solved four similar Normalization doubts." |
| 1:20–1:45 | Unlock hint 1 (nudge), then hint 2 with the `From #…` citation chip; click the chip | "While you wait, the hint ladder makes you think first. Hints are grounded in your seniors' accepted answers, and they cite them." |
| 1:45–2:00 | Click **Switch to Rohan Mehta (helper)** in the banner. Open the bell, open the doubt, post an answer. Switch back, click **Accept answer**, rate 5 stars | "A senior answers, the learner accepts, and that answer now powers the next student's radar and hints. That's the loop." |
| 2:00–2:15 | **Pulse**: stat tiles, daily chart, confusion map. Click the Normalization bar, which opens the filtered feed. Keep the seeded-data note visible | "Campus Pulse shows faculty exactly where the batch is stuck this week." |
| 2:15–2:40 | README architecture diagram, then `/api/docs` (Swagger), then the green CI badge | "Java 21 and Spring Boot with pgvector on Postgres, Gemini with structured output and fallbacks, rotating refresh tokens with reuse detection, and 75 integration tests on a real database." |
| 2:40–3:00 | **Ask a doubt**, then the **Snap it** tab: photograph a handwritten CRC question and it gets transcribed. End card: live link and repo URL | "Even a photo of your notebook works. Try it now, no signup." |

### Text to type at 0:30

Title: `How do I check if a relation is in BCNF with two candidate keys?`

Body: `R(A,B,C,D) with FDs AB->C, C->A, AB->D. Is it in BCNF? How should I decompose it? Sir said check whether every left side is a superkey.`

### Before recording

- `/api/health` should show `"ai":"GEMINI"` (the live site runs on Gemini). Hints take 1 to 3 seconds, and Snap
  transcribes the photo in about 2 seconds.
- Reset the demo campus between takes so the counts are clean:

  ```bash
  curl -X POST https://studyloop-bi2z.onrender.com/api/admin/demo/reset -H "X-Admin-Token: $ADMIN_TOKEN"
  ```

  `ADMIN_TOKEN` is in the Render dashboard under the `studyloop` service, then **Environment**.
- Use a clean browser profile at 100% zoom, and hide the bookmarks bar.

## Three-minute live pitch (if a final round is announced)

1. **Hook (20 s).** "It's 11 PM, your DBMS doubt is stuck, and the batch group is asleep. StudyLoop already knows a
   senior who solved it."
2. **Problem (30 s).** WhatsApp groups bury doubts, the same doubts come back every semester, seniors are unreachable,
   and chatbots hand over answers instead of teaching. Add survey numbers only if they were collected.
3. **The loop (60 s, live).** Radar, then post, then routed seniors with reasons, then hint ladder with citations, then
   answer and accept. Every accepted answer makes the next doubt easier.
4. **Why it's hard (40 s).** Evidence-based matching instead of self-declared levels; hints that never leak the answer
   in rungs 1 to 3 and only cite real campus doubts; an idempotent reputation ledger with a race-safe accept; embedding
   versioning so vectors from different models are never compared; 75 integration tests on real Postgres.
5. **Impact (30 s).** Self-serve rate and time to first answer per campus, a confusion map for faculty, and new colleges
   join by email domain with zero setup.
