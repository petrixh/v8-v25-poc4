package de.symeda.sormas.ui.location;

import de.symeda.sormas.ui.utils.fields.TextField;

/**
 * Was a Vaadin 7 text field that made {@code getState()} public, so callers could read its "modified" flag. Flow fields
 * have no shared state: callers use {@code isModified()} (the same flag). The class stays as the type callers check for.
 */
public class AccessibleTextField extends TextField {
}
