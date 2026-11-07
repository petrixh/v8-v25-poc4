/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2026 SORMAS Foundation gGmbH
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package de.symeda.sormas.ui.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;

/**
 * Replaces the Vaadin 8 {@code com.vaadin.ui.CustomLayout}: an HTML template in which components are placed into the
 * elements that have a {@code location} attribute (the "slots"). SORMAS forms build these templates as strings
 * ({@code LayoutUtil.loc(..)}, {@code fluidRowLocs(..)}, ...).
 * <p>
 * The template is parsed on the server (jsoup, as Flow's {@code Html} component does) and rebuilt as a tree of Flow
 * {@link Element}s, so components can be appended to the slot elements with the public Element API. As in Vaadin 8, the
 * content of a slot element is replaced by the component, a component added to an occupied location replaces the old
 * one, and a component whose location is not in the template stays a child of the layout but is not shown (it is kept as
 * a virtual child).
 */
@Tag("div")
public class CustomLayout extends Component implements HasSize, HasStyle {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(CustomLayout.class);

	/**
	 * The attribute that marks a slot, as in Vaadin 8.
	 */
	public static final String LOCATION_ATTRIBUTE = "location";

	private String templateContents;

	private final Map<String, Element> slotElements = new HashMap<>();

	/**
	 * Component by location, in the order they were added (the Vaadin 8 iteration order).
	 */
	private final Map<String, Component> slots = new LinkedHashMap<>();

	public CustomLayout() {
	}

	public CustomLayout(String templateContents) {
		setTemplateContents(templateContents);
	}

	/**
	 * Sets the HTML template. Components already added are moved into the slots of the new template.
	 */
	public void setTemplateContents(String templateContents) {

		this.templateContents = templateContents;

		// take the components out before the old template elements are dropped
		for (Component component : slots.values()) {
			detachFromSlot(component);
		}

		getElement().removeAllChildren();
		slotElements.clear();

		if (templateContents != null && !templateContents.isEmpty()) {
			org.jsoup.nodes.Element body = Jsoup.parseBodyFragment(templateContents).body();
			for (Node node : body.childNodes()) {
				Element child = toElement(node);
				if (child != null) {
					getElement().appendChild(child);
				}
			}
		}

		for (Map.Entry<String, Component> entry : slots.entrySet()) {
			attachToSlot(entry.getValue(), entry.getKey());
		}
	}

	public String getTemplateContents() {
		return templateContents;
	}

	private Element toElement(Node node) {

		if (node instanceof TextNode) {
			return Element.createText(((TextNode) node).getWholeText());
		}
		if (node instanceof DataNode) {
			return Element.createText(((DataNode) node).getWholeData());
		}
		if (!(node instanceof org.jsoup.nodes.Element)) {
			// comments, doctype
			return null;
		}

		org.jsoup.nodes.Element source = (org.jsoup.nodes.Element) node;
		Element element = new Element(source.tagName());
		for (Attribute attribute : source.attributes()) {
			try {
				element.setAttribute(attribute.getKey(), attribute.getValue());
			} catch (IllegalArgumentException e) {
				logger.warn("Template attribute {}=\"{}\" ignored: {}", attribute.getKey(), attribute.getValue(), e.getMessage());
			}
		}

		String location = source.attr(LOCATION_ATTRIBUTE);
		if (source.hasAttr(LOCATION_ATTRIBUTE)) {
			// as in Vaadin 8, the content of a slot is replaced by its component; a location that is in the template more
			// than once is the last of its slots (Vaadin 8 VCustomLayout.scanForLocations), the others stay empty
			slotElements.put(location, element);
			return element;
		}

		for (Node childNode : source.childNodes()) {
			Element child = toElement(childNode);
			if (child != null) {
				element.appendChild(child);
			}
		}
		return element;
	}

	/**
	 * Adds the component into the slot with the given location. A component already in that location is removed.
	 */
	public void addComponent(Component component, String location) {

		Objects.requireNonNull(component, "component");
		Objects.requireNonNull(location, "location");

		Component old = slots.get(location);
		if (old == component) {
			return;
		}
		if (old != null) {
			removeComponent(old);
		}

		// a component moved to another location of this layout
		String oldLocation = getPosition(component);
		if (oldLocation != null) {
			slots.remove(oldLocation);
		}

		detachFromSlot(component);
		slots.put(location, component);
		attachToSlot(component, location);
	}

	/**
	 * Vaadin 8: adds the component to the location {@code ""}.
	 */
	public void addComponent(Component component) {
		addComponent(component, "");
	}

	private void attachToSlot(Component component, String location) {

		Element slot = slotElements.get(location);
		Element componentElement = component.getElement();
		if (componentElement.getParent() != null) {
			componentElement.removeFromParent();
		}
		if (slot != null) {
			slot.removeAllChildren();
			slot.appendChild(componentElement);
		} else {
			// not in the template: a child that is not shown, as in Vaadin 8
			getElement().appendVirtualChild(componentElement);
		}
	}

	private void detachFromSlot(Component component) {

		Element componentElement = component.getElement();
		if (componentElement.getParent() == null) {
			return;
		}
		if (componentElement.isVirtualChild()) {
			componentElement.getParent().removeVirtualChild(componentElement);
		} else {
			componentElement.removeFromParent();
		}
	}

	public Component getComponent(String location) {
		return slots.get(location);
	}

	/**
	 * @return the location of the component, or {@code null} if it is not in this layout
	 */
	public String getPosition(Component component) {
		for (Map.Entry<String, Component> entry : slots.entrySet()) {
			if (entry.getValue() == component) {
				return entry.getKey();
			}
		}
		return null;
	}

	public void removeComponent(String location) {
		Component component = slots.get(location);
		if (component != null) {
			removeComponent(component);
		}
	}

	public void removeComponent(Component component) {
		String location = getPosition(component);
		if (location != null) {
			slots.remove(location);
			detachFromSlot(component);
		}
	}

	public void removeAllComponents() {
		for (Component component : new ArrayList<>(slots.values())) {
			removeComponent(component);
		}
	}

	/**
	 * Swaps the locations of two components, or puts the new component in the old one's location.
	 */
	public void replaceComponent(Component oldComponent, Component newComponent) {

		String oldLocation = getPosition(oldComponent);
		String newLocation = getPosition(newComponent);
		if (oldLocation == null) {
			if (newLocation == null) {
				throw new IllegalArgumentException("Neither component is in this layout");
			}
			addComponent(oldComponent, newLocation);
		} else if (newLocation == null) {
			addComponent(newComponent, oldLocation);
		} else {
			removeComponent(oldComponent);
			removeComponent(newComponent);
			addComponent(oldComponent, newLocation);
			addComponent(newComponent, oldLocation);
		}
	}

	/**
	 * @return whether the template has a slot for the location
	 */
	public boolean hasLocation(String location) {
		return slotElements.containsKey(location);
	}

	/**
	 * @return the slot element of the location, or {@code null}
	 */
	public Element getSlotElement(String location) {
		return slotElements.get(location);
	}

	public int getComponentCount() {
		return slots.size();
	}

	public Iterator<Component> iterator() {
		return new ArrayList<>(slots.values()).iterator();
	}

	/**
	 * The components of the layout, in the order they were added (also those whose location is not in the template).
	 */
	@Override
	public Stream<Component> getChildren() {
		List<Component> children = new ArrayList<>(slots.values());
		return children.stream();
	}

	// Vaadin 8 size conveniences used by the forms

	public void setWidthUndefined() {
		setWidth(null);
	}

	public void setHeightUndefined() {
		setHeight(null);
	}

	/**
	 * Vaadin 8 style names: added as class names.
	 */
	public void addStyleName(String styleName) {
		if (styleName != null) {
			Stream.of(styleName.trim().split("\\s+")).filter(s -> !s.isEmpty()).forEach(this::addClassName);
		}
	}

	public void removeStyleName(String styleName) {
		if (styleName != null) {
			Stream.of(styleName.trim().split("\\s+")).filter(s -> !s.isEmpty()).forEach(this::removeClassName);
		}
	}
}
