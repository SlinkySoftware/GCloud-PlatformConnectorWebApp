/*
 *   platformconnector - CloudPlatform.java
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
package com.slinkytoybox.gcloud.platformconnector.connection;

import com.mypurecloud.sdk.v2.ApiClient;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Data
@ToString(exclude = {"apiClientId", "apiClientSecret", "apiClient", "apiRegion", "azureAdAccessGroup"})
@Accessors (chain  = true)
public class CloudPlatform {
    private Long id;
    private String name;
    private String organisationName;
    private String organisationId;
    private String apiRegion;
    private String apiClientId;
    private String apiClientSecret;
    private String azureAdAccessGroup;
    private ApiClient apiClient;
}
