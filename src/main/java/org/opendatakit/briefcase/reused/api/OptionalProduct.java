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
package org.opendatakit.briefcase.reused.api;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * This class represents the type-safe Application of an arbitrary amount of {@link Optional} values.
 * <p>
 * When all its members are present, it behaves like a n-arity version of an {@link Optional} value, being able to
 * map and flatMap its contents.
 * <p>
 * When any of its members is missing, it behaves like a {@link Optional#empty()}
 */
public interface OptionalProduct {

  /**
   * Factory of {@link OptionalProduct} for an arity of 4
   */
  static <T, U, V, W> OptionalProduct4<T, U, V, W> all(Optional<T> t, Optional<U> u, Optional<V> v, Optional<W> w) {
    if (t.isPresent() && u.isPresent() && v.isPresent() && w.isPresent())
      return new OptionalProduct4.Some<>(t.get(), u.get(), v.get(), w.get());
    return new OptionalProduct4.None<>();
  }

  /**
   * Factory of {@link OptionalProduct} for an arity of 3
   */
  static <T, U, V> OptionalProduct3<T, U, V> all(Optional<T> t, Optional<U> u, Optional<V> v) {
    if (t.isPresent() && u.isPresent() && v.isPresent())
      return new OptionalProduct3.Some<>(t.get(), u.get(), v.get());
    return new OptionalProduct3.None<>();
  }

  /**
   * Factory of {@link OptionalProduct} for an arity of 2
   */
  static <T, U> OptionalProduct2<T, U> all(Optional<T> t, Optional<U> u) {
    if (t.isPresent() && u.isPresent())
      return new OptionalProduct2.Some<>(t.get(), u.get());
    return new OptionalProduct2.None<>();
  }

  @SafeVarargs
  static <T> Optional<T> firstPresent(Optional<T>... optionals) {
    return Stream.of(optionals)
        .filter(Optional::isPresent)
        .findFirst()
        .flatMap(o -> o);
  }

  interface OptionalProduct2<T, U> {
    <V> Optional<V> map(BiFunction<T, U, V> mapper);

    <V> Optional<V> flatMap(BiFunction<T, U, Optional<V>> mapper);

    <TT, UU> OptionalProduct2<TT, UU> map2(BiFunction<T, U, Pair<TT, UU>> mapMapper);

    void ifPresent(BiConsumer<T, U> consumer);

    boolean isPresent();

    class Some<T, U> implements OptionalProduct2<T, U> {
      private final T t;
      private final U u;

      Some(T t, U u) {
        this.t = t;
        this.u = u;
      }

      @Override
      public <V> Optional<V> map(BiFunction<T, U, V> mapper) {
        return Optional.of(mapper.apply(t, u));
      }

      @Override
      public <V> Optional<V> flatMap(BiFunction<T, U, Optional<V>> mapper) {
        return mapper.apply(t, u);
      }

      @Override
      public <TT, UU> OptionalProduct2<TT, UU> map2(BiFunction<T, U, Pair<TT, UU>> mapMapper) {
        Pair<TT, UU> pair = mapMapper.apply(t, u);
        return new Some<>(pair.getLeft(), pair.getRight());
      }

      @Override
      public void ifPresent(BiConsumer<T, U> consumer) {
        consumer.accept(t, u);
      }

      @Override
      public boolean isPresent() {
        return true;
      }
    }

    class None<T, U> implements OptionalProduct2<T, U> {

      @Override
      public <V> Optional<V> map(BiFunction<T, U, V> mapper) {
        return Optional.empty();
      }

      @Override
      public <TT, UU> OptionalProduct2<TT, UU> map2(BiFunction<T, U, Pair<TT, UU>> mapMapper) {
        return new None<>();
      }

      @Override
      public <V> Optional<V> flatMap(BiFunction<T, U, Optional<V>> mapper) {
        return Optional.empty();
      }

      @Override
      public void ifPresent(BiConsumer<T, U> consumer) {
        // Do nothing
      }

      @Override
      public boolean isPresent() {
        return false;
      }
    }
  }

  interface OptionalProduct3<T, U, V> {
    <W> Optional<W> map(TriFunction<T, U, V, W> mapper);

    <W> Optional<W> flatMap(TriFunction<T, U, V, Optional<W>> mapper);

    <TT, UU> OptionalProduct2<TT, UU> map2(TriFunction<T, U, V, Pair<TT, UU>> mapMapper);

    <TT, UU, VV> OptionalProduct3<TT, UU, VV> map3(TriFunction<T, U, V, Triple<TT, UU, VV>> mapMapper);

    class Some<T, U, V> implements OptionalProduct3<T, U, V> {
      private final T t;
      private final U u;
      private final V v;

      Some(T t, U u, V v) {
        this.t = t;
        this.u = u;
        this.v = v;
      }

      @Override
      public <W> Optional<W> map(TriFunction<T, U, V, W> mapper) {
        return Optional.of(mapper.apply(t, u, v));
      }

      @Override
      public <W> Optional<W> flatMap(TriFunction<T, U, V, Optional<W>> mapper) {
        return mapper.apply(t, u, v);
      }

      @Override
      public <TT, UU> OptionalProduct2<TT, UU> map2(TriFunction<T, U, V, Pair<TT, UU>> mapMapper) {
        Pair<TT, UU> pair = mapMapper.apply(t, u, v);
        return new OptionalProduct2.Some<>(pair.getLeft(), pair.getRight());
      }

      @Override
      public <TT, UU, VV> OptionalProduct3<TT, UU, VV> map3(TriFunction<T, U, V, Triple<TT, UU, VV>> mapMapper) {
        Triple<TT, UU, VV> triple = mapMapper.apply(t, u, v);
        return new Some<>(triple.get1(), triple.get2(), triple.get3());
      }
    }

    class None<T, U, V> implements OptionalProduct3<T, U, V> {

      @Override
      public <W> Optional<W> map(TriFunction<T, U, V, W> mapper) {
        return Optional.empty();
      }

      @Override
      public <W> Optional<W> flatMap(TriFunction<T, U, V, Optional<W>> mapper) {
        return Optional.empty();
      }

      @Override
      public <TT, UU> OptionalProduct2<TT, UU> map2(TriFunction<T, U, V, Pair<TT, UU>> mapMapper) {
        return new OptionalProduct2.None<>();
      }

      @Override
      public <TT, UU, VV> OptionalProduct3<TT, UU, VV> map3(TriFunction<T, U, V, Triple<TT, UU, VV>> mapMapper) {
        return new None<>();
      }

    }
  }

  interface OptionalProduct4<T, U, V, W> {
    <X> Optional<X> map(TetraFunction<T, U, V, W, X> mapper);

    <X> Optional<X> flatMap(TetraFunction<T, U, V, W, Optional<X>> mapper);

    <TT, UU> OptionalProduct2<TT, UU> map2(TetraFunction<T, U, V, W, Pair<TT, UU>> mapMapper);

    <TT, UU, VV> OptionalProduct3<TT, UU, VV> map3(TetraFunction<T, U, V, W, Triple<TT, UU, VV>> mapMapper);

    <TT, UU, VV, WW> OptionalProduct4<TT, UU, VV, WW> map4(TetraFunction<T, U, V, W, Quad<TT, UU, VV, WW>> mapMapper);

    class Some<T, U, V, W> implements OptionalProduct4<T, U, V, W> {
      private final T t;
      private final U u;
      private final V v;
      private final W w;

      Some(T t, U u, V v, W w) {
        this.t = t;
        this.u = u;
        this.v = v;
        this.w = w;
      }

      @Override
      public <X> Optional<X> map(TetraFunction<T, U, V, W, X> mapper) {
        return Optional.of(mapper.apply(t, u, v, w));
      }

      @Override
      public <X> Optional<X> flatMap(TetraFunction<T, U, V, W, Optional<X>> mapper) {
        return mapper.apply(t, u, v, w);
      }

      @Override
      public <TT, UU> OptionalProduct2<TT, UU> map2(TetraFunction<T, U, V, W, Pair<TT, UU>> mapMapper) {
        Pair<TT, UU> pair = mapMapper.apply(t, u, v, w);
        return new OptionalProduct2.Some<>(pair.getLeft(), pair.getRight());
      }

      @Override
      public <TT, UU, VV> OptionalProduct3<TT, UU, VV> map3(TetraFunction<T, U, V, W, Triple<TT, UU, VV>> mapMapper) {
        Triple<TT, UU, VV> triple = mapMapper.apply(t, u, v, w);
        return new OptionalProduct3.Some<>(triple.get1(), triple.get2(), triple.get3());
      }

      @Override
      public <TT, UU, VV, WW> OptionalProduct4<TT, UU, VV, WW> map4(TetraFunction<T, U, V, W, Quad<TT, UU, VV, WW>> mapMapper) {
        Quad<TT, UU, VV, WW> quad = mapMapper.apply(t, u, v, w);
        return new Some<>(quad.get1(), quad.get2(), quad.get3(), quad.get_4());
      }
    }

    class None<T, U, V, W> implements OptionalProduct4<T, U, V, W> {

      @Override
      public <X> Optional<X> map(TetraFunction<T, U, V, W, X> mapper) {
        return Optional.empty();
      }

      @Override
      public <X> Optional<X> flatMap(TetraFunction<T, U, V, W, Optional<X>> mapper) {
        return Optional.empty();
      }

      @Override
      public <TT, UU> OptionalProduct2<TT, UU> map2(TetraFunction<T, U, V, W, Pair<TT, UU>> mapMapper) {
        return new OptionalProduct2.None<>();
      }

      @Override
      public <TT, UU, VV> OptionalProduct3<TT, UU, VV> map3(TetraFunction<T, U, V, W, Triple<TT, UU, VV>> mapMapper) {
        return new OptionalProduct3.None<>();
      }

      @Override
      public <TT, UU, VV, WW> OptionalProduct4<TT, UU, VV, WW> map4(TetraFunction<T, U, V, W, Quad<TT, UU, VV, WW>> mapMapper) {
        return new None<>();
      }
    }
  }
}
