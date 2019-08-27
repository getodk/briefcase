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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.javarosa.core.model.DataType.GEOPOINT;
import static org.javarosa.core.model.DataType.GEOSHAPE;
import static org.javarosa.core.model.DataType.GEOTRACE;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geojson.Feature;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.Polygon;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.javarosa.core.model.DataType;
import org.junit.Test;
import org.opendatakit.briefcase.reused.api.Pair;
import org.opendatakit.briefcase.reused.model.XmlElement;
import org.opendatakit.briefcase.reused.model.form.FormModel;
import org.opendatakit.briefcase.reused.model.form.ModelBuilder;
import org.opendatakit.briefcase.reused.model.submission.Submission;
import org.opendatakit.briefcase.reused.model.submission.SubmissionKey;
import org.opendatakit.briefcase.reused.model.submission.SubmissionMetadata;
import org.xmlpull.v1.XmlPullParserException;

public class GeoJsonTest {

  @Test
  public void transforms_a_submission_to_a_feature_list() throws IOException, XmlPullParserException {
    Map<String, Feature> features = getFeatures(
        Pair.of(GEOPOINT, "1 2 3"),
        Pair.of(GEOTRACE, "1 2;3 4"),
        Pair.of(GEOSHAPE, "1 2;3 4;5 6;1 2")
    ).stream().collect(toMap(
        f -> f.getProperty("field"),
        f -> f
    ));
    assertThat(features.values(), hasSize(3));
    assertThat(features.values(), allMatch(feature -> feature.getProperty("key").equals("uuid:39f3dd36-161e-45cb-a1a4-395831d253a7")));
    assertThat(features.values(), allMatch(feature -> feature.getProperty("empty").equals("no")));
    assertThat(features.get("some-field-1").getGeometry(), is(new Point(2, 1, 3)));
    assertThat(features.get("some-field-2").getGeometry(), is(new LineString(new LngLatAlt(2, 1), new LngLatAlt(4, 3))));
    assertThat(features.get("some-field-3").getGeometry(), is(new Polygon(new LngLatAlt(2, 1), new LngLatAlt(4, 3), new LngLatAlt(6, 5), new LngLatAlt(2, 1))));
  }

  @Test
  public void handles_empty_spatial_fields() throws IOException, XmlPullParserException {
    List<Feature> features = getFeatures(
        Pair.of(GEOPOINT, ""),
        Pair.of(GEOTRACE, ""),
        Pair.of(GEOSHAPE, "")
    );
    assertThat(features, hasSize(3));
    assertThat(features, allMatch(feature -> feature.getGeometry() == null));
    assertThat(features, allMatch(feature -> feature.getProperty("empty").equals("yes")));
  }

  @Test
  public void handles_invalid_GEOPOINT_values() throws IOException, XmlPullParserException {
    List<Feature> features = getFeatures(
        Pair.of(GEOPOINT, "a"), // we expect doubles
        Pair.of(GEOPOINT, "1"), // we expect at least two values
        Pair.of(GEOPOINT, "1 2 3 4 5"), // we expect at most 4 values
        Pair.of(GEOPOINT, "1 2;3 4"), // we dont' expect a string of points
        Pair.of(GEOPOINT, "91 2"), // we dont' expect latitudes greater than 90
        Pair.of(GEOPOINT, "-91 2"), // we dont' expect latitudes less than -90
        Pair.of(GEOPOINT, "1 181"), // we dont' expect latitudes greater than 180
        Pair.of(GEOPOINT, "1 -181") // we dont' expect latitudes less than -180
    );
    assertThat(features, hasSize(8));
    assertThat(features, allMatch(feature -> feature.getGeometry() == null));
    assertThat(features, allMatch(feature -> feature.getProperty("empty") == "no"));
    assertThat(features, allMatch(feature -> feature.getProperty("valid") == "no"));
  }

  @Test
  public void handles_invalid_GEOTRACE_values() throws IOException, XmlPullParserException {
    List<Feature> features = getFeatures(
        Pair.of(GEOTRACE, "1 2") // we expect at least a GEOPOINT
    );
    assertThat(features, hasSize(1));
    assertThat(features, allMatch(feature -> feature.getGeometry() == null));
    assertThat(features, allMatch(feature -> feature.getProperty("empty") == "no"));
    assertThat(features, allMatch(feature -> feature.getProperty("valid") == "no"));
  }

  @Test
  public void handles_invalid_GEOSHAPE_values() throws IOException, XmlPullParserException {
    List<Feature> features = getFeatures(
        Pair.of(GEOSHAPE, "1 2"), // we expect at least four GEOPOINTS
        Pair.of(GEOSHAPE, "1 2;2 3"), // we expect at least four GEOPOINTS
        Pair.of(GEOSHAPE, "1 2;2 3;4 5"), // we expect at least four GEOPOINTS
        Pair.of(GEOSHAPE, "1 2;2 3;4 5;6 7") // we expect the first and last GEOPOINTS to be the same
    );
    assertThat(features, hasSize(4));
    assertThat(features, allMatch(feature -> feature.getGeometry() == null));
    assertThat(features, allMatch(feature -> feature.getProperty("empty") == "no"));
    assertThat(features, allMatch(feature -> feature.getProperty("valid") == "no"));
  }

  static <T> Matcher<Collection<T>> allMatch(Predicate<T> predicate) {
    return new TypeSafeMatcher<Collection<T>>() {
      @Override
      protected boolean matchesSafely(Collection<T> item) {
        return item.stream().allMatch(predicate);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("all elements match given predicate");
      }
    };
  }

  @SafeVarargs
  private static List<Feature> getFeatures(Pair<DataType, String>... contents) throws XmlPullParserException, IOException {
    AtomicInteger seq = new AtomicInteger(1);
    List<Pair<ModelBuilder, String>> fieldsAndContents = Stream.of(contents).map(pair1 -> {
      String fieldName1 = "some-field-" + seq.getAndIncrement();
      return pair1.map(
          type -> ModelBuilder.field(fieldName1, type),
          c -> c
      );
    }).collect(toList());

    FormModel model = ModelBuilder.instance(fieldsAndContents.stream().map(Pair::getLeft).collect(toList())).build();

    StringBuilder xml = new StringBuilder("<data instanceID=\"uuid:39f3dd36-161e-45cb-a1a4-395831d253a7\">");
    fieldsAndContents.forEach(pair -> xml
        .append("<").append(pair.getLeft().getName()).append(">")
        .append(pair.getRight())
        .append("</").append(pair.getLeft().getName()).append(">"));
    xml.append("</data>");

    XmlElement root = ModelBuilder.parseXmlElement(xml.toString());
    Submission submission = Submission.plain(
        new SubmissionMetadata(
            new SubmissionKey(
                "Some form",
                Optional.empty(),
                "uuid:39f3dd36-161e-45cb-a1a4-395831d253a7"
            ),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Collections.emptyList()
        ),
        root
    );
    return GeoJson.toFeatures(model, submission).collect(Collectors.toList());
  }
}
