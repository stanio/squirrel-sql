package net.sourceforge.squirrel_sql.client.edtwatcher;

import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;


final class EventQueueWorkingCheck implements Runnable
{

   static final int MAX_ACCEPTED_EDT_DELAY_TIME = 2000;

   private static ILogger s_log = LoggerController.createLogger(EventQueueWorkingCheck.class);

   private final long m_startTime;

   EventQueueWorkingCheck()
   {
      m_startTime = System.currentTimeMillis();
   }

   @Override
   public void run()
   {
      if (System.currentTimeMillis() - m_startTime > MAX_ACCEPTED_EDT_DELAY_TIME)
         writeLog();
   }

   private void writeLog()
   {
      StringWriter sw = new StringWriter();

      try (PrintWriter pw = new PrintWriter(sw))
      {
         ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
         ThreadInfo[] threadInfo = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 1000);

         pw.println("----------------------------------------------------------------------------------------------------------------");
         pw.println("-- Detected Swing-EDT event running for longer than " + MAX_ACCEPTED_EDT_DELAY_TIME + " millis. Writing Stack dump:");
         pw.println("-- STACK DUMP BEGIN");


         for( ThreadInfo info : threadInfo )
         {
            if(null == info)
            {
               continue;
            }

            pw.println("Threadname: " + info.getThreadName());
            pw.println("ThreadId: " + info.getThreadId());
            pw.println("Threadstate: " + info.getThreadState());

            for( StackTraceElement stackTraceElement : info.getStackTrace() )
            {
               pw.println("    at " +  stackTraceElement);
            }

            pw.println();
            pw.println();
         }

         pw.println("-- STACK DUMP END");
         pw.println("----------------------------------------------------------------------------------------------------------------");

      }

      s_log.warn(sw.toString());
   }

}
