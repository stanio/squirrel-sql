package net.sourceforge.squirrel_sql.client.gui.desktopcontainer.docktabdesktop;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.gui.desktopcontainer.TabWidget;
import net.sourceforge.squirrel_sql.client.gui.mainframe.SquirrelDesktopManager;
import net.sourceforge.squirrel_sql.client.resources.SquirrelResources;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.gui.buttontabcomponent.ButtonTabComponent;
import net.sourceforge.squirrel_sql.fw.gui.buttontabcomponent.SmallTabButton;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;

public class TabWindowController implements DockTabDesktopPaneHolder
{
   private static final StringManager s_stringMgr = StringManagerFactory.getStringManager(DockTabDesktopPaneHolder.class);


   private final DockTabDesktopPane _dockTabDesktopPane;
   private IApplication _app;
   private final JMenu _mnuSession;
   private final JFrame _tabWindowFrame;

   private static class MoveTabBackToMainWinMarker {}

   public TabWindowController(Point locationOnScreen, Dimension size, final IApplication app)
   {
      _app = app;
      _tabWindowFrame = new JFrame(_app.getMainFrame().getTitle() + " " +s_stringMgr.getString("docktabdesktop.TabWindowController.titlePostFix"));

      _tabWindowFrame.setLocation(locationOnScreen);
      _tabWindowFrame.setSize(size);
      _tabWindowFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

      DockTabDesktopPaneListener dockTabDesktopPaneListener = new DockTabDesktopPaneListener()
      {
         @Override
         public void tabWasRemoved(TabHandle tabHandle)
         {
            onTabWasRemoved();
         }
      };

      _dockTabDesktopPane = new DockTabDesktopPane(app, false, dockTabDesktopPaneListener);
      _dockTabDesktopPane.setDesktopManager(new SquirrelDesktopManager(app));

      _tabWindowFrame.getContentPane().add(_dockTabDesktopPane);

      JMenuBar mnuBar = new JMenuBar();
      _mnuSession = cloneMenu(app.getMainFrame().getSessionMenu());
      mnuBar.add(_mnuSession);

      _tabWindowFrame.setJMenuBar(mnuBar);

      WindowFocusListener l = new WindowFocusListener()
      {
         @Override
         public void windowGainedFocus(WindowEvent e)
         {
            onWindowFocusGained(app);
         }

         @Override
         public void windowLostFocus(WindowEvent e)
         {
         }
      };
      _tabWindowFrame.addWindowFocusListener(l);


      final Icon icon = app.getResources().getIcon(SquirrelResources.IImageNames.APPLICATION_ICON);
      if (icon != null)
      {
         _tabWindowFrame.setIconImage(GUIUtils.getIconImage(icon));
      }


      _tabWindowFrame.addWindowListener(new WindowAdapter()
      {
         @Override
         public void windowClosing(WindowEvent e)
         {
            onWindowClosing();
         }
      });

      _tabWindowFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

      _tabWindowFrame.setVisible(true);

      GUIUtils.executeDelayed(() -> _tabWindowFrame.toFront());
   }

   private void onTabWasRemoved()
   {
      if(0 == _dockTabDesktopPane.getTabCount())
      {
         closeFrame();
      }
   }

   private void onWindowClosing()
   {
      dispose();
   }

   private void dispose()
   {
      ArrayList<TabHandle> handels = _dockTabDesktopPane.getAllHandels();
      TabHandle[] clone = handels.toArray(new TabHandle[handels.size()]);
      for (TabHandle handel : clone)
      {
         if(false == handel.removeTab(DockTabDesktopPane.TabClosingMode.DISPOSE))
         {
            return;
         }
      }

      _tabWindowFrame.setVisible(false);
      _tabWindowFrame.dispose();

      _app.getMultipleWindowsHandler().unregisterDesktop(this);
   }


   @Override
   public void setSelected(boolean b)
   {
      _dockTabDesktopPane.setSelected(true);
      adjustSessionMenu();
   }

   private void adjustSessionMenu()
   {
      if(null == _dockTabDesktopPane.getSelectedWidget())
      {
         _mnuSession.setEnabled(false);
      }
      else
      {
         _mnuSession.setEnabled(true);
      }
   }

   @Override
   public void tabDragedAndDroped()
   {
      adjustSessionMenu();
   }

   private void onWindowFocusGained(IApplication app)
   {
      app.getMultipleWindowsHandler().selectDesktop(this);
   }

   private JMenu cloneMenu(JMenu menu)
   {
      JMenu ret = new JMenu(menu.getText());

      for (int i = 0; i < menu.getItemCount(); i++)
      {
         JMenuItem toClone = menu.getItem(i);

         if (toClone instanceof JMenu)
         {
            ret.add(cloneMenu((JMenu) toClone));
         }
         else if(toClone instanceof JMenuItem)
         {
            JMenuItem clone = new JMenuItem(toClone.getText(), toClone.getIcon());
            clone.setMnemonic(toClone.getMnemonic());
            clone.setAction(toClone.getAction());
            clone.setAccelerator(toClone.getAccelerator());
            clone.setToolTipText(toClone.getToolTipText());

            ret.add(clone);
         }
         else
         {
            ret.addSeparator();
         }
      }

      return ret;
   }

   @Override
   public void addTabWidgetAt(final TabWidget widget, int tabIndex, final ArrayList<SmallTabButton> externalButtons, boolean widgetMovedButNotCreated)
   {

      SmallTabButton btnMoveTabBackToMainWin = null;
      for (SmallTabButton externalButton : externalButtons)
      {
         if(externalButton.getUserObject() == MoveTabBackToMainWinMarker.class)
         {
            btnMoveTabBackToMainWin = externalButton;
            break;
         }
      }

      ArrayList<SmallTabButton> buf = externalButtons;
      if (null == btnMoveTabBackToMainWin)
      {
         buf = new ArrayList<SmallTabButton>();
         String toolTipText = s_stringMgr.getString("docktabdesktop.TabWindowController.moveTabBackToMainWin");
         Icon icon = _app.getResources().getIcon(SquirrelResources.IImageNames.TAB_DETACH_SMALL_REVERT);
         btnMoveTabBackToMainWin = new SmallTabButton(toolTipText, icon, MoveTabBackToMainWinMarker.class);
         buf.add(btnMoveTabBackToMainWin);
         buf.addAll(externalButtons);
      }

      final TabHandle tabHandle = _dockTabDesktopPane.addTabWidgetAt(widget, tabIndex, buf, widgetMovedButNotCreated);

      btnMoveTabBackToMainWin.addActionListener(new ActionListener()
      {
         @Override
         public void actionPerformed(ActionEvent e)
         {
            onMoveBackToMainWin(tabHandle);
         }
      });

   }

   private void onMoveBackToMainWin(TabHandle tabHandle)
   {
      RemoveTabHandelResult removeTabHandelResult = _dockTabDesktopPane.removeTabHandel(tabHandle);

      cleanUpOnMove(removeTabHandelResult);

      DockTabDesktopPaneHolder mainDockTabDesktopHolder = _app.getMultipleWindowsHandler().getMainDockTabDesktopHolder();
      ButtonTabComponent removedButtonTabComponent = removeTabHandelResult.getRemovedButtonTabComponent();

      if (null != removedButtonTabComponent)
      {
         ArrayList<SmallTabButton> externalButtons = removedButtonTabComponent.getExternalButtons();
         mainDockTabDesktopHolder.addTabWidget(tabHandle.getWidget(), externalButtons, true);
      }
   }

   @Override
   public RemoveTabHandelResult removeTabHandel(int tabIndex)
   {
      RemoveTabHandelResult tabHandelResult = _dockTabDesktopPane.removeTabHandel(tabIndex);

      cleanUpOnMove(tabHandelResult);

      return tabHandelResult;
   }

   private void cleanUpOnMove(RemoveTabHandelResult tabHandelResult)
   {
      ButtonTabComponent removedButtonTabComponent = tabHandelResult.getRemovedButtonTabComponent();

      SmallTabButton stb = null;

      if (null != removedButtonTabComponent)
      {
         stb = removedButtonTabComponent.findSmallTabButtonByUserObject(MoveTabBackToMainWinMarker.class);
      }

      if (null != stb)
      {
         for (ActionListener actionListener : stb.getActionListeners())
         {
            stb.removeActionListener(actionListener);
         }
         removedButtonTabComponent.removeSmallTabButton(stb);
      }

      if(0 == _dockTabDesktopPane.getTabCount())
      {
         closeFrame();
      }

   }

   private void closeFrame()
   {
      dispose();
      _tabWindowFrame.setVisible(false);
      _tabWindowFrame.dispose();
   }

   @Override
   public boolean isMyTabbedPane(JTabbedPane tabbedPane)
   {
      return _dockTabDesktopPane.isMyTabbedPane(tabbedPane);
   }

   @Override
   public void addTabWidget(TabWidget widget, ArrayList<SmallTabButton> externalButtons, boolean widgetMovedButNotCreated)
   {
      _dockTabDesktopPane.addTabWidgetAt(widget, _dockTabDesktopPane.getTabCount(), externalButtons, widgetMovedButNotCreated);
   }
}
