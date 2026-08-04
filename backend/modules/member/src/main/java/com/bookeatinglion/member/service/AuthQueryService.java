package com.bookeatinglion.member.service;

import com.bookeatinglion.common.exception.BusinessException;
import com.bookeatinglion.common.exception.ErrorCode;
import com.bookeatinglion.member.domain.Member;
import com.bookeatinglion.member.dto.AuthDto;
import com.bookeatinglion.member.infra.cognito.CognitoAuthClient;
import com.bookeatinglion.member.repository.MemberRepository;
import com.nimbusds.jwt.JWTParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

import java.text.ParseException;

/**
 * 인증(Auth) 도메인의 조회(Query) 위주 유스케이스를 담당하는 서비스.
 *
 * <p>로그인/토큰 재발급 모두 AWS Cognito User Pool로 위임한다. 이 서비스는 Cognito가
 * 돌려준 인증 결과를 받아 로컬 {@code members} 테이블에서 회원 요약 정보를 조회해
 * 응답을 구성하는 역할만 하며, 비밀번호 비교나 토큰 서명/발급은 하지 않는다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthQueryService {

    private final MemberRepository memberRepository;
    private final CognitoAuthClient cognitoAuthClient;

    /**
     * {@code POST /api/auth/login} 요청을 처리한다.
     *
     * @param request 로그인 요청 바디(아이디/비밀번호)
     * @return Cognito가 발급한 Access/Refresh Token과 회원 요약 정보
     * @throws BusinessException {@link ErrorCode#INVALID_CREDENTIALS} - Cognito가 아이디/비밀번호를 거부한 경우
     * @throws BusinessException {@link ErrorCode#MEMBER_NOT_FOUND} - Cognito 인증은 성공했으나 로컬 회원 레코드가 없는 경우
     */
    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        AuthenticationResultType result;
        try {
            result = cognitoAuthClient.login(request.getUsername(), request.getPassword());
        } catch (NotAuthorizedException | UserNotFoundException e) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return toTokenResponse(result, member, null);
    }

    /**
     * {@code POST /api/auth/refresh} 요청을 처리한다.
     *
     * <p>Refresh Token 자체의 검증은 Cognito({@code REFRESH_TOKEN_AUTH} 플로우)가 전담한다.
     * Cognito는 기본적으로 재발급 시 새 Refresh Token을 돌려주지 않으므로, 응답에 없으면
     * 요청에 실려온 기존 Refresh Token을 그대로 돌려준다.</p>
     *
     * @param request 리프레시 요청 바디(refreshToken)
     * @return 새로 발급된 Access Token과 회원 요약 정보
     * @throws BusinessException {@link ErrorCode#INVALID_REFRESH_TOKEN} -
     *         Cognito가 리프레시 토큰을 거부했거나, 반환된 Access Token에서 사용자를 특정할 수 없는 경우
     * @throws BusinessException {@link ErrorCode#MEMBER_NOT_FOUND} - Cognito 인증은 성공했으나 로컬 회원 레코드가 없는 경우
     */
    public AuthDto.TokenResponse refresh(AuthDto.RefreshRequest request) {
        AuthenticationResultType result;
        try {
            result = cognitoAuthClient.refresh(request.getRefreshToken());
        } catch (NotAuthorizedException | UserNotFoundException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // Cognito가 이번 호출에서 방금 발급한 토큰의 클레임을 읽는 것뿐이므로 서명 재검증이 필요 없다.
        String username = extractUsername(result.accessToken());
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        return toTokenResponse(result, member, request.getRefreshToken());
    }

    private String extractUsername(String accessToken) {
        try {
            return JWTParser.parse(accessToken).getJWTClaimsSet().getStringClaim("username");
        } catch (ParseException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private AuthDto.TokenResponse toTokenResponse(
            AuthenticationResultType result, Member member, String fallbackRefreshToken) {
        String refreshToken = result.refreshToken() != null ? result.refreshToken() : fallbackRefreshToken;

        return AuthDto.TokenResponse.builder()
                .accessToken(result.accessToken())
                .refreshToken(refreshToken)
                .tokenType(result.tokenType())
                .memberId(member.getId())
                .username(member.getUsername())
                .role(member.getRole().name())
                .build();
    }
}
