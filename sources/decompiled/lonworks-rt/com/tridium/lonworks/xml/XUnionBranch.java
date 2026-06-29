package com.tridium.lonworks.xml;

public class XUnionBranch extends XLonData {
   public String branchName;
   public String condition;

   public XUnionBranch(String n, String c) {
      this.branchName = n;
      this.condition = c;
   }
}
