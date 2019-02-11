/*
 * Copy_2 (C) 2019 Nafundi
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

package org.opendatakit.briefcase.reused;

import java.util.Objects;

@SuppressWarnings("checkstyle:MethodTypeParameterName")
public class Triple<T, U, V> {
  private final T _1;
  private final U _2;
  private final V _3;

  public Triple(T _1, U _2, V _3) {
    this._1 = _1;
    this._2 = _2;
    this._3 = _3;
  }

  public static <TT, UU, VV> Triple<TT, UU, VV> of(TT _1, UU _2, VV _3) {
    return new Triple<>(_1, _2, _3);
  }

  public T get1() {
    return _1;
  }

  public U get2() {
    return _2;
  }

  public V get3() {
    return _3;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
    return Objects.equals(_1, triple._1) &&
        Objects.equals(_2, triple._2) &&
        Objects.equals(_3, triple._3);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_1, _2, _3);
  }
}
