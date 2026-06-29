package com.tridium.lonworks.resource;

public class TestRead {
   public static void main(String[] args) {
      try {
         ResourceFile rf = ResourceFileUtil.getResourceFile(args[0]);
         System.out.println("*** Read Complete ***");
         Fpt[] fpts = ((FptFile)rf).fpts;

         for (int i = 0; i < fpts.length; i++) {
            System.out.println(fpts[i].index + " " + fpts[i].key + " " + fpts[i].name);
         }

         if (args.length > 1 && args[1].equals("detailed")) {
            System.out.println(rf.toString());
         }
      } catch (Throwable var4) {
         System.out.println("Just couldn't do it." + var4);
         var4.printStackTrace();
      }
   }
}
