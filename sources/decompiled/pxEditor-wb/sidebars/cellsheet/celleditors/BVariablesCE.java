package com.tridium.px.editor.sidebars.cellsheet.celleditors;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.ui.px.BPxInclude;
import javax.baja.workbench.celleditor.BButtonCE;

@NiagaraType
public class BVariablesCE extends BButtonCE {
   public static final Type TYPE = Sys.loadType(BVariablesCE.class);
   private BOrd[] origOrds;

   public Type getType() {
      return TYPE;
   }

   public BVariablesCE() {
      this(new BOrd[0]);
   }

   public BVariablesCE(BOrd[] origOrds) {
      this.origOrds = origOrds;
   }

   public static BFacets makeFacets(BPxInclude inc) {
      BFacets fac = inc.getVariables();
      String[] keys = getVariableKeys(inc.getRootWidget());
      if (fac.isNull() && keys.length > 0) {
         BString[] vals = new BString[keys.length];

         for (int i = 0; i < keys.length; i++) {
            vals[i] = BString.DEFAULT;
         }

         fac = BFacets.make(keys, vals);
      }

      return fac;
   }

   public static String[] getVariableKeys(BWidget root) {
      Set<String> keySet = new TreeSet<>();
      findIncludeVars(root, keySet);
      return keySet.toArray(new String[0]);
   }

   private static void findIncludeVars(BComponent comp, Set<String> keySet) {
      if (comp != null) {
         BOrd[] ords = (BOrd[])comp.getChildren(BOrd.class);

         for (int i = 0; i < ords.length; i++) {
            keySet.addAll(Arrays.asList(ords[i].getVariables()));
         }

         BComponent[] kids = comp.getChildComponents();

         for (int i = 0; i < kids.length; i++) {
            findIncludeVars(kids[i], keySet);
         }
      }
   }

   public void computePreferredSize() {
      this.setPreferredSize(150.0, 10.0);
   }

   protected BObject dialog() throws Exception {
      return BVariablesEditor.open(this, this.getPropertyName(), (BFacets)this.value, this.origOrds, this.isReadonly());
   }
}
