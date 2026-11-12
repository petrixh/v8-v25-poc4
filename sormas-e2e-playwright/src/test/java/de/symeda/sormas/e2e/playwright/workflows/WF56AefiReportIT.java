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

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/**
 * migration/workflows/WF56-aefi-report.md. German UI; most AEFI captions are English (B036). The report needs a reporting
 * ID number and the primary suspect vaccination; the investigations section appears only for a serious report.
 */
@DisplayName("WF56 Report an adverse event following immunization and investigate it (NatUser, C2)")
@AppCopy(AppCopy.Config.C2)
class WF56AefiReportIT extends WorkflowTest {

	Ui ui;
	String report;
	String vacc;
	String onset;

	@BeforeAll
	void open() {
		report = de(0);
		vacc = de(-35);
		onset = de(-34);
		ui = newUi();
		ui.loginDe("NatUser");
	}

	private Locator tb(String regex) {
		return ui.field(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
	}

	private Locator wtb(String regex) {
		return ui.windowFieldLike(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
	}

	private void opt(String regex, String value) {
		ui.optionAfterCaption(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), value);
	}

	private void choose(Locator input, String text) {
		ui.comboToggleChoose(input, text);
	}

	private void save() {
		ui.pageButtonLike(Pattern.compile("^Speichern$"));
	}

	private String note() {
		return ui.newestNotificationText(15_000);
	}

	private String body() {
		return ui.bodyText().replaceAll("\\s+", " ");
	}

	private static String section(String t, String from, int len) {
		int i = t.indexOf(from);
		return i < 0 ? "" : t.substring(i, Math.min(t.length(), i + len));
	}

	private void pickSuspectVaccine() {
		ui.clickEntry("Select Suspect Vaccination");
		ui.waitText("COMIRNATY", 10_000);
		ui.windowClickText("COMIRNATY");
		ui.settle(300);
		ui.windowButtonLike(Pattern.compile("^Speichern$"));
		ui.noModal();
	}

	@Test
	@Order(1)
	@DisplayName("1. Open Immunisierungen and SIMM01: the view shows the means Vaccination and a side section for adverse events")
	void step1() {
		ui.openView("Immunisierungen");
		ui.waitRow("SIMM01", 10_000);
		assertTrue(ui.rowTextWith("SIMM01").contains("Golf"), "listed");
		ui.clickInRow(ui.rowIndex("SIMM01"), "SIMM01", true);
		assertTrue(ui.waitText("Golf SEEDPERSON (* 1.12.1995) (SIMM01)", 20_000), "title");
		String t = body();
		assertEquals("Impfung", tb("^Mittel der Immunisierung").inputValue(), "means");
		assertEquals("COVID-19", tb("^Krankheit").inputValue(), "disease");
		assertTrue(section(t, "Adverse events", 400).contains("No adverse events have been created for this immunization"), "aefi section");
		assertFalse(Pattern.compile("COMIRNATY|\\d\\d\\.\\d\\d\\.\\d{4}").matcher(section(t, "IMPFSTOFFDOSIS", 120)).find(), "no vaccinations yet");
	}

	@Test
	@Order(2)
	@DisplayName("2. Add a vaccination (35 days before, COMIRNATY) and save the immunization: the vaccination is listed and a new adverse event is possible")
	void step2() {
		ui.clickEntry("Neuer Eintrag");
		ui.waitText("Impfung", 10_000);
		assertTrue(ui.windowText().contains("Impfung"), "window title");
		ui.fillDate(wtb("^Impfdatum"), vacc, 400);
		choose(wtb("^Impfstoffname"), "COMIRNATY");
		assertEquals("BioNTech/Pfizer", wtb("^Impfstoffhersteller").inputValue(), "manufacturer");
		ui.windowButtonLike(Pattern.compile("^Erledigt$"));
		ui.noModal();
		assertEquals(0, ui.windowCount(), "window closed");
		save();
		String n = note();
		assertTrue(n != null && n.contains("Immunisierungsdaten gespeichert"), "saved: " + n);
		ui.settle(800);
		ui.dismissNotifications();
		assertTrue(section(body(), "Impfungen", 300).contains(vacc + " COMIRNATY BioNTech/Pfizer"), "vaccination listed");
		assertTrue(ui.buttonEnabled(Pattern.compile("new adverse event", Pattern.CASE_INSENSITIVE)), "new adverse event enabled");
		ui.page.getByText("COMIRNATY", new Page.GetByTextOptions().setExact(true)).first().scrollIntoViewIfNeeded();
	}

	@Test
	@Order(3)
	@DisplayName("3. New adverse event report: fever, onset a day after the vaccination, Serious (hospitalization), outcome Recovered; save: listed on the immunization")
	void step3() {
		ui.pageButtonLike(Pattern.compile("new adverse event", Pattern.CASE_INSENSITIVE));
		ui.waitText("Select Suspect Vaccination", 20_000);
		ui.settle(800);
		assertEquals(report, tb("^date of report").inputValue(), "report date preset");
		assertTrue(section(body(), "Vaccinations", 300).contains("BioNTech/Pfizer, COMIRNATY, " + vacc), "vaccination offered");
		tb("^reporting id number").fill("WF-AEFI-01");
		pickSuspectVaccine();
		opt("^Fever >38degC$", "YES");
		ui.fillDate(tb("^date and time aefi started"), onset, 400);
		tb("^describe aefi").fill("Fever the day after vaccination");
		opt("^serious\\*?$", "JA");
		choose(tb("^reason for serious"), "Hospitalization");
		opt("^outcome\\*?$", "RECOVERED");
		save();
		String n = note();
		assertTrue(n != null && n.contains("Adverse event saved"), "saved: " + n);
		ui.waitText("Investigations", 15_000);
		ui.dismissNotifications();
		ui.clickEntry("Immunisierung");
		ui.waitText("Adverse events", 15_000);
		ui.waitMatch("SERIOUS COMIRNATY", 10_000);
		String s = section(body(), "Adverse events", 200);
		assertTrue(Pattern.compile("SERIOUS COMIRNATY Vaccination date: ").matcher(s).find() && s.contains(vacc) && Pattern.compile("FEVER >38DEGC", Pattern.CASE_INSENSITIVE).matcher(s).find(), "listed on the immunization: " + s);
	}

	@Test
	@Order(4)
	@DisplayName("4. Adverse events directory: the seeded report and the new one are listed, the new one for Golf Seedperson")
	void step4() {
		ui.openView("Adverse Events");
		assertTrue(ui.waitText("Adverse Events Directory", 15_000), "title");
		ui.settle(800);
		List<String> rows = ui.rowTexts();
		assertEquals(2, rows.size(), "rows: " + rows);
		String mine = rows.stream().filter(r -> r.contains("Golf Seedperson")).findFirst().orElse("");
		assertTrue(mine.contains("Golf Seedperson COVID-19") && mine.contains("Ja COMIRNATY Recovered " + vacc + " " + report + " " + onset + " Fever >38degC"), "row: " + mine);
	}

	@Test
	@Order(5)
	@DisplayName("5. Open the report, start an investigation and save it: the report's investigations list shows it; the data is stored")
	void step5() {
		ui.clickRow(ui.rowIndex("Golf"));
		assertTrue(ui.waitText("No investigations have been created for this adverse event", 20_000), "no investigations yet");
		ui.pageButtonLike(Pattern.compile("new investigation", Pattern.CASE_INSENSITIVE));
		ui.waitText("Select Suspect Vaccination", 20_000);
		ui.settle(800);
		assertEquals(report, tb("^date of report").inputValue(), "report date preset");
		tb("^investigation case id").fill("WF-INV-01");
		opt("^place of vaccination", "GOVERNMENT HEALTH FACILITY");
		opt("^vaccination in", "ROUTINE");
		ui.fillDate(tb("^date of investigation"), report, 400);
		ui.fillDate(tb("^date of filling this form"), report, 400);
		opt("^this report is", "FIRST");
		pickSuspectVaccine();
		opt("^type of site", "FIXED");
		ui.fillDate(tb("^date & time of first\\/key symptom"), onset, 400);
		opt("^status on the date of investigation", "RECOVERED COMPLETELY");
		opt("^investigation status\\*?$", "DONE");
		opt("^classification of aefi\\*?$", "UNDETERMINED");
		save();
		String n = note();
		assertTrue(n != null && n.contains("Adverse event investigation saved"), "saved: " + n);
		ui.settle(800);
		ui.dismissNotifications();
		ui.clickEntry(Pattern.compile("^Adverse event$", Pattern.CASE_INSENSITIVE));
		ui.waitMatch("WF-INV-01", 15_000);
		String s = section(body(), "Investigations", 200);
		assertTrue(s.contains("WF-INV-01 Investigation Date: " + report)
			&& Pattern.compile("STAGE: FIRST", Pattern.CASE_INSENSITIVE).matcher(s).find()
			&& Pattern.compile("CLASSIFICATION: UNDETERMINED", Pattern.CASE_INSENSITIVE).matcher(s).find(), "investigation listed: " + s);
		// Data check: SIMM01 has one vaccination; one adverse event report and one investigation exist for it
		assertEquals(
			"1",
			copy.sql("SELECT count(*) FROM vaccination v JOIN immunization i ON i.id = v.immunization_id WHERE i.uuid LIKE 'SIMM01%'"));
		assertEquals(
			"1",
			copy.sql(
				"SELECT count(*) FROM adverseeventsfollowingimmunization a JOIN immunization i ON i.id = a.immunization_id WHERE i.uuid LIKE 'SIMM01%'"));
		assertEquals(
			"1",
			copy.sql(
				"SELECT count(*) FROM adverseeventsfollowingimmunizationinvestigation v JOIN adverseeventsfollowingimmunization a ON a.id = v.adverseeventsfollowingimmunization_id"
					+ " JOIN immunization i ON i.id = a.immunization_id WHERE i.uuid LIKE 'SIMM01%'"));
	}
}
