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

import java.util.ArrayList;
import java.util.List;

import com.orientechnologies.common.collection.OMultiValue;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionAbstract;

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

public class OSQLFunctionExclude extends OSQLFunctionAbstract {

  public static final String NAME = "exclude";

  public OSQLFunctionExclude() {
    super(NAME, 1, -1);
  }

  @Override
  public Object execute(Object iThis, final OIdentifiable iCurrentRecord, final Object iCurrentResult, final Object[] iFuncParams, final OCommandContext iContext) {

    if (iFuncParams[0] != null)
      if (iFuncParams[0] instanceof ODocument) {
        // ACT ON SINGLE DOCUMENT
        return copy((ODocument) iFuncParams[0], iFuncParams, 1);
      } else if (OMultiValue.isMultiValue(iFuncParams[0])) {
        // ACT ON MULTIPLE DOCUMENTS
        final List<Object> result = new ArrayList<Object>(OMultiValue.getSize(iFuncParams[0]));
        for (Object o : OMultiValue.getMultiValueIterable(iFuncParams[0]))
          if (o instanceof OIdentifiable)
            result.add(copy((ODocument) ((OIdentifiable) o).getRecord(), iFuncParams, 1));
        return result;
      }

    // INVALID, RETURN NULL
    return null;
  }

  private Object copy(final ODocument document, final Object[] iFieldNames, final int iStartIndex) {
    final ODocument doc = document.copy();
    for (int i = iStartIndex; i < iFieldNames.length; ++i) {
      if (iFieldNames[i] != null) {
        final String fieldName = (String) iFieldNames[i].toString();
        doc.removeField(fieldName);
      }
    }
    return doc;
  }

  @Override
  public String getSyntax() {
    return "Syntax error: exclude(<field|value|expression> [,<field-name>]*)";
  }
}
