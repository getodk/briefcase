package org.opendatakit.briefcase.pull.aggregate;

import org.opendatakit.briefcase.reused.http.response.Response;

class InstanceIdBatchGetterException extends RuntimeException {
  final Response<?> aggregateResponse;

  InstanceIdBatchGetterException(Response<?> aggregateResponse) {
    super("Error getting instance ID batch");
    this.aggregateResponse = aggregateResponse;
  }
}
