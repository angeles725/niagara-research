package com.tridium.svg.batik;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import javax.baja.file.BIFile;
import javax.baja.naming.BOrd;
import org.apache.batik.util.ParsedURLData;

public class OrdParsedURLData extends ParsedURLData {
   public static final String PROTOCOL = "ord";
   private final BOrd ord;

   public OrdParsedURLData(BOrd ord, String ref) {
      if (ord == null) {
         throw new IllegalArgumentException("ORD required");
      } else {
         this.protocol = "ord";
         String ordString = ord.toString();
         if (ordString.indexOf(35) >= 0) {
            throw new IllegalArgumentException("ORD cannot contain a '#' character");
         } else if (ref != null && ref.indexOf(35) >= 0) {
            throw new IllegalArgumentException("ref cannot contain a '#' character");
         } else {
            this.path = URI.create(BatikOrdUtils.toBatikUrl(ord, ref)).getPath();
            this.ord = ord;
            this.ref = ref;
         }
      }
   }

   public InputStream openStream(String userAgent, Iterator mimeTypes) throws IOException {
      BIFile file = (BIFile)this.ord.get();
      return new ByteArrayInputStream(file.read());
   }

   public InputStream openStreamRaw(String userAgent, Iterator mimeTypes) throws IOException {
      return this.openStream(userAgent, mimeTypes);
   }

   public String toString() {
      return BatikOrdUtils.toBatikUrl(this.ord, this.ref);
   }

   public boolean complete() {
      return true;
   }

   public int hashCode() {
      return this.ord.hashCode();
   }

   public boolean equals(Object o) {
      if (!(o instanceof OrdParsedURLData)) {
         return false;
      } else {
         OrdParsedURLData data = (OrdParsedURLData)o;
         if (!this.ord.equals(data.ord)) {
            return false;
         } else {
            return this.ref == null ? data.ref == null : this.ref.equals(data.ref);
         }
      }
   }

   protected boolean sameFile(ParsedURLData other) {
      if (!(other instanceof OrdParsedURLData)) {
         return false;
      } else {
         OrdParsedURLData data = (OrdParsedURLData)other;
         return this.ord.equals(data.ord);
      }
   }

   public BOrd getOrd() {
      return this.ord;
   }
}
