package de.symeda.sormas.ui.contact;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.UI;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseReferenceDto;
import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.contact.ContactIndexDto;
import de.symeda.sormas.api.contact.ContactSimilarityCriteria;
import de.symeda.sormas.api.contact.SimilarContactDto;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.ui.utils.V7UuidRenderer;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.HeightMode;
import de.symeda.sormas.ui.utils.grid.renderers.ConvertingHtmlRenderer;
import de.symeda.sormas.ui.utils.grid.renderers.DateRenderer;
import de.symeda.sormas.ui.utils.grid.renderers.TextRenderer;

public class ContactSelectionGrid extends Grid<SimilarContactDto> {

	private static final long serialVersionUID = 1766319542062167849L;

	public ContactSelectionGrid(ContactSimilarityCriteria criteria) {
		super(SimilarContactDto.class);
		buildGrid();
		loadData(criteria);
	}

	public ContactSelectionGrid(List<SimilarContactDto> contacts) {
		super(SimilarContactDto.class);
		buildGrid();
		setContainerData(contacts);
	}

	private void buildGrid() {
		setSizeFull();
		setSelectionMode(SelectionMode.SINGLE);
		setHeightMode(HeightMode.ROW);

		setColumns();
	}

	protected void setColumns() {
		setColumns(
			SimilarContactDto.UUID,
			SimilarContactDto.FIRST_NAME,
			SimilarContactDto.LAST_NAME,
			SimilarContactDto.CAZE,
			SimilarContactDto.CASE_ID_EXTERNAL_SYSTEM,
			SimilarContactDto.LAST_CONTACT_DATE,
			SimilarContactDto.CONTACT_PROXIMITIES,
			SimilarContactDto.CONTACT_CLASSIFICATION,
			SimilarContactDto.CONTACT_STATUS,
			SimilarContactDto.FOLLOW_UP_STATUS);

		for (Column<SimilarContactDto, ?> column : getAllColumns()) {
			column.setHeaderCaption(
				I18nProperties.findPrefixCaption(
					column.getPropertyId().toString(),
					SimilarContactDto.I18N_PREFIX,
					ContactIndexDto.I18N_PREFIX,
					ContactDto.I18N_PREFIX));
		}

		getColumn(SimilarContactDto.UUID).setRenderer(new V7UuidRenderer());
		setV7ColumnRenderers();
		getColumn(SimilarContactDto.FIRST_NAME).setMinimumWidth(150);
		getColumn(SimilarContactDto.LAST_NAME).setMinimumWidth(150);
	}

	/**
	 * The Vaadin 7 grid showed these property values through the session's default converters: the case as its caption,
	 * the date in the medium date-time format of the UI locale (V7 {@code StringToDateConverter}).
	 */
	@SuppressWarnings("unchecked")
	protected void setV7ColumnRenderers() {
		((Column<SimilarContactDto, CaseReferenceDto>) getColumn(SimilarContactDto.CAZE))
			.setRenderer(ConvertingHtmlRenderer::toPresentation, new TextRenderer());
		UI ui = UI.getCurrent();
		Locale locale = ui != null ? ui.getLocale() : Locale.getDefault();
		((Column<SimilarContactDto, Date>) getColumn(SimilarContactDto.LAST_CONTACT_DATE))
			.setRenderer(new DateRenderer(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, locale)));
	}

	private void loadData(ContactSimilarityCriteria criteria) {
		final List<SimilarContactDto> similarContacts = FacadeProvider.getContactFacade().getMatchingContacts(criteria);
		setContainerData(similarContacts);
	}

	private void setContainerData(List<SimilarContactDto> similarContacts) {
		setItems(similarContacts);
		setHeightByRows(similarContacts.size() > 0 ? (similarContacts.size() <= 10 ? similarContacts.size() : 10) : 1);
	}
}
