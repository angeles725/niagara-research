package javax.baja.xml;

public class XException extends RuntimeException {
   private static final long serialVersionUID = 879536511108390186L;
   private int line;
   private int col;
   private Throwable cause;
   private XElem elem;

   public XException(String msg, int line, int column, Throwable cause) {
      super(format(msg, line, column));
      this.line = line;
      this.col = column;
      this.cause = cause;
   }

   public XException(String msg, int line, int column) {
      this(msg, line, column, null);
   }

   public XException(String msg, int line, Throwable cause) {
      this(msg, line, 0, cause);
   }

   public XException(String msg, int line) {
      this(msg, line, 0, null);
   }

   public XException(String msg, XParser parser, Throwable cause) {
      this(msg, parser.line(), parser.column(), cause);
   }

   public XException(String msg, XParser parser) {
      this(msg, parser.line(), parser.column(), null);
   }

   public XException(String msg, XElem elem, Throwable cause) {
      this(msg, elem.line(), 0, cause);
      this.elem = elem;
   }

   public XException(String msg, XElem elem) {
      this(msg, elem.line(), 0, null);
      this.elem = elem;
   }

   public XException(String msg, Throwable cause) {
      this(msg, 0, 0, cause);
   }

   public XException(String msg) {
      this(msg, 0, 0, null);
   }

   public XException(Throwable cause) {
      this("", 0, 0, cause);
   }

   public XException() {
      this("", 0, 0, null);
   }

   public int line() {
      return this.line;
   }

   public int column() {
      return this.col;
   }

   public XElem getElem() {
      return null;
   }

   @Override
   public Throwable getCause() {
      return this.cause;
   }

   public static String format(String msg, int line, int col) {
      if (line == 0 && col == 0) {
         return msg;
      } else {
         return col == 0 ? msg + " [line " + line + ']' : msg + " [" + line + ':' + col + ']';
      }
   }
}
