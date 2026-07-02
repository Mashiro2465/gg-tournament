package com.esports.platform.domain.payment.repository;

import com.esports.platform.domain.payment.entity.Payment;
import com.esports.platform.domain.payment.entity.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(String orderId);

    List<Payment> findByUserId(Long userId);

    Optional<Payment> findByTournamentIdAndUserIdAndStatus(Long tournamentId, Long userId, PaymentStatus status);
}
