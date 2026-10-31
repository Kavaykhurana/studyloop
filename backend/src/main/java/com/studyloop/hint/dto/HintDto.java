package com.studyloop.hint.dto;

import java.util.List;

public record HintDto(int level, String contentMd, List<CitedDoubt> citedDoubts, String confidence) {

    public record CitedDoubt(Long id, String title) {}
}
