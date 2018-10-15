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

  private GeoJson() {
  }

  public static Stream<Feature> toFeatures(Model model, Submission submission) {
    return model.getSpatialFields().stream().map(field -> {
      Optional<String> maybeValue = submission.findElement(field.getName()).flatMap(XmlElement::maybeValue);
      List<LngLatAlt> lngLatAlts = maybeValue.map(GeoJson::toLngLatAlts).orElse(emptyList());
      Optional<GeoJsonObject> geoJsonObject = toGeoJsonObject(field, lngLatAlts);
      return toFeature(field, submission, geoJsonObject);
    });
  }

  public static Feature toFeature(Model field, Submission submission, Optional<GeoJsonObject> maybeGeoJsonObject) {
    Feature feature = new Feature();
    feature.setGeometry(maybeGeoJsonObject.orElse(null));
    feature.setProperty("key", submission.getInstanceId(false));
    feature.setProperty("field", field.getName());
    feature.setProperty("empty", maybeGeoJsonObject.map(__ -> "no").orElse("yes"));
    return feature;
  }

  public static List<LngLatAlt> toLngLatAlts(String value) {
    Stream<String> split = Stream.of(value.split(";"));
    Stream<LngLatAlt> optionalStream = split.flatMap(s -> GeoJson.getLngLatAlt(s).map(Stream::of).orElse(empty()));
    return optionalStream.collect(toList());
  }

  public static Optional<GeoJsonObject> toGeoJsonObject(Model field, List<LngLatAlt> lngLatAlts) {
    if (lngLatAlts.isEmpty())
      return Optional.empty();

    if (lngLatAlts.size() == 1)
      return Optional.of(new Point(lngLatAlts.get(0)));

    if (lngLatAlts.size() == 2 || field.getDataType() == DataType.GEOTRACE || !lngLatAlts.get(0).equals(lngLatAlts.get(lngLatAlts.size() - 1)))
      return Optional.of(new LineString(lngLatAlts.toArray(new LngLatAlt[0])));

    return Optional.of(new Polygon(lngLatAlts.toArray(new LngLatAlt[0])));
  }

  public static void write(Stream<Feature> features) {
    FeatureCollection fc = new FeatureCollection();
    features.forEach(fc::add);
    try {
      String contents = new ObjectMapper().writeValueAsString(fc);
      UncheckedFiles.write(Paths.get("/tmp/demo.geojson"), contents, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  static Optional<LngLatAlt> getLngLatAlt(String geoPoint) {
    String[] fields = geoPoint.split(" ", 4);
    Double altitude = fields.length > 2
        ? Optional.ofNullable(fields[2]).map(Double::parseDouble).orElse(null)
        : null;
    return fields.length > 1
        ? OptionalProduct
        .all(
            Optional.ofNullable(fields[1]).filter(s -> !s.isEmpty()).map(Double::parseDouble),
            Optional.ofNullable(fields[0]).filter(s -> !s.isEmpty()).map(Double::parseDouble)
        )
        .map((lon, lat) -> altitude != null
            ? new LngLatAlt(lon, lat, altitude)
            : new LngLatAlt(lon, lat))
        : Optional.empty();
  }


}
