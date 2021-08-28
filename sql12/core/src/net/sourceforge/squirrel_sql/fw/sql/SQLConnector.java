package net.sourceforge.squirrel_sql.fw.sql;

import net.sourceforge.squirrel_sql.client.gui.db.SQLAlias;
import net.sourceforge.squirrel_sql.client.session.action.reconnect.ReconnectInfo;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public class SQLConnector
{
   private static final ILogger s_log = LoggerController.createLogger(SQLConnector.class);

   private static final StringManager s_stringMgr = StringManagerFactory.getStringManager(SQLConnector.class);

   public static SQLConnection getSqlConnection(ISQLDriver sqlDriver, SQLAlias alias, String user, String pw, SQLDriverPropertyCollection props, ReconnectInfo reconnectInfo, Driver driver)
         throws SQLException, DriverLoadException
   {
         Properties myProps = new Properties();
         if (props != null)
         {
            props.applyTo(myProps);
         }

         if(null != reconnectInfo && null != reconnectInfo.getUser())
         {
            myProps.put("user", reconnectInfo.getUser());
         }
         else if (user != null)
         {
            myProps.put("user", user);
         }

         if(null != reconnectInfo && null != reconnectInfo.getPassword())
         {
            myProps.put("password", reconnectInfo.getPassword());
         }
         else if (pw != null)
         {
            myProps.put("password", pw);
         }

         if (driver == null)
         {
            driver = DriverReference.queueUp(newJdbcDriver(sqlDriver));
         }

         String url = alias.getUrl();

         if(null != reconnectInfo && null != reconnectInfo.getUrl())
         {
            url = reconnectInfo.getUrl();
         }

         Connection jdbcConn = driver.connect(url, myProps);

         if (jdbcConn == null)
         {
            throw new SQLException(s_stringMgr.getString("SQLDriverManager.error.noconnection"));
         }
         return new SQLConnection(jdbcConn, props, sqlDriver);
   }

   static Driver newJdbcDriver(ISQLDriver sqlDriver) throws DriverLoadException
   {
      try
      {
         ClassLoader loader = new SQLDriverClassLoader(sqlDriver);
         Class<?> driverClass = Class.forName(sqlDriver.getDriverClassName(), false, loader);
         Driver jdbcDriver = (Driver) driverClass.getDeclaredConstructor().newInstance();
         return jdbcDriver;
      }
      catch (ReflectiveOperationException | LinkageError | SecurityException e)
      {
         throw new DriverLoadException(e);
      }
   }

   static class DriverReference extends WeakReference<Driver>
   {
      private static ReferenceQueue<Driver> closeQueue = new ReferenceQueue<>();

      final ClassLoader classLoader;

      DriverReference(Driver driver)
      {
         super(driver, closeQueue);
         this.classLoader = driver.getClass().getClassLoader();
         flushQueue();
      }

      static Driver queueUp(Driver driver)
      {
         new DriverReference(driver);
         return driver;
      }

      private static void flushQueue()
      {
         DriverReference ref = (DriverReference) closeQueue.poll();
         while (ref != null)
         {
            if (ref.classLoader instanceof URLClassLoader)
            {
               try
               {
                  ((URLClassLoader) ref.classLoader).close();
               }
               catch (IOException e)
               {
                  s_log.warn("", e);
               }
            }
            ref = (DriverReference) closeQueue.poll();
         }
      }
   }

}
