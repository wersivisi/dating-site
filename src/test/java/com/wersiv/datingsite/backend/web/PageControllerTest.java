package com.wersiv.datingsite.backend.web;

import com.wersiv.datingsite.backend.friend.entity.Friendship;
import com.wersiv.datingsite.backend.friend.entity.FriendshipStatus;
import com.wersiv.datingsite.backend.friend.service.FriendshipService;
import com.wersiv.datingsite.backend.message.service.MessageService;
import com.wersiv.datingsite.backend.profile.dto.ProfileResponse;
import com.wersiv.datingsite.backend.profile.exception.ProfileNotFoundException;
import com.wersiv.datingsite.backend.profile.service.ProfileService;
import com.wersiv.datingsite.backend.security.CustomUserDetailsService;
import com.wersiv.datingsite.backend.security.SecurityConfig;
import com.wersiv.datingsite.backend.security.UserPrincipal;
import com.wersiv.datingsite.backend.user.dto.SettingsRequest;
import com.wersiv.datingsite.backend.user.entity.User;
import com.wersiv.datingsite.backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PageController.class)
@Import(SecurityConfig.class)
class PageControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean UserService users;
    @MockitoBean ProfileService profiles;
    @MockitoBean FriendshipService friends;
    @MockitoBean MessageService messages;
    @MockitoBean com.wersiv.datingsite.backend.message.service.ConversationService conversations;
    @MockitoBean CustomUserDetailsService userDetails;
    UserPrincipal principal;

    @BeforeEach
    void setUp() {
        User account = new User();
        account.setId(1L);
        account.setEmail("test@example.com");
        account.setUsername("Tester");
        account.setPassword("encoded");
        principal = new UserPrincipal(account);
        when(profiles.getByUserId(anyLong())).thenAnswer(call -> ProfileResponse.builder()
                .userId(call.getArgument(0)).handle("tester").displayName("Tester")
                .birthdate(LocalDate.of(1990, 1, 1)).build());
        when(users.getUserById(1L)).thenReturn(new com.wersiv.datingsite.backend.user.dto.UserResponse());
        when(users.getSettings(1L)).thenReturn(new SettingsRequest());
        when(conversations.list(1L)).thenReturn(List.of());
        when(conversations.state(eq(1L), anyLong())).thenAnswer(call ->
                new com.wersiv.datingsite.backend.message.dto.ConversationState(1L,
                        new com.wersiv.datingsite.backend.message.dto.ConversationResponse(call.getArgument(1), "Tester", null, null, null, "NEW", true), List.of()));
        when(friends.getRelationships(1L)).thenReturn(List.of());
        when(profiles.discover(anyLong(), anyString(), anyString(), anyString())).thenReturn(List.of());
    }

    @Test
    void publicPagesAndLegacyAliasesRenderWithoutLogin() throws Exception {
        for (String page : List.of("index", "login", "register", "help", "safety", "privacy", "terms",
                "forgot-password", "verify-email", "reset-password", "reset-success")) {
            for (String suffix : List.of("", ".html")) {
                mvc.perform(get("/" + page + suffix).servletPath("/" + page + suffix))
                        .andExpect(status().isOk()).andExpect(view().name(page))
                        .andExpect(content().string(containsString("/css/styles.css")));
            }
        }
        mvc.perform(get("/css/styles.css")).andExpect(status().isOk());
    }

    @Test
    void privatePagesRedirectGuestsToLogin() throws Exception {
        for (String page : List.of("discover", "likes", "messages", "conversation", "profile", "my-profile", "create-profile", "settings", "states")) {
            mvc.perform(get("/" + page)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
        mvc.perform(get("/api/profiles/me").servletPath("/api/profiles/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedTemplatesRenderWithEmptyAndPopulatedData() throws Exception {
        for (String page : List.of("discover", "likes", "messages", "profile", "my-profile", "settings", "states")) {
            mvc.perform(get("/" + page).servletPath("/" + page).with(user(principal)))
                    .andExpect(status().isOk()).andExpect(view().name(page));
        }
        mvc.perform(get("/conversation").param("userId", "2").with(user(principal)))
                .andExpect(status().isOk()).andExpect(view().name("conversation"));
        when(friends.getRelationships(1L)).thenReturn(List.of(new Friendship(1L, 2L, FriendshipStatus.ACCEPTED)));
        when(conversations.list(1L)).thenReturn(List.of(
                new com.wersiv.datingsite.backend.message.dto.ConversationResponse(2L, "Tester", null, null, null, "ACCEPTED", true)));
        mvc.perform(get("/likes").with(user(principal))).andExpect(status().isOk())
                .andExpect(content().string(containsString("/conversation?userId=2")));
        mvc.perform(get("/conversation").param("userId", "2").with(user(principal)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void friendLayoutUsesRealProfileDetailsAndRelationshipCounts() throws Exception {
        var profile = ProfileResponse.builder().userId(2L).displayName("Real Person")
                .birthdate(LocalDate.now().minusYears(25).minusDays(1))
                .interests("Кино, , Книги, Кино").goal("friendship").idealEvening("Вечер с книгой").build();
        when(profiles.getByUserId(2L)).thenReturn(profile);
        when(profiles.discover(anyLong(), anyString(), anyString(), anyString())).thenReturn(List.of(profile));
        when(friends.getRelationships(1L)).thenReturn(List.of(
                new Friendship(1L, 2L, FriendshipStatus.ACCEPTED),
                new Friendship(3L, 1L, FriendshipStatus.PENDING),
                new Friendship(1L, 4L, FriendshipStatus.PENDING),
                new Friendship(1L, 5L, FriendshipStatus.BLOCKED)));
        mvc.perform(get("/discover").with(user(principal))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Real Person, 25")))
                .andExpect(content().string(containsString("Найдено анкет: 1")))
                .andExpect(content().string(containsString("/friends/2/like")))
                .andExpect(content().string(containsString("✦ Кино")));
        mvc.perform(get("/profile").param("userId", "2").with(user(principal))).andExpect(status().isOk())
                .andExpect(content().string(containsString("Вечер с книгой")))
                .andExpect(content().string(containsString("Дружбу и общение")));
        mvc.perform(get("/likes").with(user(principal))).andExpect(status().isOk())
                .andExpect(model().attribute("mutualCount", 1L))
                .andExpect(model().attribute("incomingCount", 1L))
                .andExpect(model().attribute("outgoingCount", 1L))
                .andExpect(content().string(containsString("Взаимные · 1")));
    }

    @Test
    void missingProfileOpensCreationAndInvalidFormDoesNotSave() throws Exception {
        when(profiles.getByUserId(1L)).thenThrow(new ProfileNotFoundException("1"));
        mvc.perform(get("/my-profile").with(user(principal))).andExpect(redirectedUrl("/create-profile"));
        mvc.perform(get("/create-profile").with(user(principal))).andExpect(status().isOk());
        mvc.perform(post("/create-profile").with(user(principal)).with(csrf())
                        .param("displayName", "Tester").param("handle", "tester").param("birthdate", "2020-01-01"))
                .andExpect(status().isOk()).andExpect(view().name("create-profile")).andExpect(model().attributeExists("error"));
        verify(profiles, never()).createProfile(anyLong(), any());
    }

    @Test
    void formsValidateAndRequireCsrf() throws Exception {
        mvc.perform(post("/register").with(csrf()).param("username", "tester").param("email", "test@example.com")
                        .param("password", "password123"))
                .andExpect(view().name("register")).andExpect(model().attributeExists("error"));
        verify(users, never()).createUser(any());
        mvc.perform(post("/register").with(csrf()).param("username", "tester").param("email", "test@example.com")
                        .param("password", "password123").param("adult", "true").param("terms", "true"))
                .andExpect(redirectedUrl("/login?registered"));
        mvc.perform(post("/friends/2/like").with(user(principal))).andExpect(status().isForbidden());
        mvc.perform(post("/friends/2/like").with(user(principal)).with(csrf())).andExpect(redirectedUrl("/likes"));
        verify(friends).like(1L, 2L);
        mvc.perform(post("/conversation").with(user(principal)).with(csrf()).param("receiverId", "2").param("content", "Hello"))
                .andExpect(redirectedUrl("/conversation?userId=2"));
        verify(messages).sendMessage(eq(1L), argThat(r -> r.getContent().equals("Hello") && r.getReceiverId().equals(2L)));
    }

    @Test
    void failuresRenderHtmlAndUnavailableMailDoesNotClaimSuccess() throws Exception {
        mvc.perform(get("/profile").param("userId", "invalid").with(user(principal)))
                .andExpect(status().isBadRequest()).andExpect(view().name("page-error"));
        when(profiles.getByUserId(2L)).thenThrow(new ProfileNotFoundException("2"));
        mvc.perform(get("/profile").param("userId", "2").with(user(principal)))
                .andExpect(status().isNotFound()).andExpect(view().name("page-error"));
        mvc.perform(post("/forgot-password").servletPath("/forgot-password").with(csrf()).param("email", "test@example.com"))
                .andExpect(view().name("forgot-password")).andExpect(model().attributeExists("error"));
    }

    @Test
    void loginCreatesSessionAndLogoutEndsIt() throws Exception {
        principal.getUser().setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("password123"));
        when(userDetails.loadUserByUsername("test@example.com")).thenReturn(principal);
        var result = mvc.perform(post("/login").with(csrf()).param("email", "test@example.com").param("password", "password123"))
                .andExpect(redirectedUrl("/my-profile")).andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) result.getRequest().getSession(false);
        org.junit.jupiter.api.Assertions.assertNotNull(session);
        mvc.perform(get("/my-profile").session(session)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(redirectedUrl("/login?logout"));
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
    }

    @Test
    void profileAndSettingsFormsPassAllEditableFieldsToServices() throws Exception {
        mvc.perform(multipart("/my-profile").with(user(principal)).with(csrf())
                        .param("displayName", "New name").param("birthdate", "1991-02-03")
                        .param("city", "Moscow").param("interests", "Music").param("goal", "friendship")
                        .param("idealEvening", "Walking").param("bio", "Hello").param("preferences", "Everyone"))
                .andExpect(redirectedUrl("/my-profile"));
        verify(profiles).updateProfile(eq(1L), argThat(p -> p.getDisplayName().equals("New name")
                && p.getBirthdate().equals(LocalDate.of(1991, 2, 3)) && p.getInterests().equals("Music")
                && p.getIdealEvening().equals("Walking")));
        mvc.perform(post("/settings").with(user(principal)).with(csrf()).param("paused", "on"))
                .andExpect(redirectedUrl("/settings"));
        verify(users).updateSettings(eq(1L), argThat(p -> p.isPaused() && !p.isEmailNews()));
    }

    @Test
    void incomingLikesBlockedSettingsAndValidationErrorsRender() throws Exception {
        Friendship incoming = new Friendship(2L, 1L, FriendshipStatus.PENDING);
        when(friends.getRelationships(1L)).thenReturn(List.of(incoming));
        mvc.perform(get("/likes").param("tab", "incoming").with(user(principal)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/friends/2/accept")));
        incoming.setStatus(FriendshipStatus.BLOCKED);
        incoming.setBlockedBy(1L);
        mvc.perform(get("/settings").with(user(principal))).andExpect(status().isOk())
                .andExpect(content().string(containsString("/friends/2/unblock")));
        mvc.perform(post("/my-profile").with(user(principal)).with(csrf()).param("displayName", ""))
                .andExpect(status().isOk()).andExpect(view().name("my-profile")).andExpect(model().attributeExists("error"));
    }
}
