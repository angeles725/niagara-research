package javax.baja.nre.security;

public final class ClientTlsParameters extends TlsParameters {
   public static final ClientTlsParameters DEFAULT = new ClientTlsParameters();

   public ClientTlsParameters() {
      this.setTlsCipherSuiteGroup(TlsCipherSuiteGroup.supported);
   }

   public ClientTlsParameters(String minTlsProtocol) {
      this.setMinTlsProtocol(minTlsProtocol);
      this.setTlsCipherSuiteGroup(TlsCipherSuiteGroup.supported);
   }

   public ClientTlsParameters(String minTlsProtocol, String certAlias) {
      this.setMinTlsProtocol(minTlsProtocol);
      this.setCertAlias(certAlias);
      this.setTlsCipherSuiteGroup(TlsCipherSuiteGroup.supported);
   }

   @Override
   public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("ClientTlsParameters:[minTlsProtocol:").append(this.minTlsProtocol);
      builder.append(", certAlias:").append(this.certAlias == null ? "null" : this.certAlias);
      builder.append(", tlsCipherSuiteGroup:").append(this.group);
      builder.append("]");
      return builder.toString();
   }
}
