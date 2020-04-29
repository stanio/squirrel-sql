package net.sourceforge.squirrel_sql.fw.gui.buttonchooser;

import net.sourceforge.squirrel_sql.client.Main;

import javax.swing.AbstractButton;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;

public class ComboButton extends JToggleButton
{
   private static final int TRI_HEIGHT = 3;
   private static final int DIST = 3;

   private AbstractButton linkedButton;
   private transient MouseListener hoverHandler = new HoverHandler();
   private transient KeyListener actionKeyHandler = new ActionKeyHandler();

   public ComboButton()
   {
      super.setFocusable(false);
      super.addMouseListener(hoverHandler);
   }

   private int getMaxWidth()
   {
      return Main.getApplication().getIconHandler().iconScale_ceil((DIST + TRI_HEIGHT) * 2);
   }

   @Override
   public Dimension getMinimumSize()
   {
      if (isMinimumSizeSet())
      {
         return super.getMinimumSize();
      }
      int maxWidth = getMaxWidth();
      return new Dimension(maxWidth, maxWidth);
   }

   @Override
   public Dimension getPreferredSize()
   {
      if (isPreferredSizeSet())
      {
         return super.getPreferredSize();
      }
      int maxWidth = getMaxWidth();
      return (linkedButton == null)
             ? new Dimension(maxWidth, maxWidth * 2)
             : new Dimension(maxWidth, linkedButton.getPreferredSize().height);
   }

   @Override
   public Dimension getMaximumSize()
   {
      if (isMaximumSizeSet())
      {
         return super.getMaximumSize();
      }
      int maxWidth = getMaxWidth();

      if (linkedButton == null)
      {
         return new Dimension(maxWidth, Short.MAX_VALUE);
      }
      return new Dimension(maxWidth, linkedButton.getMaximumSize().height);
   }

   @Override
   protected void paintComponent(Graphics g)
   {
      super.paintComponent(g);
      g = g.create();
      if (g instanceof Graphics2D)
      {
         ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                           RenderingHints.VALUE_ANTIALIAS_ON);
      }

      Dimension size = getSize();

      if (isEnabled())
      {
         g.setColor(getForeground());
      }
      else
      {
         g.setColor(Color.gray);
      }

      Path2D pg = new Path2D.Double();
      double scaledHeight = Main.getApplication().getIconHandler().iconScale_round(TRI_HEIGHT);
      double scaledDist = Main.getApplication().getIconHandler().iconScale_round(DIST);
      double y12 = size.height / 2.0 - scaledHeight;
      pg.moveTo(scaledDist, y12);
      pg.lineTo(size.width - scaledDist, y12);
      pg.lineTo(size.width / 2.0, size.height / 2.0 + scaledHeight);
      pg.closePath();

      if (g instanceof Graphics2D)
      {
         ((Graphics2D) g).fill(pg);
      }
      else
      {
         g.fillPolygon(toPolygon(pg));
      }
      g.dispose();
   }

   private static Polygon toPolygon(Shape shape)
   {
      Polygon polygon = new Polygon();
      PathIterator iter = shape.getPathIterator(null);
      double[] coords = new double[6];

   done:
      while (!iter.isDone())
      {
         double x, y;
         switch (iter.currentSegment(coords))
         {
            case PathIterator.SEG_CLOSE:
               break done;

            case PathIterator.SEG_CUBICTO:
               x = coords[4];
               y = coords[5];
               break;

            case PathIterator.SEG_QUADTO:
               x = coords[2];
               y = coords[3];
               break;

            case PathIterator.SEG_MOVETO:
            case PathIterator.SEG_LINETO:
            default:
               x = coords[0];
               y = coords[1];
         }
         polygon.addPoint((int) Math.round(x), (int) Math.round(y));
      }
      return polygon;
   }

   public void setLinkedButton(AbstractButton actionButton)
   {
      if (linkedButton != null)
      {
         linkedButton.removeMouseListener(hoverHandler);
         linkedButton.removeKeyListener(actionKeyHandler);
      }

      linkedButton = actionButton;

      if (linkedButton != null)
      {
         linkedButton.addMouseListener(hoverHandler);
         linkedButton.addKeyListener(actionKeyHandler);
      }
   }

   protected void processActionEvent()
   {
      ActionListener listener = actionListener;
      if (listener != null)
      {
         listener.actionPerformed(new ActionEvent(this,
               ActionEvent.ACTION_PERFORMED, getActionCommand()));
      }
   }

   public PopupMenuListener getPopupMenuListener()
   {
      return new PopupMenuListener()
      {
         @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) { /* no-op */ }

         @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
         {
            setSelected(false);
         }

         @Override public void popupMenuCanceled(PopupMenuEvent e) { /* no-op */ }
      };
   }


   class HoverHandler extends MouseAdapter
   {
      private boolean forwarding;

      @Override public void mouseEntered(MouseEvent e)
      {
         forwardEvent(e);
      }

      @Override public void mouseExited(MouseEvent e)
      {
         forwardEvent(e);
      }

      private void forwardEvent(MouseEvent e)
      {
         if (forwarding)
            return;

         forwarding = true;
         try
         {
            if (e.getSource() == ComboButton.this)
            {
               if (linkedButton != null)
               {
                  linkedButton.dispatchEvent(convertEvent(e, linkedButton));
               }
            }
            else
            {
               processMouseEvent(convertEvent(e, ComboButton.this));
            }
         }
         finally
         {
            forwarding = false;
         }
      }

      private MouseEvent convertEvent(MouseEvent e, Component destination)
      {
         return SwingUtilities.convertMouseEvent((Component) e.getSource(), e, destination);
      }
   }


   class ActionKeyHandler extends KeyAdapter
   {
      @Override public void keyPressed(KeyEvent e)
      {
         if (e.getKeyCode() == KeyEvent.VK_DOWN)
         {
            setSelected(true);
            processActionEvent();
            e.consume(); // IMPORTANT
         }
      }
   }


}
