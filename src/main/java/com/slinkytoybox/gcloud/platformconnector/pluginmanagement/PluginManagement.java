/*
 *   platformconnector - PluginManagement.java
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
package com.slinkytoybox.gcloud.platformconnector.pluginmanagement;

import com.slinkytoybox.gcloud.platformconnectorplugin.PlatformConnectorPlugin;
import com.slinkytoybox.gcloud.platformconnectorplugin.health.HealthResult;
import com.slinkytoybox.gcloud.platformconnectorplugin.health.HealthStatus;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.pf4j.spring.SpringPluginManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Slf4j
public class PluginManagement {

    @Autowired
    private SpringPluginManager pluginManager;

    private final Map<String, RegisteredPlugin> pluginMap = new HashMap<>();

    @PostConstruct
    public Map<String, RegisteredPlugin> getAllPlugins() {
        final String logPrefix = "getAllPlugins() - ";
        log.trace("{}Entering Method", logPrefix);
        log.debug("{}Getting all registered plugins", logPrefix);
        pluginMap.clear();
        for (PluginWrapper plug : pluginManager.getPlugins()) {
            String pluginId = plug.getPluginId();
            log.trace("{}Evaluating plugin {}", logPrefix, pluginId);
            List<PlatformConnectorPlugin> pcpList = pluginManager.getExtensions(PlatformConnectorPlugin.class, pluginId);
            PlatformConnectorPlugin pcp = null;
            if (pcpList.size() != 1) {
                log.warn("{}Plugin {} has more than one extension of type PlatformConnectorPlugin. This plugin will not be usable: {}", logPrefix, pcpList);
            }
            else {
                pcp = pcpList.get(0);
            }
            RegisteredPlugin rp = new RegisteredPlugin()
                    .setId(pluginId)
                    .setPlugin(pcp)
                    .setHealth(pcp == null ? new HealthResult().setOverallStatus(HealthStatus.UNKNOWN) : pcp.getPluginHealth())
                    .setDescription(plug.getDescriptor().getPluginDescription())
                    .setVersion(plug.getDescriptor().getVersion())
                    .setCls(plug.getDescriptor().getPluginClass())
                    .setProvider(plug.getDescriptor().getProvider())
                    .setState(plug.getPluginState().name())
                    ;
            pluginMap.put(pluginId, rp);
        }
        log.info("{}Found {} registered plugins", logPrefix, pluginMap.size());
        return pluginMap;
    }

    public RegisteredPlugin getPluginByName(String pluginName) {
        final String logPrefix = "getPluginByName() - ";
        log.trace("{}Entering Method", logPrefix);
        log.debug("{}Finding plugin {}", logPrefix, pluginName);
        if (pluginMap.containsKey(pluginName)) {
            return pluginMap.get(pluginName);
        }
        else {
            log.error("{}Plugin {} is not registered, returning null", logPrefix, pluginName);
            return null;
        }
    }

    public boolean startPlugin(String pluginName) {
        final String logPrefix = "startPlugin() - ";
        log.trace("{}Entering Method", logPrefix);
        log.debug("{}Finding plugin {}", logPrefix, pluginName);
        if (pluginMap.containsKey(pluginName)) {
            log.info("{}Attempting to start {}", logPrefix, pluginName);
            pluginMap.get(pluginName).setState(pluginManager.startPlugin(pluginName).name());
            log.info("{}Plugin {} state {}", logPrefix, pluginName, pluginMap.get(pluginName).getState());
            return pluginMap.get(pluginName).getState().equalsIgnoreCase("STARTED");
        }
        else {
            log.warn("{}Plugin {} is not registered, not doing anything", logPrefix, pluginName);
            return false;
        }
    }

    public boolean stopPlugin(String pluginName) {
        final String logPrefix = "stopPlugin() - ";
        log.trace("{}Entering Method", logPrefix);
        log.debug("{}Finding plugin {}", logPrefix, pluginName);
        if (pluginMap.containsKey(pluginName)) {
            log.info("{}Attempting to stop {}", logPrefix, pluginName);
            pluginMap.get(pluginName).setState(pluginManager.stopPlugin(pluginName).name());
            log.info("{}Plugin {} state {}", logPrefix, pluginName, pluginMap.get(pluginName).getState());
            return pluginMap.get(pluginName).getState().equalsIgnoreCase("STOPPED");
        }
        else {
            log.warn("{}Plugin {} is not registered, not doing anything", logPrefix, pluginName);
            return false;
        }
    }

    public boolean unloadPlugin(String pluginName) {
        final String logPrefix = "unloadPlugin() - ";
        log.trace("{}Entering Method", logPrefix);
        log.debug("{}Finding plugin {}", logPrefix, pluginName);
        if (pluginMap.containsKey(pluginName)) {
            log.info("{}Attempting to unload {}", logPrefix, pluginName);

            if (pluginManager.unloadPlugin(pluginName)) {
                log.info("{}Plugin successfully unloaded", logPrefix);
                pluginMap.remove(pluginName);
                return true;
            }
            else {
                log.error("{}Plugin unload failed", logPrefix);
                return false;
            }
        }
        else {
            log.warn("{}Plugin {} is not registered, not doing anything", logPrefix, pluginName);
            return false;
        }
    }

    public boolean loadPlugin(String pluginFile) {
        final String logPrefix = "loadPlugin() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Attempting to load {}", logPrefix, pluginFile);
        Path plugPath = Path.of("plugins/" + pluginFile);
        String ret = pluginManager.loadPlugin(plugPath);
        log.info("{}Load Plugin returned: {}", logPrefix, ret);
         return true;
    }

}
