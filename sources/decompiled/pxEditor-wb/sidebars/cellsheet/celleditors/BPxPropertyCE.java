package com.tridium.px.editor.sidebars.cellsheet.celleditors;

import javax.baja.gx.BBrush;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.px.PxProperty;
import javax.baja.workbench.celleditor.BButtonCE;

@NiagaraType
public class BPxPropertyCE extends BButtonCE {
   public static final Type TYPE = Sys.loadType(BPxPropertyCE.class);
   private static final BBrush BG = BColor.make(187, 255, 187).toBrush();
   private String groupNames;

   public Type getType() {
      return TYPE;
   }

   public BPxPropertyCE(PxProperty[] groups) {
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < groups.length; i++) {
         if (i > 0) {
            sb.append(",");
         }

         sb.append(groups[i].getName());
      }

      this.groupNames = sb.toString();
   }

   public BBrush getBackground() {
      return BG;
   }

   public void paint(Graphics g) {
      this.paintBackground(g);
      this.drawString(g, this.groupNames);
   }

   protected BObject dialog() throws Exception {
      return null;
   }
}
