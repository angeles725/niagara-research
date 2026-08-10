package com.tridium.wiresheet.commands;

import com.tridium.wiresheet.BWireSheetPane;
import com.tridium.wiresheet.BWsOptions;
import com.tridium.wiresheet.ComponentGlyph;
import com.tridium.wiresheet.Glyph;
import javax.baja.ui.CommandArtifact;

public class MoveGlyphsCommand extends WsCommand {
   ComponentGlyph[] glyphs;
   int pDiff;
   int qDiff;
   MoveGlyphsCommand.Artifact artifact;

   public MoveGlyphsCommand(BWireSheetPane ws, ComponentGlyph[] glyphs, int pDiff, int qDiff) {
      super(ws, "ws.move");
      this.glyphs = glyphs;
      this.pDiff = pDiff;
      this.qDiff = qDiff;
      this.artifact = new MoveGlyphsCommand.Artifact();
   }

   public CommandArtifact doInvoke() throws Exception {
      this.artifact.redo();
      return this.artifact;
   }

   class Artifact implements CommandArtifact {
      public void redo() throws Exception {
         for (int i = 0; i < MoveGlyphsCommand.this.glyphs.length; i++) {
            Glyph g = MoveGlyphsCommand.this.glyphs[i];
            int wWidth = 0;
            int wHeight = 0;
            if (g instanceof ComponentGlyph) {
               ComponentGlyph comp = (ComponentGlyph)g;
               MoveGlyphsCommand.this.ws.grid.set(comp.p, comp.q, comp.ww, comp.wh, 0);
               wWidth = comp.ww;
               wHeight = comp.wh;
            }

            int p = g.p + MoveGlyphsCommand.this.pDiff;
            int q = g.q + MoveGlyphsCommand.this.qDiff;
            if (p < 0) {
               p = 0;
            }

            if (p > BWsOptions.make().getMaxWidth() - wWidth) {
               p = BWsOptions.make().getMaxWidth() - wWidth;
            }

            if (q < 0) {
               q = 0;
            }

            if (q > BWsOptions.make().getMaxHeight() - wHeight) {
               q = BWsOptions.make().getMaxHeight() - wHeight;
            }

            g.setWixelBounds(p, q, g.ww, g.wh);
         }

         MoveGlyphsCommand.this.ws.grid.clearLinks();
         MoveGlyphsCommand.this.ws.doLayout(MoveGlyphsCommand.this.ws.getChildWidgets());
         MoveGlyphsCommand.this.ws.getCanvas().getRootGlyph().layout();
         MoveGlyphsCommand.this.ws.controller.saveAnnotations(MoveGlyphsCommand.this.glyphs);
      }

      public void undo() throws Exception {
         MoveGlyphsCommand.this.pDiff = -MoveGlyphsCommand.this.pDiff;
         MoveGlyphsCommand.this.qDiff = -MoveGlyphsCommand.this.qDiff;
         this.redo();
         MoveGlyphsCommand.this.ws.grid.postLoad();
         MoveGlyphsCommand.this.pDiff = -MoveGlyphsCommand.this.pDiff;
         MoveGlyphsCommand.this.qDiff = -MoveGlyphsCommand.this.qDiff;
      }
   }
}
