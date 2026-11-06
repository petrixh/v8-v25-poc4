package de.symeda.sormas.backend.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import de.symeda.sormas.api.task.TaskContext;
import de.symeda.sormas.backend.AbstractUnitTest;
import de.symeda.sormas.backend.common.ConfigFacadeEjb.ConfigFacadeEjbLocal;

/**
 * Links in task notification mails point to the UI's routes: {@code <ui.url>/<view name>/<uuid>}, no {@code #!} fragment.
 */
class TaskFacadeEjbUiUrlTest extends AbstractUnitTest {

	private static final String UUID = "ABCDEF-GHIJKL-MNOPQR-STUVWXYZ";

	@InjectMocks
	private TaskFacadeEjb taskFacade;

	@Mock
	private ConfigFacadeEjbLocal configFacade;

	@Test
	void testUiUrlIsRoutePath() {
		when(configFacade.getUiUrl()).thenReturn("https://sormas.example.org/sormas-ui/");

		assertEquals("https://sormas.example.org/sormas-ui/cases/data/" + UUID, taskFacade.getUiUrl(TaskContext.CASE, UUID));
		assertEquals("https://sormas.example.org/sormas-ui/contacts/data/" + UUID, taskFacade.getUiUrl(TaskContext.CONTACT, UUID));
		assertEquals("https://sormas.example.org/sormas-ui/events/data/" + UUID, taskFacade.getUiUrl(TaskContext.EVENT, UUID));
		assertEquals("https://sormas.example.org/sormas-ui/travelEntries/data/" + UUID, taskFacade.getUiUrl(TaskContext.TRAVEL_ENTRY, UUID));
		assertEquals("https://sormas.example.org/sormas-ui/environments/data/" + UUID, taskFacade.getUiUrl(TaskContext.ENVIRONMENT, UUID));
	}

	@Test
	void testUiUrlWithoutTrailingSlash() {
		when(configFacade.getUiUrl()).thenReturn("https://sormas.example.org/sormas-ui");

		assertEquals("https://sormas.example.org/sormas-ui/cases/data/" + UUID, taskFacade.getUiUrl(TaskContext.CASE, UUID));
	}

	@Test
	void testNoUiUrl() {
		when(configFacade.getUiUrl()).thenReturn(null);

		assertNull(taskFacade.getUiUrl(TaskContext.CASE, UUID));
		assertNull(taskFacade.getUiUrl(TaskContext.GENERAL, UUID));
	}
}
