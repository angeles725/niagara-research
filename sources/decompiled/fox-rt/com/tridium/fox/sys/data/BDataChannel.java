package com.tridium.fox.sys.data;

import com.tridium.data.BDataTable;
import com.tridium.data.BToDataTable;
import com.tridium.data.DataTableDecoder;
import com.tridium.data.DataTableEncoder;
import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.session.Fox;
import com.tridium.fox.session.FoxCircuit;
import com.tridium.fox.session.FoxRequest;
import com.tridium.fox.session.FoxResponse;
import com.tridium.fox.session.InvalidCommandException;
import com.tridium.fox.sys.BFoxChannel;
import com.tridium.fox.sys.NiagaraStation;
import com.tridium.json.JSONObject;
import com.tridium.json.JSONTokener;
import com.tridium.sys.Nre;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.baja.collection.BITable;
import javax.baja.collection.Column;
import javax.baja.collection.TableCursor;
import javax.baja.data.BIDataTable;
import javax.baja.data.BIDataValue;
import javax.baja.entityIo.json.JsonEntityDecoder;
import javax.baja.entityIo.json.JsonEntityEncoder;
import javax.baja.entityIo.json.JsonEntityEncoder.Options;
import javax.baja.io.ValueDocDecoder;
import javax.baja.io.ValueDocEncoder;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdList;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.UnresolvedException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.query.BIQueryHandler;
import javax.baja.query.BQueryResult;
import javax.baja.query.BQueryScheme;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIObject;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.tag.Entity;
import javax.baja.tag.util.BasicEntity;
import javax.baja.util.BTypeSpec;
import javax.baja.util.Version;

@NiagaraType
public class BDataChannel extends BFoxChannel {
   public static final Type TYPE = Sys.loadType(BDataChannel.class);
   static final Logger logger = Logger.getLogger("fox.data");
   private static final Version VER_4_4 = new Version("4.4");
   private static final Version VER_4_14 = new Version("4.14");
   private static final String RESOLVE_COMMAND = "resolve";
   private static final String RESOLVE_REQ_ORD = "ord";
   private static final String RESOLVE_RESP_EXCEPTION = "exception";
   private static final String RESOLVE_RESP_RESOLVED = "resolved";
   private static final String RESOLVE_RESP_TARGET_TYPE = "targetType";
   private static final String RESOLVE_TARGET_TYPE_TABLE = "table";
   private static final String RESOLVE_TARGET_TYPE_VALUE = "value";
   private static final String RESOLVE_VALUE_TYPE = "valueType";
   private static final String RESOLVE_ENTITIES_COMMAND = "resolveEntities";
   private static final String RESOLVE_ENTITIES_REQ_ORDS = "queryOrds";
   private static final String RESOLVE_ENTITIES_REQ_OFFSET = "offset";
   private static final String RESOLVE_ENTITIES_REQ_LIMIT = "limit";
   private static final String RESOLVE_ENTITIES_REQ_PRE_VALIDATOR = "preQueryValidator";
   private static final String RESOLVE_ENTITIES_REQ_POST_FILTER = "postQueryFilter";
   private static final BFacets lightweightQueryResultsFacets = BFacets.make("lightweightSystemDbQueryResults", true);
   private static final int MAX_QUERY_PARAMETER_SIZE = 10;
   private static final String ENTITY_COLUMN = "entity";
   private static final String EXPORT_ENTITIES_COMMAND = "exportEntities";
   private static final String CAN_EXPORT_ENTITIES_COMMAND = "canExportEntities";
   private static final String EXPORT_ENTITIES_ORDS = "entityConsumerOrds";
   private static final String EXPORT_ENTITIES_ACCEPTS_EXPORT = "acceptsExport";
   private static final String ENTITY_VERSION = "eVer";
   private static final int ENTITY_ENCODING_VERSION = 1;
   private static final String SHOULD_ENCODE_TAGS = "shouldEncodeTags";
   private static final String SHOULD_ENCODE_RELATIONS = "shouldEncodeRelations";
   private static final Entity EMPTY_ENTITY = new BasicEntity();
   private static final InputStream EMPTY_INPUT_STREAM = new InputStream() {
      @Override
      public int read() {
         return -1;
      }
   };
   private static final JsonEntityDecoder ENTITY_DECODER = new JsonEntityDecoder(EMPTY_INPUT_STREAM);

   @Override
   public Type getType() {
      return TYPE;
   }

   public BDataChannel() {
      super("data");
   }

   @Override
   public void checkProcessCircuit(FoxCircuit circuit) throws Throwable {
   }

   @Override
   public FoxResponse process(FoxRequest req) throws Exception {
      String command = req.command;
      if ("canExportEntities".equals(command)) {
         return this.canExportEntities(req);
      } else {
         throw new InvalidCommandException(command);
      }
   }

   @Override
   public void circuitOpened(FoxCircuit circuit) throws Exception {
      String command = circuit.command;
      if ("resolve".equals(command)) {
         this.resolve(circuit);
      } else if ("resolveEntities".equals(command)) {
         this.resolveEntities(circuit);
      } else if ("exportEntities".equals(command)) {
         this.exportEntities(circuit);
      } else {
         throw new InvalidCommandException(command);
      }
   }

   @Override
   public Map<String, Integer> getCircuitCommandThreadPriorities() {
      Map<String, Integer> cmdToThreadPriorityMap = new HashMap<>();
      cmdToThreadPriorityMap.put("resolveEntities", Nre.getEngineManager().getPriority() - 1);
      return cmdToThreadPriorityMap;
   }

   @Override
   protected boolean allowRoutingCircuitToReachableStation(FoxCircuit circuit) {
      return "resolveEntities".equals(circuit.command) || "resolve".equals(circuit.command);
   }

   @Override
   protected Version getMinReachableStationVersionForCircuit(FoxCircuit circuit) {
      return "resolveEntities".equals(circuit.command) ? VER_4_4 : super.getMinReachableStationVersionForCircuit(circuit);
   }

   @Override
   protected Version getMinVersionAlongRouteForCircuit(FoxCircuit circuit) {
      return "resolve".equals(circuit.command) ? VER_4_14 : super.getMinVersionAlongRouteForCircuit(circuit);
   }

   public BObject resolve(BOrd ord, String... reachableStations) throws Exception {
      FoxMessage metadata = null;
      if (reachableStations != null && reachableStations.length != 0) {
         metadata = makeFoxMessageWithReachableStationRoute(this, null, VER_4_14, reachableStations);
      }

      FoxMessage req = new FoxMessage();
      req.add("ord", ord.toString());
      FoxCircuit circuit = this.openCircuit("resolve", metadata);
      circuit.writeMessage(req);
      circuit.flush();
      FoxMessage resp = circuit.readMessage();
      if (resp.getString("exception", null) != null) {
         throw Fox.exceptionTranslator.messageToException(resp);
      } else if (!resp.getBoolean("resolved", false)) {
         throw new UnresolvedException(ord.toString());
      } else {
         String targetType = resp.getString("targetType");
         if ("table".equals(targetType)) {
            BIDataTable<?> result = DataTableDecoder.decode(new DataInputStream(circuit.getInputStream()));
            circuit.close();
            return (BObject)result;
         } else if ("value".equals(targetType)) {
            String typeStr = resp.getString("valueType");
            Type valType = BTypeSpec.make(typeStr).getResolvedType();
            BObject decoder = valType.getInstance();
            BObject result = ((BIDataValue)decoder).decode(new DataInputStream(circuit.getInputStream()));
            circuit.close();
            return result;
         } else {
            throw new UnresolvedException("Unsupported result type (" + targetType + ") for " + ord);
         }
      }
   }

   public void resolve(FoxCircuit circuit) throws Exception {
      FoxMessage req = circuit.readMessage();
      String ordText = req.getString("ord", null);
      if (ordText == null) {
         circuit.writeMessage(unresolved());
         circuit.flush();
      } else {
         BObject o = null;

         try {
            BOrd ord = BOrd.make("local:|" + ordText);
            o = ord.resolve(null, this.getSessionContext()).get();
         } catch (UnresolvedException var8) {
            logger.log(Level.WARNING, "Could not resolve ord " + ordText, (Throwable)var8);
            circuit.writeMessage(unresolved());
            circuit.flush();
            return;
         } catch (Exception var9) {
            logger.log(Level.WARNING, "Error encountered resolving ord " + ordText, (Throwable)var9);
            circuit.writeMessage(Fox.exceptionTranslator.exceptionToMessage(var9));
            circuit.flush();
            return;
         }

         FoxMessage resp = new FoxMessage();
         resp.add("resolved", true);
         BObject result;
         if (o instanceof BITable) {
            resp.add("targetType", "table");
            BITable<?> table = (BITable<?>)o;
            result = (BObject & BIDataTable)BToDataTable.toDataTable(table);
         } else {
            resp.add("targetType", "value");
            result = (BObject & BIDataValue)o.toDataValue();
            resp.add("valueType", result.getType().getTypeSpec().toString());
         }

         circuit.writeMessage(resp);
         circuit.flush();
         DataOutputStream out = new DataOutputStream(circuit.getOutputStream());
         if (result instanceof BIDataTable) {
            DataTableEncoder.encode((BIDataTable)result, out, this.getSessionContext());
         } else {
            ((BIDataValue)result).encode(out);
         }

         out.flush();
         out.close();
      }
   }

   private static FoxMessage unresolved() {
      FoxMessage m = new FoxMessage();
      m.add("resolved", false);
      return m;
   }

   public Stream<Entity> resolveEntities(
      BOrdList queryOrds, int offset, int limit, BIPreQueryValidator[] preQueryValidators, BIPostQueryFilter[] postQueryFilters
   ) throws Exception {
      return this.doClientResolveEntities(
         queryOrds, offset, limit, preQueryValidators, postQueryFilters, JsonEntityEncoder.ENCODE_TAGS_AND_RELATIONS, BDataChannel::decodeEntitiesToStream
      );
   }

   public Stream<Entity> resolveEntities(
      BOrdList queryOrds, int offset, int limit, Options encodingOptions, BIPreQueryValidator[] preQueryValidators, BIPostQueryFilter[] postQueryFilters
   ) throws Exception {
      return this.doClientResolveEntities(queryOrds, offset, limit, preQueryValidators, postQueryFilters, encodingOptions, BDataChannel::decodeEntitiesToStream);
   }

   public Stream<Entity> resolveReachableStationEntities(
      BOrdList queryOrds,
      int offset,
      int limit,
      Options encodingOptions,
      BIPreQueryValidator[] preQueryValidators,
      BIPostQueryFilter[] postQueryFilters,
      String... targetStationRoute
   ) throws Exception {
      return this.doClientResolveEntities(
         queryOrds, offset, limit, preQueryValidators, postQueryFilters, encodingOptions, BDataChannel::decodeEntitiesToStream, targetStationRoute
      );
   }

   public List<Entity> resolveEntitiesToList(
      BOrdList queryOrds, int offset, int limit, BIPreQueryValidator[] preQueryValidators, BIPostQueryFilter[] postQueryFilters
   ) throws Exception {
      return this.doClientResolveEntities(
         queryOrds, offset, limit, preQueryValidators, postQueryFilters, JsonEntityEncoder.ENCODE_TAGS_AND_RELATIONS, BDataChannel::decodeEntitiesToList
      );
   }

   public List<Entity> resolveEntitiesToList(
      BOrdList queryOrds, int offset, int limit, Options encodingOptions, BIPreQueryValidator[] preQueryValidators, BIPostQueryFilter[] postQueryFilters
   ) throws Exception {
      return this.doClientResolveEntities(queryOrds, offset, limit, preQueryValidators, postQueryFilters, encodingOptions, BDataChannel::decodeEntitiesToList);
   }

   public List<Entity> resolveReachableStationEntitiesToList(
      BOrdList queryOrds,
      int offset,
      int limit,
      Options encodingOptions,
      BIPreQueryValidator[] preQueryValidators,
      BIPostQueryFilter[] postQueryFilters,
      String... targetStationRoute
   ) throws Exception {
      return this.doClientResolveEntities(
         queryOrds, offset, limit, preQueryValidators, postQueryFilters, encodingOptions, BDataChannel::decodeEntitiesToList, targetStationRoute
      );
   }

   private <E> E doClientResolveEntities(
      BOrdList queryOrds,
      int offset,
      int limit,
      BIPreQueryValidator[] preQueryValidators,
      BIPostQueryFilter[] postQueryFilters,
      Options encodingOptions,
      BiFunction<Integer, FoxCircuit, E> decodingFunction,
      String... targetStationRoute
   ) throws Exception {
      verifyRemoteVersion(this, VER_4_4);
      FoxMessage req = new FoxMessage();
      req.add("queryOrds", queryOrds.encodeToString());
      req.add("offset", offset);
      req.add("limit", limit);
      req.add("eVer", 1);
      if (encodingOptions == null) {
         encodingOptions = JsonEntityEncoder.ENCODE_TAGS_AND_RELATIONS;
      }

      req.add("shouldEncodeTags", encodingOptions.shouldEncodeTags());
      req.add("shouldEncodeRelations", encodingOptions.shouldEncodeRelations());
      if (preQueryValidators != null) {
         if (preQueryValidators.length > 10) {
            throw new IllegalArgumentException("preQueryValidators argument size exceeds max (" + preQueryValidators.length + '>' + 10 + ')');
         }

         for (BIPreQueryValidator validator : preQueryValidators) {
            if (!validator.getType().is(BValue.TYPE)) {
               throw new IllegalArgumentException("PreQueryValidator is not a BValue: " + validator);
            }

            req.add("preQueryValidator", ValueDocEncoder.marshal((BValue)validator.as(BValue.class)));
         }
      }

      if (postQueryFilters != null) {
         if (postQueryFilters.length > 10) {
            throw new IllegalArgumentException("postQueryFilters argument size exceeds max (" + postQueryFilters.length + '>' + 10 + ')');
         }

         for (BIPostQueryFilter filter : postQueryFilters) {
            if (!filter.getType().is(BValue.TYPE)) {
               throw new IllegalArgumentException("PostQueryFilter is not a BValue: " + filter);
            }

            req.add("postQueryFilter", ValueDocEncoder.marshal((BValue)filter.as(BValue.class)));
         }
      }

      FoxMessage metadata = makeFoxMessageWithReachableStationRoute(this, null, null, targetStationRoute);
      FoxCircuit circuit = this.openCircuit("resolveEntities", metadata);

      Object var24;
      try {
         circuit.writeMessage(req);
         circuit.flush();
         FoxMessage resp = circuit.readMessage();
         if (resp.getString("exception", null) != null) {
            throw Fox.exceptionTranslator.messageToException(resp);
         }

         if (!resp.getBoolean("resolved", false)) {
            throw new UnresolvedException(queryOrds.toString());
         }

         var24 = decodingFunction.apply(resp.getInt("eVer", -1), circuit);
      } finally {
         circuit.close();
      }

      return (E)var24;
   }

   private void resolveEntities(FoxCircuit circuit) throws Exception {
      FoxMessage req = circuit.readMessage();
      BOrdList queryOrds = (BOrdList)BOrdList.DEFAULT.decodeFromString(req.getString("queryOrds"));
      int offset = req.getInt("offset", -1);
      int limit = req.getInt("limit", -1);
      int encodingVersion = req.getInt("eVer", -1);
      boolean shouldEncodeTags = req.getBoolean("shouldEncodeTags", true);
      boolean shouldEncodeRelations = req.getBoolean("shouldEncodeRelations", true);
      Stream<Entity> mergedEntityStream = null;

      try {
         String[] encodedValidators = req.listStrings("preQueryValidator");
         if (encodedValidators.length > 10) {
            throw new IllegalArgumentException("preQueryValidators argument size exceeds max (" + encodedValidators.length + '>' + 10 + ')');
         }

         Collection<BIPreQueryValidator> preQueryValidators = new ArrayList<>();

         for (String encodedValidator : encodedValidators) {
            preQueryValidators.add((BIPreQueryValidator)ValueDocDecoder.unmarshal(encodedValidator));
         }

         String[] encodedFilters = req.listStrings("postQueryFilter");
         if (encodedFilters.length > 10) {
            throw new IllegalArgumentException("postQueryFilters argument size exceeds max (" + encodedFilters.length + '>' + 10);
         }

         Collection<BIPostQueryFilter> postQueryFilters = new ArrayList<>();

         for (String encodedFilter : encodedFilters) {
            postQueryFilters.add((BIPostQueryFilter)ValueDocDecoder.unmarshal(encodedFilter));
         }

         BIQueryHandler.validateQueryOrds(queryOrds);
         NiagaraStation station = this.getConnection().getConnectionTarget(NiagaraStation.class).orElse(null);

         for (BIPreQueryValidator validator : preQueryValidators) {
            validator.validateRemoteQuery(station, queryOrds, offset, limit, this.getSessionContext());
         }

         for (BOrd queryOrd : queryOrds) {
            BOrd ord = BOrd.make(BOrd.make("local:"), queryOrd);
            OrdTarget ordTarget = ord.resolve(null, new BasicContext(this.getSessionContext(), lightweightQueryResultsFacets));
            BObject queryResult = ordTarget.get();
            Stream<Entity> entityStream = null;
            if (queryResult.getType().is(BQueryResult.TYPE)) {
               entityStream = ((BQueryResult)queryResult).stream();
            } else if (queryResult.getType().is(BITable.TYPE)) {
               BITable<BIObject> table = (BITable<BIObject>)queryResult;
               entityStream = table.cursor().stream(true).filter(obj -> obj instanceof Entity).map(obj -> (Entity)obj);
            } else if (queryResult instanceof Entity) {
               entityStream = Stream.of((Entity)queryResult);
            }

            if (entityStream != null) {
               if (mergedEntityStream == null) {
                  mergedEntityStream = entityStream;
               } else {
                  mergedEntityStream = Stream.concat(mergedEntityStream, entityStream);
               }
            }
         }

         if (mergedEntityStream != null) {
            mergedEntityStream = mergedEntityStream.distinct();

            for (BIPostQueryFilter queryFilter : postQueryFilters) {
               mergedEntityStream = queryFilter.postQueryFilter(mergedEntityStream, this.getSessionContext());
            }
         } else {
            mergedEntityStream = Stream.empty();
         }

         long skip = Math.max(offset, 0);
         long max = limit <= 0 ? Long.MAX_VALUE : limit;
         mergedEntityStream = mergedEntityStream.skip(skip).limit(max);
         encodeEntities(encodingVersion, circuit, mergedEntityStream, true, new Options(shouldEncodeTags, shouldEncodeRelations), this.getSessionContext());
      } catch (UnresolvedException var26) {
         if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "UnresolvedException encountered in resolveEntities", (Throwable)var26);
         }

         if (circuit.isOpen()) {
            FoxMessage m = new FoxMessage();
            m.add("resolved", false);
            circuit.writeMessage(m);
            circuit.flush();
         }
      } catch (Exception var27) {
         logger.log(Level.WARNING, "Error in resolveEntities", (Throwable)var27);
         if (circuit.isOpen()) {
            circuit.writeMessage(Fox.exceptionTranslator.exceptionToMessage(var27));
            circuit.flush();
         }
      } finally {
         if (mergedEntityStream != null) {
            mergedEntityStream.close();
         }
      }
   }

   public boolean canExportEntities(BOrdList entityExportConsumerOrds) throws Exception {
      verifyRemoteVersion(this, VER_4_4);
      FoxRequest req = this.makeRequest("canExportEntities");
      req.add("entityConsumerOrds", entityExportConsumerOrds.encodeToString());
      FoxResponse resp = this.sendSync(req);
      return resp.getBoolean("acceptsExport");
   }

   private FoxResponse canExportEntities(FoxRequest req) throws Exception {
      BOrdList entityExportConsumerOrds = (BOrdList)BOrdList.DEFAULT.decodeFromString(req.getString("entityConsumerOrds"));
      FoxResponse resp = new FoxResponse(req);
      resp.add("acceptsExport", !this.getEnabledEntityExportConsumers(entityExportConsumerOrds).isEmpty());
      return resp;
   }

   public void exportEntities(BOrdList entityExportConsumerOrds, Stream<Entity> entityStream, Context cx, BIPostQueryFilter... postQueryFilters) throws Exception {
      Stream<Entity> filteredStream = entityStream;

      try {
         verifyRemoteVersion(this, VER_4_4);
         FoxMessage req = new FoxMessage();
         req.add("entityConsumerOrds", entityExportConsumerOrds.encodeToString());
         req.add("eVer", 1);
         FoxCircuit circuit = this.openCircuit("exportEntities");
         circuit.writeMessage(req);
         circuit.flush();
         FoxMessage resp = circuit.readMessage();
         if (!resp.getBoolean("acceptsExport")) {
            throw new LocalizableRuntimeException("niagaraDriver", "niagaraSystemIndex.exportDisabled");
         }

         if (postQueryFilters != null) {
            for (BIPostQueryFilter queryFilter : postQueryFilters) {
               filteredStream = queryFilter.postQueryFilter(filteredStream, cx);
            }
         }

         encodeEntities(resp.getInt("eVer", -1), circuit, filteredStream, false, JsonEntityEncoder.ENCODE_TAGS_AND_RELATIONS, null);
      } finally {
         filteredStream.close();
      }
   }

   private void exportEntities(FoxCircuit circuit) throws Exception {
      FoxMessage req = circuit.readMessage();
      BOrdList entityExportConsumerOrds = (BOrdList)BOrdList.DEFAULT.decodeFromString(req.getString("entityConsumerOrds"));
      int encodingVersion = req.getInt("eVer", -1);
      List<EntityExportConsumer> consumers = this.getEnabledEntityExportConsumers(entityExportConsumerOrds);
      FoxMessage resp = new FoxMessage();
      resp.add("acceptsExport", !consumers.isEmpty());
      resp.add("eVer", encodingVersion);
      circuit.writeMessage(resp);
      circuit.flush();
      if (!consumers.isEmpty()) {
         List<Entity> entities;
         try {
            entities = decodeEntitiesToList(encodingVersion, circuit);
         } finally {
            circuit.close();
         }

         for (EntityExportConsumer consumer : consumers) {
            consumer.consumeEntitiesFromRemoteExport(entities, this.getSessionContext());
         }
      } else {
         circuit.close();
      }
   }

   private List<EntityExportConsumer> getEnabledEntityExportConsumers(BOrdList entityExportConsumerOrds) {
      if (entityExportConsumerOrds.size() < 1) {
         return Collections.emptyList();
      } else {
         List<EntityExportConsumer> consumers = new ArrayList<>();
         NiagaraStation station = this.getConnection().getConnectionTarget(NiagaraStation.class).orElse(null);
         BObject base = station instanceof BObject ? (BObject)station : null;

         for (BOrd ord : entityExportConsumerOrds) {
            EntityExportConsumer consumer;
            try {
               consumer = (EntityExportConsumer)ord.get(base, this.getSessionContext());
            } catch (Exception var9) {
               throw new LocalizableRuntimeException("fox", "fox.data.unresolvedEntityConsumer", new Object[]{ord.toString()}, var9);
            }

            if (consumer.canAcceptEntitiesFromRemoteExport(this.getSessionContext())) {
               consumers.add(consumer);
            }
         }

         return consumers;
      }
   }

   private static void addRowToDataTable(Entity entity, Options encodingOptions, BDataTable dataTable) {
      dataTable.startRow();

      try {
         dataTable.set(JsonEntityEncoder.encodeToString(entity, encodingOptions), BFacets.NULL);
      } catch (Exception var6) {
         Optional<BOrd> optional = entity.getOrdToEntity();
         Object displayObj = optional.isPresent() ? optional.get() : entity;
         throw new BajaRuntimeException("Failed to encode Entity: " + displayObj, var6);
      }

      dataTable.endRow();
   }

   private static void encodeEntities(int encodingVersion, FoxCircuit circuit, Stream<Entity> entities, boolean confirm, Options encodingOptions, Context cx) throws Exception {
      if (encodingVersion == 1) {
         if (confirm) {
            FoxMessage resp = new FoxMessage();
            resp.add("resolved", true);
            resp.add("eVer", 1);
            circuit.writeMessage(resp);
            circuit.flush();
         }

         JsonEntityEncoder encoder = new JsonEntityEncoder(circuit.getOutputStream(), encodingOptions);
         Throwable out = null;

         try {
            AtomicInteger count = new AtomicInteger();
            entities.forEachOrdered(e -> {
               try {
                  encoder.encode(e, Integer.toString(count.getAndIncrement()));
               } catch (Exception var6x) {
                  Optional<BOrd> optional = e.getOrdToEntity();
                  Object displayObj = optional.isPresent() ? optional.get() : e;
                  throw new BajaRuntimeException("Failed to encode Entity: " + displayObj, var6x);
               }
            });
            if (count.get() == 0) {
               encoder.encode(EMPTY_ENTITY, Integer.toString(count.getAndIncrement()));
            }
         } catch (Throwable var31) {
            out = var31;
            throw var31;
         } finally {
            if (encoder != null) {
               if (out != null) {
                  try {
                     encoder.close();
                  } catch (Throwable var29) {
                     out.addSuppressed(var29);
                  }
               } else {
                  encoder.close();
               }
            }
         }
      } else {
         BDataTable result = new BDataTable();
         result.addColumn("entity", BString.TYPE, 0, BFacets.NULL);
         result.startRows();
         entities.forEachOrdered(e -> addRowToDataTable(e, encodingOptions, result));
         result.endRows();
         if (confirm) {
            FoxMessage resp = new FoxMessage();
            resp.add("resolved", true);
            circuit.writeMessage(resp);
            circuit.flush();
         }

         try (DataOutputStream out = new DataOutputStream(circuit.getOutputStream())) {
            DataTableEncoder.encode(result, out, cx);
            out.flush();
         }
      }
   }

   private static Stream<Entity> decodeEntitiesToStream(int encodingVersion, FoxCircuit circuit) {
      return encodingVersion == 1 ? decodeEntitiesVersion1(circuit).stream() : decodeEntitiesVersion0(circuit);
   }

   private static List<Entity> decodeEntitiesToList(int encodingVersion, FoxCircuit circuit) {
      return encodingVersion == 1 ? decodeEntitiesVersion1(circuit) : decodeEntitiesVersion0(circuit).collect(Collectors.toList());
   }

   private static Stream<Entity> decodeEntitiesVersion0(FoxCircuit circuit) {
      BIDataTable<?> result;
      try {
         result = DataTableDecoder.decode(new DataInputStream(circuit.getInputStream()));
      } catch (IOException var22) {
         throw new BajaRuntimeException(var22);
      } finally {
         circuit.close();
      }

      Column col = result.getColumns().get("entity");
      TableCursor cursor = result.cursor();
      Throwable var4 = null;

      Stream var5;
      try {
         var5 = cursor.stream().map(row -> {
            try {
               return JsonEntityDecoder.decodeFromString(row.cell(col).toString());
            } catch (Exception var3x) {
               throw new BajaRuntimeException(var3x);
            }
         });
      } catch (Throwable var21) {
         var4 = var21;
         throw var21;
      } finally {
         if (cursor != null) {
            if (var4 != null) {
               try {
                  cursor.close();
               } catch (Throwable var20) {
                  var4.addSuppressed(var20);
               }
            } else {
               cursor.close();
            }
         }
      }

      return var5;
   }

   private static List<Entity> decodeEntitiesVersion1(FoxCircuit circuit) {
      List<Entity> list = new ArrayList<>();

      try (Reader reader = new InputStreamReader(circuit.getInputStream())) {
         JSONObject root = new JSONObject(new JSONTokener(reader));
         int count = 0;

         for (String key = Integer.toString(count); root.has(key); key = Integer.toString(++count)) {
            list.add(ENTITY_DECODER.decodeEntity(root.getJSONObject(key)));
         }
      } catch (IOException var17) {
         throw new BajaRuntimeException(var17);
      }

      if (list.size() == 1) {
         Entity e = list.get(0);
         if (!e.getOrdToEntity().isPresent() && e.tags().isEmpty() && e.relations().isEmpty()) {
            list = Collections.emptyList();
         }
      }

      circuit.close();
      return list;
   }

   static {
      BQueryScheme.registerPriorityQueryHandler(BFoxQueryHandler.INSTANCE, 0);
   }
}
