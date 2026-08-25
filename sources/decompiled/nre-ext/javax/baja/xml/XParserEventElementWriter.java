package javax.baja.xml;

import java.util.logging.Logger;

public class XParserEventElementWriter implements XParserEventListener {
   private static final Logger logger = Logger.getLogger(XParserEventElementWriter.class.getName());
   private final XWriter writer;

   public XParserEventElementWriter(XWriter writer) {
      this.writer = writer;
   }

   @Override
   public void handleEvent(XParserEvent event) {
      switch (event.getEventId()) {
         case -1:
         case 0:
         default:
            break;
         case 1:
            ((XElem)event.getContent()).write(this.writer, 0, event.isSelfClosing());
            break;
         case 2:
            if (!event.isSelfClosing()) {
               this.writer.write("</" + ((XElem)event.getContent()).qname() + ">");
            }
            break;
         case 3:
            event.getContent().write(this.writer);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("XParserEventElementWriter{");
      sb.append("writer=").append(this.writer);
      sb.append('}');
      return sb.toString();
   }
}
