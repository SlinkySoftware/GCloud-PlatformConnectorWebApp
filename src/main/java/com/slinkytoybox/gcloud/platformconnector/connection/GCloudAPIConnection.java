/*
 *   platformconnector - GCloudAPIConnection.java
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

import com.mypurecloud.sdk.v2.*;
import com.mypurecloud.sdk.v2.extensions.AuthResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Locale;
import jakarta.annotation.PostConstruct;
import java.sql.*;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Service("GCloudAPIConnection")
@DependsOn("CloudDatabaseConnection")
@Slf4j
public class GCloudAPIConnection {

    private static final String USER_AGENT = "GCloud-License-Management";
    
    @Autowired
    private CloudDatabaseConnection dbConn;

    @Value("${genesys.cloud.api-timeout:10000}")
    private Integer apiTimeout;
    
    @Value("${genesys.cloud.max-retry-sec:5}")
    private Integer maxRetrySec;
    
    @Value("${genesys.cloud.max-retry-count:3}")
    private Integer maxRetryCount;

    @Value("${cloud.platform.id:-1}")
    private Long cloudPlatformId;

    private CloudPlatform cp = null;

    private String platformGuid;
    
    
    @PostConstruct
    private void init() throws SQLException {
        final String logPrefix = "init() - ";
        log.trace("{}Entering Method", logPrefix);
        if (cloudPlatformId <= 0) {
            log.error("{}Cloud platform ID is not configured correctly. Please set cloud.platform.id in the configuration file", logPrefix);
            throw new IllegalArgumentException("Cloud platform ID is not configured correctly.");
        }


        log.info("{}Initialisating Genesys Cloud connections", logPrefix);

        log.debug("{}Getting list of cloud plaforms from Database", logPrefix);

        String platformSql = "SELECT Id, Name, OrganisationName, OrganisationId, OrganisationGuid, ApiRegion, ApiClientId, ApiClientSecret, AzureAdAccessGroup FROM COM_CLOUD_PLATFORM WHERE Enabled = 1 AND Id=?";
        try (Connection dbConnection = dbConn.getDatabaseConnection()) {
            try (PreparedStatement ps = dbConnection.prepareStatement(platformSql)) {
                ps.setLong(1, cloudPlatformId);
                try (ResultSet rs = ps.executeQuery()) {
                    log.info("{}Finding Genesys cloud instance", logPrefix);
                    while (rs.next()) {
                        cp = new CloudPlatform()
                                .setId(rs.getLong("Id"))
                                .setName(rs.getString("Name"))
                                .setOrganisationName(rs.getNString("OrganisationName"))
                                .setOrganisationId(rs.getNString("OrganisationId"))
                                .setApiRegion(rs.getNString("ApiRegion"))
                                .setApiClientId(rs.getNString("ApiClientId"))
                                .setApiClientSecret(rs.getNString("APIClientSecret"))
                                .setAzureAdAccessGroup(rs.getNString("AzureAdAccessGroup"))
                                .setOrganisationGuid(rs.getNString("OrganisationGuid"));
                        log.info("{}Got Cloud Platform {} - attempting to connect", logPrefix, cp);

                        cp = initCloudPlatform(cp);
                    }
                    if (cp == null) {
                        log.error("{}Could not find cloud platform with id {} in database", logPrefix, cloudPlatformId);
                        throw new IllegalArgumentException("");
                    }
                }
            }
        }
        this.platformGuid = cp.getOrganisationGuid();
        log.info("{}Cloud Platform {} Initialised", logPrefix, cp);
    }

    private CloudPlatform initCloudPlatform(CloudPlatform cp) {
        final String logPrefix = "initCloudPlatform() - ";
        log.trace("{}Entering Method", logPrefix);

        PureCloudRegionHosts region = null;
        ApiClient apiClient;

        if (cp.getApiClientId() == null || cp.getApiClientSecret() == null || cp.getApiRegion() == null) {
            throw new IllegalArgumentException("Genesys cloud configuration not specified in database");
        }
        try {
            region = PureCloudRegionHosts.valueOf(cp.getApiRegion());
        }
        catch (Exception ex) {
            log.error("{}Invalid region specifed: {}", logPrefix, cp.getApiRegion());
            throw new IllegalArgumentException("Invalid Genesys cloud region", ex);
        }
        log.info("{}Genesys Cloud '{}' -> Client ID: {} @ Region: {}", logPrefix, cp.getOrganisationName(), cp.getApiClientId(), cp.getApiRegion());
        ApiClient.RetryConfiguration retryConfig = new ApiClient.RetryConfiguration();
        retryConfig.setMaxRetryTimeSec(maxRetrySec);
        retryConfig.setRetryMax(maxRetryCount);
        log.debug("{}Building api connection", logPrefix);
        apiClient = ApiClient.Builder
                .standard()
                .withBasePath(region)
                .withUserAgent(USER_AGENT)
                .withShouldRefreshAccessToken(true)
                //.withDateFormat(dateFormat)
                .withShouldThrowErrors(true)
                .withConnectionTimeout(apiTimeout)
                .withRetryConfiguration(retryConfig)
                .build();

        log.debug("{}Authenticating to Genesys cloud", logPrefix);
        try {
            ApiResponse<AuthResponse> authResponse = apiClient.authorizeClientCredentials(cp.getApiClientId(), cp.getApiClientSecret());
            log.info("{}Client Authentication Response: {}", logPrefix, authResponse.getBody());
        }
        catch (ApiException | IOException ex) {
            throw new IllegalArgumentException("Exception authenticating", ex);
        }
        log.debug("{}Successfully authenticated", logPrefix);
        cp.setApiClient(apiClient);
        return cp;

    }

    public ApiClient getApiClient() {
        final String logPrefix = "getApiClient() - ";
        log.trace("{}Entering Method", logPrefix);
        if (cp == null) {
            log.error("{}Cloud platform does not exist", logPrefix);
            return null;
        }
        return cp.getApiClient();
    }

    public CloudPlatform getCloudPlatform() {
        final String logPrefix = "getCloudPlatform() - ";
        log.trace("{}Entering Method", logPrefix);
        return cp;
    }
    
    public String getPlatformGuid() {
        return platformGuid;
    }

}
