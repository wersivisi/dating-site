package com.wersiv.datingsite.backend.common.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
@ConditionalOnProperty(name = "app.portable.enabled", havingValue = "true")
public class PortableConsole {
    @EventListener(ApplicationReadyEvent.class)
    public void ready(ApplicationReadyEvent event) {
        ConfigurableApplicationContext context = event.getApplicationContext();
        String port = context.getEnvironment().getProperty("local.server.port", "8080");
        System.out.println("\nSwipely is ready: http://localhost:" + port);
        System.out.println("Type exit and press Enter, or press Ctrl+C to stop.\n");
        Thread console = new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.strip().equalsIgnoreCase("exit") || line.strip().equals("выход")) {
                        context.close();
                        return;
                    }
                }
            } catch (IOException exception) {
                System.err.println("Console input unavailable. Use Ctrl+C to stop.");
            }
        }, "portable-console");
        console.setDaemon(true);
        console.start();
    }
}
