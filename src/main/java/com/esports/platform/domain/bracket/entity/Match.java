package com.esports.platform.domain.bracket.entity;

import com.esports.platform.domain.participant.entity.TournamentParticipant;
import com.esports.platform.domain.tournament.entity.Tournament;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(nullable = false)
    private int round;

    @Column(name = "match_number", nullable = false)
    private int matchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant1_id")
    private TournamentParticipant participant1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant2_id")
    private TournamentParticipant participant2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private TournamentParticipant winner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status;

    @Column(name = "played_at")
    private LocalDateTime playedAt;

    @Builder
    private Match(
            Tournament tournament,
            int round,
            int matchNumber,
            TournamentParticipant participant1,
            TournamentParticipant participant2
    ) {
        this.tournament = tournament;
        this.round = round;
        this.matchNumber = matchNumber;
        this.participant1 = participant1;
        this.participant2 = participant2;
        this.status = MatchStatus.SCHEDULED;
    }

    public static Match createScheduled(
            Tournament tournament,
            int round,
            int matchNumber,
            TournamentParticipant participant1,
            TournamentParticipant participant2
    ) {
        return Match.builder()
                .tournament(tournament)
                .round(round)
                .matchNumber(matchNumber)
                .participant1(participant1)
                .participant2(participant2)
                .build();
    }

    // 참가자 수가 2의 거듭제곱이 아니어서 부전승이 발생하는 경우, 상대 없이 즉시 승자가 확정된 경기를 생성한다.
    public static Match createBye(Tournament tournament, int round, int matchNumber, TournamentParticipant participant) {
        Match match = Match.builder()
                .tournament(tournament)
                .round(round)
                .matchNumber(matchNumber)
                .participant1(participant)
                .build();
        match.status = MatchStatus.BYE;
        match.winner = participant;
        match.playedAt = LocalDateTime.now();
        return match;
    }

    public boolean isBye() {
        return this.status == MatchStatus.BYE;
    }

    public boolean isFinished() {
        return this.status == MatchStatus.FINISHED || this.status == MatchStatus.BYE;
    }

    public boolean hasParticipant(Long participantId) {
        return (this.participant1 != null && this.participant1.getId().equals(participantId))
                || (this.participant2 != null && this.participant2.getId().equals(participantId));
    }

    // 이전 라운드 경기의 승자를 이 경기의 빈 슬롯에 배정한다. 각 슬롯은 정확히 하나의
    // 이전 라운드 경기 결과로만 채워지므로(대진표 구조상 보장됨), 이미 채워진 슬롯을
    // 다시 채우는 상황은 발생하지 않는다.
    public void assignParticipant(TournamentParticipant participant, boolean intoFirstSlot) {
        if (intoFirstSlot) {
            this.participant1 = participant;
        } else {
            this.participant2 = participant;
        }
    }

    public void recordResult(TournamentParticipant winner) {
        if (this.isFinished()) {
            throw new BusinessException(ErrorCode.MATCH_ALREADY_FINISHED);
        }
        if (!hasParticipant(winner.getId())) {
            throw new BusinessException(ErrorCode.MATCH_INVALID_WINNER);
        }
        this.winner = winner;
        this.status = MatchStatus.FINISHED;
        this.playedAt = LocalDateTime.now();
    }
}
