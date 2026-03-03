package com.example.ForDay.domain.friend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportFriendResDto {
    private String message;
    private String nickname;
    private String userId;
}
