package com.esports.platform.domain.bracket.dto;

import jakarta.validation.constraints.NotNull;

public record RecordMatchResultRequest(
        @NotNull(message = "승자 참가자 ID는 필수입니다.")
        Long winnerParticipantId
) {
}
