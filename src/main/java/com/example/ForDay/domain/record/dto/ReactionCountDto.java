package com.example.ForDay.domain.record.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor // JPQL의 'new' 키워드 사용 시 필수
public class ReactionCountDto {
    private Long recordId;
    private Long count;
}