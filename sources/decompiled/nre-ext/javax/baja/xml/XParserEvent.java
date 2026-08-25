package javax.baja.xml;

public interface XParserEvent {
   XContent getContent();

   XElemLocation getLocation();

   int getEventId();

   boolean isSelfClosing();
}
