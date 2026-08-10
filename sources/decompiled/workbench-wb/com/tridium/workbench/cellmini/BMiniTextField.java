package com.tridium.workbench.cellmini;

import javax.baja.gx.BInsets;
import javax.baja.gx.Graphics;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BTextDropDown;
import javax.baja.ui.BTextField;
import javax.baja.ui.BWidget;
import javax.baja.ui.UndoManager;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.text.Position;
import javax.baja.ui.text.SingleLineParser;
import javax.baja.ui.text.TextController;
import javax.baja.workbench.celleditor.BWbCellEditor;

@NiagaraType
public class BMiniTextField extends BTextField {
   public static final Type TYPE = Sys.loadType(BMiniTextField.class);
   private static final BIcon icon = BIcon.std("widgets/textField.png");
   private static BInsets insets = BInsets.make(0.0, 0.0, 0.0, 0.0);
   private double scrollOffset;

   public Type getType() {
      return TYPE;
   }

   public BMiniTextField() {
      this.setParser(new SingleLineParser());
      this.setInstalledUndoManager(new UndoManager(this));
      this.setController(new BMiniTextField.Controller());
   }

   public boolean isSingleLine() {
      return true;
   }

   public void scrollToVisible(Position pos) {
      double cellWidth = this.getRenderer().getColumnWidth(null, 0);
      double width = this.getWidth() - insets.left - insets.right;
      double cx1 = this.scrollOffset;
      double cx2 = cx1 + width;
      double dx1 = pos.column * cellWidth - insets.left;
      double dx2 = dx1 + cellWidth;
      if (cx2 < dx2) {
         cx1 += dx2 - cx2;
      }

      if (cx1 > dx1) {
         cx1 = dx1;
      }

      if (cx1 < 0.0) {
         cx1 = 0.0;
      }

      this.scrollOffset = cx1;
      this.repaint();
   }

   public Position getPositionAt(double x, double y) {
      return super.getPositionAt(x + this.scrollOffset, y);
   }

   public void computePreferredSize() {
   }

   protected BInsets getInsets() {
      return insets;
   }

   public void paint(Graphics g) {
      g.translate(-this.scrollOffset, 0.0);
      super.paint(g);
      g.translate(this.scrollOffset, 0.0);
   }

   protected void paintBorder(Graphics g) {
   }

   public void keyPressed(BKeyEvent event) {
      if (event.getKeyCode() != 10 && event.getKeyCode() != 27) {
         if (event.getKeyCode() == 40) {
            BObject parent = this.getParent();
            if (parent instanceof BTextDropDown) {
               ((BTextDropDown)parent).openDropDown();
            }
         } else {
            super.keyPressed(event);
         }
      }
   }

   public void keyTyped(BKeyEvent event) {
      char key = event.getKeyChar();
      if (key != '\n' && key != '\r') {
         if (event.getKeyCode() != 27) {
            super.keyTyped(event);
         }
      }
   }

   public BIcon getIcon() {
      return icon;
   }

   private class Controller extends TextController {
      private BWbCellEditor ce;

      private Controller() {
      }

      public void mousePressed(BMouseEvent event) {
         BWbCellEditor ce = this.getCellEditor();
         ce.cellSelected();
         if (event.isButton1Down()) {
            super.mousePressed(event);
         } else if (event.isButton3Down()) {
            ce.cellPopup(event);
         }
      }

      private BWbCellEditor getCellEditor() {
         if (this.ce == null) {
            BWidget parent = BMiniTextField.this.getParentWidget();

            while (!(parent instanceof BWbCellEditor)) {
               parent = parent.getParentWidget();
            }

            this.ce = (BWbCellEditor)parent;
         }

         return this.ce;
      }
   }
}
