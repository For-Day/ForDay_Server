package com.example.ForDay.domain.record.command;

import com.example.ForDay.domain.record.type.RecordVisibility;

/**
 * 활동 기록 수정에 필요한 값 묶음.
 *
 * @see RecordCreateCommand
 */
public record RecordUpdateCommand(
        String sticker,
        String memo,
        RecordVisibility visibility,
        String imageUrl
) {
}
