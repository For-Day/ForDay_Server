package com.example.ForDay.domain.friend.dto.response;

import com.example.ForDay.domain.user.entity.User;
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

    public static ReportFriendResDto of(String message, User targetUser) {
        return new ReportFriendResDto(
                message,
                targetUser.getNickname(),
                targetUser.getId()
        );
    }
}
