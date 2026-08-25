package com.tridium.niagarad.servlet;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.security.EncryptionAlgorithmBundle;
import com.tridium.nre.security.SessionKey;
import java.lang.ref.WeakReference;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.WeakHashMap;
import java.util.Base64.Decoder;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.security.SharedSecretKey;
import javax.baja.nre.util.SecurityUtil;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

public class SharedKeyServlet extends DaemonServlet {
   private static final WeakHashMap<HttpSession, SharedKeyServlet.SessionKeyLRUCache> sessionToSessionKeys = new WeakHashMap<>();
   private Logger filter;
   public static final int MAX_SESSION_KEYS_PER_SESSION = 32;
   public static final int MAX_SHARED_KEY_NAME_LENGTH = 128;

   public SharedKeyServlet() {
      super("sharedKey");
   }

   @Override
   public boolean doStart() {
      this.filter = Logger.getLogger("sharedKey");
      return true;
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      Decoder decoder = Base64.getDecoder();
      if (this.isPing(query)) {
         return 200;
      }

      if (this.isKeyInitialization(query)) {
         if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
            MessageBundle msg = new MessageBundle("invalid CSRF token in request");
            handler.error(msg);
            this.filter.severe("invalid CSRF token in request");
            return 403;
         }

         try {
            HttpSession session = request.getSession(false);
            SessionKey sessionKey = (SessionKey)session.getAttribute("sessionKey");
            if (sessionKey == null) {
               this.filter.severe("session key not found, can not generate shared key");
               MessageBundle msg = new MessageBundle("sharedKey: Session key not found, can not generate shared key");
               handler.error(msg);
               return 400;
            }

            String sharedKeyQueryName = query.get("name", null);
            if (sharedKeyQueryName == null) {
               this.filter.severe("invalid request, shared key query missing name");
               MessageBundle msg = new MessageBundle("sharedKey: invalid request, shared key query missing name");
               handler.error(msg);
               return 400;
            }

            if (sharedKeyQueryName.length() > 128) {
               this.filter.severe("invalid request, shared key query name length exceeds maximum limit 128");
               MessageBundle msg = new MessageBundle("sharedKey: invalid request, shared key query name length exceeds maximum limit");
               handler.error(msg);
               return 400;
            }

            SharedSecretKey sharedKey = sessionKey.generateSharedSecret(
               sharedKeyQueryName, decoder.decode(query.get("salt", null)), decoder.decode(query.get("iv", null))
            );

            try {
               sharedKey.validateVerificationMessage(decoder.decode(query.get("message", null)));
            } catch (Exception verificationException) {
               sharedKey.close();
               throw verificationException;
            }

            String sharedKeyAttributeName = "sharedKey_" + sharedKey.getName();
            session.setAttribute(sharedKeyAttributeName, sharedKey);
            int keySize = 0;
            synchronized (this) {
               SharedKeyServlet.SessionKeyLRUCache sessionKeys = sessionToSessionKeys.computeIfAbsent(
                  session, k -> new SharedKeyServlet.SessionKeyLRUCache(session)
               );
               sessionKeys.put(sharedKeyAttributeName, sharedKey);
               keySize = sessionKeys.size();
            }

            if (this.filter.isLoggable(Level.FINE)) {
               this.filter
                  .fine(
                     "created shared key '"
                        + sharedKeyAttributeName
                        + "' for session '"
                        + SecurityUtil.calculateSessionIdHash(session.getId())
                        + "', session key count = "
                        + keySize
                        + " (sessionToSessionKeys count = "
                        + sessionToSessionKeys.size()
                        + ")"
                  );
            }

            return 200;
         } catch (Exception e) {
            MessageBundle msg = new MessageBundle("sharedKey: Invalid shared key data provided: " + e);
            handler.error(msg);
            this.filter.severe("invalid shared key data provided: " + e);
            if (this.filter.isLoggable(Level.FINE)) {
               this.filter.log(Level.FINE, "Stack trace: ", e);
            }

            return 400;
         }
      } else {
         if (!this.isEncryptionAlgorithmBundle(query)) {
            MessageBundle msg = new MessageBundle("sharedKey: Unrecognized message format");
            handler.error(msg);
            return 400;
         }

         HttpSession session = request.getSession(false);
         SessionKey sessionKey = (SessionKey)session.getAttribute("sessionKey");
         if (sessionKey == null) {
            this.filter.severe("session key not found, cannot negotiate encryption algorithm bundle");
            MessageBundle msg = new MessageBundle("sharedKey: Session key not found, cannot negotiate encryption algorithm bundle");
            handler.error(msg);
            return 400;
         }

         String algorithmBundles = query.get("encryptionAlgorithmBundles", null);
         String[] bundles = algorithmBundles.split(":");
         EncryptionAlgorithmBundle encryptionAlgorithmBundle = null;

         for (String bundle : bundles) {
            CryptographicAlgorithmBundle algorithmBundle = CryptographicAlgorithmBundle.getInstance(bundle);
            if (algorithmBundle instanceof EncryptionAlgorithmBundle) {
               encryptionAlgorithmBundle = (EncryptionAlgorithmBundle)algorithmBundle;
               break;
            }
         }

         if (encryptionAlgorithmBundle != null) {
            sessionKey.setEncryptionAlgorithmBundle(encryptionAlgorithmBundle);
            content.w("<encryptionAlgorithmBundle ").attr("name", encryptionAlgorithmBundle.getAlgorithmName()).w("/>");
            return 200;
         } else {
            MessageBundle msg = new MessageBundle("sharedKey: Unable to negotiate supported EncryptionAlgorithmBundle");
            handler.error(msg);
            return 400;
         }
      }
   }

   private boolean isKeyInitialization(KeyedList query) {
      return query != null && query.containsKey("name") && query.containsKey("salt") && query.containsKey("iv") && query.containsKey("message");
   }

   private boolean isPing(KeyedList query) {
      return query != null && "true".equals(query.get("ping", ""));
   }

   private boolean isEncryptionAlgorithmBundle(KeyedList query) {
      return query != null && query.containsKey("encryptionAlgorithmBundles");
   }

   private class SessionKeyLRUCache extends LinkedHashMap<String, SharedSecretKey> implements HttpSessionAttributeListener {
      public WeakReference<HttpSession> sessionReference;

      public SessionKeyLRUCache(HttpSession session) {
         super(32, 0.75F);
         this.sessionReference = new WeakReference<>(session);
      }

      @Override
      public boolean removeEldestEntry(Entry<String, SharedSecretKey> eldest) {
         boolean remove = this.size() > 32;
         if (remove) {
            HttpSession session = this.sessionReference.get();
            if (session != null) {
               if (SharedKeyServlet.this.filter.isLoggable(Level.FINE)) {
                  SharedKeyServlet.this.filter
                     .fine(
                        "removing oldest shared key '"
                           + eldest.getKey()
                           + "' for session '"
                           + SecurityUtil.calculateSessionIdHash(session.getId())
                           + "', key count exceeded max value of "
                           + 32
                     );
               }

               session.removeAttribute(eldest.getKey());
            }
         }

         return remove;
      }

      public void attributeAdded(HttpSessionBindingEvent event) {
      }

      public void attributeRemoved(HttpSessionBindingEvent event) {
         if (event.getSession() == this.sessionReference.get() && event.getName().startsWith("sharedKey_")) {
            this.remove(event.getName());
         }
      }

      public void attributeReplaced(HttpSessionBindingEvent event) {
      }
   }
}
