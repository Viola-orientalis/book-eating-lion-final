package com.bookeatinglion.member.service;

import com.bookeatinglion.member.domain.Member;
import com.bookeatinglion.member.dto.MemberResponse;
import com.bookeatinglion.member.dto.MemberSubscriptionResponse;
import com.bookeatinglion.member.dto.MemberUpdateRequest;
import com.bookeatinglion.member.exception.MemberNotFoundException;
import com.bookeatinglion.member.repository.MemberRepository;
import com.bookeatinglion.member.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;

    public MemberResponse getMyProfile(String cognitoSub) {
        return MemberResponse.from(getMember(cognitoSub));
    }

    @Transactional
    public MemberResponse updateProfile(String cognitoSub, MemberUpdateRequest request) {
        Member member = getMember(cognitoSub);
        member.updateProfile(request.name(), request.phoneNumber(), request.gender(), request.birthDate());
        return MemberResponse.from(member);
    }

    public MemberSubscriptionResponse getSubscription(String cognitoSub) {
        Member member = getMember(cognitoSub);
        return MemberSubscriptionResponse.from(
                subscriptionRepository.findFirstByMemberIdOrderByCreatedAtDesc(member.getId()).orElse(null));
    }

    private Member getMember(String cognitoSub) {
        return memberRepository.findByCognitoSub(cognitoSub)
                .orElseThrow(() -> new MemberNotFoundException(cognitoSub));
    }
}
