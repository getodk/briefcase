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

import java.io.UnsupportedEncodingException;
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

public class EncryptionInformation {
  
  private CipherFactory cipherFactory;
  
  public EncryptionInformation(String base64EncryptedSymmetricKey, String instanceId, PrivateKey rsaPrivateKey) throws CryptoException {

    try {
      // construct the base64-encoded RSA-encrypted symmetric key
      Cipher pkCipher;
      pkCipher = Cipher.getInstance(FileSystemUtils.ASYMMETRIC_ALGORITHM);
      // write AES key
      pkCipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
      byte[] encryptedSymmetricKey = Base64.decodeBase64(base64EncryptedSymmetricKey);
      byte[] decryptedKey = pkCipher.doFinal(encryptedSymmetricKey);
      cipherFactory = new CipherFactory(instanceId, decryptedKey);
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedKey Cause: " + e.toString());
    } catch (NoSuchPaddingException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedKey Cause: " + e.toString());
    } catch (InvalidKeyException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedKey Cause: " + e.toString());
    } catch (IllegalBlockSizeException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedKey Cause: " + e.toString());
    } catch (BadPaddingException e) {
      e.printStackTrace();
      throw new CryptoException("Error decrypting base64EncryptedKey Cause: " + e.toString());
    }
  }
  
  Cipher getCipher(String context) throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException {
    return cipherFactory.getCipher(context);
  }
  
  Cipher getCipher(String context, String fieldName) throws InvalidKeyException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException {
    return cipherFactory.getCipher(context, fieldName);
  }
}