package net.sourceforge.squirrel_sql.client.gui.db;

import net.sourceforge.squirrel_sql.client.mainframe.action.ConnectToAliasCommand;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
@FunctionalInterface
public interface AliasListSelectionListener
{
   void selectionChanged(SQLAlias alias);

   default FocusListener getFocusListener(IAliasesList list)
   {
      return new FocusListener()
      {
         @Override public void focusLost(FocusEvent evt)
         {
            selectionChanged(null);
         }

         @Override public void focusGained(FocusEvent evt)
         {
            selectionChanged(list.getLeadSelectionValue());
         }
      };
   }

   default KeyListener getActionKeyListener(IAliasesList list)
   {
      return new KeyAdapter()
      {
         @Override public void keyPressed(KeyEvent evt)
         {
            if (evt.getKeyCode() == KeyEvent.VK_ENTER)
            {
               List<SQLAlias> aliases = list.getSelectedAliases();
               if (aliases.size() > 1)
               {
                  Component parent = (evt.getSource() instanceof Component)
                                     ? SwingUtilities.getWindowAncestor((Component) evt.getSource())
                                     : null;
                  StringBuilder msg = new StringBuilder();
                  msg.append("<html>Open " + aliases.size() + " selected aliases?")
                        .append("<ul style='margin-left: 14; padding-left: 0'>");
                  for (SQLAlias item : aliases)
                  {
                     msg.append("<li>")
                           .append(item.getName().replace("&", "&amp;").replace("<", "&lt;"))
                           .append("</li>");
                  }
                  msg.append("</ul></html>");
                  int option = JOptionPane.showConfirmDialog(parent,
                        msg.toString(), "Open multiple aliases",
                        JOptionPane.OK_CANCEL_OPTION);
                  if (option != JOptionPane.OK_OPTION)
                  {
                     return;
                  }
               }

               for (SQLAlias item : aliases)
               {
                  new ConnectToAliasCommand(item).executeConnect();
               }
            }
         }
      };
   }
}
