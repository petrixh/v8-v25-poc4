/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2026 SORMAS Foundation gGmbH
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
package de.symeda.sormas.ui.survey;

import java.util.List;

import de.symeda.sormas.ui.utils.ContentMode;
import com.vaadin.flow.component.html.Span;
import com.vaadin.ui.Panel;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.dialog.Dialog;

import de.symeda.sormas.api.FacadeProvider;
import de.symeda.sormas.api.survey.SurveyTokenReferenceDto;
import de.symeda.sormas.api.survey.external.views.ExternalSurveyView;
import de.symeda.sormas.api.survey.external.views.QuestionAnswersView;
import org.apache.commons.lang3.StringUtils;

/**
 * Modal window displaying a survey questionnaire as a structured HTML view.
 * Shows questions, their answers and nested subquestions.
 * 
 * @implNote this was inspired from a survey tool's simplified view within EMAL.
 */
public class SurveyQuestionnaireWindow {

	public SurveyQuestionnaireWindow(SurveyTokenReferenceDto surveyTokenRef) {
		ExternalSurveyView surveyView;
		try {
			surveyView = FacadeProvider.getSurveyTokenFacade().getExternalSurveyView(surveyTokenRef.getUuid());
		} catch (Exception e) {
			surveyView = null;
		}

		Dialog window = new Dialog(surveyTokenRef.getCaption());
		window.setModal(true);
		window.setResizable(true);
		window.setWidth(700, com.vaadin.server.Unit.PIXELS);
		window.setHeight(80, com.vaadin.server.Unit.PERCENTAGE);

		VerticalLayout mainLayout = new VerticalLayout();
		mainLayout.setPadding(true);
		mainLayout.setSpacing(false);

		if (surveyView == null || surveyView.getQuestionAnswersViews() == null || surveyView.getQuestionAnswersViews().isEmpty()) {
			mainLayout.add(new Span("No questionnaire data available."));
		} else {
			Span htmlContent = new Span(buildHtml(surveyView.getQuestionAnswersViews()), ContentMode.HTML);
			htmlContent.setSizeFull();
			mainLayout.add(htmlContent);
		}

		Panel scrollPanel = new Panel(mainLayout);
		scrollPanel.setSizeFull();

		window.setContent(scrollPanel);
		window.open();
	}

	private String buildHtml(List<QuestionAnswersView> questions) {
		StringBuilder sb = new StringBuilder();
		sb.append("<table style=\"width:100%;border-collapse:collapse;font-family:sans-serif;font-size:13px;\">");
		sb.append("<thead><tr style=\"background:#f0f0f0;\">")
			.append("<th style=\"text-align:left;padding:6px 8px;border-bottom:2px solid #ccc;\">Question</th>")
			.append("<th style=\"text-align:left;padding:6px 8px;border-bottom:2px solid #ccc;\">Answer</th>")
			.append("</tr></thead>");
		sb.append("<tbody>");

		for (QuestionAnswersView q : questions) {
			appendQuestion(sb, q, 0);
		}

		sb.append("</tbody></table>");
		return sb.toString();
	}

	private void appendQuestion(StringBuilder sb, QuestionAnswersView q, int depth) {
		boolean hasSubquestions = q.getSubquestions() != null && !q.getSubquestions().isEmpty();
		String indent = depth > 0 ? "padding-left:" + (depth * 20) + "px;" : "";
		String rowStyle = depth > 0 ? "background:#fafafa;" : "background:#fff;";
		String questionStyle = hasSubquestions && q.getAnswer() == null ? "font-weight:bold;color:#333;" : "color:#333;";

		sb.append("<tr style=\"")
			.append(rowStyle)
			.append("border-bottom:1px solid #eee;\">")
			.append("<td style=\"padding:5px 8px;")
			.append(indent)
			.append(questionStyle)
			.append("\">")
			.append(q.getQuestion())
			.append("</td>")
			.append("<td style=\"padding:5px 8px;color:#555;\">")
			.append(resolveAnswer(q))
			.append("</td>")
			.append("</tr>");

		if (hasSubquestions) {
			for (QuestionAnswersView sub : q.getSubquestions()) {
				appendQuestion(sb, sub, depth + 1);
			}
		}
	}

	private String resolveAnswer(QuestionAnswersView q) {
		// Prefer human-readable answerText, fall back to raw answer
		String answerText = q.getAnswerText();
		if (StringUtils.isNotBlank(answerText)) {
			return answerText;
		}
		String answer = q.getAnswer();
		if (StringUtils.isNotBlank(answer)) {
			return answer;
		}
		return "<span>—</span>";
	}

}
