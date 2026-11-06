package de.symeda.sormas.ui.dashboard.surveillance.components.disease;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.disease.DiseaseBurdenDto;
import de.symeda.sormas.ui.dashboard.surveillance.components.disease.burden.DiseaseBurdenComponent;
import de.symeda.sormas.ui.dashboard.surveillance.components.disease.tile.DiseaseTileViewLayout;
import de.symeda.sormas.ui.utils.ButtonHelper;
import de.symeda.sormas.ui.utils.CssStyles;

public class DiseaseOverviewComponent extends HorizontalLayout {

    private static final int NUMBER_OF_DISEASES_COLLAPSED = 6;

    private final DiseaseBurdenComponent diseaseBurdenComponent;
    private final DiseaseTileViewLayout diseaseTileViewLayout;

    private final Button showTableViewButton;

    public DiseaseOverviewComponent() {
        setWidth(100, Unit.PERCENTAGE);
        setPadding(false);

        diseaseBurdenComponent = new DiseaseBurdenComponent();
        diseaseTileViewLayout = new DiseaseTileViewLayout();

        add(diseaseTileViewLayout);
        setFlexGrow(1, diseaseTileViewLayout);

        // "Expand" and "Collapse" buttons
        showTableViewButton =
            ButtonHelper.createIconButtonWithCaption("showTableView", "", VaadinIcon.TABLE, null, CssStyles.BUTTON_SUBTLE, CssStyles.VSPACE_NONE);
        Button showTileViewButton = ButtonHelper
            .createIconButtonWithCaption("showTileView", "", VaadinIcon.SQUARE_SHADOW, null, CssStyles.BUTTON_SUBTLE, CssStyles.VSPACE_NONE);

        showTableViewButton.addClickListener(e -> {
            remove(diseaseTileViewLayout);
            add(diseaseBurdenComponent);
            setFlexGrow(1, diseaseBurdenComponent);

            remove(showTableViewButton);
            add(showTileViewButton);
            setVerticalComponentAlignment(FlexComponent.Alignment.START, showTileViewButton);
        });
        showTileViewButton.addClickListener(e -> {
            remove(diseaseBurdenComponent);
            add(diseaseTileViewLayout);
            setFlexGrow(1, diseaseTileViewLayout);

            remove(showTileViewButton);
            add(showTableViewButton);
            setVerticalComponentAlignment(FlexComponent.Alignment.START, showTableViewButton);
        });

        add(showTableViewButton);
        setVerticalComponentAlignment(FlexComponent.Alignment.START, showTableViewButton);
    }

    public void refresh(List<DiseaseBurdenDto> diseasesBurden, boolean isShowingAllDiseases) {
        // sort, limit and filter
        Stream<DiseaseBurdenDto> diseasesBurdenStream =
            diseasesBurden.stream().sorted((dto1, dto2) -> (int) (dto2.getCaseCount() - dto1.getCaseCount()));
        if (!isShowingAllDiseases) {
            diseasesBurdenStream = diseasesBurdenStream.limit(NUMBER_OF_DISEASES_COLLAPSED);
        }
        diseasesBurden = diseasesBurdenStream.collect(Collectors.toList());

        diseaseBurdenComponent.refresh(diseasesBurden);
        diseaseTileViewLayout.refresh(diseasesBurden);
    }

    public Button getShowTableViewButton() {
        return showTableViewButton;
    }
}
