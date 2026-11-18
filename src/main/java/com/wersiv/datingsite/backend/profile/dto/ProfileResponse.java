package com.wersiv.datingsite.backend.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponse {
    private Long userId;
    private String handle;
    private String displayName;
    private String bio;

    private java.time.LocalDate birthdate;

    private String city;

    private String gender;

    private String interests;

    private String goal;

    private String idealEvening;

    private String preferences;
    private String avatarUrl;

    @com.fasterxml.jackson.annotation.JsonIgnore
    public Integer getAge() {
        return birthdate == null ? null : java.time.Period.between(birthdate, java.time.LocalDate.now()).getYears();
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public java.util.List<String> getInterestList() {
        if (interests == null || interests.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(interests.split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).distinct().toList();
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getGoalLabel() {
        if (goal == null) return "";
        return switch (goal) {
            case "relationship" -> "Отношения";
            case "friendship" -> "Дружбу и общение";
            case "unsure" -> "Пока не знаю";
            default -> goal;
        };
    }
}
