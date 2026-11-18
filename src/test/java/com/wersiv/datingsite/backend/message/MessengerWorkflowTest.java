package com.wersiv.datingsite.backend.message;

import com.wersiv.datingsite.backend.DatingSiteApplication;
import com.wersiv.datingsite.backend.security.UserPrincipal;
import com.wersiv.datingsite.backend.user.entity.User;
import com.wersiv.datingsite.backend.user.repository.UserRepository;
import com.wersiv.datingsite.backend.message.repository.MessageRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DatingSiteApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:messenger;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "app.portable.enabled=false", "app.test-data.enabled=false"
})
@ActiveProfiles("portable")
@AutoConfigureMockMvc
@Transactional
class MessengerWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired MessageRepository messages;
    @Autowired EntityManager entityManager;
    UserPrincipal alice;
    UserPrincipal bob;
    UserPrincipal stranger;

    @BeforeEach
    void setUp() {
        alice = account("Alice");
        bob = account("Bob");
        stranger = account("Stranger");
    }

    private UserPrincipal account(String name) {
        User user = new User();
        user.setUsername(name);
        user.setEmail(name.toLowerCase() + "@example.test");
        user.setPassword("unused");
        return new UserPrincipal(users.saveAndFlush(user));
    }

    private void send(UserPrincipal sender, UserPrincipal recipient, String text, int expectedStatus) throws Exception {
        mvc.perform(post("/api/messages").with(user(sender)).contentType("application/json")
                        .content("{\"receiverId\":" + recipient.getId() + ",\"content\":\"" + text + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void realAccountsWithoutProfilesCanExchangePersistedMessages() throws Exception {
        mvc.perform(get("/api/messages/conversations").with(user(alice))).andExpect(jsonPath("$", hasSize(0)));
        send(alice, bob, "Hello from the database", 201);
        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/api/messages/conversations").with(user(bob)))
                .andExpect(jsonPath("$[0].displayName", is("Alice")))
                .andExpect(jsonPath("$[0].lastMessage", is("Hello from the database")));
        mvc.perform(get("/api/messages/with/" + bob.getId() + "/state").with(user(alice)))
                .andExpect(jsonPath("$.recipient.canSend", is(false)))
                .andExpect(jsonPath("$.messages", hasSize(1)));
        send(alice, bob, "Too soon", 403);
        send(bob, alice, "Reply", 201);
        send(alice, bob, "Thanks", 201);
        mvc.perform(get("/api/messages/with/" + bob.getId() + "/state").with(user(alice)))
                .andExpect(jsonPath("$.recipient.canSend", is(true)))
                .andExpect(jsonPath("$.messages", hasSize(3)));
        mvc.perform(get("/conversation").param("userId", bob.getId().toString()).with(user(alice)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("/js/messaging.js")))
                .andExpect(content().string(containsString("Hello from the database")));
        assertEquals(3, messages.count());
    }

    @Test
    void anotherAccountCannotSeeSomeoneElsesConversation() throws Exception {
        send(alice, bob, "Private message", 201);
        mvc.perform(get("/api/messages/conversations").with(user(stranger))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/messages/with/" + bob.getId() + "/state").with(user(stranger)))
                .andExpect(jsonPath("$.messages", hasSize(0)));
        mvc.perform(get("/api/messages/conversations")).andExpect(status().isUnauthorized());
    }

    @Test
    void blockStopsPollingAndSendingAndHidesConversation() throws Exception {
        send(alice, bob, "Hello", 201);
        mvc.perform(post("/api/friends/" + alice.getId() + "/block").with(user(bob))).andExpect(status().isOk());
        for (UserPrincipal viewer : new UserPrincipal[]{alice, bob}) {
            Long other = viewer == alice ? bob.getId() : alice.getId();
            mvc.perform(get("/api/messages/with/" + other + "/state").with(user(viewer))).andExpect(status().isForbidden());
            mvc.perform(get("/api/messages/with/" + other).with(user(viewer))).andExpect(status().isForbidden());
            mvc.perform(get("/api/messages/conversations").with(user(viewer))).andExpect(jsonPath("$", hasSize(0)));
        }
        send(alice, bob, "Blocked", 403);
        assertEquals(1, messages.count());
    }

    @Test
    void invalidMessagesDoNotCreateRowsAndContentIsEscapedInHtml() throws Exception {
        send(alice, bob, "   ", 400);
        send(alice, alice, "Self", 403);
        send(alice, bob, "<script>alert(1)</script>", 201);
        mvc.perform(get("/conversation").param("userId", bob.getId().toString()).with(user(alice)))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
        assertEquals(1, messages.count());
    }

    @Test
    void landingCtaUsesAuthenticationAndKeepsDemoCharacters() throws Exception {
        String guest = mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String signedIn = mvc.perform(get("/").with(user(alice))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(guest.matches("(?s).*<a\\b(?=[^>]*\\bid=\"find-match\")(?=[^>]*\\bhref=\"/login\")[^>]*>.*"));
        assertTrue(signedIn.matches("(?s).*<a\\b(?=[^>]*\\bid=\"find-match\")(?=[^>]*\\bhref=\"/discover\")[^>]*>.*"));
        assertTrue(guest.contains("Снайпер Дед"));
        assertTrue(signedIn.contains("Мой профиль"));
        assertFalse(signedIn.contains(">Регистрация</a>"));
        mvc.perform(get("/js/messaging.js")).andExpect(status().isOk());
    }
}
