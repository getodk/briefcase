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
package org.opendatakit.briefcase.reused.api;

/**
 * This is the version of {@link java.util.function.Function} for an arity of 4
 *
 * @param <I1> first arg's type
 * @param <I2> second arg's type
 * @param <I3> third arg's type
 * @param <I4> fourth arg's type
 * @param <O>  output type
 */
@FunctionalInterface
public interface TetraFunction<I1, I2, I3, I4, O> {
  O apply(I1 i1, I2 i2, I3 i3, I4 i4);
}
