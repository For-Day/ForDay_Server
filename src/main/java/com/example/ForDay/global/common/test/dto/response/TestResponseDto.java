package com.example.ForDay.global.common.test.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestResponseDto {
    private String message;
    private int serverPort;
    private String serverEnv;
    private String serverName;


}
