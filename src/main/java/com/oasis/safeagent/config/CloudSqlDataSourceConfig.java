package com.oasis.safeagent.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class CloudSqlDataSourceConfig {

    @Bean
    @ConditionalOnProperty(
            name = "safeagent.cloudsql.enabled",
            havingValue = "true"
    )
    public DataSource cloudSqlDataSource() {

        String instanceConnectionName =
                System.getenv("INSTANCE_CONNECTION_NAME");

        String dbName =
                System.getenv("DB_NAME");

        String dbUser =
                System.getenv("DB_USER");

        String dbPass =
                System.getenv("DB_PASS");

        HikariConfig config =
                new HikariConfig();

        config.setJdbcUrl(
                "jdbc:postgresql:///" + dbName
        );

        config.setUsername(dbUser);
        config.setPassword(dbPass);

        config.addDataSourceProperty(
                "socketFactory",
                "com.google.cloud.sql.postgres.SocketFactory"
        );

        config.addDataSourceProperty(
                "cloudSqlInstance",
                instanceConnectionName
        );

        config.addDataSourceProperty(
                "ipTypes",
                "PUBLIC,PRIVATE"
        );

        config.addDataSourceProperty(
                "cloudSqlRefreshStrategy",
                "lazy"
        );

        config.setMaximumPoolSize(5);
        config.setMinimumIdle(0);

        return new HikariDataSource(config);
    }
}