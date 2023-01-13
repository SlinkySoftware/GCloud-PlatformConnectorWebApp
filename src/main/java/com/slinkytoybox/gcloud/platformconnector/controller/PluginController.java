/*
 *   platformconnector - PluginController.java
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

import com.slinkytoybox.gcloud.platformconnector.pluginmanagement.PluginManagement;
import com.slinkytoybox.gcloud.platformconnector.pluginmanagement.RegisteredPlugin;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Slf4j
@RestController
@RequestMapping("/plugins")

public class PluginController {

    @Autowired
    private Environment env;

    @Autowired
    private PluginManagement pluginManagement;

    @GetMapping(path = "/", produces = "application/json")
    public ResponseEntity<Object> plugins() {
        final String logPrefix = "plugins() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Processing GET /plugins/", logPrefix);
        Map<String, RegisteredPlugin> plugs = pluginManagement.getAllPlugins();

        return ResponseEntity.status(HttpStatus.OK).body(plugs);
    }

    @GetMapping(path = "/start/{pluginId}", produces = "application/json")
    public ResponseEntity<Object> startPlugin(@PathVariable("pluginId") String pluginId) {
        final String logPrefix = "restartPlugin() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Processing GET /plugins/start/{}", logPrefix, pluginId);
        pluginManagement.startPlugin(pluginId);
        Map<String, RegisteredPlugin> plugs = pluginManagement.getAllPlugins();

        return ResponseEntity.status(HttpStatus.OK).body(plugs);
    }

    @GetMapping(path = "/stop/{pluginId}", produces = "application/json")
    public ResponseEntity<Object> stopPlugin(@PathVariable("pluginId") String pluginId) {
        final String logPrefix = "stopPlugin() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Processing GET /plugins/stop/{}", logPrefix, pluginId);
        pluginManagement.stopPlugin(pluginId);
        Map<String, RegisteredPlugin> plugs = pluginManagement.getAllPlugins();

        return ResponseEntity.status(HttpStatus.OK).body(plugs);
    }

    @GetMapping(path = "/unload/{pluginId}", produces = "application/json")
    public ResponseEntity<Object> unloadPlugin(@PathVariable("pluginId") String pluginId) {
        final String logPrefix = "unloadPlugin() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Processing GET /plugins/unload/{}", logPrefix, pluginId);
        pluginManagement.unloadPlugin(pluginId);
        Map<String, RegisteredPlugin> plugs = pluginManagement.getAllPlugins();

        return ResponseEntity.status(HttpStatus.OK).body(plugs);
    }

    @GetMapping(path = "/load/{pluginId}", produces = "application/json")
    public ResponseEntity<Object> loadPlugin(@PathVariable("pluginId") String pluginPath) {
        final String logPrefix = "loadPlugin() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Processing GET /plugins/load/{}", logPrefix, pluginPath);
        pluginManagement.loadPlugin(pluginPath);
        Map<String, RegisteredPlugin> plugs = pluginManagement.getAllPlugins();

        return ResponseEntity.status(HttpStatus.OK).body(plugs);
    }
}
