package com.tridium.nre.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;

public final class InputStreamInfo {
   private boolean isZipped;
   private int encoding;
   private InputStream in;
   private boolean autoDetected;
   static final String[] ENCODINGS = new String[]{"UTF-8", "UTF-16BE", "UTF-16LE"};
   public static final int UTF_8 = 0;
   public static final int UTF_16_BE = 1;
   public static final int UTF_16_LE = 2;
   static final Logger log = Logger.getLogger("InputStreamInfo");

   public InputStreamInfo(InputStream inputStream) {
      this.in = inputStream;
   }

   public boolean isZipped() {
      this.detect();
      return this.isZipped;
   }

   public int getEncoding() {
      this.detect();
      return this.encoding;
   }

   public String getEncodingTag() {
      this.detect();
      return ENCODINGS[this.encoding];
   }

   public Reader toReader() throws IOException {
      this.detect();
      if (this.isZipped()) {
         ZipInputStream unzip = new ZipInputStream(this.in);
         unzip.getNextEntry();
         return new InputStreamReader(new BufferedInputStream(unzip), Charset.forName(this.getEncodingTag()));
      } else {
         return new InputStreamReader(this.in, Charset.forName(this.getEncodingTag()));
      }
   }

   private void detect() {
      if (!this.autoDetected) {
         try {
            if (!this.in.markSupported()) {
               this.in = new BufferedInputStream(this.in);
            }

            int[] sig = new int[4];
            this.in.mark(4);
            sig[0] = this.in.read();
            sig[1] = this.in.read();
            sig[2] = this.in.read();
            sig[3] = this.in.read();
            this.in.reset();
            if (match(sig, 80, 75, 3, 4)) {
               this.isZipped = true;
            }

            int enc;
            if (this.match(sig, 254, 255)) {
               this.in.read();
               this.in.read();
               enc = 1;
            } else if (this.match(sig, 255, 254)) {
               this.in.read();
               this.in.read();
               enc = 2;
            } else if (this.match(sig, 239, 187, 191)) {
               this.in.read();
               this.in.read();
               this.in.read();
               enc = 0;
            } else {
               enc = 0;
            }

            this.encoding = enc;
            this.autoDetected = true;
         } catch (IOException ex) {
            log.log(Level.INFO, "Failed to read stream", ex);
         }
      }
   }

   private boolean match(int[] sig, int b0, int b1, int b2) {
      return sig[0] == b0 && sig[1] == b1 && sig[2] == b2;
   }

   private boolean match(int[] sig, int b0, int b1) {
      return sig[0] == b0 && sig[1] == b1;
   }

   private static boolean match(int[] sig, int b0, int b1, int b2, int b3) {
      return sig[0] == b0 && sig[1] == b1 && sig[2] == b2 && sig[3] == b3;
   }
}
