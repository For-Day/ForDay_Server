package com.example.ForDay.infra.lambda.adapter;

import com.example.ForDay.domain.hobby.port.CoverGeneratorPort;
import com.example.ForDay.infra.lambda.invoker.CoverLambdaInvoker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AWS Lambda로 커버 리사이즈본을 만든다.
 *
 * <p>SET_COVER 페이로드 규약은 이 클래스 안에만 있다.
 */
@Component
@RequiredArgsConstructor
public class LambdaCoverGeneratorAdapter implements CoverGeneratorPort {

    private static final String ACTION_SET_COVER = "SET_COVER";

    private final CoverLambdaInvoker coverLambdaInvoker;

    @Override
    public void generateCover(String sourceKey, String destinationKey) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", ACTION_SET_COVER);
        payload.put("srcKey", sourceKey);
        payload.put("dstKey", destinationKey);

        coverLambdaInvoker.invokeSync(payload);
    }
}
