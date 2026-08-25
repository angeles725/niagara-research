package javax.baja.xml;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class XParser {
   public static final int EOF = -1;
   public static final int ELEM_START = 1;
   public static final int ELEM_END = 2;
   public static final int TEXT = 3;
   private static final byte[] charMap = new byte[128];
   private static final int CT_SPACE = 1;
   private static final int CT_NAME = 2;
   private static final String[] internCache;
   private XInputStreamReader in;
   private int pushback = -1;
   private int line = 1;
   private int col;
   private int type;
   private XText text = new XText();
   private int depth;
   private XElem[] stack = new XElem[256];
   private XNs[][] nsStack = new XNs[256][];
   private XNs defaultNs;
   private XText buf = new XText();
   private XText entityBuf = new XText();
   private String name;
   private String prefix;
   private boolean popStack;
   private boolean emptyElem;
   private final boolean preserveWhitespace;

   public static XParser make(File file) throws Exception {
      return make(new BufferedInputStream(new FileInputStream(file)));
   }

   public static XParser make(String xml) throws Exception {
      return make(new ByteArrayInputStream(xml.getBytes()));
   }

   public static XParser make(InputStream in) throws Exception {
      return new XParser(in);
   }

   public static XParser make(File file, boolean preserveWhitespace) throws Exception {
      return make(new BufferedInputStream(new FileInputStream(file)), preserveWhitespace);
   }

   public static XParser make(String xml, boolean preserveWhitespace) throws Exception {
      return make(new ByteArrayInputStream(xml.getBytes()), preserveWhitespace);
   }

   public static XParser make(InputStream in, boolean preserveWhitespace) throws Exception {
      return new XParser(in, preserveWhitespace);
   }

   protected XParser(InputStream in) throws IOException {
      this.in = new XInputStreamReader(in);
      this.preserveWhitespace = false;
   }

   protected XParser(InputStream in, boolean preserveWhitespace) throws IOException {
      this.in = new XInputStreamReader(in);
      this.preserveWhitespace = preserveWhitespace;
   }

   public String getEncoding() throws IOException {
      return this.in.getEncoding();
   }

   public boolean isZipped() throws IOException {
      return this.in.isZipped();
   }

   public final XElem parse() throws Exception {
      return this.parse(true);
   }

   public final XElem parse(boolean close) throws Exception {
      if (this.next() != 1) {
         if (close) {
            this.close();
         }

         throw this.error("Expecting element start");
      } else {
         return this.parseCurrent(close);
      }
   }

   public final XElem parseCurrent() throws Exception {
      return this.parseCurrent(false);
   }

   public XElem parseCurrent(boolean close) throws Exception {
      try {
         int depth = 1;
         XElem root = this.elem().copy();
         XElem cur = root;

         while (depth > 0) {
            int type = this.next();
            if (type == 1) {
               XElem oldCur = cur;
               cur = this.elem().copy();
               oldCur.addContent(cur);
               depth++;
            } else if (type == 2) {
               cur = cur.parent();
               depth--;
            } else if (type == 3) {
               cur.addContent(this.text().copy());
            } else if (type == -1) {
               throw new EOFException();
            }
         }

         return root;
      } finally {
         if (close) {
            this.close();
         }
      }
   }

   public final int next() throws Exception {
      if (this.popStack) {
         this.popStack = false;
         this.pop();
      }

      if (this.emptyElem) {
         this.emptyElem = false;
         this.popStack = true;
         return this.type = 2;
      }

      while (true) {
         int c;
         try {
            c = this.read();
         } catch (EOFException e) {
            return this.type = -1;
         }

         if (c != 60) {
            if (this.parseText(c) || this.preserveWhitespace) {
               return this.type = 3;
            }
         } else {
            c = this.read();
            if (c == 33) {
               c = this.read();
               if (c == 45) {
                  c = this.read();
                  if (c != 45) {
                     throw this.error("Expecting comment");
                  }

                  this.skipComment();
               } else {
                  if (c == 91) {
                     this.consume("CDATA[");
                     this.parseCDATA();
                     return this.type = 3;
                  }

                  if (c != 68) {
                     throw this.error("Unexpected markup");
                  }

                  this.consume("OCTYPE");
                  this.skipDocType();
               }
            } else {
               if (c != 63) {
                  if (c == 47) {
                     this.parseElemEnd();
                     this.popStack = true;
                     return this.type = 2;
                  }

                  this.parseElemStart(c);
                  return this.type = 1;
               }

               this.skipPI();
            }
         }
      }
   }

   public void skip() throws Exception {
      this.skip(this.depth);
   }

   public void skip(int toDepth) throws Exception {
      while (this.type != 2 || this.depth != toDepth) {
         int type = this.next();
         if (type == -1) {
            throw new EOFException();
         }
      }
   }

   public final int type() {
      return this.type;
   }

   public final int depth() {
      return this.depth;
   }

   public final XElem elem() {
      return this.depth < 1 ? null : this.stack[this.depth - 1];
   }

   public final XElem elem(int depth) {
      return depth >= 0 && depth < this.depth ? this.stack[depth] : null;
   }

   public final XText text() {
      return this.type == 3 ? this.text : null;
   }

   public final int line() {
      return this.line;
   }

   public final int column() {
      return this.col;
   }

   public final boolean emptyElem() {
      return this.emptyElem;
   }

   public final void close() {
      try {
         this.in.close();
      } catch (IOException var2) {
      }
   }

   private void parseElemStart(int c) throws Exception {
      XElem elem = this.push();
      this.parseQName(c);
      elem.name = this.name;
      elem.line = this.line;
      String prefix = this.prefix;
      boolean resolveAttrNs = false;

      while (true) {
         boolean sp = this.skipSpace();
         c = this.read();
         if (c == 62) {
            break;
         }

         if (c == 47) {
            c = this.read();
            if (c != 62) {
               throw this.error("Expecting /> empty element");
            }

            this.emptyElem = true;
            break;
         }

         if (!sp) {
            throw this.error("Expecting space before attribute");
         }

         resolveAttrNs |= this.parseAttr(c, elem);
      }

      if (prefix == null) {
         elem.ns = this.defaultNs;
      } else {
         elem.ns = this.prefixToNs(prefix);
      }

      if (resolveAttrNs) {
         for (int i = 0; i < elem.attrSize; i++) {
            if (elem.attr[i * 3 + 1] != null) {
               elem.attr[i * 3 + 1] = this.prefixToNs((String)elem.attr[i * 3 + 1]);
            }
         }
      }
   }

   private void parseElemEnd() throws Exception {
      this.parseQName(this.read());
      XNs ns = null;
      if (this.prefix == null) {
         ns = this.defaultNs;
      } else {
         ns = this.prefixToNs(this.prefix);
      }

      if (this.depth == 0) {
         throw this.error("Element end without start");
      }

      XElem elem = this.stack[this.depth - 1];
      if (elem.name.equals(this.name) && elem.ns == ns) {
         this.skipSpace();
         if (this.read() != 62) {
            throw this.error("Expecting > end of element");
         }
      } else {
         throw this.error("Expecting end of element '" + elem.qname() + "'[" + elem.line + "]");
      }
   }

   private boolean parseAttr(int c, XElem elem) throws Exception {
      this.parseQName(c);
      String prefix = this.prefix;
      String name = this.name;
      this.skipSpace();
      if (this.read() != 61) {
         throw this.error("Expecting '='");
      }

      this.skipSpace();
      c = this.read();
      if (c != 34 && c != 39) {
         throw this.error("Expecting quoted attribute value");
      }

      String value = this.parseString(c);
      if (prefix == null) {
         if (name.equals("xmlns")) {
            this.pushNs(elem, "", value);
         }
      } else if (prefix.equals("xmlns")) {
         this.pushNs(elem, name, value);
         prefix = null;
         name = "xmlns:" + name;
      } else if (prefix.equalsIgnoreCase("xml")) {
         prefix = null;
         name = "xml:" + name;
      }

      elem.addAttrImpl(prefix, name, value);
      return prefix != null;
   }

   private void parseQName(int c) throws Exception {
      this.prefix = null;
      this.name = this.parseName(c);
      c = this.read();
      if (c == 58) {
         this.prefix = this.name;
         this.name = this.parseName(this.read());
      } else {
         this.pushback = c;
      }
   }

   private String parseString(int quote) throws Exception {
      XText buf = this.buf;
      buf.setLength(0);

      int c;
      while ((c = this.read()) != quote) {
         buf.append(this.toCharData(c));
      }

      return this.bufToString();
   }

   private String parseName(int c) throws Exception {
      if (!isName(c)) {
         throw this.error("Expected XML name");
      }

      XText buf = this.buf;
      buf.setLength(0);
      buf.append(c);

      while (isName(c = this.read())) {
         buf.append(c);
      }

      this.pushback = c;
      return this.bufToString();
   }

   private void parseCDATA() throws Exception {
      XText text = this.text;
      text.length = 0;
      text.cdata = true;
      int c2 = -1;
      int c1 = -1;
      int c0 = -1;

      while (true) {
         c2 = c1;
         c1 = c0;
         c0 = this.read();
         if (c2 == 93 && c1 == 93 && c0 == 62) {
            text.setLength(text.length - 2);
            return;
         }

         text.append(c0);
      }
   }

   private boolean parseText(int c) throws Exception {
      XText text = this.text;
      text.length = 0;
      text.cdata = false;
      text.append(this.toCharData(c));
      boolean gotText = !isSpace(c);

      while (true) {
         try {
            c = this.read();
         } catch (EOFException e) {
            if (gotText) {
               throw e;
            }

            return false;
         }

         if (c == 60) {
            this.pushback = c;
            return gotText;
         }

         if (!isSpace(c)) {
            gotText = true;
         }

         text.append(this.toCharData(c));
      }
   }

   private boolean skipSpace() throws Exception {
      int c = this.read();
      if (!isSpace(c)) {
         this.pushback = c;
         return false;
      }

      while (isSpace(c = this.read())) {
      }

      this.pushback = c;
      return true;
   }

   private void skipComment() throws Exception {
      int c2 = -1;
      int c1 = -1;
      int c0 = -1;

      do {
         c2 = c1;
         c1 = c0;
         c0 = this.read();
      } while (c2 != 45 || c1 != 45);

      if (c0 != 62) {
         throw this.error("Cannot have -- in middle of comment");
      }
   }

   private void skipPI() throws Exception {
      int c1 = -1;
      int c0 = -1;

      do {
         c1 = c0;
         c0 = this.read();
      } while (c1 != 63 || c0 != 62);
   }

   private void skipDocType() throws Exception {
      int depth = 1;

      do {
         int c = this.read();
         if (c == 60) {
            depth++;
         }

         if (c == 62) {
            depth--;
         }
      } while (depth != 0);
   }

   private void consume(String s) throws Exception {
      int len = s.length();

      for (int i = 0; i < len; i++) {
         if (this.read() != s.charAt(i)) {
            throw this.error("Expected '" + s + "'");
         }
      }
   }

   private int read() throws Exception {
      int c = this.pushback;
      if (c != -1) {
         this.pushback = -1;
         return c;
      }

      c = this.in.read();
      if (c < 0) {
         throw new EOFException();
      }

      if (c == 10) {
         this.line++;
         this.col = 0;
         return 10;
      }

      if (c == 13) {
         int lookAhead = this.in.read();
         if (lookAhead != 10) {
            this.pushback = lookAhead;
         }

         this.line++;
         this.col = 0;
         return 10;
      } else {
         this.col++;
         return c;
      }
   }

   private int toCharData(int c) throws Exception {
      if (c == 60) {
         throw this.error("Invalid markup in char data");
      }

      if (c != 38) {
         return c;
      }

      c = this.read();
      if (c == 35) {
         c = this.in.read();
         this.col++;
         int x = 0;
         int base = 10;
         if (c == 120) {
            base = 16;
         } else {
            x = this.toNum(x, c, base);
         }

         c = this.in.read();
         this.col++;

         while (c != 59) {
            x = this.toNum(x, c, base);
            c = this.in.read();
            this.col++;
         }

         return (char)x;
      } else {
         XText ebuf = this.entityBuf;
         ebuf.setLength(0);
         ebuf.append(c);

         while ((c = this.read()) != 59) {
            ebuf.append(c);
         }

         String entity = ebuf.string().intern();
         if (entity == "lt") {
            return 60;
         } else if (entity == "gt") {
            return 62;
         } else if (entity == "amp") {
            return 38;
         } else if (entity == "quot") {
            return 34;
         } else if (entity == "apos") {
            return 39;
         } else {
            throw this.error("Unsupported entity &" + entity + ";");
         }
      }
   }

   private int toNum(int x, int c, int base) throws Exception {
      x *= base;
      if (48 <= c && c <= 57) {
         return x + (c - 48);
      }

      if (base == 16) {
         if (97 <= c && c <= 102) {
            return x + 10 + (c - 97);
         }

         if (65 <= c && c <= 70) {
            return x + 10 + (c - 65);
         }
      }

      throw this.error("Expected base " + base + " number");
   }

   private String bufToString() {
      if (this.buf.length == 1) {
         int ch = this.buf.data[0];
         if (32 <= ch && ch < 128) {
            return internCache[ch];
         }
      }

      return this.buf.string();
   }

   private XNs prefixToNs(String prefix) {
      for (int i = this.depth - 1; i >= 0; i--) {
         XNs[] ns = this.nsStack[i];
         if (ns != null) {
            for (int j = 0; j < ns.length; j++) {
               if (ns[j].prefix.equals(prefix)) {
                  return ns[j];
               }
            }
         }
      }

      throw new XException("Undeclared namespace prefix '" + prefix + "'");
   }

   private void pushNs(XElem elem, String prefix, String value) {
      XNs ns = new XNs(prefix, value);
      ns.declaringElem = elem;
      if (prefix == "") {
         if (value.equals("")) {
            this.defaultNs = null;
         } else {
            this.defaultNs = ns;
         }
      }

      XNs[] list = this.nsStack[this.depth - 1];
      if (list == null) {
         list = new XNs[]{ns};
      } else {
         XNs[] temp = new XNs[list.length + 1];
         System.arraycopy(list, 0, temp, 0, list.length);
         temp[list.length] = ns;
         list = temp;
      }

      this.nsStack[this.depth - 1] = list;
   }

   private void reEvalDefaultNs() {
      this.defaultNs = null;

      for (int i = this.depth - 1; i >= 0; i--) {
         XNs[] ns = this.nsStack[i];
         if (ns != null) {
            for (int j = 0; j < ns.length; j++) {
               if (ns[j].isDefault()) {
                  if (!ns[j].uri.equals("")) {
                     this.defaultNs = ns[j];
                  }

                  return;
               }
            }
         }
      }
   }

   private XElem push() {
      XElem elem = this.stack[this.depth];
      if (elem == null) {
         elem = this.stack[this.depth] = new XElem();
      }

      this.depth++;
      elem.clearAttr();
      return elem;
   }

   private void pop() {
      this.depth--;
      XNs[] ns = this.nsStack[this.depth];
      if (ns != null) {
         this.nsStack[this.depth] = null;
         this.reEvalDefaultNs();
      }
   }

   private XException error(String msg) {
      return new XException(msg, this.line, this.col);
   }

   static boolean isName(int c) {
      return c < 128 ? (charMap[c] & 2) != 0 : true;
   }

   static boolean isSpace(int c) {
      return c < 128 ? (charMap[c] & 1) != 0 : false;
   }

   static {
      for (int i = 97; i <= 122; i++) {
         charMap[i] = 2;
      }

      for (int i = 65; i <= 90; i++) {
         charMap[i] = 2;
      }

      for (int i = 48; i <= 57; i++) {
         charMap[i] = 2;
      }

      charMap[45] = 2;
      charMap[46] = 2;
      charMap[95] = 2;
      charMap[10] = 1;
      charMap[13] = 1;
      charMap[32] = 1;
      charMap[9] = 1;
      internCache = new String[128];

      for (int i = 32; i < 128; i++) {
         internCache[i] = new String(new char[]{(char)i}).intern();
      }
   }
}
