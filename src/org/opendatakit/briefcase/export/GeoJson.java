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

package org.opendatakit.briefcase.export;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.empty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
import org.opendatakit.briefcase.reused.OptionalProduct;
import org.opendatakit.briefcase.reused.UncheckedFiles;

class GeoJson {

  static Stream<Feature> toFeatures(Model model, Submission submission) {
    String instanceId = submission.getInstanceId(false);
    return model.getSpatialFields().stream().map(field -> {
      Optional<String> maybeValue = submission.findElement(field.getName()).flatMap(XmlElement::maybeValue);
      return maybeValue.map(value -> {
        List<LngLatAlt> lngLatAlts = maybeValue.map(GeoJson::toLngLatAlts).orElse(emptyList());
        return toGeoJsonObject(field, lngLatAlts)
            .map(geoJsonObject -> validFeature(field, instanceId, geoJsonObject))
            .orElse(invalidFeature(field, instanceId));
      }).orElse(emptyFeature(field, instanceId));
    });
  }

  static void write(Stream<Feature> features) {
    FeatureCollection fc = new FeatureCollection();
    features.forEach(fc::add);
    try {
      String contents = new ObjectMapper().writeValueAsString(fc);
      UncheckedFiles.write(Paths.get("/tmp/demo.geojson"), contents, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private static Feature validFeature(Model field, String instanceId, GeoJsonObject geoJsonObject) {
    return toFeature(field, instanceId, Optional.of(geoJsonObject), false, true);
  }

  private static Feature emptyFeature(Model field, String instanceId) {
    return toFeature(field, instanceId, Optional.empty(), true, true);
  }

  private static Feature invalidFeature(Model field, String instanceId) {
    return toFeature(field, instanceId, Optional.empty(), false, false);
  }

  private static Feature toFeature(Model field, String instanceId, Optional<GeoJsonObject> geoJsonObject, boolean empty, boolean valid) {
    Feature feature = new Feature();
    feature.setGeometry(geoJsonObject.orElse(null));
    feature.setProperty("key", instanceId);
    feature.setProperty("field", field.getName());
    feature.setProperty("empty", empty ? "yes" : "no");
    feature.setProperty("valid", valid ? "yes" : "no");
    return feature;
  }

  static List<LngLatAlt> toLngLatAlts(String value) {
    Stream<String> split = Stream.of(value.split(";"));
    Stream<LngLatAlt> optionalStream = split.flatMap(s -> GeoJson.getLngLatAlt(s).map(Stream::of).orElse(empty()));
    return optionalStream.collect(toList());
  }

  private static Optional<GeoJsonObject> toGeoJsonObject(Model field, List<LngLatAlt> lngLatAlts) {
    if (field.getDataType() == DataType.GEOPOINT && lngLatAlts.size() == 1)
      return Optional.of(new Point(lngLatAlts.get(0)));

    if (field.getDataType() == DataType.GEOTRACE && lngLatAlts.size() >= 2)
      return Optional.of(new LineString(lngLatAlts.toArray(new LngLatAlt[0])));

    if (field.getDataType() == DataType.GEOSHAPE && lngLatAlts.size() >= 4 && lngLatAlts.get(0).equals(lngLatAlts.get(lngLatAlts.size() - 1)))
      return Optional.of(new Polygon(lngLatAlts.toArray(new LngLatAlt[0])));

    return Optional.empty();
  }

  private static Optional<LngLatAlt> getLngLatAlt(String geoPoint) {
    String[] fields = geoPoint.split(" ");
    if (fields.length < 2 || fields.length > 4)
      return Optional.empty();
    Optional<Double> altitude = readAltitude(fields);
    return OptionalProduct.all(
        readLongitude(fields[1]),
        readLatitude(fields[0])
    ).map((lon, lat) -> altitude
        .map(alt -> new LngLatAlt(lon, lat, alt))
        .orElse(new LngLatAlt(lon, lat)));
  }

  private static Optional<Double> readLatitude(String field) {
    return Optional.ofNullable(field).filter(s -> !s.isEmpty()).map(Double::parseDouble).filter(lat -> lat >= -90 && lat <= 90);
  }

  private static Optional<Double> readLongitude(String field) {
    return Optional.ofNullable(field).filter(s -> !s.isEmpty()).map(Double::parseDouble).filter(lng -> lng >= -180 && lng <= 180);
  }

  private static Optional<Double> readAltitude(String[] fields) {
    return fields.length > 2
        ? Optional.ofNullable(fields[2]).map(Double::parseDouble)
        : Optional.empty();
  }

}
