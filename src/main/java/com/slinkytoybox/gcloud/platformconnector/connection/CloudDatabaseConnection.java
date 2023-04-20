/*
 *   platformconnector - CloudDatabaseConnection.java
 *
 *   Copyright (c) 2022-2023, Slinky Software
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation, either version 3 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   A copy of the GNU Affero General Public License is located in the 
 *   AGPL-3.0.md supplied with the source code.
 *
 */
package com.slinkytoybox.gcloud.platformconnector.connection;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Service;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Service("CloudDatabaseConnection")
@Slf4j
public class CloudDatabaseConnection {

    private final HikariDataSource poolSource = new HikariDataSource();

    @Value("${cloud.database.url:NOT_SET}")
    private String jdbcUrl;

    @Value("${cloud.database.username:NOT_SET}")
    private String jdbcUser;

    @Value("${cloud.database.password:NOT_SET}")
    private String jdbcPassword;

    @Value("${cloud.database.pool.min-size:3}")
    private int poolMinSize;

    @Value("${cloud.database.pool.test-query:SELECT 1}")
    private String poolTestQuery;

    @Value("${cloud.database.pool.idle-timeout:300000}")
    private Long poolIdleTimeout;

    @Value("${cloud.database.pool.keepalive-time:60000}")
    private Long poolKeepaliveTime;

    @Autowired
    private ConfigurableEnvironment env;

    @PostConstruct
    public void startDatabase() {
        final String logPrefix = "startDatabase() - ";
        log.trace("{}Entering Method", logPrefix);

        if (jdbcUrl.equalsIgnoreCase("NOT_SET") || jdbcUser.equalsIgnoreCase("NOT_SET") || jdbcPassword.equalsIgnoreCase("NOT_SET")) {
            log.error("{}JDBC Connection paramaters 'cloud.database.url|username|password' are not defined correctly", logPrefix);
            throw new IllegalArgumentException("JDBC Connection paramaters 'cloud.database.url|username|password' are not defined correctly");
        }

        log.info("{}Starting database connection pool", logPrefix);

        log.debug("{}Connection Parameters:\nURL:  {}\nUser:  {}", logPrefix, jdbcUrl, jdbcUser);

        log.debug("{}Creating Connection Pool", logPrefix);
        poolSource.setJdbcUrl(jdbcUrl);
        poolSource.setUsername(jdbcUser);
        poolSource.setPassword(jdbcPassword);
        poolSource.setMinimumIdle(poolMinSize);

        poolSource.setConnectionTestQuery(poolTestQuery);
        poolSource.setPoolName("Genesys-Cloud-DB");
        poolSource.setIdleTimeout(poolIdleTimeout);
        poolSource.setKeepaliveTime(poolKeepaliveTime);

        log.trace("{}Set pool parameters: {}", logPrefix, poolSource);

        log.info("{}Starting database pool", logPrefix);

        try (Connection conn = poolSource.getConnection()) {
            log.debug("{}Got SQL connection from pool. Testing", logPrefix);
            if (conn.isValid(5)) {
                log.info("{}Successfully connected to database - reading configuration table", logPrefix);
                MutablePropertySources propertySources = env.getPropertySources();
                Map<String, Object> configurationMap = new HashMap<>();

                try (PreparedStatement ps = conn.prepareStatement("SELECT OptionKey, OptionValue FROM COM_CONFIG_OPTION;")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            configurationMap.put(rs.getNString("OptionKey"), rs.getNString("OptionValue"));
                        }
                    }
                }
                catch (SQLException ex) {
                    log.warn("{}Could not read configuration options from database. This is not necessarilty critical", logPrefix, ex);
                }
                propertySources.addFirst(new MapPropertySource("dbConfig", configurationMap));
                log.trace("{}Added Config Options: {}", logPrefix, configurationMap);
            }
            else {
                log.error("{}Database did not respond within 5 seconds", logPrefix);
                throw new IllegalStateException("Could not start database - database did not respond within 5 seconds");
            }
        }
        catch (SQLException ex) {
            log.error("{}SQL Exception encountered getting connection from pool!", logPrefix);
            throw new IllegalStateException("Could not start database!", ex);
        }
        log.trace("{}Leaving method", logPrefix);

    }

    @PreDestroy
    public void stopDatabase() {
        final String logPrefix = "stopDatabase() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Shutting down Cloud Database connections", logPrefix);
        poolSource.close();
        log.trace("{}Leaving method", logPrefix);
    }

    public Connection getDatabaseConnection() throws SQLException {
        final String logPrefix = "getDatabaseConnection() - ";
        log.trace("{}Entering Method", logPrefix);

        try {
            Connection conn = poolSource.getConnection();
            if (conn.isValid(5)) {
                log.trace("{}Returning active connection", logPrefix);
                return conn;
            }
            else {
                log.error("{}Database did not respond within 5 seconds", logPrefix);
                throw new SQLException("Database did not respond within 5 seconds");
            }
        }
        catch (SQLException ex) {
            log.error("{}SQL Exception encountered getting connection from pool!", logPrefix);
            throw ex;
        }
    }
}
