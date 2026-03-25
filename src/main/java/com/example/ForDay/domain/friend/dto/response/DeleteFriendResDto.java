package com.example.ForDay.domain.friend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteFriendResDto {
    private String message;
    private String nickname;

    public static DeleteFriendResDto of(String message, String targetUserNickname) {
        return new DeleteFriendResDto(
                message,
                targetUserNickname
        );
    }
}
