package de.symeda.sormas.ui.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;

import java.util.function.Consumer;

public interface WithChildComponents extends HasElement {

    void forEachComponent(Consumer<Component> componentConsumer);
}
