/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2020 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.event.EventDto;
import de.symeda.sormas.api.event.EventParticipantDto;
import de.symeda.sormas.api.feature.FeatureType;
import de.symeda.sormas.api.i18n.Captions;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.immunization.ImmunizationDto;
import de.symeda.sormas.api.sample.SampleDto;
import de.symeda.sormas.api.sormastosormas.SormasServerDescriptor;
import de.symeda.sormas.api.sormastosormas.SormasToSormasOriginInfoDto;
import de.symeda.sormas.api.sormastosormas.SormasToSormasShareTree;
import de.symeda.sormas.api.sormastosormas.share.incoming.ShareRequestStatus;
import de.symeda.sormas.api.sormastosormas.share.outgoing.SormasToSormasShareInfoCriteria;
import de.symeda.sormas.api.sormastosormas.share.outgoing.SormasToSormasShareInfoDto;
import de.symeda.sormas.api.user.UserDto;
import de.symeda.sormas.api.user.UserReferenceDto;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.ui.ControllerProvider;
import de.symeda.sormas.ui.UiUtil;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;
import de.symeda.sormas.ui.utils.DateFormatHelper;
import de.symeda.sormas.ui.utils.PaginationList;

public class SormasToSormasListComponent extends VerticalLayout {

	private static final long serialVersionUID = -7189942121987530912L;
	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private final String notSharedCaptionTag;
	private final SormasToSormasList sormasToSormasList;
	private VerticalLayout contentLayout;

	private SormasToSormasOriginInfoDto originInfo;
	private ShareDataLoader loadShares;
	private boolean isEditAllowed = true;

	public SormasToSormasListComponent(CaseDataDto caze, boolean isEditAllowed) {
		this.isEditAllowed = isEditAllowed;
		sormasToSormasList = new SormasToSormasList(this::reloadListSync);
		notSharedCaptionTag = Captions.sormasToSormasCaseNotShared;

		initLayout(
			caze.getSormasToSormasOriginInfo(),
			() -> FacadeProvider.getSormasToSormasCaseFacade().getAllShares(caze.getUuid()),
			e -> ControllerProvider.getSormasToSormasController().shareCaseFromDetailsPage(caze));
	}

	public SormasToSormasListComponent(ContactDto contact, boolean isEditAllowed) {
		this.isEditAllowed = isEditAllowed;
		sormasToSormasList = new SormasToSormasList(this::reloadListSync);
		notSharedCaptionTag = Captions.sormasToSormasContactNotShared;

		initLayout(
			contact.getSormasToSormasOriginInfo(),
			() -> FacadeProvider.getSormasToSormasContactFacade().getAllShares(contact.getUuid()),
			e -> ControllerProvider.getSormasToSormasController().shareContactFromDetailsPage(contact));
	}

	public SormasToSormasListComponent(SampleDto sample) {
		sormasToSormasList = new SormasToSormasList(null);
		notSharedCaptionTag = Captions.sormasToSormasSampleNotShared;

		SormasToSormasOriginInfoDto sampleOriginInfo = sample.getSormasToSormasOriginInfo();
		initLayout(
			sampleOriginInfo,
			() -> getAllSharesFromCurrentInstance(new SormasToSormasShareInfoCriteria().sample(sample.toReference()), sampleOriginInfo),
			null);
	}

	public SormasToSormasListComponent(EventDto event) {

		sormasToSormasList = new SormasToSormasList(this::reloadListSync);
		notSharedCaptionTag = Captions.sormasToSormasEventNotShared;

		initLayout(
			event.getSormasToSormasOriginInfo(),
			() -> FacadeProvider.getSormasToSormasEventFacade().getAllShares(event.getUuid()),
			e -> ControllerProvider.getSormasToSormasController().shareEventFromDetailsPage(event));
	}

	public SormasToSormasListComponent(EventParticipantDto eventParticipant) {
		sormasToSormasList = new SormasToSormasList(null);
		notSharedCaptionTag = Captions.sormasToSormasEventParticipantNotShared;

		initLayout(
			eventParticipant.getSormasToSormasOriginInfo(),
			() -> FacadeProvider.getSormasToSormasEventFacade().getAllShares(eventParticipant.getEvent().getUuid()),
			null);
	}

	public SormasToSormasListComponent(ImmunizationDto immunzation) {
		sormasToSormasList = new SormasToSormasList(null);
		notSharedCaptionTag = Captions.sormasToSormasImmunizationNotShared;

		SormasToSormasOriginInfoDto immunizationOriginInfo = immunzation.getSormasToSormasOriginInfo();
		initLayout(
			immunizationOriginInfo,
			() -> getAllSharesFromCurrentInstance(
				new SormasToSormasShareInfoCriteria().immunization(immunzation.toReference()),
				immunizationOriginInfo),
			null);
	}

	private void initLayout(
		SormasToSormasOriginInfoDto originInfo,
		ShareDataLoader loadShares,
		ComponentEventListener<ClickEvent<Button>> shareButtonClickListener) {
		this.originInfo = originInfo;
		this.loadShares = loadShares;

		setWidthFull();
		setPadding(false);
		setSpacing(false);

		HorizontalLayout componentHeader = new HorizontalLayout();
		componentHeader.setPadding(false);
		componentHeader.setSpacing(false);
		componentHeader.setWidthFull();
		add(componentHeader);

		Span header = new Span(I18nProperties.getCaption(Captions.sormasToSormasListTitle));
		header.addClassName(CssStyles.H3);
		componentHeader.add(header);

		if (shareButtonClickListener != null && UiUtil.permitted(isEditAllowed, UserRight.SORMAS_TO_SORMAS_SHARE)) {
			Button shareButtonButton =
				ButtonHelper.createIconButton(Captions.sormasToSormasShare, VaadinIcon.SHARE, shareButtonClickListener, CssStyles.BUTTON_PRIMARY);

			componentHeader.add(shareButtonButton);
			// was Alignment.MIDDLE_RIGHT
			componentHeader.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER, shareButtonButton);
			shareButtonButton.getStyle().set("margin-left", "auto");
		}

		contentLayout = new VerticalLayout();
		contentLayout.setPadding(false);
		contentLayout.setSpacing(false);
		add(contentLayout);

		reloadList();
	}

	public void reloadList() {
		UI currentUI = UI.getCurrent();

		contentLayout.removeAll();

		Span placeHolder = new Span(I18nProperties.getString(Strings.sormasToSormasLoadingShares));
		contentLayout.add(placeHolder);

		// Flow: UI state changes need the session lock, so the poll interval is set here (request thread), not in the
		// loading thread; set in this response, the client polls for the result even when loading ends before it is sent
		currentUI.setPollInterval(300);

		Thread loadSharesThread = new Thread(() -> {
			try {
				try {
					reloadListWithWrapper(uiChanges -> currentUI.access(uiChanges::run));
				} finally {
					currentUI.access(() -> currentUI.setPollInterval(-1));
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);

				currentUI.access(() -> {
					currentUI.setPollInterval(-1);
					placeHolder.setText(I18nProperties.getString(Strings.errorSormasToSormasLoadShares));
				});
			}
		});

		loadSharesThread.start();
	}

	private void reloadListSync() {
		reloadListWithWrapper(Runnable::run);
	}

	private void reloadListWithWrapper(Consumer<Runnable> wrapUiChanges) {
		UserDto currentUser = FacadeProvider.getUserFacade().getCurrentUser();

		I18nProperties.setUserLanguage(currentUser.getLanguage());
		FacadeProvider.getI18nFacade().setUserLanguage(currentUser.getLanguage());

		List<SormasToSormasShareTree> shareInfos = loadShares.load();
		String currentServerOrgId = FacadeProvider.getSormasToSormasFacade().getOrganizationId();

		List<SormasToSormasShareInfoDto> shareInfoList = getShareInfoList(shareInfos);
		SormasToSormasShareInfoDto ownerShare = getOwnerShare(shareInfos);
		SormasToSormasOriginInfoDto rootOrigin = findRootOriginInfo(shareInfos);
		List<String> directShareUuids = getDirectShares(shareInfos).stream().map(s -> s.getShare().getUuid()).collect(Collectors.toList());

		String ownerOrganizationId = getOwnerOrganizationId(ownerShare, rootOrigin, currentServerOrgId);
		boolean isOwnedByCurrentOrg = currentServerOrgId.equals(ownerOrganizationId);
		boolean isOwnedByOrigin = originInfo != null && originInfo.getOrganizationId().equals(ownerOrganizationId);
		boolean isOwnedByRootOrg = rootOrigin != null && rootOrigin.getOrganizationId().equals(ownerOrganizationId);

		wrapUiChanges.accept(() -> {
			try {
				contentLayout.removeAll();

				// render origin
				if (originInfo != null) {
					HorizontalLayout originLayout = buildSormasOriginInfo(originInfo, isOwnedByOrigin);
					originLayout.addClassName(CssStyles.VSPACE_3);
					contentLayout.add(originLayout);
				}

				// render the owner of the entity
				if (!isOwnedByCurrentOrg && !isOwnedByOrigin) {
					Component ownerLayout = null;
					if (isOwnedByRootOrg) {
						ownerLayout = buildSormasOriginInfo(rootOrigin, true);
					} else if (ownerShare != null && !ownerShare.getTargetDescriptor().getId().equals(currentServerOrgId)) {
						ownerLayout = buildOwnerShareLayout(ownerShare);
					}

					if (ownerLayout != null) {
						((HasStyle) ownerLayout).addClassName(CssStyles.VSPACE_3);
						contentLayout.add(ownerLayout);
					}
				}

				// show shares to other systems then owner and current one
				List<SormasToSormasShareListEntryData> listData = shareInfoList.stream().filter(s -> {
					String shareOrganizationId = s.getTargetDescriptor().getId();

					if (ownerShare != null && shareOrganizationId.equals(ownerShare.getTargetDescriptor().getId())) {
						return false;
					}

					if (originInfo != null && shareOrganizationId.equals(originInfo.getOrganizationId())
					// show return share
						&& !(s.getRequestStatus() == ShareRequestStatus.PENDING && directShareUuids.contains(s.getUuid()))) {
						return false;
					}

					if (isOwnedByRootOrg && shareOrganizationId.equals(rootOrigin.getOrganizationId())) {
						return false;
					}

					return !shareOrganizationId.equals(currentServerOrgId);
				}).map(s -> {
					SormasToSormasShareListEntryData entryData = new SormasToSormasShareListEntryData();
					entryData.shareUuid = s.getUuid();
					entryData.target = s.getTargetDescriptor().getName();
					entryData.sender = s.getSender().getShortCaption();
					entryData.status = s.getRequestStatus();
					entryData.creationDate = s.getCreationDate();
					entryData.comment = s.getComment();
					entryData.ownershipHandedOver = s.isOwnershipHandedOver();
					entryData.responseComment = s.getResponseComment();
					entryData.isDirectShare = directShareUuids.contains(s.getUuid());

					return entryData;
				}).collect(Collectors.toList());

				// add the creator of the entity as a share
				if (!isOwnedByRootOrg
					&& rootOrigin != null
					&& originInfo != null
					&& !rootOrigin.getOrganizationId().equals(currentServerOrgId)
					&& !rootOrigin.getOrganizationId().equals(originInfo.getOrganizationId())) {
					SormasServerDescriptor serverDescriptor =
						FacadeProvider.getSormasToSormasFacade().getSormasServerDescriptorById(rootOrigin.getOrganizationId());

					SormasToSormasShareListEntryData entryData = new SormasToSormasShareListEntryData();
					entryData.shareUuid = null;
					entryData.target = serverDescriptor.getName();
					entryData.sender = rootOrigin.getSenderName();
					entryData.status = ShareRequestStatus.ACCEPTED;
					entryData.creationDate = rootOrigin.getCreationDate();
					entryData.comment = rootOrigin.getComment();
					entryData.ownershipHandedOver = rootOrigin.isOwnershipHandedOver();

					listData.add(entryData);
				}

				if (shareInfoList.size() > 0 && listData.size() > 0) {
					Span shareListLabel = new Span(I18nProperties.getCaption(Captions.sormasToSormasSharedWith));
					shareListLabel.addClassNames(CssStyles.LABEL_BOLD, CssStyles.VSPACE_4);
					contentLayout.add(shareListLabel);

					contentLayout.add(sormasToSormasList);
					sormasToSormasList.setData(listData);
				} else if (contentLayout.getComponentCount() == 0) {
					contentLayout.add(new Span(I18nProperties.getCaption(notSharedCaptionTag)));
				}
			} catch (Exception e) {
				logger.error("Failed to load shares", e);
				contentLayout.removeAll();
				contentLayout.add(new Span(I18nProperties.getString(Strings.errorSormasToSormasLoadShares)));
			}
		});

	}

	private String getOwnerOrganizationId(SormasToSormasShareInfoDto ownerShare, SormasToSormasOriginInfoDto rootOrigin, String ownOrganizationId) {
		if (ownerShare != null) {
			return ownerShare.getTargetDescriptor().getId();
		}

		if (rootOrigin != null && !rootOrigin.isOwnershipHandedOver()) {
			return rootOrigin.getOrganizationId();
		}

		if (originInfo != null) {
			return originInfo.getOrganizationId();
		}

		return ownOrganizationId;
	}

	private SormasToSormasOriginInfoDto findRootOriginInfo(List<SormasToSormasShareTree> shareInfos) {
		for (SormasToSormasShareTree shareInfo : shareInfos) {
			if (shareInfo.getReShares().size() > 0) {
				return shareInfo.getReShares().get(0).getOrigin();
			}
		}

		return null;
	}

	private List<SormasToSormasShareInfoDto> getShareInfoList(List<SormasToSormasShareTree> shareInfos) {
		return shareInfos.stream()
			.map((s -> Stream.concat(Stream.of(s.getShare()), getShareInfoList(s.getReShares()).stream())))
			.reduce(Stream.empty(), Stream::concat)
			.collect(Collectors.toList());
	}

	private List<SormasToSormasShareTree> getDirectShares(List<SormasToSormasShareTree> shareInfos) {
		return shareInfos.stream()
			.map((s -> Stream.concat(Stream.of(s), getDirectShares(s.getReShares()).stream())))
			.reduce(Stream.empty(), Stream::concat)
			.filter(SormasToSormasShareTree::isDirectShare)
			.collect(Collectors.toList());
	}

	private SormasToSormasShareInfoDto getOwnerShare(List<SormasToSormasShareTree> shareInfos) {
		Optional<SormasToSormasShareTree> sharedWithOwnership = shareInfos.stream().filter(s -> {
			SormasToSormasShareInfoDto share = s.getShare();

			return share.getRequestStatus() == ShareRequestStatus.ACCEPTED && share.isOwnershipHandedOver();
		}).findFirst();

		if (sharedWithOwnership.isPresent()) {
			SormasToSormasShareTree ownerTree = sharedWithOwnership.get();
			SormasToSormasShareInfoDto ownerShare = getOwnerShare(ownerTree.getReShares());

			return ownerShare == null ? ownerTree.getShare() : ownerShare;
		}

		return null;
	}

	private SormasToSormasShareInfoDto getShareFromOrigin(List<SormasToSormasShareInfoDto> shareInfoList) {
		if (originInfo != null) {
			return shareInfoList.stream()
				.filter(
					s -> s.getRequestStatus() == ShareRequestStatus.ACCEPTED
						&& s.getTargetDescriptor().getId().equals(originInfo.getOrganizationId()))
				.findFirst()
				.orElse(null);
		}

		return null;
	}

	private HorizontalLayout buildReturnShareLayout(VerticalLayout infoLayout) {
		HorizontalLayout layout = new HorizontalLayout();
		layout.setPadding(false);
		layout.setWidthFull();

		layout.add(infoLayout);
		layout.setFlexGrow(1, infoLayout);

		return layout;
	}

	private VerticalLayout buildOwnerShareLayout(SormasToSormasShareInfoDto shareInfo) {
		VerticalLayout infoLayout = new VerticalLayout();
		infoLayout.setWidthFull();
		infoLayout.setPadding(false);
		infoLayout.setSpacing(false);
		infoLayout.addClassName(CssStyles.SORMAS_LIST_ENTRY);

		Span targetLabel = new Span(I18nProperties.getCaption(Captions.sormasToSormasOwnedBy) + " " + shareInfo.getTargetDescriptor().getName());
		targetLabel.addClassName(CssStyles.LABEL_BOLD);
		infoLayout.add(targetLabel);

		Span senderLabel = new Span(I18nProperties.getCaption(Captions.sormasToSormasSharedBy) + ": " + shareInfo.getSender().getCaption());
		infoLayout.add(senderLabel);

		Span shareDateLabel =
			new Span(I18nProperties.getCaption(Captions.sormasToSormasSharedDate) + ": " + DateFormatHelper.formatDate(shareInfo.getCreationDate()));
		infoLayout.add(shareDateLabel);

		if (!DataHelper.isNullOrEmpty(shareInfo.getComment())) {
			infoLayout.add(new Span(shareInfo.getComment()));
		}

		return infoLayout;
	}

	private HorizontalLayout buildSormasOriginInfo(SormasToSormasOriginInfoDto originInfo, boolean isOwner) {
		HorizontalLayout layout = new HorizontalLayout();
		layout.setPadding(false);
		layout.setWidthFull();

		VerticalLayout infoLayout = new VerticalLayout();
		infoLayout.setPadding(false);
		infoLayout.setSpacing(false);

		SormasServerDescriptor serverDescriptor =
			FacadeProvider.getSormasToSormasFacade().getSormasServerDescriptorById(originInfo.getOrganizationId());

		Span senderOrganizationLabel =
			new Span(I18nProperties.getCaption(isOwner ? Captions.sormasToSormasOwnedBy : Captions.sormasToSormasSentFrom) + " " + serverDescriptor);
		senderOrganizationLabel.addClassNames(CssStyles.LABEL_BOLD, CssStyles.LABEL_WHITE_SPACE_NORMAL);
		infoLayout.add(senderOrganizationLabel);

		infoLayout.add(new Span(I18nProperties.getCaption(Captions.sormasToSormasSharedBy) + ": " + originInfo.getSenderName()));

		if (originInfo.getSenderEmail() != null) {
			infoLayout.add(new Span(originInfo.getSenderEmail()));
		}

		if (originInfo.getSenderPhoneNumber() != null) {
			infoLayout.add(new Span(originInfo.getSenderPhoneNumber()));
		}

		Span shareDateLabel = new Span(
			I18nProperties.getCaption(Captions.sormasToSormasSharedDate) + ": " + DateFormatHelper.formatDate(originInfo.getCreationDate()));
		infoLayout.add(shareDateLabel);

		if (!DataHelper.isNullOrEmpty(originInfo.getComment())) {
			infoLayout.add(new Span(originInfo.getComment()));
		}

		layout.add(infoLayout);
		layout.setFlexGrow(1, infoLayout);

		return layout;
	}

	private List<SormasToSormasShareTree> getAllSharesFromCurrentInstance(
		SormasToSormasShareInfoCriteria shareInfoCriteria,
		SormasToSormasOriginInfoDto originInfo) {
		List<SormasToSormasShareTree> shares = FacadeProvider.getSormasToSormasShareInfoFacade()
			.getIndexList(shareInfoCriteria, null, null)
			.stream()
			.map(s -> new SormasToSormasShareTree(null, s, Collections.emptyList(), true))
			.collect(Collectors.toList());

		if (originInfo != null) {
			// if coming from other instance, add a mock share info so the structure looks like
			// [share from origin] -> [shares from this instance] so we can show the owner of the entity on the UI
			SormasToSormasShareInfoDto shareFromOrigin = createMockShareInfoFromOrigin(originInfo);
			return Collections.singletonList(new SormasToSormasShareTree(originInfo, shareFromOrigin, shares, true));
		}

		return shares;
	}

	private SormasToSormasShareInfoDto createMockShareInfoFromOrigin(SormasToSormasOriginInfoDto sampleOriginInfo) {
		SormasToSormasShareInfoDto shareFromOrigin = new SormasToSormasShareInfoDto();
		shareFromOrigin.setSender(new UserReferenceDto(null, sampleOriginInfo.getSenderName(), ""));
		shareFromOrigin.setTargetDescriptor(
			FacadeProvider.getSormasToSormasFacade().getSormasServerDescriptorById(FacadeProvider.getSormasToSormasFacade().getOrganizationId()));
		shareFromOrigin.setOwnershipHandedOver(sampleOriginInfo.isOwnershipHandedOver());
		shareFromOrigin.setWithAssociatedContacts(sampleOriginInfo.isWithAssociatedContacts());
		shareFromOrigin.setWithSamples(sampleOriginInfo.isWithSamples());
		shareFromOrigin.setWithEvenParticipants(sampleOriginInfo.isWithEventParticipants());
		shareFromOrigin.setWithImmunizations(sampleOriginInfo.isWithImmunizations());
		shareFromOrigin.setWithSurveillanceReports(sampleOriginInfo.isWithSurveillanceReports());
		shareFromOrigin.setPseudonymizedPersonalData(sampleOriginInfo.isPseudonymizedData());
		shareFromOrigin.setPseudonymizedSensitiveData(sampleOriginInfo.isPseudonymizedData());
		shareFromOrigin.setRequestStatus(ShareRequestStatus.ACCEPTED);
		return shareFromOrigin;
	}

	private interface ShareDataLoader {

		List<SormasToSormasShareTree> load();

	}
	private static class SormasToSormasList extends PaginationList<SormasToSormasShareListEntryData> {

		private static final long serialVersionUID = -4659924105492791566L;
		private final Runnable revokeCallback;

		public SormasToSormasList(Runnable revokeCallback) {
			super(5);

			this.revokeCallback = revokeCallback;
		}

		@Override
		public void reload() {
		}

		public void setData(List<SormasToSormasShareListEntryData> data) {
			setEntries(data);
			showPage(1);
		}

		@Override
		protected void drawDisplayedEntries() {
			List<SormasToSormasShareListEntryData> displayedEntries = getDisplayedEntries();

			for (int i = 0; i < displayedEntries.size(); i++) {
				SormasToSormasShareListEntryData shareInfo = displayedEntries.get(i);
				SormasToSormasShareListEntry listEntry = new SormasToSormasShareListEntry(shareInfo, revokeCallback);
				if (i == 0) {
					listEntry.addClassName(CssStyles.SORMAS_LIST_ENTRY_NO_BORDER);
				}
				listLayout.add(listEntry);
			}
		}

	}
	private static class SormasToSormasShareListEntry extends HorizontalLayout {

		private static final long serialVersionUID = 7462585357530141263L;

		public SormasToSormasShareListEntry(SormasToSormasShareListEntryData data, Runnable revokeCallback) {
			setPadding(false);
			setSpacing(true);
			setWidthFull();
			addClassName(CssStyles.SORMAS_LIST_ENTRY);

			VerticalLayout infoLayout = new VerticalLayout();
			infoLayout.setWidthFull();
			infoLayout.setPadding(false);
			infoLayout.setSpacing(false);

			Span targetLabel = new Span(data.target);
			targetLabel.addClassName(CssStyles.LABEL_BOLD);
			infoLayout.add(targetLabel);

			Span senderLabel = new Span(I18nProperties.getCaption(Captions.sormasToSormasSharedBy) + ": " + data.sender);
			infoLayout.add(senderLabel);

			Span shareDateLabel =
				new Span(I18nProperties.getCaption(Captions.sormasToSormasSharedDate) + ": " + DateFormatHelper.formatDate(data.creationDate));
			infoLayout.add(shareDateLabel);

			if (UiUtil.enabled(FeatureType.SORMAS_TO_SORMAS_ACCEPT_REJECT)) {
				Span statusLabel = new Span(I18nProperties.getCaption(Captions.SormasToSormasShareRequest_status) + ": " + data.status);
				infoLayout.add(statusLabel);
			}

			if (!DataHelper.isNullOrEmpty(data.comment)) {
				Span comment = new Span(I18nProperties.getCaption(Captions.SormasToSormasShareRequest_comment) + ": " + data.comment);
				comment.addClassName(CssStyles.LABEL_WHITE_SPACE_NORMAL);
				infoLayout.add(comment);
			}

			if (!DataHelper.isNullOrEmpty(data.responseComment)) {
				Span responseComment =
					new Span(I18nProperties.getCaption(Captions.SormasToSormasShareRequest_responseComment) + ": " + data.responseComment);
				responseComment.addClassName(CssStyles.LABEL_WHITE_SPACE_NORMAL);
				infoLayout.add(responseComment);
			}

			add(infoLayout);
			setFlexGrow(1, infoLayout);

			if (revokeCallback != null
				&& UiUtil.permitted(FeatureType.SORMAS_TO_SORMAS_ACCEPT_REJECT, UserRight.SORMAS_TO_SORMAS_SHARE)
				&& data.shareUuid != null
				&& data.status == ShareRequestStatus.PENDING
				&& data.isDirectShare) {
				add(ButtonHelper.createIconButton(Captions.sormasToSormasRevokeShare, VaadinIcon.TRASH, (e) -> {
					ControllerProvider.getSormasToSormasController().revokeShare(data.shareUuid, revokeCallback);
				}));
			}

		}

	}

	private static class SormasToSormasShareListEntryData {

		private String shareUuid;
		private String target;
		private String sender;
		private ShareRequestStatus status;
		private Date creationDate;
		private String comment;
		private Boolean ownershipHandedOver;
		private String responseComment;
		private boolean isDirectShare;
	}
}
