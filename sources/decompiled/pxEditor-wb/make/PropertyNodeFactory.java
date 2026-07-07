package com.tridium.px.editor.make;

import java.util.ArrayList;
import java.util.List;
import javax.baja.agent.BPxView;
import javax.baja.sys.BComplex;
import javax.baja.sys.BLink;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.BStruct;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.util.BWsAnnotation;

abstract class PropertyNodeFactory {
   protected abstract PropertyNode make(BObject var1, String var2, String var3);

   PropertyNode[] makeAll(BObject obj) {
      List<PropertyNode> arr = new ArrayList<>();
      if (obj instanceof BComplex) {
         BComplex comp = (BComplex)obj;
         Property[] p = comp.getPropertiesArray();

         for (int i = 0; i < p.length; i++) {
            if (!Flags.isHidden(comp, p[i])) {
               BObject child = comp.get(p[i]);
               if (!(child instanceof BLink)
                  && !(child instanceof BWsAnnotation)
                  && !(child instanceof BPxView)
                  && (child instanceof BStruct || child instanceof BSimple)) {
                  arr.add(this.make(child, p[i].getName(), comp.getDisplayName(p[i], null)));
               }
            }
         }
      }

      return arr.toArray(new PropertyNode[0]);
   }
}
