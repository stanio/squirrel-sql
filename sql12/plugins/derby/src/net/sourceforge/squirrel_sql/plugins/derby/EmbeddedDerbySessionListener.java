package net.sourceforge.squirrel_sql.plugins.derby;

import net.sourceforge.squirrel_sql.client.ApplicationListener;
import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.event.SessionAdapter;
import net.sourceforge.squirrel_sql.client.session.event.SessionEvent;
import net.sourceforge.squirrel_sql.fw.sql.ISQLDriver;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.plugins.derby.DerbyPlugin.i18n;

import javax.swing.JOptionPane;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
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

   private final EmbeddedSessions _embeddedSessions = new EmbeddedSessions();

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
         ISQLDriver sqlDriver = session.getDriver();
         Driver jdbcDriver = getApplication()
               .getSQLDriverManager().getJDBCDriver(sqlDriver.getIdentifier());
         Map<String, List<WeakReference<ISession>>> activeSessions =
               _embeddedSessions.computeIfAbsent(jdbcDriver, drv -> new HashMap<>());
         activeSessions.computeIfAbsent(getDatabaseName(session), k -> new ArrayList<>())
                       .add(new WeakReference<>(session));
      }
   }

   @Override
   public void reconnected(SessionEvent evt)
   {
      sessionStarted(evt.getSession());
   }

   @Override
   public void connectionClosedForReconnect(SessionEvent evt)
   {
      if (isEmbeddedDerby(evt.getSession()))
      {
         ISession session = evt.getSession();
         Driver oldDriver = _embeddedSessions.removeSesion(session);
         Driver newDriver = getApplication().getSQLDriverManager()
               .getJDBCDriver(session.getDriver().getIdentifier());
         if (oldDriver != newDriver)
         {
            flushEmbeddedSessions();
         }
      }
   }

   @Override
   public void allSessionsClosed()
   {
      if (!_embeddedSessions.isEmpty())
      {
         s_log.warn("Not all embedded sessions have been shut down properly: "
                  + _embeddedSessions);
      }
   }

   @Override
   public void sessionClosed(SessionEvent evt)
   {
      flushEmbeddedSessions();
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

   private void flushEmbeddedSessions()
   {
      if (_embeddedSessions.isEmpty())
      {
         return;
      }

      Iterator<Map.Entry<Driver, Map<String, List<WeakReference<ISession>>>>> iter = _embeddedSessions.entrySet().iterator();
      while (iter.hasNext())
      {
         Map.Entry<Driver, Map<String, List<WeakReference<ISession>>>> entry = iter.next();
         Driver jdbcDr = entry.getKey();
         Map<String, List<WeakReference<ISession>>> activeSessions = entry.getValue();

         flushEmbeddedSessions(jdbcDr, activeSessions.entrySet().iterator());

         if (activeSessions.isEmpty())
         {
            iter.remove();
            shutdownEmbeddedDerby(jdbcDr);
         }
      }
   }

   private void flushEmbeddedSessions(Driver jdbcDr,
         Iterator<Map.Entry<String, List<WeakReference<ISession>>>> activeSessions)
   {
      while (activeSessions.hasNext())
      {
         Map.Entry<String, List<WeakReference<ISession>>> entry = activeSessions.next();
         if (flushEmbeddedSessions(entry.getValue()))
         {
            String dbName = entry.getKey();
            if (dbName.startsWith("memory:"))
            {
               if (shouldDrop(dbName))
               {
                  shutdownEmbeddedDerby(jdbcDr, dbName, true);
                  activeSessions.remove();
               }
            }
            else if (!dbName.equals(UNKNOWN_DBNAME))
            {
               shutdownEmbeddedDerby(jdbcDr, dbName);
               activeSessions.remove();
            }
         }
      }
   }

   private boolean flushEmbeddedSessions(Collection<WeakReference<ISession>> sessionReferences)
   {
      if (sessionReferences.isEmpty())
      {
         return false;
      }

      Iterator<WeakReference<ISession>> iter = sessionReferences.iterator();
      while (iter.hasNext())
      {
         ISession session = iter.next().get();
         if (session == null || session.isClosed())
         {
            iter.remove();
         }
      }
      return sessionReferences.isEmpty();
   }

   static boolean isEmbeddedDerby(final ISession session)
   {
      String driverClassName = session.getDriver().getDriverClassName();
      return Arrays.asList("org.apache.derby.jdbc.AutoloadedDriver",
            "org.apache.derby.jdbc.EmbeddedDriver").contains(driverClassName);
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
   private void shutdownEmbeddedDerby(Driver jdbcDr)
   {
      shutdownEmbeddedDerby(jdbcDr, "");
   }

   /**
    * Shutdown a single Embedded Derby database.
    *
    * @param dbName the database name to shut down;
    */
   private void shutdownEmbeddedDerby(Driver jdbcDr, String dbName)
   {
      shutdownEmbeddedDerby(jdbcDr, dbName, false);
   }

   private void shutdownEmbeddedDerby(Driver jdbcDr, String dbName, boolean drop)
   {
      //Shutdown Embedded Derby DB
      Properties params = new Properties();
      params.setProperty(drop ? "drop" : "shutdown", "true");
      try (Connection con = jdbcDr.connect("jdbc:derby:" + dbName, params))
      {
         SQLWarning warning = con.getWarnings();
         while (warning != null)
         {
            s_log.warn("", warning);
            warning = warning.getNextWarning();
         }
         getApplication().getMessageHandler().showWarningMessage("Derby database may not be shut down");
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
      catch (Exception e)
      {
         s_log.error(e.getMessage(), e);
      }
   }

   /**
    * Map driver instances to open databases to active session list.
    */
   private static class EmbeddedSessions
         extends IdentityHashMap<Driver, Map<String, List<WeakReference<ISession>>>>
   {
      private static final long serialVersionUID = 6994014714179557637L;

      Driver removeSesion(ISession session)
      {
         for (Map.Entry<Driver, Map<String, List<WeakReference<ISession>>>> drvEntry : entrySet())
         {
            for (Map.Entry<String, List<WeakReference<ISession>>> dbEntry : drvEntry.getValue().entrySet())
            {
               for (WeakReference<ISession> ref : dbEntry.getValue())
               {
                  if (ref.get() == session)
                  {
                     // Replace with null reference so the flush mechanic would kick-in
                     dbEntry.getValue().remove(ref);
                     dbEntry.getValue().add(new WeakReference<>(null));
                     return drvEntry.getKey();
                  }
               }
            }
         }
         return null;
      }
   }

}
