package de.symeda.sormas.ui.campaign.campaigns;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import de.symeda.sormas.ui.utils.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.campaign.diagram.CampaignDashboardElement;
import de.symeda.sormas.api.campaign.diagram.CampaignDiagramDefinitionDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.ui.utils.AbstractEditableGrid;

public class CampaignDashboardElementsGridComponent extends AbstractEditableGrid<CampaignDashboardElement> {

	public CampaignDashboardElementsGridComponent(List<CampaignDashboardElement> savedElements, List<CampaignDashboardElement> allElements) {
		super(savedElements, allElements);
		setWidthFull();
	}

	protected Binder<CampaignDashboardElement> addColumnsBinder(List<CampaignDashboardElement> allElements) {
		Binder<CampaignDashboardElement> binder = new Binder<>();

		final List<CampaignDiagramDefinitionDto> campaignDiagramDefinitionDtos = FacadeProvider.getCampaignDiagramDefinitionFacade().getAll();
		final Map<String, String> diagramIdCaptionMap = campaignDiagramDefinitionDtos.stream()
				.collect(Collectors.toMap(CampaignDiagramDefinitionDto::getDiagramId, CampaignDiagramDefinitionDto::getDiagramCaption));

		// The editor fields were created with a caption (a Captions key), which the Vaadin 8 grid editor did not show; a Flow
		// field shows its label in the editor cell, so the fields here have none
		ComboBox<String> diagramIdCaptionCombo = new ComboBox<>(null, diagramIdCaptionMap.keySet());
		diagramIdCaptionCombo.setItemLabelGenerator(diagramId -> diagramIdCaptionMap.get(diagramId));
		keepValueOnClientClear(diagramIdCaptionCombo);

		Binder.Binding<CampaignDashboardElement, String> diagramIdCaptionBind = binder.bind(
			diagramIdCaptionCombo,
			CampaignDashboardElement::getDiagramId, CampaignDashboardElement::setDiagramId);
			/*cde -> new DiagramIdCaption(cde.getDiagramId(), diagramIdCaptionMap.get(cde.getDiagramId())),
			(campaignDashboardElement, diagramIdCaption) -> {
				campaignDashboardElement.setDiagramId(diagramIdCaption.getDiagramId());
			});*/

		final Grid.Column<CampaignDashboardElement, String> diagramIdColumn =
			grid.addColumn(campaignDashboardElement -> diagramIdCaptionMap.get(campaignDashboardElement.getDiagramId()))
				.setCaption(I18nProperties.getCaption(Captions.campaignDashboardChart));
		diagramIdColumn.setEditorBinding(diagramIdCaptionBind);

		final List<String> existingTabIds = allElements.stream()
			.map(campaignDiagramDefinitionDto -> campaignDiagramDefinitionDto.getTabId())
			.filter(s -> StringUtils.isNotEmpty(s))
			.distinct()
			.collect(Collectors.toList());
		final ComboBox<String> tabIdCombo = new ComboBox<>(null, existingTabIds);

		keepValueOnClientClear(tabIdCombo);
		allowNewItems(tabIdCombo);

		final Binder.Binding<CampaignDashboardElement, String> tabIdBind =
			binder.bind(tabIdCombo, CampaignDashboardElement::getTabId, CampaignDashboardElement::setTabId);
		final Grid.Column<CampaignDashboardElement, String> tabIdColumn =
			grid.addColumn(campaignDashboardElement -> campaignDashboardElement.getTabId())
				.setCaption(I18nProperties.getCaption(Captions.campaignDashboardTabName));
		tabIdColumn.setEditorBinding(tabIdBind);

		final List<String> existingSubTabIds = allElements.stream()
			.map(campaignDiagramDefinitionDto -> campaignDiagramDefinitionDto.getSubTabId())
			.filter(s -> StringUtils.isNotEmpty(s))
			.distinct()
			.collect(Collectors.toList());
		final ComboBox<String> subTabIdCombo = new ComboBox<>(null, existingSubTabIds);

		// was setEmptySelectionAllowed(true): the value can be cleared
		subTabIdCombo.setClearButtonVisible(true);
		allowNewItems(subTabIdCombo);

		final Binder.Binding<CampaignDashboardElement, String> subTabIdBind =
			binder.bind(subTabIdCombo, CampaignDashboardElement::getSubTabId, CampaignDashboardElement::setSubTabId);
		final Grid.Column<CampaignDashboardElement, String> subTabIdColumn =
			grid.addColumn(campaignDashboardElement -> campaignDashboardElement.getSubTabId())
				.setCaption(I18nProperties.getCaption(Captions.campaignDashboardSubTabName));
		subTabIdColumn.setEditorBinding(subTabIdBind);

		TextField width = new TextField();
		Binder.Binding<CampaignDashboardElement, String> widthBind = binder.forField(width)
			.withValidator(percentValidator(), I18nProperties.getValidationError(Validations.campaignDashboardChartPercentage))
			.bind(campaignDashboardElement -> intToString(campaignDashboardElement.getWidth()), (c, s) -> c.setWidth(new Integer(s)));
		Grid.Column<CampaignDashboardElement, String> widthColumn =
			grid.addColumn(campaignDashboardElement -> intToString(campaignDashboardElement.getWidth()))
				.setCaption(I18nProperties.getCaption(Captions.campaignDashboardChartWidth));
		widthColumn.setEditorBinding(widthBind);

		TextField height = new TextField();
		Binder.Binding<CampaignDashboardElement, String> heightBind = binder.forField(height)
			.withValidator(percentValidator(), I18nProperties.getValidationError(Validations.campaignDashboardChartPercentage))
			.bind(campaignDashboardElement -> intToString(campaignDashboardElement.getHeight()), (c, s) -> c.setHeight(new Integer(s)));
		Grid.Column<CampaignDashboardElement, String> heightColumn =
			grid.addColumn(campaignDashboardElement -> intToString(campaignDashboardElement.getHeight()))
				.setCaption(I18nProperties.getCaption(Captions.campaignDashboardChartHeight));
		heightColumn.setEditorBinding(heightBind);

		TextField order = new TextField();
		order.setEnabled(false);
		Binder.Binding<CampaignDashboardElement, String> orderBind =
			binder.bind(order, campaignDashboardElement -> intToString(campaignDashboardElement.getOrder()), (c, s) -> c.setOrder(new Integer(s)));
		Grid.Column<CampaignDashboardElement, String> orderColumn =
			grid.addColumn(campaignDashboardElement -> intToString(campaignDashboardElement.getOrder()))
				.setCaption(I18nProperties.getCaption(Captions.campaignDashboardOrder));
		orderColumn.setEditorBinding(orderBind);
		return binder;
	}

	protected ComponentEventListener<ClickEvent<Button>> newRowEvent() {
		return event -> {
			final CampaignDashboardElement campaignDashboardElement = new CampaignDashboardElement();
			final ArrayList<CampaignDashboardElement> gridItems = getItems();
			gridItems.add(campaignDashboardElement);
			campaignDashboardElement.setOrder(gridItems.indexOf(campaignDashboardElement));
			grid.setItems(gridItems);
		};
	}

	/**
	 * Was {@code setEmptySelectionAllowed(false)}: a value the user clears is restored.
	 */
	private static void keepValueOnClientClear(ComboBox<String> comboBox) {
		comboBox.addValueChangeListener(e -> {
			if (e.isFromClient() && e.getValue() == null && e.getOldValue() != null) {
				comboBox.setValue(e.getOldValue());
			}
		});
	}

	/**
	 * Was {@code setTextInputAllowed(true)} with a new item provider returning the typed text: the typed text becomes the
	 * value (it is not added to the items, as in Vaadin 8).
	 */
	private static void allowNewItems(ComboBox<String> comboBox) {
		comboBox.setAllowCustomValue(true);
		comboBox.addCustomValueSetListener(e -> comboBox.setValue(e.getDetail()));
	}

	private SerializablePredicate<String> percentValidator() {
		return s -> new Integer(s) % 5 == 0;
	}

	private String intToString(Integer h) {
		return h != null ? h.toString() : StringUtils.EMPTY;
	}

	protected String getHeaderString() {
		return Strings.headingCampaignDashboard;
	}

	protected String getAdditionalRowCaption() {
		return Captions.campaignAdditionalChart;
	}

	protected void reorderGrid() {
		final ArrayList<CampaignDashboardElement> gridItems = getItems();
		gridItems.forEach(campaignDashboardElement -> campaignDashboardElement.setOrder(gridItems.indexOf(campaignDashboardElement)));
	}

	/*public static class DiagramIdCaption implements Serializable {

		private String diagramId;
		private String diagramCaption;

		public DiagramIdCaption(String diagramId, String diagramCaption) {
			this.diagramId = diagramId;
			this.diagramCaption = diagramCaption;
		}

		public String getDiagramId() {
			return diagramId;
		}

		public void setDiagramId(String diagramId) {
			this.diagramId = diagramId;
		}

		public String getDiagramCaption() {
			return diagramCaption;
		}

		public void setDiagramCaption(String diagramCaption) {
			this.diagramCaption = diagramCaption;
		}

		@Override
		public String toString() {
			return this.diagramCaption;
		}
	}*/

}
