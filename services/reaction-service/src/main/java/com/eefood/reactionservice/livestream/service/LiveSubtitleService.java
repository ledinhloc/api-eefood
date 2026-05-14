package com.eefood.reactionservice.livestream.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eefood.reactionservice.livestream.dto.response.LiveAudioTranscriptionResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class LiveSubtitleService {

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${whisper.base-url}")
  private String whisperBaseUrl;

  public LiveAudioTranscriptionResponse transcribe(Long liveStreamId, MultipartFile audioFile, String language) {
    if (audioFile == null || audioFile.isEmpty()) {
      throw new IllegalArgumentException("Audio file is required");
    }

    try {
      HttpHeaders fileHeaders = new HttpHeaders();
      fileHeaders.setContentType(parseContentType(audioFile.getContentType()));
      fileHeaders.setContentDisposition(
        ContentDisposition.formData()
          .name("file")
          .filename(audioFile.getOriginalFilename())
          .build()
      );

      ByteArrayResource resource = new ByteArrayResource(audioFile.getBytes()) {
        @Override
        public String getFilename() {
          return audioFile.getOriginalFilename();
        }
      };

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("file", new HttpEntity<>(resource, fileHeaders));
      body.add("language", normalizeLanguage(language));

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      ResponseEntity<String> response = restTemplate.postForEntity(
        whisperBaseUrl + "/inference",
        new HttpEntity<>(body, headers),
        String.class
      );

      return LiveAudioTranscriptionResponse.builder()
        .liveStreamId(liveStreamId)
        .fileName(audioFile.getOriginalFilename())
        .contentType(audioFile.getContentType())
        .text(extractText(response.getBody()))
        .build();
    } catch (IOException e) {
      throw new RuntimeException("Cannot read uploaded audio file", e);
    }
  }

  private MediaType parseContentType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return MediaType.APPLICATION_OCTET_STREAM;
    }
    return MediaType.parseMediaType(contentType);
  }

  private String normalizeLanguage(String language) {
    if (language == null || language.isBlank()) {
      return "vi";
    }
    return language.trim().toLowerCase();
  }

  private String extractText(String responseBody) throws IOException {
    if (responseBody == null || responseBody.isBlank()) {
      return "";
    }

    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode textNode = root.get("text");
    if (textNode == null || textNode.isNull()) {
      return responseBody.trim();
    }

    return textNode.asText("").trim();
  }
}
