package javax.baja.nre.security;

public final class ServerTlsParameters extends TlsParameters {
   private boolean wantClientAuth = false;
   public static final ServerTlsParameters DEFAULT_NO_ALIAS = new ServerTlsParameters("tlsv1");

   public ServerTlsParameters(String minTlsProtocol) {
      this.setMinTlsProtocol(minTlsProtocol);
   }

   public ServerTlsParameters(String minTlsProtocol, String certAlias) {
      this.setMinTlsProtocol(minTlsProtocol);
      this.setCertAlias(certAlias);
   }

   public ServerTlsParameters(String minTlsProtocol, String certAlias, TlsCipherSuiteGroup group) {
      this.setMinTlsProtocol(minTlsProtocol);
      this.setCertAlias(certAlias);
      this.setTlsCipherSuiteGroup(group);
   }

   public ServerTlsParameters(String minTlsProtocol, String certAlias, TlsCipherSuiteGroup group, boolean wantClientAuth) {
      this.setMinTlsProtocol(minTlsProtocol);
      this.setCertAlias(certAlias);
      this.setTlsCipherSuiteGroup(group);
      this.setWantClientAuth(wantClientAuth);
   }

   protected void setWantClientAuth(boolean wantClientAuth) {
      this.wantClientAuth = wantClientAuth;
   }

   public boolean getWantClientCertAuth() {
      return this.wantClientAuth;
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("ServerTlsParameters:[minTlsProtocol:").append(this.minTlsProtocol);
      builder.append(", certAlias:").append(this.certAlias == null ? "null" : this.certAlias);
      builder.append(", tlsCipherSuiteGroup:").append(this.group);
      builder.append(", wantClientAuth:").append(this.wantClientAuth);
      builder.append("]");
      return builder.toString();
   }
}
