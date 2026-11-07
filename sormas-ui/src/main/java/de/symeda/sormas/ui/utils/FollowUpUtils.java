package de.symeda.sormas.ui.utils;

import java.util.Date;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.symeda.sormas.api.VisitOrigin;
import de.symeda.sormas.api.utils.DataHelper;
import de.symeda.sormas.api.utils.DateHelper;
import de.symeda.sormas.api.visit.VisitResult;
import de.symeda.sormas.api.visit.VisitResultDto;

public class FollowUpUtils {
  private FollowUpUtils() {
    // hidden constructor
  }

  public static HorizontalLayout createFollowUpLegend() {
    HorizontalLayout legendLayout = new HorizontalLayout();
    legendLayout.setSpacing(false);
    CssStyles.style(legendLayout, CssStyles.VSPACE_TOP_4);

    Span notSymptomaticColor = new Span();
    styleLegendEntry(notSymptomaticColor, CssStyles.LABEL_BACKGROUND_FOLLOW_UP_NOT_SYMPTOMATIC, true);
    legendLayout.add(notSymptomaticColor);

    Span notSymptomaticLabel = new Span(VisitResult.NOT_SYMPTOMATIC.toString());
    legendLayout.add(notSymptomaticLabel);

    Span symptomaticColor = new Span();
    styleLegendEntry(symptomaticColor, CssStyles.LABEL_BACKGROUND_FOLLOW_UP_SYMPTOMATIC, false);
    legendLayout.add(symptomaticColor);

    Span symptomaticLabel = new Span(VisitResult.SYMPTOMATIC.toString());
    legendLayout.add(symptomaticLabel);

    Span unavailableColor = new Span();
    styleLegendEntry(unavailableColor, CssStyles.LABEL_BACKGROUND_FOLLOW_UP_UNAVAILABLE, false);
    legendLayout.add(unavailableColor);

    Span unavailableLabel = new Span(VisitResult.UNAVAILABLE.toString());
    legendLayout.add(unavailableLabel);

    Span uncooperativeColor = new Span();
    styleLegendEntry(uncooperativeColor, CssStyles.LABEL_BACKGROUND_FOLLOW_UP_UNCOOPERATIVE, false);
    legendLayout.add(uncooperativeColor);

    Span uncooperativeLabel = new Span(VisitResult.UNCOOPERATIVE.toString());
    legendLayout.add(uncooperativeLabel);

    Span notPerformedColor = new Span();
    styleLegendEntry(notPerformedColor, CssStyles.LABEL_BACKGROUND_FOLLOW_UP_NOT_PERFORMED, false);
    legendLayout.add(notPerformedColor);

    Span notPerformedLabel = new Span(VisitResult.NOT_PERFORMED.toString());
    legendLayout.add(notPerformedLabel);

    return legendLayout;
  }

  private static void styleLegendEntry(Span label, String style, boolean first) {
    label.setHeight(18, Unit.PIXELS);
    label.setWidth(12, Unit.PIXELS);
    CssStyles.style(label, style, CssStyles.HSPACE_RIGHT_4);

    if (!first) {
      CssStyles.style(label, CssStyles.HSPACE_LEFT_3);
    }
  }

  public static String getVisitResultDescription(VisitResultDto result, Date date, Date contactDate, Date followUpUntil) {

    if (!DateHelper.isBetween(date, DateHelper.getStartOfDay(contactDate), DateHelper.getEndOfDay(followUpUntil))) {
      return "";
    }
    return result.toString();
  }

  public static String getVisitResultCssStyle(VisitResultDto result, Date date, Date contactDate, Date followUpUntil) {

    if (!DateHelper.isBetween(date, DateHelper.getStartOfDay(contactDate), DateHelper.getEndOfDay(followUpUntil))) {
      return "";
    }

    if (VisitOrigin.EXTERNAL_JOURNAL.equals(result.getOrigin())) {
      switch (result.getResult()) {
        case NOT_SYMPTOMATIC:
          return CssStyles.GRID_CELL_NOT_SYMPTOMATIC_EXTERNAL;
        case SYMPTOMATIC:
          return CssStyles.GRID_CELL_SYMPTOMATIC_EXTERNAL;
        case NOT_PERFORMED:
          return CssStyles.GRID_CELL_NOT_PERFORMED_EXTERNAL;
        case UNAVAILABLE:
          return CssStyles.GRID_CELL_UNAVAILABLE_EXTERNAL;
        case UNCOOPERATIVE:
          return CssStyles.GRID_CELL_UNCOOPERATIVE_EXTERNAL;
        default:
          throw new IndexOutOfBoundsException(DataHelper.toStringNullable(result));
      }
    }

    switch (result.getResult()) {
      case NOT_SYMPTOMATIC:
        return CssStyles.GRID_CELL_NOT_SYMPTOMATIC;
      case SYMPTOMATIC:
        return CssStyles.GRID_CELL_SYMPTOMATIC;
      case NOT_PERFORMED:
        return CssStyles.GRID_CELL_NOT_PERFORMED;
      case UNAVAILABLE:
        return CssStyles.GRID_CELL_UNAVAILABLE;
      case UNCOOPERATIVE:
        return CssStyles.GRID_CELL_UNCOOPERATIVE;
      default:
        throw new IndexOutOfBoundsException(DataHelper.toStringNullable(result));
    }
  }
}
