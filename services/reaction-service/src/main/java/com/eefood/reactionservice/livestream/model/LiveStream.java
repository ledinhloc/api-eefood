package com.eefood.reactionservice.livestream.model;

import com.eefood.reactionservice.enums.LiveStreamStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "live_streams")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LiveStream {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId; // ID của người tạo livestream

  @Column(nullable = false, unique = true)
  private String roomName; // Tên phòng LiveKit

  @Column(nullable = false)
  private String title;

//  @Column(columnDefinition = "TEXT")
//  private String description;

  private String thumbnailUrl;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private LiveStreamStatus status = LiveStreamStatus.SCHEDULED;

  @Builder.Default
  private Integer viewerCount = 0;

//  @Builder.Default
//  private Integer totalHearts = 0;

  private LocalDateTime scheduledAt;
  private LocalDateTime startedAt;
  private LocalDateTime endedAt;
  // Thông tin LiveKit
  @Column(unique = true)
  private String livekitRoomSid;

  @OneToMany(mappedBy = "liveStream", cascade = CascadeType.ALL)
  @Builder.Default
  private List<LiveComment> comments = new ArrayList<>();

  @OneToMany(mappedBy = "liveStream", cascade = CascadeType.ALL)
  @Builder.Default
  private List<LiveReaction> reactions = new ArrayList<>();

//  @OneToMany(mappedBy = "liveStream", cascade = CascadeType.ALL)
//  @Builder.Default
//  private List<LiveView> views = new ArrayList<>();
}
