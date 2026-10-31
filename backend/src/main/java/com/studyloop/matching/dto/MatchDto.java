package com.studyloop.matching.dto;

import com.studyloop.user.dto.UserBrief;
import java.util.List;
import java.util.Map;

public record MatchDto(UserBrief helper, int rank, int score, Map<String, Double> breakdown, List<String> reasons) {}
