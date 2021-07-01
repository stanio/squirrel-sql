package net.sourceforge.squirrel_sql.client.gui.db;

import net.sourceforge.squirrel_sql.client.mainframe.action.ConnectToAliasCommand;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
@FunctionalInterface
public interface AliasListSelectionListener
{
   void selectionChanged(SQLAlias alias);

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
                  int option = JOptionPane.showConfirmDialog(parent,
                        "Open " + aliases.size() + " selected aliases?",
                        "Open multiple aliases", JOptionPane.OK_CANCEL_OPTION);
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
