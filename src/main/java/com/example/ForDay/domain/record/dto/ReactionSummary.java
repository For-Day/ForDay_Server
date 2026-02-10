package com.example.ForDay.domain.record.dto;

import com.example.ForDay.domain.record.type.RecordReactionType;

public record ReactionSummary(
        RecordReactionType type,
        String reactedUserId,
        boolean readWriter
) {}