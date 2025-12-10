package com.eefood.recipeservice.util;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
@Slf4j
public class ImageUtils {
  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  /**
   * Download image từ URL và encode sang base64
   */
  public static String downloadAndEncodeImage(String imageUrl) {
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(imageUrl))
        .GET()
        .build();

      HttpResponse<byte[]> response = client.send(request,
        HttpResponse.BodyHandlers.ofByteArray());

      if (response.statusCode() == 200) {
        return Base64.getEncoder().encodeToString(response.body());
      }

      log.warn("Failed to download image: {} - Status: {}", imageUrl, response.statusCode());
      return null;

    } catch (Exception e) {
      log.error("Error downloading image {}: {}", imageUrl, e.getMessage());
      return null;
    }
  }

  /**
   * Xác định MIME type từ URL
   */
  public static String getMimeType(String url) {
    String lower = url.toLowerCase();
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
    if (lower.endsWith(".png")) return "image/png";
    if (lower.endsWith(".webp")) return "image/webp";
    if (lower.endsWith(".gif")) return "image/gif";
    return "image/jpeg"; // default
  }

}
