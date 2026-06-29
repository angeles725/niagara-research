package com.tridium.fox.sys;

import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.message.FoxString;
import com.tridium.fox.message.FoxTuple;
import com.tridium.fox.session.Fox;
import com.tridium.fox.session.ServerException;
import com.tridium.util.ThrowableUtil;
import java.io.IOException;
import javax.baja.sys.LocalizableException;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.ModuleNotFoundException;

public class LocalizableExceptionTranslator extends Fox.ExceptionTranslator {
   @Override
   public FoxMessage exceptionToMessage(Throwable e) {
      FoxMessage msg = new FoxMessage();
      msg.add("exception", e.toString());

      try {
         ModuleNotFoundException x = getModuleNotFoundException(e);
         if (x != null) {
            msg.add("moduleNotFound", x.getModuleName());
            return msg;
         }

         if (e instanceof LocalizableException) {
            LocalizableException le = (LocalizableException)e;
            msg.add("lexModule", le.getLexiconModule());
            msg.add("lexKey", le.getLexiconKey());
            Object[] args = le.getLexiconArguments();
            if (args != null && args.length > 0) {
               msg.add(this.argsToMessage(args));
            }
         } else if (e instanceof LocalizableRuntimeException) {
            LocalizableRuntimeException le = (LocalizableRuntimeException)e;
            msg.add("lexModule", le.getLexiconModule());
            msg.add("lexKey", le.getLexiconKey());
            Object[] args = le.getLexiconArguments();
            if (args != null && args.length > 0) {
               msg.add(this.argsToMessage(args));
            }
         }
      } catch (Throwable var6) {
         var6.printStackTrace();
      }

      return msg;
   }

   @Override
   public Exception messageToException(FoxMessage msg) throws IOException {
      try {
         String moduleNotFound = msg.getString("moduleNotFound", null);
         if (moduleNotFound != null) {
            return new ModuleNotFoundRemoteException(moduleNotFound);
         }

         String lexModule = msg.getString("lexModule", null);
         if (lexModule != null) {
            String lexKey = msg.getString("lexKey", null);
            Object[] lexArgs = this.messageToArgs((FoxMessage)msg.getOptional("lexArgs"));
            return new LocalizableServerException(lexModule, lexKey, lexArgs);
         }
      } catch (Throwable var6) {
         var6.printStackTrace();
      }

      return msg == null ? new ServerException("cause unknown") : new ServerException(msg.getString("exception", "cause unknown"));
   }

   FoxMessage argsToMessage(Object[] args) {
      FoxMessage msg = new FoxMessage("lexArgs");

      for (int i = 0; i < args.length; i++) {
         msg.add("a", String.valueOf(args[i]));
      }

      return msg;
   }

   Object[] messageToArgs(FoxMessage msg) {
      if (msg == null) {
         return null;
      } else {
         FoxTuple[] list = msg.list("a");
         Object[] args = new Object[list.length];

         for (int i = 0; i < args.length; i++) {
            args[i] = ((FoxString)list[i]).value;
         }

         return args;
      }
   }

   static ModuleNotFoundException getModuleNotFoundException(Throwable e) {
      while (e != null) {
         if (e instanceof ModuleNotFoundException) {
            return (ModuleNotFoundException)e;
         }

         e = ThrowableUtil.getCause(e);
      }

      return null;
   }
}
