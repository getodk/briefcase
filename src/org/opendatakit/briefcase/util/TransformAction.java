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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bushe.swing.event.EventBus;
import org.opendatakit.briefcase.model.LocalFormDefinition;
import org.opendatakit.briefcase.model.OutputType;
import org.opendatakit.briefcase.model.TerminationFuture;
import org.opendatakit.briefcase.model.TransformFailedEvent;
import org.opendatakit.briefcase.model.TransformSucceededEvent;
import org.opendatakit.briefcase.ui.PrivateKeyPromptDialog;
import org.opendatakit.briefcase.ui.TransformInProgressDialog;

public class TransformAction {

  static final String SCRATCH_DIR = "scratch";

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
        if ( allSuccessful ) {
          EventBus.publish(new TransformSucceededEvent(action.getFormDefinition()));
        } else {
          EventBus.publish(new TransformFailedEvent(action.getFormDefinition()));
        }
      } catch (Exception e) {
        e.printStackTrace();
        EventBus.publish(new TransformFailedEvent(action.getFormDefinition()));
      }
    }

  }

  private static void showDialogAndRun(ITransformFormAction action, OutputType outputType, TerminationFuture terminationFuture) {
    // create the dialog first so that the background task will always have a
    // listener for its completion events...
    final TransformInProgressDialog dlg = new TransformInProgressDialog("Transforming form into " + outputType.toString(), terminationFuture);

    backgroundExecutorService.execute(new TransformFormRunnable(action));

    dlg.setVisible(true);
  }

  public static void transform(File outputDir, OutputType outputType, 
		  LocalFormDefinition lfd, TerminationFuture terminationFuture ) 
				  throws IOException {

	if ( lfd.isEncryptedForm() ) {
		PrivateKeyPromptDialog dlg = new PrivateKeyPromptDialog("Enter private key for " + lfd.getFormName());
		dlg.pack();
		dlg.setVisible(true);
		lfd.setPrivateKey( dlg.getPrivateKey() );
	}
	
	ITransformFormAction action;
	if ( outputType == OutputType.CSV ) {
		action = new TransformToCsv(outputDir, lfd, terminationFuture);
	} else {
		throw new IllegalStateException("outputType not recognized");
	}
	
    showDialogAndRun(action, outputType, terminationFuture);
  }
}
