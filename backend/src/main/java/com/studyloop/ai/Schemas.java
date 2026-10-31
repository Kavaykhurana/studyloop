package com.studyloop.ai;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** JSON schemas for structured Gemini output (spec section 9.4). */
@Component
public class Schemas {

    private static final String CLASSIFY = """
            {"type":"object","properties":{
              "subjectCode":{"type":"string","enum":[%s]},
              "topic":{"type":"string"},
              "difficulty":{"type":"integer","minimum":1,"maximum":3},
              "isAcademic":{"type":"boolean"},
              "suggestedTitle":{"type":"string","maxLength":120}},
             "required":["subjectCode","topic","difficulty","isAcademic","suggestedTitle"]}""";

    private static final String SNAP = """
            {"type":"object","properties":{
              "readable":{"type":"boolean"},
              "title":{"type":"string","maxLength":160},
              "bodyMd":{"type":"string","maxLength":6000},
              "subjectCode":{"type":"string"},
              "topic":{"type":"string"},
              "confidence":{"type":"string","enum":["HIGH","MEDIUM","LOW"]}},
             "required":["readable","title","bodyMd","subjectCode","topic","confidence"]}""";

    private static final String HINT = """
            {"type":"object","properties":{
              "contentMd":{"type":"string","maxLength":4000},
              "citedDoubtIds":{"type":"array","items":{"type":"integer"}},
              "confidence":{"type":"string","enum":["HIGH","MEDIUM","LOW"]}},
             "required":["contentMd","citedDoubtIds","confidence"]}""";

    private static final String PLAN = """
            {"type":"object","properties":{
              "summary":{"type":"string","maxLength":300},
              "days":{"type":"array","items":{"type":"object","properties":{
                 "date":{"type":"string"},
                 "focus":{"type":"string","maxLength":80},
                 "tasks":{"type":"array","items":{"type":"object","properties":{
                    "title":{"type":"string","maxLength":140},
                    "minutes":{"type":"integer","minimum":10,"maximum":240},
                    "kind":{"type":"string","enum":["LEARN","PRACTICE","REVISE","ASK_PEER"]},
                    "subjectCode":{"type":"string"},
                    "topic":{"type":"string"}},
                   "required":["title","minutes","kind","subjectCode","topic"]}}},
                "required":["date","focus","tasks"]}}},
             "required":["summary","days"]}""";

    private final JsonMapper mapper;

    public Schemas(JsonMapper mapper) {
        this.mapper = mapper;
    }

    /** The subject enum is built from the database at runtime, never hard-coded. */
    public Map<String, Object> classify(List<String> subjectCodes) {
        String enumValues = String.join(",", subjectCodes.stream().map(c -> "\"" + c + "\"").toList());
        return parse(CLASSIFY.formatted(enumValues));
    }

    public Map<String, Object> snap() {
        return parse(SNAP);
    }

    public Map<String, Object> hint() {
        return parse(HINT);
    }

    public Map<String, Object> plan() {
        return parse(PLAN);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) {
        return mapper.readValue(json, Map.class);
    }
}
