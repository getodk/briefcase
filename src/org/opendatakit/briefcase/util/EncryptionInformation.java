/*
 * Copyright (C) 2012 University of Washington.
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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.apache.commons.codec.binary.Base64;
import org.opendatakit.briefcase.model.CryptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EncryptionInformation {

  private static final Logger log = LoggerFactory.getLogger(EncryptionInformation.class);

  private CipherFactory cipherFactory;

  EncryptionInformation(String base64EncryptedSymmetricKey, String instanceId, PrivateKey rsaPrivateKey) throws CryptoException {

    try {
      // construct the base64-encoded RSA-encrypted symmetric key
      Cipher pkCipher;
      pkCipher = Cipher.getInstance(FileSystemUtils.ASYMMETRIC_ALGORITHM);
      // write AES key
      pkCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
      byte[] encryptedSymmetricKey = Base64.decodeBase64(base64EncryptedSymmetricKey);
      byte[] decryptedKey = pkCipher.doFinal(encryptedSymmetricKey);
      cipherFactory = new CipherFactory(instanceId, decryptedKey);
    } catch (NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException | NoSuchPaddingException e) {
      String msg = "Error decrypting base64EncryptedKey";
      log.error(msg, e);
      throw new CryptoException(msg + " Cause: " + e.toString());
    }
  }

  Cipher getCipher() throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
    return cipherFactory.getCipher();
  }

}
