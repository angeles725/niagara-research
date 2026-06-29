package com.tridium.fox.sys.spy;

import com.tridium.fox.sys.BFoxSession;
import com.tridium.fox.sys.BIFoxProxySpace;
import javax.baja.file.FilePath;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.BSpy;
import javax.baja.spy.BSpySpace;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.LexiconText;

@NiagaraType(
   agent = {@AgentOn(
      types = {"fox:FoxSession"}
   )}
)
public class BFoxSpySpace extends BSpySpace implements BIFoxProxySpace {
   public static final Type TYPE = Sys.loadType(BFoxSpySpace.class);

   public Type getType() {
      return TYPE;
   }

   public BFoxSpySpace() {
      super("spy", LexiconText.make("fox", "nav.spy"));
   }

   @Override
   public void init(BFoxSession foxSession) {
   }

   @Override
   public void cleanup(BFoxSession foxSession) {
   }

   public BSpy resolveSpy(FilePath path) {
      try {
         return this.channel().get(path);
      } catch (Exception var3) {
         var3.printStackTrace();
         if (var3.getMessage() != null) {
            throw new UnresolvedException(var3.getMessage(), var3);
         } else {
            throw new UnresolvedException(path.toString(), var3);
         }
      }
   }

   public BFoxSession getFoxSession() {
      return (BFoxSession)this.getNavParent();
   }

   public BSpyChannel channel() {
      return (BSpyChannel)this.getFoxSession().getConnection().getChannels().get("spy");
   }
}
