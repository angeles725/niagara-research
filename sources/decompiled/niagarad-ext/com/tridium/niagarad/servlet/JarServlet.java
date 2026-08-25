package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.log.MessageBundle;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.baja.nre.util.TextUtil;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JarServlet extends Servlet {
   private Logger filter;
   private String canonicalNiagaraHomeDirectory;
   private String canonicalNiagaraUserHomeDirectory;
   private final List<String> declaredSymbolicLinks = new ArrayList<>();

   public JarServlet() {
      super("jars");
   }

   @Override
   public boolean doStart() {
      this.filter = Logger.getLogger("jars");

      try {
         this.canonicalNiagaraHomeDirectory = new File(NiagaraDaemon.NIAGARA_HOME).getCanonicalPath();
         this.canonicalNiagaraUserHomeDirectory = new File(NiagaraDaemon.NIAGARA_USER_HOME).getCanonicalPath();
      } catch (Exception e) {
         this.filter.log(Level.SEVERE, "failed to canonicalize jar servlet directory, can not start servlet", e);
         return false;
      }

      String symlinkList = NiagaraDaemon.FILESTORE_NIAGARA_HOME_SYMLINKS;
      if (symlinkList != null) {
         String[] symlinkListArray = TextUtil.split(symlinkList, File.pathSeparatorChar);

         for (String symlinkEntry : symlinkListArray) {
            String absoluteSymlinkPath = NiagaraDaemon.NIAGARA_HOME + File.separator + symlinkEntry;
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.fine("jar servlet adding niagara_home external symlink exception at '" + absoluteSymlinkPath + "'");
            }

            this.declaredSymbolicLinks.add(absoluteSymlinkPath);
         }
      }

      symlinkList = NiagaraDaemon.FILESTORE_NIAGARA_USER_HOME_SYMLINKS;
      if (symlinkList != null) {
         String[] symlinkListArray = TextUtil.split(symlinkList, File.pathSeparatorChar);

         for (String symlinkEntry : symlinkListArray) {
            String absoluteSymlinkPath = NiagaraDaemon.NIAGARA_USER_HOME + File.separator + symlinkEntry;
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.fine("jar servlet adding niagara_user_home external symlink exception at '" + absoluteSymlinkPath + "'");
            }

            this.declaredSymbolicLinks.add(absoluteSymlinkPath);
         }
      }

      return true;
   }

   @Override
   public void doGet(HttpServletRequest req, HttpServletResponse resp) {
      String uri = this.getUriWithoutName(req.getRequestURI());
      if (!uri.startsWith("/niagara") && !uri.startsWith("/niagara_user")) {
         MessageBundle msg = new MessageBundle("platform", "JarServlet.homeRequired", "JarServlet: File must be under niagara home or niagara user home");
         Http.sendErrorXML(req, resp, 400, msg);
      } else {
         boolean jar = true;
         int extensionIndex = uri.indexOf(".jar");
         if (extensionIndex == -1) {
            jar = false;
            extensionIndex = uri.indexOf(".sjar");
            if (extensionIndex == -1) {
               MessageBundle msg = new MessageBundle("platform", "JarServlet.jarMissing", "JarServlet: Target file must be descendant of jar or sjar file");
               Http.sendErrorXML(req, resp, 400, msg);
               return;
            }
         }

         boolean isNiagaraHome = !uri.startsWith("/niagara_user");
         int mountUriLength = isNiagaraHome ? "/niagara".length() : "/niagara_user".length();
         String homeDirectory = isNiagaraHome ? NiagaraDaemon.NIAGARA_HOME : NiagaraDaemon.NIAGARA_USER_HOME;
         String canonicalHomeDirectory = isNiagaraHome ? this.canonicalNiagaraHomeDirectory : this.canonicalNiagaraUserHomeDirectory;
         String jarPath = homeDirectory + uri.substring(mountUriLength, jar ? extensionIndex + ".jar".length() : extensionIndex + ".sjar".length());

         try {
            File jarFile = new File(jarPath);
            if (!jarFile.getCanonicalPath().toLowerCase(Locale.ENGLISH).startsWith(homeDirectory.toLowerCase(Locale.ENGLISH))
               && !jarFile.getCanonicalPath().toLowerCase(Locale.ENGLISH).startsWith(canonicalHomeDirectory.toLowerCase(Locale.ENGLISH))
               && !this.isAllowedPath(jarPath)) {
               this.handleForbiddenAccess(req, resp, uri);
               return;
            }
         } catch (AccessControlException e) {
            this.handleForbiddenAccess(req, resp, uri);
            return;
         } catch (Exception e) {
            this.filter.log(Level.SEVERE, "file open failed", e);
            MessageBundle msg = new MessageBundle("platform", "JarServlet.fileOpenFailed", "JarServlet: File open failed");
            Http.sendErrorXML(req, resp, 500, msg);
         }

         int contentStartIndex = jar ? extensionIndex + ".jar/".length() : extensionIndex + ".sjar/".length();
         if (contentStartIndex > uri.length()) {
            MessageBundle msg = new MessageBundle("platform", "JarServlet.jarMissing", "JarServlet: Target file must be descendant of jar or sjar file");
            Http.sendErrorXML(req, resp, 400, msg);
         } else {
            String contentPath = uri.substring(contentStartIndex);

            for (File contentFile = new File(contentPath); contentFile != null && !contentFile.getName().isEmpty(); contentFile = contentFile.getParentFile()) {
               if (".".equals(contentFile.getName()) || "..".equals(contentFile.getName())) {
                  MessageBundle msg = new MessageBundle("platform", "JarServlet.badContentSpecification", "JarServlet: Invalid jar/sjar content specification");
                  Http.sendErrorXML(req, resp, 400, msg);
                  return;
               }
            }

            try (ZipFile zipFile = new ZipFile(jarPath)) {
               ZipEntry entry = zipFile.getEntry(contentPath);
               if (entry == null) {
                  Http.sendError(req, resp, 404);
                  return;
               }

               try (InputStream jarIn = zipFile.getInputStream(entry)) {
                  int bufSize = 16384;
                  byte[] buf = new byte[bufSize];
                  int total = (int)entry.getSize();
                  int soFar = 0;

                  int thisRead;
                  while ((thisRead = jarIn.read(buf, 0, Math.min(total - soFar, bufSize))) > 0) {
                     resp.getOutputStream().write(buf, 0, thisRead);
                     soFar += thisRead;
                  }
               } catch (IOException ioe) {
                  this.filter.log(Level.SEVERE, "file read failed", ioe);
                  MessageBundle msg = new MessageBundle("platform", "JarServlet.fileOpenFailed", "JarServlet: File read failed");
                  Http.sendErrorXML(req, resp, 500, msg);
                  return;
               }

               resp.setStatus(200);
               resp.setHeader("Content-Type", Http.getMimeType(getExtension(contentPath)));
               return;
            } catch (AccessControlException e) {
               this.handleForbiddenAccess(req, resp, uri);
            } catch (IOException ioe) {
               Http.sendError(req, resp, 404);
            }

            return;
         }
      }
   }

   public static String getExtension(String filename) {
      int dotIndex = filename.indexOf(46);
      return dotIndex == -1 ? "" : filename.substring(dotIndex);
   }

   private boolean isAllowedPath(String path) {
      for (String declaredSymbolicLink : this.declaredSymbolicLinks) {
         if (path.startsWith(declaredSymbolicLink)) {
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.fine("path '" + path + "' matches declared external symlink '" + declaredSymbolicLink + "', granting access");
            }

            return true;
         }
      }

      return false;
   }

   private void handleForbiddenAccess(HttpServletRequest req, HttpServletResponse resp, String jarPath) {
      String username = this.getServer().getAuthenticator().getRequestUserName(req);
      this.filter.warning("user \"" + username + "\" made " + req.getMethod() + " request with forbidden path \"" + jarPath + "\"");
      MessageBundle msg = new MessageBundle("platform", "FileServlet.accessForbidden", jarPath, "FileServlet: Access to " + jarPath + " is forbidden");
      Http.sendErrorXML(req, resp, 403, msg);
   }
}
