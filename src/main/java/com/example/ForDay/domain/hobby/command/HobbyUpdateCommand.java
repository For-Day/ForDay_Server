package com.example.ForDay.domain.hobby.command;

/**
 * 취미 수정에 필요한 값 묶음.
 *
 * @param goalDays 기간을 지정하지 않았으면 null
 * @see HobbyCreateCommand
 */
public record HobbyUpdateCommand(
        Long hobbyInfoId,
        String hobbyName,
        String hobbyPurpose,
        Integer hobbyTimeMinutes,
        Integer executionCount,
        Integer goalDays
) {
}
