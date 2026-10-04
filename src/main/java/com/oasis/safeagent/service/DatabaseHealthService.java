package com.oasis.safeagent.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseHealthService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthService(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    public String checkConnection() {

        String result =
                jdbcTemplate.queryForObject(
                        "SELECT current_database()",
                        String.class
                );

        return result;
    }
}