package com.example.ForDay.domain.record.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RecordDeleteCheckDto {
    private final LocalDateTime createdAt;
    private final boolean deleted;
    private final String imageUrl;
    private final Long hobbyId;
}
