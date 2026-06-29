package javax.baja.lonworks.io;

public interface LonLinkLayer {
   void verifySettings() throws Exception;

   void start() throws Exception;

   void stop();

   void sendLonMessage(AppBuffer var1);
}
