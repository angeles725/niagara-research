package com.tridium.template.ui;

import com.tridium.wiresheet.BWireSheetPane;
import com.tridium.wiresheet.WsController;
import com.tridium.wiresheet.WsState;
import javax.baja.gx.Graphics;
import javax.baja.gx.Point;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.ui.event.BMouseWheelEvent;

public class TemplateWsState extends WsState {
   protected TemplateWsState(BWireSheetPane ws) {
      super(ws);
   }

   protected TemplateWsState(BWireSheetPane ws, WsController controller) {
      super(ws, controller);
   }

   public void keyPressed(BKeyEvent event) {
   }

   public void keyReleased(BKeyEvent event) {
   }

   public void keyTyped(BKeyEvent event) {
   }

   public void mousePressed(BMouseEvent event) {
   }

   public void mouseReleased(BMouseEvent event) {
   }

   public void mouseEntered(BMouseEvent event) {
   }

   public void mouseExited(BMouseEvent event) {
   }

   public void mouseMoved(BMouseEvent event) {
   }

   public void mouseDragged(BMouseEvent event) {
   }

   public void mousePulsed(BMouseEvent event) {
   }

   public void mouseWheel(BMouseWheelEvent event) {
   }

   public void paintFx(Graphics g) {
   }

   public Point translateToCanvasPane(BMouseEvent event) {
      return this.translateToCanvasPane(new Point(event.getX(), event.getY()));
   }

   public Point translateToCanvasPane(Point pt) {
      return this.ws.getScrollPane().translateFromChild(this.ws.getCanvas(), pt);
   }

   public boolean pulseViewport(BMouseEvent event) {
      return this.ws.controller.pulseViewport(event.getX(), event.getY());
   }

   public double snapToGrid(double pixel) {
      double d = Math.IEEEremainder(pixel, this.ws.grid.wixel);
      return d > this.ws.grid.wixel / 2 ? pixel - d + this.ws.grid.wixel : pixel - d;
   }
}
