package com.bookeatinglion.member.infra.cognito;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminSetUserPasswordRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.MessageActionType;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * AWS Cognito User Pool로 회원가입/로그인/토큰 재발급을 위임하는 클라이언트.
 *
 * <p>백엔드는 더 이상 비밀번호를 저장하거나 토큰을 직접 서명하지 않는다. Admin 계열 API
 * (AdminCreateUser/AdminSetUserPassword/AdminInitiateAuth)로 Cognito User Pool에
 * 요청을 그대로 전달하고 결과만 돌려받는다. 이 클래스는 AWS 예외를 그대로 전파하며,
 * 비즈니스 예외로의 변환은 호출부(AuthCommandService/AuthQueryService)가 담당한다.</p>
 */
@Component
@RequiredArgsConstructor
public class CognitoAuthClient {

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${cognito.user-pool-id}")
    private String userPoolId;

    @Value("${cognito.client-id}")
    private String clientId;

    /** App Client에 시크릿이 없는 public 클라이언트라면 빈 문자열로 둔다. */
    @Value("${cognito.client-secret:}")
    private String clientSecret;

    /**
     * Cognito에 사용자를 즉시 활성화된 상태로 생성한다(이메일 인증 절차 없음).
     *
     * @param username 로그인 아이디
     * @param password 최종 비밀번호. 임시 비밀번호 단계 없이 바로 영구 비밀번호로 설정하여,
     *                 가입 직후 {@code NEW_PASSWORD_REQUIRED} 챌린지 없이 로그인할 수 있게 한다.
     * @return Cognito User Pool 상의 고유 식별자(sub)
     */
    public String createUser(String username, String password) {
        AdminCreateUserResponse response = cognitoClient.adminCreateUser(AdminCreateUserRequest.builder()
                .userPoolId(userPoolId)
                .username(username)
                .temporaryPassword(password)
                .messageAction(MessageActionType.SUPPRESS)
                .build());

        cognitoClient.adminSetUserPassword(AdminSetUserPasswordRequest.builder()
                .userPoolId(userPoolId)
                .username(username)
                .password(password)
                .permanent(true)
                .build());

        return response.user().attributes().stream()
                .filter(attribute -> "sub".equals(attribute.name()))
                .map(AttributeType::value)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cognito가 sub 속성을 반환하지 않았습니다."));
    }

    /** 아이디/비밀번호로 Cognito에 로그인하고 발급된 토큰 세트를 반환한다. */
    public AuthenticationResultType login(String username, String password) {
        Map<String, String> authParameters = new HashMap<>();
        authParameters.put("USERNAME", username);
        authParameters.put("PASSWORD", password);
        if (!clientSecret.isBlank()) {
            authParameters.put("SECRET_HASH", calculateSecretHash(username));
        }

        AdminInitiateAuthResponse response = cognitoClient.adminInitiateAuth(AdminInitiateAuthRequest.builder()
                .userPoolId(userPoolId)
                .clientId(clientId)
                .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                .authParameters(authParameters)
                .build());

        return response.authenticationResult();
    }

    /**
     * Refresh Token으로 새 토큰 세트를 발급받는다.
     *
     * <p>Cognito는 {@code REFRESH_TOKEN_AUTH} 플로우에서 기본적으로 새 Refresh Token을
     * 돌려주지 않는다(App Client의 Refresh Token Rotation이 꺼져 있는 한 — 켜져 있으면
     * 이 플로우 자체가 동작하지 않으니 절대 켜면 안 된다). 새 Refresh Token 유무 판단은
     * 호출부가 담당한다.</p>
     */
    public AuthenticationResultType refresh(String refreshToken) {
        if (!clientSecret.isBlank()) {
            // REFRESH_TOKEN_AUTH의 SECRET_HASH는 username이 있어야 계산할 수 있는데,
            // Refresh 요청에는 username이 없다. 시크릿 없는(public) App Client 사용을 전제로 한다.
            throw new IllegalStateException(
                    "클라이언트 시크릿이 설정된 Cognito App Client는 REFRESH_TOKEN_AUTH를 지원하지 않습니다. " +
                            "시크릿 없는 App Client를 사용하세요.");
        }

        AdminInitiateAuthResponse response = cognitoClient.adminInitiateAuth(AdminInitiateAuthRequest.builder()
                .userPoolId(userPoolId)
                .clientId(clientId)
                .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                .authParameters(Map.of("REFRESH_TOKEN", refreshToken))
                .build());

        return response.authenticationResult();
    }

    private String calculateSecretHash(String username) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] rawHmac = mac.doFinal((username + clientId).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Cognito SECRET_HASH 계산에 실패했습니다.", e);
        }
    }
}
