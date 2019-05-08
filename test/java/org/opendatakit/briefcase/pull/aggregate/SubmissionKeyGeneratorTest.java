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

package org.opendatakit.briefcase.pull.aggregate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class SubmissionKeyGeneratorTest {
  @Test
  public void parses_a_blank_form_and_produces_valid_keys() {
    SubmissionKeyGenerator subKeyGen = SubmissionKeyGenerator.from(
        PullTestHelpers.buildBlankFormXml("some-form", "2010010101", "instance-name")
    );

    assertThat(
        subKeyGen.buildKey("uuid:515a13cf-d7a5-4606-a18f-84940b0944b2"),
        is("some-form[@version=Optional[2010010101] and @uiVersion=null]/instance-name[@key=uuid:515a13cf-d7a5-4606-a18f-84940b0944b2]")
    );
  }

  @Test
  public void parses_a_blank_form_and_produces_valid_keys_on_encrypted_forms() {
    SubmissionKeyGenerator subKeyGen = SubmissionKeyGenerator.from(
        PullTestHelpers.buildEncryptedBlankFormXml("some-form", "2010010101", "instance-name")
    );

    // Encrypted forms generate keys with a fixed "data" instance name
    assertThat(
        subKeyGen.buildKey("uuid:515a13cf-d7a5-4606-a18f-84940b0944b2"),
        is("some-form[@version=Optional[2010010101] and @uiVersion=null]/data[@key=uuid:515a13cf-d7a5-4606-a18f-84940b0944b2]")
    );
  }

}
