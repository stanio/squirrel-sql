package net.sourceforge.squirrel_sql.client;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.client.session.event.SessionAdapter;
import net.sourceforge.squirrel_sql.client.session.event.SessionEvent;
import net.sourceforge.squirrel_sql.fw.id.IIdentifier;
import net.sourceforge.squirrel_sql.fw.sql.SQLDriverManager;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import java.sql.Driver;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Takes care to unload JDBC drivers after all sessions that use them get closed.
 *
 * @see  SQLDriverManager#unloadDriver(IIdentifier, Driver)
 */
class DriverUnloadListener extends SessionAdapter
{
   private static final ILogger s_log = LoggerController.createLogger(DriverUnloadListener.class);

   private final SQLDriverManager _driverMgr;

   /**
    * Maps JDBC driver instance to the {@code ISQLDriver} identifier and count
    * of sessions currently opened with it.
    */
   private final IdentityHashMap<Driver, Map<IIdentifier, Integer>> _driverSessions = new IdentityHashMap<>();

   /**
    * Maps session instance to JDBC driver instance currently in use.
    */
   private final WeakHashMap<ISession, Driver> _sessionDrivers = new WeakHashMap<>();

   DriverUnloadListener(SQLDriverManager driverMgr)
   {
      _driverMgr = driverMgr;
   }

   private synchronized void addSession(ISession session)
   {
      IIdentifier sqlDriverId = session.getDriver().getIdentifier();
      Driver oldDriver = _sessionDrivers.get(session);
      Driver jdbcDriver = _driverMgr.getJDBCDriver(sqlDriverId);
      if (oldDriver != jdbcDriver) // New session or reconnected with new driver
      {
         Map<IIdentifier, Integer> jdbcDriverSessions = _driverSessions
               .computeIfAbsent(jdbcDriver, k -> new ConcurrentHashMap<>());
         jdbcDriverSessions.merge(sqlDriverId, 1, Integer::sum);
         if (jdbcDriverSessions.size() > 1)
         {
            s_log.warn("JDBC driver instance used with multiple ISQLDriver instances",
                  new Exception(jdbcDriver + " : " + jdbcDriverSessions.keySet()));
         }
         _sessionDrivers.put(session, jdbcDriver);
      }
   }

   private synchronized void removeSession(ISession session)
   {
      Driver jdbcDriver = _sessionDrivers.remove(session);
      IIdentifier sqlDriverId = session.getDriver().getIdentifier();
      Map<IIdentifier, Integer> sessions = _driverSessions.get(jdbcDriver);
      if (sessions != null)
      {
         if (Integer.valueOf(0).equals(sessions.computeIfPresent(sqlDriverId, (k, v) -> v - 1)))
         {
            sessions.remove(sqlDriverId);
         }
      }
      if (sessions == null || sessions.isEmpty())
      {
         _driverSessions.remove(jdbcDriver);
         _driverMgr.unloadDriver(sqlDriverId, jdbcDriver);
      }
   }

   private void logSessionEvent(String type, SessionEvent evt)
   {
      ISession session = evt.getSession();
      s_log.debug(type + ": " + session
            + "\n\tsqlDriver: " + session.getDriver()
            + "\n\tjdbcDriver: " + _driverMgr
                  .getLoadedDriver(session.getDriver().getIdentifier()));
   }

   @Override
   public void sessionConnected(SessionEvent evt)
   {
      logSessionEvent("sessionConnected", evt);
      addSession(evt.getSession());
   }

   @Override
   public void sessionClosed(SessionEvent evt)
   {
      logSessionEvent("sessionClosed", evt);
      removeSession(evt.getSession());
   }

   @Override
   public void connectionClosedForReconnect(SessionEvent evt)
   {
      logSessionEvent("connectionClosedForReconnect", evt);

      ISession session = evt.getSession();
      Driver oldDriver = _sessionDrivers.get(session);
      Driver newDriver = _driverMgr.getJDBCDriver(session.getDriver().getIdentifier());
      if (oldDriver != newDriver)
      {
         removeSession(evt.getSession());
      }
   }

   @Override
   public void reconnected(SessionEvent evt)
   {
      logSessionEvent("reconnected", evt);
      addSession(evt.getSession());
   }

   // DEBUG

   @Override
   public void sessionClosing(SessionEvent evt)
   {
      logSessionEvent("sessionClosing", evt);
   }

   public void allSessionsClosed()
   {
      s_log.debug("allSessionsClosed");
   }
}
