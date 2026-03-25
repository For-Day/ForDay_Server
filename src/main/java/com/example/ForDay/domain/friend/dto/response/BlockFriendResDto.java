package com.example.ForDay.domain.friend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockFriendResDto {
    private String message;
    private String nickname;

    public static BlockFriendResDto of(String message, String targetUserNickname) {
        return new BlockFriendResDto(
                message,
                targetUserNickname
        );
    }
}
