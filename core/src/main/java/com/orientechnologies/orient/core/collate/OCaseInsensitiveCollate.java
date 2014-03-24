/*
 * Copyright 2010-2013 Luca Garulli (l.garulli--at--orientechnologies.com)
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
package com.orientechnologies.orient.core.collate;

import com.orientechnologies.common.collection.OCompositeKey;
import com.orientechnologies.common.comparator.ODefaultComparator;

/**
 * Case insensitive collate.
 * 
 * @author Luca Garulli (l.garulli--at--orientechnologies.com)
 * 
 */
public class OCaseInsensitiveCollate extends ODefaultComparator implements OCollate {
  public String getName() {
    return "ci";
  }

  public Object transform(final Object obj) {
    if (obj instanceof String)
      return ((String) obj).toLowerCase();
    else if (obj instanceof OCompositeKey) {
      final OCompositeKey result = new OCompositeKey();
      for (Object k : ((OCompositeKey) obj).getKeys()) {
        if (k instanceof String)
          result.addKey(((String) k).toLowerCase());
        else
          result.addKey(k);
      }
      return result;
    }
    return obj;
  }
}
