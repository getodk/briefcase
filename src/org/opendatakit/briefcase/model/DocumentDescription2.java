package org.opendatakit.briefcase.model;

import org.bushe.swing.event.annotation.AnnotationProcessor;

public class DocumentDescription2 {

  final String fetchDocFailed;
  final String fetchDocFailedNoDetail;
  final String documentDescriptionType;
  volatile boolean cancelled = false;

  public DocumentDescription2(String fetchDocFailed,
                             String fetchDocFailedNoDetail,
                             String documentDescriptionType) {
    AnnotationProcessor.process(this);// if not using AOP

    this.fetchDocFailed = fetchDocFailed;
    this.fetchDocFailedNoDetail = fetchDocFailedNoDetail;
    this.documentDescriptionType = documentDescriptionType;
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
