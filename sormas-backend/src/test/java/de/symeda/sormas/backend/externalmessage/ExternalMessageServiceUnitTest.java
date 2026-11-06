package de.symeda.sormas.backend.externalmessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.symeda.sormas.api.externalmessage.ExternalMessageCriteria;
import de.symeda.sormas.api.externalmessage.ExternalMessageStatus;

@ExtendWith(MockitoExtension.class)
public class ExternalMessageServiceUnitTest {

	@Mock
	private CriteriaBuilder cb;
	@Mock
	private Root<ExternalMessage> labMessage;
	@Mock
	private ExternalMessageCriteria criteria;
	@Mock
	private Path<Object> objectPath;
	@Mock
	private Predicate predicate;

	@Test
	public void testBuildCriteriaFilter() {

		ExternalMessageService sut = new ExternalMessageService();
		ExternalMessageStatus status = ExternalMessageStatus.PROCESSED;

		when(criteria.getExternalMessageStatus()).thenReturn(status);
		when(labMessage.get(ExternalMessage.STATUS)).thenReturn(objectPath);

		when(cb.equal(objectPath, status)).thenReturn(predicate);

		Predicate result = sut.buildCriteriaFilter(cb, labMessage, criteria);

		assertEquals(predicate, result);
	}

}
