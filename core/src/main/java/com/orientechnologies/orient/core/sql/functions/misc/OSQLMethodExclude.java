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
package com.orientechnologies.orient.core.sql.functions.misc;

import com.orientechnologies.common.collection.OMultiValue;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.method.misc.OAbstractSQLMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Filter the content by excluding only some fields. If the content is a document, then creates a copy without the excluded fields.
 * If it's a collection of documents it acts against on each single entry.
 * 
 * <p>
 * Syntax: <blockquote>
 * 
 * <pre>
 * exclude(&lt;field|value|expression&gt; [,&lt;field-name&gt;]* )
 * </pre>
 * 
 * </blockquote>
 * 
 * <p>
 * Examples: <blockquote>
 * 
 * <pre>
 * SELECT <b>exclude(roles, 'permissions')</b> FROM OUser
 * </pre>
 * 
 * </blockquote>
 * 
 * @author Luca Garulli (l.garulli--at--orientechnologies.com)
 */

public class OSQLMethodExclude extends OAbstractSQLMethod {

  public static final String NAME = "exclude";

  public OSQLMethodExclude() {
    super(NAME, 1, -1);
  }

  private Object copy(final ODocument document, final Object[] iFieldNames) {
    final ODocument doc = document.copy();
    for (int i = 0; i < iFieldNames.length; ++i) {
      if (iFieldNames[i] != null) {
        final String fieldName = (String) iFieldNames[i].toString();
        doc.removeField(fieldName);
      }
    }
    return doc;
  }

  @Override
  public String getSyntax() {
    return "Syntax error: exclude([<field-name>][,]*)";
  }

  @Override
  public Object execute(Object iThis, OIdentifiable iCurrentRecord, OCommandContext iContext, Object ioResult, Object[] iParams) {
    if (iThis != null) {
      if (iThis instanceof ODocument) {
        // ACT ON SINGLE DOCUMENT
        return copy((ODocument) iThis, iParams);
      } else if (OMultiValue.isMultiValue(iThis)) {
        // ACT ON MULTIPLE DOCUMENTS
        final List<Object> result = new ArrayList<Object>(OMultiValue.getSize(iThis));
        for (Object o : OMultiValue.getMultiValueIterable(iThis)) {
          if (o instanceof OIdentifiable) {
            result.add(copy((ODocument) ((OIdentifiable) o).getRecord(), iParams));
          }
        }
        return result;
      }
    }

    // INVALID, RETURN NULL
    return null;
  }
}
