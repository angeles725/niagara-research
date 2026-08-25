package com.tridium.niagarad.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;

public class XModuleInfoParser extends XParser {
   private boolean foundDependencies = false;
   private static final int _MODULE_CHILD_DEPTH = 2;
   private static final Set<String> _FILTER_ELEMS = new HashSet<>();

   public XModuleInfoParser(InputStream in) throws IOException {
      super(in);
   }

   public XElem parseCurrent(boolean close) throws Exception {
      try {
         int depth = 1;
         XElem root = this.elem().copy();
         XElem cur = root;

         while (depth > 0 && !this.foundDependencies) {
            int type = this.next();
            if (type == 1) {
               XElem oldCur = cur;
               cur = this.elem().copy();
               String name = cur.name();
               if (this.depth() == 2 && _FILTER_ELEMS.contains(name.toLowerCase())) {
                  this.skip();
               } else {
                  oldCur.addContent(cur);
                  depth++;
               }
            } else if (type == 2) {
               if (this.depth() == 2 && cur.name().equalsIgnoreCase("dependencies")) {
                  this.foundDependencies = true;
               }

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

   static {
      _FILTER_ELEMS.add("extfiles");
      _FILTER_ELEMS.add("types");
      _FILTER_ELEMS.add("defs");
      _FILTER_ELEMS.add("dirs");
   }
}
