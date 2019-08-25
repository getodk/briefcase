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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.operations.export.GeoJsonTest.allMatch;
import static org.opendatakit.briefcase.reused.api.UncheckedFiles.deleteRecursive;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Point;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GeoJsonWriteTest {

  private Path output;

  @Before
  public void setUp() throws Exception {
    output = Files.createTempDirectory("briefcase_test_").resolve("output.geojson");
  }

  @After
  public void tearDown() {
    deleteRecursive(output.getParent());
  }

  @Test
  public void writes_the_GeoJSON_file() throws IOException {
    List<Feature> outputFeatures = IntStream.range(0, 1000).mapToObj(i -> buildFeature()).collect(Collectors.toList());
    GeoJson.write(output, outputFeatures.stream());
    FeatureCollection parsedFeatureCollection = new ObjectMapper().readValue(Files.newInputStream(output), FeatureCollection.class);
    List<Feature> actualFeatures = parsedFeatureCollection.getFeatures();
    assertThat(actualFeatures, hasSize(1000));
    assertThat(actualFeatures, allMatch(feature -> feature.getGeometry() instanceof Point));
    assertThat(actualFeatures, allMatch(feature -> ((Point) feature.getGeometry()).getCoordinates() != null));
    assertThat(actualFeatures, allMatch(feature -> feature.getProperties().containsKey("key") && feature.getProperty("key") != null));
    assertThat(actualFeatures, allMatch(feature -> feature.getProperties().containsKey("field") && feature.getProperty("field").equals("some-field")));
    assertThat(actualFeatures, allMatch(feature -> feature.getProperties().containsKey("empty") && feature.getProperty("empty").equals("no")));
    assertThat(actualFeatures, allMatch(feature -> feature.getProperties().containsKey("valid") && feature.getProperty("valid").equals("yes")));
  }

  private static final AtomicInteger instanceIdSeq = new AtomicInteger(1);

  private Feature buildFeature() {
    Feature feature = new Feature();
    feature.setGeometry(new Point(Math.random() * 360 - 180, Math.random() * 180 - 90));
    feature.setProperty("key", instanceIdSeq.getAndIncrement());
    feature.setProperty("field", "some-field");
    feature.setProperty("empty", "no");
    feature.setProperty("valid", "yes");
    return feature;
  }
}
