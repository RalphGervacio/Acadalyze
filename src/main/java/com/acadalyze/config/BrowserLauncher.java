package com.acadalyze.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

@Component
public class BrowserLauncher implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI("http://localhost:8080/auth/login")); 
        } else {
            System.err.println("Desktop not supported. Can't open browser.");
        }
    }
}
