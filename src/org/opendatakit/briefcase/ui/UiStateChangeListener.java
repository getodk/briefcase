package org.opendatakit.briefcase.ui;

/** Listens for changes that may affect which UI controls should be active. */
public interface UiStateChangeListener {
    void setFullUIEnabled(boolean enabled);
}
