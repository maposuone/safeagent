package com.oasis.safeagent.exception;

import org.springframework.http.HttpStatus;

public class SafeAgentException extends RuntimeException {

    private final String code;
    private final HttpStatus httpStatus;

    public SafeAgentException(
            String code,
            String message,
            HttpStatus httpStatus) {

        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /*
     * 設定ファイルと設計書の対象ファイル名が違う。
     */
    public static SafeAgentException fileMismatch(
            String actualFileName,
            String designFileName) {

        StringBuilder message =
                new StringBuilder(
                        "設定ファイルと定数設計書の対象ファイルが一致しません。"
                );

        if (actualFileName != null
                && !actualFileName.isBlank()) {

            message.append(
                    " 設定ファイル="
            );
            message.append(
                    actualFileName
            );
        }

        if (designFileName != null
                && !designFileName.isBlank()) {

            message.append(
                    " / 設計書の対象="
            );
            message.append(
                    designFileName
            );
        }

        return new SafeAgentException(
                "FILE_MISMATCH",
                message.toString(),
                HttpStatus.BAD_REQUEST
        );
    }

    /*
     * X-1 / X-2構造がない、対象設計が空など。
     */
    public static SafeAgentException designNotFound() {

        return new SafeAgentException(
                "DESIGN_NOT_FOUND",
                "定数設計書に対象ファイルの設計が見つかりません。",
                HttpStatus.BAD_REQUEST
        );
    }

    /*
     * ANALYZE / FIXなのにExcelが未選択。
     */
    public static SafeAgentException designFileRequired() {

        return new SafeAgentException(
                "DESIGN_FILE_REQUIRED",
                "ANALYZE / FIXでは定数設計書を選択してください。",
                HttpStatus.BAD_REQUEST
        );
    }

    /*
     * Gemini呼び出し失敗。
     */
    public static SafeAgentException aiError() {

        return new SafeAgentException(
                "AI_ERROR",
                "AI処理に失敗しました。しばらくしてから再度お試しください。",
                HttpStatus.BAD_GATEWAY
        );
    }
}