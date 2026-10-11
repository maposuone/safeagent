package com.oasis.safeagent.controller;

import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** 利用者向けのエラー表示。内部の例外文やパスは応答に含めない。 */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> resourceNotFound(NoResourceFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "指定されたページまたはファイルが見つかりません。");
    }
    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<Map<String, String>> invalidRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "必要な入力が不足しています。依頼内容、ファイル、設定項目を確認してください。");
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge(Exception exception) {
        return error(HttpStatus.valueOf(413), "FILE_TOO_LARGE", "ファイルのサイズが上限を超えています。小さいファイルで試してください。");
    }
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> forbidden(Exception exception) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_BLOCKED", "許可されていない場所へのアクセスを中止しました。");
    }
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> fileError(Exception exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "IO_ERROR", "ファイルの読み取り、または書き込みに失敗しました。");
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> other(Exception exception) {
        log.error("[SAFEAGENT_ERROR] code=REQUEST_FAILED", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "REQUEST_FAILED", "処理を完了できませんでした。しばらくしてから再度お試しください。");
    }
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> databaseError(DataAccessException exception) {
        log.error("[SAFEAGENT_ERROR] code=DATABASE_ERROR", exception);
        if (exception instanceof CannotGetJdbcConnectionException) {
            return error(HttpStatus.SERVICE_UNAVAILABLE, "DATABASE_CONNECTION_FAILED",
                    "データベースに接続できませんでした。DBの起動状態と接続設定を確認してください。");
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR",
                "データベース処理に失敗しました。サーバーログとDBのテーブル定義を確認してください。");
    }
    private ResponseEntity<Map<String, String>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of("status", "FAILED", "code", code, "message", message));
    }
}
