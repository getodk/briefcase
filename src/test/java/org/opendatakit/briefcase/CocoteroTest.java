package org.opendatakit.briefcase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.Test;

public class CocoteroTest {
  @Test
  public void name() throws IOException {
    ObjectMapper mapper = new ObjectMapper();

    JsonNode jsonNode = mapper.readTree("[1,2,3]");

    List<JsonNode> collect = StreamSupport.stream(jsonNode.spliterator(), false).collect(Collectors.toList());
    System.out.println(jsonNode);
  }
}
