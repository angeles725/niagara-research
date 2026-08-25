package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.file.FileStore;
import com.tridium.niagarad.file.FileStoreElement;
import com.tridium.niagarad.file.InvalidFileStoreElement;
import com.tridium.niagarad.file.KeyRingImportFileStoreElement;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.http.HttpDateFormat;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.HttpLogger;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.log.MulticastLogger;
import com.tridium.niagarad.log.SimpleErrorHandler;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.CachedPredicate;
import com.tridium.nre.platform.IPlatformProvider;
import com.tridium.nre.platform.OperatingSystemEnum;
import com.tridium.nre.platform.PlatformUtil;
import com.tridium.nre.security.EncryptionKeySource;
import com.tridium.nre.security.KeyRingFactory;
import com.tridium.nre.security.PBEDecryptingInputStream;
import com.tridium.nre.security.PBEEncodingKey;
import com.tridium.nre.security.PBEValidator;
import com.tridium.nre.security.SecretChars;
import com.tridium.nre.security.SimpleKeyRing;
import com.tridium.nre.security.io.AESStreamEncryption;
import com.tridium.nre.security.io.KeyRingEncryptingInputStream;
import com.tridium.nre.security.io.PBEEncryptingInputStream;
import com.tridium.nre.util.BogTranscoderInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.baja.nre.security.SharedSecretKey;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.nre.util.FileUtil;
import javax.baja.nre.util.TextUtil;
import javax.baja.nre.util.FileUtil.FileInfo;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.http.BadMessageException;

public class FileServlet extends Servlet {
   public static final String FILE_MAKE_DIRECTORY = "mkdir";
   public static final String FILE_FILESYSTEM_INFO = "fsinfo";
   public static final String FILE_TRANSACTION_INFO = "transactioninfo";
   public static final String FILE_TRANSACTION = "transaction";
   public static final String FILE_TRANSACTION_STANDALONE = "standalone";
   public static final String FILE_TRANSACTION_NONE = "none";
   public static final String FILE_TRANSACTION_NEW = "new";
   public static final String FILE_TRANSACTION_VALIDATE_ENCODING = "validateEncoding";
   public static final String FILE_TRANSACTION_COMMIT = "commit";
   public static final String FILE_TRANSACTION_ABORT = "abort";
   public static final String FILE_TRANSACTION_CURRENT = "current";
   public static final String FILE_TRANSACTION_ID = "transactionId";
   public static final String FILE_TRANSCODE = "transcode";
   public static final String FILE_RENAME = "rename";
   public static final String FILE_TRANSACTION_ENCODING_VALIDATOR = "encodingValidator";
   public static final String FILE_TRANSACTION_INITIALIZE_PBE = "initializePBE";
   public static final String FILE_TRANSACTION_ENCODING_PASS_PHRASE = "encodingPassPhrase";
   public static final String FILE_TRANSACTION_ENCODING_SALT = "encodingSalt";
   public static final String FILE_TRANSACTION_ENCODING_ITERATION_COUNT = "encodingIterationCount";
   public static final int MAX_POST_FORM_TRANSFER = 536870912;
   private final NiagaraDaemon niagaraDaemon;
   private final boolean isReadonly;
   private Logger filter;
   private String rootDir;
   private String canonicalRootDir;
   private final IPlatformProvider platformProvider;
   private static final Object PREDICATE_MONITOR = new Object();
   private static final char[] ILLEGAL_CHARACTERS = new char[]{'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"', ':'};
   private static final boolean REQUIRE_BOG_TRANSCODING = AccessController.doPrivileged(() -> Boolean.getBoolean("niagara.requireBogTranscoding"));
   private static final int READ_BUFFER_SIZE = 8192;
   private final ArrayList<String> declaredSymbolicLinks = new ArrayList<>();
   private final Map<String, String> uriMap = new HashMap<>();
   private static final Set<String> PASSPHRASE_ENCRYPTED_PATHS = new HashSet<>();
   private static final String TRIDIUM_LEGACY_PLATFORM_FILE_PATH = new File(NiagaraDaemon.NIAGARA_HOME, "platform")
      .getAbsolutePath()
      .toLowerCase(Locale.ENGLISH);
   private static final String TRIDIUM_LEGACY_WIFI_FILE_PATH = new File(TRIDIUM_LEGACY_PLATFORM_FILE_PATH, "wifi")
      .getAbsolutePath()
      .toLowerCase(Locale.ENGLISH);
   private static final String TRIDIUM_LEGACY_8021X_FILE_PATH = new File(TRIDIUM_LEGACY_PLATFORM_FILE_PATH, "ieee8021x")
      .getAbsolutePath()
      .toLowerCase(Locale.ENGLISH);
   private static final HashSet<String> TRIDIUM_LEGACY_SHADOW_FILE_PATHS = new HashSet<>(
      Arrays.asList("/etc/shadow", "/etc/passwd", "/etc/group", "/etc/oshadow", "/etc/opasswd", "/etc/ogroup", "/etc/nshadow", "/etc/npasswd", "/etc/ngroup")
   );
   private static final Predicate<String> IS_TRIDIUM_LEGACY_PLATFORM_ACCESS_PATH = (Objects::nonNull)
      .and(path -> path.toLowerCase(Locale.ENGLISH).startsWith(TRIDIUM_LEGACY_PLATFORM_FILE_PATH));
   private static final Predicate<String> IS_TRIDIUM_LEGACY_FORBIDDEN_PATH = (Objects::nonNull).and(TRIDIUM_LEGACY_SHADOW_FILE_PATHS::contains);
   private static final Predicate<String> IS_TRIDIUM_LEGACY_PASSPHRASE_ENCRYPTED_PATH = (Objects::nonNull)
      .and(
         path -> path.toLowerCase(Locale.ENGLISH).startsWith(TRIDIUM_LEGACY_WIFI_FILE_PATH)
            || path.toLowerCase(Locale.ENGLISH).startsWith(TRIDIUM_LEGACY_8021X_FILE_PATH)
      );
   private static final File SYSTEM_SECURITY_DIR = new File(NiagaraDaemon.NIAGARA_USER_HOME + File.separator + "security");
   private static final String KM_FILE_PATH = new File(SYSTEM_SECURITY_DIR, ".km").getAbsolutePath().toLowerCase(Locale.ENGLISH);
   private static final String SP_FILE_PATH = new File(SYSTEM_SECURITY_DIR, ".sp").getAbsolutePath().toLowerCase(Locale.ENGLISH);
   private final Predicate<String> IS_FORBIDDEN_PATH = (Objects::nonNull)
      .and(
         (path -> !path.toLowerCase(Locale.ENGLISH).startsWith(this.rootDir.toLowerCase(Locale.ENGLISH))
               && !path.toLowerCase(Locale.ENGLISH).startsWith(this.canonicalRootDir.toLowerCase(Locale.ENGLISH)))
            .or(path -> path.toLowerCase(Locale.ENGLISH).startsWith(KM_FILE_PATH) || path.toLowerCase(Locale.ENGLISH).startsWith(SP_FILE_PATH))
      );
   private static final Predicate<String> IS_KEYRING_ENCRYPTED_PATH = new CachedPredicate(
      (Objects::nonNull).and(Pattern.compile(".*[/\\\\]stations[/\\\\][^/\\\\]+[/\\\\]ldap[/\\\\](?!krb5\\.conf$).+").asPredicate()), 1000
   );
   private static final Predicate<String> IS_PASSPHRASE_ENCRYPTED_PATH = (Objects::nonNull)
      .and(
         path -> PASSPHRASE_ENCRYPTED_PATHS.stream().anyMatch(candidate -> path.toLowerCase(Locale.ENGLISH).startsWith(candidate.toLowerCase(Locale.ENGLISH)))
      );

   public FileServlet(String pServletName, String pRootDir, NiagaraDaemon pNiagaraDaemon, boolean pIsReadonly, IPlatformProvider platformProvider) {
      super(pServletName);
      this.niagaraDaemon = pNiagaraDaemon;
      this.isReadonly = pIsReadonly;
      this.rootDir = pRootDir;
      if (this.rootDir.endsWith("/") || this.rootDir.endsWith("\\")) {
         this.rootDir = this.rootDir.substring(0, this.rootDir.length() - 1);
      }

      this.platformProvider = platformProvider;
   }

   @Override
   public boolean doStart() {
      this.filter = Logger.getLogger("file");

      try {
         this.canonicalRootDir = new File(this.rootDir).getCanonicalPath();
      } catch (Exception e) {
         this.filter.log(Level.SEVERE, "failed to canonicalize servlet root directory '" + this.rootDir + "', can not start servlet", e);
         return false;
      }

      String symlinkList = null;
      if ("niagara".equals(this.getName())) {
         symlinkList = NiagaraDaemon.FILESTORE_NIAGARA_HOME_SYMLINKS;
      } else if ("niagara_user".equals(this.getName())) {
         symlinkList = NiagaraDaemon.FILESTORE_NIAGARA_USER_HOME_SYMLINKS;
      }

      if (symlinkList != null) {
         String[] symlinkListArray = TextUtil.split(symlinkList, File.pathSeparatorChar);

         for (String symlinkEntry : symlinkListArray) {
            String absoluteSymlinkPath = this.canonicalRootDir + File.separator + symlinkEntry;
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.fine("file servlet '" + this.getName() + "' adding external symlink exception at '" + absoluteSymlinkPath + "'");
            }

            this.declaredSymbolicLinks.add(absoluteSymlinkPath);
         }
      }

      return true;
   }

   @Override
   public void doPost(HttpServletRequest req, HttpServletResponse resp) {
      StringBuilder buffer = new StringBuilder();
      String queryString = req.getQueryString();
      KeyedList query = Http.getGetForm(queryString);
      if (this.filter.isLoggable(Level.FINE)) {
         buffer.append("FileServlet::doPost ").append(req.getRequestURI());
         if (queryString != null) {
            buffer.append(" ").append(queryString);
         }

         this.filter.fine(buffer.toString());
      }

      buffer = new StringBuilder();
      if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(req, query.get("csrfToken", null))) {
         MessageBundle msg = new MessageBundle("invalid CSRF token in request");
         this.filter.severe("invalid CSRF token in request");
         Http.sendErrorXML(req, resp, 403, msg);
      } else if (this.isReadonly) {
         buffer.append("uri '").append(this.getUriWithoutName(req.getRequestURI())).append("' not allowed on read-only file servlet (doPost)");
         MessageBundle msg = new MessageBundle("platform", "FileServlet.readonlyServlet", buffer.toString());
         this.filter.severe(buffer.toString());
         Http.sendErrorXML(req, resp, 403, msg);
      } else {
         int contentLength = req.getIntHeader("Content-Length");
         if (contentLength > 536870912) {
            MessageBundle msg = new MessageBundle(
               "content length " + contentLength + " exceeds maximum allowed transfer size " + 536870912 + ", rejecting request"
            );
            this.filter.severe("content length " + contentLength + " exceeds maximum allowed transfer size " + 536870912 + ", rejecting request");
            req.setAttribute("maxPostContentLength", 536870912);
            Http.sendErrorXML(req, resp, 413, msg);
         } else if (contentLength < 0) {
            MessageBundle msg = new MessageBundle("requested chunked input stream not supported, rejecting request");
            this.filter.severe("requested chunked input stream not supported, rejecting request");
            req.setAttribute("maxPostContentLength", 536870912);
            Http.sendErrorXML(req, resp, 415, msg);
         } else if (!this.niagaraDaemon.lockClient(250L)) {
            buffer.append("multiple client access conflict (doPost)");
            MessageBundle msg = new MessageBundle("platform", "NiagaraDaemon.multipleClient", buffer.toString());
            this.filter.severe(buffer.toString());
            Http.sendErrorXML(req, resp, 409, msg);
         } else {
            try {
               String uri = this.getUriWithoutName(req.getRequestURI());
               int len = req.getIntHeader("Content-Length");
               String contentType = req.getHeader("Content-Type");
               query = Http.getGetForm(queryString);
               String transactionParameter = query.get("transaction", null);
               String transactionId = query.get("transactionId", null);
               boolean mustCommit = false;
               FileStore store;
               if (transactionParameter == null || transactionParameter.isEmpty() || transactionParameter.equalsIgnoreCase("none")) {
                  store = makeStore(req, query, true, this.platformProvider);
                  mustCommit = true;
               } else if (!transactionParameter.equalsIgnoreCase("standalone") && !transactionParameter.equalsIgnoreCase("new")) {
                  if (transactionParameter.equalsIgnoreCase("commit")) {
                     buffer.append("attempt to commit a file transfer transaction using POST method");
                     MessageBundle msg = new MessageBundle("platform", "FileServlet.postedCommit", buffer.toString());
                     this.filter.severe(buffer.toString());
                     Http.sendErrorXML(req, resp, 400, msg);
                     return;
                  }

                  if (transactionParameter.equalsIgnoreCase("abort")) {
                     buffer.append("attempt to abort a file transfer transaction using POST method");
                     MessageBundle msg = new MessageBundle("platform", "FileServlet.postedAbort", buffer.toString());
                     this.filter.severe(buffer.toString());
                     Http.sendErrorXML(req, resp, 400, msg);
                     return;
                  }

                  if (!transactionParameter.equalsIgnoreCase("current")) {
                     buffer.append("bad parameter ").append(transactionParameter);
                     MessageBundle msg = new MessageBundle("platform", "FileServlet.badTransactionParm", transactionParameter, buffer.toString());
                     this.filter.severe(buffer.toString());
                     Http.sendErrorXML(req, resp, 400, msg);
                     return;
                  }

                  store = FileStore.getInstance(transactionId);
                  if (store == null) {
                     buffer.append("store error - transaction id ").append(transactionId).append(" not current");
                     MessageBundle msg = new MessageBundle("platform", "FileServlet.tidMismatch", transactionId, buffer.toString());
                     this.filter.severe(buffer.toString());
                     Http.sendErrorXML(req, resp, 400, msg);
                     return;
                  }
               } else {
                  store = makeStore(req, query, false, this.platformProvider);
                  if (transactionParameter.equalsIgnoreCase("standalone")) {
                     mustCommit = true;
                  } else {
                     resp.setHeader("transactionId", store.getId());
                  }
               }

               if (len == -1) {
                  buffer.append("size not specified in post ").append(transactionParameter);
                  this.filter.severe(buffer.toString());
                  Http.sendError(req, resp, 411);
               } else {
                  String filename;
                  try {
                     filename = this.makeAbsoluteFileName(uri);
                  } catch (AccessControlException e) {
                     this.handleForbiddenAccess(req, resp, uri);
                     return;
                  }

                  if (filename == null) {
                     Http.sendError(req, resp, 404);
                  } else if (this.isForbiddenPath(filename) && !this.isPlatformAccessPath(filename)) {
                     this.handleForbiddenAccess(req, resp, uri);
                  } else {
                     ErrorHandler errorHandler = new SimpleErrorHandler();
                     FileStoreElement elem = null;
                     if (!"application/x-nws-dir".equalsIgnoreCase(contentType)) {
                        if (uri.equals("/security/.kr")) {
                           elem = store.addElement(new KeyRingImportFileStoreElement(filename, len, this.filter, store));
                        } else {
                           elem = store.newElement(filename, len, errorHandler, this.filter);
                        }

                        if (isProtectedWithPassphrase(filename) || isProtectedWithKeyring(filename) || filename.endsWith(".bog")) {
                           elem.useStrictSizeChecks = false;
                        }

                        if (elem instanceof InvalidFileStoreElement) {
                           this.filter.severe(errorHandler.getLastError().getNonLocalizedMessage());
                           if (elem == InvalidFileStoreElement.TOO_LARGE) {
                              req.setAttribute("maxPostContentLength", 536870912);
                              Http.sendErrorXML(req, resp, 413, errorHandler.getLastError());
                              return;
                           }

                           if (elem == InvalidFileStoreElement.PERMISSION_DENIED) {
                              Http.sendErrorXML(req, resp, 403, errorHandler.getLastError());
                           } else {
                              Http.sendErrorXML(req, resp, 400, errorHandler.getLastError());
                           }

                           return;
                        }

                        if (elem == null) {
                           this.filter.severe(errorHandler.getLastError().getNonLocalizedMessage());
                           req.setAttribute("maxPostContentLength", 536870912);
                           Http.sendErrorXML(req, resp, 413, errorHandler.getLastError());
                           return;
                        }
                     }

                     if (elem == null) {
                        MessageBundle msg = new MessageBundle("unexpected null file store element");
                        Http.sendErrorXML(req, resp, 400, msg);
                        this.filter.severe(msg.getNonLocalizedMessage());
                     } else {
                        int rc;
                        try {
                           rc = this.writeFile(filename, elem, req.getInputStream(), len, req);
                        } catch (IOException | BadMessageException e) {
                           MessageBundle msg = new MessageBundle("error occurred while writing file: " + filename + " (" + e + ")");
                           this.filter.severe(msg.getNonLocalizedMessage());
                           if (this.filter.isLoggable(Level.FINE)) {
                              this.filter.log(Level.FINE, "Stack trace: ", e);
                           }

                           if (e instanceof IOException) {
                              rc = 500;
                           } else {
                              rc = 408;
                           }
                        }

                        if (rc >= 400) {
                           FileStore.abortInstance(this.filter);
                           resp.setHeader("transactionId", null);
                           MessageBundle msg = new MessageBundle("bad return code to commit file instance");
                           Http.sendErrorXML(req, resp, rc, msg);
                        } else if (!mustCommit || FileStore.commitInstance(this.filter)) {
                           resp.setHeader("Location", req.getRequestURI());
                           Http.sendError(req, resp, 201);
                        } else {
                           MessageBundle msg = new MessageBundle("failed to commit file instance");
                           Http.sendErrorXML(req, resp, 500, msg);
                        }
                     }
                  }
               }
            } finally {
               this.niagaraDaemon.unlockClient();
            }
         }
      }
   }

   @Override
   public void doGet(HttpServletRequest req, HttpServletResponse resp) {
      StringBuilder buffer = new StringBuilder();
      String queryString = req.getQueryString();
      if (this.filter.isLoggable(Level.FINE)) {
         buffer.append("FileServlet::doGet ").append(req.getRequestURI());
         if (queryString != null) {
            buffer.append(" ").append(queryString);
         }

         this.filter.fine(buffer.toString());
      }

      buffer = new StringBuilder();
      KeyedList query = Http.getGetForm(queryString);
      String transactionParameter = query.get("transaction", null);
      String transactionId = query.get("transactionId", null);
      String rename = query.get("rename", null);
      FileStore store = null;
      boolean mustCommit = false;
      boolean mustLock = false;
      boolean mkDirParameter = Boolean.parseBoolean(query.get("mkdir", "false"));
      boolean fsInfoParameter = Boolean.parseBoolean(query.get("fsinfo", "false"));
      boolean transactionInfoParameter = Boolean.parseBoolean(query.get("transactioninfo", "false"));
      if ((
            transactionInfoParameter && query.containsKey("initializePBE")
               || mkDirParameter
               || rename != null
               || transactionParameter != null && (transactionParameter.equalsIgnoreCase("commit") || transactionParameter.equalsIgnoreCase("abort"))
         )
         && !DebugServlet.debugEnabled
         && !CsrfTokenUtil.verifyCsrfToken(req, query.get("csrfToken", null))) {
         MessageBundle msg = new MessageBundle("invalid CSRF token in request");
         this.filter.severe("invalid CSRF token in request");
         Http.sendErrorXML(req, resp, 403, msg);
      } else if ((mkDirParameter || rename != null) && this.isReadonly) {
         buffer.append("uri '").append(this.getUriWithoutName(req.getRequestURI())).append("' not allowed on read-only file servlet (doGet)");
         MessageBundle msg = new MessageBundle("platform", "FileServlet.readonlyServlet", buffer.toString());
         this.filter.severe(buffer.toString());
         Http.sendErrorXML(req, resp, 403, msg);
      } else if (transactionInfoParameter) {
         store = makeStore(req, query, false, this.platformProvider);
         ByteBuffer xmlBuffer = new ByteBuffer();
         XWriter xml = new XWriter();
         xml.setOutputStream(xmlBuffer.getOutputStream());
         xml.w("<fileTransaction").w(' ').attr("transactionId", store.getId());
         if (store.hasPBEKey() || query.containsKey("initializePBE")) {
            PBEEncodingKey pbeEncodingKey = store.getPBEKey();
            xml.w(' ')
               .attr("encodingValidator", pbeEncodingKey.getEncodedValidator())
               .w(' ')
               .attr("encodingIterationCount", String.valueOf(pbeEncodingKey.getEncodingIterationCount()))
               .w(' ')
               .attr("encodingSalt", pbeEncodingKey.getEncodingSaltHex());
         }

         xml.w("/>\n");
         xml.flush();
         xml.close();
         byte[] bytes = xmlBuffer.toByteArray();
         resp.setHeader("Content-Type", "text/xml");
         resp.setIntHeader("Content-Length", bytes.length);

         try {
            resp.getOutputStream().write(bytes);
         } catch (Exception ioe) {
            if (this.getServer() != null && this.getServer().getState() == 1) {
               this.filter.severe("error occurred while writing file transaction information (" + ioe + ")");
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter.log(Level.FINE, "Stack trace: ", ioe);
               }

               Http.sendError(req, resp, 500);
            }
         }
      } else {
         if (mkDirParameter
            || rename != null
            || transactionParameter != null && (transactionParameter.equalsIgnoreCase("commit") || transactionParameter.equalsIgnoreCase("abort"))) {
            mustLock = true;
         }

         if (!this.niagaraDaemon.lockClient(250L)) {
            buffer.append("multiple client access conflict (doGet)");
            MessageBundle msg = new MessageBundle("platform", "NiagaraDaemon.multipleClient", buffer.toString());
            this.filter.severe(buffer.toString());
            Http.sendErrorXML(req, resp, 409, msg);
         } else {
            try {
               if (!mustLock) {
                  this.niagaraDaemon.unlockClient();
               }

               if (fsInfoParameter) {
                  String uri = this.getUriWithoutName(req.getRequestURI());

                  String filename;
                  try {
                     filename = this.makeAbsoluteFileName(uri);
                  } catch (AccessControlException e) {
                     this.handleForbiddenAccess(req, resp, uri);
                     return;
                  }

                  this.sendFilesystemInfo(this.filter, filename, resp);
               } else {
                  if (transactionParameter != null) {
                     if (transactionParameter.equalsIgnoreCase("commit")) {
                        HttpLogger httpLogger = null;
                        if (mkDirParameter) {
                           MessageBundle msg = new MessageBundle("commit and mkDir cannot be sent in the same request");
                           this.filter.severe("commit and mkDir cannot be sent in the same request");
                           Http.sendErrorXML(req, resp, 400, msg);
                        } else if (rename != null) {
                           MessageBundle msg = new MessageBundle("commit and rename cannot be sent in the same request");
                           this.filter.severe("commit and rename cannot be sent in the same request");
                           Http.sendErrorXML(req, resp, 400, msg);
                        } else {
                           store = FileStore.getInstance(transactionId);
                           if (store == null) {
                              buffer.append("commit error - transaction id ").append(transactionId).append(" not current");
                              MessageBundle msg = new MessageBundle("platform", "FileServlet.tidMismatch", transactionId, buffer.toString());
                              this.filter.severe(buffer.toString());
                              Http.sendErrorXML(req, resp, 400, msg);
                           } else {
                              httpLogger = new HttpLogger(this.filter.getName(), resp);
                              Logger[] loggers = new Logger[]{httpLogger, this.filter};
                              MulticastLogger multi = new MulticastLogger(loggers);
                              if (!FileStore.commitInstance(multi)) {
                                 MessageBundle msg = new MessageBundle("failed to commit file instance");
                                 Http.sendErrorXML(req, resp, 500, msg);
                                 return;
                              }
                           }
                        }

                        if (httpLogger != null) {
                           httpLogger.finish();
                        }

                        return;
                     }

                     if (transactionParameter.equalsIgnoreCase("abort")) {
                        if (mkDirParameter) {
                           MessageBundle msg = new MessageBundle("commit and mkDir cannot be sent in the same request");
                           this.filter.severe("commit and mkDir cannot be sent in the same request");
                           Http.sendErrorXML(req, resp, 400, msg);
                           return;
                        }

                        if (rename != null) {
                           MessageBundle msg = new MessageBundle("commit and rename cannot be sent in the same request");
                           this.filter.severe("commit and rename cannot be sent in the same request");
                           Http.sendErrorXML(req, resp, 400, msg);
                           return;
                        }

                        if (FileStore.isTransactionCancelled(transactionId)) {
                           MessageBundle msg = new MessageBundle("platform", "NiagaraDaemon.abortSuccessful", "abort successful");
                           this.filter.fine("abort successful");
                           Http.sendErrorXML(req, resp, 200, msg);
                           return;
                        }

                        store = FileStore.getInstance(transactionId);
                        if (store == null) {
                           buffer.append("abort error - transaction id ").append(transactionId).append(" not current");
                           MessageBundle msg = new MessageBundle("platform", "FileServlet.tidMismatch", transactionId, buffer.toString());
                           this.filter.severe(buffer.toString());
                           Http.sendErrorXML(req, resp, 400, msg);
                        } else {
                           FileStore.abortInstance(this.filter);
                           MessageBundle msg = new MessageBundle("platform", "NiagaraDaemon.abortSuccessful", "abort successful");
                           this.filter.fine("abort successful");
                           Http.sendErrorXML(req, resp, 200, msg);
                        }

                        return;
                     }

                     if (transactionId == null) {
                        store = makeStore(req, query, false, this.platformProvider);
                     } else {
                        store = FileStore.getInstance(transactionId);
                        if (store == null) {
                           buffer.append("commit error - transaction id ").append(transactionId).append(" not current");
                           MessageBundle msg = new MessageBundle("platform", "FileServlet.tidMismatch", transactionId, buffer.toString());
                           this.filter.severe(buffer.toString());
                           Http.sendErrorXML(req, resp, 400, msg);
                           return;
                        }
                     }
                  }

                  String uri = this.getUriWithoutName(req.getRequestURI());

                  String filename;
                  try {
                     filename = this.makeAbsoluteFileName(uri);
                  } catch (AccessControlException e) {
                     this.handleForbiddenAccess(req, resp, uri);
                     return;
                  }

                  if (filename == null) {
                     Http.sendError(req, resp, 404);
                  } else if (this.isForbiddenPath(filename) && !this.isPlatformAccessPath(filename)) {
                     this.handleForbiddenAccess(req, resp, uri);
                  } else if (!mkDirParameter && rename == null) {
                     this.sendFile(filename, uri, req, resp, false, store);
                  } else {
                     if (transactionParameter == null || transactionParameter.isEmpty() || transactionParameter.equalsIgnoreCase("none")) {
                        store = makeStore(req, query, true, this.platformProvider);
                        mustCommit = true;
                     } else if (!transactionParameter.equalsIgnoreCase("standalone") && !transactionParameter.equalsIgnoreCase("new")) {
                        if (!transactionParameter.equalsIgnoreCase("current")) {
                           buffer.append("bad parameter ").append(transactionParameter);
                           MessageBundle msg = new MessageBundle("platform", "FileServlet.badTransactionParm", transactionParameter, buffer.toString());
                           this.filter.severe(buffer.toString());
                           Http.sendErrorXML(req, resp, 400, msg);
                           return;
                        }

                        store = FileStore.getInstance(transactionId);
                        if (store == null) {
                           buffer.append("store error - transaction id ").append(transactionId).append(" not current");
                           MessageBundle msg = new MessageBundle("platform", "FileServlet.tidMismatch", transactionId, buffer.toString());
                           this.filter.severe(buffer.toString());
                           Http.sendErrorXML(req, resp, 400, msg);
                           return;
                        }
                     } else {
                        store = makeStore(req, query, false, this.platformProvider);
                        if (transactionParameter.equalsIgnoreCase("standalone")) {
                           mustCommit = true;
                        } else {
                           resp.setHeader("transactionId", store.getId());
                        }
                     }

                     if (mkDirParameter) {
                        store.newMkDirElement(filename);
                     } else {
                        for (char ILLEGAL_CHARACTER : ILLEGAL_CHARACTERS) {
                           if (rename.indexOf(ILLEGAL_CHARACTER) != -1) {
                              MessageBundle msg = new MessageBundle(
                                 "platform", "FileServlet.badRenameValue", "invalid character '" + ILLEGAL_CHARACTER + "' in rename value " + rename
                              );
                              this.filter.severe("invalid character '" + ILLEGAL_CHARACTER + "' in rename value " + rename);
                              Http.sendErrorXML(req, resp, 400, msg);
                              return;
                           }
                        }

                        uri = this.getUriWithoutName(req.getRequestURI());

                        String sourcePath;
                        try {
                           sourcePath = this.makeAbsoluteFileName(uri);
                        } catch (AccessControlException e) {
                           this.handleForbiddenAccess(req, resp, uri);
                           return;
                        }

                        StringBuilder destPath = new StringBuilder();
                        if (this.isForbiddenPath(sourcePath) && !this.isPlatformAccessPath(sourcePath)) {
                           this.handleForbiddenAccess(req, resp, uri);
                           return;
                        }

                        int result = getRenamePath(sourcePath, rename, destPath);
                        if (result != 204) {
                           resp.setIntHeader("Content-Length", 0);
                           Http.sendError(req, resp, result);
                           return;
                        }

                        String destinationPath = destPath.toString().trim();
                        if (this.isForbiddenPath(destinationPath) && !this.isPlatformAccessPath(destinationPath)) {
                           this.handleForbiddenAccess(req, resp, uri);
                           return;
                        }

                        store.newRenameElement(sourcePath, destinationPath);
                     }

                     if (!mustCommit) {
                        MessageBundle msg = new MessageBundle("platform", "NiagaraDaemon.transferSuccessful", "transfer successful");
                        Http.sendErrorXML(req, resp, 200, msg);
                     } else {
                        HttpLogger httpLogger = new HttpLogger(this.filter.getName(), resp);
                        Logger[] loggers = new Logger[]{httpLogger, this.filter};
                        MulticastLogger multi = new MulticastLogger(loggers);
                        if (FileStore.commitInstance(multi)) {
                           httpLogger.finish();
                        } else {
                           MessageBundle msg = new MessageBundle("failed to commit file instance");
                           Http.sendErrorXML(req, resp, 500, msg);
                        }
                     }
                  }
               }
            } finally {
               if (mustLock) {
                  this.niagaraDaemon.unlockClient();
               }
            }
         }
      }
   }

   private static FileStore makeStore(HttpServletRequest request, KeyedList query, boolean isAutoCommit, IPlatformProvider platformProvider) {
      try {
         String encodedValidator = query.get("encodingValidator", null);
         if (encodedValidator != null && !encodedValidator.startsWith("[null")) {
            String encodedPassPhrase = query.get("encodingPassPhrase", null);
            if (encodedPassPhrase == null) {
               SecretChars secretChars = platformProvider.getSystemPassword();
               Throwable var39 = null;

               try {
                  return FileStore.make(
                     isAutoCommit, encodedValidator, query.get("encodingSalt", null), Integer.parseInt(query.get("encodingIterationCount", "-1")), secretChars
                  );
               } catch (Throwable var33) {
                  var39 = var33;
                  throw var33;
               } finally {
                  if (secretChars != null) {
                     if (var39 != null) {
                        try {
                           secretChars.close();
                        } catch (Throwable var32) {
                           var39.addSuppressed(var32);
                        }
                     } else {
                        secretChars.close();
                     }
                  }
               }
            } else {
               String sharedKeyAttributeName = "sharedKey_" + query.get("sharedKeyName", null);
               SharedSecretKey sharedKey = (SharedSecretKey)request.getSession(false).getAttribute(sharedKeyAttributeName);
               SecretChars secretChars = sharedKey.decryptChars(Base64.getDecoder().decode(encodedPassPhrase));
               Throwable var9 = null;

               try {
                  return FileStore.make(
                     isAutoCommit, encodedValidator, query.get("encodingSalt", null), Integer.parseInt(query.get("encodingIterationCount", "-1")), secretChars
                  );
               } catch (Throwable var34) {
                  var9 = var34;
                  throw var34;
               } finally {
                  if (secretChars != null) {
                     if (var9 != null) {
                        try {
                           secretChars.close();
                        } catch (Throwable var31) {
                           var9.addSuppressed(var31);
                        }
                     } else {
                        secretChars.close();
                     }
                  }
               }
            }
         } else {
            return FileStore.make(isAutoCommit);
         }
      } catch (Exception e) {
         return FileStore.make(isAutoCommit);
      }
   }

   @Override
   public void doHead(HttpServletRequest req, HttpServletResponse resp) {
      StringBuilder buffer = new StringBuilder();
      String queryString = req.getQueryString();
      if (this.filter.isLoggable(Level.FINE)) {
         buffer.append("FileServlet::doHead ").append(req.getRequestURI());
         if (queryString != null) {
            buffer.append(" ").append(queryString);
         }

         this.filter.fine(buffer.toString());
      }

      buffer = new StringBuilder();
      if (!this.niagaraDaemon.lockClient(250L)) {
         buffer.append("multiple client access conflict (doHead)");
         MessageBundle msg = new MessageBundle("platform", "NiagaraDaemon.multipleClient", buffer.toString());
         this.filter.severe(buffer.toString());
         Http.sendErrorXML(req, resp, 409, msg);
      } else {
         try {
            KeyedList query = Http.getGetForm(queryString);
            if ("validateEncoding".equalsIgnoreCase(query.get("transaction", null))) {
               sendEncodingValidationResponse(this.filter, query, req, resp, this.platformProvider);
               return;
            }

            String uri = this.getUriWithoutName(req.getRequestURI());

            String filename;
            try {
               filename = this.makeAbsoluteFileName(uri);
            } catch (AccessControlException e) {
               this.handleForbiddenAccess(req, resp, uri);
               return;
            }

            if (filename != null) {
               this.sendFile(filename, uri, req, resp, true, null);
               return;
            }

            Http.sendError(req, resp, 404);
         } finally {
            this.niagaraDaemon.unlockClient();
         }
      }
   }

   @Override
   public void doDelete(HttpServletRequest req, HttpServletResponse resp) {
      StringBuilder buffer = new StringBuilder();
      String queryString = req.getQueryString();
      KeyedList query = Http.getGetForm(queryString);
      if (this.filter.isLoggable(Level.FINE)) {
         buffer.append("FileServlet::doDelete ").append(req.getRequestURI());
         if (queryString != null) {
            buffer.append(" ").append(queryString);
         }

         this.filter.fine(buffer.toString());
      }

      buffer = new StringBuilder();
      if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(req, query.get("csrfToken", null))) {
         MessageBundle msg = new MessageBundle("invalid CSRF token in request");
         this.filter.severe("invalid CSRF token in request");
         Http.sendErrorXML(req, resp, 403, msg);
      } else if (this.isReadonly) {
         buffer.append("uri '").append(this.getUriWithoutName(req.getRequestURI())).append("' not allowed on read-only file servlet (doDelete)");
         MessageBundle msg = new MessageBundle("platform", "FileServlet.readonlyServlet", buffer.toString());
         this.filter.severe(buffer.toString());
         Http.sendErrorXML(req, resp, 403, msg);
      } else if (!this.niagaraDaemon.lockClient(250L)) {
         buffer.append("multiple client access conflict (doDelete)");
         MessageBundle msg = new MessageBundle("platform", "NiagaraDaemon.multipleClient", buffer.toString());
         this.filter.severe(buffer.toString());
         Http.sendErrorXML(req, resp, 409, msg);
      } else {
         try {
            String uri = this.getUriWithoutName(req.getRequestURI());

            String filename;
            try {
               filename = this.makeAbsoluteFileName(uri);
            } catch (AccessControlException e) {
               this.handleForbiddenAccess(req, resp, uri);
               return;
            }

            if (filename == null) {
               Http.sendError(req, resp, 200);
               return;
            }

            if (this.isForbiddenPath(filename) && !this.isPlatformAccessPath(filename)) {
               this.handleForbiddenAccess(req, resp, uri);
               return;
            }

            String transactionParameter = "";
            String transactionId = "";
            if (queryString != null && !queryString.isEmpty()) {
               query = Http.getGetForm(queryString);
               transactionParameter = query.get("transaction", null);
               transactionId = query.get("transactionId", null);
            } else {
               query = new KeyedList();
            }

            boolean mustCommit = false;
            FileStore store;
            if (transactionParameter == null || transactionParameter.isEmpty() || transactionParameter.equalsIgnoreCase("none")) {
               store = makeStore(req, query, true, this.platformProvider);
               mustCommit = true;
            } else if (!transactionParameter.equalsIgnoreCase("standalone") && !transactionParameter.equalsIgnoreCase("new")) {
               if (!transactionParameter.equalsIgnoreCase("current")) {
                  buffer.append("bad parameter ").append(transactionParameter);
                  MessageBundle msg = new MessageBundle("platform", "FileServlet.badTransactionParameter", transactionParameter, buffer.toString());
                  this.filter.severe(buffer.toString());
                  Http.sendErrorXML(req, resp, 400, msg);
                  return;
               }

               store = FileStore.getInstance(transactionId);
               if (store == null) {
                  buffer.append("store error - transaction id ").append(transactionId).append(" not current");
                  MessageBundle msg = new MessageBundle("platform", "FileServlet.tidMismatch", transactionId, buffer.toString());
                  this.filter.severe(buffer.toString());
                  Http.sendErrorXML(req, resp, 400, msg);
                  return;
               }
            } else {
               store = makeStore(req, query, false, this.platformProvider);
               if (transactionParameter.equalsIgnoreCase("standalone")) {
                  mustCommit = true;
               } else {
                  resp.setHeader("transactionId", store.getId());
               }
            }

            ErrorHandler errorHandler = new SimpleErrorHandler();
            FileStoreElement elem = store.newDeleteElement(filename, errorHandler, this.filter);
            if (!(elem instanceof InvalidFileStoreElement)) {
               if (!mustCommit) {
                  buffer.append("delete successful");
                  MessageBundle msg = new MessageBundle("platform", "NiagaraDaemon.deleteSuccessful", buffer.toString());
                  if (this.filter.isLoggable(Level.FINE)) {
                     this.filter.fine(buffer.toString());
                  }

                  Http.sendErrorXML(req, resp, 200, msg);
                  return;
               }

               HttpLogger httpLogger = new HttpLogger(this.filter.getName(), resp);
               Logger[] loggers = new Logger[]{httpLogger, this.filter};
               MulticastLogger multi = new MulticastLogger(loggers);
               if (FileStore.commitInstance(multi)) {
                  httpLogger.finish();
                  return;
               }

               MessageBundle msg = new MessageBundle("failed to commit file instance");
               Http.sendErrorXML(req, resp, 500, msg);
               return;
            }

            this.filter.severe(errorHandler.getLastError().getNonLocalizedMessage());
            if (elem == InvalidFileStoreElement.TOO_LARGE) {
               req.setAttribute("maxPostContentLength", 536870912);
               Http.sendErrorXML(req, resp, 413, errorHandler.getLastError());
               return;
            }

            if (elem == InvalidFileStoreElement.PERMISSION_DENIED) {
               Http.sendErrorXML(req, resp, 403, errorHandler.getLastError());
            } else {
               Http.sendErrorXML(req, resp, 400, errorHandler.getLastError());
            }
         } finally {
            this.niagaraDaemon.unlockClient();
         }
      }
   }

   private String makeAbsoluteFileName(String uri) {
      String decodedUri = Http.decodeUri(uri);
      String mappedUri = this.uriMap.get(decodedUri);
      if (mappedUri != null) {
         return mappedUri;
      }

      String fixedUri = decodedUri;
      fixedUri = fixedUri.replace('/', File.separatorChar);
      int uriLen = fixedUri.length() + this.rootDir.length() + 1;
      String filename;
      if (uriLen == 1) {
         filename = "/";
      } else {
         filename = this.rootDir + fixedUri;
      }

      File file = new File(filename);
      if (file.exists()) {
         filename = file.getAbsolutePath();
      }

      return filename;
   }

   private int writeFile(String fileName, FileStoreElement elem, InputStream in, int len, HttpServletRequest req) {
      try {
         boolean checkReadAgainstLen = true;
         if (isProtectedWithPassphrase(fileName)) {
            try {
               in = AESStreamEncryption.ifEncrypted(
                  in,
                  pbeEncryptedContents -> new PBEDecryptingInputStream(pbeEncryptedContents, elem.getStore().getPBEKey()),
                  unencryptedContents -> unencryptedContents
               );
               checkReadAgainstLen = false;
            } catch (Exception e) {
               elem.close();
               this.filter.severe("error occurred while writing file: " + fileName + " (" + e + ")");
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter.log(Level.FINE, "Stack trace: ", e);
               }

               return 400;
            }
         } else if (isProtectedWithKeyring(fileName)) {
            try {
               in = AESStreamEncryption.ifEncrypted(
                  in,
                  pbeEncryptedContents -> AESStreamEncryption.pbeToKeyRing(
                     pbeEncryptedContents, elem.getStore().getPBEKey(), NiagaraDaemon.getSecurityInfoProvider()
                  ),
                  unencryptedContents -> new KeyRingEncryptingInputStream(unencryptedContents, NiagaraDaemon.getSecurityInfoProvider())
               );
               checkReadAgainstLen = false;
            } catch (Exception e) {
               elem.close();
               this.filter.severe("error occurred while writing file: " + fileName + " (" + e + ")");
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter.log(Level.FINE, "Stack trace: ", e);
               }

               return 400;
            }
         } else if (fileName.endsWith(".bog")) {
            try {
               in = new BogTranscoderInputStream(
                  NiagaraDaemon.getSecurityInfoProvider().getKeyRing(), in, true, elem.getStore().getPBEKey(), EncryptionKeySource.keyring, fileName
               );
               checkReadAgainstLen = false;
            } catch (SecurityException e) {
               elem.close();
               this.filter.severe("system pass phrase does not match password for file: " + fileName);
               return 400;
            } catch (IOException e) {
               elem.close();
               this.filter.severe("error occurred while writing file: " + fileName + " (" + e + ")");
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter.log(Level.FINE, "Stack trace: ", e);
               }

               return 400;
            }
         }

         if (len > 0) {
            byte[] buf = new byte[8192];
            int remaining = len;

            try {
               int thisRead;
               if (checkReadAgainstLen) {
                  while (remaining > 0 && (thisRead = in.read(buf, 0, Math.min(remaining, 8192))) > 0) {
                     int rc = elem.write(buf, thisRead, this.filter);
                     if (rc < 0) {
                        elem.close();
                        this.filter.severe("error occurred while writing file: " + fileName);
                        return 500;
                     }

                     remaining -= rc;
                     this.getServer().touchRequestSession(req);
                  }
               } else {
                  while ((thisRead = in.read(buf, 0, 8192)) > 0) {
                     int rc = elem.write(buf, thisRead, this.filter);
                     if (rc < 0) {
                        elem.close();
                        this.filter.severe("error occurred while writing file: " + fileName);
                        return 500;
                     }

                     this.getServer().touchRequestSession(req);
                  }
               }
            } catch (IOException e) {
               this.filter.severe("exception occurred while writing file: " + fileName + " (" + e + ")");
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter.log(Level.FINE, "Stack trace: ", e);
               }

               return 500;
            }
         }

         elem.close();
         return 201;
      } finally {
         try {
            in.close();
         } catch (Exception var30) {
         }
      }
   }

   private void sendFile(String fileName, String uri, HttpServletRequest req, HttpServletResponse resp, boolean headerOnly, FileStore store) {
      if (this.isForbiddenPath(fileName) && !this.isPlatformAccessPath(fileName)) {
         this.handleForbiddenAccess(req, resp, uri);
      } else if (new File(fileName).isDirectory()) {
         if (headerOnly) {
            resp.setHeader("Content-Type", "application/x-baja-directory");
            resp.setIntHeader("Content-Length", 0);
         } else {
            resp.setHeader("Content-Type", "text/xml");
            String dirName;
            if (uri.isEmpty()) {
               dirName = "/";
            } else {
               String s = uri;
               if (s.length() > 1) {
                  if (s.charAt(s.length() - 1) == '/') {
                     s = s.substring(0, s.length() - 1);
                  }

                  int separatorIndex = s.lastIndexOf(47);
                  s = s.substring(separatorIndex + 1);
               }

               dirName = s;
            }

            KeyedList query = Http.getGetForm(req.getQueryString());
            boolean recurse = Boolean.parseBoolean(query.get("recurse", "true"));
            boolean sendCrc = Boolean.parseBoolean(query.get("sendCrc", "false"));
            boolean sendPath = Boolean.parseBoolean(query.get("sendPath", "false"));
            this.sendDirectory(fileName, dirName, resp, recurse, sendCrc, sendPath);
         }
      } else {
         KeyedList query = Http.getGetForm(req.getQueryString());
         boolean sendCrc = Boolean.parseBoolean(query.get("sendCrc", "false"));
         File targetFile = new File(fileName);
         if (!targetFile.exists()) {
            Http.sendError(req, resp, 404);
         } else {
            PBEEncodingKey pbeKey = null;
            boolean closePbeKey = false;

            try {
               if (headerOnly) {
                  String mimeField = Http.getMimeType(FileUtil.getExtension(fileName));
                  resp.setHeader("Content-Type", mimeField);
                  resp.setDateHeader("Last-Modified", targetFile.lastModified());
                  FileInfo fileInfo;
                  if (!fileName.endsWith(".bog") || !REQUIRE_BOG_TRANSCODING && !Boolean.parseBoolean(query.get("transcode", "true"))) {
                     if (targetFile.getName().equals(".kr") && targetFile.getParentFile().equals(SYSTEM_SECURITY_DIR)) {
                        fileInfo = new FileInfo();
                     } else if (!isProtectedWithPassphrase(fileName) && !isProtectedWithKeyring(fileName)) {
                        if (fileName.endsWith(".bog") && this.filter.isLoggable(Level.FINE)) {
                           this.filter.fine("returning standard header information for bog file '" + targetFile.getName() + "' at client request");
                        }

                        if (sendCrc) {
                           fileInfo = FileUtil.getFileInfo(targetFile);
                        } else {
                           fileInfo = new FileInfo();
                           fileInfo.size = targetFile.length();
                        }
                     } else {
                        fileInfo = new FileInfo();
                     }
                  } else {
                     fileInfo = new FileInfo();
                  }

                  if (sendCrc) {
                     resp.setHeader("File-CRC", String.valueOf(fileInfo.crc));
                  }

                  resp.setHeader("Content-Length", String.valueOf(fileInfo.size));
               } else {
                  FileInfo fileInfo;
                  InputStream fin;
                  try {
                     if (!fileName.endsWith(".bog") || !REQUIRE_BOG_TRANSCODING && !Boolean.parseBoolean(query.get("transcode", "true"))) {
                        if (targetFile.getName().equals(".kr") && targetFile.getParentFile().equals(SYSTEM_SECURITY_DIR)) {
                           pbeKey = store == null ? PlatformUtil.makePBEKey() : store.getPBEKey();
                           closePbeKey = store == null;
                           SimpleKeyRing kr = (SimpleKeyRing)KeyRingFactory.getInstance(SYSTEM_SECURITY_DIR, ".kr", ".km").getKeyRing();
                           byte[] bytes = kr.exportKeyData(pbeKey);
                           fileInfo = FileUtil.getFileInfo(bytes);
                           fin = new ByteArrayInputStream(bytes);
                        } else if (isProtectedWithPassphrase(fileName)) {
                           pbeKey = store == null ? PlatformUtil.makePBEKey() : store.getPBEKey();
                           closePbeKey = store == null;
                           fileInfo = null;
                           fin = new PBEEncryptingInputStream(Files.newInputStream(targetFile.toPath()), pbeKey);
                        } else if (isProtectedWithKeyring(fileName)) {
                           pbeKey = store == null ? PlatformUtil.makePBEKey() : store.getPBEKey();
                           closePbeKey = store == null;
                           PBEEncodingKey keyForLambda = pbeKey;
                           fileInfo = null;
                           fin = AESStreamEncryption.ifEncrypted(
                              Files.newInputStream(targetFile.toPath()),
                              keyringEncryptedContents -> AESStreamEncryption.keyRingToPBE(
                                 Files.newInputStream(targetFile.toPath()), NiagaraDaemon.getSecurityInfoProvider(), keyForLambda
                              ),
                              unencryptedContents -> new PBEEncryptingInputStream(unencryptedContents, keyForLambda)
                           );
                        } else {
                           if (fileName.endsWith(".bog") && this.filter.isLoggable(Level.FINE)) {
                              this.filter.fine("returning standard input stream for bog file '" + targetFile.getName() + "' at client request");
                           }

                           if (sendCrc) {
                              fileInfo = FileUtil.getFileInfo(targetFile);
                           } else {
                              fileInfo = new FileInfo();
                              fileInfo.size = targetFile.length();
                           }

                           fin = Files.newInputStream(targetFile.toPath());
                        }
                     } else {
                        if (this.filter.isLoggable(Level.FINE)) {
                           this.filter.fine("creating new BogTranscoderInputStream for requested file '" + targetFile.getName() + "'...");
                        }

                        pbeKey = store == null ? PlatformUtil.makePBEKey() : store.getPBEKey();
                        closePbeKey = store == null;
                        fileInfo = null;
                        fin = new BogTranscoderInputStream(
                           NiagaraDaemon.getSecurityInfoProvider().getKeyRing(),
                           Files.newInputStream(targetFile.toPath()),
                           true,
                           pbeKey,
                           EncryptionKeySource.external,
                           fileName
                        );
                     }
                  } catch (Exception e) {
                     this.filter.severe("exception occurred while reading file: " + fileName + " (" + e + ")");
                     if (this.filter.isLoggable(Level.FINE)) {
                        this.filter.log(Level.FINE, "Stack trace: ", e);
                     }

                     Http.sendError(req, resp, 404);
                     return;
                  }

                  byte[] buf = new byte[8192];
                  String mimeField = Http.getMimeType(FileUtil.getExtension(fileName));
                  resp.setHeader("Content-Type", mimeField);
                  if (fileInfo == null) {
                     resp.setHeader("Transfer-Encoding", "chunked");
                  } else {
                     resp.setHeader("Content-Length", String.valueOf(fileInfo.size));
                     if (sendCrc) {
                        resp.setHeader("File-CRC", String.valueOf(fileInfo.crc));
                     }
                  }

                  resp.setDateHeader("Last-Modified", targetFile.lastModified());
                  long toTransfer = fileInfo == null ? Long.MAX_VALUE : fileInfo.size;
                  long total = 0L;
                  long chunkedCounter = 0L;

                  while (total < toTransfer) {
                     int bytesRead;
                     try {
                        bytesRead = fin.read(buf);
                     } catch (IOException ioe) {
                        this.filter.severe("exception occurred while reading file '" + fileName + "' input stream (" + ioe + ")");
                        if (this.filter.isLoggable(Level.FINE)) {
                           this.filter.log(Level.FINE, "Stack trace: ", ioe);
                        }

                        try {
                           fin.close();
                        } catch (IOException var33) {
                        }

                        Http.sendError(req, resp, 404);
                        return;
                     }

                     if (bytesRead <= 0) {
                        break;
                     }

                     try {
                        resp.getOutputStream().write(buf, 0, bytesRead);
                        if (fileInfo == null && this.filter.isLoggable(Level.FINEST)) {
                           chunkedCounter += bytesRead;
                           if (chunkedCounter > 256000L) {
                              String percentString = "...";
                              long targetFileLength = targetFile.length();
                              if (targetFileLength > 0L) {
                                 percentString = " of approximate " + targetFileLength + " bytes (" + total * 100L / targetFileLength + "%)";
                              }

                              this.filter.finest("chunked transfer sent " + total + " bytes" + percentString);
                              chunkedCounter = 0L;
                           }
                        }
                     } catch (IOException ioe) {
                        this.filter.severe("exception occurred while writing file '" + fileName + "' to response output stream (" + ioe + ")");
                        if (this.filter.isLoggable(Level.FINE)) {
                           this.filter.log(Level.FINE, "Stack trace: ", ioe);
                        }
                        break;
                     }

                     total += bytesRead;
                  }

                  try {
                     fin.close();
                  } catch (IOException var34) {
                  }

                  return;
               }
            } finally {
               if (closePbeKey) {
                  pbeKey.close();
               }
            }
         }
      }
   }

   private void sendDirectory(String dirPath, String dirName, HttpServletResponse resp, boolean recurse, boolean sendCrc, boolean sendPath) {
      if (!this.isForbiddenPath(dirPath) || this.isPlatformAccessPath(dirPath)) {
         File dir = new File(dirPath);
         ByteBuffer xmlBuffer = new ByteBuffer();

         XWriter xml;
         try {
            xml = new XWriter(xmlBuffer.getOutputStream());
         } catch (IOException ioe) {
            this.filter.severe("exception occurred while sending directory '" + dir + "' to response output stream (" + ioe + ")");
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.log(Level.FINE, "Stack trace: ", ioe);
            }

            return;
         }

         xml.w("<directory").w(' ').attr("name", dirName);
         if (sendPath) {
            xml.w(' ').attr("path", dirPath);
         }

         xml.w(">\n");
         File[] children = dir.listFiles();
         if (children != null) {
            Arrays.sort(children);

            for (File child : children) {
               File current = child;
               if (!this.isForbiddenPath(current.getParent()) || this.isPlatformAccessPath(current.getParent())) {
                  if (current.isDirectory()) {
                     if (!current.getName().equalsIgnoreCase("..") && !current.getName().equalsIgnoreCase(".")) {
                        if (recurse) {
                           xml.flush();
                           xml.close();
                           byte[] bytes = xmlBuffer.toByteArray();

                           try {
                              resp.getOutputStream().write(bytes);
                           } catch (IOException ioe) {
                              if (this.getServer() != null && this.getServer().getState() == 1) {
                                 this.filter.severe("exception occurred while sending directory '" + dir + "' to response output stream (" + ioe + ")");
                                 if (this.filter.isLoggable(Level.FINE)) {
                                    this.filter.log(Level.FINE, "Stack trace: ", ioe);
                                 }
                              }

                              return;
                           }

                           this.sendDirectory(current.getPath(), current.getName(), resp, true, sendCrc, sendPath);
                           xmlBuffer = new ByteBuffer();

                           try {
                              xml = new XWriter(xmlBuffer.getOutputStream());
                           } catch (IOException ioe) {
                              this.filter.severe("exception occurred while sending directory '" + dir + "' (" + ioe + ")");
                              if (this.filter.isLoggable(Level.FINE)) {
                                 this.filter.log(Level.FINE, "Stack trace: ", ioe);
                              }

                              return;
                           }
                        } else {
                           xml.w("<directory").w(' ').attr("name", current.getName());
                           if (sendPath) {
                              xml.w(' ').attr("path", current.getPath());
                           }

                           xml.w("/>\n");
                        }
                     }
                  } else {
                     xml.w("<file").w(' ').attr("name", current.getName()).w(' ').attr("size", String.valueOf(current.length()));
                     if (sendCrc) {
                        xml.w(' ').attr("crc", String.valueOf(FileUtil.getCrc(current)));
                     }

                     if (sendPath) {
                        xml.w(' ').attr("path", current.getPath());
                     }

                     xml.w(' ').attr("lastModified", HttpDateFormat.format(current.lastModified()));
                     xml.w("/>\n");
                  }
               }
            }
         }

         xml.w("</directory>\n");
         xml.flush();
         xml.close();
         byte[] bytes = xmlBuffer.toByteArray();
         if (!recurse) {
            resp.setIntHeader("Content-Length", bytes.length);
         }

         try {
            resp.getOutputStream().write(bytes);
         } catch (IOException ioe) {
            if (this.getServer() != null && this.getServer().getState() == 1) {
               this.filter.severe("exception occurred while sending directory '" + dir + "' to response output stream (" + ioe + ")");
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter.log(Level.FINE, "Stack trace: ", ioe);
               }
            }
         }
      }
   }

   private static void sendEncodingValidationResponse(
      Logger log, KeyedList query, HttpServletRequest req, HttpServletResponse resp, IPlatformProvider platformProvider
   ) {
      PBEValidator validator;
      try {
         validator = new PBEValidator(query.get("encodingValidator", null));
      } catch (IOException ioe) {
         log.log(Level.SEVERE, "failed to create PBE validator  (" + ioe + ")", ioe);
         Http.sendError(req, resp, 400);
         return;
      }

      SecretChars secretChars = platformProvider.getSystemPassword();
      Throwable var7 = null;

      try {
         resp.setHeader("Validation-Result", String.valueOf(validator.test(secretChars)));
         Http.sendError(req, resp, 200);
      } catch (Throwable var17) {
         var7 = var17;
         throw var17;
      } finally {
         if (secretChars != null) {
            if (var7 != null) {
               try {
                  secretChars.close();
               } catch (Throwable var16) {
                  var7.addSuppressed(var16);
               }
            } else {
               secretChars.close();
            }
         }
      }
   }

   private void sendFilesystemInfo(Logger log, String fileName, HttpServletResponse resp) {
      String fsName = this.platformProvider.getFileSystemName(fileName);
      String fsDisplayName = this.platformProvider.getFileSystemDisplayName(fsName);
      ByteBuffer xmlBuffer = new ByteBuffer();
      XWriter xml = new XWriter();
      xml.setOutputStream(xmlBuffer.getOutputStream());
      xml.w("<filesystem")
         .w(' ')
         .attr("root", fsName)
         .w(' ')
         .attr("displayName", fsDisplayName != null ? fsDisplayName : "")
         .w(' ')
         .attr("totalKb", String.valueOf(this.platformProvider.getTotalBytes(fsName) / 1024L))
         .w(' ')
         .attr("freeKb", String.valueOf(this.platformProvider.getFreeBytes(fsName) / 1024L))
         .w(' ')
         .attr("maxFileCount", String.valueOf(this.platformProvider.getMaxFileCount(fsName)))
         .w(' ')
         .attr("currentFileCount", String.valueOf(this.platformProvider.getCurrentFileCount(fsName)))
         .w("/>")
         .nl();
      xml.flush();
      xml.close();
      byte[] filesystemInfoBytes = xmlBuffer.toByteArray();
      resp.setHeader("Content-Type", "text/xml");
      resp.setIntHeader("Content-Length", filesystemInfoBytes.length);

      try {
         resp.getOutputStream().write(filesystemInfoBytes);
      } catch (IOException ioe) {
         if (this.getServer() != null && this.getServer().getState() == 1) {
            resp.setStatus(500);
            log.log(Level.SEVERE, "failed to write filesystem information response (" + ioe + ")", ioe);
         }
      }
   }

   private static int getRenamePath(String currentPath, String newName, StringBuilder newPath) {
      if (!new File(currentPath).exists()) {
         return 404;
      }

      if (currentPath.charAt(currentPath.length() - 1) == File.separatorChar) {
         currentPath = currentPath.substring(0, currentPath.length() - 1);
      }

      int lastIndex = currentPath.lastIndexOf(File.separator);
      if (lastIndex == -1) {
         return 400;
      }

      if (lastIndex == currentPath.length() - 1) {
         return 400;
      }

      currentPath = currentPath.substring(0, lastIndex + 1);
      newPath.append(currentPath).append(newName);
      return new File(newPath.toString()).exists() ? 409 : 204;
   }

   private boolean isPlatformAccessPath(String path) {
      if (this.uriMap.containsValue(path)) {
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.fine("path '" + path + "' matches declared URI map path, granting access");
         }

         return true;
      } else {
         for (String declaredSymbolicLink : this.declaredSymbolicLinks) {
            if (path.startsWith(declaredSymbolicLink)) {
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter.fine("path '" + path + "' matches declared external symlink entry '" + declaredSymbolicLink + "', granting access");
               }

               return true;
            }
         }

         if (PlatformUtil.isTridiumPlatform() && OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)) {
            synchronized (PREDICATE_MONITOR) {
               if (checkPath(path, IS_TRIDIUM_LEGACY_PLATFORM_ACCESS_PATH)) {
                  if (this.filter.isLoggable(Level.FINE)) {
                     this.filter.fine("path '" + path + "' matches declared platform access predicate, granting access");
                  }

                  return true;
               }
            }
         }

         return false;
      }
   }

   private boolean isForbiddenPath(String path) {
      synchronized (PREDICATE_MONITOR) {
         return checkPath(path, this.IS_FORBIDDEN_PATH)
            ? true
            : PlatformUtil.isTridiumPlatform() && OperatingSystemEnum.isOS(OperatingSystemEnum.qnx) && checkPath(path, IS_TRIDIUM_LEGACY_FORBIDDEN_PATH);
      }
   }

   private static boolean isProtectedWithPassphrase(String path) {
      synchronized (PREDICATE_MONITOR) {
         return checkPath(path, IS_PASSPHRASE_ENCRYPTED_PATH)
            ? true
            : PlatformUtil.isTridiumPlatform()
               && OperatingSystemEnum.isOS(OperatingSystemEnum.qnx)
               && checkPath(path, IS_TRIDIUM_LEGACY_PASSPHRASE_ENCRYPTED_PATH);
      }
   }

   private static boolean isProtectedWithKeyring(String path) {
      synchronized (PREDICATE_MONITOR) {
         return checkPath(path, IS_KEYRING_ENCRYPTED_PATH);
      }
   }

   private static boolean checkPath(String path, Predicate<String> predicate) {
      if (path != null && !path.isEmpty()) {
         File targetFile = new File(path);
         if (predicate.test(targetFile.getAbsolutePath())) {
            return true;
         }

         try {
            return predicate.test(targetFile.getCanonicalPath());
         } catch (Exception e) {
            return false;
         }
      } else {
         return false;
      }
   }

   private void handleForbiddenAccess(HttpServletRequest req, HttpServletResponse resp, String fileName) {
      String username = this.getServer().getAuthenticator().getRequestUserName(req);
      this.filter.warning("user \"" + username + "\" made " + req.getMethod() + " request with forbidden path \"" + fileName + "\"");
      MessageBundle msg = new MessageBundle("platform", "FileServlet.accessForbidden", fileName, "FileServlet: Access to " + fileName + " is forbidden");
      Http.sendErrorXML(req, resp, 403, msg);
   }

   public void addUriMapping(String uri, String path) {
      this.uriMap.put(uri, path);
   }

   public static void addPassphraseEncryptedPath(String path) {
      PASSPHRASE_ENCRYPTED_PATHS.add(path);
   }
}
