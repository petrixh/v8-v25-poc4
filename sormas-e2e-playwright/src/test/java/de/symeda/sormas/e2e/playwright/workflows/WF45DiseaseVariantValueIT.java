package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.DirtiesAppMemory;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF45-disease-variant-value.md */
@DisplayName("WF45 Add a disease variant value and use it on a case (admin, SurvSup; C1)")
@DirtiesAppMemory("adds a customizable enum value: enum cache")
@AppCopy(AppCopy.Config.C1)
class WF45DiseaseVariantValueIT extends WorkflowTest {

	Ui admin;
	Ui sup;

	@BeforeAll
	void open() {
		admin = newUi();
		admin.login("admin");
		sup = newUi();
		sup.login("SurvSup");
	}

	private void openCase() {
		sup.openView("Cases");
		sup.fillSearch("caseLike", "SCAS01");
		sup.applyFilters();
		sup.settle(1000);
		sup.clickGridText("SCAS01");
		sup.waitText("Alpha SEEDPERSON", 15_000);
		sup.settle(800);
	}

	@Test
	@Order(1)
	@DisplayName("1. Configuration, customizable enums: the list of value types is shown")
	void step1() {
		admin.openView("Configuration");
		admin.clickEntry(Pattern.compile("^customizable enums$", Pattern.CASE_INSENSITIVE));
		assertTrue(admin.waitText("Customizable Enum Configuration", 10_000), "title");
		admin.waitRow("Disease variant", 10_000);
		List<String> rows = admin.rowTexts();
		for (String x : List.of("Disease variant", "Occupation type", "Pathogen")) {
			assertTrue(rows.stream().anyMatch(r -> r.contains(x)), "type " + x);
		}
		assertFalse(rows.stream().anyMatch(r -> r.contains("WF variant")), "no WF variant yet");
	}

	@Test
	@Order(2)
	@DisplayName("2. Add the disease variant value \"WF variant\", then tick COVID-19 as its disease: it is listed for COVID-19")
	void step2() {
		admin.clickEntry(Pattern.compile("new entry", Pattern.CASE_INSENSITIVE));
		admin.waitText("Data type", 10_000);
		admin.comboPick(admin.windowField("Data type", true), "Disease variant", "Disease variant", true, 500, 800);
		admin.windowField("Value", true).fill("WF_VARIANT");
		admin.windowField("Caption", true).fill("WF variant");
		admin.windowField("Caption", true).press("Tab");
		admin.settle(400);
		admin.windowButton("Save");
		admin.noModal();
		assertEquals(0, admin.windowsAfterClose(), "windows left");
		admin.waitRow("WF variant", 10_000);
		String row = admin.rowTextWith("WF variant");
		assertTrue(row.contains("Disease variant") && row.contains("WF_VARIANT") && row.contains("All diseases"), "listed: " + row);
		admin.clickRow(admin.rowIndex("WF variant"));
		admin.waitText("Edit WF variant", 10_000);
		assertTrue(admin.windowText().contains("Edit WF variant"), "edit form");
		admin.windowClickText("COVID-19");
		admin.settle(400);
		admin.windowButton("Save");
		admin.noModal();
		admin.waitRow("WF variant", 10_000);
		String row2 = admin.rowTextWith("WF variant");
		assertTrue(row2.contains("COVID-19") && !row2.contains("All diseases"), "listed for COVID-19: " + row2);
	}

	@Test
	@Order(3)
	@DisplayName("3. As SurvSup open SCAS01: the disease variant field offers \"WF variant\"; choose it and save: saved")
	void step3() {
		openCase();
		String t = sup.pageText();
		assertTrue(t.contains("Alpha SEEDPERSON") && t.contains("COVID-19"), "case shown");
		var dv = sup.field("Disease variant");
		dv.scrollIntoViewIfNeeded();
		sup.typeInCombo(dv, "WF", true, 500, true);
		sup.waitMatch("WF variant", 5000);
		assertTrue(sup.suggestionsText().contains("WF variant"), "offers WF variant");
		sup.pickSuggestion("WF variant", 500);
		sup.savePage();
		assertTrue(sup.waitText("Case saved", 10_000), "saved");
		sup.settle(800);
		sup.page.keyboard().press("Escape");
		assertEquals("WF variant", sup.field("Disease variant").inputValue(), "variant after save");
	}

	@Test
	@Order(4)
	@DisplayName("4. Reopen SCAS01: the disease variant shows \"WF variant\"; the value and the case are stored")
	void step4() {
		openCase();
		assertEquals("WF variant", sup.field("Disease variant").inputValue(), "variant");
		assertEquals("COVID-19", sup.field("Disease").inputValue(), "disease");
		// Data check: one new disease variant; SCAS01 stored with it
		assertEquals("1", copy.sql("SELECT count(*) FROM customizableenumvalue WHERE value = 'WF_VARIANT' AND datatype = 'DISEASE_VARIANT'"));
		assertEquals("WF_VARIANT", copy.sql("SELECT diseasevariant FROM cases WHERE uuid LIKE 'SCAS01%'"));
	}
}
