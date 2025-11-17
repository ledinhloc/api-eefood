package com.eefood.reactionservice.model;

import com.eefood.reactionservice.enums.StoryMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "story_setting")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class StorySetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private StoryMode mode;

    @ElementCollection
    @CollectionTable(name = "story_setting_whitelist", joinColumns = @JoinColumn(name = "setting_id"))
    @Column(name = "allowed_user_id")
    private List<Long> allowedUserIds;

    @ElementCollection
    @CollectionTable(name = "story_setting_blacklist", joinColumns = @JoinColumn(name = "setting_id"))
    @Column(name = "blocked_user_id")
    private List<Long> blockedUserIds;
}
