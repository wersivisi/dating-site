package com.wersiv.datingsite.backend.common.config;

import com.wersiv.datingsite.backend.profile.entity.Profile;
import com.wersiv.datingsite.backend.profile.repository.ProfileRepository;
import com.wersiv.datingsite.backend.user.entity.User;
import com.wersiv.datingsite.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.test-data.enabled", havingValue = "true")
public class TestDataSeeder implements ApplicationRunner {
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PasswordEncoder passwords;
    private final Path avatarDirectory;

    public TestDataSeeder(UserRepository users, ProfileRepository profiles, PasswordEncoder passwords,
                          @Value("${app.upload.avatar-dir}") String avatarDirectory) {
        this.users = users;
        this.profiles = profiles;
        this.passwords = passwords;
        this.avatarDirectory = Path.of(avatarDirectory).toAbsolutePath().normalize().resolve("testpersons");
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    public void run(ApplicationArguments arguments) throws IOException {
        for (Seed seed : SEEDS) {
            if (users.existsByEmail(seed.email()) || profiles.existsByHandle(seed.handle())) continue;
            Files.createDirectories(avatarDirectory);
            Path photo = avatarDirectory.resolve(seed.photo());
            if (!Files.exists(photo)) {
                try (var input = new ClassPathResource("test-data/avatars/" + seed.photo()).getInputStream()) {
                    Files.copy(input, photo);
                }
            }
            User user = new User();
            user.setEmail(seed.email());
            user.setUsername(seed.name());
            user.setPassword(passwords.encode(seed.password()));
            user = users.save(user);
            Profile profile = new Profile(user, seed.handle(), seed.name(), seed.bio(), seed.preferences());
            profile.setBirthdate(LocalDate.parse(seed.birthdate()));
            profile.setCity("Москва");
            profile.setGender(seed.gender());
            profile.setAvatarPath("testpersons/" + seed.photo());
            profiles.save(profile);
        }
    }

    private record Seed(String handle, String name, String birthdate, String gender, String preferences,
                        String bio, String photo, String password) {
        String email() { return handle + "@swipely.test"; }
    }

    private static final List<Seed> SEEDS = List.of(
            new Seed("stasyan", "Стасян🔥🔥🔥", "2001-07-12", "Мужчина", "Женщин", "пригоняю китайцев - будешь моей дорожной спутницей, дам в рот (пивка, я не пошляк 🔥🔥).", "stasyan.jpg", "StasyaN!71Test"),
            new Seed("alina", "Алина", "2006-03-22", "Женщина", "Мужчин", "нормисы мимо играю в майнкрафт, могу в рб SWAGA найдись котик и подари мне шайбу стань моим соулмейтом", "alina.png", "Alina!82Craft"),
            new Seed("galina", "Галина", "1968-05-07", "Женщина", "Мужчин", "продаю стиральную машину не пишите по поводу знакомств", "galina.jpg", "Galina!93Wash"),
            new Seed("vadim", "Вадим", "2007-10-24", "Мужчина", "Всех", "ем детей играю в футбол красивый душа компании и света давай целоваться 💋", "vadik.jpg", "Vadim!64Goal"),
            new Seed("maxik", "Maxik", "2004-12-16", "Мужчина", "Женщин", "эй красотка хорошая погодка была бы лодка мы бы плыли с тобой, ты моё солнце меня греешь, пиши в рб: MAXiM1220", "max.jpg", "Maxik!55Boat"),
            new Seed("muhammad", "Мухаммад", "2000-08-02", "Не указывать", "Женщин", "мне было плохо зай, теперь мне будет похуй, знай 🐺🐺🖕", "muhammad.jpg", "Muhammad!46Wolf"),
            new Seed("lera", "лерчк", "2005-01-01", "Не указывать", "Мужчин", "увлекаюсь косплеями играю на гитаре и не против познакомиться но только в своем городе, тг: @lera1010", "lera.png", "Lera!37Guitar")
    );
}
