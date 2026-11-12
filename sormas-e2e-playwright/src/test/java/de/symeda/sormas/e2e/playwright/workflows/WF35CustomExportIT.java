package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF35-custom-export.md */
@DisplayName("WF35 Saved custom case export configuration (NatUser, C1)")
@AppCopy(AppCopy.Config.C1)
class WF35CustomExportIT extends WorkflowTest {

	Ui ui;
	List<String> firstHeader;

	@BeforeAll
	void open() {
		ui = newUi();
		ui.login("NatUser");
	}

	private Locator box(String label) {
		return ui.checkboxInput(label, true);
	}

	private void check(String label) {
		box(label).check(new Locator.CheckOptions().setForce(true));
	}

	/** Line 1 holds the entity names, line 2 the field names, line 3 the captions. */
	private static List<String> lines(String csv) {
		return Arrays.stream(csv.split("\n")).filter(l -> !l.isBlank()).toList();
	}

	private static List<String> header(String csv) {
		return Arrays.asList(lines(csv).get(1).replace("\"", "").split(","));
	}

	/** Exports with the first (only) configuration: its export button. */
	private String exportConfig(String file) throws IOException {
		Download d = ui.page.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(90_000), () -> ui.windowGridButton(0));
		Path target = out.resolve(file);
		d.saveAs(target);
		return Files.readString(target, StandardCharsets.UTF_8);
	}

	@Test
	@Order(1)
	@DisplayName("1. Case directory, Export, Custom Case Export: the list of saved export configurations is shown (no named configuration)")
	void step1() {
		ui.openView("Cases");
		ui.button("Export").click();
		ui.settle(400);
		ui.clickEntry("Custom Case Export");
		ui.waitText("My exports", 10_000);
		String t = ui.windowText();
		assertTrue(t.contains("Custom Case Export"), "title");
		assertTrue(t.contains("My exports") && t.contains("NAME"), "list shown");
		assertTrue(t.contains("NEW EXPORT CONFIGURATION"), "new button");
		assertEquals(0, ui.windowGridFirstColumn().size(), "no named configuration");
	}

	@Test
	@Order(2)
	@DisplayName("2. New configuration \"Workflow export\" with case id, disease, classification, report date, first and last name; save: it is listed")
	void step2() {
		ui.windowButtonMatching(Pattern.compile("new export configuration", Pattern.CASE_INSENSITIVE));
		ui.waitText("New Export Configuration", 10_000);
		assertTrue(ui.windowText().contains("New Export Configuration"), "form title");
		ui.windowTextInput(0).fill("Workflow export");
		for (String c : List.of("Case ID", "Disease", "Case classification", "Date of report", "First name", "Last name")) {
			check(c);
		}
		assertEquals(6, ui.checkedCount(true), "six checked");
		ui.windowButton("Save");
		ui.noModal();
		ui.waitText("Workflow export", 10_000);
		assertTrue(ui.windowText().contains("Workflow export"), "listed by name");
	}

	@Test
	@Order(3)
	@DisplayName("3. Export with this configuration: a CSV with exactly these columns in the app's order downloads")
	void step3() throws IOException {
		String csv = exportConfig("workflow-export-1.csv");
		firstHeader = header(csv);
		assertEquals("uuid,disease,person.firstName,person.lastName,reportDate,caseClassification", String.join(",", firstHeader), "column order");
		assertEquals(6, firstHeader.size(), "six columns");
		assertTrue(lines(csv).size() - 3 > 0, "rows per case");
		assertTrue(csv.contains("SCAS01"), "seed case");
	}

	@Test
	@Order(4)
	@DisplayName("4. Edit the configuration to add outcome and save; export again: the outcome column is added; one configuration is stored for NatUser")
	void step4() throws IOException {
		ui.windowGridButton(1);
		ui.waitText("Outcome of case", 10_000);
		check("Outcome of case");
		assertEquals(7, ui.checkedCount(true), "seven checked");
		ui.windowButton("Save");
		ui.noModal();
		ui.settle(800);
		String second = exportConfig("workflow-export-2.csv");
		List<String> h = header(second);
		assertEquals("outcome", h.get(6), "outcome last");
		assertEquals(7, h.size(), "seven columns");
		assertEquals(firstHeader.size() + 1, h.size(), "outcome added");
		// Data check: one saved export configuration for NatUser
		assertEquals(
			"1",
			copy.sql("SELECT count(*) FROM exportconfiguration e JOIN users u ON u.id = e.user_id WHERE u.username = 'NatUser' AND e.name = 'Workflow export'"));
	}
}
