package net.sourceforge.squirrel_sql.plugins.highresicon;

import java.awt.Graphics;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthIcon;

class ScaledSynthIcon implements SynthIcon
{
   private SynthIcon icon;

   public ScaledSynthIcon(Object icon)
   {
      this.icon = (SynthIcon) icon;
   }

   @Override
   public void paintIcon(SynthContext context, Graphics g, int x, int y, int width, int height)
   {
      icon.paintIcon(context, g, x, y, width, height);
   }

   @Override
   public int getIconWidth(SynthContext context)
   {
      return IconScale.ceil(icon.getIconWidth(context));
   }

   @Override
   public int getIconHeight(SynthContext context)
   {
      return IconScale.ceil(icon.getIconHeight(context));
   }

   public static boolean isSynthIcon(Object icon)
   {
      return (icon instanceof SynthIcon);
   }

}
