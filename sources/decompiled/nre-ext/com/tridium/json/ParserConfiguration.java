package com.tridium.json;

public class ParserConfiguration {
   public static final int UNDEFINED_MAXIMUM_NESTING_DEPTH = -1;
   public static final int DEFAULT_MAXIMUM_NESTING_DEPTH = 512;
   protected boolean keepStrings;
   protected int maxNestingDepth;

   public ParserConfiguration() {
      this.keepStrings = false;
      this.maxNestingDepth = 512;
   }

   protected ParserConfiguration(boolean keepStrings, int maxNestingDepth) {
      this.keepStrings = keepStrings;
      this.maxNestingDepth = maxNestingDepth;
   }

   protected ParserConfiguration clone() {
      return new ParserConfiguration(this.keepStrings, this.maxNestingDepth);
   }

   public boolean isKeepStrings() {
      return this.keepStrings;
   }

   public <T extends ParserConfiguration> T withKeepStrings(boolean newVal) {
      T newConfig = (T)this.clone();
      newConfig.keepStrings = newVal;
      return newConfig;
   }

   public int getMaxNestingDepth() {
      return this.maxNestingDepth;
   }

   public <T extends ParserConfiguration> T withMaxNestingDepth(int maxNestingDepth) {
      T newConfig = (T)this.clone();
      if (maxNestingDepth > -1) {
         newConfig.maxNestingDepth = maxNestingDepth;
      } else {
         newConfig.maxNestingDepth = -1;
      }

      return newConfig;
   }
}
