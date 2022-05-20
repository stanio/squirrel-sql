package net.sourceforge.squirrel_sql.plugins.derby;

import net.sourceforge.squirrel_sql.client.ApplicationListener;
import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.event.SessionAdapter;
import net.sourceforge.squirrel_sql.client.session.event.SessionEvent;
import net.sourceforge.squirrel_sql.fw.id.IIdentifier;
import net.sourceforge.squirrel_sql.fw.sql.ISQLDriver;
import net.sourceforge.squirrel_sql.fw.sql.SQLDriverManager;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.plugins.derby.DerbyPlugin.i18n;

import javax.swing.JOptionPane;
import java.lang.ref.WeakReference;
import java.sql.Driver;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * A session listener that shutdown Embedded Derby when session and
 * connection are already closed
 *
 * @author Alex Pivovarov
 * @author Stanimir Stamenkov
 */
class EmbeddedDerbySessionListener extends SessionAdapter implements ApplicationListener
{
   private static final String UNKNOWN_DBNAME = "<unknown>";

   private static ILogger s_log = LoggerController.createLogger(EmbeddedDerbySessionListener.class);

   private final DerbyPlugin _derbyPlugin;

   private Map<String, List<WeakReference<ISession>>> _embeddedSessions = new HashMap<>();

   // Reference to the JDBC driver so we could perform shutdown,
   // even when session references have been cleared.
   private volatile ISQLDriver _embeddedDriver;

   private volatile boolean _appShuttingDown;

   EmbeddedDerbySessionListener(DerbyPlugin derbyPlugin)
   {
      _derbyPlugin = Objects.requireNonNull(derbyPlugin);
   }

   private IApplication getApplication()
   {
      return _derbyPlugin.getApplication();
   }

   @Override
   public void saveApplicationState()
   {
      _appShuttingDown = true;
   }

   public synchronized void sessionStarted(ISession session)
   {
      if (isEmbeddedDerby(session))
      {
         _embeddedSessions.computeIfAbsent(getDatabaseName(session), k -> new ArrayList<>())
               .add(new WeakReference<>(session));

         _embeddedDriver = session.getDriver();
      }
   }

   @Override
   public void sessionClosed(SessionEvent evt)
   {
      if (flushEmbeddedSessions())
      {
         shutdownEmbeddedDerby();
      }
   }

   private boolean shouldDrop(String dbName)
   {
      if (_appShuttingDown)
      {
         return true;
      }

      int option = JOptionPane.showOptionDialog(null, // Application-modal
                                                MessageFormat.format(i18n.DROP_DB_QUESTION, dbName.replaceFirst("^memory:", "")),
                                                i18n.EMBEDDED_DERBY_TITLE, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                                                null, new Object[]{i18n.DROP_OPTION, i18n.KEEP_OPTION}, i18n.KEEP_OPTION);
      return option == JOptionPane.YES_OPTION;
   }

   private boolean flushEmbeddedSessions()
   {
      if (_embeddedSessions.isEmpty())
      {
         return false;
      }

      for (Iterator<Map.Entry<String, List<WeakReference<ISession>>>> iter = _embeddedSessions.entrySet().iterator(); iter.hasNext(); )
      {
         Map.Entry<String, List<WeakReference<ISession>>> entry = iter.next();
         if (flushEmbeddedSessions(entry.getValue()))
         {
            String dbName = entry.getKey();
            if (dbName.startsWith("memory:"))
            {
               if (shouldDrop(dbName))
               {
                  shutdownEmbeddedDerby(dbName, true);
                  iter.remove();
               }
            }
            else if (!dbName.equals(UNKNOWN_DBNAME))
            {
               shutdownEmbeddedDerby(dbName);
               iter.remove();
            }
         }
      }
      return _embeddedSessions.isEmpty();
   }

   private boolean flushEmbeddedSessions(List<WeakReference<ISession>> list)
   {
      if (list.isEmpty())
      {
         return false;
      }

      Iterator<WeakReference<ISession>> iter = list.iterator();
      while (iter.hasNext())
      {
         ISession session = iter.next().get();
         if (session == null || session.isClosed())
         {
            iter.remove();
         }
      }
      return list.isEmpty();
   }

   static boolean isEmbeddedDerby(final ISession session)
   {
      return session.getDriver().getDriverClassName().startsWith("org.apache.derby.jdbc.EmbeddedDriver");
   }

   private static String getDatabaseName(ISession session)
   {
      try
      {
         String jdbcURL = session.getMetaData().getURL();
         int semIdx = jdbcURL.indexOf(';');
         return (semIdx > 0) ? jdbcURL.substring("jdbc:derby:".length(), semIdx)
               : jdbcURL.substring("jdbc:derby:".length());
      }
      catch (SQLException e)
      {
         s_log.warn("Could not determine Embedded Derby database name", e);
         return UNKNOWN_DBNAME;
      }
   }

   /**
    * Shutdown Embedded Derby DB and reload JDBC Driver
    *
    * @author Alex Pivovarov
    */
   private void shutdownEmbeddedDerby()
   {
      shutdownEmbeddedDerby("");
   }

   /**
    * Shutdown a single Embedded Derby database.
    *
    * @param dbName the database name to shut down;
    */
   private void shutdownEmbeddedDerby(String dbName)
   {
      shutdownEmbeddedDerby(dbName, false);
   }

   private void shutdownEmbeddedDerby(String dbName, boolean drop)
   {
      try
      {
         ISQLDriver iSqlDr = _embeddedDriver;
         if (_embeddedDriver == null)
         {
            s_log.warn("shutdownEmbeddedDerby: driver reference is null");
            return;
         }
         //the code bellow is only for Embedded Derby Driver
         IIdentifier drId = iSqlDr.getIdentifier();
         SQLDriverManager sqlDrMan = getApplication().getSQLDriverManager();
         //Getting java.sql.Driver to run shutdown command
         Driver jdbcDr = sqlDrMan.getJDBCDriver(drId);
         //Shutdown Embedded Derby DB
         try
         {
            jdbcDr.connect("jdbc:derby:" + dbName + ";"
                                 + (drop ? "drop" : "shutdown") + "=true", new Properties());
         }
         catch (SQLException e)
         {
            //it is always thrown as said in Embedded Derby API.
            //So it is not error it is info
            if (Arrays.asList("08006", "XJ015").contains(e.getSQLState()))
            {
               getApplication().getMessageHandler().showMessage(e.getMessage());
            }
            else
            {
                getApplication().getMessageHandler().showErrorMessage(e);
            }
         }
         //Re-registering driver is necessary for Embedded Derby
         if (dbName.isEmpty())
         {
            sqlDrMan.registerSQLDriver(iSqlDr);
            _embeddedDriver = null;
         }
      }
      catch (Exception e)
      {
         s_log.error(e.getMessage(), e);
      }
   }

}
