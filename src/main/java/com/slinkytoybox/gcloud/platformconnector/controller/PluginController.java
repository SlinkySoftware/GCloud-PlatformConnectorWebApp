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
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.WebRequest;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Slf4j
@Controller
@RequestMapping("/plugins")

public class PluginController {

    @Autowired
    private Environment env;

    @Autowired
    private PluginManagement pluginManagement;

    @GetMapping(path = "/", produces = "text/html")
    public String rootGet(Model model) {
        final String logPrefix = "rootGet() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Processing GET /plugins/", logPrefix);
        model = rootModel(model);
        return "plugins";
    }

    @PostMapping(path = "/", produces = "text/html")
    public String rootPost(Model model, WebRequest request) {
        final String logPrefix = "rootPost() - ";
        log.trace("{}Entering method", logPrefix);

        log.info("{}Processing POST /plugins/", logPrefix);
        String action = request.getParameter("action");
        if (action == null || action.isEmpty()) {
            log.error("{}Action not specified in request", logPrefix);
            model.addAttribute("showError", 1);
            model.addAttribute("message", "No Action Specified");
        }
        else if (action.equalsIgnoreCase("change")) {
            log.debug("{}Processing state change", logPrefix);
            String pluginId = request.getParameter("plugin");
            String state = request.getParameter("state");
            if (pluginId == null || pluginId.isEmpty() || state == null || state.isEmpty()) {
                log.error("{}Plugin or state not specified in request", logPrefix);
                model.addAttribute("showError", 1);
                model.addAttribute("message", "Plugin or State not specified in request");
            }
            else {
                log.trace("{}Got plugin ID {} from request", logPrefix, pluginId);
                Integer stateInt = -1;
                try {
                    stateInt = Integer.valueOf(state);
                }
                catch (NumberFormatException ex) {
                    log.warn("{}Exception converting '{}' to integer", logPrefix, state);
                }
                boolean status = false;
                try {
                    switch (stateInt) {
                        case 0 -> { // unload 
                            log.info("{}Attempting to Unload plugin {}", logPrefix, pluginId);
                            status = pluginManagement.unloadPlugin(pluginId);
                        }
                        case 1 -> { // start
                            log.info("{}Attempting to Start plugin {}", logPrefix, pluginId);
                            status = pluginManagement.startPlugin(pluginId);
                        }
                        case 2 -> {// stop
                            log.info("{}Attempting to Stop plugin {}", logPrefix, pluginId);
                            status = pluginManagement.stopPlugin(pluginId);
                        }
                        default -> {
                            // not valid
                            log.error("{}Invalid state request: {}", logPrefix, state);
                            model.addAttribute("showError", 1);
                            model.addAttribute("message", "Invalid state request");
                        }
                    }
                }
                catch (Exception ex) {
                    log.error("{}Exception while changing state of plugin {}", logPrefix, pluginId, ex);
                    model.addAttribute("showError", 1);
                    model.addAttribute("message", ex);
                }

                if (!status && !model.containsAttribute("showError")) {
                    log.warn("{}Changing state failed for plugin {}", logPrefix, pluginId);
                    model.addAttribute("showError", 1);
                    model.addAttribute("message", "Unknown error changing state");
                }
            }
        }

        else if (action.equalsIgnoreCase("load")) {
            log.debug("{}Processing load new plugin", logPrefix);
            String fileName = request.getParameter("filename");
            if (fileName == null || fileName.isEmpty()) {
                log.error("{}Filename not specified in request", logPrefix);
                model.addAttribute("showError", 1);
                model.addAttribute("message", "Filename not speified");
            }
            else {
                log.info("{}Attempting to load plugin {}", logPrefix, fileName);
                boolean status = false;
                try {
                    status = pluginManagement.loadPlugin(fileName);
                }
                catch (Exception ex) {
                    log.error("{}Exception while loading plugin {}", logPrefix, fileName, ex);
                    model.addAttribute("showError", 1);
                    model.addAttribute("message", ex);
                }

                if (!status && !model.containsAttribute("showError")) {
                    log.warn("{}Unable to load plugin {}", logPrefix, fileName);
                    model.addAttribute("showError", 1);
                    model.addAttribute("message", "Unknown error loading plugin");
                }
            }

        }
        else {
            log.error("{}Invalid action has been specified", logPrefix);
            model.addAttribute("showError", 1);
            model.addAttribute("message", "Invalid Action Specified");
        }
        model = rootModel(model);
        return "plugins";
    }

    private Model rootModel(Model model) {
        final String logPrefix = "rootModel() - ";
        log.trace("{}Entering method", logPrefix);
        Map<String, RegisteredPlugin> plugs = pluginManagement.getAllPlugins();
        List<RegisteredPlugin> rps = new ArrayList<>();
        plugs.values().forEach(v -> rps.add(v));
        model.addAttribute("plugins", rps);
        log.trace("{}Model: {}", logPrefix, model);
        return model;
    }

    @GetMapping(path = "/all", produces = "application/json")
    public ResponseEntity<Object> all() {
        final String logPrefix = "all() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Processing GET /plugins/all", logPrefix);
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
