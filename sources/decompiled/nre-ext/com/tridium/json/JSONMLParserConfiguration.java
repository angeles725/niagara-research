package com.tridium.json;

public class JSONMLParserConfiguration extends ParserConfiguration {
   public static final int DEFAULT_MAXIMUM_NESTING_DEPTH = 512;
   public static final JSONMLParserConfiguration ORIGINAL = new JSONMLParserConfiguration();
   public static final JSONMLParserConfiguration KEEP_STRINGS = new JSONMLParserConfiguration().withKeepStrings(true);

   public JSONMLParserConfiguration() {
      this.maxNestingDepth = 512;
   }

   protected JSONMLParserConfiguration(boolean keepStrings, int maxNestingDepth) {
      super(keepStrings, maxNestingDepth);
   }

   protected JSONMLParserConfiguration clone() {
      return new JSONMLParserConfiguration(this.keepStrings, this.maxNestingDepth);
   }

   public JSONMLParserConfiguration withKeepStrings(boolean newVal) {
      return super.withKeepStrings(newVal);
   }

   public JSONMLParserConfiguration withMaxNestingDepth(int maxNestingDepth) {
      return super.withMaxNestingDepth(maxNestingDepth);
   }
}
