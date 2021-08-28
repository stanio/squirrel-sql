package net.sourceforge.squirrel_sql.fw.sql;
/*
 * Copyright (C) 2001-2003 Colin Bell
 * colbell@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import net.sourceforge.squirrel_sql.client.gui.db.SQLAlias;
import net.sourceforge.squirrel_sql.client.session.action.reconnect.ReconnectInfo;
import net.sourceforge.squirrel_sql.fw.id.IIdentifier;
import net.sourceforge.squirrel_sql.fw.sql.SQLConnector.DriverReference;
import net.sourceforge.squirrel_sql.fw.util.StringUtilities;
import net.sourceforge.squirrel_sql.fw.util.Utilities;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import javax.swing.SwingWorker;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class replaces the standard Java class <TT>java.ql.DriverManager</TT>.
 * The main reason for replacing it is that <TT>java.ql.DriverManager</TT>
 * won't handle JDBC driver classes that were loaded by a custom classloader.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class SQLDriverManager
{
	private static final ILogger s_log = LoggerController.createLogger(SQLDriverManager.class);

	/**
	 * Collection of instances of <TT>ISQLDriver</TT> objects keyed
	 * by the <TT>SQLDriver.getIdentifier()</TT>.
	 */
	private Map<IIdentifier, ISQLDriver> _driverInfo = new HashMap<>();

	/**
	 * Collection of loaded <TT>DriverReference</TT> for
	 * each driver. keyed by <TT>SQLDriver.getIdentifier()</TT>.
	 */
	private Map<IIdentifier, DriverReference> _loadedDrivers = new HashMap<>();

	private MyDriverListener _myDriverListener = new MyDriverListener();

	public synchronized void registerSQLDriver(ISQLDriver sqlDriver) throws ClassNotFoundException
	{
		try
		{
			unregisterSQLDriver(sqlDriver);
			sqlDriver.addPropertyChangeListener(_myDriverListener);
			_driverInfo.put(sqlDriver.getIdentifier(), sqlDriver);

			// REVISIT: Could we load and update the JDBCDriverClassLoaded flag in background?
			Driver driver = getDriver(sqlDriver.getIdentifier());

			new SwingWorker<Void, Void>()
			{
				@Override protected Void doInBackground() throws Exception
				{
					unloadDriver(sqlDriver.getIdentifier(), driver);
					return null;
				}
			}.execute();
		}
		catch (DriverLoadException e)
		{
			if (e.getCause() instanceof ClassNotFoundException)
			{
				throw (ClassNotFoundException) e.getCause();
			}
			throw e;
		}
		catch (Exception e)
		{
			throw Utilities.wrapRuntime(e);
		}
	}

	public synchronized void unregisterSQLDriver(ISQLDriver sqlDriver)
	{
		sqlDriver.setJDBCDriverClassLoaded(false);
		sqlDriver.removePropertyChangeListener(_myDriverListener);
		_driverInfo.remove(sqlDriver.getIdentifier());
		_loadedDrivers.remove(sqlDriver.getIdentifier());
	}

	public synchronized void unloadDriver(IIdentifier sqlDriverId, Driver jdbcDriver)
	{
		Driver driverToUnload = jdbcDriver;
		Driver currentDriver = (sqlDriverId == null) ? null : getLoadedDriver(sqlDriverId);
		if (driverToUnload == null)
		{
			driverToUnload = currentDriver;
		}
		if (driverToUnload == null)
		{
			s_log.debug("Nothing to unload: " + sqlDriverId);
			return;
		}

		ClassLoader classLoader = driverToUnload.getClass().getClassLoader();
		if (classLoader instanceof URLClassLoader)
		{
			try
			{
				((URLClassLoader) classLoader).close();
			}
			catch (IOException e)
			{
				s_log.warn("Problem unloading driver", e);
			}
		}

		if (driverToUnload == currentDriver)
		{
			_loadedDrivers.remove(sqlDriverId);
		}
	}

	private synchronized Driver getDriver(IIdentifier sqlDriverId)
			throws DriverLoadException
	{
		Driver jdbcDriver = getLoadedDriver(sqlDriverId);
		if (jdbcDriver == null)
		{
			ISQLDriver sqlDriver = _driverInfo.get(sqlDriverId);
			if (sqlDriver == null)
			{
				return null;
			}

			jdbcDriver = SQLConnector.newJdbcDriver(sqlDriver);
			sqlDriver.setJDBCDriverClassLoaded(true);
			_loadedDrivers.put(sqlDriverId, new DriverReference(jdbcDriver));
		}
		return jdbcDriver;
	}

	/**
	 * Unlike {@code getJDBCDriver()} this will not load JDBC driver instance, if
	 * not loaded already.
	 *
	 * @param   sqlDriverId  the {@code ISQLDriver} identifier to get JDBC driver for
	 * @return  The loaded JDBC driver, or {@code null}
	 * @see     #getJDBCDriver(IIdentifier)
	 */
	public synchronized Driver getLoadedDriver(IIdentifier sqlDriverId)
	{
		DriverReference ref = _loadedDrivers.get(sqlDriverId);
		if (ref == null)
		{
			return null;
		}
		Driver driver = ref.get();
		if (driver == null)
		{
			_loadedDrivers.remove(sqlDriverId);
		}
		return driver;
	}

	public ISQLConnection getConnection(ISQLDriver sqlDriver, SQLAlias alias, String user, String pw)
	      throws SQLException, DriverLoadException
	{
		return getConnection(sqlDriver, alias, user, pw, null);
	}

	public SQLConnection getConnection(ISQLDriver sqlDriver, SQLAlias alias, String user, String pw, SQLDriverPropertyCollection props)
	      throws SQLException, DriverLoadException
	{
		return getConnection(sqlDriver, alias, user, pw, props, null);
	}


	public SQLConnection getConnection(ISQLDriver sqlDriver, SQLAlias alias, String user, String pw, SQLDriverPropertyCollection props, ReconnectInfo reconnectInfo)
	      throws SQLException, DriverLoadException
	{
		Driver driver;
		synchronized (this)
		{
			driver = getDriver(sqlDriver.getIdentifier());
		}

		return SQLConnector.getSqlConnection(sqlDriver, alias, user, pw, props, reconnectInfo, driver);
	}

	/**
	 * Return the <TT>java.sql.Driver</TT> being used for the passed
	 * <TT>ISQLDriver.getIdentifier()</TT> or <TT>null</TT> if none found.
	 *
	 * @return	the <TT>java.sql.Driver</TT> being used for the passed
	 * 			<TT>ISQLDriver.getIdentifier()</TT> or <TT>null if none found.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if <TT>null</TT> IIdentifier</TT> passed.
	 */
	public Driver getJDBCDriver(IIdentifier id) throws DriverLoadException
	{
		if (id == null)
		{
			throw new IllegalArgumentException("IIdentifier == null");
		}

		return getDriver(id);
	}

	/**
	 * Return the <TT>SQLDriverClassLoader</TT> used for the passed driver.
	 *
	 * @param	sqlDriver	Driver to find class loader for.
	 *
	 * @throws	IllegalArgumentException
	 *			Thrown if <TT>null</TT> <TT>SQLDriverClassLoader</TT> passed.
	 *
	 * @return	ClassLoader or null.
	 */
	public SQLDriverClassLoader getSQLDriverClassLoader(ISQLDriver driver)
	{
		if (driver == null)
		{
			throw new IllegalArgumentException("SQLDriverClassLoader == null");
		}

		Driver jdbcDriver = getLoadedDriver(driver.getIdentifier());
		if (jdbcDriver == null)
		{
			return null;
		}
		return (SQLDriverClassLoader) jdbcDriver.getClass().getClassLoader();
	}

	private final class MyDriverListener implements PropertyChangeListener
	{
		public void propertyChange(PropertyChangeEvent evt)
		{
			final String propName = evt.getPropertyName();
			if (propName == null
				|| propName.equals(ISQLDriver.IPropertyNames.DRIVER_CLASS)
				|| propName.equals(ISQLDriver.IPropertyNames.JARFILE_NAMES))
			{
				Object obj = evt.getSource();
				if (obj instanceof ISQLDriver)
				{
					ISQLDriver driver = (ISQLDriver) obj;
					SQLDriverManager.this.unregisterSQLDriver(driver);
					try
					{
						SQLDriverManager.this.registerSQLDriver(driver);
					}
					catch (ClassNotFoundException ex)
					{
                        String[] jars = driver.getJarFileNames();
                        String jarFileList = "<empty list>"; 
                        if (jars != null) {
                            jarFileList = 
                                "[ " + StringUtilities.join(jars, ", ") + " ]";
                        }
                       
						s_log.error("Unable to find Driver Class "
								+ driver.getDriverClassName()
								+ " for JDBC driver "
								+ driver.getName()
                                + "; jar filenames = "+jarFileList);
					}
					catch (Exception ex)
					{
						s_log.error("Unable to create instance of Class "
												+ driver.getDriverClassName()
												+ " for JDBC driver "
												+ driver.getName(), ex);
					}

				}
				else
				{
					s_log.error("SqlDriverManager.MyDriverListener is listening to a non-ISQLDriver");
				}
			}
		}
	}
}
