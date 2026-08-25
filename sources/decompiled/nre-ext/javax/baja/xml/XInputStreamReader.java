package javax.baja.xml;

import com.tridium.nre.util.InputStreamInfo;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UTFDataFormatException;
import java.util.zip.ZipInputStream;

public class XInputStreamReader extends Reader {
   static final int UTF_8 = 0;
   static final int UTF_16_BE = 1;
   static final int UTF_16_LE = 2;
   private InputStreamInfo inputStreamInfo;
   private InputStream in;
   private boolean autoDetected;
   private boolean zipped;
   private int encoding = -1;

   public XInputStreamReader(InputStream in) throws IOException {
      if (in.markSupported()) {
         this.in = in;
      } else {
         this.in = new BufferedInputStream(in);
      }
   }

   public String getEncoding() throws IOException {
      if (!this.autoDetected) {
         this.autoDetect();
      }

      return this.inputStreamInfo.getEncodingTag();
   }

   public boolean isZipped() throws IOException {
      if (!this.autoDetected) {
         this.autoDetect();
      }

      return this.zipped;
   }

   @Override
   public int read() throws IOException {
      if (!this.autoDetected) {
         this.autoDetect();
      }

      switch (this.encoding) {
         case 0:
            int c = this.in.read();
            if (c < 0) {
               return -1;
            } else {
               if ((c & 128) == 0) {
                  return c;
               }

               return this.readUtf8(c);
            }
         case 1:
            return this.readUtf16be();
         case 2:
            return this.readUtf16le();
         default:
            throw new IllegalStateException();
      }
   }

   @Override
   public int read(char[] buf, int off, int len) throws IOException {
      if (!this.autoDetected) {
         this.autoDetect();
      }

      int last = off + len;

      for (int i = 0; i < len; i++) {
         int c = this.read();
         if (c < 0) {
            return i == 0 ? -1 : i;
         }

         buf[off + i] = (char)c;
      }

      return len;
   }

   @Override
   public int read(char[] buf) throws IOException {
      return this.read(buf, 0, buf.length);
   }

   @Override
   public void close() throws IOException {
      this.in.close();
   }

   private void autoDetect() throws IOException {
      this.inputStreamInfo = new InputStreamInfo(this.in);
      if (this.inputStreamInfo.isZipped()) {
         this.zipped = true;
         ZipInputStream unzip = new ZipInputStream(this.in);
         unzip.getNextEntry();
         this.in = new BufferedInputStream(unzip);
         this.autoDetect();
      } else {
         this.encoding = this.inputStreamInfo.getEncoding();
         this.autoDetected = true;
      }
   }

   private int readUtf8(int c0) throws IOException {
      switch (c0 >> 4) {
         case 12:
         case 13:
            int c1 = this.in.read();
            if ((c1 & 192) != 128) {
               throw new UTFDataFormatException(Integer.toHexString(c0));
            }

            return (c0 & 31) << 6 | (c1 & 63) << 0;
         case 14:
            int c1 = this.in.read();
            int c2 = this.in.read();
            if ((c1 & 192) == 128 && (c2 & 192) == 128) {
               return (c0 & 15) << 12 | (c1 & 63) << 6 | (c2 & 63) << 0;
            }

            throw new UTFDataFormatException();
         case 15:
            throw new UTFDataFormatException(Integer.toHexString(c0));
         default:
            throw new UTFDataFormatException(Integer.toHexString(c0));
      }
   }

   private int readUtf16be() throws IOException {
      int c0 = this.in.read();
      int c1 = this.in.read();
      return c0 < 0 ? -1 : (c0 & 0xFF) << 8 | (c1 & 0xFF) << 0;
   }

   private int readUtf16le() throws IOException {
      int c0 = this.in.read();
      int c1 = this.in.read();
      return c0 < 0 ? -1 : (c1 & 0xFF) << 8 | (c0 & 0xFF) << 0;
   }
}
