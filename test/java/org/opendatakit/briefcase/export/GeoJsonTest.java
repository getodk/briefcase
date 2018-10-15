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

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.javarosa.core.model.DataType.GEOPOINT;
import static org.javarosa.core.model.DataType.GEOSHAPE;
import static org.javarosa.core.model.DataType.GEOTRACE;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.export.GeoJson.getLngLatAlt;
import static org.opendatakit.briefcase.export.ModelBuilder.field;
import static org.opendatakit.briefcase.export.ModelBuilder.geopoint;
import static org.opendatakit.briefcase.export.ModelBuilder.instance;
import static org.opendatakit.briefcase.export.ModelBuilder.parseXmlElement;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.geojson.LineString;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.Polygon;
import org.hamcrest.Matchers;
import org.javarosa.core.model.DataType;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

public class GeoJsonTest {

  @Test
  public void knows_how_to_transform_serialized_spatial_data_to_maybe_LngLatAlt() {
    assertThat(getLngLatAlt("1 2"), isPresentAndIs(new LngLatAlt(2, 1)));
    assertThat(getLngLatAlt("1 2 3"), isPresentAndIs(new LngLatAlt(2, 1, 3)));
    assertThat(getLngLatAlt("1 2 3 4"), isPresentAndIs(new LngLatAlt(2, 1, 3)));
  }

  @Test
  public void knows_how_to_transform_serialized_spatial_data_to_lists_of_LngLatAlt() {
    assertThat(
        GeoJson.toLngLatAlts(Optional.empty()),
        Matchers.empty()
    );
    assertThat(
        GeoJson.toLngLatAlts(Optional.of("")),
        Matchers.empty()
    );
    assertThat(
        GeoJson.toLngLatAlts(Optional.of("1 2")),
        Matchers.contains(new LngLatAlt(2, 1))
    );
    assertThat(
        GeoJson.toLngLatAlts(Optional.of("1 2;")),
        Matchers.contains(new LngLatAlt(2, 1))
    );
    assertThat(
        GeoJson.toLngLatAlts(Optional.of("1 2;3 4")),
        Matchers.contains(new LngLatAlt(2, 1), new LngLatAlt(4, 3))
    );
  }

  @Test
  public void transforms_a_list_of_LngLatAlt_to_the_corresponding_GeoJsonObject_subtype() {
    assertThat(
        transform(GEOPOINT, ""),
        isEmpty()
    );
    assertThat(
        transform(GEOPOINT, "1 2"),
        isPresentAndIs(new Point(2, 1))
    );
    assertThat(
        transform(GEOTRACE, "1 2;3 4"),
        isPresentAndIs(new LineString(new LngLatAlt(2, 1), new LngLatAlt(4, 3)))
    );
    assertThat(
        transform(GEOSHAPE, "1 2;3 4;5 6;1 2"),
        isPresentAndIs(new Polygon(new LngLatAlt(2, 1), new LngLatAlt(4, 3), new LngLatAlt(6, 5), new LngLatAlt(2, 1)))
    );
  }

  private static Optional<GeoJsonObject> transform(DataType type, String value) {
    return GeoJson.toGeoJsonObject(
        field("field", type).build(),
        GeoJson.toLngLatAlts(Optional.of(value))
    );
  }

  @Test
  public void transforms_a_GeoJsonObject_to_a_Feature_with_submission_and_field_metadata() throws IOException, XmlPullParserException {
    Point point = new Point(2, 1);
    Model field = geopoint("some-field").build();
    XmlElement root = parseXmlElement("" +
        "<data instanceID=\"uuid:39f3dd36-161e-45cb-a1a4-395831d253a7\">" +
        " <some-field>1 2</some-field>" +
        "</data>");
    Submission submission = Submission.notValidated(
        Paths.get("/some/path"),
        Paths.get("/some/path"),
        root,
        fakeSubmissionMetadata("123"),
        Optional.empty(),
        Optional.empty()
    );
    Feature feature = GeoJson.toFeature(field, submission, Optional.of(point));
    assertThat(feature.getGeometry(), is(point));
    assertThat(feature.getProperty("key"), is("123"));
    assertThat(feature.getProperty("field"), is("some-field"));
    assertThat(feature.getProperty("empty"), is("no"));
  }

  @Test
  public void transforms_a_submission_to_a_feature_list() throws IOException, XmlPullParserException {
    Model field = instance(geopoint("some-field")).build().getChildByName("some-field");
    XmlElement root = parseXmlElement("" +
        "<data instanceID=\"uuid:39f3dd36-161e-45cb-a1a4-395831d253a7\">" +
        " <some-field>1 2 3 4</some-field>" +
        "</data>");
    Submission submission = Submission.notValidated(
        Paths.get("/some/path"),
        Paths.get("/some/path"),
        root,
        fakeSubmissionMetadata("123"),
        Optional.empty(),
        Optional.empty()
    );
    List<Feature> features = GeoJson.toFeatures(field.getParent().getParent(), submission).collect(Collectors.toList());
    assertThat(features, hasSize(1));
    Feature feature = features.get(0);
    assertThat(feature.getGeometry(), is(new Point(2, 1, 3)));
    assertThat(feature.getProperty("key"), is("123"));
    assertThat(feature.getProperty("field"), is("some-field"));
    assertThat(feature.getProperty("empty"), is("no"));
  }

  private SubmissionMetaData fakeSubmissionMetadata(String instanceId) {
    return new FakeSubmissionMetaData(instanceId);
  }

  class FakeSubmissionMetaData extends SubmissionMetaData {

    private final String instanceId;

    FakeSubmissionMetaData(String instanceId) {
      super(null);
      this.instanceId = instanceId;
    }

    @Override
    Optional<String> getInstanceId() {
      return Optional.of(instanceId);
    }
  }
}