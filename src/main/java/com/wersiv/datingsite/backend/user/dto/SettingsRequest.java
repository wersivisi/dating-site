package com.wersiv.datingsite.backend.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingsRequest {
    private boolean matchNotifications;
    private boolean messageNotifications;
    private boolean emailNews;
    private boolean showOnline;
    private boolean showDistance;
    private boolean paused;
}
