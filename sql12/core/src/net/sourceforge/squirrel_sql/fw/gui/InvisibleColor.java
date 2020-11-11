package net.sourceforge.squirrel_sql.fw.gui;

import java.awt.Color;

/**
 * @see  GUIUtils#avoidOverlappingLabels(javax.swing.JTabbedPane)
 */
public class InvisibleColor extends Color
{
   private static final long serialVersionUID = 1L;

   private Color original;
   private boolean inherited;

   public InvisibleColor(Color original, boolean inherited)
   {
      super(original.getRGB() & 0x00FFFFFF, true);
      this.original = original;
      this.inherited = inherited;
   }

   public Color getOriginal()
   {
      return original;
   }

   public boolean isInherited()
   {
      return inherited;
   }

   public static InvisibleColor invisible(Color color, Color parent)
   {
      return (color instanceof InvisibleColor)
             ? (InvisibleColor) color
             : new InvisibleColor(color, color == parent);
   }

   public static Color visible(Color color)
   {
      return (color instanceof InvisibleColor)
             ? ((InvisibleColor) color).getOriginal()
             : color;
   }

   public static Color original(Color color)
   {
      if (color instanceof InvisibleColor)
      {
         InvisibleColor invisible = (InvisibleColor) color;
         return invisible.isInherited() ? null : invisible.getOriginal();
      }
      return color;
   }

}
