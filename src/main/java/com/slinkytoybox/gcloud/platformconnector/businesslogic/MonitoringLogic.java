/*
 *   platformconnector - MonitoringLogic.java
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

import com.slinkytoybox.gcloud.platformconnector.dto.response.monitoring.*;
import com.slinkytoybox.gcloud.platformconnector.pluginmanagement.PluginManagement;
import com.slinkytoybox.gcloud.platformconnector.pluginmanagement.RegisteredPlugin;
import com.slinkytoybox.gcloud.platformconnectorplugin.health.HealthMetric;
import com.slinkytoybox.gcloud.platformconnectorplugin.health.HealthResult;
import com.slinkytoybox.gcloud.platformconnectorplugin.health.HealthState;
import com.slinkytoybox.gcloud.platformconnectorplugin.health.HealthStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Slf4j
@Component
public class MonitoringLogic {

    @Autowired
    private PluginManagement pluginManagement;

    public List<PluginDiscoveryResponse> getPluginDiscovery() {
        final String logPrefix = "getPluginDiscovery() - ";
        log.trace("{}Entering method", logPrefix);
        Map<String, RegisteredPlugin> allPlugins = pluginManagement.getAllPlugins();

        List<PluginDiscoveryResponse> discoveryResponse = new ArrayList<>();
        allPlugins.keySet().forEach((plugName) -> {
            log.trace("{}Adding Plugin {} to the discovery set", logPrefix, plugName);
            discoveryResponse.add(new PluginDiscoveryResponse()
                    .setPluginId(plugName));
        });
        log.debug("{}Discovered plugins: {}", logPrefix, discoveryResponse);
        return discoveryResponse;
    }

    public List<ComponentDiscoveryResponse> getComponentDiscovery() {
        final String logPrefix = "getComponentDiscovery() - ";
        log.trace("{}Entering method", logPrefix);
        Map<String, RegisteredPlugin> allPlugins = pluginManagement.getAllPlugins();

        List<ComponentDiscoveryResponse> discoveryResponse = new ArrayList<>();

        allPlugins.forEach((plugName, plugin) -> {
            log.trace("{}Scanning plugin {} for components", logPrefix, plugName);
            if (plugin.getState().equals("STARTED")) {
                HealthResult health = plugin.getPlugin().getPluginHealth();
                if (health.getComponentStatus() != null) {
                    log.trace("{}Iterating available components", logPrefix);
                    health.getComponentStatus().keySet().forEach(componentName -> {
                        log.trace("{}Adding component {} to the dicovery set", logPrefix, componentName);
                        discoveryResponse.add(new ComponentDiscoveryResponse()
                                .setPluginId(plugName)
                                .setComponentName(plugName + "::" + componentName)
                        );
                    }
                    );
                }
            }
            else {
                log.trace("{}No component health available for {}", logPrefix, plugName);
            }
        }
        );
        log.debug("{}Discovered components: {}", logPrefix, discoveryResponse);
        return discoveryResponse;
    }

    public List<MetricDiscoveryResponse> getMetricDiscovery() {
        final String logPrefix = "getMetricDiscovery() - ";
        log.trace("{}Entering method", logPrefix);
        Map<String, RegisteredPlugin> allPlugins = pluginManagement.getAllPlugins();
        List<MetricDiscoveryResponse> discoveryResponse = new ArrayList<>();
        allPlugins.forEach((plugName, plugin) -> {
            log.trace("{}Scanning plugin {} for components", logPrefix, plugName);
            if (plugin.getState().equals("STARTED")) {
                HealthResult health = plugin.getPlugin().getPluginHealth();
                if (health.getMetrics() != null) {
                    log.trace("{}Iterating available metrics", logPrefix);
                    for (HealthMetric metric : health.getMetrics()) {
                        log.trace("{}Adding metric  {} to the dicovery set", logPrefix, metric.getMetricName());
                        discoveryResponse.add(new MetricDiscoveryResponse()
                                .setPluginId(plugName)
                                .setMetricName(plugName + "::" + metric.getMetricName())
                        );
                    }
                }
                else {
                    log.trace("{}No metrics available for {}", logPrefix, plugName);
                }
            }
        }
        );
        log.debug("{}Discovered components: {}", logPrefix, discoveryResponse);
        return discoveryResponse;
    }

    public MonitoringReportResponse getHealthReport() {
        final String logPrefix = "getHealthReport() - ";
        log.trace("{}Entering method", logPrefix);
        Map<String, RegisteredPlugin> allPlugins = pluginManagement.getAllPlugins();

        MonitoringReportResponse response = new MonitoringReportResponse();
        Map<String, String> loadedPlugins = new HashMap<>();
        Map<String, PluginHealthResponse> pluginHealth = new HashMap<>();

        final OverallStatus overall = new OverallStatus();

        allPlugins.forEach((plugName, plugin) -> {
            log.trace("{}Scanning plugin {} for health", logPrefix, plugName);
            loadedPlugins.put(plugName, plugin.getState());
            PluginHealthResponse phr = new PluginHealthResponse();
            if (plugin.getState().equals("STARTED")) {
                log.debug("{}Plugin started, adding health", logPrefix);
                overall.setAnyPluginStarted(true);
                HealthResult health = plugin.getPlugin().getPluginHealth();
                if (health.getOverallStatus().getHealthState() != HealthState.HEALTHY) {
                    overall.setAllPluginsHealthy(false);
                }
                phr.setOverallHeatlh(health.getOverallStatus());
                List<HealthMetric> hm = new ArrayList<>();
                if (health.getMetrics() != null) {
                    for (HealthMetric m : health.getMetrics()) {
                        hm.add(new HealthMetric().setMetricName(plugName + "::" + m.getMetricName()).setMetricValue(m.getMetricValue()));
                    }
                }
                log.debug("{}Added metrics: {}", logPrefix, hm);
                phr.setMetrics(hm);

                log.debug("{}Checking components", logPrefix);
                Map<String, HealthStatus> componentStatus = new HashMap<>();
                if (health.getComponentStatus() != null) {
                    health.getComponentStatus().forEach((compName, compStatus) -> {
                        componentStatus.put(plugName + "::" + compName, compStatus);
                        if (compStatus.getHealthState() != HealthState.HEALTHY) {
                            overall.setAllPluginsHealthy(false);
                        }
                    });
                }
                log.debug("{}Added components: {}", logPrefix, componentStatus);
                phr.setComponents(componentStatus);
            }
            else {
                log.warn("{}Plugin not started", logPrefix);
                overall.setAllPluginsStarted(false);

            }
            pluginHealth.put(plugName, phr);
            log.trace("{}Finished with plugin {}", logPrefix, plugName);

        });
        log.trace("{}Finished with all plugins, forming response", logPrefix);

        String message = "";
        HealthState overallState = HealthState.HEALTHY;
        if (!overall.isAllPluginsHealthy()) {
            message += (message.isEmpty() ? "" : " | ") + "Not all plugins healthy";
            overallState = HealthState.WARNING;
        }
        if (!overall.isAllPluginsStarted()) {
            message += (message.isEmpty() ? "" : " | ") + "Not all plugins started";
            overallState = HealthState.WARNING;
        }
        if (!overall.isAnyPluginStarted()) {
            message += (message.isEmpty() ? "" : " | ") + "No plugins are running";
            overallState = HealthState.FAILED;
        }
        if (overallState == HealthState.HEALTHY) {
            message = "All plugins started and healthy";
        }
        response.setLoadedPlugins(loadedPlugins);
        response.setPluginHealth(pluginHealth);
        response.setApplicationState(new HealthStatus().setHealthComment(message).setHealthState(overallState));
        log.trace("{}Returning health response: {}", logPrefix, response);
        return response;
    }

    @Data
    private class OverallStatus {

        private boolean allPluginsStarted = true;
        private boolean allPluginsHealthy = true;
        private boolean anyPluginStarted = false;
    }

}
