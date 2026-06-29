package com.tridium.fox.session;

import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.sys.BFoxChannel;
import com.tridium.fox.sys.BFoxChannelRegistry;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.nre.util.SecurityUtil;

public class SessionCircuits {
   static final int THREAD_POOL_SIZE = 2;
   private static final boolean NON_DEFAULT_THREAD_PRIORITIES_DISABLED = AccessController.doPrivileged(
      (PrivilegedAction<Boolean>)(() -> Boolean.getBoolean("niagara.fox.nonDefaultThreadPrioritiesDisabled"))
   );
   private static final Map<String, Map<String, Integer>> CHANNEL_CMD_TO_THREAD_PRIORITY_MAP = new ConcurrentHashMap<>();
   private final FoxSession session;
   private final IntMap circuits = new IntMap();
   private int nextId;
   private final SessionCircuits.ServiceThread[] threadPool = new SessionCircuits.ServiceThread[2];
   private int nonPoolServiceCount;

   SessionCircuits(FoxSession session) {
      this.session = session;
      this.nextId = session.isServer() ? 0 : 1;
   }

   synchronized FoxCircuit alloc(String chan, String cmd, FoxMessage meta) {
      int id = this.nextId;
      this.nextId += 2;
      FoxCircuit circuit = new FoxCircuit(id, this.session, chan, cmd, meta);
      this.circuits.put(id, circuit);
      return circuit;
   }

   synchronized void free(FoxCircuit c) {
      this.circuits.remove(c.id);
   }

   void kill() {
      FoxCircuit[] c = (FoxCircuit[])this.circuits.toArray(new FoxCircuit[this.circuits.size()]);

      for (int i = 0; i < c.length; i++) {
         try {
            c[i].close();
         } catch (Throwable var8) {
            var8.printStackTrace();
         }
      }

      synchronized (this.threadPool) {
         for (int i = 0; i < this.threadPool.length; i++) {
            if (this.threadPool[i] != null) {
               try {
                  this.threadPool[i].kill();
               } catch (Throwable var6) {
                  var6.printStackTrace();
               }

               this.threadPool[i] = null;
            }
         }
      }
   }

   void circuitRequestReceived(FoxFrame frame) throws Exception {
      String command = frame.command;
      if (command == "open") {
         this.processOpen(frame.message);
      } else if (command == "stream") {
         this.processStream(frame.message);
      } else if (command == "close") {
         this.processClose(frame.message);
      } else {
         throw new InvalidCommandException(command);
      }
   }

   void processOpen(FoxMessage msg) throws Exception {
      int id = msg.getInt("id");
      String channel = msg.getString("channel").intern();
      String command = msg.getString("command").intern();
      FoxMessage meta = (FoxMessage)msg.getOptional("metadata");
      FoxCircuit circuit = new FoxCircuit(id, this.session, channel, command, meta);
      synchronized (this) {
         if (this.circuits.get(id) != null) {
            throw new IllegalStateException("Circuit already allocated: " + id);
         }

         this.circuits.put(id, circuit);
      }

      this.runCircuit(circuit);
   }

   void processStream(FoxMessage msg) throws Exception {
      int id = msg.getInt("id");
      byte[] data = msg.getBlob("data");
      FoxCircuit circuit = null;
      synchronized (this) {
         circuit = (FoxCircuit)this.circuits.get(id);
      }

      if (circuit != null) {
         circuit.pushIn(data);
      }
   }

   void processClose(FoxMessage msg) throws Exception {
      int id = msg.getInt("id");
      FoxCircuit circuit = null;
      synchronized (this) {
         circuit = (FoxCircuit)this.circuits.get(id);
      }

      if (circuit != null) {
         circuit.close();
      }
   }

   @Override
   public synchronized String toString() {
      StringBuilder s = new StringBuilder();
      s.append("ThreadPool\n");

      for (int i = 0; i < this.threadPool.length; i++) {
         s.append("  pool[").append(i).append("] = ");
         if (this.threadPool[i] == null) {
            s.append("null");
         } else {
            s.append(this.threadPool[i].serviceCount);
         }

         s.append('\n');
      }

      s.append("  nonPoolServiceCount = " + this.nonPoolServiceCount).append('\n');
      s.append("Circuits\n");
      if (this.circuits.size() == 0) {
         s.append("  none\n");
      } else {
         for (int i = 0; i < this.circuits.size(); i++) {
            FoxCircuit c = (FoxCircuit)this.circuits.get(i);
            if (c != null) {
               s.append("  ").append(c).append('\n');
            }
         }
      }

      s.append("Non-Default Circuit Command Thread Priorities\n");
      CHANNEL_CMD_TO_THREAD_PRIORITY_MAP.forEach(
         (chan, map) -> map.forEach((cmd, pri) -> s.append("  ").append(chan).append(':').append(cmd).append(" = ").append(pri).append('\n'))
      );
      return s.toString();
   }

   void runCircuit(FoxCircuit circuit) {
      Integer threadPriority = getServiceThreadPriorityForCircuit(circuit);
      synchronized (this.threadPool) {
         if (threadPriority == null) {
            for (int i = 0; i < this.threadPool.length; i++) {
               if (this.threadPool[i] == null) {
                  this.threadPool[i] = new SessionCircuits.ServiceThread(
                     this.session, "Fox:Circuit:" + SecurityUtil.calculateSessionIdHash(this.session.getId()) + " (Pooled:" + i + ")", true, null
                  );
                  this.threadPool[i].start();
               }

               if (this.threadPool[i].isAvailable()) {
                  this.threadPool[i].runCircuit(circuit);
                  return;
               }
            }
         }

         this.nonPoolServiceCount++;
         StringBuilder threadName = new StringBuilder();
         threadName.append("Fox:Circuit:").append(SecurityUtil.calculateSessionIdHash(this.session.getId())).append(" (Non-pooled");
         if (threadPriority != null) {
            threadName.append(", pri:").append(threadPriority);
         }

         threadName.append(')');
         SessionCircuits.ServiceThread thread = new SessionCircuits.ServiceThread(this.session, threadName.toString(), false, threadPriority);
         thread.start();
         thread.runCircuit(circuit);
      }
   }

   private static Integer getServiceThreadPriorityForCircuit(FoxCircuit circuit) {
      if (NON_DEFAULT_THREAD_PRIORITIES_DISABLED) {
         return null;
      } else {
         Map<String, Integer> cmdToThreadPriorityMap = CHANNEL_CMD_TO_THREAD_PRIORITY_MAP.computeIfAbsent(circuit.channel, k -> {
            Map<String, Integer> cmdToThreadPriority = null;

            try {
               BFoxChannel channel = (BFoxChannel)BFoxChannelRegistry.getPrototype().get(circuit.channel);
               if (channel != null) {
                  cmdToThreadPriority = channel.getCircuitCommandThreadPriorities();
               }
            } catch (Throwable var4) {
               Logger.getLogger("fox").log(Level.WARNING, "Could not initialize circuit command thread priorities for fox channel: " + k, var4);
            }

            if (cmdToThreadPriority == null) {
               cmdToThreadPriority = Collections.emptyMap();
            } else {
               cmdToThreadPriority = Collections.unmodifiableMap(cmdToThreadPriority);
            }

            return cmdToThreadPriority;
         });
         Integer threadPriority = cmdToThreadPriorityMap.get(circuit.command);
         if (threadPriority != null) {
            if (threadPriority < 0) {
               threadPriority = null;
            } else if (threadPriority < 1) {
               threadPriority = 1;
            } else if (threadPriority > 10) {
               threadPriority = 10;
            }
         }

         return threadPriority;
      }
   }

   static class ServiceThread implements Runnable {
      Thread thread;
      boolean isAlive = true;
      boolean pooled;
      FoxCircuit circuit;
      int serviceCount;

      ServiceThread(FoxSession session, String name, boolean pooled, Integer threadPriority) {
         this.thread = session.conn.makeThread(Fox.threadGroup, this, name);
         if (threadPriority != null) {
            this.thread.setPriority(threadPriority);
         }

         this.pooled = pooled;
      }

      public void start() {
         this.thread.start();
      }

      public synchronized void kill() {
         this.isAlive = false;
         this.notify();
      }

      public synchronized boolean isAvailable() {
         return this.circuit == null;
      }

      public synchronized void runCircuit(FoxCircuit circuit) {
         if (this.circuit != null) {
            throw new IllegalStateException();
         } else {
            this.circuit = circuit;
            this.notify();
         }
      }

      @Override
      public void run() {
         while (this.isAlive) {
            synchronized (this) {
               while (this.circuit == null) {
                  if (!this.isAlive) {
                     return;
                  }

                  try {
                     this.wait();
                  } catch (InterruptedException var12) {
                     var12.printStackTrace();
                  }
               }
            }

            this.serviceCount++;

            try {
               this.circuit.session().conn().circuitOpened(this.circuit);
            } catch (Throwable var13) {
               if (var13 instanceof IncompatibleVersionException) {
                  FoxServer.log.log(Level.FINE, var13.getMessage(), var13);
               } else {
                  var13.printStackTrace();
               }
            } finally {
               this.circuit.close();
            }

            synchronized (this) {
               this.circuit = null;
               if (!this.pooled) {
                  return;
               }
            }
         }
      }
   }
}
