/*
 *   platformconnector - SecurityController.java
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

import com.slinkytoybox.gcloud.platformconnector.security.SecurityConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Slf4j
@RestController
@RequestMapping("/security")
public class SecurityController {
    
    @Autowired
    private SecurityConfiguration securityConfiguration;

    @PostMapping("/updateKey")
    public ResponseEntity<String> updateKey() {
        String logPrefix = "updateKey() - ";
        log.trace("{}Entering method", logPrefix);
        log.info("{}Processing POST /security/updateKey", logPrefix);

        return ResponseEntity.ok().body("UPDATED");

    }

}
