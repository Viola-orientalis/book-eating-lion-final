package com.bookeatinglion.member.service;

import com.bookeatinglion.member.domain.Gender;
import com.bookeatinglion.member.domain.Member;
import com.bookeatinglion.member.domain.Subscription;
import com.bookeatinglion.member.domain.SubscriptionStatus;
import com.bookeatinglion.member.dto.MemberResponse;
import com.bookeatinglion.member.dto.MemberSubscriptionResponse;
import com.bookeatinglion.member.dto.MemberUpdateRequest;
import com.bookeatinglion.member.exception.MemberNotFoundException;
import com.bookeatinglion.member.repository.MemberRepository;
import com.bookeatinglion.member.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private MemberService memberService;

    private void setId(Member member, Long id) throws Exception {
        Field field = Member.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(member, id);
    }

    @Test
    void 내_프로필을_조회한다() {
        Member member = Member.register("sub-1", "lion@bookeating.com", "책먹는사자");
        when(memberRepository.findByCognitoSub("sub-1")).thenReturn(Optional.of(member));

        MemberResponse response = memberService.getMyProfile("sub-1");

        assertThat(response.email()).isEqualTo("lion@bookeating.com");
        assertThat(response.name()).isEqualTo("책먹는사자");
    }

    @Test
    void 존재하지_않는_회원을_조회하면_예외를_던진다() {
        when(memberRepository.findByCognitoSub("sub-unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMyProfile("sub-unknown"))
                .isInstanceOf(MemberNotFoundException.class);
    }

    @Test
    void 프로필을_부분_수정한다() {
        Member member = Member.register("sub-1", "lion@bookeating.com", "책먹는사자");
        when(memberRepository.findByCognitoSub("sub-1")).thenReturn(Optional.of(member));

        MemberResponse response = memberService.updateProfile("sub-1",
                new MemberUpdateRequest("새이름", "010-1234-5678", Gender.MALE, LocalDate.of(2000, 1, 1)));

        assertThat(response.name()).isEqualTo("새이름");
        assertThat(response.phoneNumber()).isEqualTo("010-1234-5678");
        assertThat(response.gender()).isEqualTo(Gender.MALE);
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
    }

    @Test
    void 구독중인_회원의_구독_상태를_조회한다() throws Exception {
        Member member = Member.register("sub-1", "lion@bookeating.com", "책먹는사자");
        setId(member, 1L);
        when(memberRepository.findByCognitoSub("sub-1")).thenReturn(Optional.of(member));
        Subscription subscription = Subscription.builder()
                .memberId(1L).planName("월간 구독").monthlyPrice(9900)
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRepository.findFirstByMemberIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(subscription));

        MemberSubscriptionResponse response = memberService.getSubscription("sub-1");

        assertThat(response.subscribed()).isTrue();
        assertThat(response.planName()).isEqualTo("월간 구독");
    }

    @Test
    void 구독_이력이_없는_회원은_미구독_상태를_반환한다() throws Exception {
        Member member = Member.register("sub-1", "lion@bookeating.com", "책먹는사자");
        setId(member, 1L);
        when(memberRepository.findByCognitoSub("sub-1")).thenReturn(Optional.of(member));
        when(subscriptionRepository.findFirstByMemberIdOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.empty());

        MemberSubscriptionResponse response = memberService.getSubscription("sub-1");

        assertThat(response.subscribed()).isFalse();
    }
}
