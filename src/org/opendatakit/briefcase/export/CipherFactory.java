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

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Optional;
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

public final class CipherFactory {
  private static final Logger log = LoggerFactory.getLogger(CipherFactory.class);
  private static final int IV_BYTE_LENGTH = 16;
  private final SecretKeySpec symmetricKey;
  private final byte[] ivSeedArray;
  private int ivCounter = 0;

  private CipherFactory(String instanceId, byte[] symmetricKeyBytes) throws CryptoException {
    symmetricKey = new SecretKeySpec(symmetricKeyBytes, "AES/CFB/PKCS5Padding");
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(instanceId.getBytes("UTF-8"));
      md.update(symmetricKeyBytes);
      byte[] messageDigest = md.digest();
      ivSeedArray = new byte[IV_BYTE_LENGTH];
      for (int i = 0; i < IV_BYTE_LENGTH; ++i) {
        ivSeedArray[i] = messageDigest[(i % messageDigest.length)];
      }
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
      String msg = "Error constructing ivSeedArray";
      log.error(msg, e);
      throw new CryptoException(msg + " Cause: " + e);
    }
  }

  public static Optional<CipherFactory> from(String instanceId, String base64EncryptedKey, PrivateKey privateKey) {
    try {
      Cipher pkCipher;
      pkCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
      pkCipher.init(Cipher.DECRYPT_MODE, privateKey);
      byte[] encryptedSymmetricKey = Base64.decodeBase64(base64EncryptedKey);
      byte[] decryptedKey = pkCipher.doFinal(encryptedSymmetricKey);
      return Optional.of(new CipherFactory(instanceId, decryptedKey));
    } catch (NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
      return Optional.empty();
    }
  }

  static Cipher signatureDecrypter(PrivateKey privateKey) {
    try {
      Cipher pkCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
      pkCipher.init(Cipher.DECRYPT_MODE, privateKey);
      return pkCipher;
    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
      throw new CryptoException("Can't build Cipher instance");
    }
  }

  Cipher getSubmissionCipher() {
    try {
      ++ivSeedArray[ivCounter % ivSeedArray.length];
      ++ivCounter;
      IvParameterSpec baseIv = new IvParameterSpec(ivSeedArray);
      Cipher c = Cipher.getInstance("AES/CFB/PKCS5Padding");

      c.init(Cipher.DECRYPT_MODE, symmetricKey, baseIv);
      return c;
    } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException e) {
      throw new CryptoException("Can't get Cipher", e);
    }
  }
}
