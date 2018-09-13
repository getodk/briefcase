/*
 * Copyright (C) 2011 University of Washington.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.briefcase.util;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.opendatakit.briefcase.model.CryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the initialization of Cipher objects for the decryption of submission
 * and media files.
 *
 * @author mitchellsundt@gmail.com
 */
final class CipherFactory {

  private static final Logger log = LoggerFactory.getLogger(CipherFactory.class);
  private static final String SYMMETRIC_ALGORITHM = "AES/CFB/PKCS5Padding";
  private static final int IV_BYTE_LENGTH = 16;

  private final String instanceId;
  private final SecretKeySpec symmetricKey;
  private final byte[] ivSeedArray;
  private int ivCounter = 0;

  CipherFactory(String instanceId, byte[] symmetricKeyBytes) throws CryptoException {

    this.instanceId = instanceId;
    symmetricKey = new SecretKeySpec(symmetricKeyBytes, SYMMETRIC_ALGORITHM);
    // construct the fixed portion of the iv -- the ivSeedArray
    // this is the md5 hash of the instanceID and the symmetric key
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(instanceId.getBytes(UTF_8));
      md.update(symmetricKeyBytes);
      byte[] messageDigest = md.digest();
      ivSeedArray = new byte[IV_BYTE_LENGTH];
      for (int i = 0; i < IV_BYTE_LENGTH; ++i) {
        ivSeedArray[i] = messageDigest[(i % messageDigest.length)];
      }
    } catch (NoSuchAlgorithmException e) {
      String msg = "Error constructing ivSeedArray";
      log.error(msg, e);
      throw new CryptoException(msg + " Cause: " + e);
    }
  }

  Cipher getCipher() throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
    ++ivSeedArray[ivCounter % ivSeedArray.length];
    ++ivCounter;
    IvParameterSpec baseIv = new IvParameterSpec(ivSeedArray);
    Cipher c = Cipher.getInstance(SYMMETRIC_ALGORITHM);

    c.init(Cipher.DECRYPT_MODE, symmetricKey, baseIv);
    return c;
  }

  Cipher getCipher(String fieldName) throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
    MessageDigest md = MessageDigest.getInstance("MD5");
    md.update(instanceId.getBytes(UTF_8));
    md.update(symmetricKey.getEncoded());
    md.update(fieldName.getBytes(UTF_8));
    byte[] messageDigest = md.digest();
    for (int i = 0; i < IV_BYTE_LENGTH; ++i) {
      ivSeedArray[i] = messageDigest[(i % messageDigest.length)];
    }
    IvParameterSpec baseIv = new IvParameterSpec(ivSeedArray);
    Cipher c = Cipher.getInstance(SYMMETRIC_ALGORITHM);

    c.init(Cipher.DECRYPT_MODE, symmetricKey, baseIv);
    return c;
  }
}
