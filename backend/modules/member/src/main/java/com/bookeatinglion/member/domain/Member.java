package com.bookeatinglion.member.domain;

import com.bookeatinglion.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서비스 전체 회원 계정을 나타내는 JPA 엔티티.
 *
 * <p>{@code /docs/테이블 상세 역할 정리표.md}에 정의된 {@code members} 테이블에 대응하며,
 * 로그인 아이디와 AWS Cognito 연결 식별자(cognitoSub), 프로필(이름/성별/나이), 인가({@link Role}),
 * 멤버십 등급({@link MemberGrade})과 보유 포인트를 함께 관리한다. 자격증명(비밀번호) 자체는
 * 저장하지 않으며 AWS Cognito User Pool이 전담한다.</p>
 *
 * <p>다른 엔티티들과 마찬가지로 JPA 프록시 생성을 위한 기본 생성자는 {@code protected}로 제한하고,
 * 실제 객체 생성은 {@link Builder}를 통해서만 하도록 강제한다.</p>
 */
@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인에 사용되는 고유 아이디. */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** AWS Cognito User Pool 상의 고유 식별자(sub). 자격증명은 Cognito가 전담하며, 이 값으로만 연결된다. */
    @Column(nullable = false, unique = true, length = 100)
    private String cognitoSub;

    /** 회원 이름(실명 또는 닉네임). */
    @Column(nullable = false, length = 100)
    private String name;

    /** 성별. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    /** 나이. */
    @Column
    private Integer age;

    /** 시스템 상의 권한(USER/ADMIN). Spring Security 인가 규칙에 사용된다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role;

    /** 서비스 상의 멤버십 등급(BASIC/PREMIUM). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberGrade grade;

    /** 보유 포인트. */
    @Column(nullable = false)
    private long point;

    /**
     * 회원 엔티티를 생성한다. 회원가입 시(Auth 도메인)에서만 호출되어야 하며,
     * 그 외 필드 변경은 {@link #updateProfile(String, Gender, Integer)}처럼
     * 의도가 드러나는 전용 메서드를 통해서만 이루어져야 한다.
     *
     * @param username   로그인 아이디
     * @param cognitoSub AWS Cognito User Pool 상의 고유 식별자(sub)
     * @param name       회원 이름
     * @param gender     성별
     * @param age        나이
     * @param role       시스템 권한(가입 직후에는 기본적으로 {@link Role#USER})
     * @param grade      멤버십 등급(가입 직후에는 기본적으로 {@link MemberGrade#BASIC})
     * @param point      초기 보유 포인트(가입 직후에는 기본적으로 0)
     */
    @Builder
    private Member(String username, String cognitoSub, String name, Gender gender, Integer age,
                   Role role, MemberGrade grade, long point) {
        this.username = username;
        this.cognitoSub = cognitoSub;
        this.name = name;
        this.gender = gender;
        this.age = age;
        this.role = role;
        this.grade = grade;
        this.point = point;
    }

    /**
     * {@code PATCH /api/members/me} 요청을 처리하기 위한 부분 업데이트 메서드.
     * 각 파라미터가 {@code null}이면 해당 필드는 기존 값을 유지한다(부분 수정 시맨틱).
     *
     * @param name   변경할 이름. 유지하려면 {@code null}
     * @param gender 변경할 성별. 유지하려면 {@code null}
     * @param age    변경할 나이. 유지하려면 {@code null}
     */
    public void updateProfile(String name, Gender gender, Integer age) {
        if (name != null) {
            this.name = name;
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (age != null) {
            this.age = age;
        }
    }
}
