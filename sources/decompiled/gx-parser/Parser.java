package com.tridium.gx.parser;

import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.BEllipseGeom;
import javax.baja.gx.BFont;
import javax.baja.gx.BImage;
import javax.baja.gx.BInsets;
import javax.baja.gx.BLineGeom;
import javax.baja.gx.BPathGeom;
import javax.baja.gx.BPen;
import javax.baja.gx.BPoint;
import javax.baja.gx.BPolygonGeom;
import javax.baja.gx.BRectGeom;
import javax.baja.gx.BTransform;
import javax.baja.gx.IPathGeom;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.TextUtil;

public class Parser {
   public Token cur = new Token();
   public Token peek = new Token();
   Tokenizer tokenizer;

   public Parser(String s) {
      this(s, false);
   }

   public Parser(String s, boolean pathGeom) {
      this.tokenizer = new Tokenizer(s, pathGeom);
      this.initConsume();
   }

   public BLineGeom parseLine() {
      if (this.cur.id("null")) {
         this.consume();
         return BLineGeom.NULL;
      } else {
         double x1 = this.matchNum();
         this.ignoreComma();
         double y1 = this.matchNum();
         this.ignoreComma();
         double x2 = this.matchNum();
         this.ignoreComma();
         double y2 = this.matchNum();
         return BLineGeom.make(x1, y1, x2, y2);
      }
   }

   public BRectGeom parseRect() {
      if (this.cur.id("null")) {
         this.consume();
         return BRectGeom.NULL;
      } else {
         double x = this.matchNum();
         this.ignoreComma();
         double y = this.matchNum();
         this.ignoreComma();
         double w = this.matchNum();
         this.ignoreComma();
         double h = this.matchNum();
         return BRectGeom.make(x, y, w, h);
      }
   }

   public BEllipseGeom parseEllipse() {
      if (this.cur.id("null")) {
         this.consume();
         return BEllipseGeom.NULL;
      } else {
         double x = this.matchNum();
         this.ignoreComma();
         double y = this.matchNum();
         this.ignoreComma();
         double w = this.matchNum();
         this.ignoreComma();
         double h = this.matchNum();
         return BEllipseGeom.make(x, y, w, h);
      }
   }

   public BPolygonGeom parsePolygon() {
      if (this.cur.id("null")) {
         this.consume();
         return BPolygonGeom.NULL;
      } else {
         double[] x = new double[8];
         double[] y = new double[8];

         int n;
         for (n = 0; !this.isEnd(); n++) {
            if (n >= x.length) {
               x = grow(x);
               y = grow(y);
            }

            x[n] = this.matchNum();
            this.ignoreComma();
            y[n] = this.matchNum();
            this.ignoreComma();
            if (this.cur.type == 3) {
               this.consume();
            }
         }

         return BPolygonGeom.make(x, y, n);
      }
   }

   private static double[] grow(double[] a) {
      double[] g = new double[a.length * 2];
      System.arraycopy(a, 0, g, 0, a.length);
      return g;
   }

   public BPathGeom parsePath() {
      Array<IPathGeom.Segment> array = new Array(IPathGeom.Segment.class);
      int command = 63;

      while (!this.isEnd()) {
         if (this.cur.type == 21) {
            command = this.cur.pathcmd;
            this.consume();
            if (command == 90 || command == 122) {
               array.add(new IPathGeom.ClosePath());
            }
         } else {
            IPathGeom.Segment seg = null;
            switch (command) {
               case 65:
                  seg = this.parseArcTo(true);
                  break;
               case 66:
               case 68:
               case 69:
               case 70:
               case 71:
               case 73:
               case 74:
               case 75:
               case 78:
               case 79:
               case 80:
               case 82:
               case 85:
               case 87:
               case 88:
               case 89:
               case 90:
               case 91:
               case 92:
               case 93:
               case 94:
               case 95:
               case 96:
               case 98:
               case 100:
               case 101:
               case 102:
               case 103:
               case 105:
               case 106:
               case 107:
               case 110:
               case 111:
               case 112:
               case 114:
               case 117:
               default:
                  throw new ParseException("Unknown comand " + (char)command);
               case 67:
                  seg = this.parseCurveTo(true);
                  break;
               case 72:
                  seg = this.parseHLineTo(true);
                  break;
               case 76:
                  seg = this.parseLineTo(true);
                  break;
               case 77:
                  seg = this.parseMoveTo(true);
                  break;
               case 81:
                  seg = this.parseQuadTo(true);
                  break;
               case 83:
                  seg = this.parseSmoothCurveTo(true);
                  break;
               case 84:
                  seg = this.parseSmoothQuadTo(true);
                  break;
               case 86:
                  seg = this.parseVLineTo(true);
                  break;
               case 97:
                  seg = this.parseArcTo(false);
                  break;
               case 99:
                  seg = this.parseCurveTo(false);
                  break;
               case 104:
                  seg = this.parseHLineTo(false);
                  break;
               case 108:
                  seg = this.parseLineTo(false);
                  break;
               case 109:
                  seg = this.parseMoveTo(false);
                  break;
               case 113:
                  seg = this.parseQuadTo(false);
                  break;
               case 115:
                  seg = this.parseSmoothCurveTo(false);
                  break;
               case 116:
                  seg = this.parseSmoothQuadTo(false);
                  break;
               case 118:
                  seg = this.parseVLineTo(false);
            }

            array.add(seg);
         }
      }

      return BPathGeom.make((IPathGeom.Segment[])array.trim());
   }

   IPathGeom.MoveTo parseMoveTo(boolean abs) {
      double x = this.matchNum();
      this.ignoreComma();
      double y = this.matchNum();
      return new IPathGeom.MoveTo(abs, x, y);
   }

   IPathGeom.LineTo parseLineTo(boolean abs) {
      double x = this.matchNum();
      this.ignoreComma();
      double y = this.matchNum();
      return new IPathGeom.LineTo(abs, x, y);
   }

   IPathGeom.HLineTo parseHLineTo(boolean abs) {
      double x = this.matchNum();
      return new IPathGeom.HLineTo(abs, x);
   }

   IPathGeom.VLineTo parseVLineTo(boolean abs) {
      double y = this.matchNum();
      return new IPathGeom.VLineTo(abs, y);
   }

   IPathGeom.CurveTo parseCurveTo(boolean abs) {
      double x1 = this.matchNum();
      this.ignoreComma();
      double y1 = this.matchNum();
      this.ignoreComma();
      double x2 = this.matchNum();
      this.ignoreComma();
      double y2 = this.matchNum();
      this.ignoreComma();
      double x = this.matchNum();
      this.ignoreComma();
      double y = this.matchNum();
      this.ignoreComma();
      return new IPathGeom.CurveTo(abs, x1, y1, x2, y2, x, y);
   }

   IPathGeom.SmoothCurveTo parseSmoothCurveTo(boolean abs) {
      double x2 = this.matchNum();
      this.ignoreComma();
      double y2 = this.matchNum();
      this.ignoreComma();
      double x = this.matchNum();
      this.ignoreComma();
      double y = this.matchNum();
      this.ignoreComma();
      return new IPathGeom.SmoothCurveTo(abs, x2, y2, x, y);
   }

   IPathGeom.QuadTo parseQuadTo(boolean abs) {
      double x1 = this.matchNum();
      this.ignoreComma();
      double y1 = this.matchNum();
      this.ignoreComma();
      double x = this.matchNum();
      this.ignoreComma();
      double y = this.matchNum();
      this.ignoreComma();
      return new IPathGeom.QuadTo(abs, x1, y1, x, y);
   }

   IPathGeom.SmoothQuadTo parseSmoothQuadTo(boolean abs) {
      double x = this.matchNum();
      this.ignoreComma();
      double y = this.matchNum();
      this.ignoreComma();
      return new IPathGeom.SmoothQuadTo(abs, x, y);
   }

   IPathGeom.ArcTo parseArcTo(boolean abs) {
      double rx = this.matchNum();
      this.ignoreComma();
      double ry = this.matchNum();
      this.ignoreComma();
      double xAxisRotation = this.matchNum();
      this.ignoreComma();
      boolean largeArcFlag = this.matchArcFlag();
      this.ignoreComma();
      boolean sweepFlag = this.matchArcFlag();
      this.ignoreComma();
      double x = this.matchNum();
      this.ignoreComma();
      double y = this.matchNum();
      this.ignoreComma();
      return new IPathGeom.ArcTo(abs, rx, ry, xAxisRotation, largeArcFlag, sweepFlag, x, y);
   }

   boolean matchArcFlag() {
      double num = this.matchNum();
      if (num == 0.0) {
         return false;
      } else if (num == 1.0) {
         return true;
      } else {
         throw new ParseException("Invalid arc flag " + num);
      }
   }

   public BColor parseColor() {
      try {
         Token cur = this.cur;
         String str = cur.str;
         if (cur.type == 17) {
            if (str.length() == 3) {
               int r = TextUtil.hexCharToInt(str.charAt(0));
               int g = TextUtil.hexCharToInt(str.charAt(1));
               int b = TextUtil.hexCharToInt(str.charAt(2));
               this.consume();
               return BColor.make(r << 20 | r << 16 | g << 12 | g << 8 | b << 4 | b << 0, false);
            }

            if (str.length() == 6) {
               int rgb = Integer.parseInt(str, 16);
               this.consume();
               return BColor.make(rgb, false);
            }

            if (str.length() == 8) {
               int rgb = (int)(4294967295L & Long.parseLong(str, 16));
               this.consume();
               return BColor.make(rgb, true);
            }
         } else {
            if (cur.type == 15) {
               BColor c = BColor.getConstant(cur.str);
               if (c != null) {
                  this.consume();
               }

               return c;
            }

            if (cur.type == 18) {
               String func = cur.str;
               if (func.equals("rgb")) {
                  Token[] tok = this.parseFunctionParams();
                  if (tok.length != 3) {
                     throw new Exception();
                  }

                  int r = this.rgbnum(tok[0]);
                  int g = this.rgbnum(tok[1]);
                  int b = this.rgbnum(tok[2]);
                  return BColor.make(r << 16 | g << 8 | b, false);
               }

               if (func.equals("rgba")) {
                  Token[] tok = this.parseFunctionParams();
                  if (tok.length != 4) {
                     throw new Exception();
                  }

                  int r = this.rgbnum(tok[0]);
                  int g = this.rgbnum(tok[1]);
                  int b = this.rgbnum(tok[2]);
                  int a = this.alpha(tok[3]);
                  return BColor.make(a << 24 | r << 16 | g << 8 | b, true);
               }
            }
         }
      } catch (Exception var9) {
      }

      return null;
   }

   public BColor matchColor() {
      BColor color = this.parseColor();
      if (color == null) {
         throw new ParseException("Expecting color");
      } else {
         return color;
      }
   }

   public int rgbnum(Token token) {
      int x;
      switch (token.type) {
         case 19:
            x = (int)token.num;
            break;
         case 20:
            if (!"%".equals(token.str)) {
               throw new RuntimeException(token.toString());
            }

            x = (int)(255.0 * token.num / 100.0);
            break;
         default:
            throw new RuntimeException(token.toString());
      }

      if (x < 0) {
         x = 0;
      }

      if (x > 255) {
         x = 255;
      }

      return x;
   }

   public int alpha(Token token) {
      if (token.type != 19) {
         throw new RuntimeException();
      } else {
         double a = token.num;
         if (a < 0.0) {
            a = 0.0;
         }

         if (a > 1.0) {
            a = 1.0;
         }

         return (int)(255.0 * a);
      }
   }

   public BFont parseFont() {
      if (this.cur.id("null")) {
         this.eat();
         return BFont.NULL;
      } else {
         int style = 0;

         for (int i = 0; i < 3; i++) {
            if (this.cur.id("bold")) {
               this.consume();
               style |= 1;
            } else if (this.cur.id("italic")) {
               this.consume();
               style |= 2;
            } else if (this.cur.id("underline")) {
               this.consume();
               style |= 4;
            }
         }

         if (!this.cur.dimen("pt")) {
            throw this.error("Expecting {size}pt");
         } else {
            double size = this.cur.num;
            this.consume();
            String name = this.eat();
            return BFont.make(name, size, style);
         }
      }
   }

   public BBrush parseBrush() {
      BColor solid = this.parseColor();
      if (solid != null) {
         return BBrush.makeSolid(solid);
      } else if (this.cur.type == 18) {
         String func = this.cur.str;
         this.match(18);
         if (func.equals("lineargradient")) {
            return this.parseLinearGradient();
         } else if (func.equals("radialgradient")) {
            return this.parseRadialGradient();
         } else if (func.equals("image")) {
            return this.parseImage();
         } else if (func.equals("inverse")) {
            return this.parseInverse();
         } else {
            throw new ParseException("Unknown brush " + func);
         }
      } else {
         return null;
      }
   }

   BBrush parseInverse() {
      BColor c = this.matchColor();
      this.match(8);
      return BBrush.makeInverse(c);
   }

   BBrush parseImage() {
      BImage image = null;
      int tile = 0;
      int halign = 0;
      int valign = 0;

      while (this.cur.type != 8) {
         if (this.cur.function("source")) {
            this.match(18);
            if (this.cur.type == 16) {
               image = BImage.make(this.cur.str);
               this.consume();
            } else {
               image = BImage.make(this.eatToRParen());
            }

            this.match(8);
         } else if (this.cur.function("tile")) {
            tile = this.parseImageTile();
         } else if (this.cur.function("halign")) {
            halign = this.parseImageHalign();
         } else if (this.cur.function("valign")) {
            valign = this.parseImageValign();
         } else {
            this.skipBrushArg();
         }
      }

      this.consume();
      return BBrush.makeImage(image, tile, halign, valign);
   }

   int parseImageTile() {
      int tile = 0;
      this.match(18);
      String id = this.matchId();
      if (id.equals("true")) {
         tile = 1;
      } else if (id.equals("x")) {
         tile = 2;
      } else if (id.equals("y")) {
         tile = 3;
      }

      this.match(8);
      return tile;
   }

   int parseImageHalign() {
      int align = 0;
      this.match(18);
      String id = this.matchId();
      if (id.equals("left")) {
         align = 3;
      } else if (id.equals("right")) {
         align = 4;
      }

      this.match(8);
      return align;
   }

   int parseImageValign() {
      int align = 0;
      this.match(18);
      String id = this.matchId();
      if (id.equals("top")) {
         align = 1;
      } else if (id.equals("bottom")) {
         align = 2;
      }

      this.match(8);
      return align;
   }

   BBrush parseLinearGradient() {
      Array<BBrush.Stop> stops = new Array(BBrush.Stop.class);
      int spread = 1;
      double angle = 0.0;

      while (this.cur.type != 8) {
         if (this.cur.function("stop")) {
            stops.add(this.parseGradientStop());
         } else if (this.cur.function("angle")) {
            angle = this.parseGradientAngle();
         } else if (this.cur.id("pad")) {
            spread = 1;
            this.consume();
         } else if (this.cur.id("reflect")) {
            spread = 2;
            this.consume();
         } else if (this.cur.id("repeat")) {
            spread = 3;
            this.consume();
         } else {
            this.skipBrushArg();
         }
      }

      this.consume();
      return BBrush.makeLinearGradient((BBrush.Stop[])stops.trim(), spread, angle);
   }

   BBrush parseRadialGradient() {
      Array<BBrush.Stop> stops = new Array(BBrush.Stop.class);
      int spread = 1;
      BPoint c = BPoint.make(50.0, 50.0);
      double r = 50.0;
      BPoint f = null;

      while (this.cur.type != 8) {
         if (this.cur.function("stop")) {
            stops.add(this.parseGradientStop());
         } else if (this.cur.function("c")) {
            c = this.parseGradientPoint();
         } else if (this.cur.function("r")) {
            r = this.parseGradientOffset();
         } else if (this.cur.function("f")) {
            f = this.parseGradientPoint();
         } else if (this.cur.id("pad")) {
            spread = 1;
            this.consume();
         } else if (this.cur.id("reflect")) {
            spread = 2;
            this.consume();
         } else if (this.cur.id("repeat")) {
            spread = 3;
            this.consume();
         } else {
            this.skipBrushArg();
         }
      }

      this.consume();
      if (f == null) {
         f = c;
      }

      return BBrush.makeRadialGradient((BBrush.Stop[])stops.trim(), spread, c, r, f);
   }

   BBrush.Stop parseGradientStop() {
      this.match(18);
      double offset = this.matchDimen("%");
      this.ignoreComma();
      BColor color = this.matchColor();
      this.match(8);
      return BBrush.stop(offset, color);
   }

   double parseGradientAngle() {
      this.match(18);
      double angle = this.matchNum();
      this.match(8);
      return angle;
   }

   double parseGradientOffset() {
      this.match(18);
      double angle = this.matchDimen("%");
      this.match(8);
      return angle;
   }

   BPoint parseGradientPoint() {
      this.match(18);
      double x = this.matchDimen("%");
      this.ignoreComma();
      double y = this.matchDimen("%");
      this.match(8);
      return BPoint.make(x, y);
   }

   void skipBrushArg() {
      if (this.cur.type == 15) {
         this.consume();
      } else {
         if (this.cur.type == 18) {
            while (true) {
               this.consume();
               if (this.cur.type == 22) {
                  break;
               }

               if (this.cur.type == 8) {
                  this.consume();
                  return;
               }
            }
         }

         throw new ParseException("Unexpected " + this.cur);
      }
   }

   public BPen parsePen() {
      double width = 1.0;
      int cap = 101;
      int join = 201;
      double[] dash = null;

      while (!this.isEnd()) {
         if (this.cur.type == 19) {
            width = this.cur.num;
            this.consume();
         } else if (this.cur.type == 15) {
            String id = this.cur.str;
            if (id.equals("capbutt")) {
               cap = 101;
            } else if (id.equals("capsquare")) {
               cap = 102;
            } else if (id.equals("capround")) {
               cap = 103;
            } else if (id.equals("joinmiter")) {
               join = 201;
            } else if (id.equals("joinround")) {
               join = 202;
            } else if (id.equals("joinbevel")) {
               join = 203;
            }

            this.consume();
         } else if (this.cur.type == 18) {
            String func = this.cur.str;
            this.match(18);
            if (!func.equals("dash")) {
               throw new ParseException("Unexpected function " + func);
            }

            dash = this.parseDash();
         } else {
            if (this.cur.type != 2) {
               throw new ParseException("Unexpected " + this.cur);
            }

            this.consume();
         }
      }

      return BPen.make(width, cap, join, dash);
   }

   double[] parseDash() {
      double[] dash = new double[100];
      int n = 0;

      while (this.cur.type != 8) {
         dash[n++] = this.matchNum();
         this.ignoreComma();
      }

      this.consume();
      double[] trim = new double[n];
      System.arraycopy(dash, 0, trim, 0, n);
      return trim;
   }

   public BTransform parseTransform() {
      Array<BTransform.Transform> array = new Array(BTransform.Transform.class);

      while (!this.isEnd()) {
         this.verify(18);
         String func = this.cur.str;
         this.consume();
         if (func.equals("translate")) {
            array.add(this.parseTranslate());
         } else if (func.equals("scale")) {
            array.add(this.parseScale());
         } else if (func.equals("rotate")) {
            array.add(this.parseRotate());
         } else if (func.equals("skewx")) {
            array.add(this.parseSkewX());
         } else {
            if (!func.equals("skewy")) {
               throw new ParseException("Unknown transform " + func);
            }

            array.add(this.parseSkewY());
         }
      }

      return BTransform.make((BTransform.Transform[])array.trim());
   }

   BTransform.Translate parseTranslate() {
      double x = this.matchNum();
      double y = 0.0;
      this.ignoreComma();
      if (this.cur.type == 19) {
         y = this.matchNum();
      }

      this.match(8);
      return new BTransform.Translate(x, y);
   }

   BTransform.Scale parseScale() {
      double x = this.matchNum();
      double y = 0.0;
      this.ignoreComma();
      if (this.cur.type == 19) {
         y = this.matchNum();
      }

      this.match(8);
      return new BTransform.Scale(x, y);
   }

   BTransform.Rotate parseRotate() {
      double angle = this.matchNum();
      this.match(8);
      return new BTransform.Rotate(angle);
   }

   BTransform.SkewX parseSkewX() {
      double angle = this.matchNum();
      this.match(8);
      return new BTransform.SkewX(angle);
   }

   BTransform.SkewY parseSkewY() {
      double angle = this.matchNum();
      this.match(8);
      return new BTransform.SkewY(angle);
   }

   public BInsets parseInsets() {
      if (this.cur.type == 15 && this.cur.str.equals("null")) {
         this.consume();
         return BInsets.NULL;
      } else {
         double a = this.matchNum();
         if (this.cur.type == 19) {
            double b = this.matchNum();
            if (this.cur.type == 19) {
               double c = this.matchNum();
               if (this.cur.type == 19) {
                  double d = this.matchNum();
                  return BInsets.make(a, b, c, d);
               } else {
                  return BInsets.make(a, b, c, b);
               }
            } else {
               return BInsets.make(a, b, a, b);
            }
         } else {
            return BInsets.make(a, a, a, a);
         }
      }
   }

   public Token[] parseFunctionParams() {
      this.match(18);
      if (this.cur.type == 7) {
         this.consume();
         return new Token[0];
      } else {
         Array<Token> params = new Array(Token.class);

         while (this.cur.type != 22) {
            params.add(this.cur.copy());
            this.consume();
            if (this.cur.type == 8) {
               this.consume();
               return (Token[])params.trim();
            }

            if (this.cur.type != 2) {
               throw this.error("Expected , not " + this.cur);
            }

            this.consume();
         }

         throw this.error("Unmatched )");
      }
   }

   public boolean isEnd() {
      return this.cur.type == 22;
   }

   public String eat() {
      String eat = this.tokenizer.eat(this.cur.pos);
      this.cur.set(22);
      this.peek.set(22);
      return eat;
   }

   public String eatToRParen() {
      String s = this.tokenizer.eatToRParen(this.cur.pos);
      this.initConsume();
      return s;
   }

   void initConsume() {
      this.tokenizer.next(this.cur);
      this.tokenizer.next(this.peek);
   }

   public void consume() {
      this.cur.copy(this.peek);
      this.tokenizer.next(this.peek);
   }

   public void match(int type) {
      this.verify(type);
      this.consume();
   }

   public String matchId() {
      String str = this.cur.str;
      this.match(15);
      return str;
   }

   public double matchNum() {
      double num = this.cur.num;
      this.match(19);
      return num;
   }

   public double matchDimen(String str) {
      this.verify(20);
      if (!this.cur.str.equals(str)) {
         throw new ParseException("Expecting " + str + " dimension");
      } else {
         double num = this.cur.num;
         this.consume();
         return num;
      }
   }

   public void ignoreComma() {
      if (this.cur.type == 2) {
         this.consume();
      }
   }

   public void verify(int type) {
      if (this.cur.type != type) {
         throw this.error("Expecting \"" + Token.TYPES[type] + "\"");
      }
   }

   public ParseException error(String msg) {
      return new ParseException(msg);
   }
}
