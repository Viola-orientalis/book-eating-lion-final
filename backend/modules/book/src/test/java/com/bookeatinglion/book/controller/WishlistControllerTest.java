package com.bookeatinglion.book.controller;

import com.bookeatinglion.book.BookModuleTestApplication;
import com.bookeatinglion.book.exception.BookNotFoundException;
import com.bookeatinglion.book.service.WishlistService;
import com.bookeatinglion.member.domain.Member;
import com.bookeatinglion.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WishlistController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = BookModuleTestApplication.class)
class WishlistControllerTest {

    private static final String COGNITO_SUB = "cognito-sub-1";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WishlistService wishlistService;

    @MockBean
    private MemberRepository memberRepository;

    @BeforeEach
    void setUpAuthentication() throws Exception {
        Member member = member(1L, COGNITO_SUB);
        when(memberRepository.findByCognitoSub(COGNITO_SUB)).thenReturn(Optional.of(member));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", COGNITO_SUB)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private Member member(Long id, String cognitoSub) throws Exception {
        Member member = Member.builder().cognitoSub(cognitoSub).email("qa@test.com").name("QA").build();
        Field idField = Member.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(member, id);
        return member;
    }

    @Test
    void 찜하기는_200을_반환한다() throws Exception {
        mockMvc.perform(post("/api/wishlist/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(wishlistService, times(1)).addWishlist(1L, 1L);
    }

    @Test
    void 존재하지_않는_책_찜하기는_404를_반환한다() throws Exception {
        doThrow(new BookNotFoundException(999L)).when(wishlistService).addWishlist(eq(999L), eq(1L));

        mockMvc.perform(post("/api/wishlist/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 찜_삭제는_200을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/wishlist/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(wishlistService, times(1)).removeWishlist(1L, 1L);
    }
}
