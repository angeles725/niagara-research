package com.tridium.px.editor.sidebars.binding;

import java.util.ArrayList;
import java.util.List;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.px.editor.BPxEditor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BRelativizeOrds extends BOrdChanger {
   public static final Type TYPE = Sys.loadType(BRelativizeOrds.class);
   String[] baseNames;

   @Override
   public Type getType() {
      return TYPE;
   }

   public BRelativizeOrds(BPxEditor editor, String[] baseNames, BOrd[] before) {
      super(editor, before);
      this.baseNames = baseNames;
   }

   @Override
   protected BOrd after(BOrd oldOrd) {
      BOrd newOrd = relativizeOrd(this.baseNames, oldOrd);
      return newOrd == null ? oldOrd : newOrd;
   }

   @Override
   protected boolean selectable(int row) {
      return relativizeOrd(this.baseNames, this.before[row]) != null;
   }

   static BOrd relativizeOrd(String[] baseNames, BOrd oldOrd) {
      if (oldOrd.isNull()) {
         return null;
      } else {
         OrdQuery[] q = oldOrd.parse();
         if (q.length == 0) {
            return null;
         } else {
            int n = 0;
            String scheme = q[n].getScheme();
            if (scheme.equals("memory")) {
               n++;
               n++;
               if (q.length == 2) {
                  return null;
               }
            } else if (scheme.equals("station") || scheme.equals("bog")) {
               n++;
               if (q.length == 1) {
                  return null;
               }
            }

            if (!q[n].getScheme().equals("slot")) {
               return null;
            } else {
               SlotPath path = (SlotPath)q[n];
               String[] names = path.getNames();
               if (names.length == 0) {
                  return null;
               } else if (!names[0].equals(baseNames[0])) {
                  return null;
               } else {
                  String rel = relative(baseNames, names);
                  List<OrdQuery> queries = new ArrayList<>();
                  queries.add(new SlotPath("slot", rel));

                  for (int i = n + 1; i < q.length; i++) {
                     queries.add(q[i]);
                  }

                  return BOrd.make(queries.toArray(new OrdQuery[0]));
               }
            }
         }
      }
   }

   private static String relative(String[] from, String[] to) {
      if (!from[0].equals(to[0])) {
         throw new IllegalStateException();
      } else {
         StringBuilder sb = new StringBuilder();
         int a = 1;

         while (a < to.length && a < from.length && from[a].equals(to[a])) {
            a++;
         }

         int b = from.length - a;

         for (int i = 0; i < b; i++) {
            if (sb.length() > 0) {
               sb.append("/");
            }

            sb.append("..");
         }

         for (int i = a; i < to.length; i++) {
            if (sb.length() > 0) {
               sb.append("/");
            }

            sb.append(to[i]);
         }

         return sb.toString();
      }
   }
}
