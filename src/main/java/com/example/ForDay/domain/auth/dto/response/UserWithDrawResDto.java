package com.example.ForDay.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserWithDrawResDto {
    private String message;
    private LocalDateTime deletedAt;
}
