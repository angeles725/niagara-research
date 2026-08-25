package com.tridium.nre.util;

import com.tridium.nre.security.NiagaraBasicPermission;
import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedActionException;
import java.util.logging.Logger;

public abstract class NreRuntime {
   private Runtime runtime;
   private static Logger NRE_LOG = Logger.getLogger("nre");

   protected final void setRuntime(Runtime runtime) {
      this.runtime = runtime;
   }

   protected final Process exec(String[] cmd, String[] envp, File dir) throws IOException {
      if (cmd != null && cmd.length != 0) {
         try {
            Permission execPermission = new NiagaraBasicPermission("RUNTIME_EXEC");
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
               sm.checkPermission(execPermission);
            }

            StringBuilder buf = new StringBuilder("NreRuntime.exec():");

            for (int i = 0; i < cmd.length; i++) {
               buf.append(" " + cmd[i]);
            }

            if (envp != null) {
               buf.append(" with ");

               for (int i = 0; i < envp.length; i++) {
                  buf.append(envp[i] + ", ");
               }
            }

            if (dir != null) {
               buf.append(" from " + dir);
            }

            NRE_LOG.info(buf.toString());
            return AccessController.doPrivileged(() -> this.runtime.exec(cmd, envp, dir));
         } catch (PrivilegedActionException e) {
            throw new RuntimeException(e);
         }
      } else {
         throw new UnsupportedOperationException("Cannot exec null command");
      }
   }
}
