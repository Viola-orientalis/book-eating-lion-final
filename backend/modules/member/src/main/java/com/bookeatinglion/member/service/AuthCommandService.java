package com.bookeatinglion.member.service;

import com.bookeatinglion.common.exception.BusinessException;
import com.bookeatinglion.common.exception.ErrorCode;
import com.bookeatinglion.member.domain.Member;
import com.bookeatinglion.member.domain.MemberGrade;
import com.bookeatinglion.member.domain.Role;
import com.bookeatinglion.member.dto.AuthDto;
import com.bookeatinglion.member.infra.cognito.CognitoAuthClient;
import com.bookeatinglion.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UsernameExistsException;

/**
 * 인증(Auth) 도메인의 상태 변경(Command) 유스케이스를 담당하는 서비스.
 *
 * <p>회원가입은 AWS Cognito User Pool에 사용자를 즉시 활성화 상태로 생성한 뒤,
 * 로컬 {@code members} 테이블에는 자격증명(비밀번호)을 저장하지 않고 Cognito의
 * 고유 식별자(sub)만 연결해 저장한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandService {

    private final MemberRepository memberRepository;
    private final CognitoAuthClient cognitoAuthClient;

    /**
     * {@code POST /api/auth/signup} 요청을 처리한다.
     *
     * @param request 회원가입 요청 바디(아이디/비밀번호/이름/성별/나이)
     * @return 생성된 회원의 식별자와 아이디
     * @throws BusinessException {@link ErrorCode#DUPLICATE_USERNAME} - 이미 사용 중인 아이디로 가입을 시도한 경우
     */
    public AuthDto.SignupResponse signup(AuthDto.SignupRequest request) {
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        String cognitoSub;
        try {
            cognitoSub = cognitoAuthClient.createUser(request.getUsername(), request.getPassword());
        } catch (UsernameExistsException e) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        Member member = Member.builder()
                .username(request.getUsername())
                .cognitoSub(cognitoSub)
                .name(request.getName())
                .gender(request.getGender())
                .age(request.getAge())
                .role(Role.USER)
                .grade(MemberGrade.BASIC)
                .point(0L)
                .build();

        Member saved = memberRepository.save(member);

        return AuthDto.SignupResponse.builder()
                .memberId(saved.getId())
                .username(saved.getUsername())
                .build();
    }
}
