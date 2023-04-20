/*
 *   platformconnector - SecurityConfiguration.java
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
package com.slinkytoybox.gcloud.platformconnector.security;

import com.slinkytoybox.gcloud.platformconnector.connection.CloudDatabaseConnection;
import com.slinkytoybox.gcloud.platformconnector.connection.GCloudAPIConnection;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Component
@DependsOn({"CloudDatabaseConnection", "GCloudAPIConnection"})
@Slf4j
public class SecurityConfiguration {

    @Autowired
    private CloudDatabaseConnection cdc;

    @Autowired
    private GCloudAPIConnection cloudApi;

    private String securePassword = null;

    @Scheduled(fixedDelayString = "120000", initialDelayString = "60000")
    @PostConstruct
    public void checkPasswordChanged() {
        final String logPrefix = "checkPasswordExpiry() - ";
        log.trace("{}Entering Method", logPrefix);
        String keySql = "SELECT SecureKey, LastUpdated FROM INT_SECURE_KEY WHERE CloudPlatformId=?";
        String tempSecurePassword = "";
        Long cloudPlatformId = cloudApi.getCloudPlatform().getId();
        LocalDateTime lastUpdated = LocalDateTime.MIN;

        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            try (PreparedStatement ps = dbConnection.prepareStatement(keySql)) {
                ps.setLong(1, cloudPlatformId);
                try (ResultSet rs = ps.executeQuery()) {
                    log.info("{}Getting key from Database", logPrefix);
                    while (rs.next()) {
                        tempSecurePassword = rs.getNString("SecureKey");
                        lastUpdated = (rs.getTimestamp("LastUpdated") == null ? LocalDateTime.MIN : rs.getTimestamp("LastUpdated").toLocalDateTime());

                        if (tempSecurePassword.equals(securePassword)) {
                            log.trace("{}Password has not changed, exiting", logPrefix);
                            return;
                        }
                    }
                }
            }
        }
        catch (Exception ex) {
            log.error("{}Exception encountered reading password", logPrefix, ex);
            tempSecurePassword = "";
        }

        if (tempSecurePassword.isEmpty() || lastUpdated == LocalDateTime.MIN) {
            log.warn("{}Password was not retrieved. Forcing a rotate", logPrefix);
            rotatePassword();
            return;
        }

        securePassword = tempSecurePassword;

        log.debug("{}Using password: {}", logPrefix, securePassword);
        log.trace("{}Leaving method", logPrefix);

    }

    public void rotatePassword() {
        final String logPrefix = "rotatePassword() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Rotating cloud password", logPrefix);
        String tempSecurePassword = generateSecurePassword();

        if (savePassword(tempSecurePassword)) {
            securePassword = tempSecurePassword;
            if (!notifyOtherHosts()) {
                log.error("{}There was an error notifying other hosts. Please check them!");
            }
        }
        else {
            log.warn("{}Password not updated!", logPrefix);
        }
        log.debug("{}Using password: {}", logPrefix, securePassword);
        log.trace("{}Leaving method", logPrefix);
    }

    private boolean updateCloud(String password) {
        final String logPrefix = "updateCloud() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Updating password in Genesys Cloud", logPrefix);

        return true;
    }

    private boolean savePassword(String password) {
        final String logPrefix = "savePassword() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Saving password to Database", logPrefix);

        String updateKeySql = "UPDATE INT_SECURE_KEY SET SecureKey = ?, LastUpdated = GETDATE() WHERE CloudPlatformId = ?";
        String insertKeySql = "INSERT INTO INT_SECURE_KEY (CloudPlatformId, SecureKey, LastUpdated) VALUES (?, ?, GETDATE())";
        Long cloudPlatformId = cloudApi.getCloudPlatform().getId();
        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            int rowsUpdated;
            dbConnection.setAutoCommit(false);
            try (PreparedStatement ps = dbConnection.prepareStatement(updateKeySql)) {
                ps.setNString(1, password);
                ps.setLong(2, cloudPlatformId);
                rowsUpdated = ps.executeUpdate();
                log.trace("{}Statement issued, rows updated: {}", logPrefix, rowsUpdated);
            }
            boolean cloudUpdateSuccess = false;

            if (rowsUpdated == 1) {
                cloudUpdateSuccess = updateCloud(password);
            }
            else {
                try (PreparedStatement ps = dbConnection.prepareStatement(insertKeySql)) {
                    ps.setLong(1, cloudPlatformId);
                    ps.setNString(2, password);
                    rowsUpdated = ps.executeUpdate();
                    log.trace("{}Statement issued, rows inserted: {}", logPrefix, rowsUpdated);
                }
                if (rowsUpdated == 1) {
                    cloudUpdateSuccess = updateCloud(password);
                }
            }

            if (rowsUpdated == 1 && cloudUpdateSuccess) {
                log.info("{}Password updated in cloud, committing to database", logPrefix);
                dbConnection.commit();
                return true;
            }
            else if (rowsUpdated == 1) {
                log.warn("{}Unable to update cloud, rolling back DB", logPrefix);
                dbConnection.rollback();
            }
            else {
                log.warn("{}Unable to database correctly, rolling back DB", logPrefix);
                dbConnection.rollback();
            }
        }
        catch (Exception ex) {
            log.error("{}Exception encountered updating password in database", logPrefix, ex);
        }
        return false;
    }

    private String generateSecurePassword() {
        final String logPrefix = "generateSecurePassword() - ";
        log.trace("{}Entering Method", logPrefix);
        String randomString = UUID.randomUUID().toString();
        byte[] encodedString;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            encodedString = digest.digest(randomString.getBytes(StandardCharsets.UTF_8));
        }
        catch (NoSuchAlgorithmException ex) {
            log.warn("{}SHA256 hashing not available, using UUID directly");
            encodedString = randomString.getBytes(StandardCharsets.UTF_8);
        }

        return Base64.getEncoder().encodeToString(encodedString);
    }

    private boolean notifyOtherHosts() {
        return true;
    }

    public String getCurrentPassword() {
        return securePassword;
    }
}
