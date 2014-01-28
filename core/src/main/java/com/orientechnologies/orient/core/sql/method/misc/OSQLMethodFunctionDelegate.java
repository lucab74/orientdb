/*
 * Copyright 2013 Orient Technologies.
 * Copyright 2013 Geomatys.
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
package com.orientechnologies.orient.core.sql.method.misc;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.sql.functions.OSQLFunction;
import com.orientechnologies.orient.core.sql.functions.OSQLFunctionRuntime;

/**
 * Delegates the execution to a function.
 * 
 * @author Luca Garulli
 */
public class OSQLMethodFunctionDelegate extends OAbstractSQLMethod {

  public static final String  NAME = "function";
  private OSQLFunctionRuntime func;

  public OSQLMethodFunctionDelegate(final OSQLFunction f) {
    super(NAME);
    func = new OSQLFunctionRuntime(f);
  }

  @Override
  public int getMinParams() {
    final int min = func.getFunction().getMinParams();
    return min == -1 ? -1 : min - 1;
  }

  @Override
  public int getMaxParams() {
    final int max = func.getFunction().getMaxParams();
    return max == -1 ? -1 : max - 1;
  }

  @Override
  public Object execute(final Object iThis, final OIdentifiable iCurrentRecord, final OCommandContext iContext,
      final Object ioResult, final Object[] iMethodParams) {

    final Object[] newParams;
    if (iMethodParams != null) {
      newParams = new Object[iMethodParams.length + 1];
      newParams[0] = iThis;
      System.arraycopy(iMethodParams, 0, newParams, 1, iMethodParams.length);
    } else
      newParams = new Object[] { iThis };

    func.setParameters(newParams, false);

    return func.execute(iThis, iCurrentRecord, ioResult, iContext);
  }

  @Override
  public String toString() {
    return "function " + func;
  }
}
