package com.tridium.px.editor.studio.artisans.path;

import javax.baja.gx.Point;
import javax.baja.gx.IPathGeom.Segment;

public abstract class Role {
   public Point orig;
   public int idx;

   public Role(Point orig, int idx) {
      this.orig = orig;
      this.idx = idx;
   }

   public abstract Segment apply(double var1, double var3);
}
