package net.sourceforge.squirrel_sql.fw.gui.buttontabcomponent;

import net.sourceforge.squirrel_sql.client.Main;
import net.sourceforge.squirrel_sql.client.resources.SquirrelResources;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;

import javax.swing.*;
import java.awt.*;

public class SmallToolTipInfoButton
{

   private final SmallTabButton _btnShowToolTip;
   private String _infoText;
   private int _displayTimeMillis;

   public SmallToolTipInfoButton(String infoText)
   {
      this(infoText,5000);
   }

   public SmallToolTipInfoButton(String infoText, int displayTimeMillis)
   {
      _infoText = infoText;
      _displayTimeMillis = displayTimeMillis;

      Icon smallInfoIcon = getSmallInfoIcon();
      _btnShowToolTip = new SmallTabButton(null, smallInfoIcon, 0);
      //Dimension size = _btnShowToolTip.getPreferredSize();
      Dimension size = new Dimension(smallInfoIcon.getIconWidth(), smallInfoIcon.getIconWidth());
      _btnShowToolTip.setPreferredSize(size);
      _btnShowToolTip.setMinimumSize(size);
      _btnShowToolTip.setMaximumSize(size);

      GUIUtils.showExtraToolTipOnClick(_btnShowToolTip, true, _infoText, _displayTimeMillis);
   }


   public SmallTabButton getButton()
   {
      return _btnShowToolTip;
   }

   private Icon getSmallInfoIcon()
   {
      return Main.getApplication().getResources().getIcon(SquirrelResources.IImageNames.SMALL_INFO);
   }

   public void setInfoText(String infoText)
   {
      _infoText = infoText;
   }

   public void setEnabled(boolean b)
   {
      _btnShowToolTip.setEnabled(b);
   }
}
