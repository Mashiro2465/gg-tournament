package com.esports.platform.domain.payment.entity;

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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(columnNames = "order_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Builder
    private Payment(User user, Tournament tournament, String orderId, BigDecimal amount) {
        this.user = user;
        this.tournament = tournament;
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
    }

    public static Payment create(User user, Tournament tournament, String orderId, BigDecimal amount) {
        return Payment.builder()
                .user(user)
                .tournament(tournament)
                .orderId(orderId)
                .amount(amount)
                .build();
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    public boolean isConfirmed() {
        return this.status == PaymentStatus.CONFIRMED;
    }

    public void confirm(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.CONFIRMED;
        this.paidAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
    }

    public void refund() {
        this.status = PaymentStatus.REFUNDED;
        this.refundedAt = LocalDateTime.now();
    }
}
