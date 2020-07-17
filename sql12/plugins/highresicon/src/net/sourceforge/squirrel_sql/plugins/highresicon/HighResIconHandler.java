package net.sourceforge.squirrel_sql.plugins.highresicon;

import net.sourceforge.squirrel_sql.fw.resources.IconHandler;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.net.URL;

public class HighResIconHandler implements IconHandler
{
   @Override
   public ImageIcon createImageIcon(URL iconUrl)
   {
      return new SquirrelIcon(iconUrl);
   }

   @Override
   public void setDisabledIcon(Icon icon)
   {
      if (icon instanceof SquirrelIcon)
      {
         ((SquirrelIcon) icon).setDisabledIcon();
      }
   }

   @Override
   public int iconScale_round(int size)
   {
      return IconScale.round(size);
   }

   @Override
   public int iconScale_ceil(int size)
   {
      return IconScale.ceil(size);
   }
}
