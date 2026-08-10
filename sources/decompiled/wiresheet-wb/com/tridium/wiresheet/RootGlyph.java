package com.tridium.wiresheet;

import com.tridium.ui.theme.Theme;
import com.tridium.ui.theme.WiresheetTheme;
import javax.baja.gx.Graphics;
import javax.baja.gx.RectGeom;

public class RootGlyph extends Glyph {
   public final LayerGlyph componentLayer;
   public final LinkLayerGlyph linkLayer;

   RootGlyph(BWireSheetPane ws) {
      super(ws);
      this.add(this.componentLayer = new LayerGlyph(ws));
      this.add(this.linkLayer = new LinkLayerGlyph(ws));
   }

   public Glyph getSelectableGlyphAt(int p, int q) {
      Glyph glyph = this.componentLayer.getSelectableGlyphAt(p, q);
      if (glyph == null) {
         glyph = this.linkLayer.getSelectableGlyphAt(p, q);
      }

      return glyph;
   }

   @Override
   public Glyph descendentAt(int p, int q) {
      Glyph glyph = this.componentLayer.descendentAt(p, q);
      if (glyph == null) {
         glyph = this.linkLayer.descendentAt(p, q);
      }

      return glyph;
   }

   public void selectAllInPixelRect(RectGeom r) {
      this.componentLayer.selectAllInPixelRect(r);
      this.linkLayer.selectAllInPixelRect(r);
   }

   @Override
   public void removeAll() {
      this.componentLayer.removeAll();
      this.linkLayer.removeAll();
   }

   @Override
   public RectGeom getPrintBounds() {
      RectGeom r = new RectGeom(0.0, 0.0, 0.0, 0.0);
      RectGeom.bounds(r, this.componentLayer.getPrintBounds(), r);
      RectGeom.bounds(r, this.linkLayer.getPrintBounds(), r);
      return r;
   }

   @Override
   public void paint(Graphics g) {
      BWsCanvas canvas = this.ws.getCanvas();
      WiresheetTheme theme = Theme.wiresheet();
      g.setBrush(theme.canvas().getBackground(this));
      g.fillRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());
      BWsOptions options = BWsOptions.make();
      double maxWidth = options.getMaxWidth() * this.ws.grid.wixel;
      double maxHeight = options.getMaxHeight() * this.ws.grid.wixel;
      if (options.getShowGrid()) {
         double width = Math.min(canvas.getWidth(), maxWidth);
         double height = Math.min(canvas.getHeight(), maxHeight);
         g.setBrush(theme.canvas().getGridColor(this));

         for (int x = this.ws.grid.wixel; x < width; x += this.ws.grid.wixel) {
            g.strokeLine(x, 0.0, x, height);
         }

         for (int y = this.ws.grid.wixel; y < height; y += this.ws.grid.wixel) {
            g.strokeLine(0.0, y, width, y);
         }
      }

      g.setBrush(theme.canvas().getOutline(this));
      g.strokeLine(0.0, maxHeight, maxWidth, maxHeight);
      g.strokeLine(maxWidth, 0.0, maxWidth, maxHeight);
      this.linkLayer.paint(g);
      this.componentLayer.paint(g);
   }

   @Override
   public String getStyleSelector() {
      return "wire-sheet";
   }
}
