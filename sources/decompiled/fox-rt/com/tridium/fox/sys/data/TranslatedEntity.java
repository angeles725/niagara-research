package com.tridium.fox.sys.data;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.baja.data.BIDataValue;
import javax.baja.naming.BOrd;
import javax.baja.nav.BINavNode;
import javax.baja.sys.BObject;
import javax.baja.tag.Entity;
import javax.baja.tag.Id;
import javax.baja.tag.Relation;
import javax.baja.tag.Relations;
import javax.baja.tag.Tag;
import javax.baja.tag.Tags;

public class TranslatedEntity implements Entity {
   private final Tags translatedTags;
   private final Relations translatedRelations;
   private final Optional<BOrd> translatedOrd;

   public TranslatedEntity(Entity entity, BiFunction<BOrd, BObject, BOrd> ordTranslationFunction) {
      BObject base = entity instanceof BObject ? (BObject)entity : null;
      this.translatedTags = new TranslatedEntity.TranslatedTags(entity.tags(), base, ordTranslationFunction);
      this.translatedRelations = new TranslatedEntity.TranslatedRelations(entity.relations(), base, ordTranslationFunction);
      BOrd ord;
      if (entity instanceof BINavNode) {
         ord = ((BINavNode)entity).getNavOrd();
      } else {
         ord = (BOrd)entity.getOrdToEntity().orElse(null);
      }

      if (ord != null) {
         this.translatedOrd = Optional.of(ordTranslationFunction.apply(ord, base));
      } else {
         this.translatedOrd = Optional.empty();
      }
   }

   public Tags tags() {
      return this.translatedTags;
   }

   public Relations relations() {
      return this.translatedRelations;
   }

   public Optional<BOrd> getOrdToEntity() {
      return this.translatedOrd;
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof TranslatedEntity ? this.getOrdToEntity().equals(((TranslatedEntity)obj).getOrdToEntity()) : false;
   }

   @Override
   public int hashCode() {
      return this.getOrdToEntity().hashCode();
   }

   private static class TranslatedRelation implements Relation {
      private final Relation relation;
      private final BObject base;
      private final BiFunction<BOrd, BObject, BOrd> ordTranslationFunction;

      public TranslatedRelation(Relation relation, BObject base, BiFunction<BOrd, BObject, BOrd> ordTranslationFunction) {
         this.relation = relation;
         this.base = base;
         this.ordTranslationFunction = ordTranslationFunction;
      }

      public Id getId() {
         return this.relation.getId();
      }

      public boolean isInbound() {
         return this.relation.isInbound();
      }

      public boolean isOutbound() {
         return this.relation.isOutbound();
      }

      public Entity getEndpoint() {
         Entity endpoint = this.relation.getEndpoint();
         return endpoint == null ? null : new TranslatedEntity(endpoint, this.ordTranslationFunction);
      }

      public BOrd getEndpointOrd() {
         return this.ordTranslationFunction.apply(this.relation.getEndpointOrd(), this.base);
      }

      public Tags tags() {
         return new TranslatedEntity.TranslatedTags(this.relation.tags(), this.base, this.ordTranslationFunction);
      }
   }

   private static class TranslatedRelations implements Relations {
      private final Relations relations;
      private final BObject base;
      private final BiFunction<BOrd, BObject, BOrd> ordTranslationFunction;

      public TranslatedRelations(Relations relations, BObject base, BiFunction<BOrd, BObject, BOrd> ordTranslationFunction) {
         this.relations = relations;
         this.base = base;
         this.ordTranslationFunction = ordTranslationFunction;
      }

      public Collection<Relation> set(Id id, Collection<? extends Entity> endpoints) {
         return this.relations
            .set(id, endpoints)
            .stream()
            .map(r -> new TranslatedEntity.TranslatedRelation(r, this.base, this.ordTranslationFunction))
            .collect(Collectors.toList());
      }

      public Collection<Relation> set(Id id, Collection<? extends Entity> endpoints, boolean isInbound) {
         return this.relations
            .set(id, endpoints, isInbound)
            .stream()
            .map(r -> new TranslatedEntity.TranslatedRelation(r, this.base, this.ordTranslationFunction))
            .collect(Collectors.toList());
      }

      public Relation set(Id id, Entity endpoint) {
         return new TranslatedEntity.TranslatedRelation(this.relations.set(id, endpoint), this.base, this.ordTranslationFunction);
      }

      public Relation add(Relation relation) {
         return new TranslatedEntity.TranslatedRelation(this.relations.add(relation), this.base, this.ordTranslationFunction);
      }

      public Relation add(Id id, Entity endpoint) {
         return new TranslatedEntity.TranslatedRelation(this.relations.add(id, endpoint), this.base, this.ordTranslationFunction);
      }

      public Relation add(Id id, Entity endpoint, boolean isInbound) {
         return new TranslatedEntity.TranslatedRelation(this.relations.add(id, endpoint, isInbound), this.base, this.ordTranslationFunction);
      }

      public Collection<Relation> add(Id id, Collection<? extends Entity> endpoints) {
         return this.relations
            .add(id, endpoints)
            .stream()
            .map(r -> new TranslatedEntity.TranslatedRelation(r, this.base, this.ordTranslationFunction))
            .collect(Collectors.toList());
      }

      public Collection<Relation> add(Id id, Collection<? extends Entity> endpoints, boolean isInbound) {
         return this.relations
            .add(id, endpoints, isInbound)
            .stream()
            .map(r -> new TranslatedEntity.TranslatedRelation(r, this.base, this.ordTranslationFunction))
            .collect(Collectors.toList());
      }

      public boolean remove(Relation relation) {
         return this.relations.remove(relation);
      }

      public boolean remove(Id id, Entity endpoint) {
         return this.relations.remove(id, endpoint);
      }

      public boolean removeAll(Id id) {
         return this.relations.removeAll(id);
      }

      public Collection<Relation> filter(Predicate<Relation> condition, int direction) {
         return this.relations
            .filter(condition, direction)
            .stream()
            .map(r -> new TranslatedEntity.TranslatedRelation(r, this.base, this.ordTranslationFunction))
            .collect(Collectors.toList());
      }

      public Collection<Relation> getAll() {
         return this.relations
            .getAll()
            .stream()
            .map(r -> new TranslatedEntity.TranslatedRelation(r, this.base, this.ordTranslationFunction))
            .collect(Collectors.toList());
      }

      public Collection<Relation> getAll(int direction) {
         return this.relations
            .getAll(direction)
            .stream()
            .map(r -> new TranslatedEntity.TranslatedRelation(r, this.base, this.ordTranslationFunction))
            .collect(Collectors.toList());
      }

      public Optional<Relation> get(Id id) {
         return this.relations.get(id).map(r -> new TranslatedEntity.TranslatedRelation(r, this.base, this.ordTranslationFunction));
      }

      public Optional<Relation> get(Id id, int direction) {
         return this.relations.get(id, direction).map(r -> new TranslatedEntity.TranslatedRelation(r, this.base, this.ordTranslationFunction));
      }

      public Collection<Relation> getAll(Id id) {
         return this.relations
            .getAll(id)
            .stream()
            .map(r -> new TranslatedEntity.TranslatedRelation(r, this.base, this.ordTranslationFunction))
            .collect(Collectors.toList());
      }

      public Collection<Relation> getAll(Id id, int direction) {
         return this.relations
            .getAll(id, direction)
            .stream()
            .map(r -> new TranslatedEntity.TranslatedRelation(r, this.base, this.ordTranslationFunction))
            .collect(Collectors.toList());
      }

      public Optional<Relation> get(Id id, Entity endpoint) {
         return this.relations.get(id, endpoint).map(r -> new TranslatedEntity.TranslatedRelation(r, this.base, this.ordTranslationFunction));
      }

      public Optional<Relation> get(Id id, Entity endpoint, int direction) {
         return this.relations.get(id, endpoint, direction).map(r -> new TranslatedEntity.TranslatedRelation(r, this.base, this.ordTranslationFunction));
      }

      public Iterator<Relation> iterator() {
         return this.getAll().iterator();
      }

      public void forEach(Consumer<? super Relation> action) {
         this.getAll().forEach(action);
      }

      public Spliterator<Relation> spliterator() {
         return Spliterators.spliterator(this.getAll(), this.relations.spliterator().characteristics());
      }

      public boolean isEmpty() {
         return this.relations.isEmpty();
      }
   }

   private static class TranslatedTags implements Tags {
      private final Tags tags;
      private final BObject base;
      private final BiFunction<BOrd, BObject, BOrd> ordTranslationFunction;

      public TranslatedTags(Tags tags, BObject base, BiFunction<BOrd, BObject, BOrd> ordTranslationFunction) {
         this.tags = tags;
         this.base = base;
         this.ordTranslationFunction = ordTranslationFunction;
      }

      private Tag convertTag(Tag t, BObject base) {
         BIDataValue value = t.getValue();
         return value.getType().is(BOrd.TYPE) ? new Tag(t.getId(), (BIDataValue)this.ordTranslationFunction.apply((BOrd)value, base)) : t;
      }

      public boolean isEmpty() {
         return this.tags.isEmpty();
      }

      public boolean contains(Id id) {
         return this.tags.contains(id);
      }

      public boolean isMulti(Id id) {
         return this.tags.isMulti(id);
      }

      public boolean set(Tag tag) {
         return this.tags.set(tag);
      }

      public boolean set(Id id, BIDataValue value) {
         return this.tags.set(id, value);
      }

      public boolean setMulti(Id id, Collection<? extends BIDataValue> values) {
         return this.tags.setMulti(id, values);
      }

      public boolean addMulti(Id id, Collection<? extends BIDataValue> values) {
         return this.tags.addMulti(id, values);
      }

      public boolean addMulti(Tag tag) {
         return this.tags.addMulti(tag);
      }

      public boolean addMulti(Id id, BIDataValue value) {
         return this.tags.addMulti(id, value);
      }

      public boolean merge(Collection<Tag> tags) {
         return this.tags.merge(tags);
      }

      public boolean remove(Id id, BIDataValue value) {
         return this.tags.remove(id, value);
      }

      public boolean removeAll(Id id) {
         return this.tags.removeAll(id);
      }

      public boolean remove(Tag tag) {
         return this.tags.remove(tag);
      }

      public Collection<Tag> filter(Predicate<Tag> condition) {
         return this.tags.filter(condition).stream().map(t -> this.convertTag(t, this.base)).collect(Collectors.toList());
      }

      public Collection<Tag> getAll() {
         return this.tags.getAll().stream().map(t -> this.convertTag(t, this.base)).collect(Collectors.toList());
      }

      public Optional<BIDataValue> get(Id id) {
         Optional<BIDataValue> optional = this.tags.get(id);
         if (optional.isPresent()) {
            BIDataValue val = optional.get();
            if (val.getType().is(BOrd.TYPE)) {
               return Optional.of(this.ordTranslationFunction.apply((BOrd)val, this.base));
            }
         }

         return optional;
      }

      public Collection<BIDataValue> getValues(Id id) {
         return this.tags
            .getValues(id)
            .stream()
            .map(v -> v.getType().is(BOrd.TYPE) ? (BIDataValue)this.ordTranslationFunction.apply((BOrd)v, this.base) : v)
            .collect(Collectors.toList());
      }

      public Collection<Tag> getInDictionary(String dictionary) {
         return this.tags.getInDictionary(dictionary).stream().map(t -> this.convertTag(t, this.base)).collect(Collectors.toList());
      }

      public Iterator<Tag> iterator() {
         return this.getAll().iterator();
      }

      public void forEach(Consumer<? super Tag> action) {
         this.getAll().forEach(action);
      }

      public Spliterator<Tag> spliterator() {
         return Spliterators.spliterator(this.getAll(), this.tags.spliterator().characteristics());
      }
   }
}
