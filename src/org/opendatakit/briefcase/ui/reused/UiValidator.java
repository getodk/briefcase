package org.opendatakit.briefcase.ui.reused;

import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class UiValidator {
  private final JLabel label;
  private final Predicate<String>[] predicates;
  private final Supplier<Optional<String>> valueGetter;
  private boolean isDirty = false;
  private boolean isValid = false;
  private Runnable onChangeCallback = () -> { };

  @SafeVarargs
  public UiValidator(JTextField field, JLabel label, Predicate<String>... predicates) {
    this.label = label;
    this.predicates = predicates;
    this.valueGetter = () -> Optional.ofNullable(field.getText()).map(String::trim);
    KeyAdapter onKeyReleasedAdapter = new KeyAdapterBuilder().onKeyReleased(e -> update()).build();
    field.addKeyListener(onKeyReleasedAdapter);
  }

  public void update() {
    boolean previousIsValid = isValid;
    Optional<String> maybeValue = valueGetter.get().filter(s -> !s.isEmpty());

    isDirty = isDirty || maybeValue.isPresent();
    isValid = maybeValue
        .map(s -> Stream.of(predicates).map(p -> p.test(s)).reduce(Boolean.TRUE, Boolean::logicalAnd))
        .orElse(false);

    label.setForeground(isDirty && !isValid ? Color.RED : Color.BLACK);

    if (isValid != previousIsValid)
      onChangeCallback.run();
  }

  public boolean isValid() {
    return isDirty && isValid;
  }

  public UiValidator onChange(Runnable callback) {
    onChangeCallback = callback;
    return this;
  }
}
