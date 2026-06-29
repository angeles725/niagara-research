package com.prosysopc.ua.stack.builtintypes;

import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.StatusCodes;
import java.util.Arrays;
import java.util.Locale;

public class ServiceResult {
   private StatusCode code;
   private String symbolicId;
   private String namespaceUri;
   private LocalizedText localizedText;
   private String additionalInfo;
   private ServiceResult innerResult;

   public static String buildExceptionTrace(Throwable var0) {
      return var0 == null ? null : var0.getStackTrace().toString();
   }

   public static ServiceResult toServiceResult(Throwable var0) {
      ServiceResult var1 = new ServiceResult();
      var1.setCode(var0 instanceof ServiceResultException ? ((ServiceResultException)var0).getStatusCode() : new StatusCode(StatusCodes.Bad_UnexpectedError));
      var1.setSymbolicId(var1.toString());
      var1.setLocalizedText(new LocalizedText(var0.getMessage(), ""));
      var1.setAdditionalInfo(Arrays.toString((Object[])var0.getStackTrace()));
      return var1;
   }

   public ServiceResult() {
      this.initialize();
   }

   public ServiceResult(StatusCode var1) {
      this.initialize(var1);
   }

   public ServiceResult(StatusCode var1, Throwable var2) {
      this.initialize(var1, var2);
   }

   public ServiceResult(UnsignedInteger var1) {
      this.initialize(new StatusCode(var1));
   }

   public ServiceResult(UnsignedInteger var1, Throwable var2) {
      this.initialize(new StatusCode(var1), var2);
   }

   public String getAdditionalInfo() {
      return this.additionalInfo;
   }

   public StatusCode getCode() {
      return this.code;
   }

   public ServiceResult getInnerResult() {
      return this.innerResult;
   }

   public LocalizedText getLocalizedText() {
      return this.localizedText;
   }

   public String getNamespaceUri() {
      return this.namespaceUri;
   }

   public String getSymbolicId() {
      return this.symbolicId;
   }

   public boolean isBad() {
      return this.code == null ? false : this.code.isBad();
   }

   public boolean isGood() {
      return this.code == null ? false : this.code.isGood();
   }

   public void setAdditionalInfo(String var1) {
      this.additionalInfo = var1;
   }

   public void setCode(StatusCode var1) {
      this.code = var1;
   }

   public void setInnerResult(ServiceResult var1) {
      this.innerResult = var1;
   }

   public void setLocalizedText(LocalizedText var1) {
      this.localizedText = var1;
   }

   public void setNamespaceUri(String var1) {
      this.namespaceUri = var1;
   }

   public void setSymbolicId(String var1) {
      this.symbolicId = var1;
   }

   private void initialize() {
      this.initialize(StatusCode.GOOD, null);
   }

   private void initialize(StatusCode var1) {
      this.code = var1;
      this.symbolicId = this.lookUpSymbolicId(var1);
      this.localizedText = null;
      this.additionalInfo = null;
   }

   private void initialize(StatusCode var1, Throwable var2) {
      this.code = var1;
      this.symbolicId = this.lookUpSymbolicId(this.code);
      if (var2 != null) {
         this.localizedText = new LocalizedText(var2.getMessage(), Locale.ENGLISH);
         this.additionalInfo = buildExceptionTrace(var2);
      }
   }

   private String lookUpSymbolicId(StatusCode var1) {
      return var1.getName();
   }
}
