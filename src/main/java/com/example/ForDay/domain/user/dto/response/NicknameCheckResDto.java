package com.example.ForDay.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NicknameCheckResDto {
    private String nickname;
    private boolean isAvailable;
    private String message;
}
