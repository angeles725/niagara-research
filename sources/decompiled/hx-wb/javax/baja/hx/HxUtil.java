package javax.baja.hx;

import com.tridium.hx.HxHyperlinkInfo;
import com.tridium.session.NiagaraSuperSession;
import com.tridium.session.SessionManager;
import com.tridium.ui.theme.Theme;
import com.tridium.web.WebUtil;
import com.tridium.web.servlets.ViewAllOrdServlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import javax.baja.agent.AgentInfo;
import javax.baja.agent.AgentList;
import javax.baja.file.BExporter;
import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.BFont;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.gx.BBrush.Gradient;
import javax.baja.gx.BBrush.Image;
import javax.baja.gx.BBrush.LinearGradient;
import javax.baja.gx.BBrush.RadialGradient;
import javax.baja.gx.BBrush.Solid;
import javax.baja.gx.BBrush.Stop;
import javax.baja.io.HtmlWriter;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.nav.BINavNode;
import javax.baja.nre.function.ConsumerCanThrowException;
import javax.baja.nre.util.TextUtil;
import javax.baja.sys.BDouble;
import javax.baja.ui.BBorder;
import javax.baja.ui.BWidget;
import javax.baja.ui.enums.BAlign;
import javax.baja.ui.enums.BHalign;
import javax.baja.ui.enums.BValign;
import javax.baja.ui.pane.BScrollPane;
import javax.baja.xml.XWriter;

public class HxUtil {
   static String WEBKIT = "WebKit";
   static String JAVAFX = "JavaFX";
   static int pollFreq = 5000;
   static boolean spriteSheetsDisabled = true;
   static AtomicInteger imageId = new AtomicInteger(0);

   public static AgentInfo getDefaultView(HxOp op) {
      try {
         return op.getWebEnv().getDefaultView(op, getViews(op));
      } catch (Exception var2) {
         throw new RuntimeException(var2);
      }
   }

   public static AgentList getViews(HxOp op) {
      return HxHyperlinkInfo.getViews(op);
   }

   private static boolean isValidForViewSelector(AgentInfo info) {
      return !info.getAgentType().is(BExporter.TYPE);
   }

   public static AgentList getViewsForViewSelector(HxOp op) throws Exception {
      AgentList agents = getViews(op);
      ViewAllOrdServlet.removeDuplicateAgents(op, agents);
      return agents.filter(info -> isValidForViewSelector(info));
   }

   public static String makeFont(BFont font) {
      String name = getCSSFontName(font);
      double size = font.getSize();
      int style = font.getStyle();
      StringBuilder s = new StringBuilder();
      if ((style & 1) != 0) {
         s.append("bold ");
      }

      if ((style & 2) != 0) {
         s.append("italic ");
      }

      s.append(BDouble.encode(size)).append("px ");
      s.append(name);
      return s.toString();
   }

   public static String getCSSFontName(BFont font) {
      String fontName = font.getName();
      if ("sansserif".equalsIgnoreCase(fontName)) {
         return "sans-serif";
      } else {
         return "monospaced".equalsIgnoreCase(fontName) ? "monospace" : fontName;
      }
   }

   public static BColor makeColor(BBrush brush, BColor def) throws Exception {
      if (brush.isNull()) {
         return def;
      } else if (brush.getPaint() instanceof Solid) {
         return ((Solid)brush.getPaint()).getColor();
      } else if (!(brush.getPaint() instanceof Gradient)) {
         return def;
      } else {
         Stop[] stops = ((Gradient)brush.getPaint()).getStops();
         int red = 0;
         int blue = 0;
         int green = 0;

         for (int stop = 0; stop < stops.length; stop++) {
            BColor color = stops[stop].getColor();
            red += color.getRed();
            blue += color.getBlue();
            green += color.getGreen();
         }

         return BColor.make(red / stops.length, green / stops.length, blue / stops.length);
      }
   }

   public static String makeColor(BBrush brush, String def) throws Exception {
      BColor color = makeColor(brush, BColor.NULL);
      return color.equals(BColor.NULL) ? def : color.toHtmlStringWithAlpha();
   }

   public static String makeInsets(BInsets insets) throws Exception {
      StringBuilder s = new StringBuilder();
      s.append(insets.top).append("px ");
      s.append(insets.right).append("px ");
      s.append(insets.bottom).append("px ");
      s.append(insets.left).append("px");
      return s.toString();
   }

   public static String makeBorder(BBorder border) throws Exception {
      StringBuilder s = new StringBuilder();
      s.append("border-top:");
      makeBorder(border.topWidth, border.topStyle, border.topBrush, s);
      s.append("border-left:");
      makeBorder(border.leftWidth, border.leftStyle, border.leftBrush, s);
      s.append("border-right:");
      makeBorder(border.rightWidth, border.rightStyle, border.rightBrush, s);
      s.append("border-bottom:");
      makeBorder(border.bottomWidth, border.bottomStyle, border.bottomBrush, s);
      return s.toString();
   }

   private static void makeBorder(double width, int style, BBrush brush, StringBuilder s) throws Exception {
      s.append(width).append("px ");
      switch (style) {
         case 0:
            s.append("none ");
            break;
         case 1:
            s.append("solid ");
            break;
         case 2:
            s.append("dotted ");
            break;
         case 3:
            s.append("dashed ");
            break;
         case 4:
            s.append("groove ");
            break;
         case 5:
            s.append("solid ");
            break;
         case 6:
            s.append("inset ");
            break;
         case 7:
            s.append("outset ");
      }

      if (style == 6 || style == 7) {
         s.append("#ccc");
      } else if (style == 5) {
         s.append(((Solid)Theme.widget().getControlHighlight().getPaint()).getColor().toHtmlString());
      } else {
         s.append(makeColor(brush, BColor.black).toHtmlString());
      }

      s.append(";");
   }

   public static void makeAlignment(BHalign ha, BValign va, PropertiesCollection style) {
      if (ha == BHalign.center) {
         style.add("textAlign", "center");
      } else if (ha == BHalign.right) {
         style.add("textAlign", "right");
      } else if (ha != null) {
         style.add("textAlign", "left");
      }

      if (va == BValign.center) {
         style.add("verticalAlign", "middle");
      } else if (va == BValign.top) {
         style.add("verticalAlign", "top");
      } else if (va != null) {
         style.add("verticalAlign", "bottom");
      }
   }

   public static void makeAlignment(BAlign align, PropertiesCollection style) {
      if (align == BAlign.center) {
         style.add("textAlign", "center");
      } else if (align == BAlign.right) {
         style.add("textAlign", "right");
      } else if (align != null) {
         style.add("textAlign", "left");
      }

      if (align == BAlign.center) {
         style.add("verticalAlign", "middle");
      } else if (align == BAlign.bottom) {
         style.add("verticalAlign", "bottom");
      } else if (align != null) {
         style.add("verticalAlign", "top");
      }
   }

   public static void makeFont(BBrush foreground, BFont font, PropertiesCollection style, PropertiesCollection properties, boolean wrap, HxOp op) throws Exception {
      if (!wrap) {
         style.add("whiteSpace", "nowrap");
      }

      if (!foreground.isNull()) {
         style.add("color", makeColor(foreground, BColor.black).toHtmlStringWithAlpha());
         if (foreground.getPaint() instanceof Gradient) {
            String gradient = makeGradientString((Gradient)foreground.getPaint());
            style.add("background", gradient);
            style.add("-webkit-background-clip", "text");
            style.add("-webkit-text-fill-color", "transparent");
         } else {
            style.add("background", "");
            style.add("-webkit-background-clip", "");
            style.add("-webkit-text-fill-color", "");
         }
      } else {
         style.add("color", "");
         style.add("background", "");
         style.add("-webkit-background-clip", "");
         style.add("-webkit-text-fill-color", "");
      }

      if (font.isNull() && properties != null) {
         properties.append("className", "defaultNssFont");
         style.add("fontSize", "");
         style.add("fontFamily", "");
         style.add("fontWeight", "");
         style.add("textDecoration", "");
         style.add("fontStyle", "");
      } else {
         if (!font.isNull()) {
            style.add("fontSize", (int)font.getSize() + "px");
            if (properties == null) {
               style.add("fontFamily", getCSSFontName(font));
            } else {
               String var8 = font.getName().toLowerCase();
               switch (var8) {
                  case "sansserif":
                  case "sans-serif":
                     properties.append("className", "ux-font-family-sans-serif");
                     break;
                  case "monospaced":
                  case "monospace":
                     properties.append("className", "ux-font-family-monospace");
                     break;
                  case "serif":
                     properties.append("className", "ux-font-family-serif");
                     break;
                  default:
                     style.add("fontFamily", getCSSFontName(font));
               }
            }
         } else {
            style.add("fontSize", "");
            style.add("fontFamily", "");
         }

         if (!font.isNull() && font.isBold()) {
            style.add("fontWeight", "bold");
         } else {
            style.add("fontWeight", "normal");
         }

         if (!font.isNull() && font.isUnderline()) {
            style.add("textDecoration", "underline");
         } else {
            style.add("textDecoration", "");
         }

         if (!font.isNull() && font.isItalic()) {
            style.add("fontStyle", "italic");
         } else {
            style.add("fontStyle", "");
         }
      }
   }

   public static void makeBackground(BBrush bg, BBrush def, PropertiesCollection style, HxOp op) throws Exception {
      boolean unsafe = false;
      if (!bg.isNull()) {
         StringBuilder background = new StringBuilder();
         if (bg.getPaint() instanceof Solid) {
            background.append(makeColor(bg, BColor.NULL).toHtmlStringWithAlpha());
         } else if (bg.getPaint() instanceof Gradient) {
            unsafe = true;
            background.append(makeGradientString((Gradient)bg.getPaint()));
         }

         if (bg.getPaint() instanceof Image) {
            Image brush = (Image)bg.getPaint();
            BImage backgroundImage = brush.getImage();
            if (backgroundImage == null || backgroundImage.getOrdList().size() < 1) {
               style.add("backgroundImage", "");
               style.add("backgroundColor", "");
               return;
            }

            style.setSnoopEnabled(false);
            background.append("url(").append(escapeJsStringLiteral(WebUtil.toUri(op, op.getRequest(), backgroundImage.getOrdList().get(0)))).append(") ");
            switch (brush.getTile()) {
               case 0:
                  background.append("no-repeat ");
                  break;
               case 1:
                  background.append("repeat ");
                  break;
               case 2:
                  background.append("repeat-x ");
                  break;
               case 3:
                  background.append("repeat-y ");
            }

            label49:
            switch (brush.getValign()) {
               case 0:
                  switch (brush.getHalign()) {
                     case 0:
                        background.append("center center ");
                     case 1:
                     case 2:
                     default:
                        break label49;
                     case 3:
                        background.append("center left ");
                        break label49;
                     case 4:
                        background.append("center right ");
                        break label49;
                  }
               case 1:
                  switch (brush.getHalign()) {
                     case 0:
                        background.append("top center ");
                     case 1:
                     case 2:
                     default:
                        break label49;
                     case 3:
                        background.append("top left ");
                        break label49;
                     case 4:
                        background.append("top right ");
                        break label49;
                  }
               case 2:
                  switch (brush.getHalign()) {
                     case 0:
                        background.append("bottom center ");
                     case 1:
                     case 2:
                     default:
                        break;
                     case 3:
                        background.append("bottom left ");
                        break;
                     case 4:
                        background.append("bottom right ");
                  }
            }
         }

         if (unsafe) {
            style.addUnsafe("background", background.toString());
         } else {
            style.add("background", background.toString());
         }
      } else if (def != null && !def.isNull()) {
         makeBackground(def, null, style, op);
      } else {
         style.add("backgroundImage", "");
         style.add("backgroundColor", "");
      }
   }

   public static String makeGradientString(Gradient gradient) {
      if (gradient instanceof LinearGradient) {
         LinearGradient linear = (LinearGradient)gradient;
         StringBuilder b = new StringBuilder();
         String cssAngle = "" + (90.0 - linear.getAngle());
         Stop[] stops = linear.getStops();

         for (Stop stop : stops) {
            b.append(",");
            b.append(stop.getColor().toHtmlStringWithAlpha() + " " + stop.getOffset() + "%");
         }

         return "linear-gradient(" + cssAngle + "deg " + b + ")";
      } else if (!(gradient instanceof RadialGradient)) {
         return "";
      } else {
         RadialGradient radial = (RadialGradient)gradient;
         Stop[] stops = radial.getStops();
         StringBuilder b = new StringBuilder();
         b.append("radial-gradient( ");
         b.append(radial.getRadius());
         b.append("% ");
         b.append(radial.getRadius());
         b.append("% ");
         b.append("at ");
         b.append(radial.getCenter().x());
         b.append("% ");
         b.append(radial.getCenter().y());
         b.append("% ");

         for (Stop stop : stops) {
            b.append(",");
            b.append(stop.getColor().toHtmlStringWithAlpha() + " " + stop.getOffset() + "%");
         }

         b.append(")");
         return b.toString();
      }
   }

   public static boolean isWidgetEffectivelyVisible(BWidget widget) {
      return widget.getVisible() || widget.getParentWidget() instanceof BScrollPane;
   }

   public static boolean isWidgetEffectivelyVisible(BWidget widget, int width, int height) {
      return width != 0 && height != 0 && (widget.getVisible() || widget.getParentWidget() instanceof BScrollPane);
   }

   private static String getBorderWidth(double width) {
      return width + "px";
   }

   public static void makeBorder(BBorder b, PropertiesCollection style) throws Exception {
      if (b.isNull()) {
         style.add("borderTopWidth", "");
         style.add("borderTopStyle", "");
         style.add("borderTopColor", "");
         style.add("borderLeftWidth", "");
         style.add("borderLeftStyle", "");
         style.add("borderLeftColor", "");
         style.add("borderRightWidth", "");
         style.add("borderRightStyle", "");
         style.add("borderRightColor", "");
         style.add("borderBottomWidth", "");
         style.add("borderBottomStyle", "");
         style.add("borderBottomColor", "");
         style.add("borderImage", "");
      } else if (b.encodeToString().indexOf("bottom(") == -1 && b.topBrush.getPaint() instanceof Gradient) {
         Gradient gradient = (Gradient)b.topBrush.getPaint();
         String gradientString = makeGradientString(gradient) + " " + (int)b.topWidth + " stretch";
         style.add("borderStyle", "solid");
         style.addUnsafe("borderImage", gradientString);
         style.add("borderWidth", getBorderWidth(b.topWidth));
      } else {
         style.add("borderTopWidth", getBorderWidth(b.topWidth));
         style.add("borderTopColor", makeColor(b.topBrush, BColor.NULL).toHtmlStringWithAlpha());
         switch (b.topStyle) {
            case 0:
               style.add("borderTopStyle", "none");
               break;
            case 1:
               style.add("borderTopStyle", "solid");
               break;
            case 2:
               style.add("borderTopStyle", "dotted");
               break;
            case 3:
               style.add("borderTopStyle", "dashed");
               break;
            case 4:
               style.add("borderTopStyle", "groove");
               style.add("borderTopColor", "inherit");
               break;
            case 5:
               style.add("borderTopStyle", "ridge");
               style.add("borderTopColor", "inherit");
               break;
            case 6:
               style.add("borderTopStyle", "inset");
               style.add("borderTopColor", "inherit");
               break;
            case 7:
               style.add("borderTopStyle", "outset");
               style.add("borderTopColor", "inherit");
         }

         style.add("borderLeftWidth", getBorderWidth(b.leftWidth));
         style.add("borderLeftColor", makeColor(b.leftBrush, BColor.NULL).toHtmlStringWithAlpha());
         switch (b.leftStyle) {
            case 0:
               style.add("borderLeftStyle", "none");
               break;
            case 1:
               style.add("borderLeftStyle", "solid");
               break;
            case 2:
               style.add("borderLeftStyle", "dotted");
               break;
            case 3:
               style.add("borderLeftStyle", "dashed");
               break;
            case 4:
               style.add("borderLeftStyle", "groove");
               style.add("borderLeftColor", "inherit");
               break;
            case 5:
               style.add("borderLeftStyle", "ridge");
               style.add("borderLeftColor", "inherit");
               break;
            case 6:
               style.add("borderLeftStyle", "inset");
               style.add("borderLeftColor", "inherit");
               break;
            case 7:
               style.add("borderLeftStyle", "outset");
               style.add("borderLeftColor", "inherit");
         }

         style.add("borderRightWidth", getBorderWidth(b.rightWidth));
         style.add("borderRightColor", makeColor(b.rightBrush, BColor.NULL).toHtmlStringWithAlpha());
         switch (b.rightStyle) {
            case 0:
               style.add("borderRightStyle", "none");
               break;
            case 1:
               style.add("borderRightStyle", "solid");
               break;
            case 2:
               style.add("borderRightStyle", "dotted");
               break;
            case 3:
               style.add("borderRightStyle", "dashed");
               break;
            case 4:
               style.add("borderRightStyle", "groove");
               style.add("borderRightColor", "inherit");
               break;
            case 5:
               style.add("borderRightStyle", "ridge");
               style.add("borderRightColor", "inherit");
               break;
            case 6:
               style.add("borderRightStyle", "inset");
               style.add("borderRightColor", "inherit");
               break;
            case 7:
               style.add("borderRightStyle", "outset");
               style.add("borderRightColor", "inherit");
         }

         style.add("borderBottomWidth", getBorderWidth(b.bottomWidth));
         style.add("borderBottomColor", makeColor(b.bottomBrush, BColor.NULL).toHtmlStringWithAlpha());
         switch (b.bottomStyle) {
            case 0:
               style.add("borderBottomStyle", "none");
               break;
            case 1:
               style.add("borderBottomStyle", "solid");
               break;
            case 2:
               style.add("borderBottomStyle", "dotted");
               break;
            case 3:
               style.add("borderBottomStyle", "dashed");
               break;
            case 4:
               style.add("borderBottomStyle", "groove");
               style.add("borderBottomColor", "inherit");
               break;
            case 5:
               style.add("borderBottomStyle", "ridge");
               style.add("borderBottomColor", "inherit");
               break;
            case 6:
               style.add("borderBottomStyle", "inset");
               style.add("borderBottomColor", "inherit");
               break;
            case 7:
               style.add("borderBottomStyle", "outset");
               style.add("borderBottomColor", "inherit");
         }
      }
   }

   public static void makePadding(BInsets padding, PropertiesCollection style) throws Exception {
      style.add("paddingTop", (int)padding.top + "px");
      style.add("paddingBottom", (int)padding.bottom + "px");
      style.add("paddingLeft", (int)padding.left + "px");
      style.add("paddingRight", (int)padding.right + "px");
   }

   public static void makeMargin(BInsets padding, PropertiesCollection style) throws Exception {
      style.add("marginTop", (int)padding.top + "px");
      style.add("marginBottom", (int)padding.bottom + "px");
      style.add("marginLeft", (int)padding.left + "px");
      style.add("marginRight", (int)padding.right + "px");
   }

   public static void writeFormValue(String name, String value, HxOp op) throws Exception {
      HtmlWriter out = op.getHtmlWriter();
      out.w("<input ");
      out.attr("style", "display: none");
      out.w(" ");
      out.attr("type", "text");
      out.w(" ");
      out.attr("id", op.scope(name));
      out.w(" ");
      out.attr("name", op.scope(name));
      out.w(" ");
      out.attr("value", value);
      out.w("/>");
   }

   public static void persistFormValue(String name, HxOp op) throws Exception {
      if (op.getFormValue(name) != null) {
         writeFormValue(name, op.getFormValue(name), op);
      }
   }

   public static String decode(String s) {
      StringBuilder buf = new StringBuilder(s.length() + 10);
      char[] c = s.toCharArray();

      for (int i = 0; i < c.length; i++) {
         if (c[i] == '+') {
            buf.append(" ");
         } else if (c[i] == '%') {
            i++;
            int val = 0;
            val += fromHex(c[i++]) * 16;
            val += fromHex(c[i]);
            if ((val & 224) == 192) {
               int low = 0;
               i++;
               low += fromHex(c[++i]) * 16;
               low += fromHex(c[++i]);
               low = (val & 1) << 6 | low & 63;
               int three = val >> 1 & 15;
               val = three << 7 | low;
            } else if ((val & 224) == 224) {
               int two = 0;
               int one = 0;
               i++;
               two += fromHex(c[++i]) * 16;
               two += fromHex(c[++i]);
               i++;
               one += fromHex(c[++i]) * 16;
               one += fromHex(c[++i]);
               val = (val & 15) << 12 | (two & 63) << 6 | (one & 63) << 0;
            }

            buf.append((char)val);
         } else {
            buf.append(c[i]);
         }
      }

      return buf.toString();
   }

   private static int fromHex(char ch) {
      if (ch >= '0' && ch <= '9') {
         return ch - 48;
      } else if (ch >= 'A' && ch <= 'F') {
         return ch - 65 + 10;
      } else {
         throw new IllegalArgumentException("Invalid hex character: " + ch);
      }
   }

   @Deprecated
   public static void addTouchScroll(String elemID, HxOp op) {
   }

   public static boolean isPost(HxOp op) {
      String method = op.getRequest().getMethod().toLowerCase();
      return !method.equals("get");
   }

   public static String getOuterQuote(HxOp op) {
      return !isPost(op) ? "\"" : "&quot;";
   }

   public static String getInnerQuote(HxOp op) {
      return !isPost(op) ? "&quot;" : "'";
   }

   @Deprecated
   public static void makeImage(BImage image, HxOp op) throws Exception {
      makeImage(image, null, "", op);
   }

   @Deprecated
   public static void makeImage(BImage image, String attrs, HxOp op) throws Exception {
      makeImage(image, attrs, "", op);
   }

   @Deprecated
   public static void makeImage(BImage image, String attrs, String alt, HxOp op) throws Exception {
      makeImage(image, attrs, alt, null, op);
   }

   @Deprecated
   public static void makeImage(BImage image, String attrs, String alt, String quote, HxOp op) throws Exception {
      if (image != null && image.getOrdList().size() != 0) {
         HtmlWriter out = op.getHtmlWriter();
         String ord = image.getOrdList().get(0).toString();
         if (alt == null) {
            alt = "";
         }

         out.w("<img");
         attr(" alt", alt, quote, out);
         if (alt != null && alt.length() > 0) {
            attr(" title", alt, quote, out);
         }

         attr(" src", ord, quote, out);
         if (attrs != null && attrs.length() > 0) {
            out.w(" ").w(attrs);
         }

         out.w("/>");
      }
   }

   public static void makeImageJS(BImage image, HxOp op) throws Exception {
      makeImageJS(image, null, "", op);
   }

   public static void makeImageJS(BImage image, String attrs, HxOp op) throws Exception {
      makeImageJS(image, attrs, "", op);
   }

   public static void makeImageJS(BImage image, String attrs, String alt, HxOp op) throws Exception {
      makeImageJS(image, attrs, alt, null, op);
   }

   public static void makeImageJS(BImage image, String attrs, String alt, String quote, HxOp op) throws Exception {
      if (image != null && image.getOrdList().size() != 0) {
         if (attrs == null) {
            attrs = "";
         }

         if (alt == null) {
            alt = "";
         }

         if (quote == null) {
            quote = "'";
         }

         if (spriteSheetsDisabled) {
            String ord = image.getOrdList().get(0).toString();
            HtmlWriter out = op.getHtmlWriter();
            out.w("<span class=" + quote + "hxImageWrapper" + quote + ">");
            out.w("<span>");
            out.w("<img");
            attr(" alt", alt, quote, out);
            if (!alt.isEmpty()) {
               attr(" title", alt, quote, out);
            }

            attr(" src", ord, quote, out);
            if (!attrs.isEmpty()) {
               out.w(" ").w(attrs);
            }

            out.w("/>");
            out.w("</span>");
            out.w("</span>");
         } else {
            HtmlWriter html = op.getHtmlWriter();
            html.w("<span class=" + quote + "hxImageWrapper" + quote + ">");
            StringBuilder ordArray = new StringBuilder("[");
            StringBuilder opArray = new StringBuilder("[");

            for (int i = 0; i < image.getOrdList().size(); i++) {
               HxOp scopeOp = op.make("hx_image_" + imageId.getAndIncrement(), op);
               if (i != 0) {
                  ordArray.append(", ");
                  opArray.append(", ");
               }

               ordArray.append("'" + escapeJsStringLiteral(WebUtil.toUri(op, op.getRequest(), image.getOrdList().get(i))) + "'");
               opArray.append("'" + escapeJsStringLiteral(scopeOp.scope("value")) + "'");
               html.w("<span ");
               attr("id", scopeOp.scope("value"), quote, html);
               html.w("></span>");
            }

            ordArray.append("]");
            opArray.append("]");
            html.w("</span>");
            Writer writer = startOnloadWriter(op);

            try {
               HtmlWriter js = op.getHtmlWriter();
               if (quote.equals("'")) {
                  js.w(
                     "hx.makeImage("
                        + ordArray.toString()
                        + ", "
                        + opArray.toString()
                        + ", \""
                        + XWriter.safeToString(alt, false)
                        + "\", \""
                        + attrs
                        + "\").catch(function (err) { console.error(err); });"
                  );
               } else {
                  js.w(
                     "hx.makeImage("
                        + ordArray.toString()
                        + ", "
                        + opArray.toString()
                        + ", '"
                        + XWriter.safeToString(alt, false)
                        + "', '"
                        + attrs
                        + "').catch(function (err) { console.error(err); });"
                  );
               }
            } finally {
               finishOnloadWriter(writer, op);
            }
         }
      }
   }

   public static void changeImageJS(String parent, BImage image, String attrs, HxOp op) throws Exception {
      HtmlWriter html = op.getHtmlWriter();
      html.w(changeImageJSInvokeCode(parent, image, attrs, op));
   }

   public static String changeImageJSInvokeCode(String parent, BImage image, String attrs, HxOp op) {
      if (attrs == null) {
         attrs = "";
      }

      StringBuilder ordArray = new StringBuilder("[");

      for (int i = 0; i < image.getOrdList().size(); i++) {
         if (i != 0) {
            ordArray.append(", ");
         }

         ordArray.append("'" + escapeJsStringLiteral(WebUtil.toUri(op, op.getRequest(), image.getOrdList().get(i))) + "'");
      }

      ordArray.append("]");
      return "hx.changeImage('" + parent + "', " + ordArray.toString() + ", \"" + attrs + "\").catch(function (err) { console.error(err); });";
   }

   public static final HtmlWriter attr(String name, String value, String quote, HtmlWriter out) {
      if (quote == null) {
         quote = "'";
      }

      return out.w(name).w('=').w(quote).safe(value).w(quote);
   }

   public static Writer startOnloadWriter(HxOp op) throws IOException {
      Writer contentHx = new StringWriter();
      op.setWriter(new PrintWriter(contentHx));
      return contentHx;
   }

   public static void finishOnloadWriter(Writer writer, HxOp op) throws IOException {
      op.resetWriter();
      op.addOnload(writer.toString());
   }

   @Deprecated
   public static boolean isUserAgentWithoutInnerDivTouchScroll(HxOp op) {
      return false;
   }

   public static int getPollFreq() {
      return pollFreq;
   }

   public static String encodeSingleQuotes(String text) {
      return encodeQuotes(text, "'");
   }

   public static String encodeDoubleQuotes(String text) {
      return encodeQuotes(text, "\"");
   }

   private static String encodeQuotes(String text, String quoteCharacter) {
      return text.replace(quoteCharacter, "\\" + quoteCharacter);
   }

   public static String unescapeJsForInvocation(String s) {
      return s == null ? null : s.replaceAll("&quot;", "\"");
   }

   public static void writeSafeAnchor(BOrd ord, String displayText, HxOp op) throws Exception {
      writeSafeAnchorStart(ord, op);
      HtmlWriter out = op.getHtmlWriter();
      out.w(">");
      out.safe(displayText);
      out.w("</a>");
   }

   public static void writeSafeAnchorStart(BOrd ord, HxOp op) throws Exception {
      HtmlWriter out = op.getHtmlWriter();
      out.w("<!-- @noSnoop --><a href='" + encodeOrdForHref(ord, op) + '\'');
   }

   public static String encodeOrdForHref(BOrd ord, HxOp op) {
      String s = op.toUri(ord);
      int codePointCount = s.codePointCount(0, s.length());
      StringBuilder b = new StringBuilder(s.length());

      for (int i = 0; i < codePointCount; i++) {
         int codePoint = s.codePointAt(i);
         switch (codePoint) {
            case 10:
               b.append("%0A");
               break;
            case 13:
               b.append("%0D");
               break;
            case 34:
               b.append("%22");
               break;
            case 39:
               b.append("%27");
               break;
            case 92:
               b.append("%5C");
               break;
            case 8232:
               b.append("%E2%80%A8");
               break;
            case 8233:
               b.append("%E2%80%A9");
               break;
            default:
               b.appendCodePoint(codePoint);
         }
      }

      return b.toString();
   }

   public static String encodeURLForHref(String link) throws Exception {
      return link != null && (link.contains("'") || link.contains("\"")) ? URLEncoder.encode(link, "UTF-8") : link;
   }

   public static String escapeJsStringLiteral(String s) {
      return WebUtil.escapeJsStringLiteral(s);
   }

   public static String marshal(ConsumerCanThrowException<? super HxOp, Exception> r, HxOp op) throws Exception {
      StringWriter content = new StringWriter();
      op.setWriter(new PrintWriter(content));

      try {
         r.accept(op);
      } finally {
         op.resetWriter();
      }

      return content.toString();
   }

   public static String encodeText(String text) throws Exception {
      StringBuilder out = new StringBuilder();
      int len = text.length();
      int i = 0;

      while (i < len) {
         char c = text.charAt(i++);
         if (Character.isHighSurrogate(c) && i < len) {
            char c2 = text.charAt(i);
            if (Character.isLowSurrogate(c2)) {
               out.append("&#").append(Character.toCodePoint(c, c2)).append(';');
               i++;
               continue;
            }
         }

         if (c >= ' ' && c <= '~' && c != '\'' && c != '"' && c != '\\') {
            if (c == '<') {
               out.append("&lt;");
            } else if (c == '>') {
               out.append("&gt;");
            } else if (c == '&') {
               out.append("&amp;");
            } else {
               out.append(c);
            }
         } else if (c == '\n' || c == '\r') {
            out.append("<br/>");
         } else if (c == '"') {
            out.append("\\\"");
         } else if (c == '\'') {
            out.append("\\'");
         } else if (c == '\\') {
            out.append("\\\\");
         } else {
            out.append("&#").append((int)c).append(';');
         }
      }

      return out.toString();
   }

   public static String getUpdateValueInvokeCode(String formName, String stringValue, String quote, HxOp op) throws Exception {
      boolean needNewLineFix = false;
      char backslash = '\\';
      char SUBSTITUTE = '�';
      String match = "\\";
      if (stringValue.indexOf(match) > -1) {
         stringValue = TextUtil.replace(stringValue, match, "" + SUBSTITUTE + "s");
         needNewLineFix = true;
      }

      if (stringValue.indexOf(quote) > -1) {
         stringValue = TextUtil.replace(stringValue, quote, backslash + quote);
      }

      char newLine = '\n';
      if (stringValue.indexOf(newLine) > -1) {
         stringValue = TextUtil.replace(stringValue, "" + newLine, "" + SUBSTITUTE + 'n');
         needNewLineFix = true;
      }

      newLine = '\r';
      if (stringValue.indexOf(newLine) > -1) {
         stringValue = TextUtil.replace(stringValue, "" + newLine, "" + SUBSTITUTE + 'r');
         needNewLineFix = true;
      }

      StringBuilder b = new StringBuilder();
      b.append("hx.updateValue(").append(quote).append(formName).append(quote).append(",");
      b.append(quote).append(stringValue).append(quote).append(",");
      b.append(needNewLineFix).append(");");
      return b.toString();
   }

   public static void writeContextMenuListItem(BINavNode navNode, HxOp op) throws Exception {
      String quote = getOuterQuote(op);
      String onclick = "hx.hyperlink(" + quote + encodeOrdForHref(navNode.getNavOrd(), op) + quote + ");";
      String displayName = navNode.getNavDisplayName(op);
      BOrd iconOrd = null;
      if (navNode.getNavIcon() != null && navNode.getNavIcon().getOrdList().size() > 0) {
         iconOrd = navNode.getNavIcon().getOrdList().get(0);
      }

      writeContextMenuListItem(onclick, displayName, iconOrd, op);
   }

   public static void writeContextMenuListItem(String onclick, String displayName, BOrd imageOrd, HxOp op) throws Exception {
      String quote = getOuterQuote(op);
      HtmlWriter out = op.getHtmlWriter();
      String cls = "class";
      out.w("<li ").attr("class", "context-menu-item");
      out.w(" onmouseover='this.setAttribute(" + quote + cls + quote + ',' + quote + "context-menu-item hover" + quote + ");'");
      out.w(" onmouseout='this.setAttribute(" + quote + cls + quote + ',' + quote + "context-menu-item" + quote + ");'");
      out.w(" onclick='");
      out.w(onclick);
      out.w("'>");
      out.w("<span class='display'><span class='icon'>");
      if (imageOrd != null && !imageOrd.isNull()) {
         makeImageJS(BImage.make(imageOrd), null, displayName, op);
      }

      out.w("</span><span class='displayName'>");
      out.safe(displayName);
      out.w("</span></span>");
      out.w("</li>");
   }

   public static String safe(String s) {
      return XWriter.safeToString(s, true);
   }

   public static String getCsrfTokenQueryString() {
      NiagaraSuperSession session = SessionManager.getCurrentNiagaraSuperSession();

      try {
         if (session != null) {
            return "?csrfToken=" + URLEncoder.encode(session.getCsrfToken(), "UTF-8");
         }
      } catch (IOException var2) {
         BHxView.log.log(Level.INFO, "CSRF token encoding error", (Throwable)var2);
      }

      return "";
   }

   public static void addJavascriptOnload(HxOp op) throws Exception {
      HtmlWriter out = op.getHtmlWriter();
      out.w("/* @noSnoop */");
      BOrd[] scripts = op.getJavaScriptOrds();

      for (int i = 0; i < scripts.length; i++) {
         if (i == 0) {
            out.w("hx.addJavaScript(['");
         } else {
            out.w("','");
         }

         String javascriptUrl = WebUtil.toUri(op, op.getRequest(), scripts[i]);
         out.w(javascriptUrl);
      }

      if (scripts.length > 0) {
         out.w("'], function(){");
      }

      String[] global = op.getGlobal();
      if (global.length > 0) {
         for (int i = 0; i < global.length; i++) {
            if (global[i] != null) {
               out.w(global[i]);
            }
         }
      }

      String[] codes = op.getOnload();

      for (int ix = 0; ix < codes.length; ix++) {
         if (codes[ix] != null) {
            out.w(unescapeJsForInvocation(codes[ix]));
         }
      }

      if (scripts.length > 0) {
         out.w("});");
      }
   }

   public static OrdTarget getMinimalOrdTarget(OrdTarget target) {
      if (!(target instanceof HxOp)) {
         return target;
      } else {
         OrdTarget t = target;

         while (t instanceof HxOp) {
            t = t.getBaseOrdTarget();
         }

         return OrdTarget.makeWithFacetsAndLanguage(t, target.get(), target.getFacets(), target.getLanguage());
      }
   }

   static {
      try {
         pollFreq = AccessController.doPrivileged((PrivilegedAction<Integer>)(() -> Integer.getInteger("hx.poll.freq", 5000)));
         spriteSheetsDisabled = AccessController.doPrivileged((PrivilegedAction<Boolean>)(() -> Boolean.getBoolean("hx.spritesheets.disabled")));
      } catch (Exception var1) {
      }
   }
}
