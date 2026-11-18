package com.wersiv.datingsite.backend.web;

import com.wersiv.datingsite.backend.friend.entity.Friendship;
import com.wersiv.datingsite.backend.friend.entity.FriendshipStatus;
import com.wersiv.datingsite.backend.friend.service.FriendshipService;
import com.wersiv.datingsite.backend.message.dto.SendMessageRequest;
import com.wersiv.datingsite.backend.message.service.MessageService;
import com.wersiv.datingsite.backend.profile.dto.*;
import com.wersiv.datingsite.backend.profile.exception.ProfileNotFoundException;
import com.wersiv.datingsite.backend.profile.service.ProfileService;
import com.wersiv.datingsite.backend.security.UserPrincipal;
import com.wersiv.datingsite.backend.user.dto.CreateUserRequest;
import com.wersiv.datingsite.backend.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class PageController {
    private final UserService users;
    private final ProfileService profiles;
    private final FriendshipService friends;
    private final MessageService messages;
    private final com.wersiv.datingsite.backend.message.service.ConversationService conversations;

    @ModelAttribute("signedIn")
    public boolean signedIn(@AuthenticationPrincipal UserPrincipal principal) {
        return principal != null;
    }

    @GetMapping({"/", "/index", "/index.html", "/login", "/login.html", "/register", "/register.html",
            "/help", "/help.html", "/safety", "/safety.html", "/privacy", "/privacy.html",
            "/terms", "/terms.html", "/forgot-password", "/forgot-password.html", "/verify-email", "/verify-email.html",
            "/reset-password", "/reset-password.html", "/reset-success", "/reset-success.html", "/states", "/states.html"})
    public String page(HttpServletRequest request, @AuthenticationPrincipal UserPrincipal principal, Model model) {
        String page = request.getServletPath().replace("/", "").replace(".html", "");
        if (page.isEmpty()) page = "index";
        if (principal != null && Set.of("login", "register").contains(page)) return "redirect:/my-profile";
        if (page.equals("register")) model.addAttribute("registration", new CreateUserRequest());
        return page;
    }

    @PostMapping({"/register", "/register.html"})
    public String register(@Valid @ModelAttribute("registration") CreateUserRequest form, BindingResult errors,
                           @RequestParam(defaultValue = "false") boolean adult,
                           @RequestParam(defaultValue = "false") boolean terms, Model model) {
        if (!adult || !terms) errors.reject("consent", "Подтвердите совершеннолетие и согласие с условиями.");
        if (form.getPassword() != null && form.getPassword().length() < 8) errors.reject("password", "Пароль должен содержать не менее 8 символов.");
        if (errors.hasErrors()) return validation("register", errors, model);
        users.createUser(form);
        return "redirect:/login?registered";
    }

    @GetMapping({"/create-profile", "/create-profile.html"})
    public String createProfile(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        if (ownProfile(principal) != null) return "redirect:/my-profile";
        model.addAttribute("profile", new CreateProfileRequest());
        return "create-profile";
    }

    @PostMapping({"/create-profile", "/create-profile.html"})
    public String createProfile(@AuthenticationPrincipal UserPrincipal principal,
                                @Valid @ModelAttribute("profile") CreateProfileRequest form, BindingResult errors,
                                @RequestParam(required = false) MultipartFile photo, Model model, RedirectAttributes flash) {
        validatePhoto(photo, errors);
        if (errors.hasErrors()) return validation("create-profile", errors, model);
        profiles.createProfile(principal.getId(), form);
        savePhoto(principal, photo, flash);
        return "redirect:/my-profile";
    }

    @GetMapping({"/my-profile", "/my-profile.html"})
    public String myProfile(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        ProfileResponse profile = ownProfile(principal);
        if (profile == null) return "redirect:/create-profile";
        model.addAttribute("profile", profile);
        return "my-profile";
    }

    @PostMapping({"/my-profile", "/my-profile.html"})
    public String updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                @Valid @ModelAttribute("profile") UpdateProfileRequest form, BindingResult errors,
                                @RequestParam(required = false) MultipartFile photo, Model model, RedirectAttributes flash) {
        ProfileResponse current = ownProfile(principal);
        if (current == null) return "redirect:/create-profile";
        validatePhoto(photo, errors);
        if (errors.hasErrors()) {
            BeanUtils.copyProperties(form, current);
            model.addAttribute("profile", current);
            return validation("my-profile", errors, model);
        }
        profiles.updateProfile(principal.getId(), form);
        savePhoto(principal, photo, flash);
        flash.addFlashAttribute("notice", "Профиль сохранён.");
        return "redirect:/my-profile";
    }

    @GetMapping({"/discover", "/discover.html"})
    public String discover(@AuthenticationPrincipal UserPrincipal principal,
                           @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String city,
                           @RequestParam(defaultValue = "") String goal, Model model) {
        if (ownProfile(principal) == null) return "redirect:/create-profile";
        Set<Long> blocked = friends.getRelationships(principal.getId()).stream()
                .filter(f -> f.getStatus() == FriendshipStatus.BLOCKED).map(f -> other(f, principal.getId())).collect(Collectors.toSet());
        model.addAttribute("profiles", profiles.discover(principal.getId(), q, city, goal).stream()
                .filter(p -> !blocked.contains(p.getUserId())).toList());
        model.addAttribute("q", q);
        model.addAttribute("city", city);
        model.addAttribute("goal", goal);
        return "discover";
    }

    @GetMapping({"/profile", "/profile.html"})
    public String profile(@AuthenticationPrincipal UserPrincipal principal, @RequestParam(required = false) Long userId, Model model) {
        Long id = userId == null ? principal.getId() : userId;
        if (id.equals(principal.getId()) && ownProfile(principal) == null) return "redirect:/create-profile";
        model.addAttribute("profile", profiles.getByUserId(id));
        model.addAttribute("self", id.equals(principal.getId()));
        model.addAttribute("relationship", friends.getRelationships(principal.getId()).stream()
                .filter(f -> other(f, principal.getId()).equals(id)).findFirst().orElse(null));
        return "profile";
    }

    @GetMapping({"/likes", "/likes.html"})
    public String likes(@AuthenticationPrincipal UserPrincipal principal, @RequestParam(defaultValue = "mutual") String tab, Model model) {
        List<Friendship> relationships = friends.getRelationships(principal.getId());
        model.addAttribute("mutualCount", relationships.stream().filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED).count());
        model.addAttribute("incomingCount", relationships.stream().filter(f -> f.getStatus() == FriendshipStatus.PENDING
                && f.getAddresseeId().equals(principal.getId())).count());
        model.addAttribute("outgoingCount", relationships.stream().filter(f -> f.getStatus() == FriendshipStatus.PENDING
                && f.getRequesterId().equals(principal.getId())).count());
        model.addAttribute("tab", tab);
        model.addAttribute("profiles", relatedProfiles(relationships.stream().filter(f -> switch (tab) {
            case "incoming" -> f.getStatus() == FriendshipStatus.PENDING && f.getAddresseeId().equals(principal.getId());
            case "outgoing" -> f.getStatus() == FriendshipStatus.PENDING && f.getRequesterId().equals(principal.getId());
            default -> f.getStatus() == FriendshipStatus.ACCEPTED;
        }).toList(), principal.getId()));
        return "likes";
    }

    @PostMapping("/friends/{userId}/{action:like|accept|block|unblock}")
    public String relationship(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long userId,
                               @PathVariable String action) {
        users.getUserById(userId);
        switch (action) {
            case "like" -> friends.like(principal.getId(), userId);
            case "accept" -> friends.accept(principal.getId(), userId);
            case "block" -> friends.block(principal.getId(), userId);
            case "unblock" -> friends.unblock(principal.getId(), userId);
        }
        return action.equals("block") || action.equals("unblock") ? "redirect:/settings#blocked" : "redirect:/likes";
    }

    @GetMapping({"/messages", "/messages.html", "/conversation", "/conversation.html"})
    public String messages(@AuthenticationPrincipal UserPrincipal principal, @RequestParam(required = false) Long userId,
                           Model model) {
        model.addAttribute("conversations", conversations.list(principal.getId()));
        model.addAttribute("currentUserId", principal.getId());
        if (userId == null) return "messages";
        var state = conversations.state(principal.getId(), userId);
        model.addAttribute("recipient", state.recipient());
        model.addAttribute("messages", state.messages());
        model.addAttribute("canSend", state.recipient().canSend());
        return "conversation";
    }

    @PostMapping({"/conversation", "/conversation.html"})
    public String send(@AuthenticationPrincipal UserPrincipal principal, @Valid @ModelAttribute SendMessageRequest form,
                       BindingResult errors, Model model) {
        if (errors.hasErrors()) {
            String view = messages(principal, form.getReceiverId(), model);
            return validation(view, errors, model);
        }
        messages.sendMessage(principal.getId(), form);
        return "redirect:/conversation?userId=" + form.getReceiverId();
    }

    @GetMapping({"/settings", "/settings.html"})
    public String settings(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("settings", users.getSettings(principal.getId()));
        model.addAttribute("account", users.getUserById(principal.getId()));
        model.addAttribute("profiles", relatedProfiles(friends.getRelationships(principal.getId()).stream()
                .filter(f -> f.getStatus() == FriendshipStatus.BLOCKED && principal.getId().equals(f.getBlockedBy())).toList(), principal.getId()));
        return "settings";
    }

    @PostMapping({"/settings", "/settings.html"})
    public String saveSettings(@AuthenticationPrincipal UserPrincipal principal,
                               @ModelAttribute com.wersiv.datingsite.backend.user.dto.SettingsRequest settings,
                               RedirectAttributes flash) {
        users.updateSettings(principal.getId(), settings);
        flash.addFlashAttribute("notice", "Настройки сохранены.");
        return "redirect:/settings";
    }

    @PostMapping({"/forgot-password", "/forgot-password.html", "/reset-password", "/reset-password.html", "/help", "/help.html"})
    public String unavailable(HttpServletRequest request, Model model) {
        model.addAttribute("error", "Отправка почты пока не настроена. Попробуйте позже.");
        return request.getServletPath().replace("/", "").replace(".html", "");
    }

    private ProfileResponse ownProfile(UserPrincipal principal) {
        try { return profiles.getByUserId(principal.getId()); }
        catch (ProfileNotFoundException ex) { return null; }
    }

    private List<ProfileResponse> relatedProfiles(List<Friendship> relationships, Long userId) {
        return relationships.stream().map(f -> {
            Long id = other(f, userId);
            try { return profiles.getByUserId(id); }
            catch (ProfileNotFoundException ex) {
                return ProfileResponse.builder().userId(id).displayName("Пользователь " + id).build();
            }
        }).toList();
    }

    private Long other(Friendship relationship, Long userId) {
        return relationship.getRequesterId().equals(userId) ? relationship.getAddresseeId() : relationship.getRequesterId();
    }

    private String validation(String view, BindingResult errors, Model model) {
        model.addAttribute("error", errors.getAllErrors().stream().map(e -> e.getDefaultMessage()).collect(Collectors.joining(" ")));
        return view;
    }

    private void validatePhoto(MultipartFile photo, BindingResult errors) {
        if (photo != null && !photo.isEmpty() && (photo.getSize() > 5 * 1024 * 1024
                || !Set.of("image/jpeg", "image/png").contains(photo.getContentType() == null ? "" : photo.getContentType()))) {
            errors.reject("photo", "Выберите JPG или PNG размером до 5 МБ.");
        }
    }

    private void savePhoto(UserPrincipal principal, MultipartFile photo, RedirectAttributes flash) {
        if (photo != null && !photo.isEmpty()) {
            try { profiles.uploadAvatar(principal.getId(), photo); }
            catch (java.io.UncheckedIOException ex) { flash.addFlashAttribute("error", "Анкета сохранена, но фото загрузить не удалось. Попробуйте ещё раз."); }
        }
    }
}
