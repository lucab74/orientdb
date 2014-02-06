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
package com.orientechnologies.orient.graph.sql;

import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.command.OCommandRequestText;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandExecutorSQLAbstract;
import com.orientechnologies.orient.core.sql.OCommandSQLParsingException;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;

import java.util.List;
import java.util.Map;

/**
 * SQL DELETE VERTEX command.
 * 
 * @author Luca Garulli
 */
public class OCommandExecutorSQLDeleteVertex extends OCommandExecutorSQLAbstract implements OCommandResultListener {
  public static final String NAME      = "DELETE VERTEX";
  private ORecordId          rid;
  private int                removed   = 0;
  private ODatabaseRecord    database;
  private OCommandRequest    query;
  private String             returning = "COUNT";
  private List<ORecord<?>>   allDeletedRecords;

  @SuppressWarnings("unchecked")
  public OCommandExecutorSQLDeleteVertex parse(final OCommandRequest iRequest) {
    database = getDatabase();

    init((OCommandRequestText) iRequest);

    parserRequiredKeyword("DELETE");
    parserRequiredKeyword("VERTEX");

    OClass clazz = null;

    String word = parseOptionalWord(true);
    while (word != null) {

      if (word.startsWith("#")) {
        rid = new ORecordId(word);

      } else if (word.equals(KEYWORD_WHERE)) {
        if (clazz == null)
          // ASSIGN DEFAULT CLASS
          clazz = database.getMetadata().getSchema().getClass(OrientVertexType.CLASS_NAME);

        final String condition = parserGetCurrentPosition() > -1 ? " " + parserText.substring(parserGetPreviousPosition()) : "";
        query = database.command(new OSQLAsynchQuery<ODocument>("select from " + clazz.getName() + condition, this));
        break;

      } else if (word.length() > 0) {
        // GET/CHECK CLASS NAME
        clazz = database.getMetadata().getSchema().getClass(word);
        if (clazz == null)
          throw new OCommandSQLParsingException("Class '" + word + " was not found");
      }

      if (rid == null && clazz == null)
        // DELETE ALL VERTEXES
        query = database.command(new OSQLAsynchQuery<ODocument>("select from V", this));

      word = parseOptionalWord(true);
      if (parserIsEnded())
        break;
    }

    return this;
  }

  /**
   * Execute the command and return the ODocument object created.
   */
  public Object execute(final Map<Object, Object> iArgs) {
    if (rid == null && query == null)
      throw new OCommandExecutionException("Cannot execute the command because it has not been parsed yet");

    if (rid != null) {
      // REMOVE PUNCTUAL RID
      OGraphCommandExecutorSQLFactory.runInTx(new OGraphCommandExecutorSQLFactory.GraphCallBack<Object>() {
        @Override
        public Object call(OrientBaseGraph graph) {
          final OrientVertex v = graph.getVertex(rid);
          if (v != null) {
            v.remove();
            removed = 1;
          }

          return null;
        }
      });
    } else if (query != null)
      // TARGET IS A CLASS + OPTIONAL CONDITION
      query.execute(iArgs);
    else
      throw new OCommandExecutionException("Invalid target");

    return removed;
  }

  /**
   * Delete the current vertex.
   */
  public boolean result(final Object iRecord) {
    final OIdentifiable id = (OIdentifiable) iRecord;
    if (id.getIdentity().isValid()) {
      return OGraphCommandExecutorSQLFactory.runInTx(new OGraphCommandExecutorSQLFactory.GraphCallBack<Boolean>() {
        @Override
        public Boolean call(OrientBaseGraph graph) {
          final OrientVertex v = graph.getVertex(id);
          if (v != null) {
            v.remove();
            removed++;
            return true;
          }

          return false;
        }
      });
    }

    return false;
  }

  @Override
  public String getSyntax() {
    return "DELETE VERTEX <rid>|<[<class>] [WHERE <conditions>] [LIMIT <max-records>]>";
  }

  @Override
  public void end() {
  }
}
