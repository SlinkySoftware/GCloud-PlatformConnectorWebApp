/*
 *   platformconnector - HealthController.java
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

import com.slinkytoybox.gcloud.platformconnector.businesslogic.MonitoringLogic;
import com.slinkytoybox.gcloud.platformconnector.dto.response.monitoring.*;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Slf4j
@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private MonitoringLogic monitoringLogic;

    @GetMapping(path = "/discovery", produces = "application/json")
    public ResponseEntity<List<DiscoveryResponse>> getHealthDiscovery() {
        final String logPrefix = "getHealthDiscovery() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Getting Zabbix discovery records", logPrefix);
        return ResponseEntity.ok(monitoringLogic.getDiscovery());
    }

    @GetMapping(path = "/discovery/plugins", produces = "application/json")
    public ResponseEntity<List<PluginDiscoveryResponse>> getHealthPluginDiscovery() {
        final String logPrefix = "getHealthPluginDiscovery() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Getting Zabbix discovery records", logPrefix);
        return ResponseEntity.ok(monitoringLogic.getPluginDiscovery());
    }

    @GetMapping(path = "/discovery/metrics", produces = "application/json")
    public ResponseEntity<List<MetricDiscoveryResponse>> getHealthMetricDiscovery() {
        final String logPrefix = "getHealthMetricDiscovery() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Getting Zabbix discovery records", logPrefix);
        return ResponseEntity.ok(monitoringLogic.getMetricDiscovery());
    }

    @GetMapping(path = "/discovery/components", produces = "application/json")
    public ResponseEntity<List<ComponentDiscoveryResponse>> getHealthComponentDiscovery() {
        final String logPrefix = "getHealthComponentDiscovery() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Getting Zabbix discovery records", logPrefix);
        return ResponseEntity.ok(monitoringLogic.getComponentDiscovery());
    }

    @GetMapping(path = "/report", produces = "application/json")
    public ResponseEntity<MonitoringReportResponse> getHealthReport() {
        final String logPrefix = "getHealthReport() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Getting health report", logPrefix);
        return ResponseEntity.ok(monitoringLogic.getHealthReport());
    }

}
