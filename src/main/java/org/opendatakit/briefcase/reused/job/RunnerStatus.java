/*
 * Copyright (C) 2019 Nafundi
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

package org.opendatakit.briefcase.reused.job;

import java.util.function.Supplier;

/**
 * This class serves as a probe that can be checked to
 * know if a runner has been stopped.
 */
public class RunnerStatus {
  private final Supplier<Boolean> stoppedProbe;

  RunnerStatus(Supplier<Boolean> stoppedProbe) {
    this.stoppedProbe = stoppedProbe;
  }

  public boolean isStillRunning() {
    return !stoppedProbe.get();
  }

  public boolean isCancelled() {
    return !isStillRunning();
  }
}
