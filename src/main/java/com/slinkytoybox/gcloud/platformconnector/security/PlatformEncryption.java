/*
 *   platformconnector - PlatformEncryption.java
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
package com.slinkytoybox.gcloud.platformconnector.security;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.iv.RandomIvGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 *
 * @author Michael Junek (michael@juneks.com.au)
 */
@Service("PlatformEncryption")
@Slf4j
public class PlatformEncryption {

    private PooledPBEStringEncryptor encryptor;
    private final static String ENC_ALGORITHM = "PBEWithHMACSHA512AndAES_256";

    @Autowired
    private Environment env;

    @PostConstruct
    private void initialiseEncryptor() {
        final String logPrefix = "initialiseEncryptor() - ";
        log.trace("{}Entering Method", logPrefix);
        log.info("{}Setting up encryption/decryption routines", logPrefix);
        String encryptionKey = env.getProperty("secure.key", "NOT_SET");
        if (encryptionKey.equalsIgnoreCase("NOT_SET")) {
            log.error("{}Encryption key is not set!", logPrefix);
            throw new IllegalArgumentException("Encryption key is not set");
        }
        encryptor = new PooledPBEStringEncryptor();
        encryptor.setAlgorithm(ENC_ALGORITHM);
        encryptor.setIvGenerator(new RandomIvGenerator());
        encryptor.setPassword(encryptionKey);
        encryptor.setPoolSize(4);
        encryptor.initialize();
        log.trace("{}Initialised encryptor service: {}", logPrefix, encryptor);
        log.trace("{}Leaving Method", logPrefix);

    }

    public String encrypt(String plainTextString) {
        final String logPrefix = "encrypt() - ";
        log.trace("{}Entering Method", logPrefix);
        String encryptedText = encryptor.encrypt(plainTextString);
        log.debug("{}Text encrypted to: {}", logPrefix, encryptedText);
        encryptedText = "ENC:" + encryptedText;
        return encryptedText;
    }

    public String decrypt(String encryptedString) {
        final String logPrefix = "decrypt() - ";
        log.trace("{}Entering Method", logPrefix);
        String plainText;
        if (encryptedString.startsWith("ENC:")) {
            try {
                plainText = encryptor.decrypt(encryptedString.substring(4));
            }
            catch (EncryptionOperationNotPossibleException ex) {
                log.error("{}Could not decrypt text. Returning null", logPrefix);
                return null;
            }
            log.debug("{}Text decrypted from: {}", logPrefix, encryptedString);
        }
        else {
            log.warn("{}Text was not encrypted", logPrefix);
            plainText = encryptedString;
        }
        return plainText;
    }

}
