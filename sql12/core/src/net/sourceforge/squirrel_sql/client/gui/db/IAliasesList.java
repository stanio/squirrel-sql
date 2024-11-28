package net.sourceforge.squirrel_sql.client.gui.db;

import java.awt.event.MouseEvent;
import java.util.List;

/*
 * Copyright (C) 2004 Colin Bell
 * colbell@users.sourceforge.net
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

public interface IAliasesList extends IBaseList
{
	/**
	 * Return the <TT>SQLAlias</TT> that is currently selected.
    * @param evt
    */
	SQLAlias getSelectedAlias(MouseEvent evt);

   void sortAliases();

   void requestFocus();

   void deleteSelected();

   void modifySelected();

   boolean isEmpty();

   void goToAlias(SQLAlias aliasToGoTo);

   void colorSelected();

   void aliasChanged(SQLAlias sqlAlias);

   void goToAliasFolder(AliasFolder aliasFolder);

   SQLAlias getLeadSelectionValue();

   List<SQLAlias> getSelectedAliases();
}
