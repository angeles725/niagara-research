package com.tridium.template.file;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.baja.file.FilePath;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.OrdQueryList;

public class MemoryPath extends FilePath {
   private static final String FILE_PREFIX_REGEX = "(\\^\\^?|[~!])";
   private static final Pattern FILE_PREFIX = Pattern.compile("(\\^\\^?|[~!])");

   private MemoryPath(String body) {
      super("memory", body);
   }

   public static MemoryPath make(String path) {
      if (path.startsWith("/")) {
         path = path.substring(1);
      }

      return new MemoryPath(path);
   }

   public MemoryPath(FilePath path) {
      super("memory", removePrefix(path.getBody()));
   }

   private static String removePrefix(String body) {
      Matcher matcher = FILE_PREFIX.matcher(body);
      if (matcher.find()) {
         body = body.substring(matcher.end());
      }

      return body;
   }

   public OrdQuery makePath(String body) {
      return new MemoryPath(body);
   }

   public boolean isHost() {
      return false;
   }

   public boolean isSession() {
      return true;
   }

   public void normalize(OrdQueryList list, int index) {
      list.shiftToSession(index);

      for (int i = index + 1; i < list.size(); i++) {
         OrdQuery query = list.get(i);
         if ("file".equals(query.getScheme())) {
            FilePath oldPath = (FilePath)query;
            MemoryPath newPath = new MemoryPath(oldPath);
            list.replace(i, newPath);
         }
      }
   }
}
