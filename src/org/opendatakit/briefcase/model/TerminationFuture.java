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

package org.opendatakit.briefcase.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;

public class TerminationFuture {

  private static final Log logger = LogFactory.getLog(TerminationFuture.class);

  private boolean cancelled = false;
  
  public TerminationFuture() {
    AnnotationProcessor.process(this);
  }
  
  @EventSubscriber(eventClass = TransferAbortEvent.class)
  public void markAsCancelled(TransferAbortEvent event) {
    cancelled = true;
    logger.info("cancel requested: " + event.getReason());
  }
  
  @EventSubscriber(eventClass = ExportAbortEvent.class)
  public void markAsCancelled(ExportAbortEvent event) {
    cancelled = true;
    logger.info("cancel requested: " + event.getReason());
  }

  public void reset() {
    cancelled = false;
  }
  
  public boolean isCancelled() {
    return cancelled;
  }
}
