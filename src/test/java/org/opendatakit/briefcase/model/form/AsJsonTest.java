package org.opendatakit.briefcase.model.form;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.opendatakit.briefcase.model.form.AsJson.getJson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.npathai.hamcrestopt.OptionalMatchers;
import org.junit.Test;

public class AsJsonTest {

  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

  @Test
  public void adapts_null_nodes_and_values_to_empty_optionals() {
    // Create the representation of {"a": null}
    ObjectNode root = MAPPER.createObjectNode();
    root.put("a", (String) null);

    // Showcase Jackson's standard behavior
    assertThat(root.get("a").isNull(), is(true)); // OK
    assertThat(root.get("a").asText(), is("null")); // Confusing
    assertThat(root.get("unknown key"), is(nullValue())); // OK
    assertThat(root.path("a").isNull(), is(true)); // OK
    assertThat(root.path("a").asText(), is("null")); // Confusing
    assertThat(root.path("unknown key").isNull(), is(false)); // Confusing
    assertThat(root.path("unknown key").asText(), is("")); // Confusing

    // Verify that our getJson method works as we expect
    assertThat(getJson(root, "a"), OptionalMatchers.isEmpty());
    assertThat(getJson(root, "unknown key"), OptionalMatchers.isEmpty());
    // For all effects, getJson will behave as if {"a": null} is the same as {}
  }
}
