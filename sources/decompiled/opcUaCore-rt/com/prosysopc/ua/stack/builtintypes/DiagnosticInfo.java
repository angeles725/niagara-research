package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.stack.core.Identifiers;
import java.util.List;
import java.util.Objects;

public class DiagnosticInfo {
   public static boolean disableAdditionalInfo = true;
   @Deprecated
   public static final NodeId ID = Identifiers.DiagnosticInfo;
   public static final DiagnosticInfo[] EMPTY_ARRAY = new DiagnosticInfo[0];
   Integer rM;
   Integer rN;
   Integer rO;
   Integer rP;
   String additionalInfo;
   StatusCode rQ;
   DiagnosticInfo rR;
   List<String> rS;
   String[] rT;

   public static void toString(DiagnosticInfo var0, StringBuilder var1, boolean var2, boolean var3, boolean var4) {
      var1.append(var4 ? "Inner Info: " : "Diagnostic Info: ");
      if (!var2 && var0.getLocalizedTextStr() != null) {
         var1.append(var0.getLocalizedTextStr());
         var1.append(' ');
      }

      if (!var3 && var0.getInnerStatusCode() != null) {
         var1.append("(");
         var1.append(var0.getInnerStatusCode().toString());
         var1.append(")");
      }

      var1.append('\n');
      if (var0.getAdditionalInfo() != null) {
         var1.append('\t');
         var1.append(var0.getAdditionalInfo());
         var1.append('\n');
      }

      if (var0.getSymbolicIdStr() != null) {
         var1.append("\tSymbolicId: " + var0.getSymbolicIdStr() + "\n");
      }

      if (var0.getNamespaceUriStr() != null) {
         var1.append("\tNamespaceUri: " + var0.getNamespaceUriStr() + "\n");
      }

      DiagnosticInfo var5 = var0.getInnerDiagnosticInfo();
      if (var5 != null) {
         toString(var5, var1, false, false, true);
      }
   }

   public DiagnosticInfo() {
   }

   public DiagnosticInfo(String var1, DiagnosticInfo var2, StatusCode var3, Integer var4, Integer var5, Integer var6, Integer var7) {
      this.setAdditionalInfo(var1);
      this.rR = var2;
      this.rQ = var3;
      this.rP = var4;
      this.rO = var5;
      this.rN = var6;
      this.rM = var7;
   }

   public DiagnosticInfo(String var1, DiagnosticInfo var2, StatusCode var3, String var4, String var5, String var6, String var7, List<String> var8) {
      this.setAdditionalInfo(var1);
      this.rR = var2;
      this.rQ = var3;
      this.rS = var8;
      this.rP = this.addOrGetIndex(var4);
      this.rO = this.addOrGetIndex(var5);
      this.rN = this.addOrGetIndex(var6);
      this.rM = this.addOrGetIndex(var7);
   }

   @Override
   public boolean equals(Object var1) {
      if (!(var1 instanceof DiagnosticInfo)) {
         return false;
      } else {
         DiagnosticInfo var2 = (DiagnosticInfo)var1;
         return Objects.equals(var2.rM, this.rM)
            && Objects.equals(var2.rN, this.rN)
            && Objects.equals(var2.rO, this.rO)
            && Objects.equals(var2.rP, this.rP)
            && Objects.equals(var2.additionalInfo, this.additionalInfo)
            && Objects.equals(var2.rQ, this.rQ)
            && Objects.equals(var2.rR, this.rR);
      }
   }

   public String getAdditionalInfo() {
      return this.additionalInfo;
   }

   public DiagnosticInfo getInnerDiagnosticInfo() {
      return this.rR;
   }

   public StatusCode getInnerStatusCode() {
      return this.rQ;
   }

   public Integer getLocale() {
      return this.rP;
   }

   public String getLocaleStr() {
      if (this.rP == null) {
         return null;
      } else if (this.rT != null) {
         return this.rT[this.rP];
      } else {
         return this.rS != null ? this.rS.get(this.rP) : this.rP.toString();
      }
   }

   public Integer getLocalizedText() {
      return this.rO;
   }

   public String getLocalizedTextStr() {
      if (this.rO == null) {
         return null;
      } else if (this.rT != null) {
         return this.rT[this.rO];
      } else {
         return this.rS != null ? this.rS.get(this.rO) : this.rO.toString();
      }
   }

   public Integer getNamespaceUri() {
      return this.rN;
   }

   public String getNamespaceUriStr() {
      if (this.rN == null) {
         return null;
      } else if (this.rT != null) {
         return this.rT[this.rN];
      } else {
         return this.rS != null ? this.rS.get(this.rN) : this.rN.toString();
      }
   }

   public List<String> getStringTable() {
      return this.rS;
   }

   public Integer getSymbolicId() {
      return this.rM;
   }

   public String getSymbolicIdStr() {
      if (this.rM == null) {
         return null;
      } else if (this.rT != null) {
         return this.rT[this.rM];
      } else {
         return this.rS != null ? this.rS.get(this.rM) : this.rM.toString();
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.rM, this.rN, this.rO, this.rP, this.additionalInfo, this.rR, this.rQ);
   }

   public void setAdditionalInfo(String var1) {
      if (disableAdditionalInfo) {
         this.additionalInfo = null;
      } else {
         this.additionalInfo = var1;
      }
   }

   public void setInnerDiagnosticInfo(DiagnosticInfo var1) {
      this.rR = var1;
   }

   public void setInnerStatusCode(StatusCode var1) {
      this.rQ = var1;
   }

   public void setLocale(Integer var1) {
      this.rP = var1;
   }

   public void setLocaleStr(String var1) {
      this.rP = this.addOrGetIndex(var1);
   }

   public void setLocalizedText(Integer var1) {
      this.rO = var1;
   }

   public void setLocalizedTextStr(String var1) {
      this.rO = this.addOrGetIndex(var1);
   }

   public void setNamespaceUri(Integer var1) {
      this.rN = var1;
   }

   public void setNamespaceUriStr(String var1) {
      this.rN = this.addOrGetIndex(var1);
   }

   public void setStringArray(String[] var1) {
      this.rS = null;
      this.rT = var1;
   }

   public void setStringTable(List<String> var1) {
      this.rS = var1;
      this.rT = null;
   }

   public void setSymbolicId(Integer var1) {
      this.rM = var1;
   }

   public void setSymbolicIdStr(String var1) {
      this.rM = this.addOrGetIndex(var1);
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder();
      toString(this, var1, false, false, false);
      return var1.toString();
   }

   private int addOrGetIndex(String var1) {
      int var2 = this.rS.indexOf(var1);
      if (var2 >= 0) {
         return var2;
      } else {
         this.rS.add(var1);
         return this.rS.size() - 1;
      }
   }
}
