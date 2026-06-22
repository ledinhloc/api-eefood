package com.eefood.reactionservice.livestream.service;

import com.eefood.reactionservice.livestream.dto.request.LiveSubtitleTranscriptRequest;
import com.eefood.reactionservice.livestream.dto.response.LiveAudioTranscriptionResponse;
import com.eefood.reactionservice.livestream.dto.ws.LiveSubtitleMessage;
import com.eefood.reactionservice.livestream.enums.SubtitleLanguage;
import com.eefood.reactionservice.livestream.model.LiveStream;
import com.eefood.reactionservice.livestream.repository.LiveStreamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LiveSubtitleService {
  private final LiveStreamRepository liveStreamRepository;
  private final WhisperTranscriptionClient whisperTranscriptionClient;
  private final LiveSubtitlePreferenceService subtitlePreferenceService;

  public LiveAudioTranscriptionResponse transcribe(Long liveStreamId, MultipartFile audioFile, String language) {
    if (audioFile == null || audioFile.isEmpty()) {
      throw new IllegalArgumentException("Audio file is required");
    }

    try {
      LiveStream liveStream = liveStreamRepository.findById(liveStreamId)
        .orElseThrow(() -> new RuntimeException("Live stream not found"));

      String spokenLanguage = resolveSpokenLanguage(language, liveStream);
      String text = whisperTranscriptionClient.transcribe(
        audioFile.getBytes(),
        audioFile.getOriginalFilename(),
        audioFile.getContentType(),
        spokenLanguage
      );
      broadcastSubtitle(liveStreamId, spokenLanguage, text, LocalDateTime.now());

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

  public LiveSubtitleMessage publishTranscript(LiveSubtitleTranscriptRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Subtitle transcript request is required");
    }
    if (request.getLiveStreamId() == null) {
      throw new IllegalArgumentException("Live stream id is required");
    }
    if (request.getText() == null || request.getText().isBlank()) {
      throw new IllegalArgumentException("Subtitle text is required");
    }

    LiveStream liveStream = liveStreamRepository.findById(request.getLiveStreamId())
      .orElseThrow(() -> new RuntimeException("Live stream not found"));

    String spokenLanguage = resolveSpokenLanguage(request.getSpokenLanguage(), liveStream);
    String targetLanguage = request.getTargetLanguage() == null || request.getTargetLanguage().isBlank()
      ? spokenLanguage
      : SubtitleLanguage.fromCode(request.getTargetLanguage()).getCode();
    LocalDateTime createdAt = request.getCreatedAt() == null ? LocalDateTime.now() : request.getCreatedAt();

    return broadcastSubtitle(request.getLiveStreamId(), targetLanguage, request.getText().trim(), createdAt);
  }

  private String resolveSpokenLanguage(String language, LiveStream liveStream) {
    String code = language;
    if (code == null || code.isBlank()) {
      SubtitleLanguage spokenLanguage = liveStream.getSpokenLanguage();
      code = spokenLanguage == null ? null : spokenLanguage.getCode();
    }
    return SubtitleLanguage.fromCode(code).getCode();
  }

  private LiveSubtitleMessage broadcastSubtitle(
    Long liveStreamId,
    String targetLanguage,
    String text,
    LocalDateTime createdAt
  ) {
    LiveSubtitleMessage message = LiveSubtitleMessage.builder()
      .liveStreamId(liveStreamId)
      .targetLanguage(targetLanguage)
      .text(text)
      .createdAt(createdAt)
      .build();

    subtitlePreferenceService.sendToSubscribers(liveStreamId, targetLanguage, message);
    return message;
  }
}
