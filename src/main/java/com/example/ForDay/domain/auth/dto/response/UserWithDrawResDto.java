package com.example.ForDay.domain.auth.dto.response;

import com.example.ForDay.global.common.response.message.AuthSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.example.ForDay.global.common.response.message.AuthSuccessCode.WITHDRAW_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserWithDrawResDto {
    private String message;
    private LocalDateTime deletedAt;

    public static UserWithDrawResDto of(LocalDateTime deletedAt) {
        return new UserWithDrawResDto(
                AuthSuccessCode.WITHDRAW_SUCCESS.getMessage(),
                deletedAt
        );
    }
}
