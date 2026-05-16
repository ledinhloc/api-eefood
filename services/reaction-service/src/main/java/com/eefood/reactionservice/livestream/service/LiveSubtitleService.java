package com.eefood.reactionservice.livestream.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.eefood.reactionservice.livestream.dto.response.LiveAudioTranscriptionResponse;
import com.eefood.reactionservice.livestream.dto.ws.LiveSubtitleMessage;
import com.eefood.reactionservice.livestream.enums.SubtitleLanguage;
import com.eefood.reactionservice.livestream.model.LiveStream;
import com.eefood.reactionservice.livestream.repository.LiveStreamRepository;
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
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LiveSubtitleService {

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final LiveStreamRepository liveStreamRepository;
  private final LiveSubtitlePreferenceService subtitlePreferenceService;

  @Value("${whisper.base-url}")
  private String whisperBaseUrl;

  public LiveAudioTranscriptionResponse transcribe(Long liveStreamId, MultipartFile audioFile, String language) {
    if (audioFile == null || audioFile.isEmpty()) {
      throw new IllegalArgumentException("Audio file is required");
    }

    try {
      LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
        .orElseThrow(() -> new RuntimeException("Live stream not found"));

      String spokenLanguage = resolveSpokenLanguage(language, liveStream);

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
      body.add("language", spokenLanguage);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      ResponseEntity<String> response = restTemplate.postForEntity(
        whisperBaseUrl + "/inference",
        new HttpEntity<>(body, headers),
        String.class
      );

      String text = extractText(response.getBody());
      broadcastSubtitle(liveStreamId, spokenLanguage, text);

      return LiveAudioTranscriptionResponse.builder()
        .liveStreamId(liveStreamId)
        .fileName(audioFile.getOriginalFilename())
        .contentType(audioFile.getContentType())
        .spokenLanguage(spokenLanguage)
        .text(text)
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

  private String resolveSpokenLanguage(String language, LiveStream liveStream) {
    if (language != null && !language.isBlank()) {
      return SubtitleLanguage.fromCode(language).getCode();
    }

    SubtitleLanguage spokenLanguage = liveStream.getSpokenLanguage();
    if (spokenLanguage == null) {
      return SubtitleLanguage.VI.getCode();
    }

    return spokenLanguage.getCode();
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

  private void broadcastSubtitle(
    Long liveStreamId,
    String spokenLanguage,
    String text
  ) {
    LiveSubtitleMessage message = LiveSubtitleMessage.builder()
      .liveStreamId(liveStreamId)
      .targetLanguage(spokenLanguage)
      .text(text)
      .createdAt(LocalDateTime.now())
      .build();

    subtitlePreferenceService.sendToSubscribers(liveStreamId, spokenLanguage, message);
  }
}
