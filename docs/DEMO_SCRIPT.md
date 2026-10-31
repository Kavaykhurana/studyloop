# Demo video: narration script

One presenter, one take per scene, recorded on the live site while they talk. The full version runs about **3:20**; the
**3:00 cut** is at the end. Every on-screen moment below was rehearsed on the live stack with Gemini: the match
percentages, labels, notification texts and reputation numbers are what actually appears.

- Live site: https://studyloop-bi2z.onrender.com
- Personas: **Aanya Sharma** (learner, 2nd year) and **Rohan Mehta** (helper, final year), switched from the banner
- Speaking pace: slow and clear, about 140 words a minute. Pause on every click.

## 1. Before you record

1. **Warm the server.** Open https://studyloop-bi2z.onrender.com/api/health and wait until it shows
   `"status":"UP"` and `"ai":"GEMINI"`. If the server was asleep this takes up to two minutes.
2. **Reset the demo campus before every take.** Otherwise the doubt you posted in the last take shows up on the radar as
   already solved, and the counts drift. `ADMIN_TOKEN` is in Render: `studyloop` → **Environment** → `ADMIN_TOKEN`.

   macOS / Linux / Git Bash:

   ```bash
   curl -X POST https://studyloop-bi2z.onrender.com/api/admin/demo/reset -H "X-Admin-Token: $ADMIN_TOKEN"
   ```

   Windows PowerShell:

   ```powershell
   Invoke-RestMethod -Method Post -Uri https://studyloop-bi2z.onrender.com/api/admin/demo/reset -Headers @{ "X-Admin-Token" = $env:ADMIN_TOKEN }
   ```

   Wait 30 seconds, then reload `/api/health` until it says `UP` again.
3. **Browser.** Chrome incognito window (no extensions), 1920×1080, zoom 110%, bookmarks bar hidden. Turn on Do Not
   Disturb / Focus Assist so no pop-ups land in the recording.
4. **Have ready on the desktop:** a photo of the handwritten question in section 2 (JPG or PNG, under 4 MB), and this file
   open so you can copy-paste the texts below instead of typing them on camera.
5. **Audio.** Quiet room, earphone or phone mic 15 to 20 cm from your mouth. Record 10 seconds and listen back first.
6. **Recorder.** OBS Studio (free), Windows Game Bar (Win + G) or macOS QuickTime. Record each scene as its own clip,
   so a mistake costs one scene, not the whole video. In editing, trim the waits (server wake-up, typing) but keep one
   or two seconds of the AI loading, so viewers see it is live.
7. Do one full dry run without recording.

## 2. Paste-ready texts

**Doubt 1** (scene 3; the radar finds it at about 95%)

- Title: `How do I check if a relation is in BCNF with two candidate keys?`
- Body: `R(A,B,C,D) with FDs AB->C, C->A, AB->D. Is it in BCNF? How should I decompose it?`

**Doubt 2** (scene 4; the radar shows related answers at about 84 to 91%, then you post it)

- Title: `Is my BCNF decomposition lossless and dependency preserving?`
- Body: `FDs AB->C, C->A, AB->D, so the keys are AB and BC. I decomposed R(A,B,C,D) into R1(C,A) and R2(B,C,D). In the viva sir asked whether this is lossless and whether all the FDs are preserved. I think AB->C is lost, but I am not sure how to check it properly.`

**Rohan's answer** (scene 6; correct and complete, paste as is)

```
Lossless: yes. R1 and R2 share only C, and C -> A makes C a key of R1(C, A), so joining them back can never create extra rows.

Dependency preserving: no. Project the FDs onto each table: R1 keeps C -> A, and R2 keeps BC -> D (BC is a key of R). From {C -> A, BC -> D} you cannot derive AB -> C, because AB+ is just {A, B}. So AB -> C (and AB -> D) are lost.

This is the classic BCNF trade-off: a lossless BCNF decomposition always exists, but it does not always preserve every dependency. If you must keep them all, stop at 3NF.
```

**Handwritten question for Snap** (write it on a notebook page, then photograph it flat, in good light)

```
CN Tutorial - Q3
Find the CRC remainder for the message 1101011011 using the
generator polynomial 10011 (x^4 + x + 1). Also write the transmitted frame.
My attempt: appended 0000 -> 11010110110000, did XOR division,
got remainder 1011 ?? Book says 1110. Where am I wrong?
```

**Study plan** (scene 8): exam name `DBMS Mid-Sem`, exam date five days from the recording day, 2 hours a day; DBMS
and OS are already selected.

## 3. The script

Each scene: what to do on screen, then what to say. **Bold** words carry the stress. ✂ marks lines the 3:00 cut drops.

### Scene 1: The problem (0:00 to 0:13)

**Do:** Start on the landing page. Optional: open on two seconds of a blurred batch WhatsApp group.

> It's eleven at night, the day before a **DBMS mid-sem**. My doubt in the batch WhatsApp group gets **buried in two
> minutes**, even though a senior solved this **exact doubt** last year.

If your survey is done, add one sentence here with the real figures from `docs/SURVEY.md`, and only then.

### Scene 2: StudyLoop (0:13 to 0:30)

**Do:** Let the landing page sit for a beat (the live stats strip is visible). Click **Try as a learner**. On the
dashboard, point at the banner at the top ("You're exploring a demo campus as Aanya Sharma with seeded data").

> This is **StudyLoop**. It turns every doubt solved on campus into knowledge for the **next student**. Students join
> with their **college email**, which puts them on their own campus. You don't even need that: **one click**, and I'm
> Aanya, a second-year on our **seeded demo campus**.

### Scene 3: Duplicate radar (0:30 to 0:55)

**Do:** Click **Ask a doubt**. Paste doubt 1 (title, then body). Leave the subject on **Let StudyLoop decide**. Click
**Continue**. When "Already solved on campus" appears, point at the first card's match percentage and "Answered by Rohan
Mehta". Click **This solved my doubt**. The screen says "Glad it helped. That's one less doubt in the queue."

> I ask: how do I check **BCNF** with two candidate keys? Before anything is posted, the **duplicate radar** searches
> doubts my seniors have already solved, by **meaning**, not keywords. **Ninety-five percent** match, answered by
> Rohan, a final-year student. That's my doubt. **This solved my doubt**, and nobody had to wait.

Say the percentage you actually see if it differs.

### Scene 4: A new doubt gets routed (0:55 to 1:25)

**Do:** Click **Ask something else**. Paste doubt 2. Click **Continue**. The radar shows related BCNF answers: point at
them for a second. Click **None of these — post my doubt**, then **Post doubt**. On the doubt page, point at
"Posted. We've notified 3 seniors…" and at the **DBMS · Normalization** tag. In **Matched seniors**, point at Rohan's
first line, "Solved 4 similar Normalization doubts". Click **Why this match?** under Rohan.

> Now a harder one: is my decomposition **lossless**, and does it **preserve the dependencies**? The radar finds
> related answers, but not this question, so I post it. StudyLoop files it under **DBMS, Normalization**, and has
> already **notified three seniors**. Rohan comes first, not because he **claims** DBMS, but because he has
> **actually solved** four similar doubts. ✂ **Why this match** shows the exact score: proof, self-rating, reputation,
> recent activity, and how busy he already is.

### Scene 5: The hint ladder (1:25 to 1:55)

**Do:** Scroll to **Hint ladder**. Click **Unlock hint 1** and let the nudge appear (one to two seconds). Click
**Unlock hint 2**. Point at the orange citation chip ("From #" and a number; the number changes after every reset).
Don't click it: it opens the senior's doubt and leaves the page.
Point at the locked rungs 3 and 4, then at **Hints solved it — close my doubt** (don't click).

> While I wait, I'm **not spoon-fed**. Hint one is just a **nudge**. Hint two is the **approach**, with a common
> pitfall. And this little chip is a **citation**: the hint is grounded in a senior's **accepted answer** from my own
> campus, and links to it. The **full solution** waits at hint four, only if I ask. And if the hints are enough, I close
> the doubt **myself**.

### Scene 6: The senior answers, the loop closes (1:55 to 2:30)

**Do:** In the banner, click **Switch to Rohan Mehta (helper)**. Click the **bell**: the top notification says "Aanya
needs help with Normalization (DBMS)". Click it. Scroll to **Your answer**, paste Rohan's answer, click **Post answer**.
In the banner, click **Switch to Aanya Sharma (learner)**. Bell → "Rohan answered your doubt…" → open it. Click **Accept
answer**, then **Accept** in the dialog. A toast says "Answer accepted. This doubt is now part of the campus knowledge
base." Click the fifth star under "How helpful was it?". Point at "235 reputation" in Rohan's answer header.

> Now I'm **Rohan**. My bell already says: Aanya needs help with Normalization. I answer, explaining the **why**, not
> just the what. Back as Aanya: I **accept** it and rate it **five stars**. Rohan earns **twenty-five reputation**,
> through a ledger that can **never double-count**. And his answer now powers the **next student's** radar and hints.
> **That's the loop.**

### Scene 7: Campus Pulse (2:30 to 2:47)

**Do:** Click **Pulse**. Keep the seeded-data note in frame for a second. Point across the stat tiles, then scroll to
the **Confusion map** and click **Normalization** under DBMS. The feed opens filtered to those doubts.

> **Campus Pulse** shows faculty where the batch is stuck: solve rate, doubts solved **without waiting**, time to a first
> answer, and a **confusion map**. One click on Normalization opens **exactly those doubts**.

### Scene 8: Snap a doubt and the study planner (2:47 to 3:05)

**Do:** Click **Ask a doubt**, then the **Snap it** tab. Click the drop zone and choose your notebook photo. "Reading
your photo…" appears, then the title and body fill in, the subject switches to **Computer Networks**, and a green note
says "Transcribed by AI". Don't post it. ✂ Click **Plan**, fill in the study-plan inputs from section 2, click
**Build my plan**, and tick the first task when the plan appears (about five seconds).

> And since we all photograph our notebooks: **Snap it**. Gemini turns this CRC question into an **editable doubt**,
> every number, even my own attempt, and the photo is **never stored**. ✂ The **planner** turns my weak subjects and
> open doubts into a **day-by-day plan**.

### Scene 9: Under the hood, and close (3:05 to 3:20)

**Do:** Show the README architecture diagram on GitHub, then `studyloop-bi2z.onrender.com/api/docs`, then the green CI
badge. End on a card with the live link and the repo URL.

> Under the hood: **Spring Boot**, Postgres with **pgvector**, **Gemini** with structured output and fallbacks,
> rotating refresh tokens, and **seventy-five integration tests** in CI. StudyLoop: **every doubt solved once, solved for
> everyone.** Try it now, the link is on screen, **no signup** needed.

End card text:

```
StudyLoop
Live:  studyloop-bi2z.onrender.com   (Try as a learner, no signup)
Code:  github.com/Kavaykhurana/studyloop
NexHack 3.0 · Education & Student Innovation
```

## 4. Teleprompter version

Spoken lines only, with click cues in brackets. Put this on a phone or a second screen.

1. It's eleven at night, the day before a DBMS mid-sem. My doubt in the batch WhatsApp group gets buried in two minutes,
   even though a senior solved this exact doubt last year.
2. This is StudyLoop. It turns every doubt solved on campus into knowledge for the next student. Students join with
   their college email, which puts them on their own campus. You don't even need that: one click *[Try as a learner]*,
   and I'm Aanya, a second-year on our seeded demo campus.
3. I ask *[Ask a doubt, paste doubt 1]*: how do I check BCNF with two candidate keys? Before anything is posted
   *[Continue]*, the duplicate radar searches doubts my seniors have already solved, by meaning, not keywords.
   Ninety-five percent match, answered by Rohan, a final-year student. That's my doubt. *[This solved my doubt]* This
   solved my doubt, and nobody had to wait.
4. Now a harder one *[Ask something else, paste doubt 2, Continue]*: is my decomposition lossless, and does it preserve
   the dependencies? The radar finds related answers, but not this question, so I post it *[None of these, Post doubt]*.
   StudyLoop files it under DBMS, Normalization, and has already notified three seniors. Rohan comes first, not because
   he claims DBMS, but because he has actually solved four similar doubts. *[Why this match?]* Why this match shows the
   exact score: proof, self-rating, reputation, recent activity, and how busy he already is.
5. While I wait, I'm not spoon-fed. *[Unlock hint 1]* Hint one is just a nudge. *[Unlock hint 2]* Hint two is the
   approach, with a common pitfall. And this little chip is a citation: the hint is grounded in a senior's accepted
   answer from my own campus, and links to it. The full solution waits at hint four, only if I ask. And if the hints
   are enough, I close the doubt myself.
6. *[Switch to Rohan, bell, open]* Now I'm Rohan. My bell already says: Aanya needs help with Normalization.
   *[paste answer, Post answer]* I answer, explaining the why, not just the what. *[Switch to Aanya, bell, open, Accept
   answer, Accept, fifth star]* Back as Aanya: I accept it and rate it five stars. Rohan earns twenty-five reputation,
   through a ledger that can never double-count. And his answer now powers the next student's radar and hints. That's
   the loop.
7. *[Pulse, scroll, click Normalization]* Campus Pulse shows faculty where the batch is stuck: solve rate, doubts solved
   without waiting, time to a first answer, and a confusion map. One click on Normalization opens exactly those doubts.
8. *[Ask a doubt, Snap it, choose photo]* And since we all photograph our notebooks: Snap it. Gemini turns this CRC
   question into an editable doubt, every number, even my own attempt, and the photo is never stored. *[Plan, Build my
   plan]* The planner turns my weak subjects and open doubts into a day-by-day plan.
9. *[README diagram, /api/docs, CI badge, end card]* Under the hood: Spring Boot, Postgres with pgvector, Gemini with
   structured output and fallbacks, rotating refresh tokens, and seventy-five integration tests in CI. StudyLoop: every
   doubt solved once, solved for everyone. Try it now, the link is on screen, no signup needed.

## 5. The 3:00 cut

Drop the three ✂ items: the "Why this match shows the exact score…" sentence (still click it silently, the bars are
self-explanatory), the planner sentence and its clicks, and nothing else. Trim waits in editing. That lands at about 3:00
with the whole loop, Pulse, Snap and the tech close intact.

## 6. If something goes wrong on camera

| What happens | What to do |
|---|---|
| The radar shows a slightly different percentage | Say the number you see. |
| Hint 2 shows no citation chip | Model output varies a little per take. Unlock hint 3 and point at its chip instead, or say: "When a senior's answer is relevant, the hint cites it." |
| A hint takes more than three seconds | Keep talking: "It first pulls related answers from campus." Cut the wait in editing. |
| "The hint tutor is busy right now" | Click **Try again**. Free-tier Gemini can be briefly rate-limited; the app retries and falls back to its second model automatically. |
| The bell has not updated yet | It refreshes every 15 seconds. Open the doubt from **Doubts** (top of the feed) instead. |
| Snap says it couldn't find a question | Retake the photo closer, flat, in good light, with only the question in frame. |
| The page asks you to sign in again after a long pause | Click **Try as a learner** again; the demo is one click. |
| Counts or the radar look different from this script | You skipped the reset. Run it (section 1, step 2) and redo the scene. |

## 7. Three-minute live pitch (if a final round is announced)

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
