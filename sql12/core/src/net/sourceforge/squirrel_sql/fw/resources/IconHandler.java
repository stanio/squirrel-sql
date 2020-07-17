package net.sourceforge.squirrel_sql.fw.resources;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import java.net.URL;

public interface IconHandler
{
   ImageIcon createImageIcon(URL iconUrl);

   default void setDisabledIcon(Icon icon) {}

   int iconScale_round(int size);
   int iconScale_ceil(int size);
}
