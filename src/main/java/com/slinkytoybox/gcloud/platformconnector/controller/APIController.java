/*
 *   platformconnector - APIController.java
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
package com.slinkytoybox.gcloud.platformconnector.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import com.slinkytoybox.gcloud.platformconnector.dto.request.*;
import com.slinkytoybox.gcloud.platformconnector.dto.response.*;
import com.slinkytoybox.gcloud.platformconnector.pluginmanagement.PluginManagement;
import com.slinkytoybox.gcloud.platformconnector.pluginmanagement.RegisteredPlugin;
import com.slinkytoybox.gcloud.platformconnectorplugin.PlatformConnectorPlugin;
import com.slinkytoybox.gcloud.platformconnectorplugin.PluginOperation;
import com.slinkytoybox.gcloud.platformconnectorplugin.health.HealthState;
import com.slinkytoybox.gcloud.platformconnectorplugin.request.*;
import com.slinkytoybox.gcloud.platformconnectorplugin.response.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/")
public class APIController {

    private final String ORGANISATION_HEADER = "ININ-Organization-Id";
    private final String CORRELATION_HEADER = "ININ-Correlation-Id";
    private final String REQUEST_HEADER = "ININ-Request-Id";

    @Autowired
    private PluginManagement pluginManagement;

    @PostMapping("/{pluginId}/record/search")
    public ResponseEntity<JSONResponse> getRecordSearch(WebRequest webReq, @PathVariable("pluginId") String pluginId, @RequestBody JSONReadRequest request) {
        String logPrefix = "getRecordSearch() - ";
        log.trace("{}Entering method", logPrefix);
        log.debug("{}Checking GCloud Headers", logPrefix);
        final String orgHeader = (webReq.getHeader(ORGANISATION_HEADER) == null ? "" : webReq.getHeader(ORGANISATION_HEADER));
        final String corHeader = (webReq.getHeader(CORRELATION_HEADER) == null ? "" : webReq.getHeader(CORRELATION_HEADER));
        final String reqHeader = (webReq.getHeader(REQUEST_HEADER) == null ? "" : webReq.getHeader(REQUEST_HEADER));
        if (orgHeader.isEmpty() || corHeader.isEmpty() || reqHeader.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONErrorResponse().setErrorMessage("Invalid request, required headers missing"));
        }
        String requestId = reqHeader;
        logPrefix = "getRecordSearch() - " + (requestId == null ? "[null] - " : "[" + requestId + "] - ");
        log.info("{}Processing POST /{}/record/search", logPrefix, pluginId);
        log.debug("{}JSON Data: {}", logPrefix, request);
        JSONReadResponse jsonResponse = new JSONReadResponse(requestId);
        HttpStatus returnStatus;
        jsonResponse.setObjectDetails(new HashMap<>());
        jsonResponse.setPluginId(pluginId);
        PlatformPlugin plug = getPlugin(pluginId);
        if (!plug.success) {
            jsonResponse.setErrorMessage(plug.errorMessage);
            returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            return ResponseEntity.status(returnStatus).body(jsonResponse);
        }
        if (!plug.plugin.getValidOperations().contains(PluginOperation.READ)) {
            jsonResponse.setErrorMessage("READ operation is not supported");
            returnStatus = HttpStatus.METHOD_NOT_ALLOWED;
            return ResponseEntity.status(returnStatus).body(jsonResponse);
        }
        try {
            ReadRequest pluginRequest = new ReadRequest();
            pluginRequest.setRequestId(requestId);
            pluginRequest.setRequestDate(OffsetDateTime.now(ZoneId.of("Australia/Sydney")));
            pluginRequest.setSearchParameters(request.getSearchParameters());
            ReadResponse pluginResponse = (ReadResponse) plug.plugin.getResponseFromRequest(pluginRequest);
            if (pluginResponse.getSuccess()) {
                log.info("{}Successfully looked up data", logPrefix);
                jsonResponse.setObjectId(pluginResponse.getObjectId());
                jsonResponse.setObjectDetails(pluginResponse.getObjectDetails());
                returnStatus = HttpStatus.OK;
            }
            else {
                log.warn("{}Could not find record", logPrefix);
                jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                returnStatus = HttpStatus.NOT_FOUND;
            }
        }
        catch (Exception ex) {
            log.error("{}Exception when looking up record", logPrefix, ex);
            jsonResponse.setErrorMessage(ex.getMessage());
            returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(returnStatus).body(jsonResponse);
    }

    @GetMapping("/{pluginId}/record/{recordId}")
    public ResponseEntity<JSONResponse> getItemSingle(WebRequest webReq, @PathVariable("pluginId") String pluginId, @PathVariable("recordId") String recordId) {
        String logPrefix = "getItemSingle() - ";
        log.trace("{}Entering method", logPrefix);
        log.debug("{}Checking GCloud Headers", logPrefix);
        final String orgHeader = (webReq.getHeader(ORGANISATION_HEADER) == null ? "" : webReq.getHeader(ORGANISATION_HEADER));
        final String corHeader = (webReq.getHeader(CORRELATION_HEADER) == null ? "" : webReq.getHeader(CORRELATION_HEADER));
        final String reqHeader = (webReq.getHeader(REQUEST_HEADER) == null ? "" : webReq.getHeader(REQUEST_HEADER));
        if (orgHeader.isEmpty() || corHeader.isEmpty() || reqHeader.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONErrorResponse().setErrorMessage("Invalid request, required headers missing"));
        }
        String requestId = reqHeader;
        log.info("{}Processing GET /{}/record/{}", logPrefix, pluginId, recordId);
        
        HttpStatus returnStatus;
        JSONReadResponse jsonResponse = new JSONReadResponse(requestId);
        jsonResponse.setPluginId(pluginId);
        jsonResponse.setObjectDetails(new HashMap<>());
        PlatformPlugin plug = getPlugin(pluginId);
        if (!plug.success) {
            jsonResponse.setErrorMessage(plug.errorMessage);
            return ResponseEntity.internalServerError().body(jsonResponse);
        }
        if (!plug.plugin.getValidOperations().contains(PluginOperation.READ)) {
            jsonResponse.setErrorMessage("READ operation is not supported");
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(jsonResponse);
        }
        try {
            ReadRequest pluginRequest = new ReadRequest();
            pluginRequest.setRequestId(requestId);
            pluginRequest.setRequestDate(OffsetDateTime.now(ZoneId.of("Australia/Sydney")));
            pluginRequest.setObjectId(recordId);
            ReadResponse pluginResponse = (ReadResponse) plug.plugin.getResponseFromRequest(pluginRequest);
            if (pluginResponse.getSuccess()) {
                log.info("{}Successfully looked up data", logPrefix);
                jsonResponse.setObjectId(pluginResponse.getObjectId());
                jsonResponse.setObjectDetails(pluginResponse.getObjectDetails());
                returnStatus = HttpStatus.OK;
            }
            else {
                log.warn("{}Could not find record", logPrefix);
                jsonResponse.setErrorMessage(pluginResponse.getErrorMessage());
                returnStatus = HttpStatus.NOT_FOUND;
            }
        }
        catch (Exception ex) {
            log.error("{}Exception when looking up record", logPrefix, ex);
            jsonResponse.setErrorMessage(ex.getMessage());
            returnStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(returnStatus).body(jsonResponse);
    }

    @PatchMapping("/{pluginId}/record/{recordId}")
    public ResponseEntity<JSONResponse> updateItem(WebRequest webReq, @PathVariable("pluginId") String pluginId, @PathVariable("recordId") String recordId, @RequestBody JSONUpdateRequest request) {
        String logPrefix = "updateItem() - ";
        log.trace("{}Entering method", logPrefix);
        log.debug("{}Checking GCloud Headers", logPrefix);
        final String orgHeader = (webReq.getHeader(ORGANISATION_HEADER) == null ? "" : webReq.getHeader(ORGANISATION_HEADER));
        final String corHeader = (webReq.getHeader(CORRELATION_HEADER) == null ? "" : webReq.getHeader(CORRELATION_HEADER));
        final String reqHeader = (webReq.getHeader(REQUEST_HEADER) == null ? "" : webReq.getHeader(REQUEST_HEADER));
        if (orgHeader.isEmpty() || corHeader.isEmpty() || reqHeader.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONErrorResponse().setErrorMessage("Invalid request, required headers missing"));
        }
        String requestId = reqHeader;
        log.info("{}Processing PATCH /{}/record/{}", logPrefix, pluginId, recordId);
        log.debug("{}JSON Data: {}", logPrefix, request);
        JSONUpdateResponse jsonResponse = new JSONUpdateResponse(requestId);
        jsonResponse.setPluginId(pluginId);
        PlatformPlugin plug = getPlugin(pluginId);
        if (!plug.success) {
            jsonResponse.setErrorMessage(plug.errorMessage);
            return ResponseEntity.internalServerError().body(jsonResponse);
        }
        if (!plug.plugin.getValidOperations().contains(PluginOperation.UPDATE)) {
            jsonResponse.setErrorMessage("UPDATE operation is not supported");
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(jsonResponse);
        }
        return null;
    }

    @PostMapping("/{pluginId}/record/{recordId}")
    public ResponseEntity<JSONResponse> createItem(WebRequest webReq, @PathVariable("pluginId") String pluginId, @PathVariable("recordId") String recordId, @RequestBody JSONCreateRequest request) {
        String logPrefix = "createItem() - ";
        log.trace("{}Entering method", logPrefix);
        log.debug("{}Checking GCloud Headers", logPrefix);
        final String orgHeader = (webReq.getHeader(ORGANISATION_HEADER) == null ? "" : webReq.getHeader(ORGANISATION_HEADER));
        final String corHeader = (webReq.getHeader(CORRELATION_HEADER) == null ? "" : webReq.getHeader(CORRELATION_HEADER));
        final String reqHeader = (webReq.getHeader(REQUEST_HEADER) == null ? "" : webReq.getHeader(REQUEST_HEADER));
        if (orgHeader.isEmpty() || corHeader.isEmpty() || reqHeader.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONErrorResponse().setErrorMessage("Invalid request, required headers missing"));
        }
        String requestId = reqHeader;
        log.info("{}Processing POST /{}/record/{}", logPrefix, pluginId, recordId);
        log.debug("{}JSON Data: {}", logPrefix, request);
        JSONCreateResponse jsonResponse = new JSONCreateResponse(requestId);
        jsonResponse.setPluginId(pluginId);
        PlatformPlugin plug = getPlugin(pluginId);
        if (!plug.success) {
            jsonResponse.setErrorMessage(plug.errorMessage);
            return ResponseEntity.internalServerError().body(jsonResponse);
        }
        if (!plug.plugin.getValidOperations().contains(PluginOperation.CREATE)) {
            jsonResponse.setErrorMessage("CREATE operation is not supported");
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(jsonResponse);
        }
        return null;
    }

    @DeleteMapping("/{pluginId}/record/{recordId}")
    public ResponseEntity<JSONResponse> deleteItem(WebRequest webReq, @PathVariable("pluginId") String pluginId, @PathVariable("recordId") String recordId, @RequestBody JSONDeleteRequest request) {
        String logPrefix = "deleteItem() - ";
        log.trace("{}Entering method", logPrefix);
        log.debug("{}Checking GCloud Headers", logPrefix);
        final String orgHeader = (webReq.getHeader(ORGANISATION_HEADER) == null ? "" : webReq.getHeader(ORGANISATION_HEADER));
        final String corHeader = (webReq.getHeader(CORRELATION_HEADER) == null ? "" : webReq.getHeader(CORRELATION_HEADER));
        final String reqHeader = (webReq.getHeader(REQUEST_HEADER) == null ? "" : webReq.getHeader(REQUEST_HEADER));
        if (orgHeader.isEmpty() || corHeader.isEmpty() || reqHeader.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONErrorResponse().setErrorMessage("Invalid request, required headers missing"));
        }
        String requestId = reqHeader;
        log.info("{}Processing DELETE /{}/record/{}", logPrefix, pluginId, recordId);
        log.debug("{}JSON Data: {}", logPrefix, request);
        JSONDeleteResponse jsonResponse = new JSONDeleteResponse(requestId);
        jsonResponse.setPluginId(pluginId);
        PlatformPlugin plug = getPlugin(pluginId);
        if (!plug.success) {
            jsonResponse.setErrorMessage(plug.errorMessage);
            return ResponseEntity.internalServerError().body(jsonResponse);
        }
        if (!plug.plugin.getValidOperations().contains(PluginOperation.DELETE)) {
            jsonResponse.setErrorMessage("DELETE operation is not supported");
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(jsonResponse);
        }
        return null;
    }

    private PlatformPlugin getPlugin(String pluginId) {
        final String logPrefix = "getPlugin() - ";
        log.trace("{}Entering method", logPrefix);
        PlatformPlugin resp = new PlatformPlugin();

        RegisteredPlugin rp = pluginManagement.getPluginByName(pluginId);
        if (rp == null) {
            log.error("{}Plugin {} does not exist", logPrefix, pluginId);
            resp.errorMessage = "Plugin " + pluginId + " does not exist";
            resp.success = false;
            resp.plugin = null;
        }
        else if (!rp.getState().equalsIgnoreCase("STARTED")) {
            log.error("{}Plugin {} is not started", logPrefix, pluginId);
            resp.errorMessage = "Plugin " + pluginId + " is not running";
            resp.success = false;
            resp.plugin = null;
        }
        else if (rp.getHealth().getOverallStatus().getHealthState() == HealthState.FAILED) {
            log.error("{}Plugin {} is failed - {}", logPrefix, pluginId, rp.getHealth().getOverallStatus().getHealthComment());
            resp.errorMessage = "Plugin " + pluginId + " is failed - " + rp.getHealth().getOverallStatus().getHealthComment();
            resp.success = false;
            resp.plugin = null;
        }
        else {
            resp.success = true;
            resp.plugin = rp.getPlugin();
        }
        return resp;
    }

    private class PlatformPlugin {

        public PlatformConnectorPlugin plugin;
        public boolean success;
        public String errorMessage;
    }

}
