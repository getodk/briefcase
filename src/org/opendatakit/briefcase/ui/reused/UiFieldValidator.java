package org.opendatakit.briefcase.ui.reused;

import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.swing.JLabel;
import javax.swing.JTextField;
import org.opendatakit.briefcase.reused.http.RequestBuilder;

public class UiFieldValidator {
  public static final Predicate<String> REQUIRED = s -> s != null && !s.trim().isEmpty();
  public static final Predicate<String> NUMBER = s -> s == null || s.matches("\\d+");
  public static final Predicate<String> EMAIL = s -> s == null || s.matches("\\S+@\\S+");
  public static final Predicate<String> URI = s -> s == null || RequestBuilder.isUri(s);
  static final Color COLOR_NOT_VALID = new Color(215, 25, 28); // Color-blind safe red
  static final Color COLOR_VALID = new Color(44, 123, 182); // Color-blind safe blue
  private final JTextField field;
  final JLabel label;
  private final Predicate<String> predicate;
  private boolean isDirty = false;
  private boolean isValid = false;
  private Runnable onChangeCallback = () -> { };
  private Runnable afterUpdateCallback = () -> { };

  private UiFieldValidator(JTextField field, JLabel label, Predicate<String> predicate) {
    this.field = field;
    this.label = label;
    this.predicate = predicate;
    KeyAdapter onKeyReleasedAdapter = new KeyAdapterBuilder().onKeyReleased(e -> update()).build();
    field.addKeyListener(onKeyReleasedAdapter);
  }

  @SafeVarargs
  public static UiFieldValidator of(JTextField field, JLabel label, Predicate<String>... predicates) {
    return new UiFieldValidator(field, label, Stream.of(predicates).reduce(s -> true, Predicate::and));
  }

  public void update() {
    boolean previousIsDirty = isDirty;
    boolean previousIsValid = isValid;
    String value = getValue();

    isDirty = isDirty || (value != null && !value.isEmpty());
    isValid = predicate.test(value);

    label.setForeground(isDirty && !isValid ? COLOR_NOT_VALID : COLOR_VALID);

    if (isDirty != previousIsDirty || isValid != previousIsValid)
      onChangeCallback.run();
    afterUpdateCallback.run();
  }

  public boolean isValid() {
    return isDirty && isValid;
  }

  public UiFieldValidator onChange(Runnable callback) {
    onChangeCallback = callback;
    return this;
  }

  void afterUpdate(Runnable callback) {
    afterUpdateCallback = callback;
  }

  public String getValue() {
    return field.getText();
  }

  boolean isDirty() {
    return isDirty;
  }
}
