package com.tridium.template.ui;

import com.tridium.fox.sys.BFoxSession;
import com.tridium.template.BTemplateService;
import javax.baja.file.FilePath;
import javax.baja.naming.BISession;
import javax.baja.naming.BOrd;
import javax.baja.naming.SyntaxException;
import javax.baja.sys.BComponent;
import javax.baja.sys.BStation;
import javax.baja.user.BUser;
import javax.baja.user.BUserService;

public final class TemplateUiUtil {
   public static boolean isSuperUser(BComponent target) {
      BISession session = target.getSession();
      BUser user = null;
      if (session instanceof BFoxSession) {
         BFoxSession foxSession = (BFoxSession)session;

         try {
            String userName = foxSession.getUsername();
            BUserService userService = (BUserService)BOrd.make("service:baja:UserService").get(target);
            userService.lease();
            user = userService.getUser(userName);
            user.lease();
         } catch (Exception var6) {
         }
      }

      return user != null && user.getPermissions().isSuperUser();
   }

   public static BTemplateService resolveTemplateService(BStation station) {
      try {
         return (BTemplateService)BOrd.make("service:template:TemplateService").get(station);
      } catch (Exception var2) {
         return null;
      }
   }

   public static boolean isValidTemplateName(String name) {
      name = name.trim();

      try {
         int len = name.length();
         if (len == 0) {
            return false;
         } else {
            for (int i = 0; i < len; i++) {
               int c = name.charAt(i);
               if (i == 0) {
                  if ((c < 97 || c > 122) && (c < 65 || c > 90)) {
                     return false;
                  }
               } else if ((c < 97 || c > 122) && (c < 65 || c > 90) && (c < 48 || c > 57) && c != 95 && c != 45) {
                  return false;
               }
            }

            return true;
         }
      } catch (Exception var4) {
         return false;
      }
   }

   public static boolean isValidTemplateFolder(String folder) {
      folder = folder.trim();
      if (folder.isEmpty()) {
         return true;
      } else if (folder.charAt(0) == '.') {
         return false;
      } else {
         try {
            FilePath folderPath = new FilePath(folder);
            return folderPath.isRelative() || folderPath.isUserHomeAbsolute() || folderPath.isLocalAbsolute();
         } catch (SyntaxException var2) {
            return false;
         }
      }
   }

   public static boolean isValidTemplateNameEntry(String nameEntry) {
      return isValidTemplateName(templateNameFromNameEntry(nameEntry)) && isValidTemplateFolder(templateFolderFromNameEntry(nameEntry));
   }

   public static boolean isValidTemplatePath(String folderEntry, String nameEntry) {
      return isValidTemplateNameEntry(nameEntry) && isValidTemplateFolder(folderEntry);
   }

   public static String templateNameFromNameEntry(String nameEntry) {
      nameEntry = nameEntry.trim();
      return nameEntry.substring(nameEntry.lastIndexOf(47) + 1);
   }

   public static String templateFolderFromNameEntry(String nameEntry) {
      nameEntry = nameEntry.trim();
      int length = nameEntry.lastIndexOf(47);
      return length > 0 ? nameEntry.substring(0, length) : "";
   }

   public static String templateFolderFromEntries(String folderEntry, String nameEntry) {
      return templateFolderFromNameEntry(templateRelativePathFromEntries(folderEntry, nameEntry));
   }

   public static String templateRelativePathFromEntries(String folderEntry, String nameEntry) {
      nameEntry = nameEntry.trim();
      folderEntry = folderEntry.trim();
      if (folderEntry.isEmpty()) {
         return nameEntry;
      } else {
         if (!folderEntry.endsWith("/")) {
            folderEntry = folderEntry + '/';
         }

         return folderEntry + nameEntry;
      }
   }

   private TemplateUiUtil() {
   }
}
