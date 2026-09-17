package com.example.ForDay.infra.lambda.invoker;

import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CoverLambdaInvokerTest {

    @Mock
    private LambdaClient lambdaClient;

    private CoverLambdaInvoker invoker;

    private CoverLambdaInvoker newInvoker() throws Exception {
        return newInvoker(null);
    }

    private CoverLambdaInvoker newInvoker(String functionAlias) throws Exception {
        CoverLambdaInvoker invoker = new CoverLambdaInvoker(lambdaClient);
        Field functionName = CoverLambdaInvoker.class.getDeclaredField("functionName");
        functionName.setAccessible(true);
        functionName.set(invoker, "test-cover-function");

        Field aliasField = CoverLambdaInvoker.class.getDeclaredField("functionAlias");
        aliasField.setAccessible(true);
        aliasField.set(invoker, functionAlias);
        return invoker;
    }

    @Test
    @DisplayName("정상 응답이면 payload 문자열을 반환한다")
    void invokeSync_returnsPayload_onSuccess() throws Exception {
        invoker = newInvoker();
        InvokeResponse response = InvokeResponse.builder()
                .payload(SdkBytes.fromUtf8String("{\"result\":\"ok\"}"))
                .build();
        given(lambdaClient.invoke(any(InvokeRequest.class))).willReturn(response);

        String result = invoker.invokeSync(Map.of("action", "SET_COVER"));

        assertThat(result).isEqualTo("{\"result\":\"ok\"}");
    }

    @Test
    @DisplayName("apiCallTimeout(재시도 포함 전체 상한) 초과 시 COVER_GENERATION_TIMEOUT으로 변환한다")
    void invokeSync_mapsApiCallTimeout_toCoverGenerationTimeout() throws Exception {
        invoker = newInvoker();
        willThrow(ApiCallTimeoutException.create(20_000))
                .given(lambdaClient).invoke(any(InvokeRequest.class));

        assertThatThrownBy(() -> invoker.invokeSync(Map.of("action", "SET_COVER")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COVER_GENERATION_TIMEOUT);
    }

    @Test
    @DisplayName("apiCallAttemptTimeout(1회 시도 상한) 초과 시에도 COVER_GENERATION_TIMEOUT으로 변환한다")
    void invokeSync_mapsApiCallAttemptTimeout_toCoverGenerationTimeout() throws Exception {
        invoker = newInvoker();
        willThrow(ApiCallAttemptTimeoutException.create(10_000))
                .given(lambdaClient).invoke(any(InvokeRequest.class));

        assertThatThrownBy(() -> invoker.invokeSync(Map.of("action", "SET_COVER")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COVER_GENERATION_TIMEOUT);
    }

    @Test
    @DisplayName("그 외 SDK 오류(네트워크 등)는 COVER_GENERATION_FAILED로 변환한다")
    void invokeSync_mapsOtherSdkException_toCoverGenerationFailed() throws Exception {
        invoker = newInvoker();
        willThrow(SdkClientException.create("connection reset"))
                .given(lambdaClient).invoke(any(InvokeRequest.class));

        assertThatThrownBy(() -> invoker.invokeSync(Map.of("action", "SET_COVER")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COVER_GENERATION_FAILED);
    }

    @Test
    @DisplayName("Lambda 함수 자체가 에러를 던지면(functionError) COVER_GENERATION_FAILED로 변환한다")
    void invokeSync_mapsFunctionError_toCoverGenerationFailed() throws Exception {
        invoker = newInvoker();
        InvokeResponse response = InvokeResponse.builder()
                .functionError("Unhandled")
                .payload(SdkBytes.fromUtf8String("{\"errorMessage\":\"boom\"}"))
                .build();
        given(lambdaClient.invoke(any(InvokeRequest.class))).willReturn(response);

        assertThatThrownBy(() -> invoker.invokeSync(Map.of("action", "SET_COVER")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COVER_GENERATION_FAILED);
    }

    @Test
    @DisplayName("alias가 설정되어 있으면 qualifier로 그 alias를 지정해 호출한다 (#356 - $LATEST 대신 버전 고정 호출)")
    void invokeSync_setsQualifier_whenAliasConfigured() throws Exception {
        invoker = newInvoker("prod");
        given(lambdaClient.invoke(any(InvokeRequest.class)))
                .willReturn(InvokeResponse.builder().payload(SdkBytes.fromUtf8String("{}")).build());

        invoker.invokeSync(Map.of("action", "SET_COVER"));

        ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
        verify(lambdaClient).invoke(captor.capture());
        assertThat(captor.getValue().qualifier()).isEqualTo("prod");
    }

    @Test
    @DisplayName("alias가 설정돼 있지 않으면 qualifier 없이 호출한다 (기존과 동일하게 $LATEST)")
    void invokeSync_omitsQualifier_whenAliasNotConfigured() throws Exception {
        invoker = newInvoker(null);
        given(lambdaClient.invoke(any(InvokeRequest.class)))
                .willReturn(InvokeResponse.builder().payload(SdkBytes.fromUtf8String("{}")).build());

        invoker.invokeSync(Map.of("action", "SET_COVER"));

        ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
        verify(lambdaClient).invoke(captor.capture());
        assertThat(captor.getValue().qualifier()).isNull();
    }
}
