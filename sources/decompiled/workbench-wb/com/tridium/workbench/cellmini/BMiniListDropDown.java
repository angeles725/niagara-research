package com.tridium.workbench.cellmini;

import com.tridium.ui.theme.Theme;
import javax.baja.gx.Graphics;
import javax.baja.gx.RectGeom;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BListDropDown;
import javax.baja.ui.BWidget;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.workbench.celleditor.BWbCellEditor;

@NiagaraType
public class BMiniListDropDown extends BListDropDown {
   public static final Type TYPE = Sys.loadType(BMiniListDropDown.class);
   private BMiniListDropDownDisplay display = new BMiniListDropDownDisplay();
   private RectGeom buttonBounds = new RectGeom();

   public Type getType() {
      return TYPE;
   }

   public BMiniListDropDown() {
      this.setDisplayWidget(this.display);
   }

   public void computePreferredSize() {
      this.display.computePreferredSize();
      double pw = this.display.getPreferredWidth();
      double ph = this.display.getPreferredHeight();
      if (this.getDropDownEnabled()) {
         pw += ph;
      }

      this.setPreferredSize(pw, ph);
   }

   public void doLayout(BWidget[] kids) {
      double w = this.getWidth();
      double h = this.getHeight();
      this.display.computePreferredSize();
      if (this.getDropDownEnabled()) {
         double ph = Math.min(h, this.display.getPreferredHeight());
         this.buttonBounds.width = ph;
         this.buttonBounds.height = h - 4.0;
         this.buttonBounds.x = w - ph - 2.0;
         this.buttonBounds.y = 1.0;
      } else {
         this.buttonBounds.x = 0.0;
         this.buttonBounds.y = 0.0;
         this.buttonBounds.width = 0.0;
         this.buttonBounds.height = 0.0;
      }

      this.display.setBounds(0.0, 0.0, w - (int)this.buttonBounds.width, h);
      if (!this.isDropDownOpen()) {
         BWidget dropDown = this.getDropDownWidget();
         dropDown.setBounds(0.0, h, 0.0, 0.0);
      }
   }

   public void paint(Graphics g) {
      this.paintChild(g, this.display);
      if (this.getDropDownEnabled()) {
         Theme.dropDown().paintButton(g, this, this.buttonBounds, this.isDropDownOpen());
      }
   }

   public void mousePressed(BMouseEvent event) {
      BWbCellEditor ce = (BWbCellEditor)this.getParent();
      ce.cellSelected();
      if (event.isButton1Down()) {
         this.openDropDown();
      } else if (event.isButton3Down()) {
         ce.cellPopup(event);
      }

      this.repaint();
   }
}
