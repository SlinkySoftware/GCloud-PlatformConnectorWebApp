/*
 *   platformconnector - HttpErrorController.java
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

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Controller
@Slf4j
public class HttpErrorController implements ErrorController {

    @Autowired
    private ErrorAttributes errorAttributes;

    @RequestMapping(value = "/error")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> error(WebRequest request, HttpServletRequest httpReq) {
        final String logPrefix = "error() - ";
        log.trace("{}Entering method", logPrefix);
        Map<String, Object> body = getErrorAttributes(request);
        HttpStatus status = getStatus(httpReq);
        log.info("{}Rendering error page", logPrefix);
        return new ResponseEntity<>(body, status);
    }

    private Map<String, Object> getErrorAttributes(WebRequest request) {
        ErrorAttributeOptions eao = ErrorAttributeOptions.defaults().including(
                ErrorAttributeOptions.Include.MESSAGE,
                ErrorAttributeOptions.Include.STACK_TRACE,
                ErrorAttributeOptions.Include.EXCEPTION,
                ErrorAttributeOptions.Include.BINDING_ERRORS
        );
        return this.errorAttributes.getErrorAttributes(request, eao);
    }

    private HttpStatus getStatus(HttpServletRequest request) {
        final String logPrefix = "getStatus() - ";
        log.trace("{}Entering method", logPrefix);
        Integer statusCode = (Integer) request
                .getAttribute("javax.servlet.error.status_code");
        if (statusCode != null) {
            log.trace("{}Status Code: {}", logPrefix, statusCode);
            return HttpStatus.valueOf(statusCode);
        }
        log.trace("{}Status Code defaulted to 500 Internal Server Error", logPrefix);
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

}

