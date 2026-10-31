package com.studyloop.subject;

import com.studyloop.subject.dto.SubjectRef;
import com.studyloop.subject.dto.TopicRef;
import java.util.List;

public record SubjectInfo(long id, String code, String name, String colorHex, int sortOrder, List<TopicRef> topics) {

    public SubjectRef ref() {
        return new SubjectRef(id, code, name, colorHex);
    }
}
