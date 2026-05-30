package com.eefood.reactionservice.livestream.model;

import com.eefood.reactionservice.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "livestream_gift_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class LivestreamGiftLog extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long senderId;

    @Column(nullable = false)
    private Long receiverId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livestream_id")
    private LiveStream liveStream;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_gift_item_id")
    private LiveGiftItem liveGiftItem;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false)
    private Long totalDiamondSpent;

    @Column(nullable = false)
    private Long hostDiamondReceived;
}
