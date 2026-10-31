package com.studyloop.subject;

import com.studyloop.subject.dto.SubjectWithTopics;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subjects")
@Tag(name = "Subjects")
public class SubjectController {

    private final SubjectCatalog catalog;

    public SubjectController(SubjectCatalog catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<SubjectWithTopics> list() {
        return catalog.all().stream()
                .map(s -> new SubjectWithTopics(s.id(), s.code(), s.name(), s.colorHex(), s.topics()))
                .toList();
    }
}
