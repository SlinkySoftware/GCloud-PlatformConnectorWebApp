/*
 *   platformconnector - PluginLogic.java
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
package com.slinkytoybox.gcloud.platformconnector.businesslogic;

import com.slinkytoybox.gcloud.platformconnector.connection.GCloudAPIConnection;
import com.slinkytoybox.gcloud.platformconnector.dto.request.*;
import com.slinkytoybox.gcloud.platformconnector.dto.response.*;
import com.slinkytoybox.gcloud.platformconnector.pluginmanagement.PluginManagement;
import com.slinkytoybox.gcloud.platformconnector.pluginmanagement.RegisteredPlugin;
import com.slinkytoybox.gcloud.platformconnector.security.CloudSecurityConfiguration;
import com.slinkytoybox.gcloud.platformconnectorplugin.PlatformConnectorPlugin;
import com.slinkytoybox.gcloud.platformconnectorplugin.PluginOperation;
import com.slinkytoybox.gcloud.platformconnectorplugin.SourceContainer;
import com.slinkytoybox.gcloud.platformconnectorplugin.health.HealthState;
import com.slinkytoybox.gcloud.platformconnectorplugin.request.*;
import com.slinkytoybox.gcloud.platformconnectorplugin.response.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Component
@Slf4j
public class PluginLogic {

    @Autowired
    private PluginManagement pluginManagement;

    @Autowired
    private GCloudAPIConnection apiConnection;

    @Autowired
    private CloudSecurityConfiguration securityConfig;

    public ResponseEntity<JSONResponse> doCreate(WebRequest webReq, String pluginId, JSONCreateRequest request) {
        String logPrefix = "doCreate() - ";
        log.trace("{}Entering method", logPrefix);
        SecurityHeader hdr = new SecurityHeader(webReq, apiConnection.getPlatformGuid(), securityConfig.getCurrentPassword());
        if (hdr.checkMissingHeaders()) {
            log.error("{}Authentication headers missing from request", logPrefix);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONErrorResponse().setErrorMessage("Invalid request, required headers missing").setErrorCode(400));
        }
        else if (!hdr.isAuthenticationValid()) {
            log.error("{}Authentication in request does not match", logPrefix);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JSONErrorResponse().setErrorMessage("You are not authorised to access this resource").setErrorCode(403));
        }
        String requestId = hdr.reqHeader;
        logPrefix = "createItem() - " + "[" + requestId + "] - ";
        log.debug("{}JSON Data: {}", logPrefix, request);
        JSONCreateResponse jsonResponse = new JSONCreateResponse(requestId);
        jsonResponse.setPluginId(pluginId);
        PlatformPlugin plug = getPlugin(pluginId);
        ResponseEntity<JSONResponse> check = checkPlugin(jsonResponse, plug, PluginOperation.CREATE);
        if (check != null) {
            return check;
        }
        try {
            CreateRequest pluginRequest = new CreateRequest();
            pluginRequest.setRequestId(requestId);
            pluginRequest.setRequestDate(OffsetDateTime.now(ZoneId.of("Australia/Sydney")));
            pluginRequest.setObjectDetails(request.getNewDetails());
            pluginRequest.setRequestParameters(request.getQueryString());
            CreateResponse pluginResponse = (CreateResponse) plug.plugin.getResponseFromRequest(pluginRequest);
            setErrorDetails(jsonResponse, pluginResponse);
        }
        catch (Exception ex) {
            log.error("{}Exception when creating record", logPrefix, ex);
            jsonResponse.setErrorMessage(ex.getMessage());
            jsonResponse.setErrorCode(-1);
        }
        return ResponseEntity.ok().body(jsonResponse);

    }

    public ResponseEntity<JSONResponse> doUpdate(WebRequest webReq, String pluginId, JSONUpdateRequest request, String recordId) {
        String logPrefix = "doUpdate() - ";
        log.trace("{}Entering method", logPrefix);
        SecurityHeader hdr = new SecurityHeader(webReq, apiConnection.getPlatformGuid(), securityConfig.getCurrentPassword());
        if (hdr.checkMissingHeaders()) {
            log.error("{}Authentication headers missing from request", logPrefix);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONErrorResponse().setErrorMessage("Invalid request, required headers missing").setErrorCode(400));
        }
        else if (!hdr.isAuthenticationValid()) {
            log.error("{}Authentication in request does not match", logPrefix);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JSONErrorResponse().setErrorMessage("You are not authorised to access this resource").setErrorCode(403));
        }
        String requestId = hdr.reqHeader;
        logPrefix = "doUpdate() - " + "[" + requestId + "] - ";
        log.info("{}Processing PATCH /{}/record/{}", logPrefix, pluginId, recordId);

        log.debug("{}JSON Data: {}", logPrefix, request);
        JSONUpdateResponse jsonResponse = new JSONUpdateResponse(requestId);
        jsonResponse.setPluginId(pluginId);
        PlatformPlugin plug = getPlugin(pluginId);
        ResponseEntity<JSONResponse> check = checkPlugin(jsonResponse, plug, PluginOperation.UPDATE);
        if (check != null) {
            return check;
        }
        try {
            UpdateRequest pluginRequest = new UpdateRequest();
            pluginRequest.setRequestId(requestId);
            pluginRequest.setRequestDate(OffsetDateTime.now(ZoneId.of("Australia/Sydney")));
            pluginRequest.setNewDetails(request.getNewDetails());
            pluginRequest.setRequestParameters(request.getQueryString());
            UpdateResponse pluginResponse = (UpdateResponse) plug.plugin.getResponseFromRequest(pluginRequest);
            setErrorDetails(jsonResponse, pluginResponse);
        }
        catch (Exception ex) {
            log.error("{}Exception when updating record", logPrefix, ex);
            jsonResponse.setErrorMessage(ex.getMessage());
            jsonResponse.setErrorCode(-1);
        }
        return ResponseEntity.ok().body(jsonResponse);
    }

    public ResponseEntity<JSONResponse> doSearch(WebRequest webReq, String pluginId, JSONReadRequest request, String recordId) {
        String logPrefix = "doSearch() - ";
        log.trace("{}Entering method", logPrefix);
        SecurityHeader hdr = new SecurityHeader(webReq, apiConnection.getPlatformGuid(), securityConfig.getCurrentPassword());
        if (hdr.checkMissingHeaders()) {
            log.error("{}Authentication headers missing from request", logPrefix);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONErrorResponse().setErrorMessage("Invalid request, required headers missing").setErrorCode(400));
        }
        else if (!hdr.isAuthenticationValid()) {
            log.error("{}Authentication in request does not match", logPrefix);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JSONErrorResponse().setErrorMessage("You are not authorised to access this resource").setErrorCode(403));
        }
        String requestId = hdr.reqHeader;
        logPrefix = "doSearch() - " + "[" + requestId + "] - ";
        log.info("{}Performing search", logPrefix, pluginId);
        log.debug("{}JSON Data: {}", logPrefix, request);
        log.debug("{}Record Id: {}", logPrefix, recordId);

        JSONReadResponse jsonResponse = new JSONReadResponse(requestId);
        jsonResponse.setObjectDetails(new HashMap<>());
        jsonResponse.setPluginId(pluginId);
        PlatformPlugin plug = getPlugin(pluginId);
        ResponseEntity<JSONResponse> check = checkPlugin(jsonResponse, plug, PluginOperation.READ);
        if (check != null) {
            return check;
        }
        try {
            ReadRequest pluginRequest = new ReadRequest();
            pluginRequest.setRequestId(requestId);
            pluginRequest.setRequestDate(OffsetDateTime.now(ZoneId.of("Australia/Sydney")));
            pluginRequest.setRequestParameters(request.getQueryString());
            if (recordId != null) {
                pluginRequest.setObjectId(recordId);
            }
            else {
                pluginRequest.setSearchParameters(request.getSearchParameters());
            }
            ReadResponse pluginResponse = (ReadResponse) plug.plugin.getResponseFromRequest(pluginRequest);
            setErrorDetails(jsonResponse, pluginResponse);
        }
        catch (Exception ex) {
            log.error("{}Exception when looking up record", logPrefix, ex);
            jsonResponse.setErrorMessage(ex.getMessage());
        }
        return ResponseEntity.ok().body(jsonResponse);
    }

    public ResponseEntity<JSONResponse> doDelete(WebRequest webReq, String pluginId, String recordId, JSONDeleteRequest request) {
        String logPrefix = "doDelete() - ";
        log.trace("{}Entering method", logPrefix);
        SecurityHeader hdr = new SecurityHeader(webReq, apiConnection.getPlatformGuid(), securityConfig.getCurrentPassword());
        if (hdr.checkMissingHeaders()) {
            log.error("{}Authentication headers missing from request", logPrefix);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONErrorResponse().setErrorMessage("Invalid request, required headers missing").setErrorCode(400));
        }
        else if (!hdr.isAuthenticationValid()) {
            log.error("{}Authentication in request does not match", logPrefix);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new JSONErrorResponse().setErrorMessage("You are not authorised to access this resource").setErrorCode(403));
        }
        String requestId = hdr.reqHeader;
        logPrefix = "deleteItem() - " + "[" + requestId + "] - ";

        log.debug("{}JSON Data: {}", logPrefix, request);
        JSONDeleteResponse jsonResponse = new JSONDeleteResponse(requestId);
        jsonResponse.setPluginId(pluginId);
        PlatformPlugin plug = getPlugin(pluginId);
        ResponseEntity<JSONResponse> check = checkPlugin(jsonResponse, plug, PluginOperation.DELETE);
        if (check != null) {
            return check;
        }
        try {
            DeleteRequest pluginRequest = new DeleteRequest();
            pluginRequest.setRequestId(requestId);
            pluginRequest.setRequestDate(OffsetDateTime.now(ZoneId.of("Australia/Sydney")));
            pluginRequest.setObjectId(recordId);
            pluginRequest.setRequestParameters(request.getQueryString());
            DeleteResponse pluginResponse = (DeleteResponse) plug.plugin.getResponseFromRequest(pluginRequest);
            setErrorDetails(jsonResponse, pluginResponse);
        }
        catch (Exception ex) {
            log.error("{}Exception when deleting record", logPrefix, ex);
            jsonResponse.setErrorMessage(ex.getMessage());
            jsonResponse.setErrorCode(-1);
        }
        return ResponseEntity.ok().body(jsonResponse);
    }

    private void setErrorDetails(JSONResponse jsonResponse, PluginResponse pluginResponse) {
        String logPrefix = "setErrorDetails() - ";
        log.trace("{}Entering method", logPrefix);
        log.debug("{}Setting response data based on source", logPrefix);
        switch (pluginResponse.getStatus()) {
            case SUCCESS -> {
                log.info("{}Successfully created item, ID: {}", logPrefix, pluginResponse.getObjectId());
                jsonResponse.setObjectId(pluginResponse.getObjectId());
                if (pluginResponse.getClass().isInstance(CreateResponse.class)) {
                    ((JSONCreateResponse) jsonResponse).setObjectDetails(((CreateResponse) pluginResponse).getObjectDetails());
                }
                else if (pluginResponse.getClass().isInstance(UpdateResponse.class)) {
                    ((JSONUpdateResponse) jsonResponse).setObjectDetails(((UpdateResponse) pluginResponse).getObjectDetails());
                }
                else if (pluginResponse.getClass().isInstance(ReadResponse.class)) {
                    ((JSONReadResponse) jsonResponse).setObjectDetails(((ReadResponse) pluginResponse).getObjectDetails());
                }
                jsonResponse.setErrorCode(0);
            }
            case RECORD_NOT_FOUND -> {
                log.error("{}Could not find record", logPrefix);
                jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                jsonResponse.setErrorCode(1);
            }
            case MULTIPLE_RECORDS -> {
                log.error("{}Multiple records found", logPrefix);
                jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                jsonResponse.setErrorCode(2);
            }
            case FAILURE -> {
                jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                jsonResponse.setErrorCode(3);
            }
            default -> {
                log.error("{}Failure creating record", logPrefix);
                jsonResponse.setErrorCode(4);
            }
        }
        log.trace("{}Leaving method", logPrefix);
    }

    public SourceContainer getSourceCode(String pluginId) {
        String logPrefix = "getSourceCode() - ";
        log.trace("{}Entering method", logPrefix);

        PlatformPlugin plug = getPlugin(pluginId);
        if (!plug.success) {
            log.error("{}Plugin {} is not operational: {}", logPrefix, pluginId, plug.errorMessage);
            return null;
        }
        return plug.plugin.getSourceCode();
    }

    public boolean isSourceAvailable(String pluginId) {
        String logPrefix = "isSourceAvailable() - ";
        log.trace("{}Entering method", logPrefix);

        PlatformPlugin plug = getPlugin(pluginId);
        if (!plug.success) {
            log.error("{}Plugin {} is not operational: {}", logPrefix, pluginId, plug.errorMessage);
            return false;
        }
        return plug.plugin.isSourceAvailable();
    }

    private PlatformPlugin getPlugin(String pluginId) {
        final String logPrefix = "getPlugin() - ";
        log.trace("{}Entering method", logPrefix);

        RegisteredPlugin rp = pluginManagement.getPluginByName(pluginId);
        if (rp == null) {
            log.error("{}Plugin {} does not exist", logPrefix, pluginId);
            return new PlatformPlugin(null, false, "Plugin " + pluginId + " does not exist");
        }
        else if (!rp.getState().equalsIgnoreCase("STARTED")) {
            log.error("{}Plugin {} is not started", logPrefix, pluginId);
            return new PlatformPlugin(null, false, "Plugin " + pluginId + " is not running");
        }
        else if (rp.getHealth().getOverallStatus().getHealthState() == HealthState.FAILED) {
            log.error("{}Plugin {} is failed - {}", logPrefix, pluginId, rp.getHealth().getOverallStatus().getHealthComment());
            return new PlatformPlugin(null, false, "Plugin " + pluginId + " is failed - " + rp.getHealth().getOverallStatus().getHealthComment());
        }
        else {
            return new PlatformPlugin(rp.getPlugin(), true, null);
        }
    }

    private ResponseEntity<JSONResponse> checkPlugin(JSONResponse jsonResponse, PlatformPlugin plug, PluginOperation requestedOp) {
        String logPrefix = "checkPlugin() - ";
        log.trace("{}Entering method", logPrefix);
        HttpStatus returnStatus;
        log.debug("{}Checking plugin status", logPrefix);

        if (!plug.success) {
            log.debug("{}Plugin not operational: {}", logPrefix, plug.errorMessage);

            jsonResponse.setErrorMessage(plug.errorMessage);
            returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(returnStatus).body(jsonResponse);
        }
        if (!plug.plugin.getValidOperations().contains(requestedOp)) {
            log.debug("{}Operation {} is not supported", logPrefix, requestedOp.name());
            jsonResponse.setErrorMessage(requestedOp.name() + " operation is not supported");
            returnStatus = HttpStatus.METHOD_NOT_ALLOWED;
            return ResponseEntity.status(returnStatus).body(jsonResponse);
        }
        log.debug("{}Plugin checks passed", logPrefix);
        return null;
    }

    @AllArgsConstructor
    private class PlatformPlugin {

        public final PlatformConnectorPlugin plugin;
        public final boolean success;
        public final String errorMessage;
    }

    @AllArgsConstructor
    private class SecurityHeader {

        private static final String ORG_HDR = "ININ-Organization-Id";
        private static final String COR_HDR = "ININ-Correlation-Id";
        private static final String REQ_HDR = "ININ-Request-Id";
        private static final String AUTH_HDR = "X-PlatformConnector-AuthKey";

        private final String orgHeader;
        private final String corHeader;
        private final String reqHeader;
        private final String authHeader;

        private final String organisationId;
        private final String currentPassword;

        private SecurityHeader(WebRequest webReq, String organisationId, String currentPassword) {
            final String logPrefix = "ctor() - ";

            log.debug("{}Reading GCloud Headers", logPrefix);
            orgHeader = (webReq.getHeader(ORG_HDR) == null ? "" : webReq.getHeader(ORG_HDR));
            corHeader = (webReq.getHeader(COR_HDR) == null ? "" : webReq.getHeader(COR_HDR));
            reqHeader = (webReq.getHeader(REQ_HDR) == null ? "" : webReq.getHeader(REQ_HDR));
            authHeader = (webReq.getHeader(AUTH_HDR) == null ? "" : webReq.getHeader(AUTH_HDR));
            this.organisationId = organisationId;
            this.currentPassword = currentPassword;
        }

        public boolean checkMissingHeaders() {
            final String logPrefix = "checkMissingHeaders() - ";
            log.trace("{}Checking authentication headers exist", logPrefix);
            return (orgHeader.isBlank() || corHeader.isBlank() || reqHeader.isBlank() || authHeader.isBlank());
        }

        public boolean isAuthenticationValid() {
            final String logPrefix = "isAuthenticationValid() - ";
            log.trace("{}Checking authentication headers match required values", logPrefix);
            if (!orgHeader.equals(organisationId)) {
                log.trace("{}>> OrganisationId is invalid", logPrefix);
            }
            if (!authHeader.equals(currentPassword)) {
                log.trace("{}>> Auth header does not match passwordF", logPrefix);
            }
            return (orgHeader.equals(organisationId) && authHeader.equals(currentPassword));
        }
    }
}
