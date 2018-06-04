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

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.opendatakit.briefcase.pull.PullEvent;
import org.opendatakit.briefcase.push.PushEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminationFuture {

  private static final Logger log = LoggerFactory.getLogger(TerminationFuture.class);

  private boolean cancelled = false;

  public TerminationFuture() {
    AnnotationProcessor.process(this);
  }

  @EventSubscriber(eventClass = PushEvent.Abort.class)
  public void markAsCancelled(PushEvent.Abort event) {
    cancelled = true;
    log.info("cancel requested: " + event.cause);
  }

  @EventSubscriber(eventClass = PullEvent.Abort.class)
  public void markAsCancelled(PullEvent.Abort event) {
    cancelled = true;
    log.info("cancel requested: " + event.cause);
  }

  public void reset() {
    cancelled = false;
  }

  public boolean isCancelled() {
    return cancelled;
  }
}
