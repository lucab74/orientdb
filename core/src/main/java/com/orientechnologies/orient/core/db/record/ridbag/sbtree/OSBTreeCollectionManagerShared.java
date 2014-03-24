/*
 * Copyright 2010-2012 Luca Garulli (l.garulli(at)orientechnologies.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orientechnologies.orient.core.db.record.ridbag.sbtree;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.orientechnologies.common.serialization.types.OIntegerSerializer;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;
import com.orientechnologies.orient.core.index.sbtreebonsai.local.OSBTreeBonsai;
import com.orientechnologies.orient.core.index.sbtreebonsai.local.OSBTreeBonsaiLocal;
import com.orientechnologies.orient.core.serialization.serializer.binary.impl.OLinkSerializer;
import com.orientechnologies.orient.core.storage.impl.local.OStorageLocalAbstract;

/**
 * @author <a href="mailto:enisher@gmail.com">Artem Orobets</a>
 */
public class OSBTreeCollectionManagerShared extends OSBTreeCollectionManagerAbstract {
  private ThreadLocal<Map<UUID, OBonsaiCollectionPointer>> collectionPointerChanges = new ThreadLocal<Map<UUID, OBonsaiCollectionPointer>>() {
                                                                                      @Override
                                                                                      protected Map<UUID, OBonsaiCollectionPointer> initialValue() {
                                                                                        return new HashMap<UUID, OBonsaiCollectionPointer>();
                                                                                      }
                                                                                    };

  public OSBTreeCollectionManagerShared() {
    super();
  }

  public OSBTreeCollectionManagerShared(int evictionThreshold, int cacheMaxSize) {
    super(evictionThreshold, cacheMaxSize);
  }

  @Override
  public OBonsaiCollectionPointer createSBTree(int clusterId, UUID ownerUUID) {
    final OBonsaiCollectionPointer pointer = super.createSBTree(clusterId, ownerUUID);

    if (ownerUUID != null) {
      Map<UUID, OBonsaiCollectionPointer> changedPointers = collectionPointerChanges.get();
      changedPointers.put(ownerUUID, pointer);
    }

    return pointer;
  }

  @Override
  protected OSBTreeBonsaiLocal<OIdentifiable, Integer> createTree(int clusterId) {
    OSBTreeBonsaiLocal<OIdentifiable, Integer> tree = new OSBTreeBonsaiLocal<OIdentifiable, Integer>(DEFAULT_EXTENSION, true);
    tree.create(FILE_NAME_PREFIX + clusterId, OLinkSerializer.INSTANCE, OIntegerSerializer.INSTANCE);

    return tree;
  }

  @Override
  protected OSBTreeBonsai<OIdentifiable, Integer> loadTree(OBonsaiCollectionPointer collectionPointer) {
    OSBTreeBonsaiLocal<OIdentifiable, Integer> tree = new OSBTreeBonsaiLocal<OIdentifiable, Integer>(DEFAULT_EXTENSION, true);

    tree.load(collectionPointer.getFileId(), collectionPointer.getRootPointer(),
        (OStorageLocalAbstract) ODatabaseRecordThreadLocal.INSTANCE.get().getStorage().getUnderlying());

    return tree;
  }

  /**
   * Change UUID to null to prevent its serialization to disk.
   * 
   * @param collection
   * @return
   */
  @Override
  public UUID listenForChanges(ORidBag collection) {
    UUID ownerUUID = collection.getTemporaryId();
    if (ownerUUID != null) {
      final OBonsaiCollectionPointer pointer = collection.getPointer();

      Map<UUID, OBonsaiCollectionPointer> changedPointers = collectionPointerChanges.get();
      changedPointers.put(ownerUUID, pointer);
    }

    return null;
  }

  @Override
  public void updateCollectionPointer(UUID uuid, OBonsaiCollectionPointer pointer) {
  }

  @Override
  public void clearPendingCollections() {
  }

  public Map<UUID, OBonsaiCollectionPointer> changedIds() {
    return collectionPointerChanges.get();
  }

  public void clearChangedIds() {
    collectionPointerChanges.get().clear();
  }
}
