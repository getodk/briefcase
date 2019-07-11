package org.opendatakit.briefcase.ui.reused;

import static org.opendatakit.briefcase.ui.reused.UiFieldValidator.COLOR_NOT_VALID;
import static org.opendatakit.briefcase.ui.reused.UiFieldValidator.COLOR_VALID;

public class UiLinkedFieldValidator {
  private final UiFieldValidator validatorA;
  private final UiFieldValidator validatorB;
  private Runnable onChangeCallback = () -> { };
  private boolean isValid = false;

  private UiLinkedFieldValidator(UiFieldValidator validatorA, UiFieldValidator validatorB) {
    this.validatorA = validatorA;
    this.validatorB = validatorB;

    validatorA.afterUpdate(this::update);
    validatorB.afterUpdate(this::update);
  }

  private void update() {
    boolean previousIsValid = isValid;
    String valueA = validatorA.getValue();
    String valueB = validatorB.getValue();
    isValid = (valueA == null && valueB == null)
        || (valueA != null && valueA.trim().isEmpty() && valueB != null && valueB.trim().isEmpty())
        || (valueA != null && !valueA.trim().isEmpty() && valueB != null && !valueB.trim().isEmpty());

    validatorA.label.setForeground(isValid && validatorA.isValid() ? COLOR_VALID : COLOR_NOT_VALID);
    validatorB.label.setForeground(isValid && validatorB.isValid() ? COLOR_VALID : COLOR_NOT_VALID);

    if (isValid != previousIsValid)
      onChangeCallback.run();
  }

  public static UiLinkedFieldValidator of(UiFieldValidator validatorA, UiFieldValidator validatorB) {
    return new UiLinkedFieldValidator(validatorA, validatorB);
  }

  public UiLinkedFieldValidator onChange(Runnable callback) {
    onChangeCallback = callback;
    return this;
  }

  public boolean isValid() {
    return isValid;
  }
}
