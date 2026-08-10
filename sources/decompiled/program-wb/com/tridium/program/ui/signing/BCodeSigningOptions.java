package com.tridium.program.ui.signing;

import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.nre.security.SecurityInitializer;
import com.tridium.workbench.util.BPasswordPromptDialog;
import java.security.AccessController;
import java.security.UnrecoverableKeyException;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.security.BPassword;
import javax.baja.sys.BFacets;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;
import javax.baja.ui.options.BUserOptions;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "signingCert",
      type = "String",
      defaultValue = "",
      facets = {@Facet(
         name = "BFacets.FIELD_EDITOR",
         value = "\"workbench:WbCertificateAliasFE\""
      )}
   ), @NiagaraProperty(
      name = "tsaUrl",
      type = "String",
      defaultValue = "http://timestamp.digicert.com"
   )})
public class BCodeSigningOptions extends BUserOptions {
   public static final Property signingCert = newProperty(0, "", BFacets.make("fieldEditor", "workbench:WbCertificateAliasFE"));
   public static final Property tsaUrl = newProperty(0, "http://timestamp.digicert.com", null);
   public static final Type TYPE = Sys.loadType(BCodeSigningOptions.class);
   private char[] keyPassword;
   private static BCodeSigningOptions options;
   protected static Lexicon lex = Lexicon.make("program");

   public String getSigningCert() {
      return this.getString(signingCert);
   }

   public void setSigningCert(String v) {
      this.setString(signingCert, v, null);
   }

   public String getTsaUrl() {
      return this.getString(tsaUrl);
   }

   public void setTsaUrl(String v) {
      this.setString(tsaUrl, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BCodeSigningOptions make() {
      if (options == null) {
         options = (BCodeSigningOptions)load(TYPE);
      }

      return options;
   }

   public void changed(Property prop, Context cx) {
      if (prop.equals(signingCert)) {
         SecurityUtil.zeroCharArray(this.keyPassword);
         this.keyPassword = null;
      }
   }

   public char[] getKeyPassword(BWidget parent) throws Exception {
      CoreCryptoManager ccm = CoreCryptoManager.get(SecurityInitializer.getInstance().getSecurityInfoProvider());
      String alias = this.getSigningCert();
      boolean newPassword = false;
      if (this.keyPassword == null) {
         BPassword password = BPasswordPromptDialog.open(
            parent, lex.get("program.certPassword.title"), lex.getText("program.certPassword.description", new Object[]{alias})
         );
         if (password != null) {
            this.keyPassword = AccessController.<String>doPrivileged(password::getValue).toCharArray();
         } else {
            this.keyPassword = new char[0];
         }

         newPassword = true;
      }

      try {
         ccm.getKeyStore().getKey(alias, (char[])this.keyPassword.clone());
      } catch (UnrecoverableKeyException var9) {
         SecurityUtil.zeroCharArray(this.keyPassword);
         this.keyPassword = null;
         if (newPassword) {
            throw var9;
         }

         BPassword password = BPasswordPromptDialog.open(
            parent, lex.get("program.certPassword.title"), lex.getText("program.certPassword.description", new Object[]{alias})
         );
         if (password != null) {
            this.keyPassword = AccessController.<String>doPrivileged(password::getValue).toCharArray();
         } else {
            this.keyPassword = new char[0];
         }

         try {
            ccm.getKeyStore().getKey(alias, (char[])this.keyPassword.clone());
         } catch (UnrecoverableKeyException var8) {
            SecurityUtil.zeroCharArray(this.keyPassword);
            this.keyPassword = null;
            throw var8;
         }
      }

      return (char[])this.keyPassword.clone();
   }
}
