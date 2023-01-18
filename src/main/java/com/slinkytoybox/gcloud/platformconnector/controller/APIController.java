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

import com.slinkytoybox.gcloud.platformconnector.businesslogic.PluginLogic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import com.slinkytoybox.gcloud.platformconnector.dto.request.*;
import com.slinkytoybox.gcloud.platformconnector.dto.response.*;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private PluginLogic pluginLogic;

    @PostMapping("/{pluginId}/record/search")
    public ResponseEntity<JSONResponse> getRecordSearch(WebRequest webReq, @PathVariable("pluginId") String pluginId, @RequestBody JSONReadRequest request) {
        String logPrefix = "getRecordSearch() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Processing POST /{}/record/search", logPrefix, pluginId);

        return pluginLogic.doSearch(webReq, pluginId, request, null);
    }

    @GetMapping("/{pluginId}/record/{recordId}")
    public ResponseEntity<JSONResponse> getItemSingle(WebRequest webReq, @PathVariable("pluginId") String pluginId, @PathVariable("recordId") String recordId) {
        String logPrefix = "getItemSingle() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Processing GET /{}/record/{}", logPrefix, pluginId, recordId);
        return pluginLogic.doSearch(webReq, pluginId, null, recordId);

    }

    @PatchMapping("/{pluginId}/record/{recordId}")
    public ResponseEntity<JSONResponse> updateItem(WebRequest webReq, @PathVariable("pluginId") String pluginId, @PathVariable("recordId") String recordId, @RequestBody JSONUpdateRequest request) {
        String logPrefix = "updateItem() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Processing PATCH /{}/record/{}", logPrefix, pluginId, recordId);
        return pluginLogic.doUpdate(webReq, pluginId, request, recordId);
    }

    @PostMapping("/{pluginId}/record/")
    public ResponseEntity<JSONResponse> createItem(WebRequest webReq, @PathVariable("pluginId") String pluginId, @RequestBody JSONCreateRequest request) {
        String logPrefix = "createItem() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Processing POST /{}/record/", logPrefix, pluginId);
        return pluginLogic.doCreate(webReq, pluginId, request);
    }

    @DeleteMapping("/{pluginId}/record/{recordId}")
    public ResponseEntity<JSONResponse> deleteItem(WebRequest webReq, @PathVariable("pluginId") String pluginId, @PathVariable("recordId") String recordId, @RequestBody JSONDeleteRequest request) {
        String logPrefix = "deleteItem() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Processing DELETE /{}/record/{}", logPrefix, pluginId, recordId);
        return pluginLogic.doDelete(webReq, pluginId, recordId, request);
    }

}
