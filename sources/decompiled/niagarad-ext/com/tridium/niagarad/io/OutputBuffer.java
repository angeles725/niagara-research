package com.tridium.niagarad.io;

import com.tridium.niagarad.NiagaraDaemon;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.ByteArrayUtil;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;

public class OutputBuffer {
   public static final int _MAX_OUTPUT_BUFFER_SIZE = 262144;
   public static final int _MIN_MAX_OUTPUT_BUFFER_SIZE = 8192;
   public static final int _MAX_MAX_OUTPUT_BUFFER_SIZE = 524288;
   public static final int _STREAM_BUFFER_SIZE = 8192;
   protected Logger logger;
   private byte[] buf;
   private boolean full;
   private int nextWrite;
   private int memLimit;
   private MulticastWriter teeWriters;
   protected SessionList teeSessions;
   protected final Object outputMonitor = new Object();

   public OutputBuffer(int memBufferSize, Logger logger) {
      this.init(memBufferSize, logger);
   }

   public void clear() {
      synchronized (this.outputMonitor) {
         this.full = false;
         this.nextWrite = 0;
         ByteArrayUtil.memset(this.buf, (byte)0);
      }
   }

   public void check() {
      synchronized (this.outputMonitor) {
         if (this.teeSessions != null) {
            this.teeSessions.check();
         }
      }
   }

   public void tee(Writer writer) {
      synchronized (this.outputMonitor) {
         this.teeWriters.addWriter(writer);
      }
   }

   public boolean saveToFile(String path, int fileLimit) {
      File target = new File(path);
      label130:
      if (target.exists()) {
         try (BufferedWriter writer = new BufferedWriter(new FileWriter(target), 8192)) {
            synchronized (this.outputMonitor) {
               OutputBuffer.OutputBufferCursor cursor = new OutputBuffer.OutputBufferCursor(this);
               byte[] writeBuf = new byte[8192];
               int nWritten = 0;
               cursor.seek(2, fileLimit);

               int nRead;
               while ((nRead = cursor.read(writeBuf, 0, 8192)) > 0 && nWritten <= fileLimit) {
                  int toWrite = nRead;
                  if (nWritten + nRead > fileLimit) {
                     toWrite = fileLimit - nWritten;
                  }

                  writer.write(new String(writeBuf, 0, toWrite, StandardCharsets.UTF_8));
                  nWritten += toWrite;
               }
            }

            writer.flush();
            return true;
         } catch (IOException ioe) {
            this.logger.severe("failed to write buffer contents for save operation (" + ioe + ")");
            return false;
         }
      } else {
         if (target.getParentFile() != null && !target.getParentFile().exists() && !target.getParentFile().mkdirs()) {
            this.logger.severe("failed to create file for save operation (mkdirs() returned false)");
            return false;
         }

         try {
            if (!target.createNewFile()) {
               this.logger.severe("failed to create file for save operation (createNewFile() returned false)");
               return false;
            }
            break label130;
         } catch (IOException e) {
            this.logger.severe("failed to create file for save operation (" + e + ")");
            return false;
         }
      }
   }

   public boolean loadFile(String path) {
      char[] buf = new char[8192];
      File file = new File(path);
      if (!file.exists()) {
         return false;
      }

      try (FileReader reader = new FileReader(file)) {
         int nRead;
         while ((nRead = reader.read(buf, 0, 8192)) > 0) {
            this.writeBuffer(new String(buf, 0, nRead).getBytes(StandardCharsets.UTF_8), nRead);
         }

         return true;
      } catch (IOException e) {
         return false;
      }
   }

   public void printf(byte[] line) {
      this.writeBuffer(line, line.length);
   }

   public int resetMemBufferSize(int memBufferSize) {
      int result = 0;
      synchronized (this.outputMonitor) {
         try {
            this.buf = new byte[memBufferSize];
            this.full = false;
            this.nextWrite = 0;
            this.memLimit = this.buf.length - 1;
            ByteArrayUtil.memset(this.buf, (byte)0);
         } catch (OutOfMemoryError oome) {
            result = -1;
            this.logger.severe("out of memory exception occurred resetting output buffer, ignoring reset: " + oome);
         }

         return result;
      }
   }

   public int getMemBufferSize() {
      synchronized (this.outputMonitor) {
         return this.memLimit + 1;
      }
   }

   public boolean streamBufferContents(Session out, boolean follow, boolean updatesOnly) {
      this.logger.fine("OutputBuffer: streaming output to websocket");
      if (!updatesOnly) {
         OutputBuffer.OutputBufferCursor cursor = new OutputBuffer.OutputBufferCursor(this);
         byte[] cursorBuffer = new byte[8192];
         boolean wroteAny = false;

         try {
            int nRead;
            while ((nRead = cursor.read(cursorBuffer, 0, 8192)) > 0) {
               out.getRemote().sendString(new String(cursorBuffer, 0, nRead, StandardCharsets.UTF_8));
               wroteAny = true;
            }

            if (wroteAny) {
               out.getRemote().flush();
            } else if (this == NiagaraDaemon.niagaraDaemonOutputBuffer && NiagaraDaemon.getFilter().getLevel() != Level.OFF) {
               this.logger.warning("OutputBuffer: no existing output available when streaming niagara daemon content");
            }
         } catch (IOException | WebSocketException ex) {
            this.logger.log(Level.WARNING, "OutputBuffer: error streaming existing output buffer contents to socket (" + ex + ")");
            if (this.logger.isLoggable(Level.FINEST)) {
               this.logger.log(Level.WARNING, "Stack trace: ", ex);
            }

            return false;
         }
      }

      if (follow) {
         out.setIdleTimeout(30000L);
         if (this.logger.isLoggable(Level.FINE)) {
            this.logger.fine("OutputBuffer: adding output websocket session '" + out.getRemoteAddress() + "' to session list");
         }

         this.teeSessions.addSession(out);
      } else {
         out.setIdleTimeout(10000L);
         if (this.logger.isLoggable(Level.FINE)) {
            this.logger.fine("OutputBuffer: output websocket session '" + out.getRemoteAddress() + "' not following, session timeout decreased");
         }
      }

      return true;
   }

   public void writeBuffer(byte[] buffer, int len) {
      synchronized (this.outputMonitor) {
         if (this.buf != null) {
            if (len <= this.memLimit - this.nextWrite) {
               System.arraycopy(buffer, 0, this.buf, this.nextWrite, len);
               this.nextWrite += len;
               if (this.nextWrite == this.memLimit) {
                  this.full = true;
                  this.nextWrite = 0;
               }
            } else if (len >= this.memLimit) {
               this.full = true;
               System.arraycopy(buffer, len - this.memLimit, this.buf, 0, this.memLimit);
               this.nextWrite = this.memLimit - 1;
            } else {
               this.full = true;
               int firstCopy = this.memLimit - this.nextWrite;
               int leftover = len - firstCopy;
               System.arraycopy(buffer, 0, this.buf, this.nextWrite, firstCopy);
               System.arraycopy(buffer, firstCopy, this.buf, 0, leftover);
               this.nextWrite = leftover;
            }
         }

         String message = new String(buffer, 0, len, StandardCharsets.UTF_8);

         try {
            this.teeWriters.write(message);
            this.teeWriters.flush();
         } catch (IOException var7) {
         }

         this.teeSessions.sendAll(message, true);
      }
   }

   private void init(int memBufferSize, Logger logger) {
      synchronized (this.outputMonitor) {
         this.resetMemBufferSize(memBufferSize);
         this.teeWriters = new MulticastWriter();
         this.teeSessions = new SessionList(logger);
         this.logger = logger;
         OutputBufferList.getInstance().addBuffer(this);
      }
   }

   private static class OutputBufferCursor {
      private OutputBuffer buffer = null;
      private int nextRead = -1;
      private int lastNextWrite = -1;
      public static final int OBC_SEEK_END = 2;

      protected OutputBufferCursor(OutputBuffer pBuffer) {
         this.buffer = pBuffer;
         this.lastNextWrite = -1;
         if (pBuffer.full) {
            this.nextRead = pBuffer.nextWrite;
         } else {
            this.nextRead = 0;
         }
      }

      protected int read(byte[] dest, int offset, int bytesRequested) {
         if (this.buffer.getMemBufferSize() <= 0) {
            return 0;
         }

         synchronized (this.buffer.outputMonitor) {
            if (this.nextRead == this.buffer.nextWrite && this.nextRead == this.lastNextWrite) {
               return 0;
            }

            this.lastNextWrite = this.buffer.nextWrite;
            if (this.nextRead >= this.buffer.nextWrite) {
               if (this.buffer.full) {
                  int bytesRemaining = this.buffer.getMemBufferSize() - this.nextRead - 1;
                  if (bytesRemaining > bytesRequested) {
                     System.arraycopy(this.buffer.buf, this.nextRead, dest, offset, bytesRequested);
                     this.nextRead += bytesRequested;
                     return bytesRequested;
                  } else {
                     System.arraycopy(this.buffer.buf, this.nextRead, dest, offset, bytesRemaining);
                     this.nextRead = 0;
                     return bytesRemaining + this.read(dest, bytesRemaining, bytesRequested - bytesRemaining);
                  }
               } else {
                  return 0;
               }
            } else {
               int bytesRemaining = this.buffer.nextWrite - this.nextRead;
               if (bytesRemaining > bytesRequested) {
                  System.arraycopy(this.buffer.buf, this.nextRead, dest, offset, bytesRequested);
                  this.nextRead += bytesRequested;
                  return bytesRequested;
               } else {
                  System.arraycopy(this.buffer.buf, this.nextRead, dest, offset, bytesRemaining);
                  this.nextRead += bytesRemaining;
                  return bytesRemaining;
               }
            }
         }
      }

      protected int seek(int seekMode, int offset) {
         if (this.buffer.getMemBufferSize() <= 0) {
            return -1;
         }

         if (offset >= this.buffer.getMemBufferSize()) {
            if (this.buffer.full) {
               this.nextRead = this.buffer.nextWrite;
            } else {
               this.nextRead = 0;
            }
         } else {
            if (seekMode != 2) {
               return -1;
            }

            this.nextRead = this.buffer.nextWrite - offset;
            if (this.buffer.full) {
               if (this.nextRead < 0) {
                  this.nextRead = this.nextRead + this.buffer.getMemBufferSize();
               }
            } else if (this.nextRead < 0) {
               this.nextRead = 0;
            }
         }

         return 0;
      }
   }
}
