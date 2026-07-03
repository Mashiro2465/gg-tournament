package com.esports.platform.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    KAKAO_LOGIN_FAILED(HttpStatus.BAD_GATEWAY, "카카오 로그인에 실패했습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다."),

    // Tournament
    TOURNAMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "대회를 찾을 수 없습니다."),
    TOURNAMENT_NOT_HOST(HttpStatus.FORBIDDEN, "대회 주최자만 수행할 수 있는 작업입니다."),
    TOURNAMENT_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "이미 마감된 대회입니다."),
    TOURNAMENT_CAPACITY_EXCEEDED(HttpStatus.BAD_REQUEST, "참가 정원이 초과되었습니다."),

    // Participant
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "참가 정보를 찾을 수 없습니다."),
    PARTICIPANT_ALREADY_JOINED(HttpStatus.CONFLICT, "이미 참가 신청한 대회입니다."),
    PARTICIPANT_LOCK_FAILED(HttpStatus.CONFLICT, "참가 신청 처리 중입니다. 잠시 후 다시 시도해주세요."),

    // Payment
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 정보를 찾을 수 없습니다."),
    PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다."),
    PAYMENT_ALREADY_PROCESSED(HttpStatus.CONFLICT, "이미 처리된 결제입니다."),
    PAYMENT_NOT_CONFIRMED(HttpStatus.BAD_REQUEST, "확정되지 않은 결제는 환불할 수 없습니다."),
    PAYMENT_CONFIRM_FAILED(HttpStatus.BAD_GATEWAY, "결제 승인에 실패했습니다."),
    PAYMENT_CANCEL_FAILED(HttpStatus.BAD_GATEWAY, "결제 취소(환불)에 실패했습니다."),
    INVALID_TOSS_SIGNATURE(HttpStatus.UNAUTHORIZED, "유효하지 않은 웹훅 서명입니다."),

    // Match
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "경기를 찾을 수 없습니다."),
    MATCH_ALREADY_FINISHED(HttpStatus.BAD_REQUEST, "이미 종료된 경기입니다."),
    MATCH_INVALID_WINNER(HttpStatus.BAD_REQUEST, "해당 경기의 참가자만 승자로 지정할 수 있습니다."),

    // Bracket
    BRACKET_ALREADY_GENERATED(HttpStatus.CONFLICT, "이미 대진표가 생성된 대회입니다."),
    BRACKET_NOT_ENOUGH_PARTICIPANTS(HttpStatus.BAD_REQUEST, "대진표를 생성하려면 최소 2명의 참가자가 필요합니다.");

    private final HttpStatus status;
    private final String message;
}
