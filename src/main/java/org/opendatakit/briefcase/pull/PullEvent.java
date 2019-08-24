/*
 * Copyright (C) 2018 Nafundi
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

package org.opendatakit.briefcase.pull;

import java.util.Optional;
import java.util.function.BiConsumer;
import org.opendatakit.briefcase.model.form.FormKey;
import org.opendatakit.briefcase.pull.aggregate.Cursor;
import org.opendatakit.briefcase.reused.transfer.RemoteServer;

public class PullEvent {

  public static class Success extends PullEvent {
    public final FormKey formKey;
    public final Optional<RemoteServer> remoteServer;
    public final Optional<Cursor> lastCursor;

    private Success(FormKey formKey, Optional<RemoteServer> remoteServer, Optional<Cursor> lastCursor) {
      this.formKey = formKey;
      this.remoteServer = remoteServer;
      this.lastCursor = lastCursor;
    }

    public static Success of(FormKey formKey) {
      return new Success(formKey, Optional.empty(), Optional.empty());
    }

    public static Success of(FormKey formKey, RemoteServer remoteServer) {
      return new Success(formKey, Optional.of(remoteServer), Optional.empty());
    }

    public static Success of(FormKey formKey, RemoteServer remoteServer, Cursor lastCursor) {
      return new Success(formKey, Optional.of(remoteServer), Optional.of(lastCursor));
    }

    public void ifRemoteServer(BiConsumer<FormKey, RemoteServer> consumer) {
      remoteServer.ifPresent(server -> consumer.accept(formKey, server));
    }
  }

  public static class Cancel extends PullEvent {
    public final String cause;

    public Cancel(String cause) {
      this.cause = cause;
    }
  }

  public static class PullComplete extends PullEvent {

  }
}
