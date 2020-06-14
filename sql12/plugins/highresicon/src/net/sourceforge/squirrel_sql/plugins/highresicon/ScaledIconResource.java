package net.sourceforge.squirrel_sql.plugins.highresicon;

import io.github.stanio.xbrz.awt.AwtXbrz;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.io.Serializable;
import java.util.Collections;

@SuppressWarnings("serial")
class ScaledIconResource implements UIResource, Serializable, Icon
{
   private Icon icon;
   private int iconWidth;
   private int iconHeight;

   private transient Image resolutionVariant;

   private ScaledIconResource(Icon icon)
   {
      this.icon = icon;
      // Avoid infinite loop when instance replaced back into UIDefaults,
      // for example with VistaMenuItemCheckIcon.
      this.iconWidth = icon.getIconWidth();
      this.iconHeight = icon.getIconHeight();
   }

   @Override
   public void paintIcon(Component c, Graphics g, int x, int y)
   {
      if (g instanceof Graphics2D)
      {
         paintIcon((Graphics2D) g, x, y, c);
      }
      else
      {
         icon.paintIcon(c, g, x, y);
      }
   }

   private void paintIcon(Graphics2D g, int x, int y, Component c)
   {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                          RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                          RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                          RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
      if (icon instanceof ImageIcon)
      {
         AffineTransform transform = g.getTransform();
         int width = (int) Math.ceil(getIconWidth() * transform.getScaleX());
         int height = (int) Math.ceil(getIconHeight() * transform.getScaleY());
         g2.drawImage(getResolutionVariant(width, height),
                      x, y, getIconWidth(), getIconHeight(), c);
      }
      else
      {
         g2.translate(x, y);
         g2.scale(IconScale.getFactor(), IconScale.getFactor());
         icon.paintIcon(c, g2, 0, 0);
      }
      g2.dispose();
   }

   private Image getResolutionVariant(int width, int height)
   {
      if (resolutionVariant == null
            || resolutionVariant.getWidth(null) < width
            || resolutionVariant.getHeight(null) < height)
      {
         resolutionVariant = AwtXbrz.scaleImage(((ImageIcon) icon).getImage(), width, height);
      }
      return resolutionVariant;
   }

   @Override
   public int getIconWidth()
   {
      return IconScale.ceil(iconWidth);
   }

   @Override
   public int getIconHeight()
   {
      return IconScale.ceil(iconHeight);
   }

   static void install()
   {
      updateUI();
      UIManager.addPropertyChangeListener(evt ->
      {
         if (evt.getPropertyName().equals("lookAndFeel"))
            updateUI();
      });
   }

   private static void updateUI()
   {
      boolean updated = false;
      UIDefaults defaults = UIManager.getLookAndFeelDefaults();
      for (Object key : Collections.list(defaults.keys()))
      {
         Object value = defaults.get(key);
         if (!(value instanceof Icon))
            continue;

         if (value instanceof ScaledIconResource)
         {
            System.out.println("Already scaled icon: " + key);
         }
         else if (value.getClass().getPackage().getName().startsWith("com.formdev.flatlaf"))
         {
            // FlatLaf icons are already scaled.
         }
         else
         {
            defaults.put(key, new ScaledIconResource((Icon) value));
            updated = true;
         }
      }
      if (updated)
      {
         for (Window w : Window.getWindows())
         {
            SwingUtilities.invokeLater(() -> SwingUtilities.updateComponentTreeUI(w));
            //SwingUtilities.updateComponentTreeUI(w);
         }
      }
   }

}
