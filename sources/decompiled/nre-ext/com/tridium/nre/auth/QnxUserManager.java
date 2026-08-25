package com.tridium.nre.auth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.List;

public final class QnxUserManager {
   private static final int _MAXIMUM_ARGUMENT_LENGTH = 4096;
   private static final String _EXECUTABLE_NAME = "/proc/boot/usermgr";
   private static final String _USER_ID_ARG = "-u";
   private static final String _GROUP_ID_ARG = "-g";
   private static final String _OLD_PASSWORD_ARG = "-o";
   private static final String _COMMENT_ARG = "-c";
   private static final String _PASSWORD_ARG = "-p";
   private static final String _HASHED_PASSWORD_ARG = "-h";
   private static final String _ADD_USER_COMMAND = "add";
   private static final String _REMOVE_USER_COMMAND = "remove";
   private static final String _ADD_USER_GROUP_COMMAND = "gradd";
   private static final String _REMOVE_USER_GROUP_COMMAND = "grremove";
   private static final String _CHANGE_PASSWORD_COMMAND = "change";

   public static String addUserAccount(String name, String password, String comment, boolean passwordHashed) {
      if (!NativeAccount.isAccountQualifierValid(name)) {
         return null;
      }

      if (name.length() > 4096) {
         return null;
      }

      if (password == null) {
         return null;
      }

      if (password.length() > 4096) {
         return null;
      }

      if (comment == null) {
         return null;
      }

      if (comment.length() > 4096) {
         return null;
      }

      StringBuilder output = new StringBuilder();
      if (NativeAccount.isAccountNameFullyQualified(name)) {
         name = new UserAccount(name, null).getAccountName();
      }

      List<String> command = new ArrayList<>();
      command.add("/proc/boot/usermgr");
      command.add("-u");
      command.add(name);
      command.add("-p");
      command.add(password);
      if (passwordHashed) {
         command.add("-h");
      }

      command.add("-c");
      command.add(comment);
      command.add("add");
      if (execute(command.toArray(new String[0]), output) != 0) {
         return null;
      }

      String rawOutput = output.toString();
      return !rawOutput.contains(":") ? null : rawOutput.split(":")[1];
   }

   public static boolean removeUserAccount(String userId) {
      if (userId == null) {
         return false;
      } else {
         return userId.length() > 4096 ? false : execute(new String[]{"/proc/boot/usermgr", "-u", userId, "remove"}, null) == 0;
      }
   }

   public static boolean addUserToGroup(String userId, String groupId) {
      if (userId == null) {
         return false;
      } else if (userId.length() > 4096) {
         return false;
      } else if (groupId == null) {
         return false;
      } else {
         return groupId.length() > 4096 ? false : execute(new String[]{"/proc/boot/usermgr", "-u", userId, "-g", groupId, "gradd"}, null) == 0;
      }
   }

   public static boolean removeUserFromGroup(String userId, String groupId) {
      if (userId == null) {
         return false;
      } else if (userId.length() > 4096) {
         return false;
      } else if (groupId == null) {
         return false;
      } else {
         return groupId.length() > 4096 ? false : execute(new String[]{"/proc/boot/usermgr", "-u", userId, "-g", groupId, "grremove"}, null) == 0;
      }
   }

   public static boolean changeUserPassword(String userId, String oldPassword, String newPassword) {
      if (userId == null) {
         return false;
      } else if (userId.length() > 4096) {
         return false;
      } else if (oldPassword == null) {
         return false;
      } else if (oldPassword.length() > 4096) {
         return false;
      } else if (newPassword == null) {
         return false;
      } else {
         return newPassword.length() > 4096
            ? false
            : execute(new String[]{"/proc/boot/usermgr", "-u", userId, "-o", oldPassword, "-p", newPassword, "change"}, null) == 0;
      }
   }

   private static int execute(String[] arguments, StringBuilder standardOutputBuffer) {
      int rc;
      try {
         rc = AccessController.doPrivileged(() -> {
            Process process = Runtime.getRuntime().exec(arguments);
            if (standardOutputBuffer != null) {
               String line;
               try (BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                  while ((line = stdoutReader.readLine()) != null) {
                     standardOutputBuffer.append(line);
                  }
               }
            }

            return process.waitFor();
         });
      } catch (PrivilegedActionException pae) {
         pae.printStackTrace();
         rc = -1;
      }

      return rc;
   }
}
