package com.wersiv.datingsite.backend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

//local stored avatars through avatars/***
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.avatar-dir}")
    private String avatarDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:" + normalizeDir(avatarDir));
    }

    private String normalizeDir(String dir) {
        return dir.endsWith("/") ? dir : dir + "/";
    }
}
