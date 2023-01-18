/*
 *   platformconnector - PluginHealthResponse.java
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
package com.slinkytoybox.gcloud.platformconnector.dto.response.monitoring;

import com.slinkytoybox.gcloud.platformconnectorplugin.health.HealthMetric;
import com.slinkytoybox.gcloud.platformconnectorplugin.health.HealthStatus;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Data
@Accessors(chain = true)
public class PluginHealthResponse implements Serializable {

    private HealthStatus overallHeatlh;
    private Map<String, HealthStatus> components;
    private List<HealthMetric> metrics;

}
