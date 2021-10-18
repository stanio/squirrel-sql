package net.sourceforge.squirrel_sql.plugins.highresicon;

import io.github.stanio.xbrz.awt.AwtXbrz;

import java.awt.Image;

class ImageScaler
{

   public static ImageScaler getInstance()
   {
      return new ImageScaler();
   }

   public Image scale(Image source, int destWidth, int destHeight)
   {
      return AwtXbrz.scaleImage(source, destWidth, destHeight);
   }

}
