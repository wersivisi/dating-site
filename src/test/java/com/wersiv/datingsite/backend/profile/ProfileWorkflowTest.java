package com.wersiv.datingsite.backend.profile;

import com.wersiv.datingsite.backend.DatingSiteApplication;
import com.wersiv.datingsite.backend.profile.repository.ProfileRepository;
import com.wersiv.datingsite.backend.security.UserPrincipal;
import com.wersiv.datingsite.backend.user.entity.User;
import com.wersiv.datingsite.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DatingSiteApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:profile-report;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "app.portable.enabled=false",
        "app.upload.avatar-dir=target/test-avatars",
        "app.test-data.enabled=true"
})
@ActiveProfiles("portable")
@AutoConfigureMockMvc
@Transactional
class ProfileWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ProfileRepository profiles;
    @Autowired EntityManager entityManager;
    @Autowired com.wersiv.datingsite.backend.common.config.TestDataSeeder seeder;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    UserPrincipal principal;

    @BeforeEach
    void setUp() {
        User account = new User();
        account.setEmail("report@example.test");
        account.setUsername("Report tester");
        account.setPassword("unused");
        principal = new UserPrincipal(users.saveAndFlush(account));
    }

    @Test
    void adultFormCreatesProfileAndOpensEditorInRussianLocale() throws Exception {
        LocalDate birthdate = LocalDate.now().minusYears(18);
        mvc.perform(multipart("/create-profile").with(user(principal)).with(csrf()).locale(Locale.forLanguageTag("ru"))
                        .param("handle", "report_tester").param("displayName", "Тестировщик")
                        .param("birthdate", birthdate.toString()).param("city", "Казань"))
                .andExpect(redirectedUrl("/my-profile"));
        entityManager.flush();
        entityManager.clear();
        var saved = profiles.findById(principal.getId()).orElseThrow();
        assertEquals(birthdate, saved.getBirthdate());
        assertEquals("Казань", saved.getCity());
        mvc.perform(get("/my-profile").with(user(principal)))
                .andExpect(status().isOk()).andExpect(view().name("my-profile"));
        mvc.perform(get("/create-profile").with(user(principal))).andExpect(redirectedUrl("/my-profile"));
        mvc.perform(multipart("/my-profile").with(user(principal)).with(csrf()).locale(Locale.forLanguageTag("ru"))
                        .param("displayName", "Новое имя").param("birthdate", birthdate.toString()))
                .andExpect(redirectedUrl("/my-profile"));
        entityManager.flush();
        entityManager.clear();
        assertEquals("Новое имя", profiles.findById(principal.getId()).orElseThrow().getDisplayName());
    }

    @Test
    void underageFormDoesNotCreateProfileAndIdentifiesBirthdateField() throws Exception {
        mvc.perform(multipart("/create-profile").with(user(principal)).with(csrf())
                        .param("handle", "report_tester").param("displayName", "Tester")
                        .param("birthdate", LocalDate.now().minusYears(18).plusDays(1).toString()))
                .andExpect(view().name("create-profile"))
                .andExpect(model().attributeHasFieldErrors("profile", "birthdate"));
        assertFalse(profiles.existsById(principal.getId()));
    }

    @Test
    void apiRejectsMissingUnderageAndFutureBirthdates() throws Exception {
        for (String value : new String[]{"null", "\"" + LocalDate.now().minusYears(18).plusDays(1) + "\"",
                "\"" + LocalDate.now().plusDays(1) + "\""}) {
            mvc.perform(post("/api/profiles/me").with(user(principal)).contentType("application/json")
                            .content("{\"handle\":\"report_tester\",\"displayName\":\"Tester\",\"birthdate\":" + value + "}"))
                    .andExpect(status().isBadRequest());
            assertFalse(profiles.existsById(principal.getId()));
        }
    }

    @Test
    void apiAcceptsAdultAndRejectsUnderageUpdateWithoutChangingStoredProfile() throws Exception {
        LocalDate adult = LocalDate.now().minusYears(18);
        mvc.perform(post("/api/profiles/me").with(user(principal)).contentType("application/json")
                        .content("{\"handle\":\"report_tester\",\"displayName\":\"Tester\",\"birthdate\":\"" + adult + "\"}"))
                .andExpect(status().isCreated());
        mvc.perform(put("/api/profiles/me").with(user(principal)).contentType("application/json")
                        .content("{\"displayName\":\"Changed\",\"birthdate\":\"" + adult.plusDays(1) + "\"}"))
                .andExpect(status().isBadRequest());
        entityManager.flush();
        entityManager.clear();
        var saved = profiles.findById(principal.getId()).orElseThrow();
        assertEquals(adult, saved.getBirthdate());
        assertEquals("Tester", saved.getDisplayName());
    }

    @Test
    void testAccountsHaveProfilesPhotosAndPasswordsAndAreNotOverwritten() throws Exception {
        String[][] accounts = {
                {"stasyan", "StasyaN!71Test"}, {"alina", "Alina!82Craft"},
                {"galina", "Galina!93Wash"}, {"vadim", "Vadim!64Goal"},
                {"maxik", "Maxik!55Boat"}, {"muhammad", "Muhammad!46Wolf"}, {"lera", "Lera!37Guitar"}
        };
        for (String[] account : accounts) {
            var user = users.findByEmail(account[0] + "@swipely.test").orElseThrow();
            assertTrue(passwordEncoder.matches(account[1], user.getPassword()));
            var profile = profiles.findById(user.getId()).orElseThrow();
            assertEquals("Москва", profile.getCity());
            assertFalse(profile.getBirthdate().isAfter(LocalDate.now().minusYears(18)));
            mvc.perform(get("/avatars/" + profile.getAvatarPath())).andExpect(status().isOk());
        }
        var alina = users.findByEmail("alina@swipely.test").orElseThrow();
        alina.setPassword(passwordEncoder.encode("ChangedPassword123"));
        profiles.findById(alina.getId()).orElseThrow().setBio("Changed biography");
        long userCount = users.count();
        long profileCount = profiles.count();
        seeder.run(new org.springframework.boot.DefaultApplicationArguments(new String[0]));
        entityManager.flush();
        entityManager.clear();
        assertEquals(userCount, users.count());
        assertEquals(profileCount, profiles.count());
        assertEquals("Changed biography", profiles.findById(alina.getId()).orElseThrow().getBio());
        assertTrue(passwordEncoder.matches("ChangedPassword123", users.findById(alina.getId()).orElseThrow().getPassword()));
    }
}
