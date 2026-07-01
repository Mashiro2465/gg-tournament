package com.esports.platform.domain.tournament.entity;

import com.esports.platform.domain.user.entity.User;
import com.esports.platform.global.entity.BaseTimeEntity;
import com.esports.platform.global.exception.BusinessException;
import com.esports.platform.global.exception.ErrorCode;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tournaments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tournament extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "game_type", nullable = false, length = 50)
    private String gameType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TournamentFormat format;

    @Column(name = "max_participants", nullable = false)
    private int maxParticipants;

    @Column(name = "current_participants", nullable = false)
    private int currentParticipants;

    @Column(name = "entry_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal entryFee;

    @Column(name = "prize_pool", nullable = false, precision = 10, scale = 2)
    private BigDecimal prizePool;

    @Column(name = "prize_structure", columnDefinition = "json")
    private String prizeStructure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TournamentStatus status;

    @Column(name = "registration_deadline", nullable = false)
    private LocalDateTime registrationDeadline;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Builder
    private Tournament(
            User host,
            String title,
            String gameType,
            TournamentFormat format,
            int maxParticipants,
            BigDecimal entryFee,
            String prizeStructure,
            LocalDateTime registrationDeadline,
            LocalDateTime startAt
    ) {
        this.host = host;
        this.title = title;
        this.gameType = gameType;
        this.format = format;
        this.maxParticipants = maxParticipants;
        this.currentParticipants = 0;
        this.entryFee = entryFee;
        this.prizePool = BigDecimal.ZERO;
        this.prizeStructure = prizeStructure;
        this.status = TournamentStatus.RECRUITING;
        this.registrationDeadline = registrationDeadline;
        this.startAt = startAt;
    }

    public static Tournament create(
            User host,
            String title,
            String gameType,
            TournamentFormat format,
            int maxParticipants,
            BigDecimal entryFee,
            String prizeStructure,
            LocalDateTime registrationDeadline,
            LocalDateTime startAt
    ) {
        return Tournament.builder()
                .host(host)
                .title(title)
                .gameType(gameType)
                .format(format)
                .maxParticipants(maxParticipants)
                .entryFee(entryFee)
                .prizeStructure(prizeStructure)
                .registrationDeadline(registrationDeadline)
                .startAt(startAt)
                .build();
    }

    public boolean isHost(Long userId) {
        return this.host.getId().equals(userId);
    }

    public void updateDetails(
            String title,
            String gameType,
            int maxParticipants,
            String prizeStructure,
            LocalDateTime registrationDeadline,
            LocalDateTime startAt
    ) {
        this.title = title;
        this.gameType = gameType;
        this.maxParticipants = maxParticipants;
        this.prizeStructure = prizeStructure;
        this.registrationDeadline = registrationDeadline;
        this.startAt = startAt;
    }

    public void increaseParticipantCount() {
        if (this.currentParticipants >= this.maxParticipants) {
            throw new BusinessException(ErrorCode.TOURNAMENT_CAPACITY_EXCEEDED);
        }
        this.currentParticipants++;
    }

    public void decreaseParticipantCount() {
        this.currentParticipants = Math.max(0, this.currentParticipants - 1);
    }

    public void close() {
        this.status = TournamentStatus.CLOSED;
    }

    public void start() {
        this.status = TournamentStatus.IN_PROGRESS;
    }

    public void finish() {
        this.status = TournamentStatus.FINISHED;
    }

    public void cancel() {
        this.status = TournamentStatus.CANCELLED;
    }
}
