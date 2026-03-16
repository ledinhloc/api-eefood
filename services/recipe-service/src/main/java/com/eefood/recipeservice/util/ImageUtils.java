package com.eefood.recipeservice.util;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
@Slf4j
public class ImageUtils {
  private static final HttpClient CLIENT = HttpClient.newHttpClient();
  private static final int MAX_IMAGE_DIMENSION = 512;
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

  public static String downloadAndEncodeImageLight(String imageUrl) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
              .uri(URI.create(imageUrl))
              .header("User-Agent", "Mozilla/5.0")
              .header("Accept", "image/*")
              .header("Referer", imageUrl)
              .GET()
              .build();

      HttpResponse<byte[]> response =
              CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

      if (response.statusCode() != 200) {
        log.warn("Failed to download image: {} - Status: {}", imageUrl, response.statusCode());
        return null;
      }

      byte[] compressed = compressImage(response.body(), MAX_IMAGE_DIMENSION);
      return Base64.getEncoder().encodeToString(compressed);

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

  private static byte[] compressImage(byte[] original, int maxDim) throws IOException {
    BufferedImage img = ImageIO.read(new ByteArrayInputStream(original));
    if (img == null) return original;

    int w = img.getWidth(), h = img.getHeight();
    if (w <= maxDim && h <= maxDim) return original;

    double scale = (double) maxDim / Math.max(w, h);
    int newW = (int) (w * scale);
    int newH = (int) (h * scale);

    Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
    BufferedImage output = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
    output.getGraphics().drawImage(scaled, 0, 0, null);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(output, "JPEG", baos);
    return baos.toByteArray();
  }

}
