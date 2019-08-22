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

package org.opendatakit.briefcase.transfer;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.opendatakit.briefcase.model.FormStatusBuilder;

public class TransferFormsTest {
  @Test
  public void empty_forms_can_merge_forms() {
    // What we are really testing is that no UnsupportedOperationException
    // is thrown when merging forms into an empty TransferForms object
    TransferForms forms = TransferForms.empty();
    forms.merge(singletonList(FormStatusBuilder.buildFormStatus(1)));
    assertThat(forms.isEmpty(), is(false));
  }

  @Test
  public void forms_from_varargs_factory_can_be_cleared() {
    // What we are really testing is that no UnsupportedOperationException
    // is thrown when clearing an TransferForms object obtained using
    // the varargs factory method
    TransferForms forms = TransferForms.of(FormStatusBuilder.buildFormStatus(1));
    forms.clear();
    assertThat(forms.isEmpty(), is(true));
  }
}
