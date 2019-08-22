package org.opendatakit.briefcase.pull.aggregate;

import org.opendatakit.briefcase.reused.http.response.Response;

public class InstanceIdBatchGetterException extends RuntimeException {
  public final Response<?> aggregateResponse;

  public InstanceIdBatchGetterException(Response<?> aggregateResponse) {
    super("Error getting instance ID batch");
    this.aggregateResponse = aggregateResponse;
  }
}
