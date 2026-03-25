package com.example.ForDay.domain.friend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddFriendResDto {
    private String message;
    private String nickname;

    public static AddFriendResDto of(String message, String targetUserNickname) {
        return new AddFriendResDto(message, targetUserNickname);
    }
}
