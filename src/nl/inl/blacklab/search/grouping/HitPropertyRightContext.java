/*******************************************************************************
 * Copyright (c) 2010, 2012 Institute for Dutch Lexicology
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
 *******************************************************************************/
package nl.inl.blacklab.search.grouping;

import java.util.Arrays;
import java.util.List;

import nl.inl.blacklab.forwardindex.Terms;
import nl.inl.blacklab.index.complex.ComplexFieldUtil;
import nl.inl.blacklab.search.Hit;
import nl.inl.blacklab.search.Searcher;

/**
 * A hit property for grouping on the context of the hit. Requires HitConcordances as input (so we
 * have the hit text available).
 */
public class HitPropertyRightContext extends HitProperty {

	private String fieldName;

	private Terms terms;

	private boolean sensitive;

	private Searcher searcher;

	public HitPropertyRightContext(Searcher searcher, String field, String property) {
		this(searcher, field, property, searcher.isDefaultSearchCaseSensitive());
	}

	public HitPropertyRightContext(Searcher searcher, String field) {
		this(searcher, field, null, searcher.isDefaultSearchCaseSensitive());
	}

	public HitPropertyRightContext(Searcher searcher) {
		this(searcher, searcher.getContentsFieldMainPropName(), searcher.isDefaultSearchCaseSensitive());
	}

	public HitPropertyRightContext(Searcher searcher, String field, String property, boolean sensitive) {
		super();
		this.searcher = searcher;
		if (property == null || property.length() == 0)
			this.fieldName = ComplexFieldUtil.mainPropertyField(searcher.getIndexStructure(), field);
		else
			this.fieldName = ComplexFieldUtil.propertyField(field, property);
		this.terms = searcher.getTerms(fieldName);
		this.sensitive = sensitive;
	}

	public HitPropertyRightContext(Searcher searcher, String field, boolean sensitive) {
		this(searcher, field, null, sensitive);
	}

	public HitPropertyRightContext(Searcher searcher, boolean sensitive) {
		this(searcher, searcher.getContentsFieldMainPropName(), sensitive);
	}

	@Override
	public HitPropValueContextWords get(Hit result) {
		if (result.context == null) {
			throw new RuntimeException("Context not available in hits objects; cannot sort/group on context");
		}

		// Copy the desired part of the context
		int n = result.context.length - result.contextRightStart;
		if (n <= 0)
			return new HitPropValueContextWords(searcher, fieldName, new int[0], sensitive);
		int[] dest = new int[n];
		int contextStart = result.contextLength * contextIndices.get(0);
		System.arraycopy(result.context, contextStart + result.contextRightStart, dest, 0, n);
		return new HitPropValueContextWords(searcher, fieldName, dest, sensitive);
	}

	@Override
	public int compare(Object oa, Object ob) {
		Hit a = (Hit) oa, b = (Hit) ob;

		// Compare the right context for these two hits
		int contextIndex = contextIndices.get(0);
		int ai = a.contextRightStart;
		int bi = b.contextRightStart;
		while (ai < a.context.length && bi < b.context.length) {
			int cmp = terms.compareSortPosition(a.context[contextIndex * a.contextLength + ai],
					b.context[contextIndex * b.contextLength + bi], sensitive);
			if (cmp != 0)
				return cmp;
			ai++;
			bi++;
		}
		// One or both ran out, and so far, they're equal.
		if (ai >= a.contextLength) {
			if (bi < b.contextLength) {
				// b longer than a => a < b
				return -1;
			}
			return 0; // same length; a == b
		}
		return 1; // a longer than b => a > b
	}

	@Override
	public List<String> needsContext() {
		return Arrays.asList(fieldName);
	}

	@Override
	public String getName() {
		return "right context";
	}

}
