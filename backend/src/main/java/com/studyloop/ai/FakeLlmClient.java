package com.studyloop.ai;

import com.studyloop.ai.dto.ClassifyResult;
import com.studyloop.ai.dto.HintResult;
import com.studyloop.ai.dto.PlanResult;
import com.studyloop.ai.dto.SnapResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Deterministic stand-in used when no Gemini key is configured, and in every test. */
public class FakeLlmClient implements LlmClient {

    private static final Pattern SUBJECTS_LINE = Pattern.compile("SUBJECTS:\\s*(\\{[^\\n]*\\})");
    private static final Pattern RUNG = Pattern.compile("RUNG:\\s*(\\d)");
    private static final Pattern TITLE = Pattern.compile("DOUBT TITLE:\\s*([^\\n]*)");
    private static final Pattern DATES = Pattern.compile("DATES:\\s*([^\\n]*)");
    private static final Pattern BUDGET = Pattern.compile("DAILY_BUDGET_MINUTES:\\s*(\\d+)");
    private static final Pattern OPEN = Pattern.compile("OPEN_DOUBTS:\\s*(\\[[^\\n]*\\])");

    private final JsonMapper mapper;

    public FakeLlmClient(JsonMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public <T> AiResult<T> generateJson(AiRequest request, Class<T> type) {
        Object value = switch (request.feature()) {
            case CLASSIFY -> classify(request.userText());
            case SNAP -> snap(request.userText());
            case HINT -> hint(request.userText());
            case PLAN -> plan(request.userText());
            case EMBED -> throw new AiUnavailableException("FAKE_UNSUPPORTED");
        };
        return new AiResult<>(type.cast(value), "fake", 1);
    }

    private ClassifyResult classify(String text) {
        Matcher t = TITLE.matcher(text);
        String title = t.find() ? t.group(1).trim() : "Transcribed doubt";
        int marker = text.indexOf("DOUBT TITLE:");
        FakeTopicGuesser.Guess guess = FakeTopicGuesser.guess(marker >= 0 ? text.substring(marker) : text,
                subjects(text));
        return new ClassifyResult(guess.subjectCode(), guess.topic(), 2, true,
                title.length() <= 120 ? title : title.substring(0, 120));
    }

    private SnapResult snap(String text) {
        return new SnapResult(true, "Transcribed doubt",
                "Transcribed from a photo of the notebook page. Edit this text so it matches the question.",
                firstSubject(text), "General", "MEDIUM");
    }

    private static final Pattern TOPIC = Pattern.compile("TOPIC:\\s*([^\\n]*)");
    private static final Pattern KNOWLEDGE = Pattern.compile(
            "\\[#(\\d+)\\] Q: ([^\\n]*?) — [^\\n]*\\nAccepted answer: ([^\\n]*)");
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+");

    /** Rung-shaped offline hint that reuses the closest campus answer and cites it, mirroring the real prompt. */
    private HintResult hint(String text) {
        Matcher m = RUNG.matcher(text);
        int rung = m.find() ? Integer.parseInt(m.group(1)) : 1;
        Matcher tm = TOPIC.matcher(text);
        String topic = tm.find() ? tm.group(1).trim() : "this topic";
        Matcher km = KNOWLEDGE.matcher(text);
        Long citedId = null;
        String citedTitle = null;
        String citedAnswer = null;
        if (km.find()) {
            citedId = Long.parseLong(km.group(1));
            citedTitle = km.group(2).trim();
            citedAnswer = km.group(3).trim();
        }
        String cite = citedId == null ? "" : " [#" + citedId + "]";
        String firstStep = null;
        if (citedAnswer != null) {
            String[] sentences = SENTENCE_END.split(citedAnswer, 3);
            firstStep = sentences.length > 1 && sentences[0].length() < 60
                    ? sentences[0] + " " + sentences[1] : sentences[0];
        }
        String content = switch (rung) {
            case 1 -> "What is the one defining rule of **" + topic + "** that your question depends on? Write it "
                    + "down in one line, then check which part of your problem does not obey it.";
            case 2 -> "- Restate exactly what is given and what is asked.\n"
                    + "- Recall the standard method for " + topic + " and apply it one step at a time.\n"
                    + "- Check each intermediate result against the definition before moving on.\n"
                    + "- Pitfall: skipping the check on the edge case is where most marks are lost."
                    + (citedId == null ? "" : "\n\nA senior solved a very similar doubt:" + cite + " " + citedTitle);
            case 3 -> (firstStep == null
                    ? "Start by writing the definition of " + topic + " and applying it to the first part of your "
                            + "problem only."
                    : "Start the way the senior did in" + cite + ": " + firstStep)
                    + "\n\nNow do the same for the next part of your problem yourself.";
            default -> "**Answer:** " + (citedAnswer == null
                    ? "Apply the " + topic + " method to every part of the problem and combine the results."
                    : citedAnswer + cite)
                    + "\n\n**Common mistake:** stopping after the first part without checking the remaining cases."
                    + "\n\n**Try this:** change one value in your problem and solve it again from scratch.";
        };
        boolean cites = citedId != null && content.contains(cite);
        return new HintResult(content, cites ? List.of(citedId) : List.of(), cites ? "HIGH" : "MEDIUM");
    }

    private PlanResult plan(String text) {
        Matcher dm = DATES.matcher(text);
        List<String> dates = new ArrayList<>();
        if (dm.find()) {
            for (String d : dm.group(1).split(",")) {
                if (!d.isBlank()) {
                    dates.add(d.trim());
                }
            }
        }
        Matcher bm = BUDGET.matcher(text);
        int budget = bm.find() ? Integer.parseInt(bm.group(1)) : 60;
        Map<String, List<String>> subjects = subjects(text);
        List<String> codes = new ArrayList<>(subjects.keySet());
        String openDoubt = openDoubt(text);

        List<PlanResult.Day> days = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            String code = codes.get(i % codes.size());
            List<String> topics = subjects.get(code);
            String topic = topics.get(i % topics.size());
            boolean last = i == dates.size() - 1 && dates.size() > 1;
            int a = Math.min(240, floor5(budget * 0.45));
            int b = Math.min(240, floor5(budget * 0.35));
            int c = Math.min(240, floor5(budget * 0.10));
            List<PlanResult.Task> tasks = new ArrayList<>();
            tasks.add(new PlanResult.Task(last ? "Revise " + topic + " notes and formulas"
                    : "Read and summarise " + topic + " (2 pages of notes)", Math.max(10, a),
                    last ? "REVISE" : "LEARN", code, topic));
            tasks.add(new PlanResult.Task(last ? "Redo 5 solved " + topic + " problems"
                    : "Solve 5 " + topic + " practice questions", Math.max(10, b),
                    last ? "REVISE" : "PRACTICE", code, topic));
            if (i == 0 && openDoubt != null) {
                tasks.add(new PlanResult.Task("Ask a senior: " + openDoubt, Math.max(10, c), "ASK_PEER", code, topic));
            } else if (c >= 10) {
                tasks.add(new PlanResult.Task("Write 3 quick revision cards on " + topic, c, "REVISE", code, topic));
            }
            days.add(new PlanResult.Day(dates.get(i), topic + " focus day", trimToBudget(tasks, budget)));
        }
        return new PlanResult("Start with your weakest topics, practise daily, and keep the last day for revision.",
                days);
    }

    private static List<PlanResult.Task> trimToBudget(List<PlanResult.Task> tasks, int budget) {
        List<PlanResult.Task> kept = new ArrayList<>();
        int total = 0;
        for (PlanResult.Task t : tasks) {
            if (total + t.minutes() > budget) {
                break;
            }
            total += t.minutes();
            kept.add(t);
        }
        return kept;
    }

    private static int floor5(double v) {
        return ((int) v / 5) * 5;
    }

    private String firstSubject(String text) {
        List<String> codes = new ArrayList<>(subjects(text).keySet());
        return codes.isEmpty() ? "DSA" : codes.get(0);
    }

    private Map<String, List<String>> subjects(String text) {
        Matcher m = SUBJECTS_LINE.matcher(text);
        if (!m.find()) {
            return Map.of("DSA", List.of("General"));
        }
        return mapper.readValue(m.group(1), new TypeReference<java.util.LinkedHashMap<String, List<String>>>() {});
    }

    private String openDoubt(String text) {
        Matcher m = OPEN.matcher(text);
        if (!m.find()) {
            return null;
        }
        List<String> titles = mapper.readValue(m.group(1), new TypeReference<List<String>>() {});
        return titles.isEmpty() ? null : titles.get(0);
    }
}
