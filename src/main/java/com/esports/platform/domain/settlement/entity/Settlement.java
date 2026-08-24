package com.esports.platform.domain.settlement.entity;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "placement_rank", nullable = false)
    private int rank;

    @Column(name = "prize_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal prizeAmount;

    @Column(name = "platform_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal platformFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Builder
    private Settlement(Tournament tournament, User user, int rank, BigDecimal prizeAmount, BigDecimal platformFee) {
        this.tournament = tournament;
        this.user = user;
        this.rank = rank;
        this.prizeAmount = prizeAmount;
        this.platformFee = platformFee;
        this.status = SettlementStatus.PENDING;
    }

    public static Settlement create(Tournament tournament, User user, int rank, BigDecimal prizeAmount, BigDecimal platformFee) {
        return Settlement.builder()
                .tournament(tournament)
                .user(user)
                .rank(rank)
                .prizeAmount(prizeAmount)
                .platformFee(platformFee)
                .build();
    }

    public boolean isPending() {
        return this.status == SettlementStatus.PENDING;
    }

    public void complete() {
        this.status = SettlementStatus.COMPLETED;
        this.settledAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = SettlementStatus.FAILED;
    }
}
