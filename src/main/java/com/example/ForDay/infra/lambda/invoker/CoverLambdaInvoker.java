package com.example.ForDay.infra.lambda.invoker;

import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoverLambdaInvoker {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${lambda.create-cover-function-name}")
    private String functionName;

    // #356: $LATEST를 직접 호출하는 대신 alias(prod)를 통해 버전 고정 호출한다.
    // 비워두면(로컬/테스트처럼 alias가 없는 환경) qualifier를 지정하지 않아 기존과 동일하게
    // $LATEST가 호출된다 - 하위 호환을 위해 기본값을 빈 문자열로 둔다.
    @Value("${lambda.create-cover-function-alias:}")
    private String functionAlias;

    public String invokeSync(Map<String, Object> payload) throws Exception {
        byte[] json = objectMapper.writeValueAsBytes(payload);

        InvokeRequest.Builder reqBuilder = InvokeRequest.builder()
                .functionName(functionName)
                .invocationType(InvocationType.REQUEST_RESPONSE)
                .payload(SdkBytes.fromByteArray(json));

        if (StringUtils.hasText(functionAlias)) {
            reqBuilder.qualifier(functionAlias);
        }

        InvokeRequest req = reqBuilder.build();

        InvokeResponse res;
        try {
            res = lambdaClient.invoke(req);
        } catch (ApiCallTimeoutException | ApiCallAttemptTimeoutException e) {
            // AwsLambdaConfig의 apiCallAttemptTimeout/apiCallTimeout(재시도 포함) 초과.
            // 콜드 스타트·지연으로 인한 시간 초과를 다른 SDK 오류와 구분해 사용자에게 노출한다.
            log.error("[Lambda] 커버 생성 호출 타임아웃 - functionName={}", functionName, e);
            throw new CustomException(ErrorCode.COVER_GENERATION_TIMEOUT);
        } catch (SdkException e) {
            log.error("[Lambda] 커버 생성 호출 실패 - functionName={}", functionName, e);
            throw new CustomException(ErrorCode.COVER_GENERATION_FAILED);
        }

        // Lambda 함수 에러(런타임/throw)면 여기로 잡힘
        if (res.functionError() != null) {
            String errPayload = res.payload() != null
                    ? res.payload().asUtf8String()
                    : "";
            log.error("[Lambda] 커버 생성 함수 에러 - functionError={}, payload={}", res.functionError(), errPayload);
            throw new CustomException(ErrorCode.COVER_GENERATION_FAILED);
        }

        return res.payload() != null ? res.payload().asUtf8String() : "";
    }
}
