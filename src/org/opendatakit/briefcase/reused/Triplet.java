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

public class Triplet<L, M, R> {
  public final L left;
  public final M middle;
  public final R right;

  public Triplet(L left, M middle, R right) {
    this.left = left;
    this.middle = middle;
    this.right = right;
  }

  @SuppressWarnings("checkstyle:MethodTypeParameterName")
  public static <LL, MM, RR> Triplet<LL, MM, RR> of(LL left, MM middle, RR right) {
    return new Triplet<>(left, middle, right);
  }

  public L getLeft() {
    return left;
  }

  public M getMiddle() {
    return middle;
  }

  public R getRight() {
    return right;
  }

  @Override
  public String toString() {
    return "Pair{" +
        "left=" + left +
        ", middle=" + middle +
        ", right=" + right +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Triplet<?, ?, ?> triplet = (Triplet<?, ?, ?>) o;
    return Objects.equals(left, triplet.left) &&
        Objects.equals(middle, triplet.middle) &&
        Objects.equals(right, triplet.right);
  }

  @Override
  public int hashCode() {
    return Objects.hash(left, middle, right);
  }
}
