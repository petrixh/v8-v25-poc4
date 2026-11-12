package de.symeda.sormas.ui.utils;

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;

import de.symeda.sormas.api.i18n.I18nProperties;

public class MenuBarHelper {

	public static MenuBar createDropDown(String captionKey, MenuBarItem... items) {
		return createDropDown(captionKey, Arrays.asList(items));
	}

	public static MenuBar createDropDown(String captionKey, List<MenuBarItem> items) {
		MenuBar menuBar = new MenuBar();
		menuBar.setId(captionKey);
		// Vaadin 8 showed an arrow after the caption of a menu item that opens a sub menu
		menuBar.addThemeVariants(MenuBarVariant.LUMO_DROPDOWN_INDICATORS);

		MenuItem mainItem = menuBar.addItem(I18nProperties.getCaption(captionKey));

		for (MenuBarItem item : items) {
			MenuItem dropDownItem;
			if (item.icon != null) {
				// was addItem(caption, icon, command)
				Span itemContent = new Span(item.icon.create(), new Text(item.caption));
				dropDownItem = mainItem.getSubMenu().addItem(itemContent, item.command);
			} else {
				dropDownItem = mainItem.getSubMenu().addItem(item.caption, item.command);
			}
			dropDownItem.setVisible(item.visible);
		}

		return menuBar;
	}

	public static class MenuBarItem {

		private String caption;
		private VaadinIcon icon;
		private ComponentEventListener<ClickEvent<MenuItem>> command;
		private boolean visible = true;

		public MenuBarItem(String caption, VaadinIcon icon, ComponentEventListener<ClickEvent<MenuItem>> command) {
			this.caption = caption;
			this.icon = icon;
			this.command = command;
		}

		public MenuBarItem(String caption, VaadinIcon icon, ComponentEventListener<ClickEvent<MenuItem>> command, Boolean visible) {
			this.caption = caption;
			this.icon = icon;
			this.command = command;
			this.visible = visible;
		}
	}
}
