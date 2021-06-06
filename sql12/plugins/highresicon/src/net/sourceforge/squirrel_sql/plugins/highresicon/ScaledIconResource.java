package net.sourceforge.squirrel_sql.plugins.highresicon;

import io.github.stanio.xbrz.awt.AwtXbrz;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIDefaults.ActiveValue;
import javax.swing.UIDefaults.LazyValue;
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
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
class ScaledIconResource implements UIResource, Serializable, Icon, Accessible, ScaledIcon
{
   static Map<Object, Object> activeKeys = new HashMap<>();

   private Icon icon;
   private Object key;

   private transient Image resolutionVariant;

   private ScaledIconResource(Icon icon, Object key)
   {
      this.icon = icon;
      this.key = key;
   }

   @Override
   public void paintIcon(Component c, Graphics g, int x, int y)
   {
      try
      {
         activeKeys.put(key, icon);
         if (g instanceof Graphics2D)
         {
            paintIcon((Graphics2D) g, x, y, c);
         }
         else
         {
            icon.paintIcon(c, g, x, y);
         }
      }
      finally
      {
         activeKeys.remove(key);
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
      try
      {
         activeKeys.put(key, icon);
         return IconScale.ceil(icon.getIconWidth());
      }
      finally
      {
         activeKeys.remove(key);
      }
   }

   @Override
   public int getIconHeight()
   {
      try
      {
         activeKeys.put(key, icon);
         return IconScale.ceil(icon.getIconHeight());
      }
      finally
      {
         activeKeys.remove(key);
      }
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
      UIDefaults defaults = UIManager.getLookAndFeelDefaults();
      UIDefaults updated = new UIDefaults();
      for (Map.Entry<Object, Object> entry : defaults.entrySet())
      {
         Object key = entry.getKey();
         Object value = entry.getValue();
         if (value instanceof ScaledIcon)
         {
            System.err.println("Already scaled icon: " + key);
            continue;
         }

         Object computed;
         if (value instanceof LazyValue)
         {
            computed = ScaledLazyProxy.of((LazyValue) value, key);
         }
         else if (value instanceof ActiveValue)
         {
            computed = ScaledActiveProxy.of((ActiveValue) value, key);
         }
         else
         {
            computed = scaledIcon(value, key);
         }

         if (computed != value)
         {
            updated.put(key, computed);
         }
      }
      if (!updated.isEmpty())
      {
         defaults.putAll(updated);
         for (Window w : Window.getWindows())
         {
            SwingUtilities.invokeLater(() -> SwingUtilities.updateComponentTreeUI(w));
            //SwingUtilities.updateComponentTreeUI(w);
         }
      }
   }


   static Object scaledIcon(Object value, Object key)
   {
      if (!(value instanceof Icon))
         return value;

      if (value.getClass().getPackage().getName().startsWith("com.formdev.flatlaf"))
      {
         return value;
      }
      else if (ScaledSynthIcon.isSynthIcon(value))
      {
         return new ScaledSynthIcon((Icon) value);
      }
      else
      {
         return new ScaledIconResource((Icon) value, key);
      }
   }


   static interface ScaledLazyProxy extends LazyValue, ScaledIcon
   {
      static ScaledLazyProxy of(LazyValue delegate, Object key)
      {
         return table -> scaledIcon(delegate.createValue(table), key);
      }
   }


   static interface ScaledActiveProxy extends ActiveValue, ScaledIcon
   {
      static ScaledActiveProxy of(ActiveValue delegate, Object key)
      {
         return table ->
         {
            // Avoid infinite recursion / stack overflow with Windows LAF.
            if (activeKeys.containsKey(key))
            {
               return activeKeys.get(key);
            }

            try
            {
               activeKeys.put(key, null);
               Object value = delegate.createValue(table);
               activeKeys.put(key, value);
               return scaledIcon(value, key);
            }
            finally
            {
               activeKeys.remove(key);
            }
         };
      }
   }


   @Override
   public AccessibleContext getAccessibleContext()
   {
      return (icon instanceof Accessible)
             ? ((Accessible) icon).getAccessibleContext()
             : new ImageIcon().getAccessibleContext();
   }


}


interface ScaledIcon { /* Tagging interface */ }
