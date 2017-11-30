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
import java.util.Date;
import java.util.concurrent.Executor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.openssl.PEMReader;
import org.opendatakit.briefcase.model.BriefcaseFormDefinition;
import org.opendatakit.briefcase.model.ExportFailedEvent;
import org.opendatakit.briefcase.model.ExportProgressEvent;
import org.opendatakit.briefcase.model.ExportSucceededEvent;
import org.opendatakit.briefcase.model.ExportSucceededWithErrorsEvent;
import org.opendatakit.briefcase.model.ExportType;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.ui.ODKOptionPane;
import org.opendatakit.common.pubsub.PubSub;

/**
 * This class has the logic for exporting a form from Briefcase
 * <p>
 * It is a pure-logic class without dependencies to any framework or execution-context.
 * <p>
 * To execute an {@link ExportAction} you need to provide (via constructor):
 * <ul>
 * <li>a {@link PubSub} instance that this class will use to inform about its outcome</li>
 * <li>an {@link Executor} instance that will ultimately run this operation's logic</li>
 * </ul>
 */
public class ExportAction {
  private static final Log log = LogFactory.getLog(ExportAction.class);
  private final PubSub pubSub;
  private final Executor executor;

  /**
   * Main constructor for this class.
   *
   * @param pubSub   a {@link PubSub} instance that the instance will use to inform about its outcome
   * @param executor an {@link Executor} instance that will ultimately run this operation's logic
   */
  public ExportAction(PubSub pubSub, Executor executor) {
    this.pubSub = pubSub;
    this.executor = executor;
  }

  /**
   * Exports a given form to the given outputDir
   *
   * @param outputDir         directory where files will be written with the export results
   * @param outputType        type of output desired. Must be a member of {@link ExportType}
   * @param lfd               an instance of {@link BriefcaseFormDefinition} with the form's definition
   * @param pemFile           a {@link File} with PEM cryptographic keys to decrypt encrypted form submissions
   * @param terminationFuture a {@link TerminationFuture} instance
   * @param start             a {@link Date} instance defining the range of dates you want to export
   * @param end               a {@link Date} instance defining the range of dates you want to export
   */
  public void export(
      File outputDir, ExportType outputType, BriefcaseFormDefinition lfd, File pemFile,
      TerminationFuture terminationFuture, Date start, Date end) {

    if (lfd.isFileEncryptedForm() || lfd.isFieldEncryptedForm()) {

      String errorMsg = null;
      boolean success = false;
      for (; ; ) /* this only executes once... */ {
        try {
          BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(pemFile), "UTF-8"));
          PEMReader rdr = new PEMReader(br);
          Object o = rdr.readObject();
          try {
            rdr.close();
          } catch (IOException e) {
            // ignore.
          }
          if (o == null) {
            ODKOptionPane.showErrorDialog(null,
                errorMsg = "The supplied file is not in PEM format.",
                "Invalid RSA Private Key");
            break;
          }
          PrivateKey privKey;
          if (o instanceof KeyPair) {
            KeyPair kp = (KeyPair) o;
            privKey = kp.getPrivate();
          } else if (o instanceof PrivateKey) {
            privKey = (PrivateKey) o;
          } else {
            privKey = null;
          }
          if (privKey == null) {
            ODKOptionPane.showErrorDialog(null,
                errorMsg = "The supplied file does not contain a private key.",
                "Invalid RSA Private Key");
            break;
          }
          lfd.setPrivateKey(privKey);
          success = true;
          break;
        } catch (IOException e) {
          String msg = "The supplied PEM file could not be parsed.";
          log.error(msg, e);
          ODKOptionPane.showErrorDialog(null, errorMsg = msg, "Invalid RSA Private Key");
          break;
        }
      }
      if (!success) {
        pubSub.publish(new ExportProgressEvent(errorMsg));
        pubSub.publish(new ExportFailedEvent(lfd));
        return;
      }
    }

    ITransformFormAction action;
    if (outputType == ExportType.CSV) {
      action = new ExportToCsv(outputDir, lfd, terminationFuture, start, end);
    } else {
      throw new IllegalStateException("outputType not recognized");
    }

    executor.execute(() -> {
      try {
        boolean allSuccessful = true;
        allSuccessful = action.doAction();
        if (allSuccessful) {
          if (action.totalFilesSkipped() == FilesSkipped.SOME) {
            pubSub.publish(new ExportSucceededWithErrorsEvent(
                action.getFormDefinition()));
          } else if (action.totalFilesSkipped() == FilesSkipped.ALL) {
            // None of the instances were exported
            pubSub.publish(new ExportFailedEvent(action.getFormDefinition()));
          } else {
            pubSub.publish(new ExportSucceededEvent(action.getFormDefinition()));
          }
        } else {
          pubSub.publish(new ExportFailedEvent(action.getFormDefinition()));
        }
      } catch (Exception e) {
        log.error("export action failed", e);
        pubSub.publish(new ExportFailedEvent(action.getFormDefinition()));
      }
    });
  }
}
