package com.tridium.niagarad.servlet;

import com.tridium.crypto.core.cert.CertUtils;
import com.tridium.crypto.core.cert.NCertificateParameters;
import com.tridium.crypto.core.cert.NHostExemption;
import com.tridium.crypto.core.cert.NKey;
import com.tridium.crypto.core.cert.NPKCS10CertificationRequest;
import com.tridium.crypto.core.cert.NX509Certificate;
import com.tridium.crypto.core.cert.NX509CertificateBuilderBundle;
import com.tridium.crypto.core.io.CoreCryptoManager;
import com.tridium.crypto.core.io.CryptoStoreId;
import com.tridium.crypto.core.io.ICoreExemptionStore;
import com.tridium.crypto.core.io.ICoreKeyStore;
import com.tridium.crypto.core.io.ICoreStore;
import com.tridium.crypto.core.io.ICoreTrustStore;
import com.tridium.crypto.core.io.ServerCertificateHealth;
import com.tridium.crypto.core.provider.NProvider;
import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.crypto.DaemonCryptoManager;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.log.SimpleErrorHandler;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.security.PasswordStrength;
import com.tridium.nre.security.SecretBytes;
import com.tridium.nre.security.SecretChars;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.IX509CertificateEntry;
import javax.baja.nre.security.SharedSecretKey;
import javax.baja.nre.util.ByteBuffer;
import javax.baja.xml.XElem;
import javax.baja.xml.XParser;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CryptoServlet extends DaemonServlet {
   protected CoreCryptoManager cryptoManager;
   protected ICoreKeyStore userKeyStore;
   protected ICoreTrustStore userTrustStore;
   protected ICoreTrustStore userUntrustedStore;
   protected ICoreTrustStore systemTrustStore;
   private final ICoreExemptionStore exemptions;
   private final Logger filter = Logger.getLogger("crypto");
   private static final String ALIAS = "alias";
   private static final String PASSWD = "passwd";
   private static final String ACTION = "action";
   private static final String CERT_BUILDER = "certBuilder";
   private static final String DEFAULT_CERT_FORCE = "force";
   private static final String CERT_PARAMS = "certParams";
   private static final String KEYSTORE_TYPE = "keystoretype";
   private static final String CERT_ENTRY = "certEntry";
   private static final String KEY_ENTRY = "keyEntry";
   private static final String CREATION_DATE = "creationDate";
   private static final String CERTIFICATE = "certificate";
   private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
   public static final String SERVLET_NAME = "crypto";
   public static final int MAX_POST_FORM_TRANSFER = 16777216;
   private static final int SEND_ALL_ALIASES = 1;
   private static final int SEND_SERVER_ALIASES = 2;
   private static final int SEND_CLIENT_ALIASES = 3;
   private static final String DISABLE_CERTIFICATE_GENERATION = AccessController.doPrivileged(() -> System.getProperty("niagarad.disableCertGen"));

   public CryptoServlet() {
      super("crypto");

      try {
         DaemonCryptoManager.getInstance();
         this.cryptoManager = CoreCryptoManager.get(NiagaraDaemon.getSecurityInfoProvider());
         this.userKeyStore = this.cryptoManager.getKeyStore();
         this.userTrustStore = this.cryptoManager.getUserTrustStore();
         this.userUntrustedStore = this.cryptoManager.getUserUntrustedStore();
         this.systemTrustStore = this.cryptoManager.getSystemTrustStore();
         this.exemptions = this.cryptoManager.getExemptionStore();
      } catch (Exception e) {
         this.filter.log(Level.SEVERE, "Failed to initialize the crypto servlet (" + e + ")");
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.log(Level.SEVERE, "Stack trace: ", e);
         }

         throw new IllegalArgumentException("Failed to initialize the crypto servlet", e);
      }
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      boolean requireAdmin = true;
      String queryString = req.getQueryString();
      if (queryString != null) {
         KeyedList query = Http.getGetForm(queryString);
         String action = query.get("action", null);
         if (action != null && ("getCertHealth".equalsIgnoreCase(action) || "getPasswordStrength".equalsIgnoreCase(action))) {
            requireAdmin = false;
         }
      }

      return requireAdmin
         ? DaemonAuthUtil.authAdmin(this.getServer().getAuthenticator(), req, resp)
         : DaemonAuthUtil.authUser(this.getServer().getAuthenticator(), req, resp);
   }

   @Override
   public int doGet(HttpServletRequest req, ErrorHandler handler, KeyedList query, XWriter content) {
      int retVal = 404;
      if (query != null) {
         String action = query.get("action", "sendAliases");
         String alias = query.get("alias", null);
         String password = query.get("passwd", null);
         String certParams = query.get("certParams", null);
         String certBuilder = query.get("certBuilder", null);

         ICoreStore keystore;
         try {
            keystore = this.getKeyStore(query);
         } catch (IllegalArgumentException iae) {
            this.filter.warning("invalid keystoretype '" + query.get("keystoretype", null) + "' specified");
            return 400;
         }

         boolean sharedKeyRequired = false;
         switch (action.toLowerCase(Locale.ENGLISH)) {
            case "getkey":
               sharedKeyRequired = true;
               break;
            case "generatecsr":
               sharedKeyRequired = password != null;
               break;
            case "generatecert":
               sharedKeyRequired = certParams != null || certBuilder != null;
         }

         boolean update = false;
         switch (action.toLowerCase(Locale.ENGLISH)) {
            case "savekeystore":
            case "deleteentry":
            case "generatecsr":
            case "generatecert":
            case "resetuserkeystore":
            case "deleteexemption":
            case "saveexemptions":
               update = true;
            default:
               if (update && !DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(req, query.get("csrfToken", null))) {
                  MessageBundle msg = new MessageBundle("invalid CSRF token in request");
                  handler.error(msg);
                  return 403;
               }

               SharedSecretKey sharedKey = null;
               if (sharedKeyRequired) {
                  try {
                     String sharedKeyInQuery = query.get("sharedKeyName", null);
                     if (sharedKeyInQuery == null) {
                        throw new Exception("No shared key found in query");
                     }

                     String sharedKeyAttributeName = "sharedKey_" + sharedKeyInQuery;
                     sharedKey = (SharedSecretKey)req.getSession(false).getAttribute(sharedKeyAttributeName);
                     if (sharedKey == null) {
                        throw new Exception("Shared key \"" + sharedKeyAttributeName + "\" not found in session");
                     }
                  } catch (Exception e) {
                     this.filter.severe("exception occurred (" + e + ") while attempting to obtain session shared key");
                     return 400;
                  }
               }

               if (this.filter.isLoggable(Level.FINE)) {
                  String queryString = req.getQueryString();
                  StringBuilder builder = new StringBuilder();
                  builder.append("CryptoServlet::doGet ").append(req.getRequestURI());
                  if (queryString != null) {
                     builder.append(" ").append(queryString);
                  }

                  this.filter.fine(builder.toString());
               }

               if ("sendAliases".equalsIgnoreCase(action)) {
                  retVal = sendAliases(content, keystore, alias, this.filter, 1);
               } else if ("sendServerAliases".equalsIgnoreCase(action)) {
                  retVal = sendAliases(content, keystore, alias, this.filter, 2);
               } else if ("sendClientAliases".equalsIgnoreCase(action)) {
                  retVal = sendAliases(content, keystore, alias, this.filter, 3);
               } else if ("saveKeyStore".equalsIgnoreCase(action)) {
                  retVal = saveKeyStore(keystore, this.filter);
               } else if ("getCertificate".equalsIgnoreCase(action)) {
                  retVal = getCertificate(content, keystore, alias, this.filter);
               } else if ("deleteEntry".equalsIgnoreCase(action)) {
                  retVal = deleteEntry(keystore, alias, this.filter);
               } else if ("getKey".equalsIgnoreCase(action)) {
                  retVal = getKey(content, keystore, alias, password, sharedKey, this.filter);
               } else if ("getCertificateChain".equalsIgnoreCase(action)) {
                  retVal = getCertificateChain(content, keystore, alias, this.filter);
               } else if ("getCertificates".equalsIgnoreCase(action)) {
                  retVal = getCertificates(content, keystore, this.filter);
               } else if ("getProvider".equalsIgnoreCase(action)) {
                  retVal = this.getProvider(content, query.get("providerName", null));
               } else if ("generateCSR".equalsIgnoreCase(action)) {
                  retVal = this.generateCSR(content, alias, password, sharedKey);
               } else if ("generateCert".equalsIgnoreCase(action)) {
                  if (DISABLE_CERTIFICATE_GENERATION != null && NiagaraDaemon.getInstance().getStationRegistry().appRunning()) {
                     retVal = this.unableToGenerateCert(true, handler);
                  } else {
                     retVal = this.genCertificate(content, handler, query, sharedKey);
                  }
               } else if ("genCertStatus".equalsIgnoreCase(action)) {
                  int requestId = -1;

                  try {
                     requestId = Integer.parseInt(query.get("requestId", "-1"));
                  } catch (Exception var17) {
                  }

                  retVal = this.genCertStatus(content, requestId);
               } else if ("resetUserKeyStore".equalsIgnoreCase(action)) {
                  if (DISABLE_CERTIFICATE_GENERATION != null && NiagaraDaemon.getInstance().getStationRegistry().appRunning()) {
                     retVal = this.unableToGenerateCert(true, handler);
                  } else {
                     retVal = this.resetUserKeyStore(content, handler, query);
                  }
               } else if ("getExemption".equalsIgnoreCase(action)) {
                  retVal = this.getExemption(content, query.get("host", null));
               } else if ("deleteExemption".equalsIgnoreCase(action)) {
                  retVal = this.deleteExemption(query.get("host", null));
               } else if ("getExemptions".equalsIgnoreCase(action)) {
                  retVal = this.getExemptions(content);
               } else if ("saveExemptions".equalsIgnoreCase(action)) {
                  retVal = this.saveExemptions(this.filter);
               } else if ("getPasswordStrength".equalsIgnoreCase(action)) {
                  retVal = getPasswordStrength(content);
               } else if ("getCertHealth".equalsIgnoreCase(action)) {
                  retVal = this.getCertHealth(content);
               }
         }
      }

      return retVal;
   }

   @Override
   public synchronized void doPost(HttpServletRequest req, HttpServletResponse resp) {
      this.responseSent = false;
      this.errorOnSend = false;
      int retVal = 404;
      int contentLength = req.getIntHeader("Content-Length");
      if (contentLength == -1) {
         MessageBundle msg = new MessageBundle("requested chunked input stream not supported, rejecting request");
         this.filter.severe("requested chunked input stream not supported, rejecting request");
         req.setAttribute("maxPostContentLength", 16777216);
         Http.sendErrorXML(req, resp, 415, msg);
      } else if (contentLength <= 16777216 && Http.validateHeapAvailable(contentLength)) {
         String queryString = req.getQueryString();
         KeyedList query = Http.getGetForm(queryString);
         String action = query.get("action", null);
         if (action != null) {
            ErrorHandler handler = new SimpleErrorHandler();
            ByteBuffer buffer = new ByteBuffer();
            XWriter content = new XWriter();
            content.setOutputStream(buffer.getOutputStream());
            String alias = query.get("alias", null);
            ICoreStore keystore = this.getKeyStore(query);
            String password = query.get("passwd", null);
            boolean sharedKeyRequired = false;
            switch (action.toLowerCase(Locale.ENGLISH)) {
               case "setkeyentry":
               case "setkeyentrywithresponse":
                  sharedKeyRequired = true;
            }

            if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(req, query.get("csrfToken", null))) {
               MessageBundle msg = new MessageBundle("invalid CSRF token in request");
               this.filter.severe("invalid CSRF token in request");
               Http.sendErrorXML(req, resp, 403, msg);
               return;
            }

            SharedSecretKey sharedKey = null;
            if (sharedKeyRequired) {
               try {
                  String sharedKeyInQuery = query.get("sharedKeyName", null);
                  if (sharedKeyInQuery == null) {
                     throw new Exception("No shared key found in query");
                  }

                  String sharedKeyAttributeName = "sharedKey_" + sharedKeyInQuery;
                  sharedKey = (SharedSecretKey)req.getSession(false).getAttribute(sharedKeyAttributeName);
                  if (sharedKey == null) {
                     throw new Exception("Shared key \"" + sharedKeyAttributeName + "\" not found in session");
                  }
               } catch (Exception e) {
                  this.filter.severe("exception occurred (" + e + ") while attempting to obtain session shared key");
                  Http.sendError(req, resp, 400);
                  return;
               }
            }

            if (this.filter.isLoggable(Level.FINE)) {
               StringBuilder builder = new StringBuilder();
               builder.append("CryptoServlet::doPost ").append(req.getRequestURI());
               if (queryString != null) {
                  builder.append(" ").append(queryString);
               }

               this.filter.fine(builder.toString());
            }

            try {
               if ("findCertificate".equalsIgnoreCase(action)) {
                  retVal = findCertificate(content, keystore, readCertificate(req.getInputStream(), contentLength, this.filter), this.filter);
               } else {
                  if ("getCertificateAlias".equalsIgnoreCase(action)) {
                     resp.setStatus(getCertificateAlias(content, keystore, readCertificate(req.getInputStream(), contentLength, this.filter), this.filter));
                     this.sendResponse(req, resp, handler, buffer, content, false);
                     return;
                  }

                  if ("setCertificateEntry".equalsIgnoreCase(action)) {
                     retVal = setCertificateEntry(keystore, alias, readCertificate(req.getInputStream(), contentLength, this.filter), this.filter);
                  } else if ("setKeyEntry".equalsIgnoreCase(action)) {
                     retVal = setKeyEntry(keystore, alias, password, req.getInputStream(), contentLength, sharedKey, this.filter);
                  } else {
                     if ("setKeyEntryWithResponse".equalsIgnoreCase(action)) {
                        resp.setStatus(
                           setKeyEntryWithResponse(content, handler, keystore, alias, password, req.getInputStream(), contentLength, sharedKey, this.filter)
                        );
                        this.sendResponse(req, resp, handler, buffer, content, false);
                        return;
                     }

                     if ("setExemption".equalsIgnoreCase(action)) {
                        retVal = this.setExemption(readExemption(req.getInputStream(), contentLength, this.filter));
                     }
                  }
               }
            } catch (Exception nonFatalException) {
               retVal = 400;
               this.filter.log(Level.WARNING, "Exception occurred while handling doPost (" + nonFatalException + ")");
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
               }
            }

            Http.sendError(req, resp, retVal);
         }
      } else {
         MessageBundle msg = new MessageBundle("content length " + contentLength + " exceeds maximum allowed transfer size " + 16777216 + ", rejecting request");
         this.filter.severe("content length " + contentLength + " exceeds maximum allowed transfer size " + 16777216 + ", rejecting request");
         req.setAttribute("maxPostContentLength", 16777216);
         Http.sendErrorXML(req, resp, 413, msg);
      }
   }

   private static int getCertificate(XWriter content, ICoreStore store, String alias, Logger filter) {
      String requestedAlias = alias;
      if (requestedAlias == null) {
         requestedAlias = "default";
      }

      try {
         ICoreTrustStore trustStore = (ICoreTrustStore)store;
         X509Certificate cert = trustStore.getCertificate(requestedAlias);
         content.w("<certificate>").nl();
         if (cert != null) {
            content.w(NX509Certificate.make(cert).encodeToString()).nl();
         }

         content.w("</certificate>");
         return 200;
      } catch (Exception nonFatalException) {
         filter.log(Level.WARNING, "Exception occurred while getting certificate (" + nonFatalException + ")");
         if (filter.isLoggable(Level.FINE)) {
            filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }

         return 404;
      }
   }

   private static int findCertificate(XWriter content, ICoreStore store, X509Certificate cert, Logger filter) {
      try {
         ICoreTrustStore trustStore = (ICoreTrustStore)store;
         if (cert != null) {
            String alias = trustStore.findCertificate(cert);
            if (alias != null) {
               content.w("<certificate ").attr("alias", alias).w("/>");
            } else {
               content.w("<certificate ").attr("found", String.valueOf(false)).w("/>");
            }

            return 200;
         }
      } catch (Exception nonFatalException) {
         filter.log(Level.WARNING, "Exception occurred while finding certificate (" + nonFatalException + ")");
         if (filter.isLoggable(Level.FINE)) {
            filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }
      }

      return 404;
   }

   private static int getKey(XWriter content, ICoreStore store, String alias, String password, SharedSecretKey sharedKey, Logger filter) {
      boolean passwordProvided = password != null && !password.isEmpty();

      try {
         ICoreKeyStore keystore = (ICoreKeyStore)store;
         if (alias != null && !alias.isEmpty()) {
            Key key;
            if (passwordProvided) {
               SecretChars passChars = sharedKey.decryptChars(Base64.getDecoder().decode(password));
               Throwable var10 = null;

               try {
                  key = keystore.getKey(alias, passChars.get());
               } catch (Throwable var36) {
                  var10 = var36;
                  throw var36;
               } finally {
                  if (passChars != null) {
                     if (var10 != null) {
                        try {
                           passChars.close();
                        } catch (Throwable var35) {
                           var10.addSuppressed(var35);
                        }
                     } else {
                        passChars.close();
                     }
                  }
               }
            } else {
               key = keystore.getKey(alias, null);
            }

            if (key != null) {
               content.w("<key ").attr("alias", alias).w(">");
               SecretBytes keyBytes = SecretBytes.fromString(NKey.encodeToString(key));
               Throwable var41 = null;

               try {
                  content.w(Base64.getEncoder().encodeToString(sharedKey.encrypt(keyBytes)));
               } catch (Throwable var34) {
                  var41 = var34;
                  throw var34;
               } finally {
                  if (keyBytes != null) {
                     if (var41 != null) {
                        try {
                           keyBytes.close();
                        } catch (Throwable var33) {
                           var41.addSuppressed(var33);
                        }
                     } else {
                        keyBytes.close();
                     }
                  }
               }

               content.w("</key>");
               return 200;
            }

            throw new Exception("Provided key " + alias + " not found");
         }
      } catch (Exception nonFatalException) {
         if (passwordProvided || alias == null) {
            filter.log(Level.WARNING, "unable to retrieve key '" + alias + "' with provided password (" + nonFatalException + ")");
         } else if (filter.isLoggable(Level.FINE)) {
            filter.log(Level.FINE, "unable to retrieve key '" + alias + "' without a password (" + nonFatalException + ")");
         }

         if (filter.isLoggable(Level.FINEST)) {
            filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }
      }

      return 404;
   }

   private static int deleteEntry(ICoreStore store, String alias, Logger filter) {
      try {
         ICoreTrustStore trustStore = (ICoreTrustStore)store;
         if (alias != null && !alias.isEmpty()) {
            trustStore.deleteEntry(alias);
         }

         return 200;
      } catch (Exception nonFatalException) {
         filter.log(Level.WARNING, "Exception occurred while deleting entry (" + nonFatalException + ")");
         if (filter.isLoggable(Level.FINE)) {
            filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }

         return 404;
      }
   }

   private static int saveKeyStore(ICoreStore store, Logger filter) {
      ICoreTrustStore trustStore = (ICoreTrustStore)store;

      try {
         trustStore.save();
         return 200;
      } catch (Exception e) {
         filter.log(Level.SEVERE, "Failed to save trust store (" + e + ")");
         if (filter.isLoggable(Level.FINE)) {
            filter.log(Level.SEVERE, "Stack trace: ", e);
         }

         return 500;
      }
   }

   private int saveExemptions(Logger filter) {
      try {
         this.exemptions.save();
         return 200;
      } catch (Exception e) {
         filter.log(Level.SEVERE, "Failed to save exemption store (" + e + ")");
         if (filter.isLoggable(Level.FINE)) {
            filter.log(Level.SEVERE, "Stack trace: ", e);
         }

         return 500;
      }
   }

   private static int sendAliases(XWriter content, ICoreStore store, String selectedAlias, Logger filter, int aliasType) {
      try {
         ICoreTrustStore trustStore = (ICoreTrustStore)store;
         boolean canGenerate = true;
         if (DISABLE_CERTIFICATE_GENERATION != null) {
            canGenerate = !NiagaraDaemon.getInstance().getStationRegistry().appRunning();
         }

         content.w("<userKeyStore ").attr("canGenerate", canGenerate ? "true" : "false").w(">");
         if (trustStore != null) {
            Enumeration<String> e = trustStore.aliases();

            while (e.hasMoreElements()) {
               String alias = e.nextElement();
               switch (aliasType) {
                  case 1:
                     break;
                  case 2:
                     if (trustStore instanceof ICoreKeyStore && !CertUtils.isValidServerCert(alias, (ICoreKeyStore)trustStore)) {
                        continue;
                     }
                     break;
                  case 3:
                     if (trustStore instanceof ICoreKeyStore && !CertUtils.isClientCert(trustStore.getCertificate(alias))) {
                        continue;
                     }
                     break;
                  default:
                     throw new IllegalStateException("Unknown alias type");
               }

               sendAlias(selectedAlias, alias, trustStore, content);
            }
         }

         content.w("</userKeyStore>");
         return 200;
      } catch (Exception nonFatalException) {
         filter.log(Level.WARNING, "Exception occurred while sending aliases (" + nonFatalException + ")");
         if (filter.isLoggable(Level.FINE)) {
            filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }

         return 404;
      }
   }

   private static void sendAlias(String selectedAlias, String alias, ICoreTrustStore trustStore, XWriter content) throws Exception {
      if (selectedAlias == null || alias.equalsIgnoreCase(selectedAlias)) {
         String isCert = String.valueOf(trustStore.isCertificateEntry(alias));
         String isKeyEntry = String.valueOf(trustStore.isKeyEntry(alias));
         Date create = trustStore.getCreationDate(alias);
         String creationDate = "";
         if (create != null) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            creationDate = df.format(create);
         }

         content.w("<key ")
            .attr("alias", alias)
            .w(' ')
            .attr("certEntry", isCert)
            .w(' ')
            .attr("keyEntry", isKeyEntry)
            .w(' ')
            .attr("creationDate", creationDate)
            .w("/>")
            .nl();
      }
   }

   private static void writeCertificate(XWriter content, NX509Certificate cert, String alias, Logger filter) {
      if (cert != null) {
         try {
            String encodedCert = cert.encodeToString();
            if (encodedCert != null && !encodedCert.isEmpty()) {
               content.w("<certificate ").attr("alias", alias).w(">").w(encodedCert).nl().w("</certificate>");
            }
         } catch (Exception nonFatalException) {
            filter.log(Level.WARNING, "Exception occurred while writing certificate (" + nonFatalException + ")");
            if (filter.isLoggable(Level.FINE)) {
               filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
            }
         }
      }
   }

   private static int getCertificateAlias(XWriter content, ICoreStore store, X509Certificate cert, Logger filter) {
      if (cert != null) {
         try {
            ICoreTrustStore trustStore = (ICoreTrustStore)store;
            String alias = trustStore.getCertificateAlias(cert);
            if (alias != null) {
               content.w("<certificate ").attr("alias", alias).w("/>");
            }

            return 200;
         } catch (Exception nonFatalException) {
            filter.log(Level.WARNING, "Exception occurred while getting certificate alias (" + nonFatalException + ")");
            if (filter.isLoggable(Level.FINE)) {
               filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
            }
         }
      }

      return 404;
   }

   private static int getCertificates(XWriter content, ICoreStore store, Logger filter) {
      try {
         ICoreTrustStore trustStore = (ICoreTrustStore)store;
         Iterable<IX509CertificateEntry> certificates = trustStore.getCertificateEntries();
         content.w("<certificates>");

         for (IX509CertificateEntry entry : certificates) {
            if (entry != null) {
               content.w("<certificateEntry ").attr("alias", entry.getAlias()).w(">").w(entry.encodeToString()).w("</certificateEntry>");
            }
         }

         content.w("</certificates>");
         return 200;
      } catch (Exception nonFatalException) {
         filter.log(Level.WARNING, "Exception occurred while getting certificates (" + nonFatalException + ")");
         if (filter.isLoggable(Level.FINE)) {
            filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }

         return 404;
      }
   }

   private static int getCertificateChain(XWriter content, ICoreStore store, String alias, Logger filter) {
      try {
         ICoreTrustStore trustStore = (ICoreTrustStore)store;
         X509Certificate[] certChain = trustStore.getCertificateChain(alias);
         content.w("<certChain ").attr("alias", alias).w(">").nl();
         if (certChain != null) {
            for (X509Certificate cert : certChain) {
               writeCertificate(content, NX509Certificate.make(cert), alias, filter);
            }
         }

         content.w("</certChain>");
         return 200;
      } catch (Exception nonFatalException) {
         filter.log(Level.WARNING, "Exception occurred while getting certificate chain (" + nonFatalException + ")");
         if (filter.isLoggable(Level.FINE)) {
            filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }

         return 404;
      }
   }

   private int resetUserKeyStore(XWriter content, ErrorHandler handler, KeyedList query) {
      try {
         int id = this.cryptoManager.resetUserKeyStore();
         if (id != -1) {
            content.w("<resetUserKeyStore ").attr("requestId", String.valueOf(id)).w(" />").nl();
            return 200;
         } else {
            return this.unableToGenerateCert(false, handler);
         }
      } catch (Exception ignore) {
         return this.unableToGenerateCert(false, handler);
      }
   }

   private int genCertificate(XWriter content, ErrorHandler handler, KeyedList query, SharedSecretKey sharedKey) {
      String certBuilderStr = query.get("certBuilder", null);
      if (certBuilderStr != null) {
         try {
            certBuilderStr = sharedKey.decrypt(Base64.getDecoder().decode(certBuilderStr)).asString(true, StandardCharsets.UTF_8);
            NX509CertificateBuilderBundle bundle = NX509CertificateBuilderBundle.decodeFromString(certBuilderStr);
            this.filter.info("received generate certificate request as builder: alias '" + bundle.getBuilder().getAlias() + "', enqueuing request");
            int id = this.cryptoManager.generateSelfSignedCert(bundle.getBuilder(), bundle.getGenerator(), bundle.getPassword());
            if (id != -1) {
               content.w("<genCertificate ").attr("requestId", String.valueOf(id)).w(" />").nl();
               return 200;
            } else {
               return this.unableToGenerateCert(false, handler);
            }
         } catch (Exception nonFatalException) {
            this.filter.log(Level.WARNING, "Exception occurred while generating certificate from builder (" + nonFatalException + ")");
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
            }

            return 404;
         }
      } else {
         String certParamsStr = query.get("certParams", null);

         try {
            certParamsStr = sharedKey.decrypt(Base64.getDecoder().decode(certParamsStr)).asString(true, StandardCharsets.UTF_8);
            NCertificateParameters certParams = new NCertificateParameters(certParamsStr);
            this.filter
               .info(
                  "received generate certificate request as params: alias '"
                     + certParams.getAlias()
                     + "', purpose '"
                     + certParams.getKeyPurpose()
                     + "', key size '"
                     + certParams.getKeySize()
                     + "', enqueuing request"
               );
            int id = this.cryptoManager.generateSelfSignedCert(certParams);
            if (id != -1) {
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter.fine("certificate request enqueued, requestId '" + id + "'");
               }

               content.w("<genCertificate ").attr("requestId", String.valueOf(id)).w(" />").nl();
               return 200;
            } else {
               return this.unableToGenerateCert(false, handler);
            }
         } catch (Exception nonFatalException) {
            this.filter.log(Level.WARNING, "Exception occurred while generating certificate from params (" + nonFatalException + ")");
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
            }

            return 404;
         }
      }
   }

   private int unableToGenerateCert(boolean disabledByProperty, ErrorHandler handler) {
      if (disabledByProperty) {
         this.filter.warning("certificate generation not supported while an application is running");
         handler.error("certificate generation not supported while an application is running");
      } else {
         this.filter.warning("certificate generation request failed!");
         handler.error("certificate generation request failed!");
      }

      return 503;
   }

   private int genCertStatus(XWriter content, int requestId) {
      try {
         int status = this.cryptoManager.getCertGenerationStatus(requestId);
         content.w("<genCertStatus ").attr("status", String.valueOf(status)).w(" />").nl();
         return 200;
      } catch (Exception nonFatalException) {
         this.filter.log(Level.WARNING, "Exception occurred while generating certificate status (" + nonFatalException + ")");
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }

         return 404;
      }
   }

   private static int setCertificateEntry(ICoreStore store, String alias, X509Certificate certificate, Logger filter) {
      try {
         ICoreTrustStore trustStore = (ICoreTrustStore)store;
         if (alias != null && certificate != null) {
            trustStore.setCertificateEntry(alias, certificate);
            return 200;
         }
      } catch (Exception nonFatalException) {
         filter.log(Level.WARNING, "Exception occurred while setting certificate entry (" + nonFatalException + ")");
         if (filter.isLoggable(Level.FINE)) {
            filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }
      }

      return 404;
   }

   private static int setKeyEntry(ICoreStore store, String alias, String password, InputStream in, int len, SharedSecretKey sharedKey, Logger filter) throws IOException {
      ICoreKeyStore keyStore = (ICoreKeyStore)store;
      String input = readInput(in, len, filter);

      try {
         XElem root = XParser.make(input).parse();
         if (root != null) {
            XElem keyElem = root.elem("key");
            if (keyElem != null) {
               String keyString = keyElem.text() != null ? keyElem.text().toString() : "";
               if (keyString != null && !keyString.isEmpty()) {
                  SecretChars keySecretChars = sharedKey.decryptChars(Base64.getDecoder().decode(keyString));
                  Key key = NKey.decodeFromString(keySecretChars.asString(true));
                  X509Certificate[] certChain = parseCertChain(root, filter);
                  if (alias != null && key != null && certChain != null) {
                     if (password != null && !password.isEmpty()) {
                        SecretChars secretChars = sharedKey.decryptChars(Base64.getDecoder().decode(password));
                        Throwable var16 = null;

                        try {
                           keyStore.setKeyEntry(alias, key, secretChars.get(), certChain);
                        } catch (Throwable var26) {
                           var16 = var26;
                           throw var26;
                        } finally {
                           if (secretChars != null) {
                              if (var16 != null) {
                                 try {
                                    secretChars.close();
                                 } catch (Throwable var25) {
                                    var16.addSuppressed(var25);
                                 }
                              } else {
                                 secretChars.close();
                              }
                           }
                        }
                     } else {
                        keyStore.setKeyEntry(alias, key, null, certChain);
                     }

                     return 201;
                  }
               }
            }
         }
      } catch (Exception nonFatalException) {
         filter.log(Level.WARNING, "Exception occurred while setting key entry (" + nonFatalException + ")");
         if (filter.isLoggable(Level.FINE)) {
            filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }
      }

      return 404;
   }

   private static int setKeyEntryWithResponse(
      XWriter content, ErrorHandler handler, ICoreStore store, String alias, String password, InputStream in, int len, SharedSecretKey sharedKey, Logger filter
   ) throws IOException {
      String input = readInput(in, len, filter);

      try {
         ICoreKeyStore keyStore = (ICoreKeyStore)store;
         content.w("<setKeyResult>");
         XElem root = XParser.make(input).parse();
         if (root != null) {
            XElem keyElem = root.elem("key");
            if (keyElem != null) {
               String keyString = keyElem.text() != null ? keyElem.text().toString() : "";
               if (keyString != null && !keyString.isEmpty()) {
                  SecretChars keySecretChars = sharedKey.decryptChars(Base64.getDecoder().decode(keyString));
                  Key key = NKey.decodeFromString(keySecretChars.asString(true));
                  X509Certificate[] certChain = parseCertChain(root, filter);
                  if (alias != null && key != null && certChain != null) {
                     if (password != null && !password.isEmpty()) {
                        SecretChars secretChars = sharedKey.decryptChars(Base64.getDecoder().decode(password));
                        Throwable var18 = null;

                        try {
                           keyStore.setKeyEntry(alias, key, secretChars.get(), certChain);
                        } catch (Throwable var28) {
                           var18 = var28;
                           throw var28;
                        } finally {
                           if (secretChars != null) {
                              if (var18 != null) {
                                 try {
                                    secretChars.close();
                                 } catch (Throwable var27) {
                                    var18.addSuppressed(var27);
                                 }
                              } else {
                                 secretChars.close();
                              }
                           }
                        }
                     } else {
                        keyStore.setKeyEntry(alias, key, null, certChain);
                     }

                     content.w("success");
                     content.w("</setKeyResult>");
                     return 201;
                  }
               }
            }
         }
      } catch (Exception exception) {
         filter.log(Level.SEVERE, "Exception occurred while setting key entry with response (" + exception + ")");
         if (filter.isLoggable(Level.FINE)) {
            filter.log(Level.SEVERE, "Stack trace: ", exception);
         }

         content.w(exception.toString());
         handler.error(exception.toString());
      }

      content.w("</setKeyResult>");
      return 404;
   }

   private static X509Certificate[] parseCertChain(XElem root, Logger filter) {
      if (root != null) {
         XElem chain = root.elem("certChain");
         if (chain != null) {
            XElem[] xmlCertChain = chain.elems("certificate");
            if (xmlCertChain != null && xmlCertChain.length > 0) {
               X509Certificate[] certChain = new X509Certificate[xmlCertChain.length];

               try {
                  for (int i = 0; i < certChain.length; i++) {
                     if (xmlCertChain[i] == null || xmlCertChain[i].text() == null) {
                        return null;
                     }

                     String certChainEntry = xmlCertChain[i].text().toString();
                     certChain[i] = CertUtils.decodeX509Certificate(certChainEntry);
                  }

                  return certChain;
               } catch (Exception nonFatalException) {
                  filter.log(Level.WARNING, "Exception occurred while parsing certificate chain (" + nonFatalException + ")");
                  if (filter.isLoggable(Level.FINE)) {
                     filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
                  }
               }
            }
         }
      }

      return null;
   }

   private int generateCSR(XWriter content, String alias, String password, SharedSecretKey sharedKey) {
      try {
         NPKCS10CertificationRequest csr;
         if (password != null && !password.isEmpty()) {
            SecretChars passChars = sharedKey.decryptChars(Base64.getDecoder().decode(password));
            csr = this.cryptoManager.generateCSR(alias, passChars.asString(true));
         } else {
            csr = this.cryptoManager.generateCSR(alias, password);
         }

         String csrResponse = csr.encodeToString();
         content.w("<csr ").attr("alias", alias).w(">").w(csrResponse).w("</csr>");
         return 200;
      } catch (Exception nonFatalException) {
         this.filter.log(Level.WARNING, "Exception occurred while generating CSR (" + nonFatalException + ")");
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }

         return 404;
      }
   }

   protected int getProvider(XWriter content, String providerName) {
      try {
         content.w("<providers>").nl();
         if (providerName != null) {
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.fine("getProvider encoding provider '" + providerName + "' to string");
            }

            NProvider provider = NProvider.getProvider(providerName);
            content.w("<provider ").attr("name", providerName).w(">").w(provider.encodeToString()).w("</provider>");
         } else {
            NProvider[] providers = NProvider.getProviders();

            for (NProvider provider : providers) {
               if (this.filter.isLoggable(Level.FINE)) {
                  this.filter.fine("getProvider encoding provider '" + provider.getName() + "' to string");
               }

               content.w("<provider ").attr("name", provider.getName()).w(">").w(provider.encodeToString()).w("</provider>");
            }
         }

         content.w("</providers>");
         return 200;
      } catch (OutOfMemoryError outOfMemoryError) {
         this.filter
            .log(Level.SEVERE, "OutOfMemoryError encountered while handling request for provider" + (providerName != null ? " '" + providerName + "'" : ""));
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.log(Level.SEVERE, "Stack trace: ", outOfMemoryError);
         }

         return 500;
      } catch (UTFDataFormatException utfDataFormatException) {
         this.filter
            .log(
               Level.SEVERE, "UTFDataFormatException encountered while handling request for provider" + (providerName != null ? " '" + providerName + "'" : "")
            );
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.log(Level.SEVERE, "Stack trace: ", utfDataFormatException);
         }

         return 500;
      } catch (Exception nonFatalException) {
         this.filter
            .log(
               Level.WARNING,
               "Exception occurred while retrieving provider" + (providerName != null ? " '" + providerName + "'" : "") + "(" + nonFatalException + ")"
            );
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }

         return 404;
      }
   }

   private int deleteExemption(String host) {
      try {
         if (host != null && !host.isEmpty()) {
            this.exemptions.deleteExemption(host);
            return 200;
         }
      } catch (Exception nonFatalException) {
         this.filter.log(Level.WARNING, "Exception occurred while deleting exemption (" + nonFatalException + ")");
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }
      }

      return 404;
   }

   private int setExemption(NHostExemption exemption) {
      try {
         if (exemption != null) {
            this.exemptions.setExemption(exemption);
            return 200;
         }
      } catch (Exception nonFatalException) {
         this.filter.log(Level.WARNING, "Exception occurred while setting exemption (" + nonFatalException + ")");
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }
      }

      return 404;
   }

   private int getExemptions(XWriter content) {
      try {
         if (this.exemptions != null) {
            Enumeration<NHostExemption> hostExemptions = this.exemptions.exemptions();
            if (hostExemptions != null) {
               content.w("<exemptions>");

               while (hostExemptions.hasMoreElements()) {
                  NHostExemption exemption = hostExemptions.nextElement();
                  writeExemption(exemption, content);
               }

               content.w("</exemptions>");
            }
         }
      } catch (Exception nonFatalException) {
         this.filter.log(Level.WARNING, "Exception occurred while getting exemptions (" + nonFatalException + ")");
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }
      }

      return 200;
   }

   private int getExemption(XWriter content, String host) {
      try {
         if (host != null && !host.isEmpty()) {
            NHostExemption exemption = this.exemptions.getExemption(host);
            writeExemption(exemption, content);
            return 200;
         }
      } catch (Exception nonFatalException) {
         this.filter.log(Level.WARNING, "Exception occurred while getting exemption (" + nonFatalException + ")");
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }
      }

      return 404;
   }

   private static void writeExemption(NHostExemption exemption, XWriter content) throws Exception {
      if (exemption != null) {
         content.w("<exemption>");
         content.write(exemption.encodeToString());
         content.w("</exemption>");
      }
   }

   private static int getPasswordStrength(XWriter content) {
      content.write("<passwordStrength ");
      content.attr("minimumLength", Integer.toString(PasswordStrength.DEFAULT.getMinimumLength())).w(' ');
      content.attr("minimumLowerCase", Integer.toString(PasswordStrength.DEFAULT.getMinimumLowerCase())).w(' ');
      content.attr("minimumUpperCase", Integer.toString(PasswordStrength.DEFAULT.getMinimumUpperCase())).w(' ');
      content.attr("minimumDigits", Integer.toString(PasswordStrength.DEFAULT.getMinimumDigits())).w(' ');
      content.attr("minimumSpecial", Integer.toString(PasswordStrength.DEFAULT.getMinimumSpecial())).w(' ');
      content.attr("maximumLength", Integer.toString(PasswordStrength.DEFAULT.getMaximumLength())).w(' ');
      content.w("/>");
      return 200;
   }

   private int getCertHealth(XWriter content) {
      ServerCertificateHealth certHealth = this.getServer().getCertHealth();
      content.w("<certHealth ")
         .attr("requestedCert", certHealth.getRequestedCert() != null ? certHealth.getRequestedCert() : "")
         .w(' ')
         .attr("returnedCert", certHealth.getReturnedCert() != null ? certHealth.getReturnedCert() : "")
         .w(' ')
         .attr("certStatus", certHealth.getCause().name())
         .w("/>")
         .nl();
      return 200;
   }

   private ICoreStore getKeyStore(KeyedList query) throws IllegalArgumentException {
      try {
         String idName = query.get("keystoretype", null);
         CryptoStoreId id = idName != null ? CryptoStoreId.getEnum(idName) : CryptoStoreId.USER_KEY_STORE;
         switch (id) {
            case USER_TRUST_STORE:
               return this.userTrustStore;
            case USER_UNTRUSTED_STORE:
               return this.userUntrustedStore;
            case SYSTEM_TRUST_STORE:
               return this.systemTrustStore;
            case USER_KEY_STORE:
               return this.userKeyStore;
         }
      } catch (Exception nonFatalException) {
         this.filter.log(Level.WARNING, "Exception occurred while getting key store (" + nonFatalException + ")");
         if (this.filter.isLoggable(Level.FINE)) {
            this.filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }
      }

      throw new IllegalArgumentException("invalid keystore type");
   }

   private static X509Certificate readCertificate(InputStream inputStream, int len, Logger filter) throws IOException {
      String input = readInput(inputStream, len, filter);
      if (input != null) {
         try {
            String encodedCert = getText("certificate", input, filter);
            if (encodedCert != null) {
               return CertUtils.decodeX509Certificate(encodedCert);
            }
         } catch (Exception nonFatalException) {
            filter.log(Level.WARNING, "Exception occurred while reading certificate (" + nonFatalException + ")");
            if (filter.isLoggable(Level.FINE)) {
               filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
            }
         }
      }

      return null;
   }

   private static NHostExemption readExemption(InputStream inputStream, int len, Logger filter) throws IOException {
      String input = readInput(inputStream, len, filter);
      if (input != null) {
         try {
            input = getText("exemption", input, filter);
            if (input != null) {
               return NHostExemption.decodeFromString(input);
            }
         } catch (Exception nonFatalException) {
            filter.log(Level.WARNING, "Exception occurred while reading exemption (" + nonFatalException + ")");
            if (filter.isLoggable(Level.FINE)) {
               filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
            }
         }
      }

      return null;
   }

   private static String getText(String elementName, String input, Logger filter) {
      try {
         XElem element = XParser.make(input).parse();
         if (!element.name().equalsIgnoreCase(elementName)) {
            element = element.elem(elementName);
         }

         if (element != null) {
            return element.text().toString();
         }
      } catch (Exception nonFatalException) {
         filter.log(Level.WARNING, "Exception occurred while getting text (" + nonFatalException + ")");
         if (filter.isLoggable(Level.FINE)) {
            filter.log(Level.WARNING, "Stack trace: ", nonFatalException);
         }
      }

      return null;
   }

   private static String readInput(InputStream inputStream, int len, Logger filter) throws IOException {
      try {
         if (!Http.validateHeapAvailable(len)) {
            throw new IOException("Requested content length is too large '" + len + "'");
         }

         int bytesRead = 0;
         byte[] buf = new byte[1024];

         try {
            ByteArrayOutputStream data = new ByteArrayOutputStream(len);

            while (bytesRead < len) {
               int readBytes = inputStream.read(buf);
               if (readBytes == -1) {
                  throw new IOException("EOF encountered before data complete");
               }

               if (readBytes > 0) {
                  data.write(buf, 0, readBytes);
                  bytesRead += readBytes;
               } else {
                  try {
                     Thread.sleep(100L);
                  } catch (Exception var16) {
                  }
               }
            }

            return data.toString();
         } catch (OutOfMemoryError outOfMemoryError) {
            filter.log(Level.SEVERE, "OutOfMemoryError encountered while handling input size '" + len + "'");
            if (filter.isLoggable(Level.FINE)) {
               filter.log(Level.SEVERE, "Stack trace: ", outOfMemoryError);
            }

            throw new IOException("OutOfMemoryError encountered while handling input size '" + len + "'");
         }
      } finally {
         try {
            inputStream.close();
         } catch (Exception var15) {
         }
      }
   }
}
