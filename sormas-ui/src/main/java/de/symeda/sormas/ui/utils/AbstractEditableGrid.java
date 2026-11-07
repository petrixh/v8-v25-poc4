package de.symeda.sormas.ui.utils;

import static de.symeda.sormas.ui.utils.ButtonHelper.createButton;
import static de.symeda.sormas.ui.utils.CssStyles.H3;
import static de.symeda.sormas.ui.utils.LayoutUtil.loc;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.dnd.GridDragEndEvent;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;

import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.ui.navigation.View;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.HeightMode;
import de.symeda.sormas.ui.utils.grid.renderers.HtmlRenderer;

public abstract class AbstractEditableGrid<T> extends CustomLayout implements View {

	private static final String HEADING_LOC = "headingLoc";
	private static final String GRID_LOC = "gridLoc";
	private static final String ADDITIONAL_ROW_LOC = "additionalRowLoc";

	private static final String HTML_LAYOUT = loc(HEADING_LOC) + loc(GRID_LOC) + loc(ADDITIONAL_ROW_LOC);
	public static final String DELETE = "delete";

	protected Grid<T> grid = new Grid<>();
	protected List<T> savedItems = new ArrayList<>();

	/** The row being dragged (Vaadin 8 {@code GridRowDragger} moved it on drop). */
	private T draggedItem;

	public AbstractEditableGrid(List<T> savedElements, List<T> allElements) {

		setTemplateContents(HTML_LAYOUT);
		setWidthFull();
		setHeightFull();

		final Span headingLabel = new Span(I18nProperties.getString(getHeaderString()));
		headingLabel.addClassName(H3);
		addComponent(headingLabel, HEADING_LOC);

		final HorizontalLayout gridLayout = new HorizontalLayout();
		gridLayout.setPadding(false);
		gridLayout.setSpacing(true);
		gridLayout.setWidthFull();
		addComponent(gridLayout, GRID_LOC);

		gridLayout.add(grid);
		gridLayout.setFlexGrow(1, grid);

		grid.setHeightMode(HeightMode.UNDEFINED);
		grid.setWidthFull();

		savedItems.addAll(savedElements);
		grid.setItems(new ArrayList<>(savedElements));
		reorderGrid();
		grid.setSelectionMode(Grid.SelectionMode.NONE);
		setSizeFull();

		// Vaadin 8 GridRowDragger: rows are reordered by drag and drop within the grid
		grid.setRowsDraggable(true);
		grid.setDropMode(GridDropMode.BETWEEN);
		grid.addDragStartListener(e -> draggedItem = e.getDraggedItems().isEmpty() ? null : e.getDraggedItems().get(0));
		grid.addDropListener(e -> {
			if (draggedItem == null) {
				return;
			}
			final List<T> items = getItems();
			T dropTarget = e.getDropTargetItem().orElse(null);
			if (draggedItem.equals(dropTarget)) {
				return;
			}
			items.remove(draggedItem);
			int index = dropTarget == null
				? items.size()
				: items.indexOf(dropTarget) + (e.getDropLocation() == GridDropLocation.BELOW ? 1 : 0);
			items.add(index, draggedItem);
			grid.getDataProvider().refreshAll();
		});
		grid.addDragEndListener(e -> draggedItem = null);
		grid.addDragEndListener(gridDragEndListener());

		final Binder<T> binder = addColumnsBinder(allElements);

		Grid.Column<T, String> deleteColumn = grid.addColumn(t -> HtmlRenderer.iconHtml(VaadinIcon.TRASH), new HtmlRenderer())
			.setKey(DELETE)
			.setCaption(I18nProperties.getCaption(Captions.remove));
		deleteColumn.setMaximumWidth(50).setStyleGenerator(item -> CssStyles.GRID_CELL_LINK + " " + CssStyles.ALIGN_CENTER);

		grid.getAllColumns().stream().forEach(col -> {
			col.setSortable(false);
		});

		grid.addItemClickListener(e -> {
			if (grid.getEditor().isOpen()) {
				// Vaadin 8: clicks in the open editor row did not reach the grid
				return;
			}
			final List<T> items = getItems();
			int i = items.indexOf(e.getItem());

			if (e.getColumn() != null && DELETE.equals(e.getColumn().getKey())) {
				showGridRowRemoveConfirmation(items, i);
			} else if (i > -1) {
				grid.getEditor().editItem(e.getItem());
			}
		});

		grid.getEditor().setBinder(binder);
		grid.getEditor().setBuffered(true);

		// Vaadin 8's buffered editor had its own Save and Cancel buttons; in Flow they are the delete column's editor
		final Button saveButton = new Button("Save", e -> grid.getEditor().save());
		final Button cancelButton = new Button("Cancel", e -> grid.getEditor().cancel());
		final HorizontalLayout editorButtons = new HorizontalLayout(saveButton, cancelButton);
		editorButtons.setPadding(false);
		deleteColumn.setEditorComponent(editorButtons);

		grid.getEditor().addSaveListener(e -> {
			final List<T> items = getItems();
			grid.setItems(items);
		});

		final HorizontalLayout buttonLayout = new HorizontalLayout();
		buttonLayout.setPadding(false);
		buttonLayout.setSpacing(true);
		buttonLayout.setWidthFull();
		buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		buttonLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
		final Button additionalRow = createButton(getAdditionalRowCaption(), newRowEvent(), CssStyles.BUTTON_LINK);
		buttonLayout.add(additionalRow);
		addComponent(buttonLayout, ADDITIONAL_ROW_LOC);
	}

	protected ComponentEventListener<GridDragEndEvent<T>> gridDragEndListener() {
		return gridDragEndEvent -> reorderGrid();
	}

	public void discardGrid() {
		this.grid.setItems(new ArrayList<>(this.savedItems));
		reorderGrid();
	}

	private void showGridRowRemoveConfirmation(List<T> items, int index) {
		Dialog confirmationPopup = VaadinUiUtil.showConfirmationPopup(
			I18nProperties.getString(Strings.confirmationRemoveGridRowTitle),
			new Span(I18nProperties.getString(Strings.confirmationRemoveGridRowMessage)),
			popupWindow -> {
				ConfirmationComponent confirmationComponent = new ConfirmationComponent(false, null) {

					private static final long serialVersionUID = 3664636750443474734L;

					@Override
					protected void onConfirm() {
						items.remove(index);
						grid.setItems(items);
						popupWindow.close();;
					}

					@Override
					protected void onCancel() {
						popupWindow.close();
					}
				};

				confirmationComponent.getConfirmButton().setText(I18nProperties.getString(Strings.confirmationRemoveGridRowConfirm));
				confirmationComponent.getCancelButton().setText(I18nProperties.getString(Strings.confirmationRemoveGridRowCancel));

				return confirmationComponent;
			},
			400);

		VaadinUiUtil.setClosable(confirmationPopup, true);
	}

	protected abstract void reorderGrid();

	protected abstract String getHeaderString();

	protected abstract String getAdditionalRowCaption();

	protected abstract ComponentEventListener<ClickEvent<Button>> newRowEvent();

	protected abstract Binder<T> addColumnsBinder(List<T> allElements);

	public void setSavedItems(List<T> savedItems) {
		this.savedItems = savedItems;
		this.grid.setItems(new ArrayList<>(savedItems));
	}

	@SuppressWarnings("unchecked")
	public ArrayList<T> getItems() {
		return (ArrayList<T>) ((ListDataProvider<T>) this.grid.getDataProvider()).getItems();
	}
}
