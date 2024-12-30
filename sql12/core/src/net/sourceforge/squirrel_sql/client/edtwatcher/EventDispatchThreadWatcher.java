package net.sourceforge.squirrel_sql.client.edtwatcher;

import javax.swing.*;
import java.util.Timer;
import java.util.TimerTask;

public final class EventDispatchThreadWatcher
{
   private static Timer s_timer;

   private EventDispatchThreadWatcher()
   {
      // No instances.
   }

   public static synchronized void start()
   {
      if (s_timer != null)
      {
         return;
      }
      s_timer = new Timer("EventDispatchThreadWatcher", true);

      TimerTask task = new TimerTask()
      {
         @Override
         public void run()
         {
            SwingUtilities.invokeLater(new EventQueueWorkingCheck());
         }
      };

      s_timer.schedule(task, 1000, 1000);
   }

   public static synchronized void stop()
   {
      if (s_timer != null)
      {
         s_timer.cancel();
         s_timer = null;
      }
   }


}