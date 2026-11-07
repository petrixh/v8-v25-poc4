package de.symeda.sormas.ui.campaign.expressions;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;

import de.symeda.sormas.api.campaign.data.CampaignFormDataEntry;
import de.symeda.sormas.api.campaign.form.CampaignFormElement;
import de.symeda.sormas.ui.SormasUI;
import de.symeda.sormas.ui.campaign.campaigndata.CampaignFormBuilder;
import de.symeda.sormas.ui.campaign.campaigndata.CampaignFormBuilder.GridLayout;
import de.symeda.sormas.ui.utils.fields.TextField;

public class ExpressionProcessorTest {

	private ExpressionProcessor expressionProcessor;
	private CampaignFormBuilder campaignFormBuilder;

	@BeforeAll
	public static void setupClass() {
		// was a SormasUI (a Vaadin 8 UI) as the current UI and a Page mock: SormasUI is now a holder attached to a Flow UI
		// (1a-30c), and CampaignFormBuilder no longer reads the Page (1a-65)
		UI ui = new UI();
		ComponentUtil.setData(ui, SormasUI.class, new SormasUI(ui));
		final MockedStatic<UI> uiMockedStatic = Mockito.mockStatic(UI.class);
		uiMockedStatic.when(UI::getCurrent).thenReturn(ui);
	}

	@BeforeEach
	public void setup() throws IOException {
		GridLayout campaignFormLayout = new GridLayout(12, 1);
		ObjectMapper objectMapper = new ObjectMapper();

		List<CampaignFormElement> campaignFormElements =
			createData(objectMapper, this.getClass().getResourceAsStream("/campaign/expressions/formelements.json"), CampaignFormElement.class);
		List<CampaignFormDataEntry> campaignFormDataEntries =
			createData(objectMapper, this.getClass().getResourceAsStream("/campaign/expressions/formvalues.json"), CampaignFormDataEntry.class);

		campaignFormBuilder = new CampaignFormBuilder(campaignFormElements, campaignFormDataEntries, campaignFormLayout, Collections.emptyList());
		campaignFormBuilder.buildForm();
		expressionProcessor = new ExpressionProcessor(campaignFormBuilder);
	}

	@Test
	public void testDisableExpressionFieldsForEditing() {
		expressionProcessor.disableExpressionFieldsForEditing();
		assertThat(campaignFormBuilder.getFields(), not(equalTo(Collections.emptyMap())));
		assertThat(campaignFormBuilder.getFields().entrySet().stream().filter(entry -> !entry.getValue().isEnabled()).count(), is(2L));
	}

	@Test
	public void testAddExpressionListener() {
		expressionProcessor.addExpressionListener();
		final TextField field = (TextField) campaignFormBuilder.getFields().get("childrenVaccinatedRecall");
		field.setValue("xyz");
		field.setValue("42");
	}

	@Test
	public void testConfigureExpressionFieldsWithTooltip() {
		expressionProcessor.configureExpressionFieldsWithTooltip();
		final TextField field = (TextField) campaignFormBuilder.getFields().get("childrenLivingInHouses");
		assertThat(field.getDescription(), is("Calculated based on: Number of < 5 years children vaccinated based on recall"));
		final TextField fieldRecall = (TextField) campaignFormBuilder.getFields().get("childrenVaccinatedRecall");
		assertThat(fieldRecall.getDescription(), isEmptyOrNullString());
	}

	private <D> List<D> createData(final ObjectMapper objectMapper, final InputStream inputStream, Class<D> dataType) throws IOException {
		JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, dataType);
		return objectMapper.readValue(inputStream, type);
	}
}
