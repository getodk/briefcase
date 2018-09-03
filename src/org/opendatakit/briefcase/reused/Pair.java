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

package org.opendatakit.briefcase.reused;

import java.util.Objects;

@SuppressWarnings("checkstyle:MethodTypeParameterName")
public class Pair<T, U> {
  private final T left;
  private final U right;

  public Pair(T left, U right) {
    this.left = left;
    this.right = right;
  }

  public static <TT, UU> Pair<TT, UU> of(TT left, UU right) {
    return new Pair<>(left, right);
  }

  public T getLeft() {
    return left;
  }

  public U getRight() {
    return right;
  }

  @Override
  public String toString() {
    return "Pair{" +
        "left=" + left +
        ", right=" + right +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Pair<?, ?> pair = (Pair<?, ?>) o;
    return Objects.equals(left, pair.left) &&
        Objects.equals(right, pair.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(left, right);
  }

}
