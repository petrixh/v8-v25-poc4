/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2022 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

package de.symeda.sormas.ui.sormastosormas;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;

import de.symeda.sormas.api.EntityDto;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.sormastosormas.share.incoming.SormasToSormasPersonPreview;
import de.symeda.sormas.api.utils.DateFormatHelper;
import de.symeda.sormas.api.utils.LocationHelper;
import de.symeda.sormas.api.utils.pseudonymization.PseudonymizableDto;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.NotificationHelper;
import de.symeda.sormas.ui.utils.ShowDetailsListener;
import de.symeda.sormas.ui.utils.grid.Grid;
import de.symeda.sormas.ui.utils.grid.HeightMode;

public abstract class BasePreviewGrid<T extends PseudonymizableDto> extends Grid<T> {

	private static final long serialVersionUID = -8437095723489884925L;

	private static final String PERSON_NAME = "personName";
	private static final String BIRTH_DATE = "birthdDate";

	public BasePreviewGrid(
		Class<T> beanType,
		String entityCaptionTag,
		Function<String, Boolean> existsUuidFn,
		Consumer<String> navigateToUuid,
		boolean isPendingRequest) {
		super(beanType);

		setSizeFull();
		setSelectionMode(SelectionMode.SINGLE);
		setHeightMode(HeightMode.ROW);

		buildGrid();
		addUuidColumnHandler(entityCaptionTag, existsUuidFn, navigateToUuid, isPendingRequest);
	}

	protected abstract void buildGrid();

	private void addUuidColumnHandler(
		String entityCaptionTag,
		Function<String, Boolean> existsFn,
		Consumer<String> navigateToUuid,
		boolean isPendingRequest) {
		addItemClickListener(new ShowDetailsListener<>(EntityDto.UUID, e -> {
			String uuid = e.getUuid();
			if (existsFn.apply(uuid)) {
				navigateToUuid.accept(uuid);
				fireEvent(new NavigateEvent(this));
			} else if (isPendingRequest) {
				NotificationHelper.show(I18nProperties.getString(Strings.messageAcceptRequestToNavigate), NotificationHelper.Type.TRAY_NOTIFICATION);
			} else {
				NotificationHelper.show(
					String.format(I18nProperties.getString(Strings.messageEntityNotFound), I18nProperties.getCaption(entityCaptionTag).toLowerCase()),
					NotificationHelper.Type.WARNING_MESSAGE);
			}
		}));
	}

	protected List<String> createPersonColumns(Function<T, SormasToSormasPersonPreview> getPerson) {
		addComponentColumn(previewData -> {
			SormasToSormasPersonPreview person = getPerson.apply(previewData);
			if (person.isPseudonymized()) {
				return new Span(I18nProperties.getCaption(Captions.inaccessibleValue));
			}
			return new Span(person.getFirstName() + " " + person.getLastName());
		}).setKey(PERSON_NAME).setStyleGenerator(item -> {
			if (item.isPseudonymized()) {
				return CssStyles.INACCESSIBLE_COLUMN;
			}

			return "";
		});
		addComponentColumn(
			previewData -> new Span(
				DateFormatHelper.formatDate(
					getPerson.apply(previewData).getBirthdateDD(),
					getPerson.apply(previewData).getBirthdateMM(),
					getPerson.apply(previewData).getBirthdateYYYY()))).setKey(BIRTH_DATE);
		addComponentColumn(previewData -> new Span(getPerson.apply(previewData).getSex().toString())).setKey(SormasToSormasPersonPreview.SEX);
		addComponentColumn(previewData -> new Span(LocationHelper.buildLocationString(getPerson.apply(previewData).getAddress())))
			.setKey(SormasToSormasPersonPreview.ADDRESS);

		return Arrays.asList(PERSON_NAME, BIRTH_DATE, SormasToSormasPersonPreview.SEX, SormasToSormasPersonPreview.ADDRESS);
	}

	@Override
	public GridListDataView<T> setItems(Collection<T> items) {
		GridListDataView<T> dataView = super.setItems(items);
		setHeightByRows(items.size() > 0 ? (Math.min(items.size(), 10)) : 1);
		return dataView;
	}

	public Registration addNavigateListener(NavigateListener listener) {
		return addListener(NavigateEvent.class, listener);
	}

	public interface NavigateListener extends ComponentEventListener<NavigateEvent> {

		void onNavigate(NavigateEvent event);

		@Override
		default void onComponentEvent(NavigateEvent event) {
			onNavigate(event);
		}
	}

	public static final class NavigateEvent extends ComponentEvent<BasePreviewGrid<?>> {

		private static final long serialVersionUID = -6258483442328832173L;

		public NavigateEvent(BasePreviewGrid<?> source) {
			super(source, false);
		}
	}
}
