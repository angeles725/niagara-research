package com.tridium.nre.auth;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.apache.thrift.AsyncProcessFunction;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.ProcessFunction;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.TBase;
import org.apache.thrift.TBaseAsyncProcessor;
import org.apache.thrift.TBaseHelper;
import org.apache.thrift.TBaseProcessor;
import org.apache.thrift.TException;
import org.apache.thrift.TFieldIdEnum;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TSerializable;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.TServiceClientFactory;
import org.apache.thrift.annotation.Nullable;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClient;
import org.apache.thrift.async.TAsyncClientFactory;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.async.TAsyncMethodCall;
import org.apache.thrift.async.TAsyncMethodCall.State;
import org.apache.thrift.meta_data.FieldMetaData;
import org.apache.thrift.meta_data.FieldValueMetaData;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;
import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.server.AbstractNonblockingServer.AsyncFrameBuffer;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.apache.thrift.transport.TNonblockingTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SnapUserManager {
   private static final int _MAXIMUM_ARGUMENT_LENGTH = 4096;
   private static final int _THRIFT_USERMGR_PORT = 9091;

   public static String addUserAccount(String name, String password, String comment, boolean passwordHashed) {
      if (!NativeAccount.isAccountQualifierValid(name)) {
         return null;
      }

      if (name.length() > 4096) {
         return null;
      }

      if (password == null) {
         return null;
      }

      if (password.length() > 4096) {
         return null;
      }

      if (comment == null) {
         return null;
      }

      if (comment.length() > 4096) {
         return null;
      }

      if (SnapUserManager.ThriftClientHolder._THRIFT_CLIENT_INSTANCE == null) {
         return null;
      }

      String username = NativeAccount.isAccountNameFullyQualified(name) ? new UserAccount(name, null).getAccountName() : name;

      String result;
      try {
         result = AccessController.doPrivileged(
            () -> SnapUserManager.ThriftClientHolder._THRIFT_CLIENT_INSTANCE.addUserAccount(username, password, comment, passwordHashed)
         );
      } catch (PrivilegedActionException pae) {
         boolean suppressException = false;
         if (pae.getCause() instanceof TApplicationException) {
            TApplicationException thriftException = (TApplicationException)pae.getCause();
            suppressException = thriftException.getType() == 6;
         }

         if (!suppressException) {
            pae.printStackTrace();
         }

         return null;
      }

      return !result.contains(":") ? null : result.split(":")[1];
   }

   public static boolean removeUserAccount(String userId) {
      if (userId == null) {
         return false;
      }

      if (userId.length() > 4096) {
         return false;
      }

      if (SnapUserManager.ThriftClientHolder._THRIFT_CLIENT_INSTANCE == null) {
         return false;
      }

      boolean rc = false;

      try {
         rc = AccessController.doPrivileged(() -> SnapUserManager.ThriftClientHolder._THRIFT_CLIENT_INSTANCE.removeUserAccount(userId));
      } catch (PrivilegedActionException pae) {
         boolean suppressException = false;
         if (pae.getCause() instanceof TApplicationException) {
            TApplicationException thriftException = (TApplicationException)pae.getCause();
            suppressException = thriftException.getType() == 6;
         }

         if (!suppressException) {
            pae.printStackTrace();
         }
      }

      return rc;
   }

   public static boolean addUserToGroup(String userId, String groupId) {
      if (userId == null) {
         return false;
      }

      if (userId.length() > 4096) {
         return false;
      }

      if (groupId == null) {
         return false;
      }

      if (groupId.length() > 4096) {
         return false;
      }

      if (SnapUserManager.ThriftClientHolder._THRIFT_CLIENT_INSTANCE == null) {
         return false;
      }

      boolean rc = false;

      try {
         rc = AccessController.doPrivileged(() -> SnapUserManager.ThriftClientHolder._THRIFT_CLIENT_INSTANCE.addUserToGroup(userId, groupId));
      } catch (PrivilegedActionException pae) {
         boolean suppressException = false;
         if (pae.getCause() instanceof TApplicationException) {
            TApplicationException thriftException = (TApplicationException)pae.getCause();
            suppressException = thriftException.getType() == 6;
         }

         if (!suppressException) {
            pae.printStackTrace();
         }
      }

      return rc;
   }

   public static boolean removeUserFromGroup(String userId, String groupId) {
      if (userId == null) {
         return false;
      }

      if (userId.length() > 4096) {
         return false;
      }

      if (groupId == null) {
         return false;
      }

      if (groupId.length() > 4096) {
         return false;
      }

      if (SnapUserManager.ThriftClientHolder._THRIFT_CLIENT_INSTANCE == null) {
         return false;
      }

      boolean rc = false;

      try {
         rc = AccessController.doPrivileged(() -> SnapUserManager.ThriftClientHolder._THRIFT_CLIENT_INSTANCE.removeUserFromGroup(userId, groupId));
      } catch (PrivilegedActionException pae) {
         boolean suppressException = false;
         if (pae.getCause() instanceof TApplicationException) {
            TApplicationException thriftException = (TApplicationException)pae.getCause();
            suppressException = thriftException.getType() == 6;
         }

         if (!suppressException) {
            pae.printStackTrace();
         }
      }

      return rc;
   }

   public static boolean changeUserPassword(String userId, String oldPassword, String newPassword) {
      if (userId == null) {
         return false;
      }

      if (userId.length() > 4096) {
         return false;
      }

      if (oldPassword == null) {
         return false;
      }

      if (oldPassword.length() > 4096) {
         return false;
      }

      if (newPassword == null) {
         return false;
      }

      if (newPassword.length() > 4096) {
         return false;
      }

      if (SnapUserManager.ThriftClientHolder._THRIFT_CLIENT_INSTANCE == null) {
         return false;
      }

      boolean rc = false;

      try {
         rc = AccessController.doPrivileged(
            () -> SnapUserManager.ThriftClientHolder._THRIFT_CLIENT_INSTANCE.changeUserPassword(userId, oldPassword, newPassword)
         );
      } catch (PrivilegedActionException pae) {
         boolean suppressException = false;
         if (pae.getCause() instanceof TApplicationException) {
            TApplicationException thriftException = (TApplicationException)pae.getCause();
            suppressException = thriftException.getType() == 6;
         }

         if (!suppressException) {
            pae.printStackTrace();
         }
      }

      return rc;
   }

   private static SnapUserManager.Client loadThriftClient() {
      try {
         return AccessController.doPrivileged(() -> {
            TTransport transport = new TSocket("localhost", 9091);
            transport.open();
            TProtocol protocol = new TBinaryProtocol(transport);
            return new SnapUserManager.Client(protocol);
         });
      } catch (PrivilegedActionException pae) {
         System.err.println("SEVERE [" + new Date() + "][nre] error creating usermgr client connection (" + pae + ")");
         pae.printStackTrace();
         return null;
      }
   }

   public static class AsyncClient extends TAsyncClient implements SnapUserManager.AsyncIface {
      public AsyncClient(TProtocolFactory protocolFactory, TAsyncClientManager clientManager, TNonblockingTransport transport) {
         super(protocolFactory, clientManager, transport);
      }

      @Override
      public void addUserAccount(String name, String password, String comment, boolean passwordHashed, AsyncMethodCallback<String> resultHandler) throws TException {
         this.checkReady();
         SnapUserManager.AsyncClient.addUserAccount_call method_call = new SnapUserManager.AsyncClient.addUserAccount_call(
            name, password, comment, passwordHashed, resultHandler, this, this.___protocolFactory, this.___transport
         );
         this.___currentMethod = method_call;
         this.___manager.call(method_call);
      }

      @Override
      public void removeUserAccount(String userId, AsyncMethodCallback<Boolean> resultHandler) throws TException {
         this.checkReady();
         SnapUserManager.AsyncClient.removeUserAccount_call method_call = new SnapUserManager.AsyncClient.removeUserAccount_call(
            userId, resultHandler, this, this.___protocolFactory, this.___transport
         );
         this.___currentMethod = method_call;
         this.___manager.call(method_call);
      }

      @Override
      public void addUserToGroup(String userId, String groupId, AsyncMethodCallback<Boolean> resultHandler) throws TException {
         this.checkReady();
         SnapUserManager.AsyncClient.addUserToGroup_call method_call = new SnapUserManager.AsyncClient.addUserToGroup_call(
            userId, groupId, resultHandler, this, this.___protocolFactory, this.___transport
         );
         this.___currentMethod = method_call;
         this.___manager.call(method_call);
      }

      @Override
      public void removeUserFromGroup(String userId, String groupId, AsyncMethodCallback<Boolean> resultHandler) throws TException {
         this.checkReady();
         SnapUserManager.AsyncClient.removeUserFromGroup_call method_call = new SnapUserManager.AsyncClient.removeUserFromGroup_call(
            userId, groupId, resultHandler, this, this.___protocolFactory, this.___transport
         );
         this.___currentMethod = method_call;
         this.___manager.call(method_call);
      }

      @Override
      public void changeUserPassword(String userId, String oldPassword, String newPassword, AsyncMethodCallback<Boolean> resultHandler) throws TException {
         this.checkReady();
         SnapUserManager.AsyncClient.changeUserPassword_call method_call = new SnapUserManager.AsyncClient.changeUserPassword_call(
            userId, oldPassword, newPassword, resultHandler, this, this.___protocolFactory, this.___transport
         );
         this.___currentMethod = method_call;
         this.___manager.call(method_call);
      }

      public static class Factory implements TAsyncClientFactory<SnapUserManager.AsyncClient> {
         private TAsyncClientManager clientManager;
         private TProtocolFactory protocolFactory;

         public Factory(TAsyncClientManager clientManager, TProtocolFactory protocolFactory) {
            this.clientManager = clientManager;
            this.protocolFactory = protocolFactory;
         }

         public SnapUserManager.AsyncClient getAsyncClient(TNonblockingTransport transport) {
            return new SnapUserManager.AsyncClient(this.protocolFactory, this.clientManager, transport);
         }
      }

      public static class addUserAccount_call extends TAsyncMethodCall<String> {
         private String name;
         private String password;
         private String comment;
         private boolean passwordHashed;

         public addUserAccount_call(
            String name,
            String password,
            String comment,
            boolean passwordHashed,
            AsyncMethodCallback<String> resultHandler,
            TAsyncClient client,
            TProtocolFactory protocolFactory,
            TNonblockingTransport transport
         ) throws TException {
            super(client, protocolFactory, transport, resultHandler, false);
            this.name = name;
            this.password = password;
            this.comment = comment;
            this.passwordHashed = passwordHashed;
         }

         public void write_args(TProtocol prot) throws TException {
            prot.writeMessageBegin(new TMessage("addUserAccount", (byte)1, 0));
            SnapUserManager.addUserAccount_args args = new SnapUserManager.addUserAccount_args();
            args.setName(this.name);
            args.setPassword(this.password);
            args.setComment(this.comment);
            args.setPasswordHashed(this.passwordHashed);
            args.write(prot);
            prot.writeMessageEnd();
         }

         public String getResult() throws TException {
            if (this.getState() != State.RESPONSE_READ) {
               throw new IllegalStateException("Method call not finished!");
            }

            TMemoryInputTransport memoryTransport = new TMemoryInputTransport(this.getFrameBuffer().array());
            TProtocol prot = this.client.getProtocolFactory().getProtocol(memoryTransport);
            return new SnapUserManager.Client(prot).recv_addUserAccount();
         }
      }

      public static class addUserToGroup_call extends TAsyncMethodCall<Boolean> {
         private String userId;
         private String groupId;

         public addUserToGroup_call(
            String userId,
            String groupId,
            AsyncMethodCallback<Boolean> resultHandler,
            TAsyncClient client,
            TProtocolFactory protocolFactory,
            TNonblockingTransport transport
         ) throws TException {
            super(client, protocolFactory, transport, resultHandler, false);
            this.userId = userId;
            this.groupId = groupId;
         }

         public void write_args(TProtocol prot) throws TException {
            prot.writeMessageBegin(new TMessage("addUserToGroup", (byte)1, 0));
            SnapUserManager.addUserToGroup_args args = new SnapUserManager.addUserToGroup_args();
            args.setUserId(this.userId);
            args.setGroupId(this.groupId);
            args.write(prot);
            prot.writeMessageEnd();
         }

         public Boolean getResult() throws TException {
            if (this.getState() != State.RESPONSE_READ) {
               throw new IllegalStateException("Method call not finished!");
            }

            TMemoryInputTransport memoryTransport = new TMemoryInputTransport(this.getFrameBuffer().array());
            TProtocol prot = this.client.getProtocolFactory().getProtocol(memoryTransport);
            return new SnapUserManager.Client(prot).recv_addUserToGroup();
         }
      }

      public static class changeUserPassword_call extends TAsyncMethodCall<Boolean> {
         private String userId;
         private String oldPassword;
         private String newPassword;

         public changeUserPassword_call(
            String userId,
            String oldPassword,
            String newPassword,
            AsyncMethodCallback<Boolean> resultHandler,
            TAsyncClient client,
            TProtocolFactory protocolFactory,
            TNonblockingTransport transport
         ) throws TException {
            super(client, protocolFactory, transport, resultHandler, false);
            this.userId = userId;
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
         }

         public void write_args(TProtocol prot) throws TException {
            prot.writeMessageBegin(new TMessage("changeUserPassword", (byte)1, 0));
            SnapUserManager.changeUserPassword_args args = new SnapUserManager.changeUserPassword_args();
            args.setUserId(this.userId);
            args.setOldPassword(this.oldPassword);
            args.setNewPassword(this.newPassword);
            args.write(prot);
            prot.writeMessageEnd();
         }

         public Boolean getResult() throws TException {
            if (this.getState() != State.RESPONSE_READ) {
               throw new IllegalStateException("Method call not finished!");
            }

            TMemoryInputTransport memoryTransport = new TMemoryInputTransport(this.getFrameBuffer().array());
            TProtocol prot = this.client.getProtocolFactory().getProtocol(memoryTransport);
            return new SnapUserManager.Client(prot).recv_changeUserPassword();
         }
      }

      public static class removeUserAccount_call extends TAsyncMethodCall<Boolean> {
         private String userId;

         public removeUserAccount_call(
            String userId, AsyncMethodCallback<Boolean> resultHandler, TAsyncClient client, TProtocolFactory protocolFactory, TNonblockingTransport transport
         ) throws TException {
            super(client, protocolFactory, transport, resultHandler, false);
            this.userId = userId;
         }

         public void write_args(TProtocol prot) throws TException {
            prot.writeMessageBegin(new TMessage("removeUserAccount", (byte)1, 0));
            SnapUserManager.removeUserAccount_args args = new SnapUserManager.removeUserAccount_args();
            args.setUserId(this.userId);
            args.write(prot);
            prot.writeMessageEnd();
         }

         public Boolean getResult() throws TException {
            if (this.getState() != State.RESPONSE_READ) {
               throw new IllegalStateException("Method call not finished!");
            }

            TMemoryInputTransport memoryTransport = new TMemoryInputTransport(this.getFrameBuffer().array());
            TProtocol prot = this.client.getProtocolFactory().getProtocol(memoryTransport);
            return new SnapUserManager.Client(prot).recv_removeUserAccount();
         }
      }

      public static class removeUserFromGroup_call extends TAsyncMethodCall<Boolean> {
         private String userId;
         private String groupId;

         public removeUserFromGroup_call(
            String userId,
            String groupId,
            AsyncMethodCallback<Boolean> resultHandler,
            TAsyncClient client,
            TProtocolFactory protocolFactory,
            TNonblockingTransport transport
         ) throws TException {
            super(client, protocolFactory, transport, resultHandler, false);
            this.userId = userId;
            this.groupId = groupId;
         }

         public void write_args(TProtocol prot) throws TException {
            prot.writeMessageBegin(new TMessage("removeUserFromGroup", (byte)1, 0));
            SnapUserManager.removeUserFromGroup_args args = new SnapUserManager.removeUserFromGroup_args();
            args.setUserId(this.userId);
            args.setGroupId(this.groupId);
            args.write(prot);
            prot.writeMessageEnd();
         }

         public Boolean getResult() throws TException {
            if (this.getState() != State.RESPONSE_READ) {
               throw new IllegalStateException("Method call not finished!");
            }

            TMemoryInputTransport memoryTransport = new TMemoryInputTransport(this.getFrameBuffer().array());
            TProtocol prot = this.client.getProtocolFactory().getProtocol(memoryTransport);
            return new SnapUserManager.Client(prot).recv_removeUserFromGroup();
         }
      }
   }

   public interface AsyncIface {
      void addUserAccount(String var1, String var2, String var3, boolean var4, AsyncMethodCallback<String> var5) throws TException;

      void removeUserAccount(String var1, AsyncMethodCallback<Boolean> var2) throws TException;

      void addUserToGroup(String var1, String var2, AsyncMethodCallback<Boolean> var3) throws TException;

      void removeUserFromGroup(String var1, String var2, AsyncMethodCallback<Boolean> var3) throws TException;

      void changeUserPassword(String var1, String var2, String var3, AsyncMethodCallback<Boolean> var4) throws TException;
   }

   public static class AsyncProcessor<I extends SnapUserManager.AsyncIface> extends TBaseAsyncProcessor<I> {
      private static final Logger _LOGGER = LoggerFactory.getLogger(SnapUserManager.AsyncProcessor.class.getName());

      public AsyncProcessor(I iface) {
         super(iface, getProcessMap(new HashMap<>()));
      }

      protected AsyncProcessor(I iface, Map<String, AsyncProcessFunction<I, ? extends TBase, ?>> processMap) {
         super(iface, getProcessMap(processMap));
      }

      private static <I extends SnapUserManager.AsyncIface> Map<String, AsyncProcessFunction<I, ? extends TBase, ?>> getProcessMap(
         Map<String, AsyncProcessFunction<I, ? extends TBase, ?>> processMap
      ) {
         processMap.put("addUserAccount", new SnapUserManager.AsyncProcessor.addUserAccount());
         processMap.put("removeUserAccount", new SnapUserManager.AsyncProcessor.removeUserAccount());
         processMap.put("addUserToGroup", new SnapUserManager.AsyncProcessor.addUserToGroup());
         processMap.put("removeUserFromGroup", new SnapUserManager.AsyncProcessor.removeUserFromGroup());
         processMap.put("changeUserPassword", new SnapUserManager.AsyncProcessor.changeUserPassword());
         return processMap;
      }

      public static class addUserAccount<I extends SnapUserManager.AsyncIface> extends AsyncProcessFunction<I, SnapUserManager.addUserAccount_args, String> {
         public addUserAccount() {
            super("addUserAccount");
         }

         public SnapUserManager.addUserAccount_args getEmptyArgsInstance() {
            return new SnapUserManager.addUserAccount_args();
         }

         public AsyncMethodCallback<String> getResultHandler(final AsyncFrameBuffer fb, final int seqid) {
            final AsyncProcessFunction fcall = this;
            return new AsyncMethodCallback<String>() {
               public void onComplete(String o) {
                  SnapUserManager.addUserAccount_result result = new SnapUserManager.addUserAccount_result();
                  result.success = o;

                  try {
                     fcall.sendResponse(fb, result, (byte)2, seqid);
                  } catch (TTransportException e) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("TTransportException writing to internal frame buffer", e);
                     fb.close();
                  } catch (Exception e) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("Exception writing to internal frame buffer", e);
                     this.onError(e);
                  }
               }

               public void onError(Exception e) {
                  byte msgType = 2;
                  new SnapUserManager.addUserAccount_result();
                  if (e instanceof TTransportException) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("TTransportException inside handler", e);
                     fb.close();
                  } else {
                     TSerializable msg;
                     if (e instanceof TApplicationException) {
                        SnapUserManager.AsyncProcessor._LOGGER.error("TApplicationException inside handler", e);
                        msgType = 3;
                        msg = (TApplicationException)e;
                     } else {
                        SnapUserManager.AsyncProcessor._LOGGER.error("Exception inside handler", e);
                        msgType = 3;
                        msg = new TApplicationException(6, e.getMessage());
                     }

                     try {
                        fcall.sendResponse(fb, msg, msgType, seqid);
                     } catch (Exception ex) {
                        SnapUserManager.AsyncProcessor._LOGGER.error("Exception writing to internal frame buffer", ex);
                        fb.close();
                     }
                  }
               }
            };
         }

         protected boolean isOneway() {
            return false;
         }

         public void start(I iface, SnapUserManager.addUserAccount_args args, AsyncMethodCallback<String> resultHandler) throws TException {
            iface.addUserAccount(args.name, args.password, args.comment, args.passwordHashed, resultHandler);
         }
      }

      public static class addUserToGroup<I extends SnapUserManager.AsyncIface> extends AsyncProcessFunction<I, SnapUserManager.addUserToGroup_args, Boolean> {
         public addUserToGroup() {
            super("addUserToGroup");
         }

         public SnapUserManager.addUserToGroup_args getEmptyArgsInstance() {
            return new SnapUserManager.addUserToGroup_args();
         }

         public AsyncMethodCallback<Boolean> getResultHandler(final AsyncFrameBuffer fb, final int seqid) {
            final AsyncProcessFunction fcall = this;
            return new AsyncMethodCallback<Boolean>() {
               public void onComplete(Boolean o) {
                  SnapUserManager.addUserToGroup_result result = new SnapUserManager.addUserToGroup_result();
                  result.success = o;
                  result.setSuccessIsSet(true);

                  try {
                     fcall.sendResponse(fb, result, (byte)2, seqid);
                  } catch (TTransportException e) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("TTransportException writing to internal frame buffer", e);
                     fb.close();
                  } catch (Exception e) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("Exception writing to internal frame buffer", e);
                     this.onError(e);
                  }
               }

               public void onError(Exception e) {
                  byte msgType = 2;
                  new SnapUserManager.addUserToGroup_result();
                  if (e instanceof TTransportException) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("TTransportException inside handler", e);
                     fb.close();
                  } else {
                     TSerializable msg;
                     if (e instanceof TApplicationException) {
                        SnapUserManager.AsyncProcessor._LOGGER.error("TApplicationException inside handler", e);
                        msgType = 3;
                        msg = (TApplicationException)e;
                     } else {
                        SnapUserManager.AsyncProcessor._LOGGER.error("Exception inside handler", e);
                        msgType = 3;
                        msg = new TApplicationException(6, e.getMessage());
                     }

                     try {
                        fcall.sendResponse(fb, msg, msgType, seqid);
                     } catch (Exception ex) {
                        SnapUserManager.AsyncProcessor._LOGGER.error("Exception writing to internal frame buffer", ex);
                        fb.close();
                     }
                  }
               }
            };
         }

         protected boolean isOneway() {
            return false;
         }

         public void start(I iface, SnapUserManager.addUserToGroup_args args, AsyncMethodCallback<Boolean> resultHandler) throws TException {
            iface.addUserToGroup(args.userId, args.groupId, resultHandler);
         }
      }

      public static class changeUserPassword<I extends SnapUserManager.AsyncIface>
         extends AsyncProcessFunction<I, SnapUserManager.changeUserPassword_args, Boolean> {
         public changeUserPassword() {
            super("changeUserPassword");
         }

         public SnapUserManager.changeUserPassword_args getEmptyArgsInstance() {
            return new SnapUserManager.changeUserPassword_args();
         }

         public AsyncMethodCallback<Boolean> getResultHandler(final AsyncFrameBuffer fb, final int seqid) {
            final AsyncProcessFunction fcall = this;
            return new AsyncMethodCallback<Boolean>() {
               public void onComplete(Boolean o) {
                  SnapUserManager.changeUserPassword_result result = new SnapUserManager.changeUserPassword_result();
                  result.success = o;
                  result.setSuccessIsSet(true);

                  try {
                     fcall.sendResponse(fb, result, (byte)2, seqid);
                  } catch (TTransportException e) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("TTransportException writing to internal frame buffer", e);
                     fb.close();
                  } catch (Exception e) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("Exception writing to internal frame buffer", e);
                     this.onError(e);
                  }
               }

               public void onError(Exception e) {
                  byte msgType = 2;
                  new SnapUserManager.changeUserPassword_result();
                  if (e instanceof TTransportException) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("TTransportException inside handler", e);
                     fb.close();
                  } else {
                     TSerializable msg;
                     if (e instanceof TApplicationException) {
                        SnapUserManager.AsyncProcessor._LOGGER.error("TApplicationException inside handler", e);
                        msgType = 3;
                        msg = (TApplicationException)e;
                     } else {
                        SnapUserManager.AsyncProcessor._LOGGER.error("Exception inside handler", e);
                        msgType = 3;
                        msg = new TApplicationException(6, e.getMessage());
                     }

                     try {
                        fcall.sendResponse(fb, msg, msgType, seqid);
                     } catch (Exception ex) {
                        SnapUserManager.AsyncProcessor._LOGGER.error("Exception writing to internal frame buffer", ex);
                        fb.close();
                     }
                  }
               }
            };
         }

         protected boolean isOneway() {
            return false;
         }

         public void start(I iface, SnapUserManager.changeUserPassword_args args, AsyncMethodCallback<Boolean> resultHandler) throws TException {
            iface.changeUserPassword(args.userId, args.oldPassword, args.newPassword, resultHandler);
         }
      }

      public static class removeUserAccount<I extends SnapUserManager.AsyncIface>
         extends AsyncProcessFunction<I, SnapUserManager.removeUserAccount_args, Boolean> {
         public removeUserAccount() {
            super("removeUserAccount");
         }

         public SnapUserManager.removeUserAccount_args getEmptyArgsInstance() {
            return new SnapUserManager.removeUserAccount_args();
         }

         public AsyncMethodCallback<Boolean> getResultHandler(final AsyncFrameBuffer fb, final int seqid) {
            final AsyncProcessFunction fcall = this;
            return new AsyncMethodCallback<Boolean>() {
               public void onComplete(Boolean o) {
                  SnapUserManager.removeUserAccount_result result = new SnapUserManager.removeUserAccount_result();
                  result.success = o;
                  result.setSuccessIsSet(true);

                  try {
                     fcall.sendResponse(fb, result, (byte)2, seqid);
                  } catch (TTransportException e) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("TTransportException writing to internal frame buffer", e);
                     fb.close();
                  } catch (Exception e) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("Exception writing to internal frame buffer", e);
                     this.onError(e);
                  }
               }

               public void onError(Exception e) {
                  byte msgType = 2;
                  new SnapUserManager.removeUserAccount_result();
                  if (e instanceof TTransportException) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("TTransportException inside handler", e);
                     fb.close();
                  } else {
                     TSerializable msg;
                     if (e instanceof TApplicationException) {
                        SnapUserManager.AsyncProcessor._LOGGER.error("TApplicationException inside handler", e);
                        msgType = 3;
                        msg = (TApplicationException)e;
                     } else {
                        SnapUserManager.AsyncProcessor._LOGGER.error("Exception inside handler", e);
                        msgType = 3;
                        msg = new TApplicationException(6, e.getMessage());
                     }

                     try {
                        fcall.sendResponse(fb, msg, msgType, seqid);
                     } catch (Exception ex) {
                        SnapUserManager.AsyncProcessor._LOGGER.error("Exception writing to internal frame buffer", ex);
                        fb.close();
                     }
                  }
               }
            };
         }

         protected boolean isOneway() {
            return false;
         }

         public void start(I iface, SnapUserManager.removeUserAccount_args args, AsyncMethodCallback<Boolean> resultHandler) throws TException {
            iface.removeUserAccount(args.userId, resultHandler);
         }
      }

      public static class removeUserFromGroup<I extends SnapUserManager.AsyncIface>
         extends AsyncProcessFunction<I, SnapUserManager.removeUserFromGroup_args, Boolean> {
         public removeUserFromGroup() {
            super("removeUserFromGroup");
         }

         public SnapUserManager.removeUserFromGroup_args getEmptyArgsInstance() {
            return new SnapUserManager.removeUserFromGroup_args();
         }

         public AsyncMethodCallback<Boolean> getResultHandler(final AsyncFrameBuffer fb, final int seqid) {
            final AsyncProcessFunction fcall = this;
            return new AsyncMethodCallback<Boolean>() {
               public void onComplete(Boolean o) {
                  SnapUserManager.removeUserFromGroup_result result = new SnapUserManager.removeUserFromGroup_result();
                  result.success = o;
                  result.setSuccessIsSet(true);

                  try {
                     fcall.sendResponse(fb, result, (byte)2, seqid);
                  } catch (TTransportException e) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("TTransportException writing to internal frame buffer", e);
                     fb.close();
                  } catch (Exception e) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("Exception writing to internal frame buffer", e);
                     this.onError(e);
                  }
               }

               public void onError(Exception e) {
                  byte msgType = 2;
                  new SnapUserManager.removeUserFromGroup_result();
                  if (e instanceof TTransportException) {
                     SnapUserManager.AsyncProcessor._LOGGER.error("TTransportException inside handler", e);
                     fb.close();
                  } else {
                     TSerializable msg;
                     if (e instanceof TApplicationException) {
                        SnapUserManager.AsyncProcessor._LOGGER.error("TApplicationException inside handler", e);
                        msgType = 3;
                        msg = (TApplicationException)e;
                     } else {
                        SnapUserManager.AsyncProcessor._LOGGER.error("Exception inside handler", e);
                        msgType = 3;
                        msg = new TApplicationException(6, e.getMessage());
                     }

                     try {
                        fcall.sendResponse(fb, msg, msgType, seqid);
                     } catch (Exception ex) {
                        SnapUserManager.AsyncProcessor._LOGGER.error("Exception writing to internal frame buffer", ex);
                        fb.close();
                     }
                  }
               }
            };
         }

         protected boolean isOneway() {
            return false;
         }

         public void start(I iface, SnapUserManager.removeUserFromGroup_args args, AsyncMethodCallback<Boolean> resultHandler) throws TException {
            iface.removeUserFromGroup(args.userId, args.groupId, resultHandler);
         }
      }
   }

   public static class Client extends TServiceClient implements SnapUserManager.Iface {
      public Client(TProtocol prot) {
         super(prot, prot);
      }

      public Client(TProtocol iprot, TProtocol oprot) {
         super(iprot, oprot);
      }

      @Override
      public String addUserAccount(String name, String password, String comment, boolean passwordHashed) throws TException {
         this.send_addUserAccount(name, password, comment, passwordHashed);
         return this.recv_addUserAccount();
      }

      public void send_addUserAccount(String name, String password, String comment, boolean passwordHashed) throws TException {
         SnapUserManager.addUserAccount_args args = new SnapUserManager.addUserAccount_args();
         args.setName(name);
         args.setPassword(password);
         args.setComment(comment);
         args.setPasswordHashed(passwordHashed);
         this.sendBase("addUserAccount", args);
      }

      public String recv_addUserAccount() throws TException {
         SnapUserManager.addUserAccount_result result = new SnapUserManager.addUserAccount_result();
         this.receiveBase(result, "addUserAccount");
         if (result.isSetSuccess()) {
            return result.success;
         } else {
            throw new TApplicationException(5, "addUserAccount failed: unknown result");
         }
      }

      @Override
      public boolean removeUserAccount(String userId) throws TException {
         this.send_removeUserAccount(userId);
         return this.recv_removeUserAccount();
      }

      public void send_removeUserAccount(String userId) throws TException {
         SnapUserManager.removeUserAccount_args args = new SnapUserManager.removeUserAccount_args();
         args.setUserId(userId);
         this.sendBase("removeUserAccount", args);
      }

      public boolean recv_removeUserAccount() throws TException {
         SnapUserManager.removeUserAccount_result result = new SnapUserManager.removeUserAccount_result();
         this.receiveBase(result, "removeUserAccount");
         if (result.isSetSuccess()) {
            return result.success;
         } else {
            throw new TApplicationException(5, "removeUserAccount failed: unknown result");
         }
      }

      @Override
      public boolean addUserToGroup(String userId, String groupId) throws TException {
         this.send_addUserToGroup(userId, groupId);
         return this.recv_addUserToGroup();
      }

      public void send_addUserToGroup(String userId, String groupId) throws TException {
         SnapUserManager.addUserToGroup_args args = new SnapUserManager.addUserToGroup_args();
         args.setUserId(userId);
         args.setGroupId(groupId);
         this.sendBase("addUserToGroup", args);
      }

      public boolean recv_addUserToGroup() throws TException {
         SnapUserManager.addUserToGroup_result result = new SnapUserManager.addUserToGroup_result();
         this.receiveBase(result, "addUserToGroup");
         if (result.isSetSuccess()) {
            return result.success;
         } else {
            throw new TApplicationException(5, "addUserToGroup failed: unknown result");
         }
      }

      @Override
      public boolean removeUserFromGroup(String userId, String groupId) throws TException {
         this.send_removeUserFromGroup(userId, groupId);
         return this.recv_removeUserFromGroup();
      }

      public void send_removeUserFromGroup(String userId, String groupId) throws TException {
         SnapUserManager.removeUserFromGroup_args args = new SnapUserManager.removeUserFromGroup_args();
         args.setUserId(userId);
         args.setGroupId(groupId);
         this.sendBase("removeUserFromGroup", args);
      }

      public boolean recv_removeUserFromGroup() throws TException {
         SnapUserManager.removeUserFromGroup_result result = new SnapUserManager.removeUserFromGroup_result();
         this.receiveBase(result, "removeUserFromGroup");
         if (result.isSetSuccess()) {
            return result.success;
         } else {
            throw new TApplicationException(5, "removeUserFromGroup failed: unknown result");
         }
      }

      @Override
      public boolean changeUserPassword(String userId, String oldPassword, String newPassword) throws TException {
         this.send_changeUserPassword(userId, oldPassword, newPassword);
         return this.recv_changeUserPassword();
      }

      public void send_changeUserPassword(String userId, String oldPassword, String newPassword) throws TException {
         SnapUserManager.changeUserPassword_args args = new SnapUserManager.changeUserPassword_args();
         args.setUserId(userId);
         args.setOldPassword(oldPassword);
         args.setNewPassword(newPassword);
         this.sendBase("changeUserPassword", args);
      }

      public boolean recv_changeUserPassword() throws TException {
         SnapUserManager.changeUserPassword_result result = new SnapUserManager.changeUserPassword_result();
         this.receiveBase(result, "changeUserPassword");
         if (result.isSetSuccess()) {
            return result.success;
         } else {
            throw new TApplicationException(5, "changeUserPassword failed: unknown result");
         }
      }

      public static class Factory implements TServiceClientFactory<SnapUserManager.Client> {
         public SnapUserManager.Client getClient(TProtocol prot) {
            return new SnapUserManager.Client(prot);
         }

         public SnapUserManager.Client getClient(TProtocol iprot, TProtocol oprot) {
            return new SnapUserManager.Client(iprot, oprot);
         }
      }
   }

   public interface Iface {
      String addUserAccount(String var1, String var2, String var3, boolean var4) throws TException;

      boolean removeUserAccount(String var1) throws TException;

      boolean addUserToGroup(String var1, String var2) throws TException;

      boolean removeUserFromGroup(String var1, String var2) throws TException;

      boolean changeUserPassword(String var1, String var2, String var3) throws TException;
   }

   public static class Processor<I extends SnapUserManager.Iface> extends TBaseProcessor<I> implements TProcessor {
      private static final Logger _LOGGER = LoggerFactory.getLogger(SnapUserManager.Processor.class.getName());

      public Processor(I iface) {
         super(iface, getProcessMap(new HashMap<>()));
      }

      protected Processor(I iface, Map<String, ProcessFunction<I, ? extends TBase>> processMap) {
         super(iface, getProcessMap(processMap));
      }

      private static <I extends SnapUserManager.Iface> Map<String, ProcessFunction<I, ? extends TBase>> getProcessMap(
         Map<String, ProcessFunction<I, ? extends TBase>> processMap
      ) {
         processMap.put("addUserAccount", new SnapUserManager.Processor.addUserAccount());
         processMap.put("removeUserAccount", new SnapUserManager.Processor.removeUserAccount());
         processMap.put("addUserToGroup", new SnapUserManager.Processor.addUserToGroup());
         processMap.put("removeUserFromGroup", new SnapUserManager.Processor.removeUserFromGroup());
         processMap.put("changeUserPassword", new SnapUserManager.Processor.changeUserPassword());
         return processMap;
      }

      public static class addUserAccount<I extends SnapUserManager.Iface> extends ProcessFunction<I, SnapUserManager.addUserAccount_args> {
         public addUserAccount() {
            super("addUserAccount");
         }

         public SnapUserManager.addUserAccount_args getEmptyArgsInstance() {
            return new SnapUserManager.addUserAccount_args();
         }

         protected boolean isOneway() {
            return false;
         }

         protected boolean rethrowUnhandledExceptions() {
            return false;
         }

         public SnapUserManager.addUserAccount_result getResult(I iface, SnapUserManager.addUserAccount_args args) throws TException {
            SnapUserManager.addUserAccount_result result = new SnapUserManager.addUserAccount_result();
            result.success = iface.addUserAccount(args.name, args.password, args.comment, args.passwordHashed);
            return result;
         }
      }

      public static class addUserToGroup<I extends SnapUserManager.Iface> extends ProcessFunction<I, SnapUserManager.addUserToGroup_args> {
         public addUserToGroup() {
            super("addUserToGroup");
         }

         public SnapUserManager.addUserToGroup_args getEmptyArgsInstance() {
            return new SnapUserManager.addUserToGroup_args();
         }

         protected boolean isOneway() {
            return false;
         }

         protected boolean rethrowUnhandledExceptions() {
            return false;
         }

         public SnapUserManager.addUserToGroup_result getResult(I iface, SnapUserManager.addUserToGroup_args args) throws TException {
            SnapUserManager.addUserToGroup_result result = new SnapUserManager.addUserToGroup_result();
            result.success = iface.addUserToGroup(args.userId, args.groupId);
            result.setSuccessIsSet(true);
            return result;
         }
      }

      public static class changeUserPassword<I extends SnapUserManager.Iface> extends ProcessFunction<I, SnapUserManager.changeUserPassword_args> {
         public changeUserPassword() {
            super("changeUserPassword");
         }

         public SnapUserManager.changeUserPassword_args getEmptyArgsInstance() {
            return new SnapUserManager.changeUserPassword_args();
         }

         protected boolean isOneway() {
            return false;
         }

         protected boolean rethrowUnhandledExceptions() {
            return false;
         }

         public SnapUserManager.changeUserPassword_result getResult(I iface, SnapUserManager.changeUserPassword_args args) throws TException {
            SnapUserManager.changeUserPassword_result result = new SnapUserManager.changeUserPassword_result();
            result.success = iface.changeUserPassword(args.userId, args.oldPassword, args.newPassword);
            result.setSuccessIsSet(true);
            return result;
         }
      }

      public static class removeUserAccount<I extends SnapUserManager.Iface> extends ProcessFunction<I, SnapUserManager.removeUserAccount_args> {
         public removeUserAccount() {
            super("removeUserAccount");
         }

         public SnapUserManager.removeUserAccount_args getEmptyArgsInstance() {
            return new SnapUserManager.removeUserAccount_args();
         }

         protected boolean isOneway() {
            return false;
         }

         protected boolean rethrowUnhandledExceptions() {
            return false;
         }

         public SnapUserManager.removeUserAccount_result getResult(I iface, SnapUserManager.removeUserAccount_args args) throws TException {
            SnapUserManager.removeUserAccount_result result = new SnapUserManager.removeUserAccount_result();
            result.success = iface.removeUserAccount(args.userId);
            result.setSuccessIsSet(true);
            return result;
         }
      }

      public static class removeUserFromGroup<I extends SnapUserManager.Iface> extends ProcessFunction<I, SnapUserManager.removeUserFromGroup_args> {
         public removeUserFromGroup() {
            super("removeUserFromGroup");
         }

         public SnapUserManager.removeUserFromGroup_args getEmptyArgsInstance() {
            return new SnapUserManager.removeUserFromGroup_args();
         }

         protected boolean isOneway() {
            return false;
         }

         protected boolean rethrowUnhandledExceptions() {
            return false;
         }

         public SnapUserManager.removeUserFromGroup_result getResult(I iface, SnapUserManager.removeUserFromGroup_args args) throws TException {
            SnapUserManager.removeUserFromGroup_result result = new SnapUserManager.removeUserFromGroup_result();
            result.success = iface.removeUserFromGroup(args.userId, args.groupId);
            result.setSuccessIsSet(true);
            return result;
         }
      }
   }

   private static class ThriftClientHolder {
      public static final SnapUserManager.Client _THRIFT_CLIENT_INSTANCE = SnapUserManager.loadThriftClient();
   }

   public static class addUserAccount_args
      implements TBase<SnapUserManager.addUserAccount_args, SnapUserManager.addUserAccount_args._Fields>,
      Serializable,
      Cloneable,
      Comparable<SnapUserManager.addUserAccount_args> {
      private static final TStruct STRUCT_DESC = new TStruct("addUserAccount_args");
      private static final TField NAME_FIELD_DESC = new TField("name", (byte)11, (short)1);
      private static final TField PASSWORD_FIELD_DESC = new TField("password", (byte)11, (short)2);
      private static final TField COMMENT_FIELD_DESC = new TField("comment", (byte)11, (short)3);
      private static final TField PASSWORD_HASHED_FIELD_DESC = new TField("passwordHashed", (byte)2, (short)4);
      private static final SchemeFactory STANDARD_SCHEME_FACTORY = new SnapUserManager.addUserAccount_args.addUserAccount_argsStandardSchemeFactory();
      private static final SchemeFactory TUPLE_SCHEME_FACTORY = new SnapUserManager.addUserAccount_args.addUserAccount_argsTupleSchemeFactory();
      @Nullable
      public String name;
      @Nullable
      public String password;
      @Nullable
      public String comment;
      public boolean passwordHashed;
      private static final int __PASSWORDHASHED_ISSET_ID = 0;
      private byte __isset_bitfield = 0;
      public static final Map<SnapUserManager.addUserAccount_args._Fields, FieldMetaData> metaDataMap;

      public addUserAccount_args() {
      }

      public addUserAccount_args(String name, String password, String comment, boolean passwordHashed) {
         this();
         this.name = name;
         this.password = password;
         this.comment = comment;
         this.passwordHashed = passwordHashed;
         this.setPasswordHashedIsSet(true);
      }

      public addUserAccount_args(SnapUserManager.addUserAccount_args other) {
         this.__isset_bitfield = other.__isset_bitfield;
         if (other.isSetName()) {
            this.name = other.name;
         }

         if (other.isSetPassword()) {
            this.password = other.password;
         }

         if (other.isSetComment()) {
            this.comment = other.comment;
         }

         this.passwordHashed = other.passwordHashed;
      }

      public SnapUserManager.addUserAccount_args deepCopy() {
         return new SnapUserManager.addUserAccount_args(this);
      }

      public void clear() {
         this.name = null;
         this.password = null;
         this.comment = null;
         this.setPasswordHashedIsSet(false);
         this.passwordHashed = false;
      }

      @Nullable
      public String getName() {
         return this.name;
      }

      public SnapUserManager.addUserAccount_args setName(@Nullable String name) {
         this.name = name;
         return this;
      }

      public void unsetName() {
         this.name = null;
      }

      public boolean isSetName() {
         return this.name != null;
      }

      public void setNameIsSet(boolean value) {
         if (!value) {
            this.name = null;
         }
      }

      @Nullable
      public String getPassword() {
         return this.password;
      }

      public SnapUserManager.addUserAccount_args setPassword(@Nullable String password) {
         this.password = password;
         return this;
      }

      public void unsetPassword() {
         this.password = null;
      }

      public boolean isSetPassword() {
         return this.password != null;
      }

      public void setPasswordIsSet(boolean value) {
         if (!value) {
            this.password = null;
         }
      }

      @Nullable
      public String getComment() {
         return this.comment;
      }

      public SnapUserManager.addUserAccount_args setComment(@Nullable String comment) {
         this.comment = comment;
         return this;
      }

      public void unsetComment() {
         this.comment = null;
      }

      public boolean isSetComment() {
         return this.comment != null;
      }

      public void setCommentIsSet(boolean value) {
         if (!value) {
            this.comment = null;
         }
      }

      public boolean isPasswordHashed() {
         return this.passwordHashed;
      }

      public SnapUserManager.addUserAccount_args setPasswordHashed(boolean passwordHashed) {
         this.passwordHashed = passwordHashed;
         this.setPasswordHashedIsSet(true);
         return this;
      }

      public void unsetPasswordHashed() {
         this.__isset_bitfield = EncodingUtils.clearBit(this.__isset_bitfield, 0);
      }

      public boolean isSetPasswordHashed() {
         return EncodingUtils.testBit(this.__isset_bitfield, 0);
      }

      public void setPasswordHashedIsSet(boolean value) {
         this.__isset_bitfield = EncodingUtils.setBit(this.__isset_bitfield, 0, value);
      }

      public void setFieldValue(SnapUserManager.addUserAccount_args._Fields field, @Nullable Object value) {
         switch (field) {
            case NAME:
               if (value == null) {
                  this.unsetName();
               } else {
                  this.setName((String)value);
               }
               break;
            case PASSWORD:
               if (value == null) {
                  this.unsetPassword();
               } else {
                  this.setPassword((String)value);
               }
               break;
            case COMMENT:
               if (value == null) {
                  this.unsetComment();
               } else {
                  this.setComment((String)value);
               }
               break;
            case PASSWORD_HASHED:
               if (value == null) {
                  this.unsetPasswordHashed();
               } else {
                  this.setPasswordHashed((Boolean)value);
               }
         }
      }

      @Nullable
      public Object getFieldValue(SnapUserManager.addUserAccount_args._Fields field) {
         switch (field) {
            case NAME:
               return this.getName();
            case PASSWORD:
               return this.getPassword();
            case COMMENT:
               return this.getComment();
            case PASSWORD_HASHED:
               return this.isPasswordHashed();
            default:
               throw new IllegalStateException();
         }
      }

      public boolean isSet(SnapUserManager.addUserAccount_args._Fields field) {
         if (field == null) {
            throw new IllegalArgumentException();
         }

         switch (field) {
            case NAME:
               return this.isSetName();
            case PASSWORD:
               return this.isSetPassword();
            case COMMENT:
               return this.isSetComment();
            case PASSWORD_HASHED:
               return this.isSetPasswordHashed();
            default:
               throw new IllegalStateException();
         }
      }

      @Override
      public boolean equals(Object that) {
         return that instanceof SnapUserManager.addUserAccount_args ? this.equals((SnapUserManager.addUserAccount_args)that) : false;
      }

      public boolean equals(SnapUserManager.addUserAccount_args that) {
         if (that == null) {
            return false;
         }

         if (this == that) {
            return true;
         }

         boolean this_present_name = this.isSetName();
         boolean that_present_name = that.isSetName();
         if (this_present_name || that_present_name) {
            if (!this_present_name || !that_present_name) {
               return false;
            }

            if (!this.name.equals(that.name)) {
               return false;
            }
         }

         boolean this_present_password = this.isSetPassword();
         boolean that_present_password = that.isSetPassword();
         if (this_present_password || that_present_password) {
            if (!this_present_password || !that_present_password) {
               return false;
            }

            if (!this.password.equals(that.password)) {
               return false;
            }
         }

         boolean this_present_comment = this.isSetComment();
         boolean that_present_comment = that.isSetComment();
         if (this_present_comment || that_present_comment) {
            if (!this_present_comment || !that_present_comment) {
               return false;
            }

            if (!this.comment.equals(that.comment)) {
               return false;
            }
         }

         boolean this_present_passwordHashed = true;
         boolean that_present_passwordHashed = true;
         if (this_present_passwordHashed || that_present_passwordHashed) {
            if (!this_present_passwordHashed || !that_present_passwordHashed) {
               return false;
            }

            if (this.passwordHashed != that.passwordHashed) {
               return false;
            }
         }

         return true;
      }

      @Override
      public int hashCode() {
         int hashCode = 1;
         hashCode = hashCode * 8191 + (this.isSetName() ? 131071 : 524287);
         if (this.isSetName()) {
            hashCode = hashCode * 8191 + this.name.hashCode();
         }

         hashCode = hashCode * 8191 + (this.isSetPassword() ? 131071 : 524287);
         if (this.isSetPassword()) {
            hashCode = hashCode * 8191 + this.password.hashCode();
         }

         hashCode = hashCode * 8191 + (this.isSetComment() ? 131071 : 524287);
         if (this.isSetComment()) {
            hashCode = hashCode * 8191 + this.comment.hashCode();
         }

         return hashCode * 8191 + (this.passwordHashed ? 131071 : 524287);
      }

      public int compareTo(SnapUserManager.addUserAccount_args other) {
         if (!this.getClass().equals(other.getClass())) {
            return this.getClass().getName().compareTo(other.getClass().getName());
         }

         int lastComparison = 0;
         lastComparison = Boolean.compare(this.isSetName(), other.isSetName());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetName()) {
            lastComparison = TBaseHelper.compareTo(this.name, other.name);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         lastComparison = Boolean.compare(this.isSetPassword(), other.isSetPassword());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetPassword()) {
            lastComparison = TBaseHelper.compareTo(this.password, other.password);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         lastComparison = Boolean.compare(this.isSetComment(), other.isSetComment());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetComment()) {
            lastComparison = TBaseHelper.compareTo(this.comment, other.comment);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         lastComparison = Boolean.compare(this.isSetPasswordHashed(), other.isSetPasswordHashed());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetPasswordHashed()) {
            lastComparison = TBaseHelper.compareTo(this.passwordHashed, other.passwordHashed);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         return 0;
      }

      @Nullable
      public SnapUserManager.addUserAccount_args._Fields fieldForId(int fieldId) {
         return SnapUserManager.addUserAccount_args._Fields.findByThriftId(fieldId);
      }

      public void read(TProtocol iprot) throws TException {
         scheme(iprot).read(iprot, this);
      }

      public void write(TProtocol oprot) throws TException {
         scheme(oprot).write(oprot, this);
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder("addUserAccount_args(");
         boolean first = true;
         sb.append("name:");
         if (this.name == null) {
            sb.append("null");
         } else {
            sb.append(this.name);
         }

         first = false;
         if (!first) {
            sb.append(", ");
         }

         sb.append("password:");
         if (this.password == null) {
            sb.append("null");
         } else {
            sb.append(this.password);
         }

         first = false;
         if (!first) {
            sb.append(", ");
         }

         sb.append("comment:");
         if (this.comment == null) {
            sb.append("null");
         } else {
            sb.append(this.comment);
         }

         first = false;
         if (!first) {
            sb.append(", ");
         }

         sb.append("passwordHashed:");
         sb.append(this.passwordHashed);
         first = false;
         sb.append(")");
         return sb.toString();
      }

      public void validate() throws TException {
      }

      private void writeObject(ObjectOutputStream out) throws IOException {
         try {
            this.write(new TCompactProtocol(new TIOStreamTransport(out)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         try {
            this.__isset_bitfield = 0;
            this.read(new TCompactProtocol(new TIOStreamTransport(in)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private static <S extends IScheme> S scheme(TProtocol proto) {
         return (S)(StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
      }

      static {
         Map<SnapUserManager.addUserAccount_args._Fields, FieldMetaData> tmpMap = new EnumMap<>(SnapUserManager.addUserAccount_args._Fields.class);
         tmpMap.put(SnapUserManager.addUserAccount_args._Fields.NAME, new FieldMetaData("name", (byte)3, new FieldValueMetaData((byte)11)));
         tmpMap.put(SnapUserManager.addUserAccount_args._Fields.PASSWORD, new FieldMetaData("password", (byte)3, new FieldValueMetaData((byte)11)));
         tmpMap.put(SnapUserManager.addUserAccount_args._Fields.COMMENT, new FieldMetaData("comment", (byte)3, new FieldValueMetaData((byte)11)));
         tmpMap.put(SnapUserManager.addUserAccount_args._Fields.PASSWORD_HASHED, new FieldMetaData("passwordHashed", (byte)3, new FieldValueMetaData((byte)2)));
         metaDataMap = Collections.unmodifiableMap(tmpMap);
         FieldMetaData.addStructMetaDataMap(SnapUserManager.addUserAccount_args.class, metaDataMap);
      }

      public enum _Fields implements TFieldIdEnum {
         NAME((short)1, "name"),
         PASSWORD((short)2, "password"),
         COMMENT((short)3, "comment"),
         PASSWORD_HASHED((short)4, "passwordHashed");

         private static final Map<String, SnapUserManager.addUserAccount_args._Fields> byName = new HashMap<>();
         private final short _thriftId;
         private final String _fieldName;

         @Nullable
         public static SnapUserManager.addUserAccount_args._Fields findByThriftId(int fieldId) {
            switch (fieldId) {
               case 1:
                  return NAME;
               case 2:
                  return PASSWORD;
               case 3:
                  return COMMENT;
               case 4:
                  return PASSWORD_HASHED;
               default:
                  return null;
            }
         }

         public static SnapUserManager.addUserAccount_args._Fields findByThriftIdOrThrow(int fieldId) {
            SnapUserManager.addUserAccount_args._Fields fields = findByThriftId(fieldId);
            if (fields == null) {
               throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
            } else {
               return fields;
            }
         }

         @Nullable
         public static SnapUserManager.addUserAccount_args._Fields findByName(String name) {
            return byName.get(name);
         }

         _Fields(short thriftId, String fieldName) {
            this._thriftId = thriftId;
            this._fieldName = fieldName;
         }

         public short getThriftFieldId() {
            return this._thriftId;
         }

         public String getFieldName() {
            return this._fieldName;
         }

         static {
            for (SnapUserManager.addUserAccount_args._Fields field : EnumSet.allOf(SnapUserManager.addUserAccount_args._Fields.class)) {
               byName.put(field.getFieldName(), field);
            }
         }
      }

      private static class addUserAccount_argsStandardScheme extends StandardScheme<SnapUserManager.addUserAccount_args> {
         private addUserAccount_argsStandardScheme() {
         }

         public void read(TProtocol iprot, SnapUserManager.addUserAccount_args struct) throws TException {
            iprot.readStructBegin();

            while (true) {
               TField schemeField = iprot.readFieldBegin();
               if (schemeField.type == 0) {
                  iprot.readStructEnd();
                  struct.validate();
                  return;
               }

               switch (schemeField.id) {
                  case 1:
                     if (schemeField.type == 11) {
                        struct.name = iprot.readString();
                        struct.setNameIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  case 2:
                     if (schemeField.type == 11) {
                        struct.password = iprot.readString();
                        struct.setPasswordIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  case 3:
                     if (schemeField.type == 11) {
                        struct.comment = iprot.readString();
                        struct.setCommentIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  case 4:
                     if (schemeField.type == 2) {
                        struct.passwordHashed = iprot.readBool();
                        struct.setPasswordHashedIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  default:
                     TProtocolUtil.skip(iprot, schemeField.type);
               }

               iprot.readFieldEnd();
            }
         }

         public void write(TProtocol oprot, SnapUserManager.addUserAccount_args struct) throws TException {
            struct.validate();
            oprot.writeStructBegin(SnapUserManager.addUserAccount_args.STRUCT_DESC);
            if (struct.name != null) {
               oprot.writeFieldBegin(SnapUserManager.addUserAccount_args.NAME_FIELD_DESC);
               oprot.writeString(struct.name);
               oprot.writeFieldEnd();
            }

            if (struct.password != null) {
               oprot.writeFieldBegin(SnapUserManager.addUserAccount_args.PASSWORD_FIELD_DESC);
               oprot.writeString(struct.password);
               oprot.writeFieldEnd();
            }

            if (struct.comment != null) {
               oprot.writeFieldBegin(SnapUserManager.addUserAccount_args.COMMENT_FIELD_DESC);
               oprot.writeString(struct.comment);
               oprot.writeFieldEnd();
            }

            oprot.writeFieldBegin(SnapUserManager.addUserAccount_args.PASSWORD_HASHED_FIELD_DESC);
            oprot.writeBool(struct.passwordHashed);
            oprot.writeFieldEnd();
            oprot.writeFieldStop();
            oprot.writeStructEnd();
         }
      }

      private static class addUserAccount_argsStandardSchemeFactory implements SchemeFactory {
         private addUserAccount_argsStandardSchemeFactory() {
         }

         public SnapUserManager.addUserAccount_args.addUserAccount_argsStandardScheme getScheme() {
            return new SnapUserManager.addUserAccount_args.addUserAccount_argsStandardScheme();
         }
      }

      private static class addUserAccount_argsTupleScheme extends TupleScheme<SnapUserManager.addUserAccount_args> {
         private addUserAccount_argsTupleScheme() {
         }

         public void write(TProtocol prot, SnapUserManager.addUserAccount_args struct) throws TException {
            TTupleProtocol oprot = (TTupleProtocol)prot;
            BitSet optionals = new BitSet();
            if (struct.isSetName()) {
               optionals.set(0);
            }

            if (struct.isSetPassword()) {
               optionals.set(1);
            }

            if (struct.isSetComment()) {
               optionals.set(2);
            }

            if (struct.isSetPasswordHashed()) {
               optionals.set(3);
            }

            oprot.writeBitSet(optionals, 4);
            if (struct.isSetName()) {
               oprot.writeString(struct.name);
            }

            if (struct.isSetPassword()) {
               oprot.writeString(struct.password);
            }

            if (struct.isSetComment()) {
               oprot.writeString(struct.comment);
            }

            if (struct.isSetPasswordHashed()) {
               oprot.writeBool(struct.passwordHashed);
            }
         }

         public void read(TProtocol prot, SnapUserManager.addUserAccount_args struct) throws TException {
            TTupleProtocol iprot = (TTupleProtocol)prot;
            BitSet incoming = iprot.readBitSet(4);
            if (incoming.get(0)) {
               struct.name = iprot.readString();
               struct.setNameIsSet(true);
            }

            if (incoming.get(1)) {
               struct.password = iprot.readString();
               struct.setPasswordIsSet(true);
            }

            if (incoming.get(2)) {
               struct.comment = iprot.readString();
               struct.setCommentIsSet(true);
            }

            if (incoming.get(3)) {
               struct.passwordHashed = iprot.readBool();
               struct.setPasswordHashedIsSet(true);
            }
         }
      }

      private static class addUserAccount_argsTupleSchemeFactory implements SchemeFactory {
         private addUserAccount_argsTupleSchemeFactory() {
         }

         public SnapUserManager.addUserAccount_args.addUserAccount_argsTupleScheme getScheme() {
            return new SnapUserManager.addUserAccount_args.addUserAccount_argsTupleScheme();
         }
      }
   }

   public static class addUserAccount_result
      implements TBase<SnapUserManager.addUserAccount_result, SnapUserManager.addUserAccount_result._Fields>,
      Serializable,
      Cloneable,
      Comparable<SnapUserManager.addUserAccount_result> {
      private static final TStruct STRUCT_DESC = new TStruct("addUserAccount_result");
      private static final TField SUCCESS_FIELD_DESC = new TField("success", (byte)11, (short)0);
      private static final SchemeFactory STANDARD_SCHEME_FACTORY = new SnapUserManager.addUserAccount_result.addUserAccount_resultStandardSchemeFactory();
      private static final SchemeFactory TUPLE_SCHEME_FACTORY = new SnapUserManager.addUserAccount_result.addUserAccount_resultTupleSchemeFactory();
      @Nullable
      public String success;
      public static final Map<SnapUserManager.addUserAccount_result._Fields, FieldMetaData> metaDataMap;

      public addUserAccount_result() {
      }

      public addUserAccount_result(String success) {
         this();
         this.success = success;
      }

      public addUserAccount_result(SnapUserManager.addUserAccount_result other) {
         if (other.isSetSuccess()) {
            this.success = other.success;
         }
      }

      public SnapUserManager.addUserAccount_result deepCopy() {
         return new SnapUserManager.addUserAccount_result(this);
      }

      public void clear() {
         this.success = null;
      }

      @Nullable
      public String getSuccess() {
         return this.success;
      }

      public SnapUserManager.addUserAccount_result setSuccess(@Nullable String success) {
         this.success = success;
         return this;
      }

      public void unsetSuccess() {
         this.success = null;
      }

      public boolean isSetSuccess() {
         return this.success != null;
      }

      public void setSuccessIsSet(boolean value) {
         if (!value) {
            this.success = null;
         }
      }

      public void setFieldValue(SnapUserManager.addUserAccount_result._Fields field, @Nullable Object value) {
         switch (field) {
            case SUCCESS:
               if (value == null) {
                  this.unsetSuccess();
               } else {
                  this.setSuccess((String)value);
               }
         }
      }

      @Nullable
      public Object getFieldValue(SnapUserManager.addUserAccount_result._Fields field) {
         switch (field) {
            case SUCCESS:
               return this.getSuccess();
            default:
               throw new IllegalStateException();
         }
      }

      public boolean isSet(SnapUserManager.addUserAccount_result._Fields field) {
         if (field == null) {
            throw new IllegalArgumentException();
         }

         switch (field) {
            case SUCCESS:
               return this.isSetSuccess();
            default:
               throw new IllegalStateException();
         }
      }

      @Override
      public boolean equals(Object that) {
         return that instanceof SnapUserManager.addUserAccount_result ? this.equals((SnapUserManager.addUserAccount_result)that) : false;
      }

      public boolean equals(SnapUserManager.addUserAccount_result that) {
         if (that == null) {
            return false;
         }

         if (this == that) {
            return true;
         }

         boolean this_present_success = this.isSetSuccess();
         boolean that_present_success = that.isSetSuccess();
         if (this_present_success || that_present_success) {
            if (!this_present_success || !that_present_success) {
               return false;
            }

            if (!this.success.equals(that.success)) {
               return false;
            }
         }

         return true;
      }

      @Override
      public int hashCode() {
         int hashCode = 1;
         hashCode = hashCode * 8191 + (this.isSetSuccess() ? 131071 : 524287);
         if (this.isSetSuccess()) {
            hashCode = hashCode * 8191 + this.success.hashCode();
         }

         return hashCode;
      }

      public int compareTo(SnapUserManager.addUserAccount_result other) {
         if (!this.getClass().equals(other.getClass())) {
            return this.getClass().getName().compareTo(other.getClass().getName());
         }

         int lastComparison = 0;
         lastComparison = Boolean.compare(this.isSetSuccess(), other.isSetSuccess());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetSuccess()) {
            lastComparison = TBaseHelper.compareTo(this.success, other.success);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         return 0;
      }

      @Nullable
      public SnapUserManager.addUserAccount_result._Fields fieldForId(int fieldId) {
         return SnapUserManager.addUserAccount_result._Fields.findByThriftId(fieldId);
      }

      public void read(TProtocol iprot) throws TException {
         scheme(iprot).read(iprot, this);
      }

      public void write(TProtocol oprot) throws TException {
         scheme(oprot).write(oprot, this);
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder("addUserAccount_result(");
         boolean first = true;
         sb.append("success:");
         if (this.success == null) {
            sb.append("null");
         } else {
            sb.append(this.success);
         }

         first = false;
         sb.append(")");
         return sb.toString();
      }

      public void validate() throws TException {
      }

      private void writeObject(ObjectOutputStream out) throws IOException {
         try {
            this.write(new TCompactProtocol(new TIOStreamTransport(out)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         try {
            this.read(new TCompactProtocol(new TIOStreamTransport(in)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private static <S extends IScheme> S scheme(TProtocol proto) {
         return (S)(StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
      }

      static {
         Map<SnapUserManager.addUserAccount_result._Fields, FieldMetaData> tmpMap = new EnumMap<>(SnapUserManager.addUserAccount_result._Fields.class);
         tmpMap.put(SnapUserManager.addUserAccount_result._Fields.SUCCESS, new FieldMetaData("success", (byte)3, new FieldValueMetaData((byte)11)));
         metaDataMap = Collections.unmodifiableMap(tmpMap);
         FieldMetaData.addStructMetaDataMap(SnapUserManager.addUserAccount_result.class, metaDataMap);
      }

      public enum _Fields implements TFieldIdEnum {
         SUCCESS((short)0, "success");

         private static final Map<String, SnapUserManager.addUserAccount_result._Fields> byName = new HashMap<>();
         private final short _thriftId;
         private final String _fieldName;

         @Nullable
         public static SnapUserManager.addUserAccount_result._Fields findByThriftId(int fieldId) {
            switch (fieldId) {
               case 0:
                  return SUCCESS;
               default:
                  return null;
            }
         }

         public static SnapUserManager.addUserAccount_result._Fields findByThriftIdOrThrow(int fieldId) {
            SnapUserManager.addUserAccount_result._Fields fields = findByThriftId(fieldId);
            if (fields == null) {
               throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
            } else {
               return fields;
            }
         }

         @Nullable
         public static SnapUserManager.addUserAccount_result._Fields findByName(String name) {
            return byName.get(name);
         }

         _Fields(short thriftId, String fieldName) {
            this._thriftId = thriftId;
            this._fieldName = fieldName;
         }

         public short getThriftFieldId() {
            return this._thriftId;
         }

         public String getFieldName() {
            return this._fieldName;
         }

         static {
            for (SnapUserManager.addUserAccount_result._Fields field : EnumSet.allOf(SnapUserManager.addUserAccount_result._Fields.class)) {
               byName.put(field.getFieldName(), field);
            }
         }
      }

      private static class addUserAccount_resultStandardScheme extends StandardScheme<SnapUserManager.addUserAccount_result> {
         private addUserAccount_resultStandardScheme() {
         }

         public void read(TProtocol iprot, SnapUserManager.addUserAccount_result struct) throws TException {
            iprot.readStructBegin();

            while (true) {
               TField schemeField = iprot.readFieldBegin();
               if (schemeField.type == 0) {
                  iprot.readStructEnd();
                  struct.validate();
                  return;
               }

               switch (schemeField.id) {
                  case 0:
                     if (schemeField.type == 11) {
                        struct.success = iprot.readString();
                        struct.setSuccessIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  default:
                     TProtocolUtil.skip(iprot, schemeField.type);
               }

               iprot.readFieldEnd();
            }
         }

         public void write(TProtocol oprot, SnapUserManager.addUserAccount_result struct) throws TException {
            struct.validate();
            oprot.writeStructBegin(SnapUserManager.addUserAccount_result.STRUCT_DESC);
            if (struct.success != null) {
               oprot.writeFieldBegin(SnapUserManager.addUserAccount_result.SUCCESS_FIELD_DESC);
               oprot.writeString(struct.success);
               oprot.writeFieldEnd();
            }

            oprot.writeFieldStop();
            oprot.writeStructEnd();
         }
      }

      private static class addUserAccount_resultStandardSchemeFactory implements SchemeFactory {
         private addUserAccount_resultStandardSchemeFactory() {
         }

         public SnapUserManager.addUserAccount_result.addUserAccount_resultStandardScheme getScheme() {
            return new SnapUserManager.addUserAccount_result.addUserAccount_resultStandardScheme();
         }
      }

      private static class addUserAccount_resultTupleScheme extends TupleScheme<SnapUserManager.addUserAccount_result> {
         private addUserAccount_resultTupleScheme() {
         }

         public void write(TProtocol prot, SnapUserManager.addUserAccount_result struct) throws TException {
            TTupleProtocol oprot = (TTupleProtocol)prot;
            BitSet optionals = new BitSet();
            if (struct.isSetSuccess()) {
               optionals.set(0);
            }

            oprot.writeBitSet(optionals, 1);
            if (struct.isSetSuccess()) {
               oprot.writeString(struct.success);
            }
         }

         public void read(TProtocol prot, SnapUserManager.addUserAccount_result struct) throws TException {
            TTupleProtocol iprot = (TTupleProtocol)prot;
            BitSet incoming = iprot.readBitSet(1);
            if (incoming.get(0)) {
               struct.success = iprot.readString();
               struct.setSuccessIsSet(true);
            }
         }
      }

      private static class addUserAccount_resultTupleSchemeFactory implements SchemeFactory {
         private addUserAccount_resultTupleSchemeFactory() {
         }

         public SnapUserManager.addUserAccount_result.addUserAccount_resultTupleScheme getScheme() {
            return new SnapUserManager.addUserAccount_result.addUserAccount_resultTupleScheme();
         }
      }
   }

   public static class addUserToGroup_args
      implements TBase<SnapUserManager.addUserToGroup_args, SnapUserManager.addUserToGroup_args._Fields>,
      Serializable,
      Cloneable,
      Comparable<SnapUserManager.addUserToGroup_args> {
      private static final TStruct STRUCT_DESC = new TStruct("addUserToGroup_args");
      private static final TField USER_ID_FIELD_DESC = new TField("userId", (byte)11, (short)1);
      private static final TField GROUP_ID_FIELD_DESC = new TField("groupId", (byte)11, (short)2);
      private static final SchemeFactory STANDARD_SCHEME_FACTORY = new SnapUserManager.addUserToGroup_args.addUserToGroup_argsStandardSchemeFactory();
      private static final SchemeFactory TUPLE_SCHEME_FACTORY = new SnapUserManager.addUserToGroup_args.addUserToGroup_argsTupleSchemeFactory();
      @Nullable
      public String userId;
      @Nullable
      public String groupId;
      public static final Map<SnapUserManager.addUserToGroup_args._Fields, FieldMetaData> metaDataMap;

      public addUserToGroup_args() {
      }

      public addUserToGroup_args(String userId, String groupId) {
         this();
         this.userId = userId;
         this.groupId = groupId;
      }

      public addUserToGroup_args(SnapUserManager.addUserToGroup_args other) {
         if (other.isSetUserId()) {
            this.userId = other.userId;
         }

         if (other.isSetGroupId()) {
            this.groupId = other.groupId;
         }
      }

      public SnapUserManager.addUserToGroup_args deepCopy() {
         return new SnapUserManager.addUserToGroup_args(this);
      }

      public void clear() {
         this.userId = null;
         this.groupId = null;
      }

      @Nullable
      public String getUserId() {
         return this.userId;
      }

      public SnapUserManager.addUserToGroup_args setUserId(@Nullable String userId) {
         this.userId = userId;
         return this;
      }

      public void unsetUserId() {
         this.userId = null;
      }

      public boolean isSetUserId() {
         return this.userId != null;
      }

      public void setUserIdIsSet(boolean value) {
         if (!value) {
            this.userId = null;
         }
      }

      @Nullable
      public String getGroupId() {
         return this.groupId;
      }

      public SnapUserManager.addUserToGroup_args setGroupId(@Nullable String groupId) {
         this.groupId = groupId;
         return this;
      }

      public void unsetGroupId() {
         this.groupId = null;
      }

      public boolean isSetGroupId() {
         return this.groupId != null;
      }

      public void setGroupIdIsSet(boolean value) {
         if (!value) {
            this.groupId = null;
         }
      }

      public void setFieldValue(SnapUserManager.addUserToGroup_args._Fields field, @Nullable Object value) {
         switch (field) {
            case USER_ID:
               if (value == null) {
                  this.unsetUserId();
               } else {
                  this.setUserId((String)value);
               }
               break;
            case GROUP_ID:
               if (value == null) {
                  this.unsetGroupId();
               } else {
                  this.setGroupId((String)value);
               }
         }
      }

      @Nullable
      public Object getFieldValue(SnapUserManager.addUserToGroup_args._Fields field) {
         switch (field) {
            case USER_ID:
               return this.getUserId();
            case GROUP_ID:
               return this.getGroupId();
            default:
               throw new IllegalStateException();
         }
      }

      public boolean isSet(SnapUserManager.addUserToGroup_args._Fields field) {
         if (field == null) {
            throw new IllegalArgumentException();
         }

         switch (field) {
            case USER_ID:
               return this.isSetUserId();
            case GROUP_ID:
               return this.isSetGroupId();
            default:
               throw new IllegalStateException();
         }
      }

      @Override
      public boolean equals(Object that) {
         return that instanceof SnapUserManager.addUserToGroup_args ? this.equals((SnapUserManager.addUserToGroup_args)that) : false;
      }

      public boolean equals(SnapUserManager.addUserToGroup_args that) {
         if (that == null) {
            return false;
         }

         if (this == that) {
            return true;
         }

         boolean this_present_userId = this.isSetUserId();
         boolean that_present_userId = that.isSetUserId();
         if (this_present_userId || that_present_userId) {
            if (!this_present_userId || !that_present_userId) {
               return false;
            }

            if (!this.userId.equals(that.userId)) {
               return false;
            }
         }

         boolean this_present_groupId = this.isSetGroupId();
         boolean that_present_groupId = that.isSetGroupId();
         if (this_present_groupId || that_present_groupId) {
            if (!this_present_groupId || !that_present_groupId) {
               return false;
            }

            if (!this.groupId.equals(that.groupId)) {
               return false;
            }
         }

         return true;
      }

      @Override
      public int hashCode() {
         int hashCode = 1;
         hashCode = hashCode * 8191 + (this.isSetUserId() ? 131071 : 524287);
         if (this.isSetUserId()) {
            hashCode = hashCode * 8191 + this.userId.hashCode();
         }

         hashCode = hashCode * 8191 + (this.isSetGroupId() ? 131071 : 524287);
         if (this.isSetGroupId()) {
            hashCode = hashCode * 8191 + this.groupId.hashCode();
         }

         return hashCode;
      }

      public int compareTo(SnapUserManager.addUserToGroup_args other) {
         if (!this.getClass().equals(other.getClass())) {
            return this.getClass().getName().compareTo(other.getClass().getName());
         }

         int lastComparison = 0;
         lastComparison = Boolean.compare(this.isSetUserId(), other.isSetUserId());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetUserId()) {
            lastComparison = TBaseHelper.compareTo(this.userId, other.userId);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         lastComparison = Boolean.compare(this.isSetGroupId(), other.isSetGroupId());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetGroupId()) {
            lastComparison = TBaseHelper.compareTo(this.groupId, other.groupId);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         return 0;
      }

      @Nullable
      public SnapUserManager.addUserToGroup_args._Fields fieldForId(int fieldId) {
         return SnapUserManager.addUserToGroup_args._Fields.findByThriftId(fieldId);
      }

      public void read(TProtocol iprot) throws TException {
         scheme(iprot).read(iprot, this);
      }

      public void write(TProtocol oprot) throws TException {
         scheme(oprot).write(oprot, this);
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder("addUserToGroup_args(");
         boolean first = true;
         sb.append("userId:");
         if (this.userId == null) {
            sb.append("null");
         } else {
            sb.append(this.userId);
         }

         first = false;
         if (!first) {
            sb.append(", ");
         }

         sb.append("groupId:");
         if (this.groupId == null) {
            sb.append("null");
         } else {
            sb.append(this.groupId);
         }

         first = false;
         sb.append(")");
         return sb.toString();
      }

      public void validate() throws TException {
      }

      private void writeObject(ObjectOutputStream out) throws IOException {
         try {
            this.write(new TCompactProtocol(new TIOStreamTransport(out)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         try {
            this.read(new TCompactProtocol(new TIOStreamTransport(in)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private static <S extends IScheme> S scheme(TProtocol proto) {
         return (S)(StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
      }

      static {
         Map<SnapUserManager.addUserToGroup_args._Fields, FieldMetaData> tmpMap = new EnumMap<>(SnapUserManager.addUserToGroup_args._Fields.class);
         tmpMap.put(SnapUserManager.addUserToGroup_args._Fields.USER_ID, new FieldMetaData("userId", (byte)3, new FieldValueMetaData((byte)11)));
         tmpMap.put(SnapUserManager.addUserToGroup_args._Fields.GROUP_ID, new FieldMetaData("groupId", (byte)3, new FieldValueMetaData((byte)11)));
         metaDataMap = Collections.unmodifiableMap(tmpMap);
         FieldMetaData.addStructMetaDataMap(SnapUserManager.addUserToGroup_args.class, metaDataMap);
      }

      public enum _Fields implements TFieldIdEnum {
         USER_ID((short)1, "userId"),
         GROUP_ID((short)2, "groupId");

         private static final Map<String, SnapUserManager.addUserToGroup_args._Fields> byName = new HashMap<>();
         private final short _thriftId;
         private final String _fieldName;

         @Nullable
         public static SnapUserManager.addUserToGroup_args._Fields findByThriftId(int fieldId) {
            switch (fieldId) {
               case 1:
                  return USER_ID;
               case 2:
                  return GROUP_ID;
               default:
                  return null;
            }
         }

         public static SnapUserManager.addUserToGroup_args._Fields findByThriftIdOrThrow(int fieldId) {
            SnapUserManager.addUserToGroup_args._Fields fields = findByThriftId(fieldId);
            if (fields == null) {
               throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
            } else {
               return fields;
            }
         }

         @Nullable
         public static SnapUserManager.addUserToGroup_args._Fields findByName(String name) {
            return byName.get(name);
         }

         _Fields(short thriftId, String fieldName) {
            this._thriftId = thriftId;
            this._fieldName = fieldName;
         }

         public short getThriftFieldId() {
            return this._thriftId;
         }

         public String getFieldName() {
            return this._fieldName;
         }

         static {
            for (SnapUserManager.addUserToGroup_args._Fields field : EnumSet.allOf(SnapUserManager.addUserToGroup_args._Fields.class)) {
               byName.put(field.getFieldName(), field);
            }
         }
      }

      private static class addUserToGroup_argsStandardScheme extends StandardScheme<SnapUserManager.addUserToGroup_args> {
         private addUserToGroup_argsStandardScheme() {
         }

         public void read(TProtocol iprot, SnapUserManager.addUserToGroup_args struct) throws TException {
            iprot.readStructBegin();

            while (true) {
               TField schemeField = iprot.readFieldBegin();
               if (schemeField.type == 0) {
                  iprot.readStructEnd();
                  struct.validate();
                  return;
               }

               switch (schemeField.id) {
                  case 1:
                     if (schemeField.type == 11) {
                        struct.userId = iprot.readString();
                        struct.setUserIdIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  case 2:
                     if (schemeField.type == 11) {
                        struct.groupId = iprot.readString();
                        struct.setGroupIdIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  default:
                     TProtocolUtil.skip(iprot, schemeField.type);
               }

               iprot.readFieldEnd();
            }
         }

         public void write(TProtocol oprot, SnapUserManager.addUserToGroup_args struct) throws TException {
            struct.validate();
            oprot.writeStructBegin(SnapUserManager.addUserToGroup_args.STRUCT_DESC);
            if (struct.userId != null) {
               oprot.writeFieldBegin(SnapUserManager.addUserToGroup_args.USER_ID_FIELD_DESC);
               oprot.writeString(struct.userId);
               oprot.writeFieldEnd();
            }

            if (struct.groupId != null) {
               oprot.writeFieldBegin(SnapUserManager.addUserToGroup_args.GROUP_ID_FIELD_DESC);
               oprot.writeString(struct.groupId);
               oprot.writeFieldEnd();
            }

            oprot.writeFieldStop();
            oprot.writeStructEnd();
         }
      }

      private static class addUserToGroup_argsStandardSchemeFactory implements SchemeFactory {
         private addUserToGroup_argsStandardSchemeFactory() {
         }

         public SnapUserManager.addUserToGroup_args.addUserToGroup_argsStandardScheme getScheme() {
            return new SnapUserManager.addUserToGroup_args.addUserToGroup_argsStandardScheme();
         }
      }

      private static class addUserToGroup_argsTupleScheme extends TupleScheme<SnapUserManager.addUserToGroup_args> {
         private addUserToGroup_argsTupleScheme() {
         }

         public void write(TProtocol prot, SnapUserManager.addUserToGroup_args struct) throws TException {
            TTupleProtocol oprot = (TTupleProtocol)prot;
            BitSet optionals = new BitSet();
            if (struct.isSetUserId()) {
               optionals.set(0);
            }

            if (struct.isSetGroupId()) {
               optionals.set(1);
            }

            oprot.writeBitSet(optionals, 2);
            if (struct.isSetUserId()) {
               oprot.writeString(struct.userId);
            }

            if (struct.isSetGroupId()) {
               oprot.writeString(struct.groupId);
            }
         }

         public void read(TProtocol prot, SnapUserManager.addUserToGroup_args struct) throws TException {
            TTupleProtocol iprot = (TTupleProtocol)prot;
            BitSet incoming = iprot.readBitSet(2);
            if (incoming.get(0)) {
               struct.userId = iprot.readString();
               struct.setUserIdIsSet(true);
            }

            if (incoming.get(1)) {
               struct.groupId = iprot.readString();
               struct.setGroupIdIsSet(true);
            }
         }
      }

      private static class addUserToGroup_argsTupleSchemeFactory implements SchemeFactory {
         private addUserToGroup_argsTupleSchemeFactory() {
         }

         public SnapUserManager.addUserToGroup_args.addUserToGroup_argsTupleScheme getScheme() {
            return new SnapUserManager.addUserToGroup_args.addUserToGroup_argsTupleScheme();
         }
      }
   }

   public static class addUserToGroup_result
      implements TBase<SnapUserManager.addUserToGroup_result, SnapUserManager.addUserToGroup_result._Fields>,
      Serializable,
      Cloneable,
      Comparable<SnapUserManager.addUserToGroup_result> {
      private static final TStruct STRUCT_DESC = new TStruct("addUserToGroup_result");
      private static final TField SUCCESS_FIELD_DESC = new TField("success", (byte)2, (short)0);
      private static final SchemeFactory STANDARD_SCHEME_FACTORY = new SnapUserManager.addUserToGroup_result.addUserToGroup_resultStandardSchemeFactory();
      private static final SchemeFactory TUPLE_SCHEME_FACTORY = new SnapUserManager.addUserToGroup_result.addUserToGroup_resultTupleSchemeFactory();
      public boolean success;
      private static final int __SUCCESS_ISSET_ID = 0;
      private byte __isset_bitfield = 0;
      public static final Map<SnapUserManager.addUserToGroup_result._Fields, FieldMetaData> metaDataMap;

      public addUserToGroup_result() {
      }

      public addUserToGroup_result(boolean success) {
         this();
         this.success = success;
         this.setSuccessIsSet(true);
      }

      public addUserToGroup_result(SnapUserManager.addUserToGroup_result other) {
         this.__isset_bitfield = other.__isset_bitfield;
         this.success = other.success;
      }

      public SnapUserManager.addUserToGroup_result deepCopy() {
         return new SnapUserManager.addUserToGroup_result(this);
      }

      public void clear() {
         this.setSuccessIsSet(false);
         this.success = false;
      }

      public boolean isSuccess() {
         return this.success;
      }

      public SnapUserManager.addUserToGroup_result setSuccess(boolean success) {
         this.success = success;
         this.setSuccessIsSet(true);
         return this;
      }

      public void unsetSuccess() {
         this.__isset_bitfield = EncodingUtils.clearBit(this.__isset_bitfield, 0);
      }

      public boolean isSetSuccess() {
         return EncodingUtils.testBit(this.__isset_bitfield, 0);
      }

      public void setSuccessIsSet(boolean value) {
         this.__isset_bitfield = EncodingUtils.setBit(this.__isset_bitfield, 0, value);
      }

      public void setFieldValue(SnapUserManager.addUserToGroup_result._Fields field, @Nullable Object value) {
         switch (field) {
            case SUCCESS:
               if (value == null) {
                  this.unsetSuccess();
               } else {
                  this.setSuccess((Boolean)value);
               }
         }
      }

      @Nullable
      public Object getFieldValue(SnapUserManager.addUserToGroup_result._Fields field) {
         switch (field) {
            case SUCCESS:
               return this.isSuccess();
            default:
               throw new IllegalStateException();
         }
      }

      public boolean isSet(SnapUserManager.addUserToGroup_result._Fields field) {
         if (field == null) {
            throw new IllegalArgumentException();
         }

         switch (field) {
            case SUCCESS:
               return this.isSetSuccess();
            default:
               throw new IllegalStateException();
         }
      }

      @Override
      public boolean equals(Object that) {
         return that instanceof SnapUserManager.addUserToGroup_result ? this.equals((SnapUserManager.addUserToGroup_result)that) : false;
      }

      public boolean equals(SnapUserManager.addUserToGroup_result that) {
         if (that == null) {
            return false;
         }

         if (this == that) {
            return true;
         }

         boolean this_present_success = true;
         boolean that_present_success = true;
         if (this_present_success || that_present_success) {
            if (!this_present_success || !that_present_success) {
               return false;
            }

            if (this.success != that.success) {
               return false;
            }
         }

         return true;
      }

      @Override
      public int hashCode() {
         int hashCode = 1;
         return hashCode * 8191 + (this.success ? 131071 : 524287);
      }

      public int compareTo(SnapUserManager.addUserToGroup_result other) {
         if (!this.getClass().equals(other.getClass())) {
            return this.getClass().getName().compareTo(other.getClass().getName());
         }

         int lastComparison = 0;
         lastComparison = Boolean.compare(this.isSetSuccess(), other.isSetSuccess());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetSuccess()) {
            lastComparison = TBaseHelper.compareTo(this.success, other.success);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         return 0;
      }

      @Nullable
      public SnapUserManager.addUserToGroup_result._Fields fieldForId(int fieldId) {
         return SnapUserManager.addUserToGroup_result._Fields.findByThriftId(fieldId);
      }

      public void read(TProtocol iprot) throws TException {
         scheme(iprot).read(iprot, this);
      }

      public void write(TProtocol oprot) throws TException {
         scheme(oprot).write(oprot, this);
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder("addUserToGroup_result(");
         boolean first = true;
         sb.append("success:");
         sb.append(this.success);
         first = false;
         sb.append(")");
         return sb.toString();
      }

      public void validate() throws TException {
      }

      private void writeObject(ObjectOutputStream out) throws IOException {
         try {
            this.write(new TCompactProtocol(new TIOStreamTransport(out)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         try {
            this.__isset_bitfield = 0;
            this.read(new TCompactProtocol(new TIOStreamTransport(in)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private static <S extends IScheme> S scheme(TProtocol proto) {
         return (S)(StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
      }

      static {
         Map<SnapUserManager.addUserToGroup_result._Fields, FieldMetaData> tmpMap = new EnumMap<>(SnapUserManager.addUserToGroup_result._Fields.class);
         tmpMap.put(SnapUserManager.addUserToGroup_result._Fields.SUCCESS, new FieldMetaData("success", (byte)3, new FieldValueMetaData((byte)2)));
         metaDataMap = Collections.unmodifiableMap(tmpMap);
         FieldMetaData.addStructMetaDataMap(SnapUserManager.addUserToGroup_result.class, metaDataMap);
      }

      public enum _Fields implements TFieldIdEnum {
         SUCCESS((short)0, "success");

         private static final Map<String, SnapUserManager.addUserToGroup_result._Fields> byName = new HashMap<>();
         private final short _thriftId;
         private final String _fieldName;

         @Nullable
         public static SnapUserManager.addUserToGroup_result._Fields findByThriftId(int fieldId) {
            switch (fieldId) {
               case 0:
                  return SUCCESS;
               default:
                  return null;
            }
         }

         public static SnapUserManager.addUserToGroup_result._Fields findByThriftIdOrThrow(int fieldId) {
            SnapUserManager.addUserToGroup_result._Fields fields = findByThriftId(fieldId);
            if (fields == null) {
               throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
            } else {
               return fields;
            }
         }

         @Nullable
         public static SnapUserManager.addUserToGroup_result._Fields findByName(String name) {
            return byName.get(name);
         }

         _Fields(short thriftId, String fieldName) {
            this._thriftId = thriftId;
            this._fieldName = fieldName;
         }

         public short getThriftFieldId() {
            return this._thriftId;
         }

         public String getFieldName() {
            return this._fieldName;
         }

         static {
            for (SnapUserManager.addUserToGroup_result._Fields field : EnumSet.allOf(SnapUserManager.addUserToGroup_result._Fields.class)) {
               byName.put(field.getFieldName(), field);
            }
         }
      }

      private static class addUserToGroup_resultStandardScheme extends StandardScheme<SnapUserManager.addUserToGroup_result> {
         private addUserToGroup_resultStandardScheme() {
         }

         public void read(TProtocol iprot, SnapUserManager.addUserToGroup_result struct) throws TException {
            iprot.readStructBegin();

            while (true) {
               TField schemeField = iprot.readFieldBegin();
               if (schemeField.type == 0) {
                  iprot.readStructEnd();
                  struct.validate();
                  return;
               }

               switch (schemeField.id) {
                  case 0:
                     if (schemeField.type == 2) {
                        struct.success = iprot.readBool();
                        struct.setSuccessIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  default:
                     TProtocolUtil.skip(iprot, schemeField.type);
               }

               iprot.readFieldEnd();
            }
         }

         public void write(TProtocol oprot, SnapUserManager.addUserToGroup_result struct) throws TException {
            struct.validate();
            oprot.writeStructBegin(SnapUserManager.addUserToGroup_result.STRUCT_DESC);
            if (struct.isSetSuccess()) {
               oprot.writeFieldBegin(SnapUserManager.addUserToGroup_result.SUCCESS_FIELD_DESC);
               oprot.writeBool(struct.success);
               oprot.writeFieldEnd();
            }

            oprot.writeFieldStop();
            oprot.writeStructEnd();
         }
      }

      private static class addUserToGroup_resultStandardSchemeFactory implements SchemeFactory {
         private addUserToGroup_resultStandardSchemeFactory() {
         }

         public SnapUserManager.addUserToGroup_result.addUserToGroup_resultStandardScheme getScheme() {
            return new SnapUserManager.addUserToGroup_result.addUserToGroup_resultStandardScheme();
         }
      }

      private static class addUserToGroup_resultTupleScheme extends TupleScheme<SnapUserManager.addUserToGroup_result> {
         private addUserToGroup_resultTupleScheme() {
         }

         public void write(TProtocol prot, SnapUserManager.addUserToGroup_result struct) throws TException {
            TTupleProtocol oprot = (TTupleProtocol)prot;
            BitSet optionals = new BitSet();
            if (struct.isSetSuccess()) {
               optionals.set(0);
            }

            oprot.writeBitSet(optionals, 1);
            if (struct.isSetSuccess()) {
               oprot.writeBool(struct.success);
            }
         }

         public void read(TProtocol prot, SnapUserManager.addUserToGroup_result struct) throws TException {
            TTupleProtocol iprot = (TTupleProtocol)prot;
            BitSet incoming = iprot.readBitSet(1);
            if (incoming.get(0)) {
               struct.success = iprot.readBool();
               struct.setSuccessIsSet(true);
            }
         }
      }

      private static class addUserToGroup_resultTupleSchemeFactory implements SchemeFactory {
         private addUserToGroup_resultTupleSchemeFactory() {
         }

         public SnapUserManager.addUserToGroup_result.addUserToGroup_resultTupleScheme getScheme() {
            return new SnapUserManager.addUserToGroup_result.addUserToGroup_resultTupleScheme();
         }
      }
   }

   public static class changeUserPassword_args
      implements TBase<SnapUserManager.changeUserPassword_args, SnapUserManager.changeUserPassword_args._Fields>,
      Serializable,
      Cloneable,
      Comparable<SnapUserManager.changeUserPassword_args> {
      private static final TStruct STRUCT_DESC = new TStruct("changeUserPassword_args");
      private static final TField USER_ID_FIELD_DESC = new TField("userId", (byte)11, (short)1);
      private static final TField OLD_PASSWORD_FIELD_DESC = new TField("oldPassword", (byte)11, (short)2);
      private static final TField NEW_PASSWORD_FIELD_DESC = new TField("newPassword", (byte)11, (short)3);
      private static final SchemeFactory STANDARD_SCHEME_FACTORY = new SnapUserManager.changeUserPassword_args.changeUserPassword_argsStandardSchemeFactory();
      private static final SchemeFactory TUPLE_SCHEME_FACTORY = new SnapUserManager.changeUserPassword_args.changeUserPassword_argsTupleSchemeFactory();
      @Nullable
      public String userId;
      @Nullable
      public String oldPassword;
      @Nullable
      public String newPassword;
      public static final Map<SnapUserManager.changeUserPassword_args._Fields, FieldMetaData> metaDataMap;

      public changeUserPassword_args() {
      }

      public changeUserPassword_args(String userId, String oldPassword, String newPassword) {
         this();
         this.userId = userId;
         this.oldPassword = oldPassword;
         this.newPassword = newPassword;
      }

      public changeUserPassword_args(SnapUserManager.changeUserPassword_args other) {
         if (other.isSetUserId()) {
            this.userId = other.userId;
         }

         if (other.isSetOldPassword()) {
            this.oldPassword = other.oldPassword;
         }

         if (other.isSetNewPassword()) {
            this.newPassword = other.newPassword;
         }
      }

      public SnapUserManager.changeUserPassword_args deepCopy() {
         return new SnapUserManager.changeUserPassword_args(this);
      }

      public void clear() {
         this.userId = null;
         this.oldPassword = null;
         this.newPassword = null;
      }

      @Nullable
      public String getUserId() {
         return this.userId;
      }

      public SnapUserManager.changeUserPassword_args setUserId(@Nullable String userId) {
         this.userId = userId;
         return this;
      }

      public void unsetUserId() {
         this.userId = null;
      }

      public boolean isSetUserId() {
         return this.userId != null;
      }

      public void setUserIdIsSet(boolean value) {
         if (!value) {
            this.userId = null;
         }
      }

      @Nullable
      public String getOldPassword() {
         return this.oldPassword;
      }

      public SnapUserManager.changeUserPassword_args setOldPassword(@Nullable String oldPassword) {
         this.oldPassword = oldPassword;
         return this;
      }

      public void unsetOldPassword() {
         this.oldPassword = null;
      }

      public boolean isSetOldPassword() {
         return this.oldPassword != null;
      }

      public void setOldPasswordIsSet(boolean value) {
         if (!value) {
            this.oldPassword = null;
         }
      }

      @Nullable
      public String getNewPassword() {
         return this.newPassword;
      }

      public SnapUserManager.changeUserPassword_args setNewPassword(@Nullable String newPassword) {
         this.newPassword = newPassword;
         return this;
      }

      public void unsetNewPassword() {
         this.newPassword = null;
      }

      public boolean isSetNewPassword() {
         return this.newPassword != null;
      }

      public void setNewPasswordIsSet(boolean value) {
         if (!value) {
            this.newPassword = null;
         }
      }

      public void setFieldValue(SnapUserManager.changeUserPassword_args._Fields field, @Nullable Object value) {
         switch (field) {
            case USER_ID:
               if (value == null) {
                  this.unsetUserId();
               } else {
                  this.setUserId((String)value);
               }
               break;
            case OLD_PASSWORD:
               if (value == null) {
                  this.unsetOldPassword();
               } else {
                  this.setOldPassword((String)value);
               }
               break;
            case NEW_PASSWORD:
               if (value == null) {
                  this.unsetNewPassword();
               } else {
                  this.setNewPassword((String)value);
               }
         }
      }

      @Nullable
      public Object getFieldValue(SnapUserManager.changeUserPassword_args._Fields field) {
         switch (field) {
            case USER_ID:
               return this.getUserId();
            case OLD_PASSWORD:
               return this.getOldPassword();
            case NEW_PASSWORD:
               return this.getNewPassword();
            default:
               throw new IllegalStateException();
         }
      }

      public boolean isSet(SnapUserManager.changeUserPassword_args._Fields field) {
         if (field == null) {
            throw new IllegalArgumentException();
         }

         switch (field) {
            case USER_ID:
               return this.isSetUserId();
            case OLD_PASSWORD:
               return this.isSetOldPassword();
            case NEW_PASSWORD:
               return this.isSetNewPassword();
            default:
               throw new IllegalStateException();
         }
      }

      @Override
      public boolean equals(Object that) {
         return that instanceof SnapUserManager.changeUserPassword_args ? this.equals((SnapUserManager.changeUserPassword_args)that) : false;
      }

      public boolean equals(SnapUserManager.changeUserPassword_args that) {
         if (that == null) {
            return false;
         }

         if (this == that) {
            return true;
         }

         boolean this_present_userId = this.isSetUserId();
         boolean that_present_userId = that.isSetUserId();
         if (this_present_userId || that_present_userId) {
            if (!this_present_userId || !that_present_userId) {
               return false;
            }

            if (!this.userId.equals(that.userId)) {
               return false;
            }
         }

         boolean this_present_oldPassword = this.isSetOldPassword();
         boolean that_present_oldPassword = that.isSetOldPassword();
         if (this_present_oldPassword || that_present_oldPassword) {
            if (!this_present_oldPassword || !that_present_oldPassword) {
               return false;
            }

            if (!this.oldPassword.equals(that.oldPassword)) {
               return false;
            }
         }

         boolean this_present_newPassword = this.isSetNewPassword();
         boolean that_present_newPassword = that.isSetNewPassword();
         if (this_present_newPassword || that_present_newPassword) {
            if (!this_present_newPassword || !that_present_newPassword) {
               return false;
            }

            if (!this.newPassword.equals(that.newPassword)) {
               return false;
            }
         }

         return true;
      }

      @Override
      public int hashCode() {
         int hashCode = 1;
         hashCode = hashCode * 8191 + (this.isSetUserId() ? 131071 : 524287);
         if (this.isSetUserId()) {
            hashCode = hashCode * 8191 + this.userId.hashCode();
         }

         hashCode = hashCode * 8191 + (this.isSetOldPassword() ? 131071 : 524287);
         if (this.isSetOldPassword()) {
            hashCode = hashCode * 8191 + this.oldPassword.hashCode();
         }

         hashCode = hashCode * 8191 + (this.isSetNewPassword() ? 131071 : 524287);
         if (this.isSetNewPassword()) {
            hashCode = hashCode * 8191 + this.newPassword.hashCode();
         }

         return hashCode;
      }

      public int compareTo(SnapUserManager.changeUserPassword_args other) {
         if (!this.getClass().equals(other.getClass())) {
            return this.getClass().getName().compareTo(other.getClass().getName());
         }

         int lastComparison = 0;
         lastComparison = Boolean.compare(this.isSetUserId(), other.isSetUserId());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetUserId()) {
            lastComparison = TBaseHelper.compareTo(this.userId, other.userId);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         lastComparison = Boolean.compare(this.isSetOldPassword(), other.isSetOldPassword());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetOldPassword()) {
            lastComparison = TBaseHelper.compareTo(this.oldPassword, other.oldPassword);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         lastComparison = Boolean.compare(this.isSetNewPassword(), other.isSetNewPassword());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetNewPassword()) {
            lastComparison = TBaseHelper.compareTo(this.newPassword, other.newPassword);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         return 0;
      }

      @Nullable
      public SnapUserManager.changeUserPassword_args._Fields fieldForId(int fieldId) {
         return SnapUserManager.changeUserPassword_args._Fields.findByThriftId(fieldId);
      }

      public void read(TProtocol iprot) throws TException {
         scheme(iprot).read(iprot, this);
      }

      public void write(TProtocol oprot) throws TException {
         scheme(oprot).write(oprot, this);
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder("changeUserPassword_args(");
         boolean first = true;
         sb.append("userId:");
         if (this.userId == null) {
            sb.append("null");
         } else {
            sb.append(this.userId);
         }

         first = false;
         if (!first) {
            sb.append(", ");
         }

         sb.append("oldPassword:");
         if (this.oldPassword == null) {
            sb.append("null");
         } else {
            sb.append(this.oldPassword);
         }

         first = false;
         if (!first) {
            sb.append(", ");
         }

         sb.append("newPassword:");
         if (this.newPassword == null) {
            sb.append("null");
         } else {
            sb.append(this.newPassword);
         }

         first = false;
         sb.append(")");
         return sb.toString();
      }

      public void validate() throws TException {
      }

      private void writeObject(ObjectOutputStream out) throws IOException {
         try {
            this.write(new TCompactProtocol(new TIOStreamTransport(out)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         try {
            this.read(new TCompactProtocol(new TIOStreamTransport(in)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private static <S extends IScheme> S scheme(TProtocol proto) {
         return (S)(StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
      }

      static {
         Map<SnapUserManager.changeUserPassword_args._Fields, FieldMetaData> tmpMap = new EnumMap<>(SnapUserManager.changeUserPassword_args._Fields.class);
         tmpMap.put(SnapUserManager.changeUserPassword_args._Fields.USER_ID, new FieldMetaData("userId", (byte)3, new FieldValueMetaData((byte)11)));
         tmpMap.put(SnapUserManager.changeUserPassword_args._Fields.OLD_PASSWORD, new FieldMetaData("oldPassword", (byte)3, new FieldValueMetaData((byte)11)));
         tmpMap.put(SnapUserManager.changeUserPassword_args._Fields.NEW_PASSWORD, new FieldMetaData("newPassword", (byte)3, new FieldValueMetaData((byte)11)));
         metaDataMap = Collections.unmodifiableMap(tmpMap);
         FieldMetaData.addStructMetaDataMap(SnapUserManager.changeUserPassword_args.class, metaDataMap);
      }

      public enum _Fields implements TFieldIdEnum {
         USER_ID((short)1, "userId"),
         OLD_PASSWORD((short)2, "oldPassword"),
         NEW_PASSWORD((short)3, "newPassword");

         private static final Map<String, SnapUserManager.changeUserPassword_args._Fields> byName = new HashMap<>();
         private final short _thriftId;
         private final String _fieldName;

         @Nullable
         public static SnapUserManager.changeUserPassword_args._Fields findByThriftId(int fieldId) {
            switch (fieldId) {
               case 1:
                  return USER_ID;
               case 2:
                  return OLD_PASSWORD;
               case 3:
                  return NEW_PASSWORD;
               default:
                  return null;
            }
         }

         public static SnapUserManager.changeUserPassword_args._Fields findByThriftIdOrThrow(int fieldId) {
            SnapUserManager.changeUserPassword_args._Fields fields = findByThriftId(fieldId);
            if (fields == null) {
               throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
            } else {
               return fields;
            }
         }

         @Nullable
         public static SnapUserManager.changeUserPassword_args._Fields findByName(String name) {
            return byName.get(name);
         }

         _Fields(short thriftId, String fieldName) {
            this._thriftId = thriftId;
            this._fieldName = fieldName;
         }

         public short getThriftFieldId() {
            return this._thriftId;
         }

         public String getFieldName() {
            return this._fieldName;
         }

         static {
            for (SnapUserManager.changeUserPassword_args._Fields field : EnumSet.allOf(SnapUserManager.changeUserPassword_args._Fields.class)) {
               byName.put(field.getFieldName(), field);
            }
         }
      }

      private static class changeUserPassword_argsStandardScheme extends StandardScheme<SnapUserManager.changeUserPassword_args> {
         private changeUserPassword_argsStandardScheme() {
         }

         public void read(TProtocol iprot, SnapUserManager.changeUserPassword_args struct) throws TException {
            iprot.readStructBegin();

            while (true) {
               TField schemeField = iprot.readFieldBegin();
               if (schemeField.type == 0) {
                  iprot.readStructEnd();
                  struct.validate();
                  return;
               }

               switch (schemeField.id) {
                  case 1:
                     if (schemeField.type == 11) {
                        struct.userId = iprot.readString();
                        struct.setUserIdIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  case 2:
                     if (schemeField.type == 11) {
                        struct.oldPassword = iprot.readString();
                        struct.setOldPasswordIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  case 3:
                     if (schemeField.type == 11) {
                        struct.newPassword = iprot.readString();
                        struct.setNewPasswordIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  default:
                     TProtocolUtil.skip(iprot, schemeField.type);
               }

               iprot.readFieldEnd();
            }
         }

         public void write(TProtocol oprot, SnapUserManager.changeUserPassword_args struct) throws TException {
            struct.validate();
            oprot.writeStructBegin(SnapUserManager.changeUserPassword_args.STRUCT_DESC);
            if (struct.userId != null) {
               oprot.writeFieldBegin(SnapUserManager.changeUserPassword_args.USER_ID_FIELD_DESC);
               oprot.writeString(struct.userId);
               oprot.writeFieldEnd();
            }

            if (struct.oldPassword != null) {
               oprot.writeFieldBegin(SnapUserManager.changeUserPassword_args.OLD_PASSWORD_FIELD_DESC);
               oprot.writeString(struct.oldPassword);
               oprot.writeFieldEnd();
            }

            if (struct.newPassword != null) {
               oprot.writeFieldBegin(SnapUserManager.changeUserPassword_args.NEW_PASSWORD_FIELD_DESC);
               oprot.writeString(struct.newPassword);
               oprot.writeFieldEnd();
            }

            oprot.writeFieldStop();
            oprot.writeStructEnd();
         }
      }

      private static class changeUserPassword_argsStandardSchemeFactory implements SchemeFactory {
         private changeUserPassword_argsStandardSchemeFactory() {
         }

         public SnapUserManager.changeUserPassword_args.changeUserPassword_argsStandardScheme getScheme() {
            return new SnapUserManager.changeUserPassword_args.changeUserPassword_argsStandardScheme();
         }
      }

      private static class changeUserPassword_argsTupleScheme extends TupleScheme<SnapUserManager.changeUserPassword_args> {
         private changeUserPassword_argsTupleScheme() {
         }

         public void write(TProtocol prot, SnapUserManager.changeUserPassword_args struct) throws TException {
            TTupleProtocol oprot = (TTupleProtocol)prot;
            BitSet optionals = new BitSet();
            if (struct.isSetUserId()) {
               optionals.set(0);
            }

            if (struct.isSetOldPassword()) {
               optionals.set(1);
            }

            if (struct.isSetNewPassword()) {
               optionals.set(2);
            }

            oprot.writeBitSet(optionals, 3);
            if (struct.isSetUserId()) {
               oprot.writeString(struct.userId);
            }

            if (struct.isSetOldPassword()) {
               oprot.writeString(struct.oldPassword);
            }

            if (struct.isSetNewPassword()) {
               oprot.writeString(struct.newPassword);
            }
         }

         public void read(TProtocol prot, SnapUserManager.changeUserPassword_args struct) throws TException {
            TTupleProtocol iprot = (TTupleProtocol)prot;
            BitSet incoming = iprot.readBitSet(3);
            if (incoming.get(0)) {
               struct.userId = iprot.readString();
               struct.setUserIdIsSet(true);
            }

            if (incoming.get(1)) {
               struct.oldPassword = iprot.readString();
               struct.setOldPasswordIsSet(true);
            }

            if (incoming.get(2)) {
               struct.newPassword = iprot.readString();
               struct.setNewPasswordIsSet(true);
            }
         }
      }

      private static class changeUserPassword_argsTupleSchemeFactory implements SchemeFactory {
         private changeUserPassword_argsTupleSchemeFactory() {
         }

         public SnapUserManager.changeUserPassword_args.changeUserPassword_argsTupleScheme getScheme() {
            return new SnapUserManager.changeUserPassword_args.changeUserPassword_argsTupleScheme();
         }
      }
   }

   public static class changeUserPassword_result
      implements TBase<SnapUserManager.changeUserPassword_result, SnapUserManager.changeUserPassword_result._Fields>,
      Serializable,
      Cloneable,
      Comparable<SnapUserManager.changeUserPassword_result> {
      private static final TStruct STRUCT_DESC = new TStruct("changeUserPassword_result");
      private static final TField SUCCESS_FIELD_DESC = new TField("success", (byte)2, (short)0);
      private static final SchemeFactory STANDARD_SCHEME_FACTORY = new SnapUserManager.changeUserPassword_result.changeUserPassword_resultStandardSchemeFactory(
         
      );
      private static final SchemeFactory TUPLE_SCHEME_FACTORY = new SnapUserManager.changeUserPassword_result.changeUserPassword_resultTupleSchemeFactory();
      public boolean success;
      private static final int __SUCCESS_ISSET_ID = 0;
      private byte __isset_bitfield = 0;
      public static final Map<SnapUserManager.changeUserPassword_result._Fields, FieldMetaData> metaDataMap;

      public changeUserPassword_result() {
      }

      public changeUserPassword_result(boolean success) {
         this();
         this.success = success;
         this.setSuccessIsSet(true);
      }

      public changeUserPassword_result(SnapUserManager.changeUserPassword_result other) {
         this.__isset_bitfield = other.__isset_bitfield;
         this.success = other.success;
      }

      public SnapUserManager.changeUserPassword_result deepCopy() {
         return new SnapUserManager.changeUserPassword_result(this);
      }

      public void clear() {
         this.setSuccessIsSet(false);
         this.success = false;
      }

      public boolean isSuccess() {
         return this.success;
      }

      public SnapUserManager.changeUserPassword_result setSuccess(boolean success) {
         this.success = success;
         this.setSuccessIsSet(true);
         return this;
      }

      public void unsetSuccess() {
         this.__isset_bitfield = EncodingUtils.clearBit(this.__isset_bitfield, 0);
      }

      public boolean isSetSuccess() {
         return EncodingUtils.testBit(this.__isset_bitfield, 0);
      }

      public void setSuccessIsSet(boolean value) {
         this.__isset_bitfield = EncodingUtils.setBit(this.__isset_bitfield, 0, value);
      }

      public void setFieldValue(SnapUserManager.changeUserPassword_result._Fields field, @Nullable Object value) {
         switch (field) {
            case SUCCESS:
               if (value == null) {
                  this.unsetSuccess();
               } else {
                  this.setSuccess((Boolean)value);
               }
         }
      }

      @Nullable
      public Object getFieldValue(SnapUserManager.changeUserPassword_result._Fields field) {
         switch (field) {
            case SUCCESS:
               return this.isSuccess();
            default:
               throw new IllegalStateException();
         }
      }

      public boolean isSet(SnapUserManager.changeUserPassword_result._Fields field) {
         if (field == null) {
            throw new IllegalArgumentException();
         }

         switch (field) {
            case SUCCESS:
               return this.isSetSuccess();
            default:
               throw new IllegalStateException();
         }
      }

      @Override
      public boolean equals(Object that) {
         return that instanceof SnapUserManager.changeUserPassword_result ? this.equals((SnapUserManager.changeUserPassword_result)that) : false;
      }

      public boolean equals(SnapUserManager.changeUserPassword_result that) {
         if (that == null) {
            return false;
         }

         if (this == that) {
            return true;
         }

         boolean this_present_success = true;
         boolean that_present_success = true;
         if (this_present_success || that_present_success) {
            if (!this_present_success || !that_present_success) {
               return false;
            }

            if (this.success != that.success) {
               return false;
            }
         }

         return true;
      }

      @Override
      public int hashCode() {
         int hashCode = 1;
         return hashCode * 8191 + (this.success ? 131071 : 524287);
      }

      public int compareTo(SnapUserManager.changeUserPassword_result other) {
         if (!this.getClass().equals(other.getClass())) {
            return this.getClass().getName().compareTo(other.getClass().getName());
         }

         int lastComparison = 0;
         lastComparison = Boolean.compare(this.isSetSuccess(), other.isSetSuccess());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetSuccess()) {
            lastComparison = TBaseHelper.compareTo(this.success, other.success);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         return 0;
      }

      @Nullable
      public SnapUserManager.changeUserPassword_result._Fields fieldForId(int fieldId) {
         return SnapUserManager.changeUserPassword_result._Fields.findByThriftId(fieldId);
      }

      public void read(TProtocol iprot) throws TException {
         scheme(iprot).read(iprot, this);
      }

      public void write(TProtocol oprot) throws TException {
         scheme(oprot).write(oprot, this);
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder("changeUserPassword_result(");
         boolean first = true;
         sb.append("success:");
         sb.append(this.success);
         first = false;
         sb.append(")");
         return sb.toString();
      }

      public void validate() throws TException {
      }

      private void writeObject(ObjectOutputStream out) throws IOException {
         try {
            this.write(new TCompactProtocol(new TIOStreamTransport(out)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         try {
            this.__isset_bitfield = 0;
            this.read(new TCompactProtocol(new TIOStreamTransport(in)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private static <S extends IScheme> S scheme(TProtocol proto) {
         return (S)(StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
      }

      static {
         Map<SnapUserManager.changeUserPassword_result._Fields, FieldMetaData> tmpMap = new EnumMap<>(SnapUserManager.changeUserPassword_result._Fields.class);
         tmpMap.put(SnapUserManager.changeUserPassword_result._Fields.SUCCESS, new FieldMetaData("success", (byte)3, new FieldValueMetaData((byte)2)));
         metaDataMap = Collections.unmodifiableMap(tmpMap);
         FieldMetaData.addStructMetaDataMap(SnapUserManager.changeUserPassword_result.class, metaDataMap);
      }

      public enum _Fields implements TFieldIdEnum {
         SUCCESS((short)0, "success");

         private static final Map<String, SnapUserManager.changeUserPassword_result._Fields> byName = new HashMap<>();
         private final short _thriftId;
         private final String _fieldName;

         @Nullable
         public static SnapUserManager.changeUserPassword_result._Fields findByThriftId(int fieldId) {
            switch (fieldId) {
               case 0:
                  return SUCCESS;
               default:
                  return null;
            }
         }

         public static SnapUserManager.changeUserPassword_result._Fields findByThriftIdOrThrow(int fieldId) {
            SnapUserManager.changeUserPassword_result._Fields fields = findByThriftId(fieldId);
            if (fields == null) {
               throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
            } else {
               return fields;
            }
         }

         @Nullable
         public static SnapUserManager.changeUserPassword_result._Fields findByName(String name) {
            return byName.get(name);
         }

         _Fields(short thriftId, String fieldName) {
            this._thriftId = thriftId;
            this._fieldName = fieldName;
         }

         public short getThriftFieldId() {
            return this._thriftId;
         }

         public String getFieldName() {
            return this._fieldName;
         }

         static {
            for (SnapUserManager.changeUserPassword_result._Fields field : EnumSet.allOf(SnapUserManager.changeUserPassword_result._Fields.class)) {
               byName.put(field.getFieldName(), field);
            }
         }
      }

      private static class changeUserPassword_resultStandardScheme extends StandardScheme<SnapUserManager.changeUserPassword_result> {
         private changeUserPassword_resultStandardScheme() {
         }

         public void read(TProtocol iprot, SnapUserManager.changeUserPassword_result struct) throws TException {
            iprot.readStructBegin();

            while (true) {
               TField schemeField = iprot.readFieldBegin();
               if (schemeField.type == 0) {
                  iprot.readStructEnd();
                  struct.validate();
                  return;
               }

               switch (schemeField.id) {
                  case 0:
                     if (schemeField.type == 2) {
                        struct.success = iprot.readBool();
                        struct.setSuccessIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  default:
                     TProtocolUtil.skip(iprot, schemeField.type);
               }

               iprot.readFieldEnd();
            }
         }

         public void write(TProtocol oprot, SnapUserManager.changeUserPassword_result struct) throws TException {
            struct.validate();
            oprot.writeStructBegin(SnapUserManager.changeUserPassword_result.STRUCT_DESC);
            if (struct.isSetSuccess()) {
               oprot.writeFieldBegin(SnapUserManager.changeUserPassword_result.SUCCESS_FIELD_DESC);
               oprot.writeBool(struct.success);
               oprot.writeFieldEnd();
            }

            oprot.writeFieldStop();
            oprot.writeStructEnd();
         }
      }

      private static class changeUserPassword_resultStandardSchemeFactory implements SchemeFactory {
         private changeUserPassword_resultStandardSchemeFactory() {
         }

         public SnapUserManager.changeUserPassword_result.changeUserPassword_resultStandardScheme getScheme() {
            return new SnapUserManager.changeUserPassword_result.changeUserPassword_resultStandardScheme();
         }
      }

      private static class changeUserPassword_resultTupleScheme extends TupleScheme<SnapUserManager.changeUserPassword_result> {
         private changeUserPassword_resultTupleScheme() {
         }

         public void write(TProtocol prot, SnapUserManager.changeUserPassword_result struct) throws TException {
            TTupleProtocol oprot = (TTupleProtocol)prot;
            BitSet optionals = new BitSet();
            if (struct.isSetSuccess()) {
               optionals.set(0);
            }

            oprot.writeBitSet(optionals, 1);
            if (struct.isSetSuccess()) {
               oprot.writeBool(struct.success);
            }
         }

         public void read(TProtocol prot, SnapUserManager.changeUserPassword_result struct) throws TException {
            TTupleProtocol iprot = (TTupleProtocol)prot;
            BitSet incoming = iprot.readBitSet(1);
            if (incoming.get(0)) {
               struct.success = iprot.readBool();
               struct.setSuccessIsSet(true);
            }
         }
      }

      private static class changeUserPassword_resultTupleSchemeFactory implements SchemeFactory {
         private changeUserPassword_resultTupleSchemeFactory() {
         }

         public SnapUserManager.changeUserPassword_result.changeUserPassword_resultTupleScheme getScheme() {
            return new SnapUserManager.changeUserPassword_result.changeUserPassword_resultTupleScheme();
         }
      }
   }

   public static class removeUserAccount_args
      implements TBase<SnapUserManager.removeUserAccount_args, SnapUserManager.removeUserAccount_args._Fields>,
      Serializable,
      Cloneable,
      Comparable<SnapUserManager.removeUserAccount_args> {
      private static final TStruct STRUCT_DESC = new TStruct("removeUserAccount_args");
      private static final TField USER_ID_FIELD_DESC = new TField("userId", (byte)11, (short)1);
      private static final SchemeFactory STANDARD_SCHEME_FACTORY = new SnapUserManager.removeUserAccount_args.removeUserAccount_argsStandardSchemeFactory();
      private static final SchemeFactory TUPLE_SCHEME_FACTORY = new SnapUserManager.removeUserAccount_args.removeUserAccount_argsTupleSchemeFactory();
      @Nullable
      public String userId;
      public static final Map<SnapUserManager.removeUserAccount_args._Fields, FieldMetaData> metaDataMap;

      public removeUserAccount_args() {
      }

      public removeUserAccount_args(String userId) {
         this();
         this.userId = userId;
      }

      public removeUserAccount_args(SnapUserManager.removeUserAccount_args other) {
         if (other.isSetUserId()) {
            this.userId = other.userId;
         }
      }

      public SnapUserManager.removeUserAccount_args deepCopy() {
         return new SnapUserManager.removeUserAccount_args(this);
      }

      public void clear() {
         this.userId = null;
      }

      @Nullable
      public String getUserId() {
         return this.userId;
      }

      public SnapUserManager.removeUserAccount_args setUserId(@Nullable String userId) {
         this.userId = userId;
         return this;
      }

      public void unsetUserId() {
         this.userId = null;
      }

      public boolean isSetUserId() {
         return this.userId != null;
      }

      public void setUserIdIsSet(boolean value) {
         if (!value) {
            this.userId = null;
         }
      }

      public void setFieldValue(SnapUserManager.removeUserAccount_args._Fields field, @Nullable Object value) {
         switch (field) {
            case USER_ID:
               if (value == null) {
                  this.unsetUserId();
               } else {
                  this.setUserId((String)value);
               }
         }
      }

      @Nullable
      public Object getFieldValue(SnapUserManager.removeUserAccount_args._Fields field) {
         switch (field) {
            case USER_ID:
               return this.getUserId();
            default:
               throw new IllegalStateException();
         }
      }

      public boolean isSet(SnapUserManager.removeUserAccount_args._Fields field) {
         if (field == null) {
            throw new IllegalArgumentException();
         }

         switch (field) {
            case USER_ID:
               return this.isSetUserId();
            default:
               throw new IllegalStateException();
         }
      }

      @Override
      public boolean equals(Object that) {
         return that instanceof SnapUserManager.removeUserAccount_args ? this.equals((SnapUserManager.removeUserAccount_args)that) : false;
      }

      public boolean equals(SnapUserManager.removeUserAccount_args that) {
         if (that == null) {
            return false;
         }

         if (this == that) {
            return true;
         }

         boolean this_present_userId = this.isSetUserId();
         boolean that_present_userId = that.isSetUserId();
         if (this_present_userId || that_present_userId) {
            if (!this_present_userId || !that_present_userId) {
               return false;
            }

            if (!this.userId.equals(that.userId)) {
               return false;
            }
         }

         return true;
      }

      @Override
      public int hashCode() {
         int hashCode = 1;
         hashCode = hashCode * 8191 + (this.isSetUserId() ? 131071 : 524287);
         if (this.isSetUserId()) {
            hashCode = hashCode * 8191 + this.userId.hashCode();
         }

         return hashCode;
      }

      public int compareTo(SnapUserManager.removeUserAccount_args other) {
         if (!this.getClass().equals(other.getClass())) {
            return this.getClass().getName().compareTo(other.getClass().getName());
         }

         int lastComparison = 0;
         lastComparison = Boolean.compare(this.isSetUserId(), other.isSetUserId());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetUserId()) {
            lastComparison = TBaseHelper.compareTo(this.userId, other.userId);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         return 0;
      }

      @Nullable
      public SnapUserManager.removeUserAccount_args._Fields fieldForId(int fieldId) {
         return SnapUserManager.removeUserAccount_args._Fields.findByThriftId(fieldId);
      }

      public void read(TProtocol iprot) throws TException {
         scheme(iprot).read(iprot, this);
      }

      public void write(TProtocol oprot) throws TException {
         scheme(oprot).write(oprot, this);
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder("removeUserAccount_args(");
         boolean first = true;
         sb.append("userId:");
         if (this.userId == null) {
            sb.append("null");
         } else {
            sb.append(this.userId);
         }

         first = false;
         sb.append(")");
         return sb.toString();
      }

      public void validate() throws TException {
      }

      private void writeObject(ObjectOutputStream out) throws IOException {
         try {
            this.write(new TCompactProtocol(new TIOStreamTransport(out)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         try {
            this.read(new TCompactProtocol(new TIOStreamTransport(in)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private static <S extends IScheme> S scheme(TProtocol proto) {
         return (S)(StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
      }

      static {
         Map<SnapUserManager.removeUserAccount_args._Fields, FieldMetaData> tmpMap = new EnumMap<>(SnapUserManager.removeUserAccount_args._Fields.class);
         tmpMap.put(SnapUserManager.removeUserAccount_args._Fields.USER_ID, new FieldMetaData("userId", (byte)3, new FieldValueMetaData((byte)11)));
         metaDataMap = Collections.unmodifiableMap(tmpMap);
         FieldMetaData.addStructMetaDataMap(SnapUserManager.removeUserAccount_args.class, metaDataMap);
      }

      public enum _Fields implements TFieldIdEnum {
         USER_ID((short)1, "userId");

         private static final Map<String, SnapUserManager.removeUserAccount_args._Fields> byName = new HashMap<>();
         private final short _thriftId;
         private final String _fieldName;

         @Nullable
         public static SnapUserManager.removeUserAccount_args._Fields findByThriftId(int fieldId) {
            switch (fieldId) {
               case 1:
                  return USER_ID;
               default:
                  return null;
            }
         }

         public static SnapUserManager.removeUserAccount_args._Fields findByThriftIdOrThrow(int fieldId) {
            SnapUserManager.removeUserAccount_args._Fields fields = findByThriftId(fieldId);
            if (fields == null) {
               throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
            } else {
               return fields;
            }
         }

         @Nullable
         public static SnapUserManager.removeUserAccount_args._Fields findByName(String name) {
            return byName.get(name);
         }

         _Fields(short thriftId, String fieldName) {
            this._thriftId = thriftId;
            this._fieldName = fieldName;
         }

         public short getThriftFieldId() {
            return this._thriftId;
         }

         public String getFieldName() {
            return this._fieldName;
         }

         static {
            for (SnapUserManager.removeUserAccount_args._Fields field : EnumSet.allOf(SnapUserManager.removeUserAccount_args._Fields.class)) {
               byName.put(field.getFieldName(), field);
            }
         }
      }

      private static class removeUserAccount_argsStandardScheme extends StandardScheme<SnapUserManager.removeUserAccount_args> {
         private removeUserAccount_argsStandardScheme() {
         }

         public void read(TProtocol iprot, SnapUserManager.removeUserAccount_args struct) throws TException {
            iprot.readStructBegin();

            while (true) {
               TField schemeField = iprot.readFieldBegin();
               if (schemeField.type == 0) {
                  iprot.readStructEnd();
                  struct.validate();
                  return;
               }

               switch (schemeField.id) {
                  case 1:
                     if (schemeField.type == 11) {
                        struct.userId = iprot.readString();
                        struct.setUserIdIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  default:
                     TProtocolUtil.skip(iprot, schemeField.type);
               }

               iprot.readFieldEnd();
            }
         }

         public void write(TProtocol oprot, SnapUserManager.removeUserAccount_args struct) throws TException {
            struct.validate();
            oprot.writeStructBegin(SnapUserManager.removeUserAccount_args.STRUCT_DESC);
            if (struct.userId != null) {
               oprot.writeFieldBegin(SnapUserManager.removeUserAccount_args.USER_ID_FIELD_DESC);
               oprot.writeString(struct.userId);
               oprot.writeFieldEnd();
            }

            oprot.writeFieldStop();
            oprot.writeStructEnd();
         }
      }

      private static class removeUserAccount_argsStandardSchemeFactory implements SchemeFactory {
         private removeUserAccount_argsStandardSchemeFactory() {
         }

         public SnapUserManager.removeUserAccount_args.removeUserAccount_argsStandardScheme getScheme() {
            return new SnapUserManager.removeUserAccount_args.removeUserAccount_argsStandardScheme();
         }
      }

      private static class removeUserAccount_argsTupleScheme extends TupleScheme<SnapUserManager.removeUserAccount_args> {
         private removeUserAccount_argsTupleScheme() {
         }

         public void write(TProtocol prot, SnapUserManager.removeUserAccount_args struct) throws TException {
            TTupleProtocol oprot = (TTupleProtocol)prot;
            BitSet optionals = new BitSet();
            if (struct.isSetUserId()) {
               optionals.set(0);
            }

            oprot.writeBitSet(optionals, 1);
            if (struct.isSetUserId()) {
               oprot.writeString(struct.userId);
            }
         }

         public void read(TProtocol prot, SnapUserManager.removeUserAccount_args struct) throws TException {
            TTupleProtocol iprot = (TTupleProtocol)prot;
            BitSet incoming = iprot.readBitSet(1);
            if (incoming.get(0)) {
               struct.userId = iprot.readString();
               struct.setUserIdIsSet(true);
            }
         }
      }

      private static class removeUserAccount_argsTupleSchemeFactory implements SchemeFactory {
         private removeUserAccount_argsTupleSchemeFactory() {
         }

         public SnapUserManager.removeUserAccount_args.removeUserAccount_argsTupleScheme getScheme() {
            return new SnapUserManager.removeUserAccount_args.removeUserAccount_argsTupleScheme();
         }
      }
   }

   public static class removeUserAccount_result
      implements TBase<SnapUserManager.removeUserAccount_result, SnapUserManager.removeUserAccount_result._Fields>,
      Serializable,
      Cloneable,
      Comparable<SnapUserManager.removeUserAccount_result> {
      private static final TStruct STRUCT_DESC = new TStruct("removeUserAccount_result");
      private static final TField SUCCESS_FIELD_DESC = new TField("success", (byte)2, (short)0);
      private static final SchemeFactory STANDARD_SCHEME_FACTORY = new SnapUserManager.removeUserAccount_result.removeUserAccount_resultStandardSchemeFactory();
      private static final SchemeFactory TUPLE_SCHEME_FACTORY = new SnapUserManager.removeUserAccount_result.removeUserAccount_resultTupleSchemeFactory();
      public boolean success;
      private static final int __SUCCESS_ISSET_ID = 0;
      private byte __isset_bitfield = 0;
      public static final Map<SnapUserManager.removeUserAccount_result._Fields, FieldMetaData> metaDataMap;

      public removeUserAccount_result() {
      }

      public removeUserAccount_result(boolean success) {
         this();
         this.success = success;
         this.setSuccessIsSet(true);
      }

      public removeUserAccount_result(SnapUserManager.removeUserAccount_result other) {
         this.__isset_bitfield = other.__isset_bitfield;
         this.success = other.success;
      }

      public SnapUserManager.removeUserAccount_result deepCopy() {
         return new SnapUserManager.removeUserAccount_result(this);
      }

      public void clear() {
         this.setSuccessIsSet(false);
         this.success = false;
      }

      public boolean isSuccess() {
         return this.success;
      }

      public SnapUserManager.removeUserAccount_result setSuccess(boolean success) {
         this.success = success;
         this.setSuccessIsSet(true);
         return this;
      }

      public void unsetSuccess() {
         this.__isset_bitfield = EncodingUtils.clearBit(this.__isset_bitfield, 0);
      }

      public boolean isSetSuccess() {
         return EncodingUtils.testBit(this.__isset_bitfield, 0);
      }

      public void setSuccessIsSet(boolean value) {
         this.__isset_bitfield = EncodingUtils.setBit(this.__isset_bitfield, 0, value);
      }

      public void setFieldValue(SnapUserManager.removeUserAccount_result._Fields field, @Nullable Object value) {
         switch (field) {
            case SUCCESS:
               if (value == null) {
                  this.unsetSuccess();
               } else {
                  this.setSuccess((Boolean)value);
               }
         }
      }

      @Nullable
      public Object getFieldValue(SnapUserManager.removeUserAccount_result._Fields field) {
         switch (field) {
            case SUCCESS:
               return this.isSuccess();
            default:
               throw new IllegalStateException();
         }
      }

      public boolean isSet(SnapUserManager.removeUserAccount_result._Fields field) {
         if (field == null) {
            throw new IllegalArgumentException();
         }

         switch (field) {
            case SUCCESS:
               return this.isSetSuccess();
            default:
               throw new IllegalStateException();
         }
      }

      @Override
      public boolean equals(Object that) {
         return that instanceof SnapUserManager.removeUserAccount_result ? this.equals((SnapUserManager.removeUserAccount_result)that) : false;
      }

      public boolean equals(SnapUserManager.removeUserAccount_result that) {
         if (that == null) {
            return false;
         }

         if (this == that) {
            return true;
         }

         boolean this_present_success = true;
         boolean that_present_success = true;
         if (this_present_success || that_present_success) {
            if (!this_present_success || !that_present_success) {
               return false;
            }

            if (this.success != that.success) {
               return false;
            }
         }

         return true;
      }

      @Override
      public int hashCode() {
         int hashCode = 1;
         return hashCode * 8191 + (this.success ? 131071 : 524287);
      }

      public int compareTo(SnapUserManager.removeUserAccount_result other) {
         if (!this.getClass().equals(other.getClass())) {
            return this.getClass().getName().compareTo(other.getClass().getName());
         }

         int lastComparison = 0;
         lastComparison = Boolean.compare(this.isSetSuccess(), other.isSetSuccess());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetSuccess()) {
            lastComparison = TBaseHelper.compareTo(this.success, other.success);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         return 0;
      }

      @Nullable
      public SnapUserManager.removeUserAccount_result._Fields fieldForId(int fieldId) {
         return SnapUserManager.removeUserAccount_result._Fields.findByThriftId(fieldId);
      }

      public void read(TProtocol iprot) throws TException {
         scheme(iprot).read(iprot, this);
      }

      public void write(TProtocol oprot) throws TException {
         scheme(oprot).write(oprot, this);
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder("removeUserAccount_result(");
         boolean first = true;
         sb.append("success:");
         sb.append(this.success);
         first = false;
         sb.append(")");
         return sb.toString();
      }

      public void validate() throws TException {
      }

      private void writeObject(ObjectOutputStream out) throws IOException {
         try {
            this.write(new TCompactProtocol(new TIOStreamTransport(out)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         try {
            this.__isset_bitfield = 0;
            this.read(new TCompactProtocol(new TIOStreamTransport(in)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private static <S extends IScheme> S scheme(TProtocol proto) {
         return (S)(StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
      }

      static {
         Map<SnapUserManager.removeUserAccount_result._Fields, FieldMetaData> tmpMap = new EnumMap<>(SnapUserManager.removeUserAccount_result._Fields.class);
         tmpMap.put(SnapUserManager.removeUserAccount_result._Fields.SUCCESS, new FieldMetaData("success", (byte)3, new FieldValueMetaData((byte)2)));
         metaDataMap = Collections.unmodifiableMap(tmpMap);
         FieldMetaData.addStructMetaDataMap(SnapUserManager.removeUserAccount_result.class, metaDataMap);
      }

      public enum _Fields implements TFieldIdEnum {
         SUCCESS((short)0, "success");

         private static final Map<String, SnapUserManager.removeUserAccount_result._Fields> byName = new HashMap<>();
         private final short _thriftId;
         private final String _fieldName;

         @Nullable
         public static SnapUserManager.removeUserAccount_result._Fields findByThriftId(int fieldId) {
            switch (fieldId) {
               case 0:
                  return SUCCESS;
               default:
                  return null;
            }
         }

         public static SnapUserManager.removeUserAccount_result._Fields findByThriftIdOrThrow(int fieldId) {
            SnapUserManager.removeUserAccount_result._Fields fields = findByThriftId(fieldId);
            if (fields == null) {
               throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
            } else {
               return fields;
            }
         }

         @Nullable
         public static SnapUserManager.removeUserAccount_result._Fields findByName(String name) {
            return byName.get(name);
         }

         _Fields(short thriftId, String fieldName) {
            this._thriftId = thriftId;
            this._fieldName = fieldName;
         }

         public short getThriftFieldId() {
            return this._thriftId;
         }

         public String getFieldName() {
            return this._fieldName;
         }

         static {
            for (SnapUserManager.removeUserAccount_result._Fields field : EnumSet.allOf(SnapUserManager.removeUserAccount_result._Fields.class)) {
               byName.put(field.getFieldName(), field);
            }
         }
      }

      private static class removeUserAccount_resultStandardScheme extends StandardScheme<SnapUserManager.removeUserAccount_result> {
         private removeUserAccount_resultStandardScheme() {
         }

         public void read(TProtocol iprot, SnapUserManager.removeUserAccount_result struct) throws TException {
            iprot.readStructBegin();

            while (true) {
               TField schemeField = iprot.readFieldBegin();
               if (schemeField.type == 0) {
                  iprot.readStructEnd();
                  struct.validate();
                  return;
               }

               switch (schemeField.id) {
                  case 0:
                     if (schemeField.type == 2) {
                        struct.success = iprot.readBool();
                        struct.setSuccessIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  default:
                     TProtocolUtil.skip(iprot, schemeField.type);
               }

               iprot.readFieldEnd();
            }
         }

         public void write(TProtocol oprot, SnapUserManager.removeUserAccount_result struct) throws TException {
            struct.validate();
            oprot.writeStructBegin(SnapUserManager.removeUserAccount_result.STRUCT_DESC);
            if (struct.isSetSuccess()) {
               oprot.writeFieldBegin(SnapUserManager.removeUserAccount_result.SUCCESS_FIELD_DESC);
               oprot.writeBool(struct.success);
               oprot.writeFieldEnd();
            }

            oprot.writeFieldStop();
            oprot.writeStructEnd();
         }
      }

      private static class removeUserAccount_resultStandardSchemeFactory implements SchemeFactory {
         private removeUserAccount_resultStandardSchemeFactory() {
         }

         public SnapUserManager.removeUserAccount_result.removeUserAccount_resultStandardScheme getScheme() {
            return new SnapUserManager.removeUserAccount_result.removeUserAccount_resultStandardScheme();
         }
      }

      private static class removeUserAccount_resultTupleScheme extends TupleScheme<SnapUserManager.removeUserAccount_result> {
         private removeUserAccount_resultTupleScheme() {
         }

         public void write(TProtocol prot, SnapUserManager.removeUserAccount_result struct) throws TException {
            TTupleProtocol oprot = (TTupleProtocol)prot;
            BitSet optionals = new BitSet();
            if (struct.isSetSuccess()) {
               optionals.set(0);
            }

            oprot.writeBitSet(optionals, 1);
            if (struct.isSetSuccess()) {
               oprot.writeBool(struct.success);
            }
         }

         public void read(TProtocol prot, SnapUserManager.removeUserAccount_result struct) throws TException {
            TTupleProtocol iprot = (TTupleProtocol)prot;
            BitSet incoming = iprot.readBitSet(1);
            if (incoming.get(0)) {
               struct.success = iprot.readBool();
               struct.setSuccessIsSet(true);
            }
         }
      }

      private static class removeUserAccount_resultTupleSchemeFactory implements SchemeFactory {
         private removeUserAccount_resultTupleSchemeFactory() {
         }

         public SnapUserManager.removeUserAccount_result.removeUserAccount_resultTupleScheme getScheme() {
            return new SnapUserManager.removeUserAccount_result.removeUserAccount_resultTupleScheme();
         }
      }
   }

   public static class removeUserFromGroup_args
      implements TBase<SnapUserManager.removeUserFromGroup_args, SnapUserManager.removeUserFromGroup_args._Fields>,
      Serializable,
      Cloneable,
      Comparable<SnapUserManager.removeUserFromGroup_args> {
      private static final TStruct STRUCT_DESC = new TStruct("removeUserFromGroup_args");
      private static final TField USER_ID_FIELD_DESC = new TField("userId", (byte)11, (short)1);
      private static final TField GROUP_ID_FIELD_DESC = new TField("groupId", (byte)11, (short)2);
      private static final SchemeFactory STANDARD_SCHEME_FACTORY = new SnapUserManager.removeUserFromGroup_args.removeUserFromGroup_argsStandardSchemeFactory();
      private static final SchemeFactory TUPLE_SCHEME_FACTORY = new SnapUserManager.removeUserFromGroup_args.removeUserFromGroup_argsTupleSchemeFactory();
      @Nullable
      public String userId;
      @Nullable
      public String groupId;
      public static final Map<SnapUserManager.removeUserFromGroup_args._Fields, FieldMetaData> metaDataMap;

      public removeUserFromGroup_args() {
      }

      public removeUserFromGroup_args(String userId, String groupId) {
         this();
         this.userId = userId;
         this.groupId = groupId;
      }

      public removeUserFromGroup_args(SnapUserManager.removeUserFromGroup_args other) {
         if (other.isSetUserId()) {
            this.userId = other.userId;
         }

         if (other.isSetGroupId()) {
            this.groupId = other.groupId;
         }
      }

      public SnapUserManager.removeUserFromGroup_args deepCopy() {
         return new SnapUserManager.removeUserFromGroup_args(this);
      }

      public void clear() {
         this.userId = null;
         this.groupId = null;
      }

      @Nullable
      public String getUserId() {
         return this.userId;
      }

      public SnapUserManager.removeUserFromGroup_args setUserId(@Nullable String userId) {
         this.userId = userId;
         return this;
      }

      public void unsetUserId() {
         this.userId = null;
      }

      public boolean isSetUserId() {
         return this.userId != null;
      }

      public void setUserIdIsSet(boolean value) {
         if (!value) {
            this.userId = null;
         }
      }

      @Nullable
      public String getGroupId() {
         return this.groupId;
      }

      public SnapUserManager.removeUserFromGroup_args setGroupId(@Nullable String groupId) {
         this.groupId = groupId;
         return this;
      }

      public void unsetGroupId() {
         this.groupId = null;
      }

      public boolean isSetGroupId() {
         return this.groupId != null;
      }

      public void setGroupIdIsSet(boolean value) {
         if (!value) {
            this.groupId = null;
         }
      }

      public void setFieldValue(SnapUserManager.removeUserFromGroup_args._Fields field, @Nullable Object value) {
         switch (field) {
            case USER_ID:
               if (value == null) {
                  this.unsetUserId();
               } else {
                  this.setUserId((String)value);
               }
               break;
            case GROUP_ID:
               if (value == null) {
                  this.unsetGroupId();
               } else {
                  this.setGroupId((String)value);
               }
         }
      }

      @Nullable
      public Object getFieldValue(SnapUserManager.removeUserFromGroup_args._Fields field) {
         switch (field) {
            case USER_ID:
               return this.getUserId();
            case GROUP_ID:
               return this.getGroupId();
            default:
               throw new IllegalStateException();
         }
      }

      public boolean isSet(SnapUserManager.removeUserFromGroup_args._Fields field) {
         if (field == null) {
            throw new IllegalArgumentException();
         }

         switch (field) {
            case USER_ID:
               return this.isSetUserId();
            case GROUP_ID:
               return this.isSetGroupId();
            default:
               throw new IllegalStateException();
         }
      }

      @Override
      public boolean equals(Object that) {
         return that instanceof SnapUserManager.removeUserFromGroup_args ? this.equals((SnapUserManager.removeUserFromGroup_args)that) : false;
      }

      public boolean equals(SnapUserManager.removeUserFromGroup_args that) {
         if (that == null) {
            return false;
         }

         if (this == that) {
            return true;
         }

         boolean this_present_userId = this.isSetUserId();
         boolean that_present_userId = that.isSetUserId();
         if (this_present_userId || that_present_userId) {
            if (!this_present_userId || !that_present_userId) {
               return false;
            }

            if (!this.userId.equals(that.userId)) {
               return false;
            }
         }

         boolean this_present_groupId = this.isSetGroupId();
         boolean that_present_groupId = that.isSetGroupId();
         if (this_present_groupId || that_present_groupId) {
            if (!this_present_groupId || !that_present_groupId) {
               return false;
            }

            if (!this.groupId.equals(that.groupId)) {
               return false;
            }
         }

         return true;
      }

      @Override
      public int hashCode() {
         int hashCode = 1;
         hashCode = hashCode * 8191 + (this.isSetUserId() ? 131071 : 524287);
         if (this.isSetUserId()) {
            hashCode = hashCode * 8191 + this.userId.hashCode();
         }

         hashCode = hashCode * 8191 + (this.isSetGroupId() ? 131071 : 524287);
         if (this.isSetGroupId()) {
            hashCode = hashCode * 8191 + this.groupId.hashCode();
         }

         return hashCode;
      }

      public int compareTo(SnapUserManager.removeUserFromGroup_args other) {
         if (!this.getClass().equals(other.getClass())) {
            return this.getClass().getName().compareTo(other.getClass().getName());
         }

         int lastComparison = 0;
         lastComparison = Boolean.compare(this.isSetUserId(), other.isSetUserId());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetUserId()) {
            lastComparison = TBaseHelper.compareTo(this.userId, other.userId);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         lastComparison = Boolean.compare(this.isSetGroupId(), other.isSetGroupId());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetGroupId()) {
            lastComparison = TBaseHelper.compareTo(this.groupId, other.groupId);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         return 0;
      }

      @Nullable
      public SnapUserManager.removeUserFromGroup_args._Fields fieldForId(int fieldId) {
         return SnapUserManager.removeUserFromGroup_args._Fields.findByThriftId(fieldId);
      }

      public void read(TProtocol iprot) throws TException {
         scheme(iprot).read(iprot, this);
      }

      public void write(TProtocol oprot) throws TException {
         scheme(oprot).write(oprot, this);
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder("removeUserFromGroup_args(");
         boolean first = true;
         sb.append("userId:");
         if (this.userId == null) {
            sb.append("null");
         } else {
            sb.append(this.userId);
         }

         first = false;
         if (!first) {
            sb.append(", ");
         }

         sb.append("groupId:");
         if (this.groupId == null) {
            sb.append("null");
         } else {
            sb.append(this.groupId);
         }

         first = false;
         sb.append(")");
         return sb.toString();
      }

      public void validate() throws TException {
      }

      private void writeObject(ObjectOutputStream out) throws IOException {
         try {
            this.write(new TCompactProtocol(new TIOStreamTransport(out)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         try {
            this.read(new TCompactProtocol(new TIOStreamTransport(in)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private static <S extends IScheme> S scheme(TProtocol proto) {
         return (S)(StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
      }

      static {
         Map<SnapUserManager.removeUserFromGroup_args._Fields, FieldMetaData> tmpMap = new EnumMap<>(SnapUserManager.removeUserFromGroup_args._Fields.class);
         tmpMap.put(SnapUserManager.removeUserFromGroup_args._Fields.USER_ID, new FieldMetaData("userId", (byte)3, new FieldValueMetaData((byte)11)));
         tmpMap.put(SnapUserManager.removeUserFromGroup_args._Fields.GROUP_ID, new FieldMetaData("groupId", (byte)3, new FieldValueMetaData((byte)11)));
         metaDataMap = Collections.unmodifiableMap(tmpMap);
         FieldMetaData.addStructMetaDataMap(SnapUserManager.removeUserFromGroup_args.class, metaDataMap);
      }

      public enum _Fields implements TFieldIdEnum {
         USER_ID((short)1, "userId"),
         GROUP_ID((short)2, "groupId");

         private static final Map<String, SnapUserManager.removeUserFromGroup_args._Fields> byName = new HashMap<>();
         private final short _thriftId;
         private final String _fieldName;

         @Nullable
         public static SnapUserManager.removeUserFromGroup_args._Fields findByThriftId(int fieldId) {
            switch (fieldId) {
               case 1:
                  return USER_ID;
               case 2:
                  return GROUP_ID;
               default:
                  return null;
            }
         }

         public static SnapUserManager.removeUserFromGroup_args._Fields findByThriftIdOrThrow(int fieldId) {
            SnapUserManager.removeUserFromGroup_args._Fields fields = findByThriftId(fieldId);
            if (fields == null) {
               throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
            } else {
               return fields;
            }
         }

         @Nullable
         public static SnapUserManager.removeUserFromGroup_args._Fields findByName(String name) {
            return byName.get(name);
         }

         _Fields(short thriftId, String fieldName) {
            this._thriftId = thriftId;
            this._fieldName = fieldName;
         }

         public short getThriftFieldId() {
            return this._thriftId;
         }

         public String getFieldName() {
            return this._fieldName;
         }

         static {
            for (SnapUserManager.removeUserFromGroup_args._Fields field : EnumSet.allOf(SnapUserManager.removeUserFromGroup_args._Fields.class)) {
               byName.put(field.getFieldName(), field);
            }
         }
      }

      private static class removeUserFromGroup_argsStandardScheme extends StandardScheme<SnapUserManager.removeUserFromGroup_args> {
         private removeUserFromGroup_argsStandardScheme() {
         }

         public void read(TProtocol iprot, SnapUserManager.removeUserFromGroup_args struct) throws TException {
            iprot.readStructBegin();

            while (true) {
               TField schemeField = iprot.readFieldBegin();
               if (schemeField.type == 0) {
                  iprot.readStructEnd();
                  struct.validate();
                  return;
               }

               switch (schemeField.id) {
                  case 1:
                     if (schemeField.type == 11) {
                        struct.userId = iprot.readString();
                        struct.setUserIdIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  case 2:
                     if (schemeField.type == 11) {
                        struct.groupId = iprot.readString();
                        struct.setGroupIdIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  default:
                     TProtocolUtil.skip(iprot, schemeField.type);
               }

               iprot.readFieldEnd();
            }
         }

         public void write(TProtocol oprot, SnapUserManager.removeUserFromGroup_args struct) throws TException {
            struct.validate();
            oprot.writeStructBegin(SnapUserManager.removeUserFromGroup_args.STRUCT_DESC);
            if (struct.userId != null) {
               oprot.writeFieldBegin(SnapUserManager.removeUserFromGroup_args.USER_ID_FIELD_DESC);
               oprot.writeString(struct.userId);
               oprot.writeFieldEnd();
            }

            if (struct.groupId != null) {
               oprot.writeFieldBegin(SnapUserManager.removeUserFromGroup_args.GROUP_ID_FIELD_DESC);
               oprot.writeString(struct.groupId);
               oprot.writeFieldEnd();
            }

            oprot.writeFieldStop();
            oprot.writeStructEnd();
         }
      }

      private static class removeUserFromGroup_argsStandardSchemeFactory implements SchemeFactory {
         private removeUserFromGroup_argsStandardSchemeFactory() {
         }

         public SnapUserManager.removeUserFromGroup_args.removeUserFromGroup_argsStandardScheme getScheme() {
            return new SnapUserManager.removeUserFromGroup_args.removeUserFromGroup_argsStandardScheme();
         }
      }

      private static class removeUserFromGroup_argsTupleScheme extends TupleScheme<SnapUserManager.removeUserFromGroup_args> {
         private removeUserFromGroup_argsTupleScheme() {
         }

         public void write(TProtocol prot, SnapUserManager.removeUserFromGroup_args struct) throws TException {
            TTupleProtocol oprot = (TTupleProtocol)prot;
            BitSet optionals = new BitSet();
            if (struct.isSetUserId()) {
               optionals.set(0);
            }

            if (struct.isSetGroupId()) {
               optionals.set(1);
            }

            oprot.writeBitSet(optionals, 2);
            if (struct.isSetUserId()) {
               oprot.writeString(struct.userId);
            }

            if (struct.isSetGroupId()) {
               oprot.writeString(struct.groupId);
            }
         }

         public void read(TProtocol prot, SnapUserManager.removeUserFromGroup_args struct) throws TException {
            TTupleProtocol iprot = (TTupleProtocol)prot;
            BitSet incoming = iprot.readBitSet(2);
            if (incoming.get(0)) {
               struct.userId = iprot.readString();
               struct.setUserIdIsSet(true);
            }

            if (incoming.get(1)) {
               struct.groupId = iprot.readString();
               struct.setGroupIdIsSet(true);
            }
         }
      }

      private static class removeUserFromGroup_argsTupleSchemeFactory implements SchemeFactory {
         private removeUserFromGroup_argsTupleSchemeFactory() {
         }

         public SnapUserManager.removeUserFromGroup_args.removeUserFromGroup_argsTupleScheme getScheme() {
            return new SnapUserManager.removeUserFromGroup_args.removeUserFromGroup_argsTupleScheme();
         }
      }
   }

   public static class removeUserFromGroup_result
      implements TBase<SnapUserManager.removeUserFromGroup_result, SnapUserManager.removeUserFromGroup_result._Fields>,
      Serializable,
      Cloneable,
      Comparable<SnapUserManager.removeUserFromGroup_result> {
      private static final TStruct STRUCT_DESC = new TStruct("removeUserFromGroup_result");
      private static final TField SUCCESS_FIELD_DESC = new TField("success", (byte)2, (short)0);
      private static final SchemeFactory STANDARD_SCHEME_FACTORY = new SnapUserManager.removeUserFromGroup_result.removeUserFromGroup_resultStandardSchemeFactory(
         
      );
      private static final SchemeFactory TUPLE_SCHEME_FACTORY = new SnapUserManager.removeUserFromGroup_result.removeUserFromGroup_resultTupleSchemeFactory();
      public boolean success;
      private static final int __SUCCESS_ISSET_ID = 0;
      private byte __isset_bitfield = 0;
      public static final Map<SnapUserManager.removeUserFromGroup_result._Fields, FieldMetaData> metaDataMap;

      public removeUserFromGroup_result() {
      }

      public removeUserFromGroup_result(boolean success) {
         this();
         this.success = success;
         this.setSuccessIsSet(true);
      }

      public removeUserFromGroup_result(SnapUserManager.removeUserFromGroup_result other) {
         this.__isset_bitfield = other.__isset_bitfield;
         this.success = other.success;
      }

      public SnapUserManager.removeUserFromGroup_result deepCopy() {
         return new SnapUserManager.removeUserFromGroup_result(this);
      }

      public void clear() {
         this.setSuccessIsSet(false);
         this.success = false;
      }

      public boolean isSuccess() {
         return this.success;
      }

      public SnapUserManager.removeUserFromGroup_result setSuccess(boolean success) {
         this.success = success;
         this.setSuccessIsSet(true);
         return this;
      }

      public void unsetSuccess() {
         this.__isset_bitfield = EncodingUtils.clearBit(this.__isset_bitfield, 0);
      }

      public boolean isSetSuccess() {
         return EncodingUtils.testBit(this.__isset_bitfield, 0);
      }

      public void setSuccessIsSet(boolean value) {
         this.__isset_bitfield = EncodingUtils.setBit(this.__isset_bitfield, 0, value);
      }

      public void setFieldValue(SnapUserManager.removeUserFromGroup_result._Fields field, @Nullable Object value) {
         switch (field) {
            case SUCCESS:
               if (value == null) {
                  this.unsetSuccess();
               } else {
                  this.setSuccess((Boolean)value);
               }
         }
      }

      @Nullable
      public Object getFieldValue(SnapUserManager.removeUserFromGroup_result._Fields field) {
         switch (field) {
            case SUCCESS:
               return this.isSuccess();
            default:
               throw new IllegalStateException();
         }
      }

      public boolean isSet(SnapUserManager.removeUserFromGroup_result._Fields field) {
         if (field == null) {
            throw new IllegalArgumentException();
         }

         switch (field) {
            case SUCCESS:
               return this.isSetSuccess();
            default:
               throw new IllegalStateException();
         }
      }

      @Override
      public boolean equals(Object that) {
         return that instanceof SnapUserManager.removeUserFromGroup_result ? this.equals((SnapUserManager.removeUserFromGroup_result)that) : false;
      }

      public boolean equals(SnapUserManager.removeUserFromGroup_result that) {
         if (that == null) {
            return false;
         }

         if (this == that) {
            return true;
         }

         boolean this_present_success = true;
         boolean that_present_success = true;
         if (this_present_success || that_present_success) {
            if (!this_present_success || !that_present_success) {
               return false;
            }

            if (this.success != that.success) {
               return false;
            }
         }

         return true;
      }

      @Override
      public int hashCode() {
         int hashCode = 1;
         return hashCode * 8191 + (this.success ? 131071 : 524287);
      }

      public int compareTo(SnapUserManager.removeUserFromGroup_result other) {
         if (!this.getClass().equals(other.getClass())) {
            return this.getClass().getName().compareTo(other.getClass().getName());
         }

         int lastComparison = 0;
         lastComparison = Boolean.compare(this.isSetSuccess(), other.isSetSuccess());
         if (lastComparison != 0) {
            return lastComparison;
         }

         if (this.isSetSuccess()) {
            lastComparison = TBaseHelper.compareTo(this.success, other.success);
            if (lastComparison != 0) {
               return lastComparison;
            }
         }

         return 0;
      }

      @Nullable
      public SnapUserManager.removeUserFromGroup_result._Fields fieldForId(int fieldId) {
         return SnapUserManager.removeUserFromGroup_result._Fields.findByThriftId(fieldId);
      }

      public void read(TProtocol iprot) throws TException {
         scheme(iprot).read(iprot, this);
      }

      public void write(TProtocol oprot) throws TException {
         scheme(oprot).write(oprot, this);
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder("removeUserFromGroup_result(");
         boolean first = true;
         sb.append("success:");
         sb.append(this.success);
         first = false;
         sb.append(")");
         return sb.toString();
      }

      public void validate() throws TException {
      }

      private void writeObject(ObjectOutputStream out) throws IOException {
         try {
            this.write(new TCompactProtocol(new TIOStreamTransport(out)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         try {
            this.__isset_bitfield = 0;
            this.read(new TCompactProtocol(new TIOStreamTransport(in)));
         } catch (TException te) {
            throw new IOException(te);
         }
      }

      private static <S extends IScheme> S scheme(TProtocol proto) {
         return (S)(StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
      }

      static {
         Map<SnapUserManager.removeUserFromGroup_result._Fields, FieldMetaData> tmpMap = new EnumMap<>(SnapUserManager.removeUserFromGroup_result._Fields.class);
         tmpMap.put(SnapUserManager.removeUserFromGroup_result._Fields.SUCCESS, new FieldMetaData("success", (byte)3, new FieldValueMetaData((byte)2)));
         metaDataMap = Collections.unmodifiableMap(tmpMap);
         FieldMetaData.addStructMetaDataMap(SnapUserManager.removeUserFromGroup_result.class, metaDataMap);
      }

      public enum _Fields implements TFieldIdEnum {
         SUCCESS((short)0, "success");

         private static final Map<String, SnapUserManager.removeUserFromGroup_result._Fields> byName = new HashMap<>();
         private final short _thriftId;
         private final String _fieldName;

         @Nullable
         public static SnapUserManager.removeUserFromGroup_result._Fields findByThriftId(int fieldId) {
            switch (fieldId) {
               case 0:
                  return SUCCESS;
               default:
                  return null;
            }
         }

         public static SnapUserManager.removeUserFromGroup_result._Fields findByThriftIdOrThrow(int fieldId) {
            SnapUserManager.removeUserFromGroup_result._Fields fields = findByThriftId(fieldId);
            if (fields == null) {
               throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
            } else {
               return fields;
            }
         }

         @Nullable
         public static SnapUserManager.removeUserFromGroup_result._Fields findByName(String name) {
            return byName.get(name);
         }

         _Fields(short thriftId, String fieldName) {
            this._thriftId = thriftId;
            this._fieldName = fieldName;
         }

         public short getThriftFieldId() {
            return this._thriftId;
         }

         public String getFieldName() {
            return this._fieldName;
         }

         static {
            for (SnapUserManager.removeUserFromGroup_result._Fields field : EnumSet.allOf(SnapUserManager.removeUserFromGroup_result._Fields.class)) {
               byName.put(field.getFieldName(), field);
            }
         }
      }

      private static class removeUserFromGroup_resultStandardScheme extends StandardScheme<SnapUserManager.removeUserFromGroup_result> {
         private removeUserFromGroup_resultStandardScheme() {
         }

         public void read(TProtocol iprot, SnapUserManager.removeUserFromGroup_result struct) throws TException {
            iprot.readStructBegin();

            while (true) {
               TField schemeField = iprot.readFieldBegin();
               if (schemeField.type == 0) {
                  iprot.readStructEnd();
                  struct.validate();
                  return;
               }

               switch (schemeField.id) {
                  case 0:
                     if (schemeField.type == 2) {
                        struct.success = iprot.readBool();
                        struct.setSuccessIsSet(true);
                     } else {
                        TProtocolUtil.skip(iprot, schemeField.type);
                     }
                     break;
                  default:
                     TProtocolUtil.skip(iprot, schemeField.type);
               }

               iprot.readFieldEnd();
            }
         }

         public void write(TProtocol oprot, SnapUserManager.removeUserFromGroup_result struct) throws TException {
            struct.validate();
            oprot.writeStructBegin(SnapUserManager.removeUserFromGroup_result.STRUCT_DESC);
            if (struct.isSetSuccess()) {
               oprot.writeFieldBegin(SnapUserManager.removeUserFromGroup_result.SUCCESS_FIELD_DESC);
               oprot.writeBool(struct.success);
               oprot.writeFieldEnd();
            }

            oprot.writeFieldStop();
            oprot.writeStructEnd();
         }
      }

      private static class removeUserFromGroup_resultStandardSchemeFactory implements SchemeFactory {
         private removeUserFromGroup_resultStandardSchemeFactory() {
         }

         public SnapUserManager.removeUserFromGroup_result.removeUserFromGroup_resultStandardScheme getScheme() {
            return new SnapUserManager.removeUserFromGroup_result.removeUserFromGroup_resultStandardScheme();
         }
      }

      private static class removeUserFromGroup_resultTupleScheme extends TupleScheme<SnapUserManager.removeUserFromGroup_result> {
         private removeUserFromGroup_resultTupleScheme() {
         }

         public void write(TProtocol prot, SnapUserManager.removeUserFromGroup_result struct) throws TException {
            TTupleProtocol oprot = (TTupleProtocol)prot;
            BitSet optionals = new BitSet();
            if (struct.isSetSuccess()) {
               optionals.set(0);
            }

            oprot.writeBitSet(optionals, 1);
            if (struct.isSetSuccess()) {
               oprot.writeBool(struct.success);
            }
         }

         public void read(TProtocol prot, SnapUserManager.removeUserFromGroup_result struct) throws TException {
            TTupleProtocol iprot = (TTupleProtocol)prot;
            BitSet incoming = iprot.readBitSet(1);
            if (incoming.get(0)) {
               struct.success = iprot.readBool();
               struct.setSuccessIsSet(true);
            }
         }
      }

      private static class removeUserFromGroup_resultTupleSchemeFactory implements SchemeFactory {
         private removeUserFromGroup_resultTupleSchemeFactory() {
         }

         public SnapUserManager.removeUserFromGroup_result.removeUserFromGroup_resultTupleScheme getScheme() {
            return new SnapUserManager.removeUserFromGroup_result.removeUserFromGroup_resultTupleScheme();
         }
      }
   }
}
