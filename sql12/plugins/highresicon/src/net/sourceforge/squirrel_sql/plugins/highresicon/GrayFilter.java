package net.sourceforge.squirrel_sql.plugins.highresicon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.JToolBar;
import javax.swing.UIManager;

class GrayFilter
{
   @FunctionalInterface
   private static interface FilterFunction { int apply(int arg, int idx); }

   private static final int BASE_LEVEL = 192;

   private static float[] bufYCbCr = new float[4];

   public static Image getDisabledImage(Image base)
   {
      BufferedImage disabled = GrayFilter.bufferImage(base);
      if (disabled == null)
      {
         return null;
      }

      // Low or no contrast image indicates a (single) solid-color, or an
      // embossed (effect) image which should be blended additionally with
      // the UI background to ensure visibility.
      float[] contrast = GrayFilter.contrast(disabled);
      if (contrast[0] < 32 && Math.abs(getBackLuma() - contrast[1]) < 32)
      {
         GrayFilter.apply(disabled, contrast[1]);
      }
      return disabled;
   }


   static void apply(BufferedImage image, float avgFontLuma)
   {
      int width = image.getWidth(), height = image.getHeight();
      int[] pixels = filterRGB(image,
            (rgb, idx) -> GrayFilter.filterRGB(rgb, avgFontLuma));
      image.setRGB(0, 0, width, height, pixels, 0, width);
   }


   private static int filterRGB(int front, float avgFrontLuma)
   {
      int alpha = Colors.getAlpha(front);
      int back = backRGB;
      if (alpha == 0)
      {
         return back & 0xFFFFFF;
      }

      float backLuma = getBackLuma();
      Colors.rgbToYCbCr(front, bufYCbCr);
      float frontLuma = bufYCbCr[0];
      float lumaDiff = frontLuma - BASE_LEVEL;
      if (backLuma < 103) { // dark theme
         lumaDiff *= -1;
      }
      bufYCbCr[0] = Colors.clamp(backLuma + lumaDiff);
      return Colors.yCbCrToRgb(bufYCbCr);
   }

   private static int[] filterRGB(BufferedImage image, FilterFunction filter)
   {
      int width = image.getWidth(), height = image.getHeight();
      int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
      for (int i = 0; i < pixels.length; i++)
      {
         pixels[i] = filter.apply(pixels[i], i);
      }
      return pixels;
   }


   static BufferedImage bufferImage(Image image)
   {
      BufferedImage buffered = new BufferedImage(image.getWidth(null),
                                                 image.getHeight(null),
                                                 BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = buffered.createGraphics();
      try
      {
         return g.drawImage(image, 0, 0, null) ? buffered : null;
      }
      finally
      {
         g.dispose();
      }
   }

   static float[] contrast(BufferedImage image)
   {
      class Stats
      {
         float minLuma = Float.MAX_VALUE;
         float maxLuma = Float.MIN_VALUE;
         float avgLuma = 0;
         int   avgCount = 0;
      }
      Stats stats = new Stats();

      filterRGB(image, (rgb, idx) -> {
         int alpha = Colors.getAlpha(rgb);
         if (alpha > 128)
         {
            float luma = Colors.luma(rgb);
            if (luma < stats.minLuma) stats.minLuma = luma;
            if (luma > stats.maxLuma) stats.maxLuma = luma;
            if (stats.avgCount++ == 0) {
               stats.avgLuma = luma;
            } else {
               stats.avgLuma += (luma - stats.avgLuma) / stats.avgCount;
            }
         }
         return rgb;
      });

      return new float[] { Math.max(stats.maxLuma - stats.avgLuma,
                                    stats.avgLuma - stats.minLuma),
                           stats.avgLuma };
   }

   static float round(float val)
   {
      final float scale = (2 << 9);
      return Math.round(val * scale) / scale;
   }

   private static int backRGB;
   private static float[] backYCbCr = new float[4];

   private static float getBackLuma()
   {
      return backYCbCr[0];
   }

   static void setBackground(Color color)
   {
      final int lightGray = 0xFFC0C0C0;
      backRGB = (color == null) ? lightGray : color.getRGB();
      Colors.rgbToYCbCr(backRGB, backYCbCr);
   }

   static
   {
      Runnable updateBackground = () ->
      {
         JToolBar control = new JToolBar();
         control.setRollover(true);
         setBackground(control.getBackground());
      };
      UIManager.addPropertyChangeListener(evt ->
      {
         String propertyName = evt.getPropertyName();
         if (propertyName.equals("lookAndFeel")
               || propertyName.equals("ToolBarUI"))
         {
            updateBackground.run();
         }
      });
      updateBackground.run();
   }

}
