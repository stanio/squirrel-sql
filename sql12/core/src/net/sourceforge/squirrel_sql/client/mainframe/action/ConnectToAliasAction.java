package net.sourceforge.squirrel_sql.client.mainframe.action;
/*
 * Copyright (C) 2001-2004 Colin Bell
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

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.gui.db.IAliasesList;
import net.sourceforge.squirrel_sql.client.gui.db.SQLAlias;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

public class ConnectToAliasAction extends AliasAction
{
   /**
    * List of all the users aliases.
    */
   private final IAliasesList _aliases;

   /**
    * Ctor specifying the list of aliases.
    *
    * @param	app		Application API.
    * @param	list	List of <TT>SQLAlias</TT> objects.
    */
   public ConnectToAliasAction(IApplication app, IAliasesList list)
   {
      super(app);
      _aliases = list;
   }

   /**
    * Perform this action. Retrieve the current alias from this list and
    * connect to it.
    *
    * @param	evt		The current event.
    */
   public void actionPerformed(ActionEvent evt)
   {
      moveToFrontAndSelectAliasFrame();      
      final List<SQLAlias> items = _aliases.getSelectedAliases();
      if (items.size() > 1)
      {
         Component parent = (evt.getSource() instanceof Component)
                            ? SwingUtilities.getWindowAncestor((Component) evt.getSource())
                            : null;
         StringBuilder msg = new StringBuilder("<html>Open ")
               .append(items.size()).append(" selected aliases?")
               .append("<ul style='margin-left: 14; padding-left: 0'>");
         for (SQLAlias alias : items)
         {
            msg.append("<li>")
                  .append(alias.getName().replace("&", "&amp;").replace("<", "&lt;"))
                  .append("</li>");
         }
         msg.append("</ul></html>");
         int option = JOptionPane.showConfirmDialog(parent,
               msg, "Open multiple aliases", JOptionPane.OK_CANCEL_OPTION);
         if (option != JOptionPane.OK_OPTION)
         {
            return;
         }
      }

      for (SQLAlias alias : items)
      {
         new ConnectToAliasCommand(alias).executeConnect();
      }
   }
}
