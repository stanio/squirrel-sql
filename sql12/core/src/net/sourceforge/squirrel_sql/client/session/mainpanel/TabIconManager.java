package net.sourceforge.squirrel_sql.client.session.mainpanel;

import net.sourceforge.squirrel_sql.client.Main;
import net.sourceforge.squirrel_sql.client.action.ActionCollection;
import net.sourceforge.squirrel_sql.client.session.action.ToggleCurrentSQLResultTabAnchoredAction;
import net.sourceforge.squirrel_sql.client.session.action.ToggleCurrentSQLResultTabStickyAction;

import javax.swing.Action;
import javax.swing.Icon;

public class TabIconManager
{

   private final Icon _stickyIcon;
   private final Icon _anchorIcon;

   public TabIconManager()
   {
      ActionCollection actionCollection = Main.getApplication().getActionCollection();
      _stickyIcon = (Icon) actionCollection.get(ToggleCurrentSQLResultTabStickyAction.class).getValue(Action.SMALL_ICON);

      _anchorIcon = (Icon) actionCollection.get(ToggleCurrentSQLResultTabAnchoredAction.class).getValue(Action.SMALL_ICON);
   }

   Icon getStickyIcon()
   {
      return _stickyIcon;
   }

   Icon getAnchorIcon()
   {
      return _anchorIcon;
   }

}
