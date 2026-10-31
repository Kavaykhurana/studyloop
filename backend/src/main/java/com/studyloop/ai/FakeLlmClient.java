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
        return new ClassifyResult(firstSubject(text), "General", 2, true,
                title.length() <= 120 ? title : title.substring(0, 120));
    }

    private SnapResult snap(String text) {
        return new SnapResult(true, "Transcribed doubt",
                "Transcribed from a photo of the notebook page. Edit this text so it matches the question.",
                firstSubject(text), "General", "MEDIUM");
    }

    private HintResult hint(String text) {
        Matcher m = RUNG.matcher(text);
        String n = m.find() ? m.group(1) : "1";
        return new HintResult("**Hint " + n + "** — Think about what changes when the input is already sorted.",
                List.of(), "MEDIUM");
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
