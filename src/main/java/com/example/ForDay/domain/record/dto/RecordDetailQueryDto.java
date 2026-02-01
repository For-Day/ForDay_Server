package com.example.ForDay.domain.record.dto;

import com.example.ForDay.domain.record.type.RecordVisibility;

import java.time.LocalDateTime;

public record RecordDetailQueryDto(
        Long hobbyId,
        Long activityId,
        Long recordId,
        String imageUrl,
        String memo,
        String sticker,
        LocalDateTime createdAt,
        RecordVisibility visibility,
        String writerId,
        String writerNickname,
        String writerProfileImageUrl,
        boolean writerDeleted,
        String activityContent,
        boolean recordDeleted
) {}