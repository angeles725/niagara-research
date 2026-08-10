package com.tridium.wiresheet;

import com.tridium.gx.util.PointMap;
import com.tridium.gx.util.RectangleMap;
import com.tridium.ui.theme.Theme;
import com.tridium.workbench.commands.PasteSpecialCommand;
import com.tridium.workbench.transfer.TransferUtil;
import java.util.ArrayList;
import javax.baja.gx.Graphics;
import javax.baja.gx.IPoint;
import javax.baja.gx.Point;
import javax.baja.gx.RectGeom;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.space.Mark;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.ui.CommandArtifact;
import javax.baja.ui.event.BFocusEvent;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.event.BMouseWheelEvent;
import javax.baja.ui.transfer.BTransferWidget;
import javax.baja.ui.transfer.TransferContext;
import javax.baja.ui.transfer.TransferEnvelope;

@NiagaraType
public class BWsCanvas extends BTransferWidget implements WsConst {
   public static final Type TYPE = Sys.loadType(BWsCanvas.class);
   static final int DRAG_LINE = 50;
   private RectGeom dragRect;
   private TransferContext dragOverContext;
   RootGlyph rootGlyph;
   RectangleMap componentGlyphMap = null;
   PointMap scaleMap = null;

   public Type getType() {
      return TYPE;
   }

   public RootGlyph getRootGlyph() {
      return this.rootGlyph == null ? (this.rootGlyph = new RootGlyph(this.getWireSheet())) : this.rootGlyph;
   }

   public void computePreferredSize() {
      this.setPreferredSize(
         this.getWireSheet().grid.getWixelWidth() * this.getWireSheet().grid.wixel, this.getWireSheet().grid.getWixelHeight() * this.getWireSheet().grid.wixel
      );
   }

   public void doLayout(BWidget[] kids) {
      this.getWireSheet().controller.doLayout();
      this.getRootGlyph().layout();
   }

   void preLoad() {
      this.getRootGlyph().removeAll();
      this.componentGlyphMap = null;
      this.scaleMap = null;
   }

   void postLoad() {
      this.needsLayout();
      this.createLinkCrossings();
   }

   public Object[] getComponentGlyphs(int p, int q) {
      if (this.componentGlyphMap == null) {
         this.componentGlyphMap = new RectangleMap(0, 0, this.getWireSheet().grid.getWixelWidth(), this.getWireSheet().grid.getWixelHeight(), 20, 20);
         this.fillComponentGlyphMap(this.getRootGlyph());
      }

      return this.componentGlyphMap.get(p, q);
   }

   private void fillComponentGlyphMap(Glyph glyph) {
      if (glyph instanceof ComponentGlyph) {
         int p = glyph.absP();
         int q = glyph.absQ();
         int w = glyph.ww;
         int h = glyph.wh;
         if (p > 0) {
            p--;
            w++;
         }

         if (p + w + 1 < this.getWireSheet().grid.getWixelWidth()) {
            w++;
         }

         this.componentGlyphMap.put(p, q, w, h, glyph);
      }

      Glyph[] kids = glyph.getChildGlyphs();

      for (int i = 0; i < kids.length; i++) {
         if (kids[i].visible) {
            this.fillComponentGlyphMap(kids[i]);
         }
      }
   }

   private void createLinkCrossings() {
      if (this.scaleMap != null) {
         IPoint[] keys = this.scaleMap.getKeys();

         label56:
         for (int i = 0; i < keys.length; i++) {
            ArrayList<LinkSnakeGlyph.Scale> list = (ArrayList<LinkSnakeGlyph.Scale>)this.scaleMap.get((int)keys[i].x(), (int)keys[i].y());
            if (list.size() >= 2) {
               int vert = 0;
               int horz = 0;

               for (int j = 0; j < list.size(); j++) {
                  LinkSnakeGlyph.Scale sh = list.get(j);
                  if (sh.type == 1) {
                     horz++;
                  } else {
                     if (sh.type != 2) {
                        continue label56;
                     }

                     vert++;
                  }
               }

               if (vert != 0 && horz != 0) {
                  for (int jx = 0; jx < list.size(); jx++) {
                     LinkSnakeGlyph.Scale sh = list.get(jx);
                     if (sh.type == 2) {
                        sh.getOwner().replaceTilesAt((int)keys[i].x(), (int)keys[i].y(), 6);
                     } else if (sh.type == 1) {
                        sh.getOwner().replaceTilesAt((int)keys[i].x(), (int)keys[i].y(), 5);
                     }
                  }
               }
            }
         }
      }
   }

   public void addScale(int p, int q, LinkSnakeGlyph.Scale sh) {
      if (this.scaleMap == null) {
         this.scaleMap = new PointMap(0);
      }

      ArrayList<LinkSnakeGlyph.Scale> list = (ArrayList<LinkSnakeGlyph.Scale>)this.scaleMap.get(p, q);
      if (list == null) {
         list = new ArrayList<>();
         this.scaleMap.put(p, q, list);
      }

      list.add(sh);
   }

   public ArrayList<LinkSnakeGlyph.Scale> getScales(int p, int q) {
      if (this.scaleMap == null) {
         this.scaleMap = new PointMap(0);
      }

      return (ArrayList<LinkSnakeGlyph.Scale>)this.scaleMap.get(p, q);
   }

   public void paint(Graphics g) {
      this.rootGlyph.paint(g);
      this.paintSelection(g);
      this.getWireSheet().controller.getState().paintFx(g);
      if (this.dragOverContext != null) {
         double x = this.dragOverContext.getX();
         double y = this.dragOverContext.getY();
         g.setBrush(Theme.widget().getDropOkBackground());
         g.strokeLine(x, y, x + 50.0, y);
         g.strokeLine(x, y, x, y + 50.0);
      }
   }

   private void paintSelection(Graphics g) {
      Glyph[] selection = this.getWireSheet().selection.get();

      for (int i = 0; i < selection.length; i++) {
         Glyph s = selection[i];
         g.translate(s.x, s.y);
         s.paintSelection(g);
         g.translate(-s.x, -s.y);
      }
   }

   public boolean isFocusTraversable() {
      return true;
   }

   public void focusGained(BFocusEvent event) {
   }

   public void focusLost(BFocusEvent event) {
   }

   public final void keyPressed(BKeyEvent event) {
      this.getWireSheet().controller.keyPressed(event);
   }

   public final void keyReleased(BKeyEvent event) {
      this.getWireSheet().controller.keyReleased(event);
   }

   public final void keyTyped(BKeyEvent event) {
      this.getWireSheet().controller.keyTyped(event);
   }

   public final void mousePressed(BMouseEvent event) {
      this.requestFocus();
      this.getWireSheet().controller.mousePressed(event);
   }

   public final void mouseReleased(BMouseEvent event) {
      this.getWireSheet().controller.mouseReleased(event);
   }

   public final void mouseEntered(BMouseEvent event) {
      this.getWireSheet().controller.mouseEntered(event);
   }

   public final void mouseExited(BMouseEvent event) {
      this.getWireSheet().controller.mouseExited(event);
   }

   public final void mouseMoved(BMouseEvent event) {
      this.getWireSheet().controller.mouseMoved(event);
   }

   public final void mouseDragged(BMouseEvent event) {
      this.getWireSheet().controller.mouseDragged(event);
   }

   public final void mousePulsed(BMouseEvent event) {
      this.getWireSheet().controller.mousePulsed(event);
   }

   public final void mouseWheel(BMouseWheelEvent event) {
      this.getWireSheet().controller.mouseWheel(event);
   }

   public TransferEnvelope getTransferData() throws Exception {
      Mark mark = this.getWireSheet().controller.getSelectionAsMark();
      return mark == null ? null : TransferEnvelope.make(mark);
   }

   public final TransferContext makeTransferContext(Context src, int action, TransferEnvelope envelope) {
      return this.getWireSheet().controller.makeTransferContext(src, action, envelope);
   }

   public CommandArtifact insertTransferData(TransferContext cx) throws Exception {
      return this.getWireSheet().controller.insertTransferData(cx);
   }

   public CommandArtifact removeTransferData(TransferContext cx) throws Exception {
      return null;
   }

   public CommandArtifact doPasteSpecial() throws Exception {
      return new PasteSpecialCommand(this, this.getWireSheet().controller.container).doInvoke();
   }

   public CommandArtifact doDelete() throws Exception {
      Mark mark = this.getWireSheet().controller.getSelectionAsMark();
      return TransferUtil.delete(this, mark);
   }

   public CommandArtifact doRename() throws Exception {
      Mark mark = this.getWireSheet().controller.getSelectionAsMark();
      return TransferUtil.rename(this, mark);
   }

   public int dragOver(TransferContext cx) {
      this.dragOverContext = cx;
      this.repaintDragOver();
      int mask = 48;
      if (cx.isPulse()) {
         this.getWireSheet().controller.pulseViewport(cx.getX(), cx.getY());
      }

      return mask;
   }

   public void dragExit(TransferContext cx) {
      this.dragOverContext = null;
      this.repaintDragOver();
   }

   public CommandArtifact drop(TransferContext cx) throws Exception {
      this.dragOverContext = null;
      this.repaintDragOver();
      this.getWireSheet().controller.dropPoint = new Point(cx.getX(), cx.getY());
      return this.getWireSheet().controller.insertTransferData(cx);
   }

   private void repaintDragOver() {
      RectGeom dirty = this.dragRect;
      TransferContext cx = this.dragOverContext;
      if (cx == null) {
         dirty = this.dragRect;
         this.dragRect = null;
      } else {
         RectGeom newDirty = new RectGeom(cx.getX(), cx.getY(), 52.0, 52.0);
         RectGeom oldDirty = this.dragRect == null ? newDirty : this.dragRect;
         dirty = RectGeom.bounds(newDirty, oldDirty, null);
         this.dragRect = newDirty;
      }

      if (dirty != null) {
         this.repaint(dirty.x, dirty.y, dirty.width, dirty.height);
      }
   }

   private BWireSheetPane getWireSheet() {
      return (BWireSheetPane)this.getParent().getParent();
   }
}
