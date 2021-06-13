package net.sourceforge.squirrel_sql.client.gui.db;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

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
}
