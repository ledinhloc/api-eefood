package com.eefood.reactionservice.util;

import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class PromptLoader {
    public String load(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load prompt: " + path, e);
        }
    }
}
