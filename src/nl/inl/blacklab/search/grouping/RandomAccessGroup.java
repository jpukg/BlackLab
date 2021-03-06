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

import java.util.Iterator;
import java.util.List;

import nl.inl.blacklab.search.Hit;
import nl.inl.blacklab.search.Hits;
import nl.inl.blacklab.search.Searcher;
import nl.inl.blacklab.search.lucene.BLSpans;

import org.apache.lucene.search.spans.Spans;

/**
 * A group of results, with its group identity and the results themselves, that you can access
 * randomly (i.e. you can obtain a list of Hit objects)
 */
public class RandomAccessGroup extends Group {
	Hits results;

	public RandomAccessGroup(Searcher searcher, HitPropValue groupIdentity, String defaultConcField) {
		super(groupIdentity);
		results = new Hits(searcher, defaultConcField);
	}

	public Hits getHits() {
		return results;
	}

	public int size() {
		return results.size();
	}

	public void add(Hit result) {
		results.add(result);
	}

	@Override
	public String toString() {
		return "GroupOfHits, identity = " + groupIdentity + ", size = " + results.size();
	}

	@Deprecated
	@Override
	public Spans getSpans() {
		return new BLSpans() {
			Iterator<Hit> it = results.iterator();

			Hit currentHit = null;

			@Override
			public int doc() {
				return currentHit.doc;
			}

			@Override
			public int end() {
				return currentHit.end;
			}

			@Override
			public boolean next() {
				if (!it.hasNext())
					return false;
				currentHit = it.next();
				return true;
			}

			@Override
			public int start() {
				return currentHit.start;
			}

			@Override
			public Hit getHit() {
				return currentHit;
			}

			@Override
			public boolean hitsEndPointSorted() {
				return false;
			}

			@Override
			public boolean hitsStartPointSorted() {
				return false;
			}

			@Override
			public boolean hitsAllSameLength() {
				return false;
			}

			@Override
			public int hitsLength() {
				return -1;
			}

			@Override
			public boolean hitsHaveUniqueStart() {
				return false;
			}

			@Override
			public boolean hitsHaveUniqueEnd() {
				return false;
			}

			@Override
			public boolean hitsAreUnique() {
				return false;
			}

		};
	}

	public void setContextField(List<String> contextField) {
		 results.setContextField(contextField);
	}
}
