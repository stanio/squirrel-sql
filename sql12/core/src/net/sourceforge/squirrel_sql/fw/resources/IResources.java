package net.sourceforge.squirrel_sql.fw.resources;

import java.util.MissingResourceException;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

/*
 * Copyright (C) 2011 Rob Manning
 * manningr@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

public interface IResources
{
	interface ActionProperties
	{
		String DISABLED_IMAGE = "disabledimage";

		String IMAGE = "image";

		String NAME = "name";

		String ROLLOVER_IMAGE = "rolloverimage";

		String TOOLTIP = "tooltip";
	}

	interface MenuProperties
	{
		String TITLE = "title";

		String MNEMONIC = "mnemonic";
	}

	interface MenuItemProperties extends MenuProperties
	{
		String ACCELERATOR = "accelerator";
	}

	interface Keys
	{
		String ACTION = "action";

		String MENU = "menu";

		String MENU_ITEM = "menuitem";
	}


	String ACCELERATOR_STRING = "SQuirreLAcceleratorString";




	KeyStroke getKeyStroke(Action action);

	JMenuItem addToPopupMenu(Action action, javax.swing.JPopupMenu menu) throws MissingResourceException;

	JCheckBoxMenuItem addToMenuAsCheckBoxMenuItem(Action action, JMenu menu) throws MissingResourceException;

	JCheckBoxMenuItem addToMenuAsCheckBoxMenuItem(Action action, JPopupMenu popupMenu);

	JMenuItem addToMenu(Action action, JMenu menu) throws MissingResourceException;

	JMenu createMenu(String menuKey) throws MissingResourceException;

	/**
	 * Setup the passed action from the resource bundle.
	 * 
	 * @param action
	 *        Action being setup.
	 *
	 * @throws IllegalArgumentException
	 *         thrown if <TT>null</TT> <TT>action</TT> passed.
    * @return
	 */
	Action setupAction(Action action);

	Icon getIcon(String keyName);

	Icon getIcon(Class<?> objClass, String propName);

	Icon getIcon(String keyName, String propName);

	String getString(String key);

	void configureMenuItem(Action action, JMenuItem item) throws MissingResourceException;

}