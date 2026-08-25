package javax.baja.xml;

public final class XText extends XContent {
   static char[] noData = new char[0];
   char[] data;
   int length;
   String string;
   boolean cdata;

   public XText(char[] data, int length) {
      this.data = data;
      this.length = length;
   }

   public XText(char[] data) {
      this.data = data;
      this.length = data.length;
   }

   public XText(String string) {
      this.string = string;
   }

   public XText() {
      this.data = noData;
   }

   public final boolean isCDATA() {
      return this.cdata;
   }

   public final void setCDATA(boolean cdata) {
      this.cdata = cdata;
   }

   public final String string() {
      if (this.string == null) {
         this.string = new String(this.data, 0, this.length);
      }

      return this.string;
   }

   public final int length() {
      return this.data != null ? this.length : this.string.length();
   }

   public final char[] data() {
      if (this.data == null) {
         this.data = this.string.toCharArray();
         this.length = this.data.length;
      }

      return this.data;
   }

   public final void append(int c) {
      if (this.length + 1 > this.data.length) {
         int resize = this.length * 2;
         if (resize < 256) {
            resize = 256;
         }

         char[] temp = new char[resize];
         System.arraycopy(this.data, 0, temp, 0, this.length);
         this.data = temp;
      }

      this.data[this.length++] = (char)c;
      this.string = null;
   }

   public final void append(String s) {
      int slen = s.length();
      if (this.length + slen > this.data.length) {
         int resize = Math.max(this.length * 2, this.length + slen);
         if (resize < 256) {
            resize = 256;
         }

         char[] temp = new char[resize];
         System.arraycopy(this.data, 0, temp, 0, this.length);
         this.data = temp;
      }

      int off = this.length;

      for (int i = 0; i < slen; i++) {
         this.data[off + i] = s.charAt(i);
      }

      this.length += slen;
      this.string = null;
   }

   public final void append(char[] buf, int off, int len) {
      if (this.length + len > this.data.length) {
         int resize = Math.max(this.length * 2, this.length + len);
         if (resize < 256) {
            resize = 256;
         }

         char[] temp = new char[resize];
         System.arraycopy(this.data, 0, temp, 0, this.length);
         this.data = temp;
      }

      int myoff = this.length;

      for (int i = 0; i < len; i++) {
         this.data[myoff + i] = buf[off + i];
      }

      this.length += len;
      this.string = null;
   }

   public final void set(int index, int c) {
      if (index >= this.length) {
         throw new ArrayIndexOutOfBoundsException();
      }

      this.data[index] = (char)c;
      this.string = null;
   }

   public final void setLength(int length) {
      this.length = length;
      if (length > this.data.length) {
         char[] temp = new char[length];
         System.arraycopy(this.data, 0, temp, 0, length);
         this.data = temp;
      }

      this.string = null;
   }

   public final XText copy() {
      int len = this.length;
      XText copy = new XText();
      copy.length = len;
      copy.data = new char[len];
      copy.string = this.string;
      copy.cdata = this.cdata;
      System.arraycopy(this.data, 0, copy.data, 0, len);
      return copy;
   }

   @Override
   public void write(XWriter out) {
      int length = this.length;
      char[] data = this.data;
      if (this.cdata) {
         out.w("<![CDATA[");
         if (data == null) {
            out.w(this.string);
         } else {
            for (int i = 0; i < length; i++) {
               out.w(data[i]);
            }
         }

         out.w("]]>");
      } else if (data == null) {
         out.safe(this.string, false);
      } else {
         for (int i = 0; i < length; i++) {
            out.safe(data[i], false);
         }
      }
   }

   @Override
   public String toString() {
      return this.string();
   }
}
