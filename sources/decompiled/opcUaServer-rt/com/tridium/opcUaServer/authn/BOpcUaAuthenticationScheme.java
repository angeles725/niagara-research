package com.tridium.opcUaServer.authn;

import com.tridium.authn.NiagaraLoginConfiguration;
import java.util.HashMap;
import javax.baja.authn.BPasswordAuthenticationScheme;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

@NiagaraType
public class BOpcUaAuthenticationScheme extends BPasswordAuthenticationScheme {
   public static final Type TYPE = Sys.loadType(BOpcUaAuthenticationScheme.class);
   private static final Lexicon lex = Lexicon.make(BOpcUaAuthenticationScheme.class);
   private static final String SCHEME_NAME = lex.getText("opcUaAuthScheme");
   private Configuration configuration = null;

   public Type getType() {
      return TYPE;
   }

   public String getSchemeName() {
      return SCHEME_NAME;
   }

   public Configuration getLoginConfiguration() {
      if (this.configuration == null) {
         this.configuration = new NiagaraLoginConfiguration(OpcUaLoginModule.class.getName(), LoginModuleControlFlag.REQUIRED, new HashMap());
      }

      return this.configuration;
   }
}
