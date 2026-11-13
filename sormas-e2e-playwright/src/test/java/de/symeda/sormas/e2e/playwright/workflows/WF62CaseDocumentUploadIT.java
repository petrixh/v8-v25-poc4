package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/** migration/workflows/WF62-case-document-upload.md. German UI; the document is written by the test (synthetic text). */
@DisplayName("WF62 Upload, download and delete a case document (NatUser, C2)")
@AppCopy(AppCopy.Config.C2)
class WF62CaseDocumentUploadIT extends WorkflowTest {

	private static final String NAME = "wf62-synthetic-document.txt";
	private static final String CONTENT = "Synthetic test document for workflow WF62.\nLine two: no real data.\n";
	private static final Pattern NO_DOCS = Pattern.compile("es gibt keine dokumente für diesen fall", Pattern.CASE_INSENSITIVE);

	Ui ui;
	Path src;

	@BeforeAll
	void open() throws IOException {
		Files.createDirectories(out);
		src = out.resolve(NAME);
		Files.writeString(src, CONTENT, StandardCharsets.UTF_8);
		ui = newUi();
		ui.loginDe("NatUser");
	}

	private String docsText() {
		String t = ui.bodyText();
		Matcher m = Pattern.compile("Dokumente").matcher(t);
		return m.find() ? t.substring(m.start(), Math.min(t.length(), m.start() + 200)).replaceAll("\\s+", " ") : "";
	}

	@Test
	@Order(1)
	@DisplayName("1. Open case SCAS01 from the case directory: its \"Dokumente\" panel says there are no documents for the case")
	void step1() {
		ui.openView("Fälle");
		ui.fillSearch("personLike", "Seedperson");
		ui.applyFilters();
		ui.settle(1000);
		ui.clickRow(ui.rowIndex("SCAS01"));
		assertTrue(ui.waitText("Alpha SEEDPERSON", 20_000), "case open");
		ui.waitMatch("Dokumente", 10_000);
		ui.settle(800);
		assertTrue(NO_DOCS.matcher(docsText()).find(), "no documents: " + docsText());
	}

	@Test
	@Order(2)
	@DisplayName("2. Upload the synthetic text document through \"Hochladen\": a success message shows and the document is listed by its name")
	void step2() {
		ui.pageUploadFile(Pattern.compile("hochladen", Pattern.CASE_INSENSITIVE), src);
		String n = ui.newestNotificationText(15_000);
		assertTrue(n != null && n.contains("Erfolgreich hochgeladen"), "success: " + n);
		ui.waitMatch(Pattern.quote(NAME), 10_000);
		assertTrue(docsText().contains(NAME), "listed: " + docsText());
	}

	@Test
	@Order(3)
	@DisplayName("3. Download it from the list: the downloaded file has the same name and the same content")
	void step3() throws IOException {
		Download d = ui.page.waitForDownload(new Page.WaitForDownloadOptions().setTimeout(30_000), () -> ui.documentRowButton(NAME, true));
		assertEquals(NAME, d.suggestedFilename(), "same name");
		Path target = out.resolve("downloaded-" + d.suggestedFilename());
		d.saveAs(target);
		assertEquals(CONTENT, Files.readString(target, StandardCharsets.UTF_8), "same content");
	}

	@Test
	@Order(4)
	@DisplayName("4. Delete it with the delete icon and confirm: the panel says again there are no documents for the case")
	void step4() {
		ui.documentRowButton(NAME, false);
		ui.waitMatch("Bestätigen|Ja|Löschen|Confirm", 10_000);
		ui.settle(400);
		ui.windowButtonLike(Pattern.compile("^(Bestätigen|Ja|Löschen|Confirm)", Pattern.CASE_INSENSITIVE));
		ui.noModal();
		ui.waitMatch("es gibt keine dokumente für diesen fall", 15_000);
		String docs = docsText();
		assertTrue(NO_DOCS.matcher(docs).find(), "no documents: " + docs);
		assertFalse(docs.contains(NAME), "not listed");
	}
}
