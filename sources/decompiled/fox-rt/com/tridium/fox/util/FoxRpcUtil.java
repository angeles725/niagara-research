package com.tridium.fox.util;

import com.tridium.fox.session.FoxSession;
import com.tridium.fox.sys.BFoxSession;
import com.tridium.fox.sys.broker.BFoxComponentSpace;
import com.tridium.json.JSONArray;
import com.tridium.util.NiagaraRpcUtil;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.naming.BISession;
import javax.baja.naming.BOrd;
import javax.baja.rpc.TransportType;
import javax.baja.space.BISpaceNode;
import javax.baja.space.BSpace;
import javax.baja.sys.BComponent;
import javax.baja.sys.Property;
import javax.baja.util.Lexicon;
import javax.baja.util.Version;

public final class FoxRpcUtil {
   private static final Logger log = Logger.getLogger("fox.rpc");

   private FoxRpcUtil() {
   }

   public static <R> Optional<R> doRpcProperty(BComponent c, Property p, String methodName, Object... args) throws Exception {
      if (!c.isMounted()) {
         return Optional.empty();
      } else {
         try {
            BISession session = c.getSession();
            if (session instanceof BFoxSession) {
               BFoxSession foxSession = (BFoxSession)session;
               Version remoteVersion = foxSession.getConnection().getRemoteVersion();
               if (remoteVersion.compareTo(FoxSession.VERSION_4_2) < 0 && NiagaraRpcUtil.isWhitelistedLegacyRpc(p.getType().getTypeSpec(), methodName)) {
                  try {
                     return Optional.ofNullable(doOldRpc(c, p, methodName, args));
                  } catch (Exception var10) {
                  }
               }

               return foxSession.rpc(BOrd.make(c.getAbsoluteOrd(), "slot:" + p.getName()), methodName, args);
            } else {
               JSONArray argsArray;
               if (NiagaraRpcUtil.isWhitelistedLegacyRpc(p.getType().getTypeSpec(), methodName)) {
                  argsArray = NiagaraRpcUtil.encodeLegacyArgs(args);
               } else {
                  argsArray = new JSONArray();

                  for (Object o : args) {
                     argsArray.put(NiagaraRpcUtil.convertFromCollection(o));
                  }
               }

               return NiagaraRpcUtil.rpc(
                  TransportType.fox,
                  true,
                  "127.0.0.1",
                  BOrd.make(c.getOrdInSession(), "slot:" + p.getName()),
                  methodName,
                  argsArray,
                  c.getSession().getSessionContext()
               );
            }
         } catch (Exception var11) {
            log.log(Level.INFO, "RPC failed for " + c.getType().getTypeName() + '#' + methodName, (Throwable)var11);
            if (!(var11 instanceof SecurityException) && !(var11.getCause() instanceof SecurityException)) {
               throw var11;
            } else {
               throw new Exception(Lexicon.make("baja").getText("niagaraRpc.securityMessage"));
            }
         }
      }
   }

   public static <R> Optional<R> doRpc(BISpaceNode node, String methodName, Object... args) throws Exception {
      if (log.isLoggable(Level.FINE)) {
         log.fine(String.format("Attempting rpc call to %s#%s", node.getNavOrd(), methodName));
      }

      if (!node.isMounted()) {
         return Optional.empty();
      } else {
         BISession session = node.getSession();

         try {
            if (session instanceof BFoxSession) {
               BFoxSession foxSession = (BFoxSession)session;
               Version remoteVersion = foxSession.getConnection().getRemoteVersion();
               if (remoteVersion.compareTo(FoxSession.VERSION_4_2) < 0 && NiagaraRpcUtil.isWhitelistedLegacyRpc(node.getType().getTypeSpec(), methodName)) {
                  try {
                     return Optional.ofNullable(doOldRpc(node, methodName, args));
                  } catch (Exception var9) {
                  }
               }

               return foxSession.rpc(node.getAbsoluteOrd(), methodName, args);
            } else {
               JSONArray argsArray;
               if (NiagaraRpcUtil.isWhitelistedLegacyRpc(node.getType().getTypeSpec(), methodName)) {
                  argsArray = NiagaraRpcUtil.encodeLegacyArgs(args);
               } else {
                  argsArray = new JSONArray();

                  for (Object o : args) {
                     argsArray.put(NiagaraRpcUtil.convertFromCollection(o));
                  }
               }

               return NiagaraRpcUtil.rpc(TransportType.fox, true, "127.0.0.1", node.getOrdInSession(), methodName, argsArray, session.getSessionContext());
            }
         } catch (Exception var10) {
            log.log(Level.INFO, "RPC failed for " + node.getType().getTypeName() + '#' + methodName, (Throwable)var10);
            if (!(var10 instanceof SecurityException) && !(var10.getCause() instanceof SecurityException)) {
               throw var10;
            } else {
               throw new Exception(Lexicon.make("baja").getText("niagaraRpc.securityMessage"));
            }
         }
      }
   }

   public static <R> Optional<R> doSilentRpc(BISpaceNode node, String methodName, Object... args) {
      try {
         return doRpc(node, methodName, args);
      } catch (Exception var4) {
         return Optional.empty();
      }
   }

   private static <R> R doOldRpc(BISpaceNode node, String methodName, Object... args) throws Exception {
      return doOldRpc(node, null, methodName, args);
   }

   private static <R> R doOldRpc(BISpaceNode node, Property p, String methodName, Object... args) throws Exception {
      BSpace space = node.getSpace();
      if (!(space instanceof BFoxComponentSpace)) {
         throw new IllegalArgumentException("Node is not mounted in a Fox Component Space");
      } else if (p != null && !(node instanceof BComponent)) {
         throw new IllegalArgumentException("Cannot reflect property on non-BComponent");
      } else {
         Object arg = null;
         switch (args.length) {
            case 1:
               arg = args[0];
            case 0:
               try {
                  return (R)space.fw(
                     109,
                     node,
                     p == null ? "reflect:" + methodName : "reflectProperty:" + p.getName() + ":" + methodName,
                     arg,
                     Optional.ofNullable(node.getSession()).map(BISession::getSessionContext).orElse(null)
                  );
               } catch (Exception var7) {
                  log.log(Level.INFO, "Fw.RPC failed for " + node.getType().getTypeName() + '#' + methodName, (Throwable)var7);
                  throw var7;
               }
            default:
               throw new IllegalArgumentException("Fw.RPC call can only have one argument");
         }
      }
   }
}
