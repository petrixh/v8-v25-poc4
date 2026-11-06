package de.symeda.sormas.ui.clinicalcourse;

import java.util.Date;
import java.util.List;

import com.vaadin.flow.component.icon.VaadinIcon;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.Language;
import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.clinicalcourse.ClinicalVisitCriteria;
import de.symeda.sormas.api.clinicalcourse.ClinicalVisitIndexDto;
import de.symeda.sormas.api.i18n.I18nProperties;
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
public class ClinicalVisitGrid extends Grid<ClinicalVisitIndexDto> implements V7AbstractGrid<ClinicalVisitCriteria> {

	private static final String ACTION_BTN_ID = "action";

	private ClinicalVisitCriteria clinicalVisitCriteria = new ClinicalVisitCriteria();

	@SuppressWarnings("unchecked")
	public ClinicalVisitGrid(CaseReferenceDto caseRef, boolean isPseudonymized, boolean isEditAllowed, boolean isDeleteAllowed) {

		super(ClinicalVisitIndexDto.class);
		setSizeFull();

		if (UiUtil.permitted(isEditAllowed, UserRight.PERFORM_BULK_OPERATIONS)) {
			setSelectionMode(SelectionMode.MULTI);
		} else {
			setSelectionMode(SelectionMode.NONE);
		}

		VaadinUiUtil.addIconColumn(this, ACTION_BTN_ID, isEditAllowed ? VaadinIcon.EDIT : VaadinIcon.EYE);

		setColumns(
			ACTION_BTN_ID,
			ClinicalVisitIndexDto.VISIT_DATE_TIME,
			ClinicalVisitIndexDto.VISITING_PERSON,
			ClinicalVisitIndexDto.TEMPERATURE,
			ClinicalVisitIndexDto.BLOOD_PRESSURE,
			ClinicalVisitIndexDto.HEART_RATE,
			ClinicalVisitIndexDto.VISIT_REMARKS);

		VaadinUiUtil.setupActionColumn(getColumn(ACTION_BTN_ID));

		Language userLanguage = I18nProperties.getUserLanguage();
		((Column<ClinicalVisitIndexDto, Date>) getColumn(ClinicalVisitIndexDto.VISIT_DATE_TIME))
			.setRenderer(new DateRenderer(DateHelper.getLocalDateTimeFormat(userLanguage)));

		for (Column<ClinicalVisitIndexDto, ?> column : getAllColumns()) {
			column.setHeaderCaption(
				I18nProperties.getPrefixCaption(ClinicalVisitIndexDto.I18N_PREFIX, column.getPropertyId().toString(), column.getHeaderCaption()));
		}

		setCellStyleGenerator(
			FieldAccessCellStyleGenerator.withFieldAccessCheckers(
				ClinicalVisitIndexDto.class,
				UiFieldAccessCheckers.forSensitiveData(isPseudonymized, FacadeProvider.getConfigFacade().getCountryLocale())));

		addItemClickListener(e -> {
			if ((e.getColumn() != null && ACTION_BTN_ID.equals(e.getColumn().getKey())) || e.getClickCount() > 1) {
				ControllerProvider.getClinicalCourseController()
					.openClinicalVisitEditForm(e.getItem(), caseRef.getUuid(), this::reload, isEditAllowed, isDeleteAllowed);
			}
		});
	}

	public void reload() {

		if (isUserSelectionAllowed()) {
			deselectAll();
		}

		List<ClinicalVisitIndexDto> entries = FacadeProvider.getClinicalVisitFacade().getIndexList(clinicalVisitCriteria, null, null, null);
		setItems(entries);
		setHeightByRows(Math.max(1, Math.min(entries.size(), 10)));
	}

	@Override
	public void setCriteria(ClinicalVisitCriteria clinicalVisitCriteria) {
		this.clinicalVisitCriteria = clinicalVisitCriteria;
	}

	@Override
	public ClinicalVisitCriteria getCriteria() {
		return clinicalVisitCriteria;
	}
}
