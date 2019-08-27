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

package org.opendatakit.briefcase.operations.export;

import static java.lang.Math.abs;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.empty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.Polygon;
import org.javarosa.core.model.DataType;
import org.opendatakit.briefcase.reused.api.OptionalProduct;
import org.opendatakit.briefcase.reused.api.UncheckedFiles;
import org.opendatakit.briefcase.reused.model.XmlElement;
import org.opendatakit.briefcase.reused.model.form.FormModel;
import org.opendatakit.briefcase.reused.model.submission.Submission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class GeoJson {
  private static final Logger log = LoggerFactory.getLogger(GeoJson.class);
  private static final String POINT_COMPONENT_SEPARATOR = " ";
  private static final String POINT_STRING_SEPARATOR = ";";

  static Stream<Feature> toFeatures(FormModel model, Submission submission) {
    String instanceId = submission.getInstanceId();
    return model.getSpatialFields().stream().map(field -> {
      // Get the value on the submission
      Optional<String> maybeValue = submission.findElement(field.getName()).flatMap(XmlElement::maybeValue);
      if (maybeValue.isEmpty())
        return emptyFeature(field, instanceId);

      // Transform the point string to a list of LngLatAlts
      List<LngLatAlt> lngLatAlts = maybeValue.map(GeoJson::toLngLatAlts).orElse(emptyList());

      // TODO This would be much better with a Try<GeoJsonObject>
      try {
        // Build the corresponding GeoJSON object and return a valid Feature
        GeoJsonObject geoJsonObject = toGeoJsonObject(field, lngLatAlts);
        return validFeature(field, instanceId, geoJsonObject);
      } catch (IllegalArgumentException e) {
        // Log the combination of field type and serialized data and return an invalid Feature
        log.debug("Illegal combination of field type {} and spatial data {}", field.getDataType().name(), maybeValue.get());
        return invalidFeature(field, instanceId);
      }
    });
  }

  static void write(Path output, Stream<Feature> features) {
    FeatureCollection fc = new FeatureCollection();
    features.forEach(fc::add);
    try {
      String contents = new ObjectMapper().writeValueAsString(fc);
      UncheckedFiles.write(output, contents, CREATE, TRUNCATE_EXISTING);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private static Feature validFeature(FormModel field, String instanceId, GeoJsonObject geoJsonObject) {
    return feature(field, instanceId, Optional.of(geoJsonObject), false, true);
  }

  private static Feature emptyFeature(FormModel field, String instanceId) {
    return feature(field, instanceId, Optional.empty(), true, true);
  }

  private static Feature invalidFeature(FormModel field, String instanceId) {
    return feature(field, instanceId, Optional.empty(), false, false);
  }

  private static Feature feature(FormModel field, String instanceId, Optional<GeoJsonObject> geoJsonObject, boolean empty, boolean valid) {
    Feature feature = new Feature();
    feature.setGeometry(geoJsonObject.orElse(null));
    feature.setProperty("key", instanceId);
    feature.setProperty("field", field.getName());
    feature.setProperty("empty", empty ? "yes" : "no");
    feature.setProperty("valid", valid ? "yes" : "no");
    return feature;
  }

  private static List<LngLatAlt> toLngLatAlts(String value) {
    return Stream.of(value.split(POINT_STRING_SEPARATOR))
        .flatMap(s -> GeoJson.parseLngLatAlt(s).map(Stream::of).orElse(empty()))
        .collect(toList());
  }

  private static GeoJsonObject toGeoJsonObject(FormModel field, List<LngLatAlt> lngLatAlts) {
    if (field.getDataType() == DataType.GEOPOINT && lngLatAlts.size() == 1)
      return new Point(lngLatAlts.get(0));

    if (field.getDataType() == DataType.GEOTRACE && lngLatAlts.size() >= 2)
      return new LineString(lngLatAlts.toArray(new LngLatAlt[0]));

    if (field.getDataType() == DataType.GEOSHAPE && lngLatAlts.size() >= 4 && lngLatAlts.get(0).equals(lngLatAlts.get(lngLatAlts.size() - 1)))
      return new Polygon(lngLatAlts.toArray(new LngLatAlt[0]));

    throw new IllegalArgumentException();
  }

  private static Optional<LngLatAlt> parseLngLatAlt(String geoPoint) {
    String[] fields = geoPoint.split(POINT_COMPONENT_SEPARATOR);
    if (fields.length < 2 || fields.length > 4)
      return Optional.empty();
    Optional<Double> altitude = fields.length > 2 ? maybeParseDouble(fields[2]) : Optional.empty();
    return OptionalProduct.all(
        maybeParseDouble(fields[1], 180),
        maybeParseDouble(fields[0], 90)
    ).map((lon, lat) -> altitude
        .map(alt -> new LngLatAlt(lon, lat, alt))
        .orElse(new LngLatAlt(lon, lat)));
  }

  private static Optional<Double> maybeParseDouble(String value) {
    return Optional.ofNullable(value).filter(s -> !s.isEmpty()).map(Double::parseDouble);
  }

  private static Optional<Double> maybeParseDouble(String value, int absBound) {
    return Optional.ofNullable(value).filter(s -> !s.isEmpty()).map(Double::parseDouble).filter(lat -> abs(lat) <= absBound);
  }

}
