package net.sourceforge.squirrel_sql.client.gui.db;

import net.sourceforge.squirrel_sql.client.mainframe.action.ConnectToAliasCommand;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
               final SQLAlias item = list.getLeadSelectionValue();
               new ConnectToAliasCommand(item).executeConnect();
            }
         }
      };
   }
}
