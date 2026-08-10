package com.tridium.wiresheet.states;

import com.tridium.wiresheet.BWireSheetPane;
import com.tridium.wiresheet.BWsCanvas;
import com.tridium.wiresheet.Glyph;
import com.tridium.wiresheet.RelationBarGlyph;
import com.tridium.wiresheet.RelationFooterBarGlyph;
import com.tridium.wiresheet.RelationKnobBarGlyph;
import com.tridium.wiresheet.Woint;
import com.tridium.wiresheet.WsState;
import javax.baja.gx.BColor;
import javax.baja.gx.Graphics;
import javax.baja.sys.BComponent;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.LinkCheck;
import javax.baja.sys.Slot;
import javax.baja.ui.event.BKeyEvent;
import javax.baja.ui.event.BMouseEvent;
import javax.baja.workbench.commands.LinkCommand;
import javax.baja.workbench.commands.RelateCommand;

public class LinkState extends WsState {
   static Context context = new BasicContext();
   private Glyph anchorGlyph;
   private int anchorZone;
   private Woint anchor;
   private double ax;
   private double ay;
   private double cx;
   private double cy;
   private Glyph linkOver;
   private boolean linkValid;
   private BComponent anchorComponent;
   private Slot anchorSlot;
   private boolean relationMode;

   public LinkState(BWireSheetPane ws, Glyph glyph, int anchorZone, double mx, double my) {
      super(ws);
      this.anchorGlyph = glyph;
      this.anchorZone = anchorZone;
      this.ax = anchorZone == 2 ? glyph.absX() + 2 : glyph.absX() + glyph.pw - 2;
      this.ay = glyph.absY() + glyph.ph / 2;
      this.anchorComponent = glyph.getLinkComponent();
      this.anchorSlot = glyph.getLinkSlot();
      this.cx = mx;
      this.cy = my;
      this.relationMode = glyph instanceof RelationFooterBarGlyph || glyph instanceof RelationKnobBarGlyph || glyph instanceof RelationBarGlyph;
      BWsCanvas canvas = ws.getCanvas();
      canvas.setMouseCursor(grabCursor);
      canvas.repaint();
   }

   @Override
   public void mouseReleased(BMouseEvent event) {
      this.anchorGlyph.setLinkOver(0);
      if (this.linkOver != null) {
         this.linkOver.setLinkOver(0);
      }

      if (!event.isShiftDown()) {
         this.transition(new NormalState(this.ws));
      }

      if (this.linkOver != null && this.linkValid) {
         String sId = null;
         if (this.anchorGlyph instanceof RelationBarGlyph) {
            sId = ((RelationBarGlyph)this.anchorGlyph).getRelationId();
         } else if (this.anchorGlyph instanceof RelationKnobBarGlyph) {
            sId = ((RelationKnobBarGlyph)this.anchorGlyph).getRelationId();
         }

         BComponent s;
         BComponent t;
         Slot ss;
         Slot ts;
         if (this.anchorZone == 1) {
            s = this.anchorComponent;
            ss = this.anchorSlot;
            t = this.linkOver.getLinkComponent();
            ts = this.linkOver.getLinkSlot();
            if (sId == null && this.linkOver instanceof RelationKnobBarGlyph) {
               sId = ((RelationKnobBarGlyph)this.linkOver).getRelationId();
            }
         } else {
            t = this.anchorComponent;
            ts = this.anchorSlot;
            if (sId == null && this.linkOver instanceof RelationBarGlyph) {
               sId = ((RelationBarGlyph)this.linkOver).getRelationId();
            }

            s = this.linkOver.getLinkComponent();
            ss = this.linkOver.getLinkSlot();
         }

         if (ss == null || ts == null) {
            this.transition(new NormalState(this.ws));
         }

         if (this.relationMode) {
            new RelateCommand(this.ws.getShell(), s, t, sId).invoke();
         } else {
            new LinkCommand(this.ws.getShell(), s, ss, t, ts).invoke();
         }
      }
   }

   @Override
   public void mouseDragged(BMouseEvent event) {
      this.mouseMoveOp(event);
   }

   @Override
   public void mouseMoved(BMouseEvent event) {
      this.mouseMoveOp(event);
   }

   private void mouseMoveOp(BMouseEvent event) {
      double mx = event.getX();
      double my = event.getY();
      int p = (int)(mx / this.ws.grid.wixel);
      int q = (int)(my / this.ws.grid.wixel);
      BWsCanvas canvas = this.ws.getCanvas();
      Glyph over = canvas.getRootGlyph().descendentAt(p, q);
      this.linkValid = false;
      int zone = 0;
      String status = null;
      if (over != null) {
         boolean isOverRelation = over instanceof RelationFooterBarGlyph || over instanceof RelationKnobBarGlyph || over instanceof RelationBarGlyph;
         zone = over.getLinkHotSpot(p - over.absP(), q - over.absQ());
         if (this.relationMode && !isOverRelation) {
            status = "Cannot relate to a slot";
            zone = 0;
         }

         if (this.relationMode && isOverRelation && this.anchorGlyph.getLinkComponent().equals(over.getLinkComponent())) {
            status = "A Component can not be relatied to itself";
            zone = 0;
         }

         if (zone == 0) {
            over = null;
         } else if (zone == this.anchorZone) {
            if (zone == 1) {
               status = this.getInvalidStatus(over, "Source to source");
            } else {
               status = this.getInvalidStatus(over, "Target to target");
            }
         } else {
            LinkCheck check = this.checkLink(over.getLinkComponent(), over.getLinkSlot());
            if (check.isValid()) {
               if (this.relationMode) {
                  String relationId = "*";
                  if (this.anchorGlyph instanceof RelationKnobBarGlyph) {
                     relationId = ((RelationKnobBarGlyph)this.anchorGlyph).getRelationId();
                  } else if (this.anchorGlyph instanceof RelationBarGlyph) {
                     relationId = ((RelationBarGlyph)this.anchorGlyph).getRelationId();
                  } else if (over instanceof RelationKnobBarGlyph) {
                     relationId = ((RelationKnobBarGlyph)over).getRelationId();
                  } else if (over instanceof RelationBarGlyph) {
                     relationId = ((RelationBarGlyph)over).getRelationId();
                  }

                  status = this.getValidStatus(over.getLinkComponent(), relationId);
               } else {
                  status = this.getValidStatus(over.getLinkComponent(), over.getLinkSlot());
               }

               this.linkValid = true;
            } else {
               status = this.getInvalidStatus(over, check.getInvalidReason());
            }
         }
      }

      this.ws.getShell().showStatus(status);
      if (over != this.linkOver) {
         if (this.linkOver != null && this.linkOver != this.anchorGlyph) {
            this.linkOver.setLinkOver(0);
         }

         this.linkOver = over;
         if (over != null && this.linkOver != this.anchorGlyph) {
            over.setLinkOver(zone);
         }
      }

      if (this.linkValid) {
         if (zone == 1) {
            canvas.setMouseCursor(linkTargetCursor);
         } else {
            canvas.setMouseCursor(linkSourceCursor);
         }
      } else if (zone == 0) {
         canvas.setMouseCursor(grabCursor);
      } else {
         canvas.setMouseCursor(linkInvalidCursor);
      }

      this.cx = mx;
      this.cy = my;
      canvas.repaint();
   }

   @Override
   public void mousePulsed(BMouseEvent event) {
      if (this.pulseViewport(event)) {
         this.mouseDragged(event);
      }
   }

   @Override
   public void keyReleased(BKeyEvent event) {
      if (event.getKeyCode() != 16) {
         this.transition(new NormalState(this.ws));
      }
   }

   public LinkCheck checkLink(BComponent cur, Slot curSlot) {
      BComponent s;
      BComponent t;
      Slot ss;
      Slot ts;
      if (this.anchorZone == 1) {
         s = this.anchorComponent;
         ss = this.anchorSlot;
         t = cur;
         ts = curSlot;
      } else {
         t = this.anchorComponent;
         ts = this.anchorSlot;
         s = cur;
         ss = curSlot;
      }

      return ss != null && ts != null ? t.checkLink(s, ss, ts, context) : LinkCheck.makeValid();
   }

   public String getInvalidStatus(Glyph over, String msg) {
      if (this.relationMode) {
         return "Relate to " + over.getLinkComponent().getName() + " invalid: " + msg;
      } else {
         Slot slot = over.getLinkSlot();
         String name = slot == null ? "*" : slot.getName();
         return "Link to \"" + name + "\" invalid: " + msg;
      }
   }

   public String getValidStatus(BComponent cur, Slot curSlot) {
      BComponent s;
      BComponent t;
      Slot ss;
      Slot ts;
      if (this.anchorZone == 1) {
         s = this.anchorComponent;
         ss = this.anchorSlot;
         t = cur;
         ts = curSlot;
      } else {
         t = this.anchorComponent;
         ts = this.anchorSlot;
         s = cur;
         ss = curSlot;
      }

      String ssStr = ss == null ? "*" : ss.getName();
      String tsStr = ts == null ? "*" : ts.getName();
      return "Link: \"" + s.getName() + "." + ssStr + "\" -> \"" + t.getName() + "." + tsStr + "\"";
   }

   public String getValidStatus(BComponent cur, String relationId) {
      BComponent s;
      BComponent t;
      if (this.anchorZone == 1) {
         s = this.anchorComponent;
         t = cur;
      } else {
         t = this.anchorComponent;
         s = cur;
      }

      return "Relate: \"" + s.getName() + "\" <" + relationId + "> \"" + t.getName() + "\"";
   }

   @Override
   public void paintFx(Graphics g) {
      g.setBrush(BColor.black);
      g.strokeLine(this.ax, this.ay, this.cx, this.cy);
   }
}
