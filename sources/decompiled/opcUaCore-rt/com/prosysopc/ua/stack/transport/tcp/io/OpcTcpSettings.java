package com.prosysopc.ua.stack.transport.tcp.io;

import com.prosysopc.ua.stack.transport.security.Cert;
import com.prosysopc.ua.stack.transport.security.CertificateValidator;
import com.prosysopc.ua.stack.transport.security.PrivKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.EnumSet;

public class OpcTcpSettings implements Cloneable {
   PrivKey ym;
   Cert rE;
   CertificateValidator cr;
   EnumSet<OpcTcpSettings.Flag> yn = EnumSet.noneOf(OpcTcpSettings.Flag.class);
   int yo = -1;
   int connectTimeout = -1;
   int yp = -1;
   int vh = 0;

   public OpcTcpSettings clone() {
      OpcTcpSettings var1 = new OpcTcpSettings();
      var1.setClientCertificate(this.rE);
      var1.setCertificateValidator(this.cr);
      var1.setPrivKey(this.ym);
      var1.yn = this.yn.clone();
      var1.setConnectTimeout(this.connectTimeout);
      var1.setHandshakeTimeout(this.yo);
      var1.setReverseHelloAcceptTimeout(this.yp);
      return var1;
   }

   public CertificateValidator getCertificateValidator() {
      return this.cr;
   }

   public Cert getClientCertificate() {
      return this.rE;
   }

   public int getConnectTimeout() {
      return this.connectTimeout;
   }

   public EnumSet<OpcTcpSettings.Flag> getFlags() {
      return this.yn;
   }

   public int getHandshakeTimeout() {
      return this.yo;
   }

   public int getMaxConnections() {
      return this.vh;
   }

   public PrivKey getPrivKey() {
      return this.ym;
   }

   public int getReverseHelloAcceptTimeout() {
      return this.yp;
   }

   public void readFrom(OpcTcpSettings var1) {
      if (var1.rE != null) {
         this.rE = var1.rE;
      }

      if (var1.cr != null) {
         this.cr = var1.cr;
      }

      if (var1.ym != null) {
         this.ym = var1.ym;
      }

      this.yn = var1.yn;
      this.yp = var1.yp;
      this.yo = var1.yo;
      this.connectTimeout = var1.connectTimeout;
      this.vh = var1.vh;
   }

   public void setCertificateValidator(CertificateValidator var1) {
      this.cr = var1;
   }

   public void setClientCertificate(Cert var1) {
      this.rE = var1;
   }

   public void setClientCertificate(X509Certificate var1) throws CertificateEncodingException {
      this.rE = new Cert(var1);
   }

   public void setConnectTimeout(int var1) {
      this.connectTimeout = var1;
   }

   public void setFlags(EnumSet<OpcTcpSettings.Flag> var1) {
      this.yn = var1;
   }

   public void setHandshakeTimeout(int var1) {
      this.yo = var1;
   }

   public void setMaxConnections(int var1) {
      this.vh = var1;
   }

   public void setPrivKey(PrivKey var1) {
      this.ym = var1;
   }

   public void setReverseHelloAcceptTimeout(int var1) {
      this.yp = var1;
   }

   public static enum Flag {
      MultiThread;
   }
}
