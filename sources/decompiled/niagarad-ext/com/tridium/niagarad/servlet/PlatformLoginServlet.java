package com.tridium.niagarad.servlet;

import com.tridium.niagarad.http.Http;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PlatformLoginServlet extends Servlet {
   private static final String LOGIN_FORM_PREFIX = "<!DOCTYPE html>\n<html>\n<head>\n  <title>Niagara Platform Login</title>\n  <link rel=\"stylesheet\" type=\"text/css\" href=\"loginfile/plat-login.css\">\n</head>\n<body>\n  <form id=\"login-form\" method=\"POST\" action=$action>\n    <fieldset id=\"login-form-container\">\n      <div id=\"login-title-container\">\n        <div id=\"login-title\">Niagara Platform Login</div>\n      </div>\n      <div id=\"login-failed\">\n        Login Failed\n      </div>\n      <div id=\"login-credentials\">\n        <script type=\"text/javascript\" src=\"loginfile/plat-login.js\"></script>\n        <script type=\"text/javascript\" src=\"loginfile/core/auth.min.js\"></script>\n";
   private static final String LOGIN_FORM_SUFFIX = "        <div class=\"login-group\">\n          <label class=\"login-label\" for=\"userName\">Username</label>\n          <input id=\"username\" class=\"login-input\" type=\"text\" name=\"j_username\" value=\"\" autofocus/>\n        </div>\n        <div class=\"login-group\">\n          <label class=\"login-label\" for=\"password\">Password</label>\n          <input id=\"password\" class=\"login-input\" type=\"password\" name=\"j_password\" autocomplete=\"off\"/>\n        </div>\n        <input id=\"login-submit\" type=\"submit\" value=\"Login\" onclick=\"return doLogin();\" disabled/>\n        <script type=\"text/javascript\">\n           if (typeof doLogin === 'function') { document.getElementById('login-submit').disabled = false; }\n        </script>\n      </div>\n    </fieldset>\n  </form>\n</body>\n</html>\n";
   public static final String _SERVLET_NAME = "login";

   public PlatformLoginServlet() {
      super("login");
   }

   @Override
   public void doGet(HttpServletRequest request, HttpServletResponse response) {
      if (!DebugServlet.debugEnabled) {
         Http.sendError(request, response, 403);
      } else {
         byte[] loginFormBytes = this.generateLoginForm(request).getBytes(StandardCharsets.UTF_8);
         response.setHeader("Content-Type", "text/html; charset=utf-8");
         response.setIntHeader("Content-Length", loginFormBytes.length);

         try {
            response.getOutputStream().write(loginFormBytes);
         } catch (Exception ioe) {
            if (this.getServer() != null && this.getServer().getState() == 1) {
               this.getServer().getFilter().log(Level.SEVERE, this.getName() + ": encountered error while writing response (" + ioe + ")", ioe);
               Http.sendError(request, response, 500);
            }
         }
      }
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean requiresAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      return true;
   }

   private String generateLoginForm(HttpServletRequest request) {
      String loginRealm = this.getServer().getAuthenticator().getAuthDomain().getRealm(request);
      String loginScheme = this.getServer().getAuthenticator().getAuthScheme();
      return "<!DOCTYPE html>\n<html>\n<head>\n  <title>Niagara Platform Login</title>\n  <link rel=\"stylesheet\" type=\"text/css\" href=\"loginfile/plat-login.css\">\n</head>\n<body>\n  <form id=\"login-form\" method=\"POST\" action=$action>\n    <fieldset id=\"login-form-container\">\n      <div id=\"login-title-container\">\n        <div id=\"login-title\">Niagara Platform Login</div>\n      </div>\n      <div id=\"login-failed\">\n        Login Failed\n      </div>\n      <div id=\"login-credentials\">\n        <script type=\"text/javascript\" src=\"loginfile/plat-login.js\"></script>\n        <script type=\"text/javascript\" src=\"loginfile/core/auth.min.js\"></script>\n        <div class=\"login-group\">\n          <label class=\"login-label\" for=\"authdomain\">Realm</label>\n          <label class=\"login-label\" for=\"authdomain\">"
         + loginRealm
         + "</label>\n          <input id=\"authdomain\" class=\"login-input\" type=\"hidden\" name=\"j_authdomain\" value=\""
         + loginRealm
         + "\" readonly=\"true\"/>\n        </div>\n        <div class=\"login-group\">\n          <label class=\"login-label\" for=\"authscheme\">Scheme</label>\n          <label class=\"login-label\" for=\"authscheme\">"
         + loginScheme
         + "</label>\n          <input id=\"authscheme\" class=\"login-input\" type=\"hidden\" name=\"j_authscheme\" value=\""
         + loginScheme
         + "\" readonly=\"true\"/>\n        </div>\n"
         + "        <div class=\"login-group\">\n          <label class=\"login-label\" for=\"userName\">Username</label>\n          <input id=\"username\" class=\"login-input\" type=\"text\" name=\"j_username\" value=\"\" autofocus/>\n        </div>\n        <div class=\"login-group\">\n          <label class=\"login-label\" for=\"password\">Password</label>\n          <input id=\"password\" class=\"login-input\" type=\"password\" name=\"j_password\" autocomplete=\"off\"/>\n        </div>\n        <input id=\"login-submit\" type=\"submit\" value=\"Login\" onclick=\"return doLogin();\" disabled/>\n        <script type=\"text/javascript\">\n           if (typeof doLogin === 'function') { document.getElementById('login-submit').disabled = false; }\n        </script>\n      </div>\n    </fieldset>\n  </form>\n</body>\n</html>\n";
   }
}
