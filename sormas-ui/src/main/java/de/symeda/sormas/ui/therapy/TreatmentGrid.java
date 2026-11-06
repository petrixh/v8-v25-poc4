package de.symeda.sormas.ui.therapy;

import java.util.Date;
import java.util.List;

import com.vaadin.flow.component.icon.VaadinIcon;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.therapy.TreatmentCriteria;
import de.symeda.sormas.api.therapy.TreatmentIndexDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.utils.fieldaccess.UiFieldAccessCheckers;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.FieldAccessCellStyleGenerator;
import de.symeda.sormas.ui.utils.V7AbstractGrid;
import de.symeda.sormas.ui.utils.VaadinUiUtil;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.renderers.DateRenderer;

@SuppressWarnings("serial")
public class TreatmentGrid extends Grid<TreatmentIndexDto> implements V7AbstractGrid<TreatmentCriteria> {

	private static final String ACTION_BTN_ID = "edit";

	private TreatmentCriteria treatmentCriteria = new TreatmentCriteria();

	@SuppressWarnings("unchecked")
	public TreatmentGrid(
		boolean isPseudonymized,
		boolean isEditAllowed,
		boolean isDeleteAllowed,
		boolean isPrescriptionEditAllowed,
		boolean isPrescriptionDeleteAllowed) {

		super(TreatmentIndexDto.class);
		setSizeFull();

		if (UiUtil.permitted(isEditAllowed, UserRight.PERFORM_BULK_OPERATIONS)) {
			setSelectionMode(SelectionMode.MULTI);
		} else {
			setSelectionMode(SelectionMode.NONE);
		}

		VaadinUiUtil.addIconColumn(this, ACTION_BTN_ID, isEditAllowed ? VaadinIcon.EDIT : VaadinIcon.EYE);

		setColumns(
			ACTION_BTN_ID,
			TreatmentIndexDto.TREATMENT_TYPE,
			TreatmentIndexDto.TREATMENT_DATE_TIME,
			TreatmentIndexDto.DOSE,
			TreatmentIndexDto.TREATMENT_ROUTE,
			TreatmentIndexDto.EXECUTING_CLINICIAN);

		VaadinUiUtil.setupActionColumn(getColumn(ACTION_BTN_ID));

		Language userLanguage = I18nProperties.getUserLanguage();
		((Column<TreatmentIndexDto, Date>) getColumn(TreatmentIndexDto.TREATMENT_DATE_TIME))
			.setRenderer(new DateRenderer(DateHelper.getLocalDateTimeFormat(userLanguage)));

		for (Column<TreatmentIndexDto, ?> column : getAllColumns()) {
			column.setHeaderCaption(
				I18nProperties.getPrefixCaption(TreatmentIndexDto.I18N_PREFIX, column.getPropertyId().toString(), column.getHeaderCaption()));
			setCellStyleGenerator(
				FieldAccessCellStyleGenerator.withFieldAccessCheckers(
					TreatmentIndexDto.class,
					UiFieldAccessCheckers.forSensitiveData(isPseudonymized, FacadeProvider.getConfigFacade().getCountryLocale())));
		}

		addItemClickListener(e -> {
			if (e.getColumn() == null) {
				return;
			}

			if (ACTION_BTN_ID.equals(e.getColumn().getKey()) || e.getClickCount() > 1) {
				ControllerProvider.getTherapyController()
					.openTreatmentEditForm(
						e.getItem(),
						this::reload,
						isEditAllowed,
						isDeleteAllowed,
						isPrescriptionEditAllowed,
						isPrescriptionDeleteAllowed);
			}
		});
	}

	public void reload() {

		if (isUserSelectionAllowed()) {
			deselectAll();
		}

		List<TreatmentIndexDto> entries = FacadeProvider.getTreatmentFacade().getIndexList(treatmentCriteria);
		setItems(entries);
	}

	@Override
	public void setCriteria(TreatmentCriteria treatmentCriteria) {
		this.treatmentCriteria = treatmentCriteria;
	}

	@Override
	public TreatmentCriteria getCriteria() {
		return treatmentCriteria;
	}
}
