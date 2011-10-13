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

public class DocumentDescription {
  
  final String fetchDocFailed;
  final String fetchDocFailedNoDetail;
  final String documentDescriptionType;
  final TerminationFuture terminationFuture;
  volatile boolean cancelled = false;
  
  public DocumentDescription(String fetchDocFailed, 
                      String fetchDocFailedNoDetail,
                      String documentDescriptionType,
                      TerminationFuture terminationFuture) {
    AnnotationProcessor.process(this);// if not using AOP

    this.fetchDocFailed = fetchDocFailed;
    this.fetchDocFailedNoDetail = fetchDocFailedNoDetail;
    this.documentDescriptionType = documentDescriptionType;
    this.terminationFuture = terminationFuture;
  }
  
  public boolean isCancelled() {
    return terminationFuture.isCancelled();
  }

  public String getFetchDocFailed() {
    return fetchDocFailed;
  }

  public String getFetchDocFailedNoDetail() {
    return fetchDocFailedNoDetail;
  }

  public String getDocumentDescriptionType() {
    return documentDescriptionType;
  }
}