package net.sourceforge.squirrel_sql.client.gui;
/*
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

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.mainframe.action.ViewLogsCommand;
import net.sourceforge.squirrel_sql.client.resources.SquirrelResources;
import net.sourceforge.squirrel_sql.fw.gui.ErrorDialog;
import net.sourceforge.squirrel_sql.fw.gui.buttontabcomponent.SmallTabButton;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILoggerListener;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.Date;
import java.util.Vector;


/**
 * This is the Status bar's Log panel.
 * It recieves all logs through the ILoggerListener interface.
 * It updates its status all 200 millis, see _displayLastLogTimer.
 * <p>
 * The traffic lights color of the last log is displayed on the JButton _btnLastLog.
 * If no new log arrives the last log color is displayed for 5000 millis and is the replaced
 * by the "white" icon, see _whiteIconTimer. This allows the user to have an eye an the logs
 * without much disturbance.
 */
public class LogPanel extends JPanel
{
   private static final StringManager s_stringMgr = StringManagerFactory.getStringManager(MemoryPanel.class);

   private SquirrelResources _resources;

   private static final int LOG_TYPE_INFO = 0;
   private static final int LOG_TYPE_WARN = 1;
   private static final int LOG_TYPE_ERROR = 2;


   private JLabel _lblLogInfo = new JLabel();
   private SmallTabButton _btnLastLog = new SmallTabButton(4);
   private JButton _btnViewLogs = new SmallTabButton(4);

   private Timer _displayLastLogTimer;
   private Timer _whiteIconTimer;

	private final Vector<LogData> _logsDuringDisplayDelay = new Vector<>();
	private LogData _curlogToDisplay;
	private IApplication _app;


   private LogStatistics _statistics = new LogStatistics();

   public LogPanel(IApplication app)
   {
      _app = app;
      _resources = _app.getResources();
      createGui();


      setIconForCurLogType();

      _whiteIconTimer = new Timer(5000, e -> _btnLastLog.setIcon(_resources.getIcon(SquirrelResources.IImageNames.WHITE_GEM)));

      _whiteIconTimer.setRepeats(false);

      int displayDelay = 200;
      _displayLastLogTimer = new Timer(displayDelay, e -> updatePanel());

      _displayLastLogTimer.setRepeats(false);

      _statistics._errorCount = LoggerController.getLogCounts().getErrorCount();
      _statistics._warnCount = LoggerController.getLogCounts().getWarningCount();
      _statistics._infoCount = LoggerController.getLogCounts().getInfoCount();

      LoggerController.addLoggerListener(new ILoggerListener()
      {
         public void info(Class<?> source, Object message)
         {
            _statistics.setInfoCount(_statistics._infoCount + 1);
            addLog(LOG_TYPE_INFO, source.getName(), message, message instanceof Throwable ? (Throwable) message : null);
         }

         public void info(Class<?> source, Object message, Throwable th)
         {
            _statistics.setInfoCount(_statistics._infoCount + 1);
            addLog(LOG_TYPE_INFO, source.getName(), message, th);
         }

         public void warn(Class<?> source, Object message)
         {
            _statistics.setWarnCount(_statistics._warnCount + 1);
            addLog(LOG_TYPE_WARN, source.getName(), message, message instanceof Throwable ? (Throwable) message : null);
         }

         public void warn(Class<?> source, Object message, Throwable th)
         {
            _statistics.setWarnCount(_statistics._warnCount + 1);
            addLog(LOG_TYPE_WARN, source.getName(), message, th);
         }

         public void error(Class<?> source, Object message)
         {
            _statistics.setErrorCount(_statistics._errorCount + 1);
            addLog(LOG_TYPE_ERROR, source.getName(), message, message instanceof Throwable ? (Throwable) message : null);
         }

         public void error(Class<?> source, Object message, Throwable th)
         {
            _statistics.setErrorCount(_statistics._errorCount + 1);
            addLog(LOG_TYPE_ERROR, source.getName(), message, th);
         }
      });


      _btnLastLog.addMouseListener(new MouseAdapter()
      {
         public void mouseEntered(MouseEvent e)
         {
            setIconForCurLogType();
         }

         public void mouseExited(MouseEvent e)
         {
            if (false == _whiteIconTimer.isRunning())
            {
               _btnLastLog.setIcon(_resources.getIcon(SquirrelResources.IImageNames.WHITE_GEM));
            }
         }
      });

      _btnLastLog.addActionListener(e -> showLogInDialog());

      _btnViewLogs.addActionListener(e -> new ViewLogsCommand(_app).execute());

   }

   private void createGui()
   {
      setLayout(new BorderLayout(5, 0));

      Icon viewLogsIcon = _resources.getIcon(SquirrelResources.IImageNames.LOGS);
      _btnViewLogs.setIcon(viewLogsIcon);

      Dimension prefButtonSize = new Dimension(viewLogsIcon.getIconWidth(), viewLogsIcon.getIconHeight());
      _btnLastLog.setPreferredSize(prefButtonSize);
      _btnViewLogs.setPreferredSize(prefButtonSize);

      _btnLastLog.setBorder(null);
      _btnViewLogs.setBorder(null);

      JPanel pnlButtons = new JPanel(new GridLayout(1, 2, 3, 0));
      pnlButtons.add(_btnLastLog);
      pnlButtons.add(_btnViewLogs);

      add(pnlButtons, BorderLayout.EAST);
      add(_lblLogInfo, BorderLayout.CENTER);

      // i18n[LogPanel.viewLastLog=Press to view last log entry]
      _btnLastLog.setToolTipText(s_stringMgr.getString("LogPanel.viewLastLog"));

      // i18n[LogPanel.openLogs=Press to open logs]
      _btnViewLogs.setToolTipText(s_stringMgr.getString("LogPanel.openLogs"));
   }


   private void showLogInDialog()
   {
      if (null != _curlogToDisplay)
      {
         // i18n[LogPanel.logMsg=Logged by {0} at {1}:\n\n {2}]
         String extMsg = s_stringMgr.getString("LogPanel.logMsg", _curlogToDisplay.source, _curlogToDisplay.logTime,_curlogToDisplay.message);

         ErrorDialog errorDialog = new ErrorDialog(_app.getMainFrame(), extMsg, _curlogToDisplay.throwable);

         String title;

         switch (_curlogToDisplay.logType)
         {
            case LOG_TYPE_INFO:
               // i18n[LogPanel.titleInfo=Last log entry (Entry type: Info)]
               title = s_stringMgr.getString("LogPanel.titleInfo");
               break;
            case LOG_TYPE_WARN:
               // i18n[LogPanel.titleWarn=Last log entry (Entry type: Warning)]
               title = s_stringMgr.getString("LogPanel.titleWarn");
               break;
            case LOG_TYPE_ERROR:
               // i18n[LogPanel.titleError=Last log entry (Entry type: ERROR)]
               title = s_stringMgr.getString("LogPanel.titleError");
               break;
            default:
               // i18n[LogPanel.titleUnknown=Last log entry (Entry type: Unknown)]
               title = s_stringMgr.getString("LogPanel.titleUnknown");
               break;
         }

         errorDialog.setTitle(title);
         errorDialog.setVisible(true);
      }
   }


   private void addLog(int logType, String source, Object message, Throwable t)
   {
      LogData log = new LogData();
      log.logType = logType;
      log.source = source;
      log.message = message;
      log.throwable = t;


      synchronized (_logsDuringDisplayDelay)
      {
         _logsDuringDisplayDelay.add(log);
      }

      _displayLastLogTimer.restart();
   }


   private void updatePanel()
   {
      LogData[] logs;
      synchronized (_logsDuringDisplayDelay)
      {
         logs = _logsDuringDisplayDelay.toArray(new LogData[_logsDuringDisplayDelay.size()]);
         _logsDuringDisplayDelay.clear();
      }

      _curlogToDisplay = null;
      for (int i = 0; i < logs.length; i++)
      {
         if (null == _curlogToDisplay)
         {
            _curlogToDisplay = logs[i];
         }
         else if (_curlogToDisplay.logType <= logs[i].logType)
         {
            _curlogToDisplay = logs[i];
         }
      }


      _lblLogInfo.setText(_statistics.toString());


      setIconForCurLogType();

      _whiteIconTimer.restart();
   }

   private void setIconForCurLogType()
   {
      if (null == _curlogToDisplay)
      {
         _btnLastLog.setIcon(_resources.getIcon(SquirrelResources.IImageNames.WHITE_GEM));
         return;
      }

      switch (_curlogToDisplay.logType)
      {
         case LOG_TYPE_INFO:
            _btnLastLog.setIcon(_resources.getIcon(SquirrelResources.IImageNames.GREEN_GEM));
            break;
         case LOG_TYPE_WARN:
            _btnLastLog.setIcon(_resources.getIcon(SquirrelResources.IImageNames.YELLOW_GEM));
            break;
         case LOG_TYPE_ERROR:
            _btnLastLog.setIcon(_resources.getIcon(SquirrelResources.IImageNames.RED_GEM));
            break;
      }
   }

   private static class LogData
   {
      int logType = -1;
      Object message = null;
      Throwable throwable = null;
      String source;
      String logTime;

      public LogData()
      {
         logTime = DateFormat.getInstance().format(new Date());
      }

   }

   private static class LogStatistics
   {
      private long _errorCount;
      private long _warnCount;
      private long _infoCount;

      private String _toString = "";

      public LogStatistics()
      {
         updateToString();
      }

      void setErrorCount(long errorCount)
      {
         this._errorCount = errorCount;
         updateToString();
      }

      void setWarnCount(long warnCount)
      {
         this._warnCount = warnCount;
         updateToString();
      }

      void setInfoCount(long infoCount)
      {
         this._infoCount = infoCount;
         updateToString();
      }

      private void updateToString()
      {
         // i18n[LogPanel.logInfoLabel=Logs: Errors {0}, Warnings {1}, Infos {2}]
         _toString = s_stringMgr.getString("LogPanel.logInfoLabel", _errorCount, _warnCount, _infoCount);
      }

      public String toString()
      {
         return _toString;
      }
   }


}