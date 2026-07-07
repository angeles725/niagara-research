package com.tridium.svg.batik;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import javax.baja.file.BIFile;
import javax.baja.naming.BOrd;
import org.apache.batik.ext.awt.image.renderable.DeferRable;
import org.apache.batik.ext.awt.image.renderable.Filter;
import org.apache.batik.ext.awt.image.renderable.RedRable;
import org.apache.batik.ext.awt.image.rendered.BufferedImageCachableRed;
import org.apache.batik.ext.awt.image.rendered.CachableRed;
import org.apache.batik.ext.awt.image.rendered.RenderedImageCachableRed;
import org.apache.batik.ext.awt.image.spi.AbstractRegistryEntry;
import org.apache.batik.ext.awt.image.spi.ImageTagRegistry;
import org.apache.batik.ext.awt.image.spi.JDKRegistryEntry;
import org.apache.batik.ext.awt.image.spi.URLRegistryEntry;
import org.apache.batik.util.ParsedURL;

public class OrdRegistryEntry extends AbstractRegistryEntry implements URLRegistryEntry {
   private final JDKRegistryEntry entry = new JDKRegistryEntry();

   public OrdRegistryEntry() {
      super("ORD", 999999.0F, new String[0], new String[]{"image/gif", "image/png", "image/jpg", "image/jpeg", "image/svg"});
   }

   public boolean isCompatibleURL(ParsedURL url) {
      return url.getProtocol().equals("ord");
   }

   private static CachableRed wrap(RenderedImage ri) {
      if (ri instanceof CachableRed) {
         return (CachableRed)ri;
      } else {
         return (CachableRed)(ri instanceof BufferedImage ? new BufferedImageCachableRed((BufferedImage)ri) : new RenderedImageCachableRed(ri));
      }
   }

   public Filter handleURL(ParsedURL url, boolean needRawData) {
      DeferRable dr = new DeferRable();
      String errCode;
      Object[] errParam;
      if (url != null) {
         errCode = "url.format.unreadable";
         errParam = new Object[]{"ORD", url};
      } else {
         errCode = "stream.format.unreadable";
         errParam = new Object[]{"ORD"};
      }

      new Thread(() -> {
         Filter filt = null;
         if (url != null) {
            try {
               BOrd ord = BOrd.make(BatikOrdUtils.fromBatikUrl(url));
               BIFile file = (BIFile)ord.get();
               byte[] bytes = file.read();
               Toolkit tk = Toolkit.getDefaultToolkit();
               Image img = tk.createImage(bytes);
               if (img != null) {
                  RenderedImage ri = this.entry.loadImage(img, dr);
                  if (ri != null) {
                     filt = new RedRable(wrap(ri));
                  }
               }
            } catch (ThreadDeath var12) {
               filt = ImageTagRegistry.getBrokenLinkImage(this, errCode, errParam);
               dr.setSource(filt);
               throw var12;
            } catch (Throwable var13) {
            }
         }

         if (filt == null) {
            filt = ImageTagRegistry.getBrokenLinkImage(this, errCode, errParam);
         }

         dr.setSource(filt);
      }, "OrdRegistryEntry#handleUrl Thread").start();
      return dr;
   }
}
