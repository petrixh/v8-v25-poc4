package de.symeda.sormas.ui.utils;

import java.util.HashMap;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.EditPermissionType;
import de.symeda.sormas.ui.caze.CaseDataView;
import de.symeda.sormas.ui.document.DocumentListComponent;
import de.symeda.sormas.ui.utils.components.sidecomponent.SideComponentLayout;

/**
 * The edit component on the left (8 of 12 columns in the old HTML template), a side panel on the right (4 of 12) whose
 * components are stacked in the order of their locations. Was a {@code CustomLayout} with that template; the slots are
 * now plain Flow layouts, the responsive column widths are theme work.
 */
public class LayoutWithSidePanel extends HorizontalLayout {

	private static final String CONTENT_COMPONENT = "contentComponent";
	private static final String SIDE_PANEL = "sidePanel";

	private final VerticalLayout sidePanel;
	private final Map<String, Div> sidePanelSlots = new HashMap<>();
	private final CommitDiscardWrapperComponent<?> editComponent;
	private Map<String, Component> sideComponents = new HashMap<>();

	public LayoutWithSidePanel(CommitDiscardWrapperComponent editComponent, String... sideComponentLocs) {
		this.editComponent = editComponent;

		addClassName(CssStyles.ROOT_COMPONENT);
		setWidthFull();
		setHeight(null);
		setPadding(false);

		editComponent.setPadding(false);
		editComponent.setWidthFull();
		if (editComponent.getWrappedComponent() instanceof HasSize) {
			((HasSize) editComponent.getWrappedComponent()).setWidthFull();
		}
		editComponent.addClassName(CssStyles.MAIN_COMPONENT);

		// was an HTML template: fluid row, edit component in slot CONTENT_COMPONENT (8 columns), side panel in SIDE_PANEL (4)
		add(editComponent);
		setFlexGrow(8, editComponent);

		sidePanel = new VerticalLayout();
		sidePanel.setPadding(false);
		for (String loc : sideComponentLocs) {
			Div slot = new Div();
			slot.setWidthFull();
			sidePanelSlots.put(loc, slot);
			sidePanel.add(slot);
		}
		add(sidePanel);
		setFlexGrow(4, sidePanel);
		// was a fluid bootstrap row: the edit component and the side panel keep their own heights (no stretch to the taller
		// one), so the Save / Discard footer of a short form follows the form
		setAlignItems(Alignment.START);
	}

	public void addSidePanelComponent(Component component, String loc) {
		Div slot = sidePanelSlots.get(loc);
		if (slot != null) {
			slot.removeAll();
			slot.add(component);
		}
		sideComponents.put(loc, component);
	}

	public VerticalLayout getSidePanelComponent() {
		return sidePanel;
	}

	public void disable() {
		disableWithViewAllow();
		sidePanel.setEnabled(false);
	}

	//excludeButtons: represent the buttons from the CommitDiscardComponent that we intend to exclude from disabling
	public void disableWithViewAllow() {
		editComponent.setNonEditable();
	}

	//excludeButtons: represent the buttons from the CommitDiscardComponent that we intend to exclude from disabling
	public void disable(boolean excludePresentDocuments, String... excludedButtons) {
		editComponent.setEditable(false, excludedButtons);
		if (excludePresentDocuments) {
			sideComponents.keySet().stream().forEach(loc -> {
				if (!loc.equals(CaseDataView.DOCUMENTS_LOC)) {
					setEnabled(sideComponents.get(loc), false);
				} else {
					((DocumentListComponent) ((SideComponentLayout) sideComponents.get(loc)).getComponent()).getMainButton().setEnabled(false);
				}
			});
		} else {
			sidePanel.setEnabled(false);
		}
	}

	public void disableIfNecessary(boolean deleted, EditPermissionType editAllowed) {
		if (deleted) {
			editComponent.addToActiveButtonsList(CommitDiscardWrapperComponent.DELETE_RESTORE);
			disable();
		} else if (editAllowed != null) {
			disableBasedOnPermissionTypes(editAllowed);
		}
	}

	public void disableBasedOnPermissionTypes(EditPermissionType editAllowed) {
		if (editAllowed.equals(EditPermissionType.ARCHIVING_STATUS_ONLY)) {
			editComponent.addToActiveButtonsList(ArchivingController.ARCHIVE_DEARCHIVE_BUTTON_ID);
			disableWithViewAllow();
		} else if (editAllowed.equals(EditPermissionType.OUTSIDE_JURISDICTION) || editAllowed.equals(EditPermissionType.REFUSED)) {
			disableWithViewAllow();
		} else if (editAllowed.equals(EditPermissionType.WITHOUT_OWNERSHIP)) {
			editComponent.addToActiveButtonsList(CommitDiscardWrapperComponent.DELETE_RESTORE);
			disableWithViewAllow();
		}
	}

	private static void setEnabled(Component component, boolean enabled) {
		if (component instanceof HasEnabled) {
			((HasEnabled) component).setEnabled(enabled);
		} else {
			component.getElement().setEnabled(enabled);
		}
	}
}
