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

import com.slinkytoybox.gcloud.platformconnector.dto.request.*;
import com.slinkytoybox.gcloud.platformconnector.dto.response.*;
import com.slinkytoybox.gcloud.platformconnector.pluginmanagement.PluginManagement;
import com.slinkytoybox.gcloud.platformconnector.pluginmanagement.RegisteredPlugin;
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

    public ResponseEntity<JSONResponse> doCreate(WebRequest webReq, String pluginId, JSONCreateRequest request) {
        String logPrefix = "doCreate() - ";
        log.trace("{}Entering method", logPrefix);
        HeaderDetails hdr = new HeaderDetails(webReq);
        if (hdr.orgHeader.isEmpty() || hdr.corHeader.isEmpty() || hdr.reqHeader.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONErrorResponse().setErrorMessage("Invalid request, required headers missing"));
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
        HttpStatus returnStatus;
        try {
            CreateRequest pluginRequest = new CreateRequest();
            pluginRequest.setRequestId(requestId);
            pluginRequest.setRequestDate(OffsetDateTime.now(ZoneId.of("Australia/Sydney")));
            pluginRequest.setObjectDetails(request.getNewDetails());
            CreateResponse pluginResponse = (CreateResponse) plug.plugin.getResponseFromRequest(pluginRequest);
            switch (pluginResponse.getStatus()) {
                case SUCCESS:
                    log.info("{}Successfully created item, ID: {}", logPrefix, pluginResponse.getObjectId());
                    jsonResponse.setObjectId(pluginResponse.getObjectId());
                    jsonResponse.setObjectDetails(pluginResponse.getObjectDetails());
                    returnStatus = HttpStatus.OK;
                    break;
                case RECORD_NOT_FOUND: // should never get this on create
                    jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                //fallthrough
                case MULTIPLE_RECORDS: // should never get this on create
                    jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                //fallthrough
                case FAILURE:
                    jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                //fallthrough
                default:
                    log.error("{}Failure creating record", logPrefix);
                    returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
        catch (Exception ex) {
            log.error("{}Exception when creating record", logPrefix, ex);
            jsonResponse.setErrorMessage(ex.getMessage());
            returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(returnStatus).body(jsonResponse);

    }

    public ResponseEntity<JSONResponse> doUpdate(WebRequest webReq, String pluginId, JSONUpdateRequest request, String recordId) {
        String logPrefix = "doUpdate() - ";
        log.trace("{}Entering method", logPrefix);
        HeaderDetails hdr = new HeaderDetails(webReq);
        if (hdr.orgHeader.isEmpty() || hdr.corHeader.isEmpty() || hdr.reqHeader.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONErrorResponse().setErrorMessage("Invalid request, required headers missing"));
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
        HttpStatus returnStatus;
        try {
            UpdateRequest pluginRequest = new UpdateRequest();
            pluginRequest.setRequestId(requestId);
            pluginRequest.setRequestDate(OffsetDateTime.now(ZoneId.of("Australia/Sydney")));
            pluginRequest.setNewDetails(request.getNewDetails());

            UpdateResponse pluginResponse = (UpdateResponse) plug.plugin.getResponseFromRequest(pluginRequest);
            switch (pluginResponse.getStatus()) {
                case SUCCESS:
                    log.info("{}Successfully updated data", logPrefix);
                    jsonResponse.setObjectId(pluginResponse.getObjectId());
                    jsonResponse.setObjectDetails(pluginResponse.getObjectDetails());
                    returnStatus = HttpStatus.OK;
                    break;
                case RECORD_NOT_FOUND:
                    log.error("{}Could not find record", logPrefix);
                    jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                    returnStatus = HttpStatus.NOT_FOUND;
                    break;
                case MULTIPLE_RECORDS:
                    log.error("{}Multiple records found", logPrefix);
                    jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                    returnStatus = HttpStatus.MULTIPLE_CHOICES;
                    break;
                case FAILURE:
                    jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                //fallthrough
                default:
                    log.error("{}Failure updating record", logPrefix);
                    returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
        catch (Exception ex) {
            log.error("{}Exception when updating record", logPrefix, ex);
            jsonResponse.setErrorMessage(ex.getMessage());
            returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(returnStatus).body(jsonResponse);

    }

    public ResponseEntity<JSONResponse> doSearch(WebRequest webReq, String pluginId, JSONReadRequest request, String recordId) {
        String logPrefix = "doSearch() - ";
        log.trace("{}Entering method", logPrefix);
        HeaderDetails hdr = new HeaderDetails(webReq);
        if (hdr.orgHeader.isEmpty() || hdr.corHeader.isEmpty() || hdr.reqHeader.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONErrorResponse().setErrorMessage("Invalid request, required headers missing"));
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
        HttpStatus returnStatus;
        try {
            ReadRequest pluginRequest = new ReadRequest();
            pluginRequest.setRequestId(requestId);
            pluginRequest.setRequestDate(OffsetDateTime.now(ZoneId.of("Australia/Sydney")));
            if (recordId != null) {
                pluginRequest.setObjectId(recordId);

            }
            else {
                pluginRequest.setSearchParameters(request.getSearchParameters());
            }
            ReadResponse pluginResponse = (ReadResponse) plug.plugin.getResponseFromRequest(pluginRequest);

            switch (pluginResponse.getStatus()) {
                case SUCCESS:
                    log.info("{}Successfully looked up data", logPrefix);
                    jsonResponse.setObjectId(pluginResponse.getObjectId());
                    jsonResponse.setObjectDetails(pluginResponse.getObjectDetails());
                    returnStatus = HttpStatus.OK;
                    break;
                case RECORD_NOT_FOUND:
                    log.warn("{}Could not find record", logPrefix);
                    jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                    returnStatus = HttpStatus.NOT_FOUND;
                    break;
                case MULTIPLE_RECORDS:
                    log.error("{}Multiple records found", logPrefix);
                    jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                    returnStatus = HttpStatus.MULTIPLE_CHOICES;
                    break;
                case FAILURE:
                    jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                //fallthrough
                default:
                    log.error("{}Failure looking up record", logPrefix);
                    returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
        catch (Exception ex) {
            log.error("{}Exception when looking up record", logPrefix, ex);
            jsonResponse.setErrorMessage(ex.getMessage());
            returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(returnStatus).body(jsonResponse);
    }

    public ResponseEntity<JSONResponse> doDelete(WebRequest webReq, String pluginId, String recordId, JSONDeleteRequest request) {
        String logPrefix = "doDelete() - ";
        log.trace("{}Entering method", logPrefix);
        HeaderDetails hdr = new HeaderDetails(webReq);
        if (hdr.orgHeader.isEmpty() || hdr.corHeader.isEmpty() || hdr.reqHeader.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONErrorResponse().setErrorMessage("Invalid request, required headers missing"));
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
        HttpStatus returnStatus;
        try {
            DeleteRequest pluginRequest = new DeleteRequest();
            pluginRequest.setRequestId(requestId);
            pluginRequest.setRequestDate(OffsetDateTime.now(ZoneId.of("Australia/Sydney")));
            pluginRequest.setObjectId(recordId);
            DeleteResponse pluginResponse = (DeleteResponse) plug.plugin.getResponseFromRequest(pluginRequest);
            switch (pluginResponse.getStatus()) {
                case SUCCESS:
                    log.info("{}Successfully created item, ID: {}", logPrefix, pluginResponse.getObjectId());
                    jsonResponse.setObjectId(pluginResponse.getObjectId());
                    returnStatus = HttpStatus.OK;
                    break;
                case RECORD_NOT_FOUND:
                    log.error("{}Could not find record", logPrefix);
                    jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                    returnStatus = HttpStatus.NOT_FOUND;
                    break;
                case MULTIPLE_RECORDS:
                    log.error("{}Multiple records found", logPrefix);
                    jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                    returnStatus = HttpStatus.MULTIPLE_CHOICES;
                    break;
                case FAILURE:
                    jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                //fallthrough
                default:
                    log.error("{}Failure creating record", logPrefix);
                    returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
        catch (Exception ex) {
            log.error("{}Exception when deleting record", logPrefix, ex);
            jsonResponse.setErrorMessage(ex.getMessage());
            returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(returnStatus).body(jsonResponse);
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
    private class HeaderDetails {

        private static final String ORG_HDR = "ININ-Organization-Id";
        private static final String COR_HDR = "ININ-Correlation-Id";
        private static final String REQ_HDR = "ININ-Request-Id";

        public final String orgHeader;
        public final String corHeader;
        public final String reqHeader;

        private HeaderDetails(WebRequest webReq) {
            final String logPrefix = "ctor() - ";

            log.debug("{}Checking GCloud Headers", logPrefix);
            orgHeader = (webReq.getHeader(ORG_HDR) == null ? "" : webReq.getHeader(ORG_HDR));
            corHeader = (webReq.getHeader(COR_HDR) == null ? "" : webReq.getHeader(COR_HDR));
            reqHeader = (webReq.getHeader(REQ_HDR) == null ? "" : webReq.getHeader(REQ_HDR));

        }
    }
}
