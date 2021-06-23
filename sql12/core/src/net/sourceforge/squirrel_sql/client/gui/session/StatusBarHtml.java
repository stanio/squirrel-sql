package net.sourceforge.squirrel_sql.client.gui.session;

import net.sourceforge.squirrel_sql.client.session.mainpanel.objecttree.ObjectTreeNode;

import javax.swing.tree.TreePath;

public class StatusBarHtml
{
   static String createStatusBarHtml(TreePath selPath)
   {
      StringBuilder buf = new StringBuilder();
      buf.append("<html><span style='white-space: pre'>");
      Object[] fullPath = selPath.getPath();
      for (int i = 0; i < fullPath.length; ++i)
      {
         if (fullPath[i] instanceof ObjectTreeNode)
         {
            ObjectTreeNode node = (ObjectTreeNode)fullPath[i];

            // See linkDescription in getTreePathForLink(...) below.
            int linkDescription = i;
            buf.append('/').append("<a href=\"" + linkDescription + "\">" + node.toString() + "</a>");
         }
      }
      buf.append("</span></html>");
      final String text = buf.toString();
      return text;
   }

   public static TreePath getTreePathForLink(String linkDescription, TreePath treePathForLink)
   {
      final int pathIndex = Integer.parseInt(linkDescription);

      TreePath path = treePathForLink;

      for (int i = treePathForLink.getPathCount() - 1; i > pathIndex; i--)
      {
         path = path.getParentPath();
      }

      return path;
   }
}
