package org.opendatakit.briefcase.model;

import static java.util.stream.Collectors.toList;
import static org.opendatakit.briefcase.model.FormStatus.TransferType.EXPORT;

import java.util.List;
import java.util.stream.IntStream;

public class FormStatusBuilder {

  public static FormStatus buildFormStatus(int id) {
    try {
      return new FormStatus(EXPORT, new TestFormDefinition(id));
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  public static List<FormStatus> buildFormStatusList(int amount) {
    return IntStream.range(0, amount).boxed().map(FormStatusBuilder::buildFormStatus).collect(toList());
  }
}
