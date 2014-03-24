/*
 * Copyright 2010-2012 Luca Garulli (l.garulli--at--orientechnologies.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orientechnologies.orient.core.serialization.serializer.binary;

import java.util.HashMap;
import java.util.Map;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.serialization.types.OBinarySerializer;
import com.orientechnologies.common.serialization.types.OBinaryTypeSerializer;
import com.orientechnologies.common.serialization.types.OBooleanSerializer;
import com.orientechnologies.common.serialization.types.OByteSerializer;
import com.orientechnologies.common.serialization.types.OCharSerializer;
import com.orientechnologies.common.serialization.types.ODateSerializer;
import com.orientechnologies.common.serialization.types.ODateTimeSerializer;
import com.orientechnologies.common.serialization.types.ODecimalSerializer;
import com.orientechnologies.common.serialization.types.ODoubleSerializer;
import com.orientechnologies.common.serialization.types.OFloatSerializer;
import com.orientechnologies.common.serialization.types.OIntegerSerializer;
import com.orientechnologies.common.serialization.types.OLongSerializer;
import com.orientechnologies.common.serialization.types.ONullSerializer;
import com.orientechnologies.common.serialization.types.OShortSerializer;
import com.orientechnologies.common.serialization.types.OStringSerializer;
import com.orientechnologies.common.serialization.types.legacy.OStringSerializer_1_5_1;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.serialization.serializer.binary.impl.OLinkSerializer;
import com.orientechnologies.orient.core.serialization.serializer.binary.impl.index.OCompositeKeySerializer;
import com.orientechnologies.orient.core.serialization.serializer.binary.impl.index.OSimpleKeySerializer;
import com.orientechnologies.orient.core.serialization.serializer.stream.OStreamSerializerListRID;
import com.orientechnologies.orient.core.serialization.serializer.stream.OStreamSerializerOldRIDContainer;
import com.orientechnologies.orient.core.serialization.serializer.stream.OStreamSerializerRID;
import com.orientechnologies.orient.core.serialization.serializer.stream.OStreamSerializerSBTreeIndexRIDContainer;
import com.orientechnologies.orient.core.storage.impl.local.eh.OClusterPositionSerializer;
import com.orientechnologies.orient.core.storage.impl.local.eh.OPhysicalPositionSerializer;

/**
 * This class is responsible for obtaining OBinarySerializer realization, by it's id of type of object that should be serialized.
 * 
 * 
 * @author <a href="mailto:gmandnepr@gmail.com">Evgeniy Degtiarenko</a>
 */
public class OBinarySerializerFactory {

  private final Map<Byte, OBinarySerializer<?>>                  serializerIdMap        = new HashMap<Byte, OBinarySerializer<?>>();
  private final Map<Byte, Class<? extends OBinarySerializer<?>>> serializerClassesIdMap = new HashMap<Byte, Class<? extends OBinarySerializer<?>>>();
  private final Map<OType, OBinarySerializer<?>>                 serializerTypeMap      = new HashMap<OType, OBinarySerializer<?>>();

  /**
   * Size of the type identifier block size
   */
  public static final int                                        TYPE_IDENTIFIER_SIZE   = 1;

  private OBinarySerializerFactory() {
  }

  public void registerSerializer(final OBinarySerializer<?> iInstance, final OType iType) {
    if (serializerIdMap.containsKey(iInstance.getId()))
      throw new IllegalArgumentException("Binary serializer with id " + iInstance.getId() + " has been already registered.");

    serializerIdMap.put(iInstance.getId(), iInstance);
    if (iType != null)
      serializerTypeMap.put(iType, iInstance);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void registerSerializer(final byte iId, final Class<? extends OBinarySerializer> iClass) {
    if (serializerClassesIdMap.containsKey(iId))
      throw new IllegalStateException("Serializer with id " + iId + " has been already registered.");

    serializerClassesIdMap.put(iId, (Class<? extends OBinarySerializer<?>>) iClass);
  }

  /**
   * Obtain OBinarySerializer instance by it's id.
   * 
   * @param identifier
   *          is serializes identifier.
   * @return OBinarySerializer instance.
   */
  public OBinarySerializer<?> getObjectSerializer(final byte identifier) {
    OBinarySerializer<?> impl = serializerIdMap.get(identifier);
    if (impl == null) {
      final Class<? extends OBinarySerializer<?>> cls = serializerClassesIdMap.get(identifier);
      if (cls != null)
        try {
          impl = cls.newInstance();
        } catch (Exception e) {
          OLogManager.instance().error(this, "Cannot create an instance of class %s invoking the empty constructor", cls);
        }
    }
    return impl;
  }

  /**
   * Obtain OBinarySerializer realization for the OType
   * 
   * @param type
   *          is the OType to obtain serializer algorithm for
   * @return OBinarySerializer instance
   */
  @SuppressWarnings("unchecked")
  public <T> OBinarySerializer<T> getObjectSerializer(final OType type) {
    return (OBinarySerializer<T>) serializerTypeMap.get(type);
  }

  public static OBinarySerializerFactory create(int binaryFormatVersion) {
    final OBinarySerializerFactory factory = new OBinarySerializerFactory();

    // STATELESS SERIALIER
    factory.registerSerializer(new ONullSerializer(), null);

    factory.registerSerializer(OBooleanSerializer.INSTANCE, OType.BOOLEAN);
    factory.registerSerializer(OIntegerSerializer.INSTANCE, OType.INTEGER);
    factory.registerSerializer(OShortSerializer.INSTANCE, OType.SHORT);
    factory.registerSerializer(OLongSerializer.INSTANCE, OType.LONG);
    factory.registerSerializer(OFloatSerializer.INSTANCE, OType.FLOAT);
    factory.registerSerializer(ODoubleSerializer.INSTANCE, OType.DOUBLE);
    factory.registerSerializer(ODateTimeSerializer.INSTANCE, OType.DATETIME);
    factory.registerSerializer(OCharSerializer.INSTANCE, null);
    if (binaryFormatVersion <= 8)
      factory.registerSerializer(OStringSerializer_1_5_1.INSTANCE, OType.STRING);
    else
      factory.registerSerializer(OStringSerializer.INSTANCE, OType.STRING);

    factory.registerSerializer(OByteSerializer.INSTANCE, OType.BYTE);
    factory.registerSerializer(ODateSerializer.INSTANCE, OType.DATE);
    factory.registerSerializer(OLinkSerializer.INSTANCE, OType.LINK);
    factory.registerSerializer(OCompositeKeySerializer.INSTANCE, null);
    factory.registerSerializer(OStreamSerializerRID.INSTANCE, null);
    factory.registerSerializer(OBinaryTypeSerializer.INSTANCE, OType.BINARY);
    factory.registerSerializer(ODecimalSerializer.INSTANCE, OType.DECIMAL);

    factory.registerSerializer(OStreamSerializerListRID.INSTANCE, null);
    factory.registerSerializer(OStreamSerializerOldRIDContainer.INSTANCE, null);
    factory.registerSerializer(OStreamSerializerSBTreeIndexRIDContainer.INSTANCE, null);

    factory.registerSerializer(OPhysicalPositionSerializer.INSTANCE, null);
    factory.registerSerializer(OClusterPositionSerializer.INSTANCE, null);

    // STATEFUL SERIALIER
    factory.registerSerializer(OSimpleKeySerializer.ID, OSimpleKeySerializer.class);

    return factory;
  }

  public static OBinarySerializerFactory getInstance() {
    final ODatabaseRecord database = ODatabaseRecordThreadLocal.INSTANCE.getIfDefined();
    if (database != null)
      return database.getSerializerFactory();
    else
      return OBinarySerializerFactory.create(Integer.MAX_VALUE);
  }
}
