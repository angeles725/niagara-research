package com.tridium.template.file;

import com.tridium.util.CompUtil;
import java.io.InputStream;
import java.util.List;
import javax.baja.agent.BPxView;
import javax.baja.file.BIFile;
import javax.baja.naming.BOrd;
import javax.baja.naming.NullOrdException;
import javax.baja.naming.SyntaxException;
import javax.baja.sys.BComponent;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;

public final class EmbeddedPxScanner {
   public static void findPxAndImageOrds(EmbeddedPxSource fileSource, BComponent root, List<String> pxList, List<String> imageList) {
      BPxView[] pxViews = (BPxView[])CompUtil.getDescendants(root, BPxView.class);

      for (BPxView view : pxViews) {
         String pxOrd = view.getPxFile().encodeToString();
         if (pxOrd != null && !pxList.contains(pxOrd)) {
            pxList.add(pxOrd);
            BOrd ord = view.getPxFile();

            try {
               BIFile file = fileSource.getPxFile(ord);

               XElem rootElement;
               try (InputStream in = file.getInputStream()) {
                  rootElement = XParser.make(in).parse();
               }

               findPxIncludeOrds(fileSource, rootElement, pxList, imageList);
            } catch (Exception var26) {
            }
         }
      }
   }

   private static void findPxIncludeOrds(EmbeddedPxSource fileSource, XElem element, List<String> pxList, List<String> imageList) {
      XElem[] children = element.elems();

      for (XElem child : children) {
         String name = child.name();
         if ("PxInclude".equals(name)) {
            try {
               String ord = child.get("ord");
               if (ord != null && !pxList.contains(ord) && ord.startsWith("file:")) {
                  pxList.add(ord);
                  BIFile nextFile = fileSource.getPxFile(BOrd.make(ord));

                  XElem rootElement;
                  try (InputStream in = nextFile.getInputStream()) {
                     rootElement = XParser.make(in).parse();
                  }

                  findPxIncludeOrds(fileSource, rootElement, pxList, imageList);
               }
            } catch (Exception var30) {
            }
         } else if ("Image".equals(name)) {
            try {
               String ord = child.get("value");
               if (ord != null && !imageList.contains(ord) && ord.startsWith("file:")) {
                  imageList.add(ord);
               }
            } catch (Exception var28) {
            }
         } else if (!"INumericToSimple".equals(name) && !"IEnumToSimple".equals(name)) {
            int index = child.attrIndex("image");
            if (index != -1) {
               String ord = child.get("image");
               if (ord != null && !imageList.contains(ord) && ord.startsWith("file:")) {
                  imageList.add(ord);
               }
            }
         } else if (child.attrIndex("map") != -1) {
            String map = child.get("map");
            map = map.substring(map.indexOf(32) + 1);
            String[] entries = map.split(";");

            for (String entry : entries) {
               int index = entry.indexOf(61);
               String possibleOrd = entry.substring(index + 1);
               if (possibleOrd.startsWith("file:")) {
                  try {
                     BOrd.make(possibleOrd).parse();
                  } catch (SyntaxException | NullOrdException var31) {
                     continue;
                  }

                  if (!imageList.contains(possibleOrd) && possibleOrd.startsWith("file:")) {
                     imageList.add(possibleOrd);
                  }
               }
            }
         }

         findPxIncludeOrds(fileSource, child, pxList, imageList);
      }
   }

   private EmbeddedPxScanner() {
   }
}
