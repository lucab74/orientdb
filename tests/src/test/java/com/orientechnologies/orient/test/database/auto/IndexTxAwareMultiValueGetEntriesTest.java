package com.orientechnologies.orient.test.database.auto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.OClusterPosition;
import com.orientechnologies.orient.core.id.OClusterPositionFactory;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexTxAwareMultiValue;
import com.orientechnologies.orient.core.index.OSimpleKeyIndexDefinition;
import com.orientechnologies.orient.core.iterator.ORecordIteratorCluster;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;

@Test
public class IndexTxAwareMultiValueGetEntriesTest {
  private ODatabaseDocumentTx database;

  @Parameters(value = "url")
  public IndexTxAwareMultiValueGetEntriesTest(final String iURL) {
    this.database = new ODatabaseDocumentTx(iURL);
  }

  @BeforeClass
  public void beforeClass() {
    database.open("admin", "admin");
    database
        .getMetadata()
        .getIndexManager()
        .createIndex("idxTxAwareMultiValueGetEntriesTest", "NOTUNIQUE", new OSimpleKeyIndexDefinition(OType.INTEGER), null, null,
            null);
    database.close();
  }

  @BeforeMethod
  public void beforeMethod() {
    database.open("admin", "admin");
  }

  @AfterMethod
  public void afterMethod() {
    database.command(new OCommandSQL("delete from index:idxTxAwareMultiValueGetEntriesTest")).execute();
    database.close();
  }

  @Test
  public void testPut() {
    database.begin();
    final OIndex<?> index = database.getMetadata().getIndexManager().getIndex("idxTxAwareMultiValueGetEntriesTest");
    Assert.assertTrue(index instanceof OIndexTxAwareMultiValue);

    final int clusterId = database.getDefaultClusterId();

    final List<OClusterPosition> positions = getValidPositions(clusterId);

    index.put(1, new ORecordId(clusterId, positions.get(0)));
    index.put(1, new ORecordId(clusterId, positions.get(1)));

    index.put(2, new ORecordId(clusterId, positions.get(2)));
    database.commit();

    Assert.assertNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultOne = index.getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultOne.size(), 3);

    database.begin();

    index.put(2, new ORecordId(clusterId, positions.get(3)));

    Assert.assertNotNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultTwo = index.getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultTwo.size(), 4);

    database.rollback();

    Assert.assertNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultThree = index.getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultThree.size(), 3);
  }

  @Test
  public void testClear() {
    database.begin();
    final OIndex<?> index = database.getMetadata().getIndexManager().getIndex("idxTxAwareMultiValueGetEntriesTest");
    Assert.assertTrue(index instanceof OIndexTxAwareMultiValue);

    final int clusterId = database.getDefaultClusterId();
    final List<OClusterPosition> positions = getValidPositions(clusterId);

    index.put(1, new ORecordId(clusterId, positions.get(0)));
    index.put(1, new ORecordId(clusterId, positions.get(1)));

    index.put(2, new ORecordId(clusterId, positions.get(2)));

    database.commit();

    Assert.assertNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultOne = index.getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultOne.size(), 3);

    database.begin();

    index.clear();

    Assert.assertNotNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultTwo = index.getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultTwo.size(), 0);

    database.rollback();

    Assert.assertNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultThree = index.getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultThree.size(), 3);
  }

  @Test
  public void testClearAndPut() {
    database.begin();
    final OIndex<?> index = database.getMetadata().getIndexManager().getIndex("idxTxAwareMultiValueGetEntriesTest");
    Assert.assertTrue(index instanceof OIndexTxAwareMultiValue);

    final int clusterId = database.getDefaultClusterId();

    final List<OClusterPosition> positions = getValidPositions(clusterId);

    index.put(1, new ORecordId(clusterId, positions.get(0)));
    index.put(1, new ORecordId(clusterId, positions.get(1)));

    index.put(2, new ORecordId(clusterId, positions.get(2)));
    database.commit();

    Assert.assertNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultOne = index.getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultOne.size(), 3);

    database.begin();

    index.clear();
    index.put(2, new ORecordId(clusterId, OClusterPositionFactory.INSTANCE.valueOf(3)));

    Assert.assertNotNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultTwo = ((OIndexTxAwareMultiValue) index).getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultTwo.size(), 1);

    database.rollback();

    Assert.assertNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultThree = ((OIndexTxAwareMultiValue) index).getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultThree.size(), 3);
  }

  @Test
  public void testRemove() {
    database.begin();
    final OIndex<?> index = database.getMetadata().getIndexManager().getIndex("idxTxAwareMultiValueGetEntriesTest");
    Assert.assertTrue(index instanceof OIndexTxAwareMultiValue);

    final int clusterId = database.getDefaultClusterId();

    final ORecordIteratorCluster<ODocument> iteratorCluster = database.browseCluster(database.getClusterNameById(clusterId));

    final List<OClusterPosition> positions = getValidPositions(clusterId);

    index.put(1, new ORecordId(clusterId, positions.get(0)));
    index.put(1, new ORecordId(clusterId, positions.get(1)));

    index.put(2, new ORecordId(clusterId, positions.get(2)));
    database.commit();

    Assert.assertNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultOne = index.getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultOne.size(), 3);

    database.begin();

    index.remove(1);

    Assert.assertNotNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultTwo = index.getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultTwo.size(), 1);

    database.rollback();

    Assert.assertNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultThree = index.getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultThree.size(), 3);
  }

  @Test
  public void testRemoveOne() {
    database.begin();
    final OIndex<?> index = database.getMetadata().getIndexManager().getIndex("idxTxAwareMultiValueGetEntriesTest");
    Assert.assertTrue(index instanceof OIndexTxAwareMultiValue);

    final int clusterId = database.getDefaultClusterId();

    final List<OClusterPosition> positions = getValidPositions(clusterId);

    final ORecordId firstRecordId = new ORecordId(clusterId, positions.get(0));
    index.put(1, firstRecordId);
    index.put(1, new ORecordId(clusterId, positions.get(1)));

    index.put(2, new ORecordId(clusterId, positions.get(2)));
    database.commit();

    Assert.assertNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultOne = index.getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultOne.size(), 3);

    database.begin();

    index.remove(1, firstRecordId);

    Assert.assertNotNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultTwo = index.getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultTwo.size(), 2);

    database.rollback();

    Assert.assertNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    final Collection<?> resultThree = index.getEntries(Arrays.asList(1, 2));
    Assert.assertEquals(resultThree.size(), 3);
  }

  @Test
  public void testMultiPut() {
    database.begin();

    final OIndex<?> index = database.getMetadata().getIndexManager().getIndex("idxTxAwareMultiValueGetEntriesTest");
    Assert.assertTrue(index instanceof OIndexTxAwareMultiValue);

    final int clusterId = database.getDefaultClusterId();
    List<OClusterPosition> positions = getValidPositions(clusterId);

    index.put(1, new ORecordId(clusterId, positions.get(1)));
    index.put(1, new ORecordId(clusterId, positions.get(1)));
    index.put(2, new ORecordId(clusterId, positions.get(2)));

    Assert.assertNotNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    Assert.assertEquals(index.getEntries(Arrays.asList(1, 2)).size(), 2);

    database.commit();

    Assert.assertEquals(((OIndexTxAwareMultiValue) index).getEntries(Arrays.asList(1, 2)).size(), 2);
  }

  @Test
  public void testPutAfterTransaction() {
    database.begin();

    final OIndex<?> index = database.getMetadata().getIndexManager().getIndex("idxTxAwareMultiValueGetEntriesTest");
    Assert.assertTrue(index instanceof OIndexTxAwareMultiValue);

    final int clusterId = database.getDefaultClusterId();
    List<OClusterPosition> positions = getValidPositions(clusterId);

    index.put(1, new ORecordId(clusterId, positions.get(1)));
    index.put(2, new ORecordId(clusterId, positions.get(2)));

    Assert.assertNotNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    Assert.assertEquals(index.getEntries(Arrays.asList(1, 2)).size(), 2);
    database.commit();

    index.put(1, new ORecordId(clusterId, positions.get(3)));

    Assert.assertEquals(index.getEntries(Arrays.asList(1, 2)).size(), 3);
  }

  @Test
  public void testRemoveOneWithinTransaction() {
    database.begin();

    final OIndex<?> index = database.getMetadata().getIndexManager().getIndex("idxTxAwareMultiValueGetEntriesTest");
    Assert.assertTrue(index instanceof OIndexTxAwareMultiValue);

    final int clusterId = database.getDefaultClusterId();
    List<OClusterPosition> positions = getValidPositions(clusterId);

    index.put(1, new ORecordId(clusterId, positions.get(1)));
    index.put(2, new ORecordId(clusterId, positions.get(2)));

    index.remove(1, new ORecordId(clusterId, positions.get(1)));

    Assert.assertNotNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    Assert.assertEquals(index.getEntries(Arrays.asList(1, 2)).size(), 1);

    database.commit();

    Assert.assertEquals(index.getEntries(Arrays.asList(1, 2)).size(), 1);
  }

  @Test
  public void testRemoveAllWithinTransaction() {
    database.begin();

    final OIndex<?> index = database.getMetadata().getIndexManager().getIndex("idxTxAwareMultiValueGetEntriesTest");
    Assert.assertTrue(index instanceof OIndexTxAwareMultiValue);

    final int clusterId = database.getDefaultClusterId();
    List<OClusterPosition> positions = getValidPositions(clusterId);

    index.put(1, new ORecordId(clusterId, positions.get(1)));
    index.put(2, new ORecordId(clusterId, positions.get(2)));

    index.remove(1, null);

    Assert.assertNotNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    Assert.assertEquals(index.getEntries(Arrays.asList(1, 2)).size(), 1);

    database.commit();

    Assert.assertEquals(index.getEntries(Arrays.asList(1, 2)).size(), 1);
  }

  @Test
  public void testPutAfterRemove() {
    database.begin();

    final OIndex<?> index = database.getMetadata().getIndexManager().getIndex("idxTxAwareMultiValueGetEntriesTest");
    Assert.assertTrue(index instanceof OIndexTxAwareMultiValue);

    final int clusterId = database.getDefaultClusterId();
    List<OClusterPosition> positions = getValidPositions(clusterId);
    index.put(1, new ORecordId(clusterId, positions.get(1)));
    index.put(2, new ORecordId(clusterId, positions.get(2)));

    index.remove(1, new ORecordId(clusterId, positions.get(1)));
    index.put(1, new ORecordId(clusterId, positions.get(1)));

    Assert.assertNotNull(database.getTransaction().getIndexChanges("idxTxAwareMultiValueGetEntriesTest"));
    Assert.assertEquals(index.getEntries(Arrays.asList(1, 2)).size(), 2);

    database.commit();

    Assert.assertEquals(index.getEntries(Arrays.asList(1, 2)).size(), 2);
  }

  private List<OClusterPosition> getValidPositions(int clusterId) {
    final List<OClusterPosition> positions = new ArrayList<OClusterPosition>();

    final ORecordIteratorCluster<?> iteratorCluster = database.browseCluster(database.getClusterNameById(clusterId));

    for (int i = 0; i < 7; i++) {
      iteratorCluster.hasNext();
      ORecord doc = iteratorCluster.next();
      positions.add(doc.getIdentity().getClusterPosition());
    }
    return positions;
  }

}
