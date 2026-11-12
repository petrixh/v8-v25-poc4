package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF07-line-listing-configuration.md */
@DisplayName("WF07 Switch on line listing for a disease in a district (SurvSup, C1)")
@AppCopy(AppCopy.Config.C1)
class WF07LineListingConfigurationIT extends WorkflowTest {

	private static final Pattern EDIT = Pattern.compile("^edit line listing$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ENABLE = Pattern.compile("enable line listing for disease", Pattern.CASE_INSENSITIVE);

	Ui ui;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("SurvSup");
	}

	private void goToLineListing() {
		ui.openView("Configuration");
		Pattern ll = Pattern.compile("^line listing$", Pattern.CASE_INSENSITIVE);
		ui.clickEntry(ll, ui.entryCount(ll) - 1);
		ui.waitText("Line Listing Configuration", 10_000);
		ui.settle(800);
	}

	private Locator checkbox() {
		return ui.windowCheckbox(null).first();
	}

	/** End date as the date field shows it, M/D/YY. */
	private String endShown() {
		return today.plusDays(14).format(java.time.format.DateTimeFormatter.ofPattern("M/d/yy"));
	}

	@Test
	@Order(1)
	@DisplayName("1. Open Configuration, line listing: the page offers to choose a disease and shows the region; none configured yet")
	void step1() {
		goToLineListing();
		String t = ui.pageText();
		assertTrue(t.contains("Line Listing Configuration"), "page shown");
		assertTrue(t.toLowerCase().contains("default region"), "region shown");
		assertTrue(ui.filterCombo(0).isVisible(), "disease choice");
		assertEquals(0, ui.buttonCount(EDIT), "no disease configured");
	}

	@Test
	@Order(2)
	@DisplayName("2. Choose Measles and \"Enable line listing for disease\": the edit window lists the Default District, inactive")
	void step2() {
		ui.comboPick(ui.filterCombo(0), "Meas", "Measles");
		ui.clickButton(ENABLE);
		ui.waitText("Edit Line Listing", 10_000);
		ui.settle(800);
		String t = ui.windowText();
		assertTrue(Pattern.compile("Edit Line Listing", Pattern.CASE_INSENSITIVE).matcher(t).find(), "window title");
		assertTrue(t.contains("Measles") && t.contains("Default Region"), "for Measles");
		assertTrue(t.contains("Default District"), "district listed");
		assertFalse(checkbox().isChecked(), "inactive");
		assertEquals("", Ui.dateValue(ui.windowLastDate()), "no end date");
	}

	@Test
	@Order(3)
	@DisplayName("3. Activate for the Default District with an end date 14 days after the reset date and save: active with that end date")
	void step3() {
		ui.windowTickFirst();
		ui.settle(300);
		ui.fillDate(ui.windowLastDate(), en(14), 400);
		assertTrue(checkbox().isChecked(), "active before save");
		assertEquals(endShown(), Ui.dateValue(ui.windowLastDate()), "end date before save");
		ui.windowButton("Save changes");
		assertTrue(ui.waitText("saved", 6000) || ui.waitText("Line listing", 3000), "confirmation");
		ui.noModal();
		assertEquals(0, ui.windowCount(), "window closed");
		assertTrue(ui.pageText().contains("Measles"), "Measles listed");
		ui.clickButton(EDIT);
		ui.waitText("Edit Line Listing", 10_000);
		ui.settle(800);
		assertTrue(checkbox().isChecked(), "active after save");
		assertEquals(endShown(), Ui.dateValue(ui.windowLastDate()), "end date after save");
	}

	@Test
	@Order(4)
	@DisplayName("4. Leave and come back: Measles' Default District is still active until the same end date")
	void step4() {
		ui.windowButton("Discard changes");
		ui.noModal();
		ui.openView("Cases");
		goToLineListing();
		ui.clickButton(EDIT);
		ui.waitText("Edit Line Listing", 10_000);
		ui.settle(800);
		assertTrue(checkbox().isChecked(), "active");
		assertEquals(endShown(), Ui.dateValue(ui.windowLastDate()), "end date");
	}

	@Test
	@Order(5)
	@DisplayName("5. Deactivate and save: Measles is no longer listed, choosing it again shows the district inactive; the setting ends inactive")
	void step5() {
		ui.windowTickFirst();
		ui.settle(300);
		ui.windowButton("Save changes");
		ui.noModal();
		assertEquals(0, ui.windowCount(), "window closed");
		assertEquals(0, ui.buttonCount(EDIT), "Measles no longer listed");
		ui.comboPick(ui.filterCombo(0), "Meas", "Measles");
		ui.clickButton(ENABLE);
		ui.waitText("Edit Line Listing", 10_000);
		ui.settle(800);
		assertFalse(checkbox().isChecked(), "inactive");
		assertEquals("", Ui.dateValue(ui.windowLastDate()), "no end date");
		// Data check: no active Measles line listing remains for the Default district
		assertEquals(
			"0",
			copy.sql(
				"SELECT count(*) FROM featureconfiguration WHERE featuretype = 'LINE_LISTING' AND disease = 'MEASLES' AND enabled = true"));
	}
}
