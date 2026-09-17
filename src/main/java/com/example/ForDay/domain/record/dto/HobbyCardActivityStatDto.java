package com.example.ForDay.domain.record.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 취미 카드 문구 생성(이슈 #388)용 활동별 기록 통계.
 * 취미 안에서 가장 많이 기록된 활동 TOP5를 뽑는 데 쓴다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HobbyCardActivityStatDto {
    private Long activityId;
    private String content;
    private Long recordCount;
}
