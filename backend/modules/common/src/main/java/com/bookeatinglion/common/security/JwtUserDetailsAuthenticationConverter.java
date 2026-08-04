package com.bookeatinglion.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * AWS Cognito가 발급한 Access Token(서명 검증은 Spring OAuth2 Resource Server가
 * JWKS로 이미 완료한 뒤)의 클레임을 우리 서비스의 {@link UserDetails}로 매핑하는 컨버터.
 *
 * <p>이 클래스는 서명/만료를 검증하지 않는다 — 검증이 끝난 {@link Jwt}만 넘겨받는다.
 * {@code UserDetailsService}는 인터페이스에만 의존하므로 특정 도메인(Member)에
 * 결합되지 않고 {@code common} 모듈에 위치할 수 있다. 실제 구현체({@code MyUserDetailsService})는
 * {@code member} 모듈에서 제공한다.</p>
 */
@RequiredArgsConstructor
public class JwtUserDetailsAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserDetailsService userDetailsService;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String username = jwt.getClaimAsString("username");
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, jwt, userDetails.getAuthorities());
    }
}
