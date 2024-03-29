/*
 *   platformconnector - CloudSecurityConfiguration.java
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

import com.mypurecloud.sdk.v2.ApiException;
import com.mypurecloud.sdk.v2.api.IntegrationsApi;
import com.mypurecloud.sdk.v2.model.Credential;
import com.mypurecloud.sdk.v2.model.CredentialInfo;
import com.mypurecloud.sdk.v2.model.CredentialType;
import com.slinkytoybox.gcloud.platformconnector.connection.CloudDatabaseConnection;
import com.slinkytoybox.gcloud.platformconnector.connection.GCloudAPIConnection;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.digest.StandardStringDigester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Component
@DependsOn({"CloudDatabaseConnection", "GCloudAPIConnection", "PlatformEncryption"})
@Slf4j
public class CloudSecurityConfiguration {

    @Autowired
    private CloudDatabaseConnection cdc;

    @Autowired
    private GCloudAPIConnection cloudApi;

    private String securePassword = null;

    @Value("${cloud.credential.id:NOT_SET}")
    private String credentialId;

    @Value("${server.id:DefaultServerId}")
    private String serverId;

    @Autowired
    private PlatformEncryption encryptor;

    @PostConstruct
    @Scheduled(fixedDelayString = "120000", initialDelayString = "60000")
    public ReadKeyStatus checkPasswordChanged() {
        final String logPrefix = "checkPasswordChanged() - ";
        log.trace("{}Entering Method", logPrefix);
        String keySql = "SELECT SecureKey, LastUpdated FROM INT_SECURE_KEY WHERE CloudPlatformId=?";
        String tempSecurePassword = "";
        Long cloudPlatformId = cloudApi.getCloudPlatform().getId();
        LocalDateTime lastUpdated = LocalDateTime.MIN;
        ReadKeyStatus result;
        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            try (PreparedStatement ps = dbConnection.prepareStatement(keySql)) {
                ps.setLong(1, cloudPlatformId);
                try (ResultSet rs = ps.executeQuery()) {
                    log.info("{}Getting key from Database", logPrefix);
                    while (rs.next()) {

                        String encPassword = rs.getNString("SecureKey");
                        log.trace("{}Decrypting DB password", logPrefix);
                        tempSecurePassword = encryptor.decrypt(encPassword);
                        lastUpdated = (rs.getTimestamp("LastUpdated") == null ? LocalDateTime.MIN : rs.getTimestamp("LastUpdated").toLocalDateTime());
                        if (tempSecurePassword.equals(securePassword)) {
                            log.trace("{}Password has not changed, exiting", logPrefix);
                            return ReadKeyStatus.PASSWORD_NOT_CHANGED;
                        }
                    }
                }
            }
             catch (Exception ex) {
            log.error("{}Exception encountered reading password", logPrefix, ex);
            tempSecurePassword = "";
        }
        }
        catch (Exception ex) {
            log.error("{}Exception encountered getting database connection - NOT MODIFYING EXISTING PASSWORD", logPrefix, ex);
            return ReadKeyStatus.PASSWORD_NOT_CHANGED;
        }
        
        result = ReadKeyStatus.PASSWORD_UPDATED;
        if (tempSecurePassword.isEmpty() || lastUpdated == LocalDateTime.MIN) {
            log.warn("{}Password was not retrieved. Forcing a rotate", logPrefix);
            RotateStatus status = rotatePassword();
            if (status != RotateStatus.FAILURE) {
                result = ReadKeyStatus.ROTATED;
            }
        }

        securePassword = tempSecurePassword;

        log.debug("{}Using password: {}", logPrefix, securePassword);
        log.trace("{}Leaving method", logPrefix);
        return result;
    }

    @Scheduled(cron = "${cloud.password.rotate-cron}")
    public RotateStatus rotatePassword() {
        final String logPrefix = "rotatePassword() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Rotating cloud password", logPrefix);
        String tempSecurePassword = generateSecurePassword();

        if (savePassword(tempSecurePassword)) {
            securePassword = tempSecurePassword;
            if (!notifyOtherHosts()) {
                log.error("{}There was an error notifying other hosts. Please check them!", logPrefix);
                return RotateStatus.ERROR_NOTIFYING_HOSTS;
            }
        }
        else {
            log.warn("{}Password not updated!", logPrefix);
            return RotateStatus.FAILURE;
        }
        log.debug("{}Using password: {}", logPrefix, securePassword);
        log.trace("{}Leaving method", logPrefix);
        return RotateStatus.SUCCESS;
    }

    private boolean updateCloud(String password) {
        final String logPrefix = "updateCloud() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Updating password in Genesys Cloud", logPrefix);
        IntegrationsApi intApi = new IntegrationsApi(cloudApi.getApiClient());
        Credential newCred = new Credential();
        newCred.setName("AutoRotated-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        newCred.setType(new CredentialType().name("userDefined"));
        Map<String, String> credFields = new HashMap<>();
        credFields.put("AuthKey", password);
        newCred.setCredentialFields(credFields);
        log.trace("{}New Credential {}", logPrefix, newCred);
        log.debug("{}About to update Genesys Cloud", logPrefix);
        try {
            CredentialInfo result = intApi.putIntegrationsCredential(credentialId, newCred);
            if (result.getId().equalsIgnoreCase(credentialId)) {
                log.info("{}Credential updated in Genesys Cloud successfully", logPrefix);
                return true;
            }
            else {
                log.info("{}Credential was not updated correctly", logPrefix);
                return false;
            }
        }
        catch (ApiException | IOException ex) {
            log.error("{}An exception was encountered whilst updating the Cloud Credential", logPrefix, ex);
            return false;
        }
    }

    private boolean savePassword(String password) {
        final String logPrefix = "savePassword() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Saving password to Database", logPrefix);

        String updateKeySql = "UPDATE INT_SECURE_KEY SET SecureKey = ?, LastUpdated = GETDATE() WHERE CloudPlatformId = ?";
        String insertKeySql = "INSERT INTO INT_SECURE_KEY (CloudPlatformId, SecureKey, LastUpdated) VALUES (?, ?, GETDATE())";
        Long cloudPlatformId = cloudApi.getCloudPlatform().getId();
        String encryptedPassword = encryptor.encrypt(password);

        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            int rowsUpdated;
            dbConnection.setAutoCommit(false);
            try (PreparedStatement ps = dbConnection.prepareStatement(updateKeySql)) {
                ps.setNString(1, encryptedPassword);
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
                    ps.setNString(2, encryptedPassword);
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
        StandardStringDigester sd = new StandardStringDigester();
        sd.setAlgorithm("SHA-256");
        sd.setIterations(20000);
        String encodedString = sd.digest(randomString);
        return Base64.getEncoder().encodeToString(encodedString.getBytes());
    }

    private boolean notifyOtherHosts() {
        final String logPrefix = "notifyOtherHosts() - ";
        log.trace("{}Entering Method", logPrefix);
        String keySql = "SELECT ServerName, ServerUrl FROM INT_SERVER WHERE ServerName != ?";
        boolean success = true;
        try (Connection dbConnection = cdc.getDatabaseConnection()) {
            try (PreparedStatement ps = dbConnection.prepareStatement(keySql)) {
                ps.setNString(1, serverId);
                log.info("{}I am {} - Getting other servers from Database", logPrefix, serverId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            String urlString = rs.getNString("ServerUrl");
                            String serverName = rs.getNString("ServerName");
                            log.trace("{}serverName='{}',  urlString='{}'", logPrefix, serverName, urlString);
                            if (urlString.isBlank() || !urlString.startsWith("http")) {
                                log.error("{}Server {} URL is invalid: {}", logPrefix, serverName, urlString);
                                success = false;
                                continue;
                            }
                            urlString += (urlString.endsWith("/") ? "" : "/");
                            urlString += "security/readKey";
                            log.debug("{}Forming request for {}", logPrefix, urlString);
                            URL url = new URL(urlString);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setConnectTimeout(2000);
                            conn.setReadTimeout(2000);
                            conn.setRequestMethod("POST");
                            conn.connect();
                            int statusCode = conn.getResponseCode();
                            if (statusCode != 200) {
                                log.error("{}Received HTTP status code {} from endpoint {}", logPrefix, statusCode, serverName);
                                success = false;
                                continue;
                            }
                            BufferedReader buff = new BufferedReader(new InputStreamReader((InputStream) conn.getContent()));
                            String response = buff.readLine();

                            log.debug("{}Successfully notified {}, response {}", logPrefix, serverName, response);
                        }
                        catch (SQLException | IOException ex) {
                            log.error("{}Exception encountered whilst reading or notifying a specific host!", logPrefix, ex);
                            success = false;
                        }
                        log.trace("{}Moving to next record", logPrefix);
                    }
                }
            }
        }
        catch (SQLException ex) {
            log.error("{}Exception encountered reading server list", logPrefix, ex);
            success = false;
        }
        log.info("{}Finished processing all servers. Overall success={}", logPrefix, success);
        return success;
    }

    public String getCurrentPassword() {
        return securePassword;
    }

    public enum RotateStatus {
        SUCCESS,
        FAILURE,
        ERROR_NOTIFYING_HOSTS
    }

    public enum ReadKeyStatus {
        PASSWORD_UPDATED,
        PASSWORD_NOT_CHANGED,
        ROTATED,
        ERROR
    }
}
