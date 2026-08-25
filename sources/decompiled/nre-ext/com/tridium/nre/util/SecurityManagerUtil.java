package com.tridium.nre.util;

import com.tridium.nre.security.NiagaraBasicPermission;
import java.security.AccessControlContext;
import java.security.AccessController;
import javax.security.auth.Subject;
import javax.security.auth.SubjectDomainCombiner;

public final class SecurityManagerUtil {
   public static void disableSecurityManager() {
      NiagaraBasicPermission permission = new NiagaraBasicPermission("DISABLE_SECURITY_MANAGER");
      SecurityManager sm = System.getSecurityManager();
      if (sm != null) {
         sm.checkPermission(permission);
      }

      AccessController.doPrivileged(() -> {
         System.setSecurityManager(null);
         return null;
      });
   }

   public static Subject getCurrentAuthenticatedSubject() {
      return Subject.getSubject(AccessController.getContext());
   }

   public static AccessControlContext addSubjectToContext(AccessControlContext context) {
      Subject subject = Subject.getSubject(AccessController.getContext());
      return subject != null ? AccessController.doPrivileged(() -> new AccessControlContext(context, new SubjectDomainCombiner(subject))) : context;
   }
}
