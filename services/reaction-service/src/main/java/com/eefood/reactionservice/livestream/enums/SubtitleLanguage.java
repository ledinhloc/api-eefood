package com.eefood.reactionservice.livestream.enums;

import java.util.Arrays;

public enum SubtitleLanguage {
  VI("vi"),
  EN("en");

  private final String code;

  SubtitleLanguage(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static SubtitleLanguage fromCode(String code) {
    if (code == null || code.isBlank()) {
      return VI;
    }

    return Arrays.stream(values())
      .filter(language -> language.code.equalsIgnoreCase(code.trim()))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Unsupported subtitle language: " + code));
  }
}
