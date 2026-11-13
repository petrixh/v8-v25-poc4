package de.symeda.sormas.e2e.playwright.workflows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import de.symeda.sormas.e2e.playwright.support.AppCopy;
import de.symeda.sormas.e2e.playwright.support.MailSink;
import de.symeda.sormas.e2e.playwright.support.Ui;
import de.symeda.sormas.e2e.playwright.support.WorkflowTest;

/**
 * migration/workflows/WF49-task-notification-mail.md. The mail goes to the copy's own mail sink. Admin may only view
 * tasks, so step 2 runs as NatUser. Accepted difference (decision M0 D5): the mail links to the case with the new route
 * form /sormas-ui/cases/data/&lt;uuid&gt; (the old app used #!cases/data/&lt;uuid&gt;).
 */
@DisplayName("WF49 Task reassignment sends a notification mail (admin, NatUser; C1)")
@AppCopy(AppCopy.Config.C1)
class WF49TaskNotificationMailIT extends WorkflowTest {

	private static final String EMAIL = "survsup@example.invalid";
	private static final Pattern SAVE = Pattern.compile("^save$", Pattern.CASE_INSENSITIVE);
	Ui admin;
	Ui nat;
	MailSink mail;
	JsonObject message;

	@BeforeAll
	void open() {
		admin = newUi();
		admin.login("admin");
		mail = new MailSink(copy);
	}

	@Test
	@Order(1)
	@DisplayName("1. As admin, Users, SurvSup: set the e-mail address survsup@example.invalid; save: saved")
	void step1() {
		Ui ui = admin;
		ui.openView("Users");
		assertEquals(0, mail.messages().get("total").getAsInt(), "mail sink empty");
		ui.clickRow(ui.rowIndex(" SurvSup "));
		ui.settle(2000);
		Locator em = ui.windowField("Email", false);
		assertEquals("", em.inputValue(), "no e-mail before");
		em.fill(EMAIL);
		em.press("Tab");
		ui.settle(1000);
		ui.windows().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(SAVE)).click();
		ui.settle(2500);
		ui.noModal();
		assertEquals(0, ui.windowsAfterClose(), "windows left");
		ui.clickRow(ui.rowIndex(" SurvSup "));
		ui.settle(2000);
		assertEquals(EMAIL, ui.windowField("Email", false).inputValue(), "e-mail after");
		ui.closeWindow();
		ui.noModal();
	}

	@Test
	@Order(2)
	@DisplayName("2. As NatUser, tasks, STSK01 (case investigation of SCAS02): assignee SurvOff to SurvSup; save")
	void step2() {
		nat = newUi();
		Ui ui = nat;
		ui.login("NatUser");
		ui.openView("Tasks");
		ui.settle(1500);
		ui.fillSearch("freeText", "Bravo");
		ui.applyFilters();
		ui.settle(2000);
		assertEquals(2, ui.rowTexts().size(), "STSK01 and the case investigation task the app made");
		ui.clickRow(ui.rowIndex("Synthetic seed task"));
		ui.settle(2000);
		assertTrue(ui.windows().getByText("Edit task", new Locator.GetByTextOptions().setExact(true)).isVisible(), "edit form");
		Locator as = ui.windowField("Assigned to", true);
		assertTrue(as.inputValue().startsWith("Surveillance OFFICER"), "assignee before: " + as.inputValue());
		ui.comboPick(as, "Surveillance S", Pattern.compile("^Surveillance SUPERVISOR"), true, 800, 1000);
		ui.windows().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(SAVE)).click();
		ui.settle(2500);
		ui.noModal();
		assertEquals(0, ui.windowsAfterClose(), "windows left");
		String r = ui.rowTexts().stream().filter(x -> x.contains("Synthetic seed task")).findFirst().orElse("");
		assertTrue(Pattern.compile("Surveillance SUPERVISOR[\\s\\S]*Synthetic seed task").matcher(r).find() && !r.contains("OFFICER"), "assignee: " + r);
	}

	@Test
	@Order(3)
	@DisplayName("3. The mail sink: within a minute one mail to survsup@example.invalid")
	void step3() throws InterruptedException {
		JsonObject msgs = mail.awaitMessages(1, 60_000);
		assertEquals(1, msgs.get("total").getAsInt(), "mails");
		JsonObject m = msgs.getAsJsonArray("messages").get(0).getAsJsonObject();
		assertEquals(EMAIL, m.getAsJsonArray("To").get(0).getAsJsonObject().get("Address").getAsString(), "to");
		message = mail.message(m.get("ID").getAsString());
	}

	@Test
	@Order(4)
	@DisplayName("4. The mail names the task type and the case (SCAS02) and links to the case (new route form, decision M0 D5)")
	void step4() {
		String text = Ui.norm(message.get("Text").getAsString());
		assertTrue(Pattern.compile("case investigation", Pattern.CASE_INSENSITIVE).matcher(text).find(), "task type");
		assertTrue(text.contains("Case SCAS02"), "case named by its short id");
		Matcher link = Pattern.compile("https?://\\S*/sormas-ui/(\\S+)").matcher(text);
		assertTrue(link.find(), "link: " + text);
		assertEquals("cases/data/<uuid>", link.group(1).replaceFirst("^(cases/data/)\\S+", "$1<uuid>"), "link route");
		// Data check: STSK01 assigned to SurvSup by NatUser; SurvSup has the e-mail address
		assertEquals(
			"SurvSup|NatUser",
			copy.sql(
				"SELECT a.username, b.username FROM task t JOIN users a ON a.id = t.assigneeuser_id JOIN users b ON b.id = t.assignedbyuser_id WHERE t.uuid LIKE 'STSK01%'"));
		assertEquals(EMAIL, copy.sql("SELECT useremail FROM users WHERE username = 'SurvSup'"));
		assertTrue(List.of(message.get("Subject").getAsString()).stream().noneMatch(String::isBlank), "subject");
	}
}
