package com.tridium.fox.session;

public class FoxAuthenticationUtil {
   public static final String FOX_VERSION = "1.0.2";
   public static final String FOX_MTD_BASIC_AUTH = "basic";
   public static final String FOX_MTD_DIGEST_AUTH = "digest";
   public static final String FOX_MTD_DIGEST_AUTH_LEGACY = "digest-md5";
   public static final String FOX_MTD_KERBEROS_AUTH = "kerberos";
   public static final String FOX_AUTHFAIL_REASON = "authfail_reason";
   public static final String CMD_LOGIN = "login";
   public static final String CMD_RESET = "accountReset";
   public static final String CMD_AUTH_FIRST_MESSAGE = "authMessage1";
   public static final String CMD_AUTH_SECOND_MESSAGE = "authMessage2";
   public static final String KEY_AUTH_INPUT = "authInput";
   public static final String KEY_AUTH_INPUT_HTTP = "authInputHttp";
   public static final String KEY_AUTH_INPUT_SCRAM = "authInputScram";
   public static final String KEY_AUTH_INPUT_RETRIEVE = "authInputRetrieve";
   public static final String KEY_HANDSHAKE_ONE = "authHandshake1";
   public static final String KEY_HANDSHAKE_TWO = "authHandshake2";
   public static final String KEY_AUTH_INPUT_LOCAL = "authInputLocal";
   public static final String KEY_AUTH_INPUT_KERB = "authInputKerb";
   public static final String KEY_CREDENTIALS = "credentials";
   public static final String KEY_NONCE = "nonce";
   public static final String KEY_PASSWORD = "password";
   public static final String KEY_USERNAME = "username";
   public static final String KEY_SESSIONID = "requestedSessionId";
}
