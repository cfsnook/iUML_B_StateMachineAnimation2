/**
 * Copyright (c) 2010 University of Southampton.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package ac.soton.eventb.statemachines.animation2.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.prob.core.domainobjects.Variable;

/**
 * @author vitaly
 *
 */
public class TranslationUtil {

	private static final Object MATH_EMPTY_SET = "\u2205";
	private static final String MATH_MAPLET = "\u21a6";
	
	/**
	 * Translate a variable that represents a set of simple elements v.2 (ProB core 6.0 with EventB math symbols).
	 * 
	 * @param var ProB variable representing a set of elements
	 * @return set of elements
	 */
	public static Set<String> translateElementSet(Variable var) {
		if (var != null) {
			String value = var.getValue();
			if (!value.equals(MATH_EMPTY_SET)) {
				value = value.substring(1, value.length() - 1);		// remove "{" and "}"
				if (value.length() > 0) {
					return new HashSet<String>(Arrays.asList(value.split(",")));
				}
			}
		}
		return Collections.emptySet();
	}
	
	/**
	 * Translate a variable that represents a set of tuples v.2 (ProB core 6.0 with EventB math symbols).
	 * 
	 * @param var ProB variable representing a set of key to value tuples
	 * @return map of keys to set of values
	 */
	public static Map<String, Set<String>> translateFunctionSet(Variable var) {
		if (var != null) {
			String value = var.getValue();
			if (!value.equals(MATH_EMPTY_SET)) {
				value = value.substring(1, value.length() - 1);			// remove "{" and "}"
				if (value.length() > 0) {
					Map<String,Set<String>> keyToValuesMap = 
						new HashMap<String,Set<String>>();
					String[] tuples = value.split(",");					// get tuples (key|->value)
					for (int n = 0; n < tuples.length; ++n) {
						String tuple = tuples[n];
						tuple = tuple.substring(1, tuple.length() - 1); 	// remove "(" and ")"
						String[] el = tuple.split(MATH_MAPLET); 			// split tuple elements
						
						// add a key and its values
						if (!keyToValuesMap.containsKey(el[1]))
							keyToValuesMap.put(el[1],new HashSet<String>());
						keyToValuesMap.get(el[1]).add(el[0]);
					}
					return keyToValuesMap;
				}
			}
		}
		return Collections.emptyMap();
	}
}
