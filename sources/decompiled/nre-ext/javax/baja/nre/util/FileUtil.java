package javax.baja.nre.util;

import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.PlatformUtil;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.AccessController;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.Spliterator;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.CRC32;
import javax.baja.nre.function.ConsumerCanThrowException;

public class FileUtil {
   static Logger log = Logger.getLogger("sys.file");
   private static final int _1K = 1024;
   private static final int _2K = 2048;
   private static final int _4K = 4096;
   private static final int _8K = 8192;
   private static final int _16K = 16384;
   private static final int _32K = 32768;
   private static final int _64K = 65536;
   public static final int MAX_FILE_PATH_LENGTH = 4096;
   private static final String ATTACHMENT_DISPOSITION_TYPE = "attachment";
   private static final String FILENAME_PARAMETER = "filename";
   private static final Pattern CONTENT_DISP_EXT_PARAM_PATTERN = Pattern.compile(".*filename\\*(?:\\s*)=(?:\\s*)(.*)('.*')([^;]*)($|\\s*;.*)", 2);

   public static String getBase(String fileName) {
      if (fileName == null) {
         return null;
      }

      int x = fileName.lastIndexOf(46);
      return x >= 1 ? fileName.substring(0, x) : fileName;
   }

   public static String getExtension(String fileName) {
      if (fileName == null) {
         return null;
      }

      int x = fileName.lastIndexOf(46);
      return x >= 1 ? fileName.substring(x + 1) : null;
   }

   public static String replaceExtension(String fileName, String newExtension) {
      if (fileName == null) {
         return null;
      }

      if (fileName.length() == 0) {
         return fileName;
      }

      int x = fileName.lastIndexOf(46);
      return x >= 1 ? fileName.substring(0, x + 1) + newExtension : fileName + "." + newExtension;
   }

   public static byte[] read(InputStream in, long size) throws IOException {
      try (InputStream input = in) {
         if (size >= 0L && size <= 2147483647L) {
            int sz = (int)size;
            byte[] buf = new byte[sz];
            int count = 0;

            while (count < sz) {
               int n = input.read(buf, count, sz - count);
               if (n < 0) {
                  throw new IOException("Unexpected EOF");
               }

               count += n;
            }

            return buf;
         } else {
            throw new IOException("Invalid size " + size);
         }
      }
   }

   public static void pipe(InputStream in, long size, OutputStream out) throws IOException {
      int len = 4096;
      byte[] buf = new byte[len];

      while (size > 0L) {
         int n = in.read(buf, 0, (int)Math.min(size, len));
         if (n <= 0) {
            throw new IOException("Unexpected EOF");
         }

         out.write(buf, 0, n);
         size -= n;
      }
   }

   public static void pipe(InputStream in, OutputStream out) throws IOException {
      int len = 4096;
      byte[] buf = new byte[len];

      while (true) {
         int n = in.read(buf, 0, len);
         if (n < 0) {
            return;
         }

         out.write(buf, 0, n);
      }
   }

   public static long getCrc(File file) {
      long crc = 0L;

      try {
         if (AccessController.doPrivileged(() -> FileUtil.PlatformProviderHolder.PLATFORM_PROVIDER_INSTANCE.isFileRegular(file.getAbsolutePath()))) {
            FileInputStream fin = new FileInputStream(file);
            crc = getCrc(fin);
            fin.close();
         }
      } catch (Exception ignored) {
         crc = 0L;
      }

      return crc;
   }

   public static long getCrc(InputStream in) throws IOException {
      try {
         byte[] buf = new byte[4096];
         CRC32 crcObj = new CRC32();

         int numRead;
         while ((numRead = in.read(buf)) > 0) {
            crcObj.update(buf, 0, numRead);
         }

         return crcObj.getValue();
      } finally {
         if (in != null) {
            in.close();
         }
      }
   }

   public static File renameToBackup(File saveFile, int maxBackups) {
      try {
         String name = saveFile.getName();
         String prefix = name.substring(0, name.indexOf(46));
         File[] backups = getBackups(saveFile.getParentFile(), prefix);

         for (int i = Math.max(0, maxBackups - 1); i < backups.length; i++) {
            backups[i].delete();
         }

         if (maxBackups != 0) {
            return renameToBackup(saveFile);
         }
      } catch (Throwable e) {
         log.log(Level.WARNING, "failed to rename file '" + saveFile + "' to backup", e);
      }

      return null;
   }

   public static File renameToBackup(File file) throws Exception {
      if (!file.exists()) {
         return null;
      }

      File parent = new File(file.getParent());
      String name = file.getName();
      String ext = "";
      int dot = name.lastIndexOf(46);
      if (dot > 0) {
         ext = name.substring(dot);
         name = name.substring(0, dot);
      }

      String pattern = "yyMMdd_HHmm";
      SimpleDateFormat format = new SimpleDateFormat(pattern);
      format.setTimeZone(TimeZone.getDefault());
      String ts = format.format(new Date(file.lastModified()));
      File newFile = new File(parent, name + "_backup_" + ts + ext);

      for (int i = 1; newFile.exists(); i++) {
         newFile = new File(parent, name + "_backup_" + ts + "_" + i + ext);
      }

      if (!file.renameTo(newFile)) {
         throw new IOException("Cannot rename");
      } else {
         return newFile;
      }
   }

   public static File[] getBackups(File dir, String prefix) {
      ArrayList<File> result = new ArrayList<>();
      File[] list = dir.listFiles();
      if (list != null) {
         for (File f : list) {
            String name = f.getName();
            if (name.startsWith(prefix) && name.contains("_backup_")) {
               result.add(f);
            }
         }
      }

      File[] sorted = result.toArray(new File[0]);
      Long[] times = new Long[sorted.length];

      for (int i = 0; i < sorted.length; i++) {
         times[i] = sorted[i].lastModified();
      }

      SortUtil.rsort(times, sorted);
      return sorted;
   }

   public static void move(File oldFile, File newFile) throws IOException {
      move(oldFile, newFile, false);
   }

   public static void move(File oldFile, File newFile, boolean deleteExisting) throws IOException {
      if (log.isLoggable(Level.FINE)) {
         log.fine("Move: " + oldFile + " -> " + newFile);
      }

      if (newFile.exists()) {
         if (!deleteExisting) {
            throw new IOException("Cannot move to existing file: " + newFile);
         }

         if (!newFile.delete()) {
            throw new IOException("Cannot move: " + oldFile + " -> " + newFile + ", failed to delete existing");
         }
      }

      Files.move(oldFile.toPath(), newFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
   }

   public static void copy(File oldFile, File newFile) throws IOException {
      if (log.isLoggable(Level.FINE)) {
         log.fine("Copy: " + oldFile + " -> " + newFile);
      }

      if (newFile.exists()) {
         throw new IOException("Cannot copy to existing file: " + newFile);
      }

      if (oldFile.isDirectory()) {
         copyDir(oldFile, newFile);
      } else {
         copyFile(oldFile, newFile);
      }
   }

   public static void copyDir(File oldFile, File newFile) throws IOException {
      if (!newFile.exists() && !newFile.mkdirs()) {
         throw new IOException("Cannot make dir: " + newFile);
      }

      File[] kids = oldFile.listFiles();
      if (kids != null) {
         for (File kid : kids) {
            copy(kid, new File(newFile, kid.getName()));
         }
      }

      newFile.setLastModified(oldFile.lastModified());
   }

   public static void copyFile(File oldFile, File newFile) throws IOException {
      FileOutputStream out = null;
      FileInputStream in = null;

      try {
         out = new FileOutputStream(newFile);
         in = new FileInputStream(oldFile);
         byte[] buf = new byte[4096];
         long size = oldFile.length();
         long copied = 0L;

         while (copied < size) {
            int n = in.read(buf, 0, buf.length);
            if (n < 0) {
               throw new EOFException();
            }

            out.write(buf, 0, n);
            copied += n;
         }
      } finally {
         if (in != null) {
            try {
               in.close();
            } catch (IOException var18) {
            }
         }

         if (out != null) {
            try {
               out.close();
            } catch (IOException var17) {
            }
         }
      }

      if (newFile.exists()) {
         newFile.setLastModified(oldFile.lastModified());
      }
   }

   public static void deleteDiligently(File file) throws IOException {
      IOException[] exceptionHolder = new IOException[1];
      doDelete(file, f -> {
         try {
            Files.delete(f.toPath());
         } catch (IOException e) {
            if (exceptionHolder[0] == null) {
               exceptionHolder[0] = e;
            }

            if (log.isLoggable(Level.WARNING)) {
               log.log(Level.WARNING, "Cannot delete: " + f);
            }
         }
      });
      if (exceptionHolder[0] != null) {
         throw exceptionHolder[0];
      }
   }

   public static void delete(File file) throws IOException {
      doDelete(file, f -> Files.delete(f.toPath()));
   }

   private static <E extends Exception> void doDelete(File file, ConsumerCanThrowException<File, E> deleter) throws E {
      if (log.isLoggable(Level.FINE)) {
         log.fine("Delete: " + file);
      }

      if (file.exists()) {
         if (file.isDirectory()) {
            File[] kids = file.listFiles();
            if (kids != null) {
               for (File kid : kids) {
                  doDelete(kid, deleter);
               }
            }
         }

         deleter.accept(file);
      }
   }

   public static boolean diff(BufferedReader b1, BufferedReader b2) throws IOException {
      boolean diff = false;
      String s1 = b1.readLine();
      String s2 = b2.readLine();

      while (s1 != null || s2 != null) {
         if (s1 == null || s2 == null || !s1.equals(s2)) {
            diff = true;
         }

         if (s1 != null) {
            s1 = b1.readLine();
         }

         if (s2 != null) {
            s2 = b2.readLine();
         }
      }

      return diff;
   }

   public static String readString(File file) throws IOException {
      return readString(new FileReader(file));
   }

   public static String readString(Reader in) throws IOException {
      try {
         StringBuilder sb = new StringBuilder();
         BufferedReader bin = new BufferedReader(in);

         String str;
         while ((str = bin.readLine()) != null) {
            sb.append(str).append("\n");
         }

         return sb.toString();
      } finally {
         in.close();
      }
   }

   public static String[] readLines(File file) throws IOException {
      return readLines(new FileReader(file));
   }

   public static String[] readLines(Reader in) throws IOException {
      try {
         ArrayList<String> list = new ArrayList<>();
         BufferedReader bin = new BufferedReader(in);

         String str;
         while ((str = bin.readLine()) != null) {
            list.add(str);
         }

         return list.toArray(new String[0]);
      } finally {
         in.close();
      }
   }

   public static Stream<String> streamLines(InputStream in) {
      return streamLines(new InputStreamReader(in));
   }

   public static Stream<String> streamLines(File file) throws IOException {
      return streamLines(new FileReader(file));
   }

   public static Stream<String> streamLines(Reader in) {
      return StreamSupport.stream(new FileUtil.ReaderLineSpliterator(in), false).onClose(() -> {
         try {
            in.close();
         } catch (IOException var2) {
         }
      });
   }

   public static FileUtil.FileInfo getFileInfo(File file) {
      FileUtil.FileInfo info = new FileUtil.FileInfo();
      info.crc = getCrc(file);
      info.size = file.length();
      return info;
   }

   public static FileUtil.FileInfo getFileInfo(byte[] contents) {
      FileUtil.FileInfo info = new FileUtil.FileInfo();
      info.size = contents.length;
      CRC32 crcObj = new CRC32();
      crcObj.update(contents);
      info.crc = crcObj.getValue();
      return info;
   }

   public static FileUtil.FileInfo getFileInfo(InputStream in) throws IOException {
      FileUtil.FileInfo info = new FileUtil.FileInfo();

      try {
         byte[] buf = new byte[4096];
         CRC32 crcObj = new CRC32();

         int numRead;
         while ((numRead = in.read(buf)) > 0) {
            info.size += numRead;
            crcObj.update(buf, 0, numRead);
         }

         info.crc = crcObj.getValue();
         return info;
      } finally {
         if (in != null) {
            in.close();
         }
      }
   }

   public static int getFileBufferSize(long size) {
      if (size < 1024L) {
         return 1024;
      }

      if (size < 2048L) {
         return 2048;
      }

      if (size < 4096L) {
         return 4096;
      }

      if (size < 8192L) {
         return 8192;
      }

      long freeHeapMemory = Runtime.getRuntime().freeMemory();
      if (freeHeapMemory > 4194304L) {
         if (size < 16384L) {
            return 16384;
         } else {
            return size < 32768L ? 32768 : 65536;
         }
      } else {
         return size < 16384L ? 16384 : 32768;
      }
   }

   public static File find(File contextRoot, String fileName) {
      return find(contextRoot, fileName, false);
   }

   public static File find(File contextRoot, String fileName, boolean matchDirectories) {
      if (contextRoot == null) {
         throw new NullPointerException("NullContextRoot");
      }

      if (fileName == null) {
         throw new NullPointerException("NullFileName");
      }

      if (!contextRoot.isDirectory()) {
         Object[] filler = new Object[]{contextRoot.getAbsolutePath()};
         String message = "NotDirectory";
         throw new IllegalArgumentException(message);
      }

      File[] files = contextRoot.listFiles();

      for (File file : files) {
         String nextName = file.getName();
         if (file.isDirectory()) {
            if (nextName.equals(fileName) && matchDirectories) {
               return file;
            }

            File match = find(file, fileName);
            if (match != null) {
               return match;
            }
         } else if (nextName.equals(fileName)) {
            return file;
         }
      }

      return null;
   }

   public static String getDownloadFilename(URLConnection conn) {
      String contentDisposition = conn.getHeaderField("Content-Disposition");
      String attachmentName = null;
      if (contentDisposition != null) {
         attachmentName = getFileNameFromContentDisposition(contentDisposition);
      }

      return attachmentName != null ? attachmentName : getDefaultDownloadFileName(conn.getURL());
   }

   private static String getFileNameFromContentDisposition(String value) {
      value = value.trim();
      if (value.toLowerCase().startsWith("attachment")) {
         value = value.substring("attachment".length());
         Matcher matcher = CONTENT_DISP_EXT_PARAM_PATTERN.matcher(value);
         if (matcher.matches()) {
            String encoding = matcher.group(1);
            String encoded = matcher.group(3);

            try {
               return URLDecoder.decode(encoded, encoding);
            } catch (UnsupportedEncodingException e) {
               log.log(Level.WARNING, "Unable to URL Decode Content-Disposition filename", e);
               return null;
            }
         }

         int startPos = value.toLowerCase().indexOf("filename");
         if (startPos > -1) {
            if (startPos > 0 && value.charAt(startPos - 1) != ' ' && value.charAt(startPos - 1) != ';') {
               return null;
            }

            value = value.substring(startPos + "filename".length()).trim();
            StringBuilder sb = new StringBuilder();
            boolean quoted = false;
            boolean escapeNext = false;

            int i;
            for (i = 0; i < value.length(); i++) {
               char c = value.charAt(i);
               if (!Character.isWhitespace(c)) {
                  if (c != '=') {
                     return null;
                  }

                  i++;
                  break;
               }
            }

            for (; i < value.length(); i++) {
               char c = value.charAt(i);
               if (escapeNext) {
                  sb.append(c);
                  escapeNext = false;
               } else if ('"' != c && '\'' != c) {
                  if (';' == c) {
                     if (!quoted) {
                        break;
                     }

                     sb.append(c);
                  } else if ('\\' == c && quoted) {
                     escapeNext = true;
                  } else {
                     sb.append(c);
                  }
               } else if (sb.length() == 0) {
                  quoted = true;
               } else {
                  if (quoted) {
                     break;
                  }

                  sb.append(c);
               }
            }

            return sb.toString().trim();
         }
      }

      return null;
   }

   private static String getDefaultDownloadFileName(URL url) {
      String file = url.getFile();
      if (file == null) {
         throw new IllegalArgumentException();
      }

      if (file.startsWith("/ord?") && url.getQuery() != null) {
         file = url.getQuery();
      }

      try {
         file = URLDecoder.decode(file, "UTF-8");
      } catch (UnsupportedEncodingException var3) {
      }

      return cleanDownloadFileName(file);
   }

   private static String cleanDownloadFileName(String name) {
      int pos = name.lastIndexOf(47);
      if (pos > -1 && name.length() > pos + 1) {
         name = name.substring(pos + 1);
      }

      name = name.replaceFirst("^file(:|-)(!|\\^|~|\\^\\^)", "");
      pos = name.indexOf("|view:");
      if (pos > -1) {
         name = name.substring(0, pos);
      }

      return name;
   }

   public static class FileInfo {
      public long size = 0L;
      public long crc = 0L;
   }

   private static final class PlatformProviderHolder {
      private static final IPlatformProvider PLATFORM_PROVIDER_INSTANCE = AccessController.doPrivileged(PlatformUtil::getPlatformProvider);
   }

   private static class ReaderLineSpliterator implements Spliterator<String> {
      private final BufferedReader reader;

      public ReaderLineSpliterator(Reader reader) {
         Objects.requireNonNull(reader);
         this.reader = new BufferedReader(reader);
      }

      @Override
      public boolean tryAdvance(Consumer<? super String> action) {
         try {
            String line = this.reader.readLine();
            if (line == null) {
               this.reader.close();
               return false;
            } else {
               action.accept(line);
               return true;
            }
         } catch (RuntimeException rethrow) {
            throw rethrow;
         } catch (IOException e) {
            return false;
         }
      }

      @Override
      public Spliterator<String> trySplit() {
         return null;
      }

      @Override
      public long estimateSize() {
         return Long.MAX_VALUE;
      }

      @Override
      public int characteristics() {
         return 1296;
      }
   }
}
