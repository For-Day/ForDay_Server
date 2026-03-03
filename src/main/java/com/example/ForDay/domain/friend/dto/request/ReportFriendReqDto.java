package com.example.ForDay.domain.friend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportFriendReqDto {
    @NotBlank(message = "사용자 ID는 필수입니다.")
    private String userId;
    private String reason;
}
