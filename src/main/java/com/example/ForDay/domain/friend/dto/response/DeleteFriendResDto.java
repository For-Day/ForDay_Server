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
}
