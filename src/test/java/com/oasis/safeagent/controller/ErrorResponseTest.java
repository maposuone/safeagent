package com.oasis.safeagent.controller;

import com.oasis.safeagent.exception.GlobalExceptionHandler;
import com.oasis.safeagent.exception.SafeAgentException;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ErrorResponseTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new Errors())
            .setControllerAdvice(new ApiExceptionHandler(), new GlobalExceptionHandler()).build();

    @RestController
    static class Errors {
        @GetMapping("/mismatch") void mismatch() {
            throw SafeAgentException.fileMismatch("httpd (4).conf", "httpd.conf");
        }
        @GetMapping("/io") void io() throws IOException { throw new IOException("internal path"); }
        @GetMapping("/path") void path() { throw new InvalidPathException("internal path", "invalid"); }
        @GetMapping("/ai") void ai() { throw SafeAgentException.aiError(); }
        @GetMapping("/other") void other() { throw new RuntimeException("internal detail"); }
        @GetMapping("/db") void db() {
            throw new org.springframework.jdbc.CannotGetJdbcConnectionException("internal DB detail");
        }
    }

    @Test void mismatchKeepsBusinessErrorDespiteCatchAllAdvice() throws Exception {
        mvc.perform(get("/mismatch")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FILE_MISMATCH"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("httpd (4).conf")));
    }
    @Test void fileReadFailureIsNotAiError() throws Exception {
        mvc.perform(get("/io")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("IO_ERROR"));
    }
    @Test void invalidPathHasFileSpecificMessage() throws Exception {
        mvc.perform(get("/path")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_PATH"));
    }
    @Test void aiFailureKeepsAiError() throws Exception {
        mvc.perform(get("/ai")).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_ERROR"));
    }
    @Test void unknownFailureDoesNotBlameAi() throws Exception {
        mvc.perform(get("/other")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("処理を完了できませんでした。しばらくしてから再度お試しください。"));
    }
    @Test void databaseConnectionFailureHasSpecificError() throws Exception {
        mvc.perform(get("/db")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DATABASE_CONNECTION_FAILED"));
    }
}
