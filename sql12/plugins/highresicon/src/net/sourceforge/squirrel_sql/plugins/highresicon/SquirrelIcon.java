package net.sourceforge.squirrel_sql.plugins.highresicon;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Drop-in replacement for {@code ImageIcon}.  Initializes a
 * {@code MultiResolutionImage} that provides better quality on hidpi screens.
 */
public class SquirrelIcon extends ImageIcon
{
   private boolean disabledIcon;

   public SquirrelIcon(URL location)
   {
      super(createImage(location));
   }

   private static Image createImage(URL location)
   {
      return CachedImage.of(location);
   }

   void setDisabledIcon()
   {
      if (disabledIcon)
         return;

      SquirrelIconImage image = (SquirrelIconImage) getImage();
      URI key = URI.create("disabled:" + image.getSourceLocation());
      setImage(CachedImage.of(key, () -> image.withGrayFilter()));
      disabledIcon = true;
   }

   private void writeObject(ObjectOutputStream out)
         throws IOException
   {
      Image image = getImage();
      if (image instanceof SquirrelIconImage)
      {
         out.writeObject(((SquirrelIconImage) image).getSourceLocation());
      }
      else
      {
         out.writeObject(null);
      }
      out.writeBoolean(disabledIcon);
   }

   private void readObject(ObjectInputStream in)
         throws IOException, ClassNotFoundException
   {
      URL baseLocation = (URL) in.readObject();
      if (baseLocation != null)
      {
         super.setImage(createImage(baseLocation));
      }
      if (in.readBoolean())
      {
         setDisabledIcon();
      }
   }


   private static class CachedImage extends WeakReference<Image>
   {
      private static Map<Object, CachedImage> cache = new HashMap<>();

      private static ReferenceQueue<Image> queue = new ReferenceQueue<>();

      private final Object key;

      private CachedImage(Object key, Image image)
      {
         super(image, queue);
         this.key = key;
         flush();
      }

      static Image of(URL location)
      {
         return of(location, () -> SquirrelIconImage.of(location));
      }

      static Image of(Object key, Supplier<Image> ctor)
      {
         Image cached = null;
         CachedImage ref = cache.get(key);
         if (ref != null)
         {
            cached = ref.get();
         }

         if (cached == null)
         {
            cached = ctor.get();
            cache.put(key, new CachedImage(key, cached));
         }
         return cached;
      }

      private static void flush()
      {
         CachedImage ref = (CachedImage) queue.poll();
         while (ref != null)
         {
            cache.remove(ref.key);
            ref = (CachedImage) queue.poll();
         }
      }
   }

}
