package javax.baja.xml;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class XWriter extends Writer {
   private static String[] SPACES = new String[50];
   private FileDescriptor fd;
   private OutputStream sink;
   private Writer xout;
   private ZipOutputStream zout;
   private boolean zipped;
   private int numWritten;

   public XWriter(File file) throws IOException {
      FileOutputStream fos = new FileOutputStream(file);

      try {
         this.fd = fos.getFD();
      } catch (IOException rethrow) {
         fos.close();
         throw rethrow;
      }

      this.sink = new BufferedOutputStream(fos);
   }

   public XWriter(OutputStream out) throws IOException {
      this.sink = out;
      if (out instanceof FileOutputStream) {
         this.fd = ((FileOutputStream)out).getFD();
      }
   }

   public XWriter() {
   }

   public void setOutputStream(OutputStream outputStream) {
      if (this.sink != null) {
         throw new IllegalStateException("Can not re-initialize sink for same instance");
      }

      if (outputStream == null) {
         throw new IllegalArgumentException("Output stream is null");
      }

      if (outputStream instanceof FileOutputStream) {
         throw new IllegalArgumentException("Output stream is instance of FileOutputStream, must use OutputStream constructor");
      }

      this.sink = outputStream;
   }

   public XWriter w(Object x) {
      this.write(String.valueOf(x));
      return this;
   }

   public final XWriter w(boolean x) {
      this.write(String.valueOf(x));
      return this;
   }

   public final XWriter w(char x) {
      this.write(x);
      return this;
   }

   public final XWriter w(int x) {
      this.write(String.valueOf(x));
      return this;
   }

   public final XWriter w(long x) {
      this.write(String.valueOf(x));
      return this;
   }

   public final XWriter w(float x) {
      this.write(String.valueOf(x));
      return this;
   }

   public final XWriter w(double x) {
      this.write(String.valueOf(x));
      return this;
   }

   public final XWriter nl() {
      this.write(10);
      return this;
   }

   public final XWriter indent(int indent) {
      this.write(getSpaces(indent));
      return this;
   }

   public final XWriter attr(String name, String value) {
      this.write(name);
      this.write(61);
      this.write(34);
      this.safe(value);
      this.write(34);
      return this;
   }

   public XWriter prolog() {
      this.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      return this;
   }

   public final XWriter safe(String s, boolean escapeWhitespace) {
      try {
         safe(this.xout, s, escapeWhitespace);
         return this;
      } catch (IOException e) {
         throw this.error(e);
      }
   }

   public final XWriter safe(String s) {
      try {
         safe(this.xout, s, true);
         return this;
      } catch (IOException e) {
         throw this.error(e);
      }
   }

   public final XWriter safe(int c, boolean escapeWhitespace) {
      try {
         safe(this, c, escapeWhitespace);
         return this;
      } catch (IOException e) {
         throw this.error(e);
      }
   }

   public static void safe(Writer out, String s, boolean escapeWhitespace) throws IOException {
      int len = s.length();

      for (int i = 0; i < len; i++) {
         safe(out, s.charAt(i), escapeWhitespace);
      }
   }

   public static void safe(Writer out, int c, boolean escapeWhitespace) throws IOException {
      if (c >= 32 && c <= 126 && c != 39 && c != 34) {
         if (c == 60) {
            out.write("&lt;");
         } else if (c == 62) {
            out.write("&gt;");
         } else if (c == 38) {
            out.write("&amp;");
         } else {
            out.write((char)c);
         }
      } else {
         if (!escapeWhitespace) {
            if (c == 10) {
               out.write(10);
               return;
            }

            if (c == 13) {
               out.write(13);
               return;
            }

            if (c == 9) {
               out.write(9);
               return;
            }
         }

         out.write("&#x");
         out.write(Integer.toHexString(c));
         out.write(59);
      }
   }

   public static String safeToString(String s, boolean escapeWhitespace) {
      try {
         ByteArrayOutputStream bout = new ByteArrayOutputStream();
         XWriter out = new XWriter(bout);
         safe(out, s, escapeWhitespace);
         out.flush();
         return new String(bout.toByteArray());
      } catch (IOException e) {
         throw new RuntimeException(e.toString());
      }
   }

   public boolean isZipped() {
      return this.zipped;
   }

   public void setZipped(boolean zipped) throws IOException {
      if (this.numWritten != 0) {
         throw new IllegalStateException("Cannot setZipped after data has been written");
      }

      this.zipped = zipped;
   }

   @Override
   public void write(int c) {
      try {
         if (this.xout == null) {
            this.initOut();
         }

         this.numWritten++;
         this.xout.write(c);
      } catch (IOException e) {
         throw this.error(e);
      }
   }

   @Override
   public void write(char[] buf) {
      try {
         if (this.xout == null) {
            this.initOut();
         }

         this.numWritten += buf.length;
         this.xout.write(buf);
      } catch (IOException e) {
         throw this.error(e);
      }
   }

   @Override
   public void write(char[] buf, int off, int len) {
      try {
         if (this.xout == null) {
            this.initOut();
         }

         this.numWritten += len;
         this.xout.write(buf, off, len);
      } catch (IOException e) {
         throw this.error(e);
      }
   }

   @Override
   public void write(String str) {
      try {
         if (this.xout == null) {
            this.initOut();
         }

         this.numWritten = this.numWritten + str.length();
         this.xout.write(str);
      } catch (IOException e) {
         throw this.error(e);
      }
   }

   @Override
   public void write(String str, int off, int len) {
      try {
         if (this.xout == null) {
            this.initOut();
         }

         this.numWritten += len;
         this.xout.write(str, off, len);
      } catch (IOException e) {
         throw this.error(e);
      }
   }

   @Override
   public void flush() {
      try {
         if (this.xout == null) {
            this.initOut();
         }

         this.xout.flush();
      } catch (IOException e) {
         throw this.error(e);
      }
   }

   @Override
   public void close() {
      try {
         if (this.xout == null) {
            this.initOut();
         }

         if (this.zipped) {
            this.xout.flush();
            this.zout.closeEntry();
            this.xout.flush();
         }

         if (this.fd != null) {
            this.fd.sync();
         }

         this.xout.close();
      } catch (IOException e) {
         throw this.error(e);
      }
   }

   void initOut() throws IOException {
      if (this.sink == null) {
         throw new IOException("sink not initialized");
      }

      if (this.zipped) {
         this.zout = new ZipOutputStream(this.sink);
         this.zout.putNextEntry(new ZipEntry("file.xml"));
         this.xout = new OutputStreamWriter(this.zout, "UTF8");
      } else {
         this.xout = new OutputStreamWriter(this.sink, "UTF8");
      }
   }

   XException error(IOException e) {
      throw new XException(e.toString(), e);
   }

   static String getSpaces(int num) {
      try {
         return SPACES[num];
      } catch (ArrayIndexOutOfBoundsException e) {
         if (num < 0) {
            return "";
         }

         int len = SPACES.length;
         StringBuilder buf = new StringBuilder(num);

         int rem;
         for (rem = num; rem >= len; rem -= len - 1) {
            buf.append(SPACES[len - 1]);
         }

         buf.append(SPACES[rem]);
         return buf.toString();
      }
   }

   static {
      SPACES[0] = "";

      for (int i = 1; i < 50; i++) {
         SPACES[i] = SPACES[i - 1] + " ";
      }
   }
}
