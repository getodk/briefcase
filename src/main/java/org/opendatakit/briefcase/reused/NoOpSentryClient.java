package org.opendatakit.briefcase.reused;

import io.sentry.SentryClient;
import io.sentry.event.helper.ShouldSendEventCallback;

public class NoOpSentryClient extends SentryClient {
  public NoOpSentryClient() {
    super(null, null);
  }

  @Override
  public void sendException(Throwable throwable) {
    // Do nothing
  }

  @Override
  public void addShouldSendEventCallback(ShouldSendEventCallback shouldSendEventCallback) {
    // Do nothing
  }
}
