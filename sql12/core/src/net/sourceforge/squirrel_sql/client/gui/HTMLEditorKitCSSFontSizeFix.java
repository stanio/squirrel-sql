package net.sourceforge.squirrel_sql.client.gui;

import java.io.Serializable;
import java.util.Enumeration;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.html.CSS;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

/**
 * Fixes CSS {@code font-size} inheritance with percentage values (relative to parent).
 * <pre>
 * JEditorPane editor = new JEditorPane();
 * editor.setEditorKitForContentType("text/html", new HTMLEditorKitCSSFontSizeFix());</pre>
 */
@SuppressWarnings("serial")
public class HTMLEditorKitCSSFontSizeFix extends HTMLEditorKit
{

   @Override
   public Document createDefaultDocument()
   {
      StyleSheet styles = new StyleSheetFix();
      styles.addStyleSheet(getStyleSheet());

      HTMLDocument doc = new HTMLDocument(styles);
      doc.setParser(getParser());
      doc.setAsynchronousLoadPriority(4);
      doc.setTokenThreshold(100);
      return doc;
   }


   static class StyleSheetFix extends StyleSheet
   {
      private Object fontSizeInherit;

      /** A {@code CSS.FontSize} value: {@code font-size: 100%} */
      Object fontSizeInherit()
      {
         if (fontSizeInherit == null)
         {
            addRule("font-size-inherit { font-size: 100% }");
            fontSizeInherit = getStyle("font-size-inherit")
                              .getAttribute(CSS.Attribute.FONT_SIZE);
         }
         return fontSizeInherit;
      }

      @Override
      public AttributeSet getViewAttributes(View v)
      {
         return new ProxyAttributeSet(super.getViewAttributes(v));
      }


      private class ProxyAttributeSet implements AttributeSet, Serializable
      {
         private AttributeSet attrs;

         ProxyAttributeSet(AttributeSet attrs)
         {
            this.attrs = attrs;
         }

         public Object getAttribute(Object key)
         {
            if (key == StyleConstants.FontSize)
            {
               // StyleConstants.FontSize is the _computed_ value.
               // Unlike CSS.FontSize.toStyleConstants(), StyleSheet.getFont()
               // preserves the necessary context to CSS.FontSize.getValue()
               // to honor W3C_LENGTH_UNITS, as well.
               return getFont(this).getSize();
            }
            else if (key == CSS.Attribute.FONT_SIZE
                  && !isDefined(CSS.Attribute.FONT_SIZE))
            {
               // Don't possibly inherit a _specified_ percentage value.
               // Use 'font-size: inherit' to force calculation at this level.
               return fontSizeInherit();
            }
            return attrs.getAttribute(key);
         }

         public int getAttributeCount()
         {
            return attrs.getAttributeCount();
         }

         public boolean isDefined(Object attrName)
         {
            return attrs.isDefined(attrName);
         }

         public boolean isEqual(AttributeSet attr)
         {
            return attrs.isEqual(attr);
         }

         public AttributeSet copyAttributes()
         {
            return attrs.copyAttributes();
         }

         public Enumeration<?> getAttributeNames()
         {
            return attrs.getAttributeNames();
         }

         public boolean containsAttribute(Object name, Object value)
         {
            return attrs.containsAttribute(name, value);
         }

         public boolean containsAttributes(AttributeSet attributes)
         {
            return attrs.containsAttributes(attributes);
         }

         public AttributeSet getResolveParent()
         {
            return attrs.getResolveParent();
         }

      } // class ProxyAttributeSet


   } // class StyleSheetFix


}
