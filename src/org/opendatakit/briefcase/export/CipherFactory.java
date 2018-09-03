/*
 * Copyright (C) 2018 Nafundi
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

package org.opendatakit.briefcase.export;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.opendatakit.briefcase.model.CryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the initialization of {@link Cipher} objects for the decryption of submission
 * and media files.
 */
final class CipherFactory {
  private static final Logger log = LoggerFactory.getLogger(CipherFactory.class);
  private static final int IV_BYTE_LENGTH = 16;
  private final SecretKeySpec symmetricKey;
  private final byte[] ivSeedArray;
  private int ivCounter = 0;

  private CipherFactory(String instanceId, byte[] symmetricKeyBytes) {
    symmetricKey = new SecretKeySpec(symmetricKeyBytes, "AES/CFB/PKCS5Padding");
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

  /**
   * Factory that initializes a new {@link CipherFactory} for the given instance ID,
   * encryption key and private key values.
   *
   * @throws CryptoException if the key can't be decrypted
   */
  static CipherFactory from(String instanceId, String base64EncryptedKey, PrivateKey privateKey) {
    try {
      Cipher pkCipher;
      pkCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
      pkCipher.init(Cipher.DECRYPT_MODE, privateKey);
      byte[] encryptedSymmetricKey = Base64.decodeBase64(base64EncryptedKey);
      byte[] decryptedKey = pkCipher.doFinal(encryptedSymmetricKey);
      return new CipherFactory(instanceId, decryptedKey);
    } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
      throw new CryptoException(e);
    }
  }

  /**
   * Factory that returns a {@link Cipher} suitable to decrypt cryptographic
   * signatures.
   *
   * @throws CryptoException
   */
  static Cipher signatureDecrypter(PrivateKey privateKey) {
    try {
      Cipher pkCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
      pkCipher.init(Cipher.DECRYPT_MODE, privateKey);
      return pkCipher;
    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
      throw new CryptoException(e);
    }
  }

  /**
   * Return the next {@link Cipher} instance. This method has side-effects and will
   * change the initialization vector, which will affect the next call to this method.
   *
   * @throws CryptoException
   */
  Cipher next() {
    try {
      ++ivSeedArray[ivCounter % ivSeedArray.length];
      ++ivCounter;
      IvParameterSpec baseIv = new IvParameterSpec(ivSeedArray);
      Cipher c = Cipher.getInstance("AES/CFB/PKCS5Padding");

      c.init(Cipher.DECRYPT_MODE, symmetricKey, baseIv);
      return c;
    } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException e) {
      throw new CryptoException(e);
    }
  }
}
