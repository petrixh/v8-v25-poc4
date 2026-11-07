package de.symeda.sormas.ui.therapy;

import java.util.Date;
import java.util.List;

import com.vaadin.flow.component.icon.VaadinIcon;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.therapy.PeriodDto;
import de.symeda.sormas.api.therapy.PrescriptionCriteria;
import de.symeda.sormas.api.therapy.PrescriptionDto;
import de.symeda.sormas.api.therapy.PrescriptionIndexDto;
import de.symeda.sormas.api.therapy.TherapyDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.FieldAccessCellStyleGenerator;
import de.symeda.sormas.ui.utils.GridButtonRenderer;
import de.symeda.sormas.ui.utils.PeriodDtoConverter;
import de.symeda.sormas.ui.utils.V7AbstractGrid;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.renderers.DateRenderer;
import de.symeda.sormas.ui.utils.grid.renderers.TextRenderer;

@SuppressWarnings("serial")
public class PrescriptionGrid extends Grid<PrescriptionIndexDto> implements V7AbstractGrid<PrescriptionCriteria> {

	private static final String ACTION_BTN_ID = "edit";
	private static final String DOCUMENT_TREATMENT_BTN_ID = "documentTreatment";

	public static final String MIN_ROW_HEIGHT_STYLE_NAME = "min-row-height";

	private PrescriptionCriteria prescriptionCriteria = new PrescriptionCriteria();

	@SuppressWarnings("unchecked")
	public PrescriptionGrid(TherapyForm parentView, boolean isPseudonymized, boolean isEditAllowed, boolean isDeleteAllowed) {

		super(PrescriptionIndexDto.class);
		setSizeFull();

		if (UiUtil.permitted(isEditAllowed, UserRight.PERFORM_BULK_OPERATIONS)) {
			setSelectionMode(SelectionMode.MULTI);
		} else {
			setSelectionMode(SelectionMode.NONE);
		}

		VaadinUiUtil.addIconColumn(this, ACTION_BTN_ID, isEditAllowed ? VaadinIcon.EDIT : VaadinIcon.EYE);

		// was a generated container property
		addColumn(prescription -> I18nProperties.getPrefixCaption(TherapyDto.I18N_PREFIX, I18nProperties.getCaption(Captions.treatmentCreateTreatment)))
			.setKey(DOCUMENT_TREATMENT_BTN_ID);

		setColumns(
			ACTION_BTN_ID,
			PrescriptionIndexDto.PRESCRIPTION_TYPE,
			PrescriptionIndexDto.PRESCRIPTION_DATE,
			PrescriptionIndexDto.PRESCRIPTION_PERIOD,
			PrescriptionIndexDto.FREQUENCY,
			PrescriptionIndexDto.DOSE,
			PrescriptionIndexDto.PRESCRIPTION_ROUTE,
			PrescriptionIndexDto.PRESCRIBING_CLINICIAN,
			DOCUMENT_TREATMENT_BTN_ID);

		VaadinUiUtil.setupActionColumn(getColumn(ACTION_BTN_ID));

		if (!isPseudonymized) {
			getColumn(DOCUMENT_TREATMENT_BTN_ID).setRenderer(new GridButtonRenderer()).setMinimumWidth(180);
			getColumn(DOCUMENT_TREATMENT_BTN_ID).setHeaderCaption("");
		}
		getColumn(DOCUMENT_TREATMENT_BTN_ID).setHidden(isPseudonymized || !isEditAllowed || !UiUtil.permitted(UserRight.TREATMENT_CREATE));

		((Column<PrescriptionIndexDto, Date>) getColumn(PrescriptionIndexDto.PRESCRIPTION_DATE))
			.setRenderer(new DateRenderer(DateFormatHelper.getDateFormat()));
		// was setConverter(new PeriodDtoConverter()): the period shows as the converter's text
		PeriodDtoConverter periodDtoConverter = new PeriodDtoConverter();
		((Column<PrescriptionIndexDto, PeriodDto>) getColumn(PrescriptionIndexDto.PRESCRIPTION_PERIOD))
			.setRenderer(period -> periodDtoConverter.convertToPresentation(period, null), new TextRenderer());

		for (Column<PrescriptionIndexDto, ?> column : getAllColumns()) {
			column.setHeaderCaption(
				I18nProperties.getPrefixCaption(PrescriptionIndexDto.I18N_PREFIX, column.getPropertyId().toString(), column.getHeaderCaption()));
			setCellStyleGenerator(
				FieldAccessCellStyleGenerator.withFieldAccessCheckers(
					PrescriptionIndexDto.class,
					UiFieldAccessCheckers.forSensitiveData(isPseudonymized, FacadeProvider.getConfigFacade().getCountryLocale())));
		}

		addItemClickListener(e -> {
			if (e.getColumn() == null) {
				return;
			}

			if (DOCUMENT_TREATMENT_BTN_ID.equals(e.getColumn().getKey())) {
				PrescriptionDto prescription = FacadeProvider.getPrescriptionFacade().getPrescriptionByUuid(e.getItem().getUuid());
				ControllerProvider.getTherapyController().openTreatmentCreateForm(prescription, (Runnable) () -> parentView.reloadTreatmentGrid());
			} else if (ACTION_BTN_ID.equals(e.getColumn().getKey()) || e.getClickCount() > 1) {
				ControllerProvider.getTherapyController().openPrescriptionEditForm(e.getItem(), this::reload, isEditAllowed, isDeleteAllowed);
			}
		});

		// without this, the first time new elements are added they will not be correctly displayed as the row is not high enough.
		addClassName(MIN_ROW_HEIGHT_STYLE_NAME);
	}

	public void reload() {

		if (isUserSelectionAllowed()) {
			deselectAll();
		}

		List<PrescriptionIndexDto> entries = FacadeProvider.getPrescriptionFacade().getIndexList(prescriptionCriteria);
		setItems(entries);
	}

	/**
	 * @return the number of rows (was {@code getContainer().size()})
	 */
	public int getItemCount() {
		return getListDataView().getItemCount();
	}

	@Override
	public void setCriteria(PrescriptionCriteria prescriptionCriteria) {
		this.prescriptionCriteria = prescriptionCriteria;
	}

	@Override
	public PrescriptionCriteria getCriteria() {
		return prescriptionCriteria;
	}
}
