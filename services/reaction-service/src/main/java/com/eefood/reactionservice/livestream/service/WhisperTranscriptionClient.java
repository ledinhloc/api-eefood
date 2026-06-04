package com.eefood.reactionservice.livestream.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class WhisperTranscriptionClient {

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${whisper.base-url}")
  private String whisperBaseUrl;
  //gọi Whisper
  public String transcribe(byte[] audioBytes, String fileName, String contentType, String language) {
    if (audioBytes == null || audioBytes.length == 0) {
      throw new IllegalArgumentException("Audio bytes are required");
    }

    HttpHeaders fileHeaders = new HttpHeaders();
    fileHeaders.setContentType(parseContentType(contentType));
    fileHeaders.setContentDisposition(
      ContentDisposition.formData()
        .name("file")
        .filename(fileName)
        .build()
    );

    ByteArrayResource resource = new ByteArrayResource(audioBytes) {
      @Override
      public String getFilename() {
        return fileName;
      }
    };

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", new HttpEntity<>(resource, fileHeaders));
    body.add("language", language);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    ResponseEntity<String> response = restTemplate.postForEntity(
      whisperBaseUrl + "/inference",
      new HttpEntity<>(body, headers),
      String.class
    );

    return extractText(response.getBody());
  }
  
  //chuẩn hóa content type file
  private MediaType parseContentType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
    return MediaType.parseMediaType(contentType);
  }

  //bóc text từ response Whisper
  private String extractText(String responseBody) {
    if (responseBody == null || responseBody.isBlank()) {
      return "";
    }

    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode textNode = root.get("text");
      if (textNode == null || textNode.isNull()) {
        return responseBody.trim();
      }

      return textNode.asText("").trim();
    } catch (IOException e) {
      throw new RuntimeException("Cannot parse whisper response", e);
    }
  }
}
