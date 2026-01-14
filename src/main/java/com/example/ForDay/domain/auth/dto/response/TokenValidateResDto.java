package com.example.ForDay.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenValidateResDto {
    private boolean accessValid;
    private boolean refreshValid;
}
