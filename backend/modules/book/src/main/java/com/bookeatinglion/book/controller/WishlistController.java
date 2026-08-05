package com.bookeatinglion.book.controller;

import com.bookeatinglion.book.service.WishlistService;
import com.bookeatinglion.common.dto.ApiResponse;
import com.bookeatinglion.common.security.SecurityUtils;
import com.bookeatinglion.member.exception.MemberNotFoundException;
import com.bookeatinglion.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final MemberRepository memberRepository;

    @PostMapping("/{bookId}")
    public ApiResponse<Void> addWishlist(@PathVariable Long bookId) {
        wishlistService.addWishlist(bookId, currentMemberId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{bookId}")
    public ApiResponse<Void> removeWishlist(@PathVariable Long bookId) {
        wishlistService.removeWishlist(bookId, currentMemberId());
        return ApiResponse.success(null);
    }

    private Long currentMemberId() {
        String memberSub = SecurityUtils.currentMemberSub();
        return memberRepository.findByCognitoSub(memberSub)
                .orElseThrow(() -> new MemberNotFoundException(memberSub))
                .getId();
    }
}
