package com.tridium.lonworks.xml;

import java.util.Vector;

public class XUnion extends XLonData {
   public String branchElem = "";
   public Vector<XUnionBranch> branch = new Vector<>();

   public XUnion() {
      this.setName("union");
   }

   public XUnion(String be) {
      this.setName("union");
      this.branchElem = be;
   }

   @Override
   public void addAttribute(String name, Object obj) {
      if (obj instanceof XUnionBranch) {
         this.branch.addElement((XUnionBranch)obj);
      }
   }

   public XUnionBranch[] getUnionBranches() {
      XUnionBranch[] a = new XUnionBranch[this.branch.size()];
      this.branch.copyInto(a);
      return a;
   }
}
