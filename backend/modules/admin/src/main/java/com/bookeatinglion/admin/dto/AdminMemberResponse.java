package com.bookeatinglion.admin.dto;

import com.bookeatinglion.member.domain.Member;
import com.bookeatinglion.member.domain.Role;

import java.time.LocalDateTime;

public record AdminMemberResponse(
        Long id,
        String email,
        String name,
        String phoneNumber,
        Role role,
        int point,
        LocalDateTime createdAt
) {
    public static AdminMemberResponse from(Member member) {
        return new AdminMemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getPhoneNumber(),
                member.getRole(),
                member.getPoint(),
                member.getCreatedAt()
        );
    }
}
