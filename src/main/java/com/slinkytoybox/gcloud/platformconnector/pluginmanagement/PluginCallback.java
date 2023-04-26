/*
 *   platformconnector - PluginCallback.java
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

import com.slinkytoybox.gcloud.platformconnector.security.PlatformEncryption;
import com.slinkytoybox.gcloud.platformconnectorplugin.ContainerInterface;
import com.slinkytoybox.gcloud.platformconnectorplugin.health.HealthResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Component
@Slf4j
public class PluginCallback implements ContainerInterface {

    
    private PluginManagement pluginManagement;
    
    @Autowired
            private PlatformEncryption encryptor;
    
    void setPluginManagement (PluginManagement pluginManagement) {
        this.pluginManagement = pluginManagement;
    }

    @Override
    public void setPluginHealth(String pluginId, HealthResult healthResult) {
        final String logPrefix = "setPluginHealth() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Updating plugin health for {} to {}", logPrefix, pluginId, healthResult);
        RegisteredPlugin plugin = pluginManagement.getPluginByName(pluginId);

        if (plugin == null) {
            log.error("{}Plugin {} not in registration list. How!", logPrefix);
            throw new IllegalArgumentException("Plugin " + pluginId + " is not registered");
        }

        plugin.setHealth(healthResult);
        log.trace("{}Leaving Method", logPrefix);
    }

    @Override
    public String encrypt(String plainText) {
        final String logPrefix = "encrypt() - ";
        log.trace("{}Entering Method", logPrefix);
        return encryptor.encrypt(plainText);
    }

    @Override
    public String decrypt(String encryptedText) {
        final String logPrefix = "decrypt() - ";
        log.trace("{}Entering Method", logPrefix);
        return encryptor.decrypt(encryptedText);
    }

}
