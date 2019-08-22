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

import static org.opendatakit.briefcase.model.form.AsJson.getJson;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.opendatakit.briefcase.model.form.AsJson;
import org.opendatakit.briefcase.reused.BriefcaseException;

/**
 * This class stores information about a cursor to a list of remote submission
 * instance IDs, or "resumptionCursor" as described in the <a href="https://docs.opendatakit.org/briefcase-api/#returned-document">Briefcase Aggregate API docs</a>
 * <p>
 * OpenRosa specifies that cursors should be opaque, but to support the "start from last"
 * and "start from date" features, we need to try to parse it and even create artificial
 * cursors.
 */
public interface Cursor extends Comparable<Cursor>, AsJson {

  static Cursor empty() {
    return new EmptyCursor();
  }

  static Cursor from(JsonNode root) {
    Type type = getJson(root, "type").map(JsonNode::asText).map(Type::from).orElseThrow(BriefcaseException::new);
    String value = getJson(root, "value").map(JsonNode::asText).orElse("");
    return type.create(value);
  }


  static Cursor from(String value) {
    return firstNonFailing(
        () -> AggregateCursor.from(value),
        () -> OnaCursor.from(value),
        () -> OpaqueCursor.from(value)
    ).orElseThrow(() -> new BriefcaseException("Unknown cursor format"));
  }

  Cursor.Type getType();

  String getValue();

  boolean isEmpty();

  /**
   * Create a cursor that would start pulling from the provided date in Aggregate servers.
   */
  static AggregateCursor of(LocalDate lastUpdateDate) {
    return AggregateCursor.of(lastUpdateDate);
  }

  /**
   * Create a cursor that would start pulling from the provided date in Aggregate servers.
   */
  static Cursor of(OffsetDateTime lastUpdateDateTime, String uid) {
    return AggregateCursor.of(lastUpdateDateTime, uid);
  }

  @SafeVarargs
  static Optional<Cursor> firstNonFailing(Supplier<Cursor>... suppliers) {
    for (Supplier<Cursor> supplier : suppliers) {
      try {
        return Optional.of(supplier.get());
      } catch (Throwable t) {
        // Ignore exception
      }
    }
    return Optional.empty();
  }


  enum Type {
    EMPTY("empty", s -> new EmptyCursor()),
    AGGREGATE("aggregate", AggregateCursor::from),
    ONA("ona", OnaCursor::from),
    OPAQUE("opaque", OpaqueCursor::from);

    private final String name;
    private final Function<String, Cursor> factory;

    Type(String name, Function<String, Cursor> factory) {
      this.name = name;
      this.factory = factory;
    }

    public static Type from(String type) {
      return Stream.of(values())
          .filter(t -> t.getName().equals(type))
          .findFirst()
          .orElse(OPAQUE);
    }

    public Cursor create(String rawValue) {
      return factory.apply(rawValue);
    }

    public String getName() {
      return name;
    }
  }

}
