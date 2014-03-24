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
package com.orientechnologies.orient.core.storage.impl.memory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.orientechnologies.common.concur.lock.OModificationLock;
import com.orientechnologies.orient.core.id.OClusterPosition;
import com.orientechnologies.orient.core.id.OClusterPositionFactory;
import com.orientechnologies.orient.core.storage.OCluster;
import com.orientechnologies.orient.core.storage.OPhysicalPosition;
import com.orientechnologies.orient.core.storage.ORawBuffer;
import com.orientechnologies.orient.core.version.ORecordVersion;

public class OClusterMemoryArrayList extends OClusterMemory implements OCluster {

  private List<OPhysicalPosition> entries = new ArrayList<OPhysicalPosition>();
  private List<OPhysicalPosition> removed = new ArrayList<OPhysicalPosition>();

  protected void clear() {
    entries.clear();
    removed.clear();
  }

  public long getEntries() {
    acquireSharedLock();
    try {

      return entries.size() - removed.size();

    } finally {
      releaseSharedLock();
    }
  }

  public boolean isHashBased() {
    return false;
  }

  @Override
  public OModificationLock getExternalModificationLock() {
    throw new UnsupportedOperationException("getExternalModificationLock");
  }

  @Override
  public OPhysicalPosition createRecord(byte[] content, ORecordVersion recordVersion, byte recordType) throws IOException {
    throw new UnsupportedOperationException("createRecord");
  }

  @Override
  public boolean deleteRecord(OClusterPosition clusterPosition) throws IOException {
    throw new UnsupportedOperationException("deleteRecord");
  }

  @Override
  public void updateRecord(OClusterPosition clusterPosition, byte[] content, ORecordVersion recordVersion, byte recordType)
      throws IOException {
    throw new UnsupportedOperationException("updateRecord");
  }

  @Override
  public ORawBuffer readRecord(OClusterPosition clusterPosition) throws IOException {
    throw new UnsupportedOperationException("readRecord");
  }

  @Override
  public boolean exists() {
    throw new UnsupportedOperationException("exists");
  }

  public long getRecordsSize() {
    acquireSharedLock();
    try {

      long size = 0;
      for (OPhysicalPosition e : entries)
        if (e != null)
          size += e.recordSize;
      return size;

    } finally {
      releaseSharedLock();
    }
  }

  @Override
  public OClusterPosition getFirstPosition() {
    acquireSharedLock();
    try {
      if (entries.isEmpty())
        return OClusterPositionFactory.INSTANCE.valueOf(-1);

      int index = 0;
      while (index < entries.size() && entries.get(index) == null)
        index++;

      return OClusterPositionFactory.INSTANCE.valueOf(index);
    } finally {
      releaseSharedLock();
    }
  }

  @Override
  public OClusterPosition getLastPosition() {
    acquireSharedLock();
    try {
      int index = entries.size() - 1;
      while (index >= 0 && entries.get(index) == null)
        index--;

      return OClusterPositionFactory.INSTANCE.valueOf(index);
    } finally {
      releaseSharedLock();
    }
  }

  public boolean addPhysicalPosition(final OPhysicalPosition iPPosition) {
    acquireExclusiveLock();
    try {

      if (!removed.isEmpty()) {
        final OPhysicalPosition recycledPosition = removed.remove(removed.size() - 1);

        // OVERWRITE DATA
        iPPosition.clusterPosition = recycledPosition.clusterPosition;
        iPPosition.recordVersion = recycledPosition.recordVersion.copy();
        if (iPPosition.recordVersion.isTombstone())
          iPPosition.recordVersion.revive();

        iPPosition.recordVersion.increment();

        int positionToStore = recycledPosition.clusterPosition.intValue();

        entries.set(positionToStore, iPPosition);

      } else {
        iPPosition.clusterPosition = allocateRecord(iPPosition);
        iPPosition.recordVersion.reset();
        entries.add(iPPosition);
      }

    } finally {
      releaseExclusiveLock();
    }

    return true;
  }

  protected OClusterPosition allocateRecord(final OPhysicalPosition iPPosition) {
    return OClusterPositionFactory.INSTANCE.valueOf(entries.size());
  }

  public void updateRecordType(final OClusterPosition iPosition, final byte iRecordType) throws IOException {
    acquireExclusiveLock();
    try {

      entries.get(iPosition.intValue()).recordType = iRecordType;

    } finally {
      releaseExclusiveLock();
    }
  }

  public void updateVersion(OClusterPosition iPosition, ORecordVersion iVersion) throws IOException {
    acquireExclusiveLock();
    try {

      entries.get(iPosition.intValue()).recordVersion = iVersion;

    } finally {
      releaseExclusiveLock();
    }
  }

  @Override
  public void convertToTombstone(OClusterPosition iPosition) throws IOException {
    throw new UnsupportedOperationException("convertToTombstone");
  }

  @Override
  public long getTombstonesCount() {
    return 0;
  }

  @Override
  public boolean hasTombstonesSupport() {
    return false;
  }

  public OPhysicalPosition getPhysicalPosition(final OPhysicalPosition iPPosition) {
    acquireSharedLock();
    try {
      if (iPPosition.clusterPosition.intValue() < 0 || iPPosition.clusterPosition.compareTo(getLastPosition()) > 0)
        return null;

      return entries.get(iPPosition.clusterPosition.intValue());

    } finally {
      releaseSharedLock();
    }
  }

  public void removePhysicalPosition(final OClusterPosition iPosition) {
    acquireExclusiveLock();
    try {

      int positionToRemove = iPosition.intValue();
      final OPhysicalPosition ppos = entries.get(positionToRemove);

      // ADD AS HOLE
      removed.add(ppos);

      entries.set(positionToRemove, null);
    } finally {
      releaseExclusiveLock();
    }
  }

  public void updateDataSegmentPosition(final OClusterPosition iPosition, final int iDataSegmentId, final long iDataPosition) {
    acquireExclusiveLock();
    try {

      final OPhysicalPosition ppos = entries.get(iPosition.intValue());
      ppos.dataSegmentId = iDataSegmentId;
      ppos.dataSegmentPos = iDataPosition;

    } finally {
      releaseExclusiveLock();
    }
  }

  @Override
  public OPhysicalPosition[] higherPositions(OPhysicalPosition position) {
    int positionInEntries = position.clusterPosition.intValue() + 1;
    while (positionInEntries < entries.size() && (positionInEntries < 0 || entries.get(positionInEntries) == null)) {
      positionInEntries++;
    }

    if (positionInEntries >= 0 && positionInEntries < entries.size()) {
      return new OPhysicalPosition[] { entries.get(positionInEntries) };
    } else {
      return new OPhysicalPosition[0];
    }
  }

  @Override
  public OPhysicalPosition[] ceilingPositions(OPhysicalPosition position) throws IOException {
    int positionInEntries = position.clusterPosition.intValue();
    while (positionInEntries < entries.size() && (positionInEntries < 0 || entries.get(positionInEntries) == null)) {
      positionInEntries++;
    }

    if (positionInEntries >= 0 && positionInEntries < entries.size()) {
      return new OPhysicalPosition[] { entries.get(positionInEntries) };
    } else {
      return new OPhysicalPosition[0];
    }
  }

  @Override
  public OPhysicalPosition[] lowerPositions(OPhysicalPosition position) {
    int positionInEntries = position.clusterPosition.intValue() - 1;
    while (positionInEntries >= 0 && entries.get(positionInEntries) == null) {
      positionInEntries--;
    }
    if (positionInEntries >= 0 && positionInEntries < entries.size())
      return new OPhysicalPosition[] { entries.get(positionInEntries) };
    else
      return new OPhysicalPosition[0];
  }

  @Override
  public OPhysicalPosition[] floorPositions(OPhysicalPosition position) throws IOException {
    int positionInEntries = position.clusterPosition.intValue();
    while (positionInEntries >= 0 && entries.get(positionInEntries) == null) {
      positionInEntries--;
    }
    if (positionInEntries >= 0 && positionInEntries < entries.size())
      return new OPhysicalPosition[] { entries.get(positionInEntries) };
    else
      return new OPhysicalPosition[0];
  }

  @Override
  public String toString() {
    return "OClusterMemory [name=" + getName() + ", id=" + getId() + ", entries=" + entries.size() + ", removed=" + removed + "]";
  }
}
