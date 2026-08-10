package com.tridium.wiresheet;

import com.tridium.util.LinkUtil;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.baja.sys.BLink;
import javax.baja.sys.BRelation;

public class LinkSnakeGlyph extends LinkLinkGlyph {
   private static final int N = 0;
   private static final int S = 1;
   private static final int E = 2;
   private static final int W = 3;
   private static final int LINK_U_TURN = -1;
   private final boolean doubleBreak = false;
   private final Woint s = new Woint();
   private final Woint t = new Woint();
   private final Woint b = new Woint();
   private final Woint r = new Woint();
   private final Woint s2 = new Woint();
   private final Woint t2 = new Woint();
   StdComponentGlyph sourceComp;
   SlotBarGlyph sourceSlot;
   public static final Logger logger = Logger.getLogger("link");
   private static int[][] board;
   private static int[] ants0P = new int[0];
   private static int[] ants0Q = new int[0];
   private static int[] ants1P = new int[0];
   private static int[] ants1Q = new int[0];
   private static int ants0Len = 0;
   private static int ants1Len = 0;

   protected LinkSnakeGlyph(BWireSheetPane ws, BLink link, StdComponentGlyph targetComp, StdComponentGlyph sourceComp) {
      super(ws, link, targetComp);
      this.sourceComp = sourceComp;
      this.sourceSlot = sourceComp.getVisibleSlotBar(link.getSourceSlotName());
   }

   protected LinkSnakeGlyph(BWireSheetPane ws, BRelation relation, StdComponentGlyph targetComp, StdComponentGlyph sourceComp) {
      super(ws, relation, targetComp, sourceComp);
      this.sourceComp = sourceComp;
      this.sourceSlot = sourceComp.getVisibleRelationBar(relation, false);
   }

   @Override
   public StdComponentGlyph getSourceComponentGlyph() {
      return this.sourceComp;
   }

   @Override
   public SlotBarGlyph getSourceSlotGlyph() {
      return this.sourceSlot;
   }

   @Override
   public String getStatusMessage() {
      if (this.isLink()) {
         String sourceText = this.sourceComp.titleBar.name + '.' + this.sourceComp.component.getDisplayName(this.sourceSlot.slot, null);
         String targetText = this.targetComp.titleBar.name + '.' + this.targetComp.component.getDisplayName(this.targetSlot.slot, null);
         return LinkUtil.linkText(sourceText, targetText, this.ws.context);
      } else {
         return LinkUtil.relationText(this.sourceComp.titleBar.name, this.relation.getRelationId(), this.targetComp.titleBar.name, this.ws.context);
      }
   }

   @Override
   public LinkGlyph.Tile[] buildTiles() {
      try {
         if (this.sourceSlot != null && this.targetSlot != null) {
            ArrayList<LinkGlyph.Tile> list = new ArrayList<>();
            this.s.p = this.sourceSlot.absP() + this.sourceSlot.ww;
            this.s.q = this.sourceSlot.absQ();
            this.t.p = this.targetSlot.absP() - 1;
            this.t.q = this.targetSlot.absQ();
            boolean routeFound = false;
            if (this.t.p > this.s.p) {
               if (this.t.q == this.s.q) {
                  routeFound = this.routeStraight(list);
               } else {
                  routeFound = this.routeLeftToRight(list);
               }
            } else if (this.t.q == this.s.q) {
               routeFound = false;
            } else {
               routeFound = this.routeAround(list);
            }

            if (!routeFound && !this.routeAntRace(list)) {
               this.routeStubs(list);
            }

            return list.toArray(new LinkGlyph.Tile[0]);
         } else {
            return new LinkGlyph.Tile[0];
         }
      } catch (Exception var3) {
         logger.severe("LinkSnakeGlyph:" + this.link);
         var3.printStackTrace();
         return new LinkGlyph.Tile[0];
      }
   }

   private void addTile(ArrayList<LinkGlyph.Tile> list, int p, int q, int mask) {
      list.add(new LinkGlyph.Tile(p, q, mask, this.ws));
      LinkSnakeGlyph.Scale sh = new LinkSnakeGlyph.Scale(p, q, this.getSourceSlotGlyph(), this.getTargetSlotGlyph(), mask);
      this.ws.getCanvas().addScale(p, q, sh);
   }

   private boolean routeStraight(ArrayList<LinkGlyph.Tile> list) {
      for (int i = this.s.p + 1; i < this.t.p; i++) {
         if (!this.usableWixel(i, this.s.q, 1)) {
            return false;
         }
      }

      for (int ix = this.s.p; ix <= this.t.p; ix++) {
         this.addTile(list, ix, this.s.q, 1);
      }

      return true;
   }

   private boolean routeAround(ArrayList<LinkGlyph.Tile> list) {
      this.s2.p = this.s.p + 1;
      this.s2.q = this.s.q;
      this.t2.p = this.t.p - 1;
      this.t2.q = this.t.q;
      int pMin = Math.min(this.s2.p, this.t2.p);
      int pMax = Math.max(this.s2.p, this.t2.p);
      int qMin = Math.min(this.s2.q, this.t2.q);
      int qMax = Math.max(this.s2.q, this.t2.q);
      int n = qMin + (qMax - qMin) / 2;
      this.b.p = this.s2.p;
      this.b.q = n;
      this.r.p = this.t2.p;
      this.r.q = n;
      if (this.isAroundValid(list)) {
         this.drawAround(list);
         return true;
      } else {
         for (int nn = 1; n - nn >= this.s.q || n + nn <= this.t.q; nn++) {
            if (n - nn >= this.s.q) {
               this.b.q = n - nn;
               this.r.q = n - nn;
               if (this.isAroundValid(list)) {
                  this.drawAround(list);
                  return true;
               }
            }

            if (n + nn <= this.t.q) {
               this.b.q = n + nn;
               this.r.q = n + nn;
               if (this.isAroundValid(list)) {
                  this.drawAround(list);
                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean isAroundValid(ArrayList<LinkGlyph.Tile> list) {
      if (!this.usableWixel(this.s2.p, this.s2.q, this.s.q < this.t.q ? 8 : 7)) {
         return false;
      } else {
         if (this.s.q < this.t.q) {
            for (int i = this.s2.q + 1; i < this.b.q; i++) {
               if (!this.usableWixel(this.s2.p, i, 2)) {
                  return false;
               }
            }
         } else {
            for (int ix = this.b.q + 1; ix < this.s2.q; ix++) {
               if (!this.usableWixel(this.s2.p, ix, 2)) {
                  return false;
               }
            }
         }

         if (!this.usableWixel(this.b.p, this.b.q, this.s.q < this.t.q ? 7 : 8)) {
            return false;
         } else {
            for (int ixx = this.r.p + 1; ixx < this.b.p; ixx++) {
               if (!this.usableWixel(ixx, this.r.q, 1)) {
                  return false;
               }
            }

            if (!this.usableWixel(this.r.p, this.r.q, this.s.q < this.t.q ? 10 : 9)) {
               return false;
            } else {
               if (this.s.q < this.t.q) {
                  for (int ixxx = this.r.q + 1; ixxx < this.t2.q; ixxx++) {
                     if (!this.usableWixel(this.r.p, ixxx, 2)) {
                        return false;
                     }
                  }
               } else {
                  for (int ixxxx = this.t2.q + 1; ixxxx < this.r.q; ixxxx++) {
                     if (!this.usableWixel(this.r.p, ixxxx, 2)) {
                        return false;
                     }
                  }
               }

               return this.usableWixel(this.t2.p, this.t2.q, this.s.q < this.t.q ? 9 : 10);
            }
         }
      }
   }

   private boolean routeLeftToRight(ArrayList<LinkGlyph.Tile> list) {
      int pMin = Math.min(this.s.p, this.t.p);
      int pMax = Math.max(this.s.p, this.t.p);
      int qMin = Math.min(this.s.q, this.t.q);
      int qMax = Math.max(this.s.q, this.t.q);
      int n = pMin + (pMax - pMin) / 2;
      this.b.p = n;
      this.b.q = this.s.q;
      this.r.p = n;
      this.r.q = this.t.q;
      if (this.isLeftToRightValid(qMin, qMax)) {
         this.drawLeftToRight(list, qMin, qMax);
         return true;
      } else {
         for (int nn = 1; n - nn >= this.s.p || n + nn <= this.t.p; nn++) {
            if (n - nn >= this.s.p) {
               this.b.p = n - nn;
               this.r.p = n - nn;
               if (this.isLeftToRightValid(qMin, qMax)) {
                  this.drawLeftToRight(list, qMin, qMax);
                  return true;
               }
            }

            if (n + nn <= this.t.p) {
               this.b.p = n + nn;
               this.r.p = n + nn;
               if (this.isLeftToRightValid(qMin, qMax)) {
                  this.drawLeftToRight(list, qMin, qMax);
                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean isLeftToRightValid(int qMin, int qMax) {
      for (int i = this.s.p + 1; i < this.b.p; i++) {
         if (!this.usableWixel(i, this.s.q, 1)) {
            return false;
         }
      }

      if (!this.usableWixel(this.b.p, this.b.q, this.r.q < this.b.q ? 7 : 8)) {
         return false;
      } else {
         for (int ix = qMin + 1; ix < qMax; ix++) {
            if (!this.usableWixel(this.b.p, ix, 2)) {
               return false;
            }
         }

         if (!this.usableWixel(this.r.p, this.r.q, this.r.q < this.b.q ? 10 : 9)) {
            return false;
         } else {
            for (int ixx = this.r.p + 1; ixx < this.t.p; ixx++) {
               if (!this.usableWixel(ixx, this.t.q, 1)) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   private boolean usableWixel(int p, int q, int mask) {
      if (p < 0 || p >= this.ws.grid.getWixelWidth()) {
         return false;
      } else if (q < 0 || q >= this.ws.grid.getWixelHeight()) {
         return false;
      } else {
         return this.hasComponents(p, q) ? false : this.canCross(p, q, mask);
      }
   }

   private boolean hasComponents(int p, int q) {
      return this.ws.grid.isComponent(p, q);
   }

   private boolean canCross(int p, int q, int mask) {
      ArrayList<LinkSnakeGlyph.Scale> list = this.ws.getCanvas().getScales(p, q);
      if (list == null) {
         return true;
      } else {
         for (int i = 0; i < list.size(); i++) {
            LinkSnakeGlyph.Scale sh = list.get(i);
            if (sh.source != this.getSourceSlotGlyph() && sh.target != this.getTargetSlotGlyph() && (mask != 1 || sh.type != 2) && (mask != 2 || sh.type != 1)) {
               return false;
            }
         }

         return true;
      }
   }

   private void drawLeftToRight(ArrayList<LinkGlyph.Tile> list, int qMin, int qMax) {
      for (int i = this.s.p; i < this.b.p; i++) {
         this.addTile(list, i, this.s.q, 1);
      }

      this.addTile(list, this.b.p, this.b.q, this.r.q < this.b.q ? 7 : 8);

      for (int i = qMin + 1; i < qMax; i++) {
         this.addTile(list, this.b.p, i, 2);
      }

      this.addTile(list, this.r.p, this.r.q, this.r.q < this.b.q ? 10 : 9);

      for (int i = this.r.p + 1; i <= this.t.p; i++) {
         this.addTile(list, i, this.t.q, 1);
      }
   }

   private void drawAround(ArrayList<LinkGlyph.Tile> list) {
      this.addTile(list, this.s.p, this.s.q, 1);
      this.addTile(list, this.s2.p, this.s2.q, this.s.q < this.t.q ? 8 : 7);
      if (this.s.q < this.t.q) {
         for (int i = this.s2.q + 1; i < this.b.q; i++) {
            this.addTile(list, this.s2.p, i, 2);
         }
      } else {
         for (int i = this.b.q + 1; i < this.s2.q; i++) {
            this.addTile(list, this.s2.p, i, 2);
         }
      }

      this.addTile(list, this.b.p, this.b.q, this.s.q < this.t.q ? 7 : 8);

      for (int i = this.r.p + 1; i < this.b.p; i++) {
         this.addTile(list, i, this.r.q, 1);
      }

      this.addTile(list, this.r.p, this.r.q, this.s.q < this.t.q ? 10 : 9);
      if (this.s.q < this.t.q) {
         for (int i = this.r.q + 1; i < this.t2.q; i++) {
            this.addTile(list, this.r.p, i, 2);
         }
      } else {
         for (int i = this.t2.q + 1; i < this.r.q; i++) {
            this.addTile(list, this.r.p, i, 2);
         }
      }

      this.addTile(list, this.t2.p, this.t2.q, this.s.q < this.t.q ? 9 : 10);
      this.addTile(list, this.t.p, this.t.q, 1);
   }

   private void routeStubs(ArrayList<LinkGlyph.Tile> list) {
      this.addTile(list, this.s.p, this.s.q, 4);
      this.addTile(list, this.t.p, this.t.q, 3);
   }

   private void routeFailed() {
      String sourceShortType = this.sourceComp.component.getType().toString();
      String targetShortType = this.targetComp.component.getType().toString();
      sourceShortType = sourceShortType.substring(sourceShortType.lastIndexOf(58) + 1, sourceShortType.length());
      targetShortType = targetShortType.substring(targetShortType.lastIndexOf(58) + 1, targetShortType.length());
      if (this.link != null) {
         logger.fine(
            "Failed to draw link between "
               + sourceShortType
               + ":"
               + this.sourceComp.component.getNavDisplayName(null)
               + ":"
               + this.link.getSourceSlotName()
               + " and "
               + targetShortType
               + ":"
               + this.targetComp.component.getNavDisplayName(null)
               + ":"
               + this.link.getTargetSlotName()
               + "."
         );
      } else {
         logger.fine(
            "Failed to draw relation between "
               + sourceShortType
               + ":"
               + this.sourceComp.component.getNavDisplayName(null)
               + ":"
               + this.relation.getRelationId()
               + " and "
               + targetShortType
               + ":"
               + this.targetComp.component.getNavDisplayName(null)
         );
      }
   }

   private boolean routeAntRace(ArrayList<LinkGlyph.Tile> list) {
      int p = this.s.p + 1;
      int q = this.s.q;
      if (p >= 0 && p < this.ws.grid.getWixelWidth() && q >= 0 && q < this.ws.grid.getWixelHeight() && !this.hasComponents(p, q)) {
         this.initBoard();
         board[this.s.p][this.s.q] = 1;
         int time = 2;
         ants0Len = 0;
         ants1Len = 0;
         ants0Len++;
         ants0P = ensureCapacity(ants0P, ants0Len);
         ants0Q = ensureCapacity(ants0Q, ants0Len);
         ants0P[ants0Len - 1] = this.s.p + 1;

         for (ants0Q[ants0Len - 1] = this.s.q; ants0Len > 0; ants1Len = 0) {
            for (int i = 0; i < ants0Len; i++) {
               int antP = ants0P[i];
               int antQ = ants0Q[i];
               if (board[antP][antQ] == 0) {
                  board[antP][antQ] = time;
                  if (antP == this.t.p - 1 && antQ == this.t.q) {
                     return this.buildAntRoad(list);
                  }

                  this.spawnAnts(antP, antQ);
               }
            }

            time++;
            int[] tempP = ants0P;
            int[] tempQ = ants0Q;
            ants0P = ants1P;
            ants0Q = ants1Q;
            ants0Len = ants1Len;
            ants1P = tempP;
            ants1Q = tempQ;
         }

         this.routeFailed();
         return false;
      } else {
         this.routeFailed();
         return false;
      }
   }

   private void initBoard() {
      board = new int[this.ws.grid.getWixelWidth()][this.ws.grid.getWixelHeight()];

      for (int i = 0; i < this.ws.grid.getWixelWidth(); i++) {
         for (int j = 0; j < this.ws.grid.getWixelHeight(); j++) {
            board[i][j] = 0;
         }
      }
   }

   private void spawnAnts(int antP, int antQ) {
      if (this.usableWixel(antP, antQ - 1, 2)) {
         this.newAnt(antP, antQ - 1);
      }

      if (this.usableWixel(antP, antQ + 1, 2)) {
         this.newAnt(antP, antQ + 1);
      }

      if (this.usableWixel(antP - 1, antQ, 1)) {
         this.newAnt(antP - 1, antQ);
      }

      if (this.usableWixel(antP + 1, antQ, 1)) {
         this.newAnt(antP + 1, antQ);
      }
   }

   private void newAnt(int antP, int antQ) {
      ants1Len++;
      ants1P = ensureCapacity(ants1P, ants1Len);
      ants1Q = ensureCapacity(ants1Q, ants1Len);
      ants1P[ants1Len - 1] = antP;
      ants1Q[ants1Len - 1] = antQ;
   }

   private static int[] ensureCapacity(int[] x, int len) {
      if (len < x.length) {
         return x;
      } else {
         int[] expand = new int[x.length == 0 ? 1 : x.length * 2];
         if (x.length > 0) {
            System.arraycopy(x, 0, expand, 0, x.length);
         }

         return expand;
      }
   }

   private boolean buildAntRoad(ArrayList<LinkGlyph.Tile> list) {
      ArrayList<LinkGlyph.Tile> localList = new ArrayList<>();
      int lastP = this.t.p;
      int lastQ = this.t.q;
      localList.add(new LinkGlyph.Tile(lastP, lastQ, 1, this.ws));
      int thisP = this.t.p - 1;
      int thisQ = this.t.q;

      for (int time = board[thisP][thisQ]; thisP != this.s.p || thisQ != this.s.q; time--) {
         int nextP = thisP;
         int nextQ = thisQ;
         if (this.availableCell(time - 1, lastP, lastQ, thisP, thisQ, thisP, thisQ + 1)) {
            nextQ = thisQ + 1;
         } else if (this.availableCell(time - 1, lastP, lastQ, thisP, thisQ, thisP - 1, thisQ)) {
            nextP = thisP - 1;
         } else if (this.availableCell(time - 1, lastP, lastQ, thisP, thisQ, thisP + 1, thisQ)) {
            nextP = thisP + 1;
         } else {
            if (!this.availableCell(time - 1, lastP, lastQ, thisP, thisQ, thisP, thisQ - 1)) {
               this.routeFailed();
               return false;
            }

            nextQ = thisQ - 1;
         }

         int tile = this.getRoadTile(lastP, lastQ, thisP, thisQ, nextP, nextQ);
         localList.add(new LinkGlyph.Tile(thisP, thisQ, tile, this.ws));
         lastP = thisP;
         lastQ = thisQ;
         thisP = nextP;
         thisQ = nextQ;
      }

      localList.add(new LinkGlyph.Tile(this.s.p, this.s.q, 1, this.ws));

      for (int i = 0; i < localList.size(); i++) {
         LinkGlyph.Tile tile = localList.get(i);
         this.addTile(list, tile.p, tile.q, tile.type);
      }

      return true;
   }

   private boolean availableCell(int time, int lastP, int lastQ, int thisP, int thisQ, int nextP, int nextQ) {
      if (nextP < 0 || nextP >= this.ws.grid.getWixelWidth()) {
         return false;
      } else if (nextQ >= 0 && nextQ < this.ws.grid.getWixelHeight()) {
         if (board[nextP][nextQ] != time) {
            return false;
         } else {
            int tile = this.getRoadTile(lastP, lastQ, thisP, thisQ, nextP, nextQ);
            return tile == -1 ? false : this.canCross(thisP, thisQ, tile);
         }
      } else {
         return false;
      }
   }

   private int getRoadTile(int lastP, int lastQ, int thisP, int thisQ, int nextP, int nextQ) {
      if (lastP == nextP && lastQ == nextQ) {
         return -1;
      } else if (Math.abs(lastQ - nextQ) == 2) {
         return 2;
      } else if (Math.abs(lastP - nextP) == 2) {
         return 1;
      } else {
         switch (this.checkCompass(thisP, thisQ, lastP, lastQ)) {
            case 0:
               switch (this.checkCompass(thisP, thisQ, nextP, nextQ)) {
                  case 2:
                     return 9;
                  case 3:
                     return 7;
                  default:
                     throw new IllegalStateException();
               }
            case 1:
               switch (this.checkCompass(thisP, thisQ, nextP, nextQ)) {
                  case 2:
                     return 10;
                  case 3:
                     return 8;
                  default:
                     throw new IllegalStateException();
               }
            case 2:
               switch (this.checkCompass(thisP, thisQ, nextP, nextQ)) {
                  case 0:
                     return 9;
                  case 1:
                     return 10;
                  default:
                     throw new IllegalStateException();
               }
            case 3:
               switch (this.checkCompass(thisP, thisQ, nextP, nextQ)) {
                  case 0:
                     return 7;
                  case 1:
                     return 8;
                  default:
                     throw new IllegalStateException();
               }
            default:
               throw new IllegalStateException();
         }
      }
   }

   private int checkCompass(int aP, int aQ, int bP, int bQ) {
      if (bP < aP) {
         return 3;
      } else if (bP > aP) {
         return 2;
      } else if (bQ < aQ) {
         return 0;
      } else if (bQ > aQ) {
         return 1;
      } else {
         throw new IllegalStateException();
      }
   }

   protected void replaceTilesAt(int p, int q, int type) {
      ArrayList<LinkGlyph.Tile> list = new ArrayList<>();

      for (int i = 0; i < this.tiles.length; i++) {
         if (this.tiles[i].p != p || this.tiles[i].q != q) {
            list.add(this.tiles[i]);
         }
      }

      list.add(new LinkGlyph.Tile(p, q, type, this.ws));
      this.tiles = list.toArray(new LinkGlyph.Tile[0]);
   }

   public class Scale {
      int p;
      int q;
      SlotBarGlyph source;
      SlotBarGlyph target;
      int type;

      Scale(int p, int q, SlotBarGlyph source, SlotBarGlyph target, int type) {
         this.p = p;
         this.q = q;
         this.source = source;
         this.target = target;
         this.type = type;
      }

      LinkSnakeGlyph getOwner() {
         return LinkSnakeGlyph.this;
      }
   }
}
