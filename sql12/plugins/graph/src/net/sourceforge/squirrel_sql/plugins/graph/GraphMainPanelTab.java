package net.sourceforge.squirrel_sql.plugins.graph;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sourceforge.squirrel_sql.client.session.mainpanel.BaseMainPanelTab;
import net.sourceforge.squirrel_sql.client.session.mainpanel.SQLPanel;
import net.sourceforge.squirrel_sql.fw.gui.buttontabcomponent.SmallTabButton;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;


public class GraphMainPanelTab extends BaseMainPanelTab
{
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(GraphMainPanelTab.class);

	private GraphPanelController _panelController;
   // i18n[graph.newGraph=New table graph]

   private JPanel _tabComponent;

   private JLabel _lblTitle;
   private JButton _btnToWindow;
   private LazyLoadListener _lazyLoadListener;

   public GraphMainPanelTab(GraphPanelController panelController, GraphPlugin plugin, boolean isLink)
   {
      _panelController = panelController;
      _tabComponent = new JPanel(new BorderLayout(3,0));
      _tabComponent.setOpaque(false);

      _lblTitle = new JLabel();
      _lblTitle.setOpaque(false);
      if(isLink)
      {
         Icon linkIcon = new GraphPluginResources(plugin).getIcon(GraphPluginResources.IKeys.LINK);
         _lblTitle.setIcon(linkIcon);
      }


      _tabComponent.add(_lblTitle, BorderLayout.CENTER);
      Icon icon = new GraphPluginResources(plugin).getIcon(GraphPluginResources.IKeys.TO_WINDOW_SMALL);

      _btnToWindow = new SmallTabButton(s_stringMgr.getString("GraphMainPanelTab.showInNewWIndow"), icon);
      _tabComponent.add(_btnToWindow, BorderLayout.EAST);
   }


   protected void refreshComponent()
   {
      _panelController.repaint();
      _lazyLoadListener.lazyLoadTables();
   }

   public String getTitle()
   {
      return _lblTitle.getText();
   }


   @Override
   public Component getTabComponent()
   {
      return _tabComponent;
   }

   public String getHint()
   {
		// i18n[graph.rightClickTable=Right click table in object tree to add to graph]
		return s_stringMgr.getString("graph.rightClickTable");
   }

   public Component getComponent()
   {
      return _panelController.getGraphPanel();
   }

   public void setTitle(String title)
   {
      _lblTitle.setText(title);
   }

   public JButton getToWindowButton()
   {
      return _btnToWindow;
   }

   public void removeGraph()
   {
      _panelController.removeGraph();
   }

   public void setLazyLoadListener(LazyLoadListener lazyLoadListener)
   {
      _lazyLoadListener = lazyLoadListener;
   }

   public void changedFromLinkToLocalCopy()
   {
      _lblTitle.setIcon(null);
   }

   @Override
   public SQLPanel getSqlPanelOrNull()
   {
      return null;
   }
}
