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
package com.orientechnologies.orient.core.iterator;

import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordAbstract;
import com.orientechnologies.orient.core.db.record.ORecordOperation;
import com.orientechnologies.orient.core.id.OClusterPosition;
import com.orientechnologies.orient.core.id.OClusterPositionFactory;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.storage.OStorage;

/**
 * Iterator class to browse forward and backward the records of a cluster. Once browsed in a direction, the iterator cannot change
 * it.
 * 
 * @author Luca Garulli
 */
public class ORecordIteratorCluster<REC extends ORecordInternal<?>> extends OIdentifiableIterator<REC> {
  private ORecord<?> currentRecord;

  public ORecordIteratorCluster(final ODatabaseRecord iDatabase, final ODatabaseRecordAbstract iLowLevelDatabase,
      final int iClusterId, final boolean iUseCache) {
    this(iDatabase, iLowLevelDatabase, iClusterId, OClusterPosition.INVALID_POSITION, OClusterPosition.INVALID_POSITION, iUseCache,
        false, OStorage.LOCKING_STRATEGY.DEFAULT);
  }

  public ORecordIteratorCluster(final ODatabaseRecord iDatabase, final ODatabaseRecordAbstract iLowLevelDatabase,
      final int iClusterId, final OClusterPosition firstClusterEntry, final OClusterPosition lastClusterEntry,
      final boolean iUseCache, final boolean iterateThroughTombstones, final OStorage.LOCKING_STRATEGY iLockingStrategy) {
    super(iDatabase, iLowLevelDatabase, iUseCache, iterateThroughTombstones, iLockingStrategy);

    if (iClusterId == ORID.CLUSTER_ID_INVALID)
      throw new IllegalArgumentException("The clusterId is invalid");

    current.clusterId = iClusterId;
    final OClusterPosition[] range = database.getStorage().getClusterDataRange(current.clusterId);

    if (firstClusterEntry.equals(OClusterPosition.INVALID_POSITION))
      this.firstClusterEntry = range[0];
    else
      this.firstClusterEntry = firstClusterEntry.compareTo(range[0]) > 0 ? firstClusterEntry : range[0];

    if (lastClusterEntry.equals(OClusterPosition.INVALID_POSITION))
      this.lastClusterEntry = range[1];
    else
      this.lastClusterEntry = lastClusterEntry.compareTo(range[1]) < 0 ? lastClusterEntry : range[1];

    totalAvailableRecords = database.countClusterElements(current.clusterId, iterateThroughTombstones);

    txEntries = iDatabase.getTransaction().getNewRecordEntriesByClusterIds(new int[] { iClusterId });

    if (txEntries != null)
      // ADJUST TOTAL ELEMENT BASED ON CURRENT TRANSACTION'S ENTRIES
      for (ORecordOperation entry : txEntries) {
        switch (entry.type) {
        case ORecordOperation.CREATED:
          totalAvailableRecords++;
          break;

        case ORecordOperation.DELETED:
          totalAvailableRecords--;
          break;
        }
      }

    begin();
  }

  @Override
  public boolean hasPrevious() {
    checkDirection(false);

    updateRangesOnLiveUpdate();

    if (currentRecord != null) {
      return true;
    }

    if (limit > -1 && browsedRecords >= limit)
      // LIMIT REACHED
      return false;

    boolean thereAreRecordsToBrowse = getCurrentEntry().compareTo(firstClusterEntry) > 0;

    if (thereAreRecordsToBrowse) {
      ORecordInternal<?> record = getRecord();
      currentRecord = readCurrentRecord(record, -1);
    }

    return currentRecord != null;
  }

  private void updateRangesOnLiveUpdate() {
    if (liveUpdated) {
      OClusterPosition[] range = database.getStorage().getClusterDataRange(current.clusterId);

      firstClusterEntry = range[0];
      lastClusterEntry = range[1];
    }
  }

  public boolean hasNext() {
    checkDirection(true);

    if (Thread.interrupted())
      // INTERRUPTED
      return false;

    updateRangesOnLiveUpdate();

    if (currentRecord != null) {
      return true;
    }

    if (limit > -1 && browsedRecords >= limit)
      // LIMIT REACHED
      return false;

    if (browsedRecords >= totalAvailableRecords)
      return false;

    if (!current.clusterPosition.isTemporary() && getCurrentEntry().compareTo(lastClusterEntry) < 0) {
      ORecordInternal<?> record = getRecord();
      currentRecord = readCurrentRecord(record, +1);
      if (currentRecord != null)
        return true;
    }

    // CHECK IN TX IF ANY
    if (txEntries != null)
      return txEntries.size() - (currentTxEntryPosition + 1) > 0;

    return false;
  }

  /**
   * Return the element at the current position and move backward the cursor to the previous position available.
   * 
   * @return the previous record found, otherwise the NoSuchElementException exception is thrown when no more records are found.
   */
  @SuppressWarnings("unchecked")
  @Override
  public REC previous() {
    checkDirection(false);

    if (currentRecord != null) {
      try {
        return (REC) currentRecord;
      } finally {
        currentRecord = null;
      }
    }
    // ITERATE UNTIL THE PREVIOUS GOOD RECORD
    while (hasPrevious()) {
      try {
        return (REC) currentRecord;
      } finally {
        currentRecord = null;
      }
    }

    return null;
  }

  /**
   * Return the element at the current position and move forward the cursor to the next position available.
   * 
   * @return the next record found, otherwise the NoSuchElementException exception is thrown when no more records are found.
   */
  @SuppressWarnings("unchecked")
  public REC next() {
    checkDirection(true);

    ORecordInternal<?> record;

    // ITERATE UNTIL THE NEXT GOOD RECORD
    while (hasNext()) {
      // FOUND
      if (currentRecord != null) {
        try {
          return (REC) currentRecord;
        } finally {
          currentRecord = null;
        }
      }

      record = getTransactionEntry();
      if (record != null)
        return (REC) record;
    }

    return null;
  }

  /**
   * Move the iterator to the begin of the range. If no range was specified move to the first record of the cluster.
   * 
   * @return The object itself
   */
  @Override
  public ORecordIteratorCluster<REC> begin() {
    updateRangesOnLiveUpdate();
    resetCurrentPosition();

    currentRecord = readCurrentRecord(getRecord(), +1);

    return this;
  }

  /**
   * Move the iterator to the end of the range. If no range was specified move to the last record of the cluster.
   * 
   * @return The object itself
   */
  @Override
  public ORecordIteratorCluster<REC> last() {
    updateRangesOnLiveUpdate();
    resetCurrentPosition();

    currentRecord = readCurrentRecord(getRecord(), -1);

    return this;
  }

  /**
   * Tell to the iterator that the upper limit must be checked at every cycle. Useful when concurrent deletes or additions change
   * the size of the cluster while you're browsing it. Default is false.
   * 
   * @param iLiveUpdated
   *          True to activate it, otherwise false (default)
   * @see #isLiveUpdated()
   */
  @Override
  public ORecordIteratorCluster<REC> setLiveUpdated(boolean iLiveUpdated) {
    super.setLiveUpdated(iLiveUpdated);

    // SET THE RANGE LIMITS
    if (iLiveUpdated) {
      firstClusterEntry = OClusterPositionFactory.INSTANCE.valueOf(0);
      lastClusterEntry = OClusterPositionFactory.INSTANCE.getMaxValue();
    } else {
      OClusterPosition[] range = database.getStorage().getClusterDataRange(current.clusterId);
      firstClusterEntry = range[0];
      lastClusterEntry = range[1];
    }

    totalAvailableRecords = database.countClusterElements(current.clusterId, isIterateThroughTombstones());

    return this;
  }
}
