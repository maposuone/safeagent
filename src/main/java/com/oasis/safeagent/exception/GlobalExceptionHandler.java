package com.oasis.safeagent.exception;

import java.time.Instant;
import java.nio.file.InvalidPathException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    /*
     * SafeAgentで想定している業務エラー。
     *
     * FILE_MISMATCH
     * DESIGN_NOT_FOUND
     * DESIGN_FILE_REQUIRED
     * AI_ERROR
     */
    @ExceptionHandler(SafeAgentException.class)
    public ResponseEntity<Map<String, Object>>
            handleSafeAgentException(
                    SafeAgentException e,
                    HttpServletRequest request) {

        log.warn(
                "[SAFEAGENT_ERROR] code={}, path={}, message={}",
                e.getCode(),
                request.getRequestURI(),
                e.getMessage()
        );

        return ResponseEntity
                .status(
                        e.getHttpStatus()
                )
                .body(
                        createBody(
                                e.getCode(),
                                e.getMessage(),
                                request.getRequestURI()
                        )
                );
    }

    /*
     * 入力値不正。
     *
     * 内部Java例外をそのままブラウザへ返さない。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>>
            handleIllegalArgumentException(
                    IllegalArgumentException e,
                    HttpServletRequest request) {

        log.warn(
                "[SAFEAGENT_ERROR] code=INVALID_REQUEST, path={}, message={}",
                request.getRequestURI(),
                e.getMessage()
        );

        return ResponseEntity
                .badRequest()
                .body(
                        createBody(
                                e instanceof InvalidPathException ? "INVALID_FILE_PATH" : "INVALID_REQUEST",
                                e instanceof InvalidPathException
                                        ? "設定ファイルのパスを処理できませんでした。ファイル名と保存先を確認してください。"
                                        : "入力内容を確認してください。",
                                request.getRequestURI()
                        )
                );
    }

    private Map<String, Object> createBody(
            String code,
            String message,
            String path) {

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put(
                "timestamp",
                Instant.now().toString()
        );

        body.put(
                "code",
                code
        );

        body.put(
                "message",
                message
        );

        body.put(
                "path",
                path
        );

        return body;
    }
}
