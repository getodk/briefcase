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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bouncycastle.openssl.PEMReader;
import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.ExportType;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.ui.ODKOptionPane;

public class ExportAction {

  static final String SCRATCH_DIR = "scratch";
  static final String UTF_8 = "UTF-8";

  private static ExecutorService backgroundExecutorService = Executors.newCachedThreadPool();

  private static class TransformFormRunnable implements Runnable {
    ITransformFormAction action;

    TransformFormRunnable(ITransformFormAction action) {
      this.action = action;
    }

    @Override
    public void run() {
      try {
        boolean allSuccessful = true;
        allSuccessful = action.doAction();
        if (allSuccessful) {
          EventBus.publish(new ExportSucceededEvent(action.getFormDefinition()));
        } else {
          EventBus.publish(new ExportFailedEvent(action.getFormDefinition()));
        }
      } catch (Exception e) {
        e.printStackTrace();
        EventBus.publish(new ExportFailedEvent(action.getFormDefinition()));
      }
    }

  }

  private static void backgroundRun(ITransformFormAction action) {
    backgroundExecutorService.execute(new TransformFormRunnable(action));
  }

  public static void export(
      File outputDir, ExportType outputType, BriefcaseFormDefinition lfd, File pemFile,
      TerminationFuture terminationFuture) throws IOException {

    if (lfd.isFileEncryptedForm() || lfd.isFieldEncryptedForm()) {

      String errorMsg = null;
      boolean success = false;
      for (;;) /* this only executes once... */ {
        try {
          BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(pemFile), "UTF-8"));
          Object o = new PEMReader(br).readObject();
          if ( o == null ) {
            ODKOptionPane.showErrorDialog(null,
                errorMsg = "The supplied file is not in PEM format.",
                "Invalid RSA Private Key");
            break;
          }
          PrivateKey privKey;
          if ( o instanceof KeyPair ) {
            KeyPair kp = (KeyPair) o;
            privKey = kp.getPrivate();
          } else if ( o instanceof PrivateKey ) {
            privKey = (PrivateKey) o;
          } else {
            privKey = null;
          }
          if ( privKey == null ) {
            ODKOptionPane.showErrorDialog(null,
                errorMsg = "The supplied file does not contain a private key.",
                "Invalid RSA Private Key");
            break;
          }
          lfd.setPrivateKey(privKey);
          success = true;
          break;
        } catch (IOException e) {
          e.printStackTrace();
          ODKOptionPane.showErrorDialog(null,
              errorMsg = "The supplied PEM file could not be parsed.",
              "Invalid RSA Private Key");
          break;
        }
      }
      if ( !success ) {
        EventBus.publish(new ExportProgressEvent(errorMsg));
        EventBus.publish(new ExportFailedEvent(lfd));
        return;
      }
    }

    ITransformFormAction action;
    if (outputType == ExportType.CSV) {
      action = new ExportToCsv(outputDir, lfd, terminationFuture);
    } else {
      throw new IllegalStateException("outputType not recognized");
    }

    backgroundRun(action);
  }
}
