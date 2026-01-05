package com.example.ForDay.global.common.test.controller;

import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.test.dto.response.TestResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
public class TestController {
    @Value("${server.port}")
    private int serverPort;

    @Value("${server.env}")
    private String serverEnv;

    @Value("${serverName}")
    private String serverName;



    @Operation(
            summary = "헬스 체크",
            description = "서버가 정상 동작 중인지 확인합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "정상 응답",
            content = @Content(schema = @Schema(implementation = TestResponseDto.class))
    )
    @GetMapping("/health_check")
    public TestResponseDto healthCheck() {
        return new TestResponseDto("테스트에 성공하였습니다~~!!", serverPort, serverEnv, serverName);
    }

    @GetMapping("/auth/check")
    public TestResponseDto authCheck() {
        return new TestResponseDto("인증에 성공하였습니다!", serverPort, serverEnv, serverName);
    }

    @Operation(
            summary = "에러 테스트",
            description = "테스트용 커스텀 예외를 발생시킵니다."
    )
    @ApiResponse(
            responseCode = "400",
            description = "테스트 에러"
    )
    @GetMapping("/error_check")
    public void errorCheck() {
        throw new CustomException(ErrorCode.TEST_ERROR_CODE);
    }

    @PostMapping("/log-test")
    public void logTest() {
        log.trace("trace Log");
        log.debug("Debug Log");
        log.info("Info Log"); // 출력
        log.warn("Warn Log"); // 출력
        log.error("Error Log"); // 출력
    }

    @GetMapping("/env")
    public String env() {
        return serverEnv;
    }
}
