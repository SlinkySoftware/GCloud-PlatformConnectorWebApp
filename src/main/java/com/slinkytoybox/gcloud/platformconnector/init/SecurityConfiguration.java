/*
 *   platformconnector - SecurityConfiguration.java
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
package com.slinkytoybox.gcloud.platformconnector.init;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Component
@Slf4j
public class SecurityConfiguration {

    @Value("${security.file}")
    private String securityFile;

    private String securePassword;

    @PostConstruct
    public void checkAndLoadPassword() {
        final String logPrefix = "checkAndLoadPassword() - ";
        log.trace("{}Entering Method", logPrefix);
        log.debug("{}Reading properties file {}", logPrefix, securityFile);
        String tempSecurePassword = "";
        try {
            File f = new File(securityFile);
            if (!f.exists()) {
                f.createNewFile();
            }
            try (InputStream in = new FileInputStream(f)) {
                Properties config = new Properties();
                config.load(in);
                in.close();
                tempSecurePassword = config.getProperty("secure.password");
            }
            catch (IOException ex) {
                log.error("{}Exception encountered reading file", logPrefix, ex);
            }
        }
        catch (IOException ex) {
            log.error("{}Exception encountered opening file", logPrefix, ex);
        }
        if (tempSecurePassword == null || tempSecurePassword.isEmpty()) {
            log.info("{}Password not defined, generating new", logPrefix);
            securePassword = java.util.UUID.randomUUID().toString();
            savePassword(tempSecurePassword);
        }
        if (updateCloud(tempSecurePassword)) {
            securePassword = tempSecurePassword;
        }
        else {
            log.warn("{}Password not updated!", logPrefix);
        }
        log.debug("{}Using password: {}", logPrefix, securePassword);
        log.trace("{}Leaving method", logPrefix);
    }

     @Scheduled(fixedDelayString = "120000", initialDelayString = "10000")
    public void rotatePassword() {
        final String logPrefix = "rotatePassword() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Rotating cloud password", logPrefix);
        String tempSecurePassword = java.util.UUID.randomUUID().toString();
        if (savePassword(tempSecurePassword) && updateCloud(tempSecurePassword)) {
            securePassword = tempSecurePassword;
        }
        else {
            log.warn("{}Password not updated!", logPrefix);
        }
        log.debug("{}Using password: {}", logPrefix, securePassword);
        log.trace("{}Leaving method", logPrefix);
    }

    private boolean updateCloud(String password) {
        final String logPrefix = "updateCloud() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Updating password in Genesys Cloud", logPrefix);
        
        
        return true;
    }

    private boolean savePassword(String password) {
        final String logPrefix = "savePassword() - ";
        log.trace("{}Entering Method", logPrefix);
        try {
            File f = new File(securityFile);
            if (!f.exists()) {
                f.createNewFile();
            }
            try (OutputStream out = new FileOutputStream(f)) {
                Properties config = new Properties();
                config.put("secure.password", password);
                config.store(out, "Password regenerated successfully");
                out.close();
            }
            catch (IOException ex) {
                log.error("{}Exception encountered saving file", logPrefix, ex);
                return false;
            }
        }
        catch (IOException ex) {
            log.error("{}Exception encountered opening file", logPrefix, ex);
            return false;
        }
        return true;
    }
}
