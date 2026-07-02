package com.esports.platform.domain.participant.entity;

import com.esports.platform.domain.tournament.entity.Tournament;
import com.esports.platform.domain.user.entity.User;
import com.esports.platform.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "tournament_participants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TournamentParticipant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantStatus status;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Builder
    private TournamentParticipant(Tournament tournament, User user) {
        this.tournament = tournament;
        this.user = user;
        this.status = ParticipantStatus.PENDING;
        this.joinedAt = LocalDateTime.now();
    }

    public static TournamentParticipant create(Tournament tournament, User user) {
        return TournamentParticipant.builder()
                .tournament(tournament)
                .user(user)
                .build();
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public boolean isCancelled() {
        return this.status == ParticipantStatus.CANCELLED;
    }

    public void confirm() {
        this.status = ParticipantStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = ParticipantStatus.CANCELLED;
    }
}
