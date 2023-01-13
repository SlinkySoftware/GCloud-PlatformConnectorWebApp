/*
 *   platformconnector - PluginConfig.java
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

import lombok.extern.slf4j.Slf4j;
import org.pf4j.spring.SpringPluginManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Configuration
@Slf4j
public class PluginConfig {

    @Bean
    public SpringPluginManager pluginManager() {
        final String logPrefix = "pluginManager() - ";
        log.trace("{}Entering method", logPrefix);
        return new SpringPluginManager();
    }
    
    

    @Bean
    @DependsOn("pluginManager") 
    public PluginManagement pluginManagement() {
        final String logPrefix = "pluginManagement() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Setting up Plugins", logPrefix);
        return new PluginManagement();
        
    }

}
