package com.example.ForDay.infra.lambda.invoker;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CoverLambdaInvoker {

    private final LambdaClient lambdaClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${lambda.create-cover-function-name}")
    private String functionName;

    public String invokeSync(Map<String, Object> payload) throws Exception {
        byte[] json = objectMapper.writeValueAsBytes(payload);

        InvokeRequest req = InvokeRequest.builder()
                .functionName(functionName)
                .invocationType(InvocationType.REQUEST_RESPONSE)
                .payload(SdkBytes.fromByteArray(json))
                .build();

        InvokeResponse res = lambdaClient.invoke(req);

        // Lambda 함수 에러(런타임/throw)면 여기로 잡힘
        if (res.functionError() != null) {
            String errPayload = res.payload() != null
                    ? res.payload().asUtf8String()
                    : "";
            throw new RuntimeException("Lambda FunctionError=" + res.functionError() + " payload=" + errPayload);
        }

        return res.payload() != null ? res.payload().asUtf8String() : "";
    }



}
