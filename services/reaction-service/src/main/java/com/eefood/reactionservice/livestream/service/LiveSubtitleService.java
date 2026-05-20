package com.eefood.reactionservice.livestream.service;

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

  private void broadcastSubtitle(Long liveStreamId, String spokenLanguage, String text) {
    subtitlePreferenceService.sendToSubscribers(
      liveStreamId,
      spokenLanguage,
      LiveSubtitleMessage.builder()
        .liveStreamId(liveStreamId)
        .targetLanguage(spokenLanguage)
        .text(text)
        .createdAt(LocalDateTime.now())
        .build()
    );
  }
}
