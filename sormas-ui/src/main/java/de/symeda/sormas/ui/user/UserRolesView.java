package de.symeda.sormas.ui.user;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

import de.symeda.sormas.ui.navigation.ViewChangeListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import de.symeda.sormas.ui.utils.NotificationHelper;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import de.symeda.sormas.ui.utils.fields.ComboBox;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.user.JurisdictionLevel;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.user.UserRoleCriteria;
import de.symeda.sormas.api.user.UserRoleDto;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.ViewModelProviders;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.ComboBoxHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DownloadUtil;
import de.symeda.sormas.ui.utils.ExportEntityName;

public class UserRolesView extends AbstractUserView {

	private static final long serialVersionUID = -3533557348112305469L;

	public static final String VIEW_NAME = ROOT_VIEW_NAME + "/userroles";

	public static final String ENABLED_FILTER = I18nProperties.getString(Strings.enabled);
	public static final String DISABLED_FILTER = I18nProperties.getString(Strings.disabled);
	public static final String ALL_FILTER = I18nProperties.getString(Strings.all);

	private static final String XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private UserRoleCriteria criteria;

	private UserRoleGrid grid;
	private Button createButton;

	private ComboBox userRightsFilter;
	private ComboBox jurisdictionFilter;
	private ComboBox enabledFilter;
	private Checkbox showOnlyRestrictedAccessToAssignedEntities;

	private VerticalLayout gridLayout;

	public UserRolesView() {
		super(VIEW_NAME);

		criteria = ViewModelProviders.of(UserRolesView.class).get(UserRoleCriteria.class);

		grid = new UserRoleGrid();
		grid.setCriteria(criteria);
		gridLayout = new VerticalLayout();
		gridLayout.add(createFilterBar());
		gridLayout.add(createStatusFilterBar());
		gridLayout.add(grid);
		gridLayout.setPadding(true);
		gridLayout.setSpacing(false);
		gridLayout.setSizeFull();
		gridLayout.setFlexGrow(1, grid);
		gridLayout.setClassName("crud-main-layout");

		addComponent(gridLayout);

		Button exportUserRightsButton = ButtonHelper.createIconButton(Captions.exportUserRoles, VaadinIcon.DOWNLOAD, null, CssStyles.BUTTON_PRIMARY);

		// was a FileDownloader extending the button: Flow downloads through a link around it. The download runs
		// without the session lock, so its error notification goes through the UI (was shown directly)
		UI currentUI = UI.getCurrent();
		String exportFileName = createFileNameWithCurrentDate(ExportEntityName.USER_ROLES, ".xlsx");
		addHeaderComponent(DownloadUtil.createDownloadLink(exportUserRightsButton, DownloadHandler.fromInputStream(event -> new DownloadResponse(new DownloadUtil.DelayedInputStream((out) -> {
			try {
				String documentPath = FacadeProvider.getUserRoleFacade().generateUserRolesDocument();
				IOUtils.copy(Files.newInputStream(new File(documentPath).toPath()), out);
			} catch (IOException e) {
				LoggerFactory.getLogger(DownloadUtil.class).error(e.getMessage(), e);
				NotificationHelper.showAsync(
					currentUI,
					I18nProperties.getString(Strings.headingExportUserRightsFailed),
					I18nProperties.getString(Strings.messageUserRightsExportFailed),
					NotificationHelper.Type.ERROR_MESSAGE);
			}
		}, (e) -> {
		}), exportFileName, XLSX_MIME_TYPE, -1))));

		if (UiUtil.permitted(UserRight.USER_ROLE_EDIT)) {
			createButton = ButtonHelper.createIconButton(
				Captions.userRoleNewUserRole,
				VaadinIcon.PLUS_CIRCLE,
				e -> ControllerProvider.getUserRoleController().create(),
				CssStyles.BUTTON_PRIMARY);

			addHeaderComponent(createButton);
		}
	}

	public HorizontalLayout createStatusFilterBar() {
		HorizontalLayout statusFilterLayout = new HorizontalLayout();
		statusFilterLayout.setSpacing(true);
		statusFilterLayout.setPadding(false);
		statusFilterLayout.setWidthFull();
		statusFilterLayout.addClassName(CssStyles.VSPACE_3);

		HorizontalLayout actionButtonsLayout = new HorizontalLayout();
		actionButtonsLayout.setSpacing(true);

		enabledFilter = ComboBoxHelper.createComboBoxV7();
		enabledFilter.setId(UserRoleDto.ENABLED);
		enabledFilter.setWidth(200, Unit.PIXELS);
		enabledFilter.setInputPrompt(I18nProperties.getPrefixCaption(UserRoleDto.I18N_PREFIX, UserRoleDto.ENABLED));
		enabledFilter.addItems(ALL_FILTER, ENABLED_FILTER, DISABLED_FILTER);
		enabledFilter.addValueChangeListener(e -> {
			criteria.enabled(
				ENABLED_FILTER.equals(e.getValue())
					? Boolean.TRUE
					: DISABLED_FILTER.equals(e.getValue()) ? Boolean.FALSE : null);
			navigateTo(criteria);
		});
		actionButtonsLayout.add(enabledFilter);
		statusFilterLayout.add(actionButtonsLayout);
		// was Alignment.TOP_RIGHT: the layout grows, its content goes right
		actionButtonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
		statusFilterLayout.setFlexGrow(1, actionButtonsLayout);

		return statusFilterLayout;
	}

	public HorizontalLayout createFilterBar() {
		HorizontalLayout filterLayout = new HorizontalLayout();
		filterLayout.setPadding(false);
		filterLayout.setSpacing(true);
		filterLayout.setSizeUndefined();

		userRightsFilter = ComboBoxHelper.createComboBoxV7();
		userRightsFilter.setId(UserRoleDto.USER_RIGHTS);
		userRightsFilter.setWidth(200, Unit.PIXELS);
		userRightsFilter.setInputPrompt(I18nProperties.getPrefixCaption(UserRoleDto.I18N_PREFIX, UserRoleDto.USER_RIGHTS));
		userRightsFilter.addItems((Object[]) UserRight.values());
		userRightsFilter.addValueChangeListener(e -> {
			criteria.userRight((UserRight) e.getValue());
			navigateTo(criteria);
		});
		filterLayout.add(userRightsFilter);

		jurisdictionFilter = ComboBoxHelper.createComboBoxV7();
		jurisdictionFilter.setId(UserRoleDto.JURISDICTION_LEVEL);
		jurisdictionFilter.setWidth(200, Unit.PIXELS);
		jurisdictionFilter.setInputPrompt(I18nProperties.getPrefixCaption(UserRoleDto.I18N_PREFIX, UserRoleDto.JURISDICTION_LEVEL));
		jurisdictionFilter.addItems((Object[]) JurisdictionLevel.values());
		jurisdictionFilter.addValueChangeListener(e -> {
			criteria.jurisdictionLevel((JurisdictionLevel) e.getValue());
			navigateTo(criteria);
		});
		filterLayout.add(jurisdictionFilter);

		showOnlyRestrictedAccessToAssignedEntities = new Checkbox();
		showOnlyRestrictedAccessToAssignedEntities.setId(UserRoleDto.RESTRICT_ACCESS_TO_ASSIGNED_ENTITIES);
		showOnlyRestrictedAccessToAssignedEntities.setLabel(I18nProperties.getCaption(Captions.userRoleShowOnlyRestrictedAccessToAssignCases));
		showOnlyRestrictedAccessToAssignedEntities.addClassName(CssStyles.CHECKBOX_FILTER_INLINE);
		showOnlyRestrictedAccessToAssignedEntities.addValueChangeListener(e -> {
			criteria.setShowOnlyRestrictedAccessToAssignedEntities(e.getValue());
			navigateTo(criteria);
		});
		filterLayout.add(showOnlyRestrictedAccessToAssignedEntities);

		return filterLayout;
	}

	@Override
	public void enter(ViewChangeListener.ViewChangeEvent event) {

		if (event != null) {
			String params = event.getParameters().trim();
			if (params.startsWith("?")) {
				params = params.substring(1);
				criteria.fromUrlParams(params);
			}
			updateFilterComponents();
		}
		grid.reload();
		super.enter(event);
	}

	public void updateFilterComponents() {

		applyingCriteria = true;

		jurisdictionFilter.setValue(criteria.getJurisdictionLevel() == null ? null : criteria.getJurisdictionLevel());
		enabledFilter.setValue(criteria.getEnabled() == null ? ALL_FILTER : criteria.getEnabled() ? ENABLED_FILTER : DISABLED_FILTER);
		userRightsFilter.setValue(criteria.getUserRight() == null ? null : criteria.getUserRight());
		showOnlyRestrictedAccessToAssignedEntities.setValue(
			criteria.getShowOnlyRestrictedAccessToAssignedEntities() == null
				? Boolean.FALSE
				: criteria.getShowOnlyRestrictedAccessToAssignedEntities());

		applyingCriteria = false;
	}

}
