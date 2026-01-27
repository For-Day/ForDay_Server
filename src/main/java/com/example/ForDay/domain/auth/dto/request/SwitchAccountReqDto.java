package com.example.ForDay.domain.auth.dto.request;

import com.example.ForDay.domain.user.type.SocialType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SwitchAccountReqDto {
    private SocialType socialType;
    private String socialCode;
}
