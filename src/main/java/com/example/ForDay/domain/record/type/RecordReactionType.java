package com.example.ForDay.domain.record.type;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum RecordReactionType {
    AWESOME("멋져요"),
    GREAT("짱이야"),
    AMAZING("대단해"),
    FIGHTING("화이팅");

    private final String description;
}
