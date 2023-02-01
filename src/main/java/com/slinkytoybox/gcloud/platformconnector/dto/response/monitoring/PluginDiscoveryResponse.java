/*
 *   platformconnector - PluginDiscoveryResponse.java
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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Data
@Accessors(chain = true)
public class PluginDiscoveryResponse implements Serializable, DiscoveryResponse {

    @JsonProperty("{#PLUGINID}")
    private String pluginId;

    @JsonProperty("{#DISCOVERYTYPE}")
    private String discoveryType = "plugin";

    // This is here because of the way Zabbix filtering is set up, it complains when the 
    // JsonProperty is missing from Components and plugins, when trying to filter for real metrics
    @JsonProperty("{#METRICTYPE}")
    private String metricType = "NOT_A_METRIC";

}
