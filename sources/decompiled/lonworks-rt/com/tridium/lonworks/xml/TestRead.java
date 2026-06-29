package com.tridium.lonworks.xml;

import java.io.File;

public class TestRead {
   public static void main(String[] args) {
      File inFile = new File(args[0]);
      File outFile = new File(args[0] + "_copy");
      boolean defaults = args.length > 1 && args[1].indexOf(100) >= 0;

      try {
         Object root = LonXMLReader.decode(inFile);
         System.out.println("*** Read Complete ***");
         LonXMLWriter writer = new LonXMLWriter(outFile, defaults);
         writer.encode(root);
         System.out.println("*** Write Complete ***");
      } catch (Throwable var6) {
         System.out.println("Just couldn't do it." + var6);
         var6.printStackTrace();
      }
   }
}
