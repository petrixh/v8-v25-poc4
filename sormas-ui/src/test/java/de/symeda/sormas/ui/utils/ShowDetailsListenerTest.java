package de.symeda.sormas.ui.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.ItemClickEvent;

import de.symeda.sormas.api.contact.ContactIndexDto;

/**
 * @see ShowDetailsListener
 */
public class ShowDetailsListenerTest {

	@Test
	@SuppressWarnings({
		"rawtypes",
		"unchecked" })
	public void testItemClickDoesNotCallHandlerWhenItemIsNull() throws InterruptedException {

		Consumer<ContactIndexDto> handler = mock(Consumer.class);
		String detailsColumnId = ContactIndexDto.UUID;
		ShowDetailsListener<ContactIndexDto> cut = new ShowDetailsListener<>(detailsColumnId, handler);

		ItemClickEvent<ContactIndexDto> event = mock(ItemClickEvent.class);
		ContactIndexDto item = mock(ContactIndexDto.class);

		// 1a. ColumnClick
		Column column = mock(Column.class);
		when(event.getColumn()).thenReturn(column);
		when(column.getKey()).thenReturn(detailsColumnId);
		cut.itemClick(event);
		verify(handler, never()).accept(null);

		// 1b. ColumnClick doing it when item is set
		when(event.getItem()).thenReturn(item);
		cut.itemClick(event);
		verify(handler).accept(item);

		reset(handler, event);

		// 2a. DoubleClick
		// was MouseEventDetails.isDoubleClick(): a Flow item click event carries the click count
		when(event.getClickCount()).thenReturn(2);
		cut.itemClick(event);
		verify(handler, never()).accept(null);

		// 2b. DoubleClick doing it when item is set
		// sleep to make sure the multi-click prevention doesn't prevent itemClick from being handled
		Thread.sleep(1000);
		when(event.getItem()).thenReturn(item);
		cut.itemClick(event);
		verify(handler).accept(item);
	}
}
