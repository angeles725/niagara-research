package com.prosysopc.ua.stack.encoding.binary;

import com.prosysopc.ua.stack.builtintypes.DiagnosticInfo;
import com.prosysopc.ua.stack.core.ResponseHeader;

public class DecoderUtils {
   public static void fixResponseHeader(ResponseHeader var0) {
      String[] var1 = var0.getStringTable();
      if (var1 != null) {
         DiagnosticInfo var2 = var0.getServiceDiagnostics();
         if (var2 != null) {
            a(var2, var1);
         }
      }
   }

   private static void a(DiagnosticInfo var0, String[] var1) {
      var0.setStringArray(var1);
      if (var0.getInnerDiagnosticInfo() != null) {
         a(var0.getInnerDiagnosticInfo(), var1);
      }
   }
}
