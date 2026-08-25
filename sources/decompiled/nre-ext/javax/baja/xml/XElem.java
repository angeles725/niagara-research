package javax.baja.xml;

import com.tridium.nre.util.IElement;
import java.io.File;
import java.io.IOException;

public class XElem extends XContent implements IElement {
   static Object[] noAttr = new Object[0];
   static XContent[] noContent = new XContent[0];
   static XElem[] noElem = new XElem[0];
   XNs ns;
   String name;
   Object[] attr = noAttr;
   int attrSize;
   XContent[] content = noContent;
   int contentSize;
   int line;

   public XElem(String nsPrefix, String nsUri, String name) {
      this.ns = this.defineNs(nsPrefix, nsUri);
      this.name = name;
   }

   public XElem(XNs ns, String name) {
      this.ns = ns;
      this.name = name;
   }

   public XElem(String name) {
      this(null, name);
   }

   public XElem() {
      this(null, "unnamed");
   }

   public final XNs ns() {
      return this.ns;
   }

   public final String prefix() {
      return this.ns == null ? null : this.ns.prefix;
   }

   public final String uri() {
      return this.ns == null ? null : this.ns.uri;
   }

   @Override
   public final String name() {
      return this.name;
   }

   public final String qname() {
      return this.ns != null && !this.ns.prefix.equals("") ? this.ns.prefix + ':' + this.name : this.name;
   }

   public final XNs findNs(String uri) {
      if (this.ns != null && this.ns.uri.equals(uri)) {
         return this.ns;
      }

      int attrSize = this.attrSize();

      for (int i = 0; i < attrSize; i++) {
         if (uri.equals(this.attrValue(i))) {
            String name = this.attrName(i);
            String prefix;
            if (name.equals("xmlns")) {
               prefix = "";
            } else {
               if (!name.startsWith("xmlns:")) {
                  throw new XException("Invalid xmlns: " + name, this);
               }

               prefix = name.substring(6);
            }

            return new XNs(prefix, uri);
         }
      }

      return null;
   }

   public final XNs setNs(XNs ns) {
      this.ns = ns;
      return ns;
   }

   public final void setName(String name) {
      if (name == null) {
         throw new NullPointerException();
      }

      this.name = name;
   }

   public XNs defineDefaultNs(String uri) {
      return this.defineNs("", uri);
   }

   public XNs defineNs(String prefix, String uri) {
      return this.defineNs(new XNs(prefix, uri));
   }

   public XNs defineNs(XNs ns) {
      String name = ns.prefix.equals("") ? "xmlns" : "xmlns:" + ns.prefix;
      this.setAttr(name, ns.uri);
      return ns;
   }

   @Override
   public final int attrSize() {
      return this.attrSize;
   }

   public final XNs attrNs(int index) {
      if (index >= this.attrSize) {
         throw new ArrayIndexOutOfBoundsException(index);
      } else {
         return (XNs)this.attr[index * 3 + 1];
      }
   }

   @Override
   public final String attrName(int index) {
      if (index >= this.attrSize) {
         throw new ArrayIndexOutOfBoundsException(index);
      } else {
         return (String)this.attr[index * 3];
      }
   }

   @Override
   public final String attrValue(int index) {
      if (index >= this.attrSize) {
         throw new ArrayIndexOutOfBoundsException(index);
      } else {
         return (String)this.attr[index * 3 + 2];
      }
   }

   public final int attrIndex(XNs ns, String name) {
      int len = this.attrSize * 3;

      for (int i = 0; i < len; i += 3) {
         if (this.attr[i].equals(name) && XNs.equals(ns, this.attr[i + 1])) {
            return i / 3;
         }
      }

      return -1;
   }

   public final int attrIndex(String name) {
      int len = this.attrSize * 3;

      for (int i = 0; i < len; i += 3) {
         if (this.attr[i].equals(name) && this.attr[i + 1] == null) {
            return i / 3;
         }
      }

      return -1;
   }

   public final String get(XNs ns, String name) {
      int len = this.attrSize * 3;

      for (int i = 0; i < len; i += 3) {
         if (this.attr[i].equals(name) && XNs.equals(ns, this.attr[i + 1])) {
            return (String)this.attr[i + 2];
         }
      }

      throw new XException("Missing attr '" + name + "'", this);
   }

   @Override
   public final String get(String name) {
      int len = this.attrSize * 3;

      for (int i = 0; i < len; i += 3) {
         if (this.attr[i].equals(name) && this.attr[i + 1] == null) {
            return (String)this.attr[i + 2];
         }
      }

      throw new XException("Missing attr '" + name + "'", this);
   }

   public final String get(XNs ns, String name, String def) {
      int len = this.attrSize * 3;

      for (int i = 0; i < len; i += 3) {
         if (this.attr[i].equals(name) && XNs.equals(ns, this.attr[i + 1])) {
            return (String)this.attr[i + 2];
         }
      }

      return def;
   }

   @Override
   public final String get(String name, String def) {
      int len = this.attrSize * 3;

      for (int i = 0; i < len; i += 3) {
         if (this.attr[i].equals(name) && this.attr[i + 1] == null) {
            return (String)this.attr[i + 2];
         }
      }

      return def;
   }

   public final boolean getb(String name) {
      String v = this.get(name);
      if (v.equals("true")) {
         return true;
      } else if (v.equals("false")) {
         return false;
      } else {
         throw new XException("Invalid boolean attr '" + name + "'='" + v + "'", this);
      }
   }

   public final boolean getb(String name, boolean def) {
      String v = this.get(name, null);
      if (v == null) {
         return def;
      } else if (v.equals("true")) {
         return true;
      } else if (v.equals("false")) {
         return false;
      } else {
         throw new XException("Invalid boolean attr '" + name + "'='" + v + "'", this);
      }
   }

   @Override
   public final int geti(String name) {
      String v = this.get(name);

      try {
         return Integer.parseInt(v);
      } catch (Exception e) {
         throw new XException("Invalid int attr '" + name + "'='" + v + "'", this);
      }
   }

   @Override
   public final int geti(String name, int def) {
      String v = this.get(name, null);
      if (v == null) {
         return def;
      }

      try {
         return Integer.parseInt(v);
      } catch (Exception e) {
         throw new XException("Invalid int attr '" + name + "'='" + v + "'", this);
      }
   }

   @Override
   public final long getl(String name) {
      String v = this.get(name);

      try {
         return Long.parseLong(v);
      } catch (Exception e) {
         throw new XException("Invalid long attr '" + name + "'='" + v + "'", this);
      }
   }

   @Override
   public final long getl(String name, long def) {
      String v = this.get(name, null);
      if (v == null) {
         return def;
      }

      try {
         return Long.parseLong(v);
      } catch (Exception e) {
         throw new XException("Invalid long attr '" + name + "'='" + v + "'", this);
      }
   }

   @Override
   public final float getf(String name) {
      String v = this.get(name);

      try {
         return Float.parseFloat(v);
      } catch (Exception e) {
         throw new XException("Invalid float attr '" + name + "'='" + v + "'", this);
      }
   }

   @Override
   public final float getf(String name, float def) {
      String v = this.get(name, null);
      if (v == null) {
         return def;
      }

      try {
         return Float.parseFloat(v);
      } catch (Exception e) {
         throw new XException("Invalid float attr '" + name + "'='" + v + "'", this);
      }
   }

   @Override
   public final double getd(String name) {
      String v = this.get(name);

      try {
         return Double.parseDouble(v);
      } catch (Exception e) {
         throw new XException("Invalid double attr '" + name + "'='" + v + "'", this);
      }
   }

   @Override
   public final double getd(String name, double def) {
      String v = this.get(name, null);
      if (v == null) {
         return def;
      }

      try {
         return Double.parseDouble(v);
      } catch (Exception e) {
         throw new XException("Invalid double attr '" + name + "'='" + v + "'", this);
      }
   }

   public final void setAttr(XNs ns, String name, String value) {
      if (name != null && value != null) {
         int index = this.attrIndex(ns, name);
         if (index != -1) {
            this.attr[index * 3 + 2] = value;
         } else {
            this.addAttr(ns, name, value);
         }
      } else {
         throw new NullPointerException();
      }
   }

   public final void setAttr(String name, String value) {
      if (name != null && value != null) {
         int index = this.attrIndex(name);
         if (index != -1) {
            this.attr[index * 3 + 2] = value;
         } else {
            this.addAttr(name, value);
         }
      } else {
         throw new NullPointerException();
      }
   }

   public final void setAttr(int index, String value) {
      if (value == null) {
         throw new NullPointerException();
      }

      if (index >= this.attrSize) {
         throw new ArrayIndexOutOfBoundsException(index);
      }

      this.attr[index * 3 + 2] = value;
   }

   public final XElem addAttr(String name, String value) {
      return this.addAttrImpl(null, name, value);
   }

   public final XElem addAttr(XNs ns, String name, String value) {
      return this.addAttrImpl(ns, name, value);
   }

   final XElem addAttrImpl(Object ns, String name, String value) {
      if (name != null && value != null) {
         if (this.attrSize >= this.attr.length / 3) {
            int resize = this.attr.length * 6;
            if (resize == 0) {
               resize = 12;
            }

            Object[] temp = new Object[resize];
            System.arraycopy(this.attr, 0, temp, 0, this.attr.length);
            this.attr = temp;
         }

         int offset = this.attrSize * 3;
         this.attr[offset] = name;
         this.attr[offset + 1] = ns;
         this.attr[offset + 2] = value;
         this.attrSize++;
         return this;
      } else {
         throw new NullPointerException();
      }
   }

   public final void removeAttr(XNs ns, String name) {
      int index = this.attrIndex(ns, name);
      if (index != -1) {
         this.removeAttr(index);
      }
   }

   public final void removeAttr(String name) {
      int index = this.attrIndex(name);
      if (index != -1) {
         this.removeAttr(index);
      }
   }

   public final void removeAttr(int index) {
      if (index >= this.attrSize) {
         throw new ArrayIndexOutOfBoundsException(index);
      }

      int shift = this.attrSize - index - 1;
      if (shift > 0) {
         System.arraycopy(this.attr, (index + 1) * 3, this.attr, index * 3, shift * 3);
      }

      this.attrSize--;
   }

   public final void clearAttr() {
      this.attrSize = 0;
   }

   public final int contentSize() {
      return this.contentSize;
   }

   public final XContent content(int index) {
      if (index >= this.contentSize) {
         throw new ArrayIndexOutOfBoundsException(index);
      } else {
         return this.content[index];
      }
   }

   public final XContent[] content() {
      XContent[] r = new XContent[this.contentSize];
      System.arraycopy(this.content, 0, r, 0, r.length);
      return r;
   }

   public int contentIndex(XContent child) {
      int len = this.contentSize;
      XContent[] content = this.content;

      for (int i = 0; i < len; i++) {
         if (content[i] == child) {
            return i;
         }
      }

      return -1;
   }

   public final XElem elem(int index) {
      if (index >= this.contentSize) {
         throw new ArrayIndexOutOfBoundsException(index);
      } else {
         return (XElem)this.content[index];
      }
   }

   public final XElem[] elems() {
      int len = this.contentSize;
      if (len == 0) {
         return noElem;
      }

      XContent[] content = this.content;
      int n = 0;
      XElem[] temp = new XElem[len];

      for (int i = 0; i < len; i++) {
         if (content[i] instanceof XElem) {
            temp[n++] = (XElem)content[i];
         }
      }

      if (n == temp.length) {
         return temp;
      }

      XElem[] r = new XElem[n];
      System.arraycopy(temp, 0, r, 0, n);
      return r;
   }

   public final XElem[] elems(XNs ns, String name) {
      int len = this.contentSize;
      if (len == 0) {
         return noElem;
      }

      XContent[] content = this.content;
      int n = 0;
      XElem[] temp = new XElem[len];

      for (int i = 0; i < len; i++) {
         if (content[i] instanceof XElem) {
            XElem kid = (XElem)content[i];
            if (XNs.equals(ns, kid.ns) && kid.name.equals(name)) {
               temp[n++] = kid;
            }
         }
      }

      if (n == temp.length) {
         return temp;
      }

      XElem[] r = new XElem[n];
      System.arraycopy(temp, 0, r, 0, n);
      return r;
   }

   public final XElem[] elems(XNs ns) {
      int len = this.contentSize;
      if (len == 0) {
         return noElem;
      }

      XContent[] content = this.content;
      int n = 0;
      XElem[] temp = new XElem[len];

      for (int i = 0; i < len; i++) {
         if (content[i] instanceof XElem) {
            XElem kid = (XElem)content[i];
            if (XNs.equals(ns, kid.ns)) {
               temp[n++] = kid;
            }
         }
      }

      if (n == temp.length) {
         return temp;
      }

      XElem[] r = new XElem[n];
      System.arraycopy(temp, 0, r, 0, n);
      return r;
   }

   public final XElem[] elems(String name) {
      int len = this.contentSize;
      if (len == 0) {
         return noElem;
      }

      XContent[] content = this.content;
      int n = 0;
      XElem[] temp = new XElem[len];

      for (int i = 0; i < len; i++) {
         if (content[i] instanceof XElem) {
            XElem kid = (XElem)content[i];
            if (kid.name.equals(name)) {
               temp[n++] = kid;
            }
         }
      }

      if (n == temp.length) {
         return temp;
      }

      XElem[] r = new XElem[n];
      System.arraycopy(temp, 0, r, 0, n);
      return r;
   }

   public final XElem elem(XNs ns, String name) {
      int len = this.contentSize;
      XContent[] content = this.content;

      for (int i = 0; i < len; i++) {
         if (content[i] instanceof XElem) {
            XElem kid = (XElem)content[i];
            if (XNs.equals(ns, kid.ns) && kid.name.equals(name)) {
               return kid;
            }
         }
      }

      return null;
   }

   public final XElem elem(String name) {
      int len = this.contentSize;
      XContent[] content = this.content;

      for (int i = 0; i < len; i++) {
         if (content[i] instanceof XElem) {
            XElem kid = (XElem)content[i];
            if (kid.name.equals(name)) {
               return kid;
            }
         }
      }

      return null;
   }

   public final XElem addText(String txt) {
      return this.addContent(new XText(txt));
   }

   public XText text() {
      return this.contentSize > 0 && this.content[0] instanceof XText ? (XText)this.content[0] : null;
   }

   public final XText text(int index) {
      if (index >= this.contentSize) {
         throw new ArrayIndexOutOfBoundsException(index);
      } else {
         return (XText)this.content[index];
      }
   }

   public String string() {
      return this.contentSize > 0 && this.content[0] instanceof XText ? ((XText)this.content[0]).string() : null;
   }

   public final XElem addContent(XContent child) {
      return this.addContent(this.contentSize, child);
   }

   public final XElem addContent(int index, XContent child) {
      if (child.parent != null) {
         throw new IllegalArgumentException("Content already parented");
      }

      if (index > this.contentSize) {
         throw new ArrayIndexOutOfBoundsException(index);
      }

      if (this.contentSize >= this.content.length) {
         int resize = this.content.length * 2;
         if (resize == 0) {
            resize = 4;
         }

         XContent[] temp = new XContent[resize];
         System.arraycopy(this.content, 0, temp, 0, this.content.length);
         this.content = temp;
      }

      int shift = this.contentSize - index;
      if (shift > 0) {
         System.arraycopy(this.content, index, this.content, index + 1, shift);
      }

      this.content[index] = child;
      this.contentSize++;
      child.parent = this;
      return this;
   }

   public final void replaceContent(int index, XContent child) {
      if (index >= this.contentSize) {
         throw new ArrayIndexOutOfBoundsException(index);
      }

      this.content[index].parent = null;
      this.content[index] = child;
      child.parent = this;
   }

   public final void removeContent(XContent child) {
      int index = this.contentIndex(child);
      if (index != -1) {
         this.removeContent(index);
      }
   }

   public final void removeContent(int index) {
      if (index >= this.contentSize) {
         throw new ArrayIndexOutOfBoundsException(index);
      }

      this.content[index].parent = null;
      int shift = this.contentSize - index - 1;
      if (shift > 0) {
         System.arraycopy(this.content, index + 1, this.content, index, shift);
      }

      this.contentSize--;
   }

   public final void clearContent() {
      int len = this.contentSize;

      for (int i = 0; i < len; i++) {
         this.content[i].parent = null;
      }

      this.contentSize = 0;
   }

   public void dump() {
      try {
         XWriter out = new XWriter(System.out);
         this.write(out, 0);
         out.flush();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public void write(File file) throws Exception {
      XWriter out = new XWriter(file);
      this.write(out);
      out.close();
   }

   @Override
   public void write(XWriter out) {
      this.write(out, 0);
   }

   public void write(XWriter out, int indent) {
      out.indent(indent);
      out.w('<');
      if (this.ns != null && !this.ns.prefix.equals("")) {
         out.w(this.ns.prefix).w(':');
      }

      out.w(this.name);
      int attrSize = this.attrSize;
      if (attrSize > 0) {
         Object[] attr = this.attr;

         for (int i = 0; i < attrSize; i++) {
            String attrName = (String)attr[i * 3];
            XNs attrNs = (XNs)attr[i * 3 + 1];
            String attrVal = (String)attr[i * 3 + 2];
            out.w(' ');
            if (attrNs != null) {
               out.w(attrNs.prefix).w(':');
            }

            out.w(attrName).w('=').w('"').safe(attrVal).w('"');
         }
      }

      int contentSize = this.contentSize;
      if (contentSize == 0) {
         out.w('/').w('>').nl();
      } else {
         XContent[] content = this.content;
         if (contentSize == 1 && content[0] instanceof XText) {
            out.w('>');
            content[0].write(out);
         } else {
            out.w('>').nl();

            for (int i = 0; i < contentSize; i++) {
               XContent c = content[i];
               if (c instanceof XElem) {
                  ((XElem)c).write(out, indent + 1);
               } else {
                  c.write(out);
               }
            }

            out.indent(indent);
         }

         out.w('<').w('/');
         if (this.ns != null && !this.ns.prefix.equals("")) {
            out.w(this.ns.prefix).w(':');
         }

         out.w(this.name);
         out.w('>').nl();
      }
   }

   public void write(XWriter out, int indent, boolean close) {
      out.indent(indent);
      out.w('<');
      if (this.ns != null && !this.ns.prefix.equals("")) {
         out.w(this.ns.prefix).w(':');
      }

      out.w(this.name);
      int attrSize = this.attrSize;
      if (attrSize > 0) {
         Object[] attr = this.attr;

         for (int i = 0; i < attrSize; i++) {
            String attrName = (String)attr[i * 3];
            XNs attrNs = (XNs)attr[i * 3 + 1];
            String attrVal = (String)attr[i * 3 + 2];
            out.w(' ');
            if (attrNs != null) {
               out.w(attrNs.prefix).w(':');
            }

            out.w(attrName).w('=').w('"').safe(attrVal).w('"');
         }
      }

      if (close) {
         out.w('/').w('>');
      } else {
         out.w('>');
      }
   }

   public final int line() {
      return this.line;
   }

   public final XElem copy() {
      XElem copy = new XElem();
      copy.ns = this.ns;
      copy.name = this.name;
      copy.line = this.line;
      if (this.attrSize > 0) {
         copy.attrSize = this.attrSize;
         copy.attr = new Object[this.attrSize * 3];
         System.arraycopy(this.attr, 0, copy.attr, 0, copy.attr.length);
      }

      if (this.contentSize > 0) {
         copy.contentSize = this.contentSize;
         copy.content = new XContent[this.contentSize];
         System.arraycopy(this.content, 0, copy.content, 0, copy.content.length);
      }

      return copy;
   }

   public final XElem deepcopy() {
      XElem copy = new XElem();
      copy.ns = this.ns;
      copy.name = this.name;
      copy.line = this.line;
      if (this.attrSize > 0) {
         copy.attrSize = this.attrSize;
         copy.attr = new Object[this.attrSize * 3];
         System.arraycopy(this.attr, 0, copy.attr, 0, copy.attr.length);
      }

      if (this.contentSize > 0) {
         copy.contentSize = this.contentSize;
         copy.content = new XContent[this.contentSize];

         for (int i = 0; i < this.contentSize; i++) {
            if (this.content[i] instanceof XElem) {
               copy.content[i] = ((XElem)this.content[i]).deepcopy();
            } else if (this.content[i] instanceof XText) {
               copy.content[i] = ((XText)this.content[i]).copy();
            } else {
               copy.content[i] = this.content[i];
            }
         }
      }

      return copy;
   }

   @Override
   public String toString() {
      StringBuilder s = new StringBuilder();
      s.append('<');
      if (this.ns != null && !this.ns.prefix.equals("")) {
         s.append(this.ns.prefix).append(':');
      }

      s.append(this.name);

      for (int i = 0; i < this.attrSize; i++) {
         String attrName = (String)this.attr[i * 3];
         XNs attrNs = (XNs)this.attr[i * 3 + 1];
         String attrVal = (String)this.attr[i * 3 + 2];
         s.append(' ');
         if (attrNs != null) {
            s.append(attrNs.prefix).append(':');
         }

         s.append(attrName).append('=').append('\'').append(attrVal).append('\'');
      }

      s.append('>');
      return s.toString();
   }
}
