package com.wersiv.datingsite.backend.profile.controller;

import com.wersiv.datingsite.backend.profile.dto.CreateProfileRequest;
import com.wersiv.datingsite.backend.profile.dto.ProfileResponse;
import com.wersiv.datingsite.backend.profile.dto.UpdateProfileRequest;
import com.wersiv.datingsite.backend.profile.service.ProfileService;
import com.wersiv.datingsite.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/me")
    public ResponseEntity<ProfileResponse> createMyProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                             @Valid @RequestBody CreateProfileRequest request) {
        ProfileResponse response = profileService.createProfile(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(profileService.getByUserId(principal.getId()));
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateMyProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                             @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(principal.getId(), request));
    }

    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    public ResponseEntity<ProfileResponse> uploadAvatar(@AuthenticationPrincipal UserPrincipal principal,
                                                          @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(profileService.uploadAvatar(principal.getId(), file));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ProfileResponse> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(profileService.getByUserId(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<ProfileResponse> searchByHandle(@RequestParam String handle) {
        return ResponseEntity.ok(profileService.getByHandle(handle));
    }
}
