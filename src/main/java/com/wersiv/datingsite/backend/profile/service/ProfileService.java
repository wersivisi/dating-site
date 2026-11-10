package com.wersiv.datingsite.backend.profile.service;

import com.wersiv.datingsite.backend.profile.dto.CreateProfileRequest;
import com.wersiv.datingsite.backend.profile.dto.ProfileResponse;
import com.wersiv.datingsite.backend.profile.dto.UpdateProfileRequest;
import com.wersiv.datingsite.backend.profile.entity.Profile;
import com.wersiv.datingsite.backend.profile.exception.HandleAlreadyTakenException;
import com.wersiv.datingsite.backend.profile.exception.ProfileAlreadyExistsException;
import com.wersiv.datingsite.backend.profile.exception.ProfileNotFoundException;
import com.wersiv.datingsite.backend.profile.repository.ProfileRepository;
import com.wersiv.datingsite.backend.user.entity.User;
import com.wersiv.datingsite.backend.user.exception.UserNotFoundException;
import com.wersiv.datingsite.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@Transactional
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final String avatarDir;

    public ProfileService(ProfileRepository profileRepository,
                           UserRepository userRepository,
                           @Value("${app.upload.avatar-dir}") String avatarDir) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.avatarDir = avatarDir;
    }

    public ProfileResponse createProfile(Long userId, CreateProfileRequest request) {
        if (profileRepository.existsById(userId)) {
            throw new ProfileAlreadyExistsException(userId);
        }
        if (profileRepository.existsByHandle(request.getHandle())) {
            throw new HandleAlreadyTakenException(request.getHandle());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Profile profile = new Profile(user, request.getHandle(), request.getDisplayName(),
                request.getBio(), request.getPreferences());
        copyDetails(profile, request.getBirthdate(), request.getCity(), request.getGender(), request.getInterests(), request.getGoal(), request.getIdealEvening());
        Profile saved = profileRepository.save(profile);

        return toResponse(saved);
    }

    public ProfileResponse getByUserId(Long userId) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException(String.valueOf(userId)));
        return toResponse(profile);
    }

    public ProfileResponse getByHandle(String handle) {
        Profile profile = profileRepository.findByHandle(handle)
                .orElseThrow(() -> new ProfileNotFoundException("@" + handle));
        return toResponse(profile);
    }

    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException(String.valueOf(userId)));

        profile.setDisplayName(request.getDisplayName());
        profile.setBio(request.getBio());
        profile.setPreferences(request.getPreferences());
        copyDetails(profile, request.getBirthdate(), request.getCity(), request.getGender(), request.getInterests(), request.getGoal(), request.getIdealEvening());

        return toResponse(profile);
    }

    public ProfileResponse uploadAvatar(Long userId, MultipartFile file) {
        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ProfileNotFoundException(String.valueOf(userId)));

        try {
            Path dir = Path.of(avatarDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String extension = extractExtension(file.getOriginalFilename());
            String fileName = "user-" + userId + "-" + UUID.randomUUID() + extension;
            Path target = dir.resolve(fileName);
            file.transferTo(target);

            profile.setAvatarPath(fileName);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store avatar", e);
        }

        return toResponse(profile);
    }

    private void copyDetails(Profile profile, java.time.LocalDate birthdate, String city, String gender,
                             String interests, String goal, String idealEvening) {
        profile.setBirthdate(birthdate);
        profile.setCity(city);
        profile.setGender(gender);
        profile.setInterests(interests);
        profile.setGoal(goal);
        profile.setIdealEvening(idealEvening);
    }

    public java.util.List<ProfileResponse> discover(Long userId, String query, String city, String goal) {
        String search = query.toLowerCase(java.util.Locale.ROOT).trim();
        return profileRepository.findAll().stream()
                .filter(p -> !p.getUserId().equals(userId) && !p.getUser().isPaused())
                .filter(p -> city.isBlank() || city.equals(p.getCity()))
                .filter(p -> goal.isBlank() || goal.equals(p.getGoal()))
                .filter(p -> (p.getDisplayName() + " " + p.getHandle() + " " + p.getCity() + " " + p.getInterests())
                        .toLowerCase(java.util.Locale.ROOT).contains(search))
                .map(this::toResponse).toList();
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }

    private ProfileResponse toResponse(Profile profile) {
        String avatarUrl = profile.getAvatarPath() != null ? "/avatars/" + profile.getAvatarPath() : null;
        return ProfileResponse.builder()
                .userId(profile.getUserId())
                .handle(profile.getHandle())
                .displayName(profile.getDisplayName())
                .bio(profile.getBio())
                .preferences(profile.getPreferences())
                .birthdate(profile.getBirthdate())
                .city(profile.getCity())
                .gender(profile.getGender())
                .interests(profile.getInterests())
                .goal(profile.getGoal())
                .idealEvening(profile.getIdealEvening())
                .avatarUrl(avatarUrl)
                .build();
    }
}
