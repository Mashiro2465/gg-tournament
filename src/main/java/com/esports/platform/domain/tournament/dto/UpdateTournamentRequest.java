package com.esports.platform.domain.tournament.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record UpdateTournamentRequest(
        @NotBlank(message = "대회명은 필수입니다.") @Size(max = 200, message = "대회명은 200자 이하여야 합니다.")
        String title,

        @NotBlank(message = "종목은 필수입니다.") @Size(max = 50, message = "종목은 50자 이하여야 합니다.")
        String gameType,

        @Min(value = 2, message = "참가 인원은 최소 2명 이상이어야 합니다.")
        int maxParticipants,

        String prizeStructure,

        @NotNull(message = "참가 마감일은 필수입니다.")
        LocalDateTime registrationDeadline,

        @NotNull(message = "대회 시작일은 필수입니다.")
        LocalDateTime startAt
) {
}
