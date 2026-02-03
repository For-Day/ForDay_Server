package com.example.ForDay.domain.friend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockFriendReqDto {
    @NotBlank(message = "차단할 사용자 ID는 필수 입력값입니다.")
    private String userId;
}
