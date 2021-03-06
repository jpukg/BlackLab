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
package nl.inl.blacklab.perdocument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import nl.inl.blacklab.search.Hit;
import nl.inl.blacklab.search.Hits;
import nl.inl.blacklab.search.Searcher;
import nl.inl.util.ReverseComparator;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.spans.SpanQuery;

/**
 * A list of DocResult objects (document-level query results). The list may be sorted by calling
 * DocResults.sort().
 */
public class DocResults implements Iterable<DocResult> {
	/**
	 * (Part of) our document results
	 */
	protected List<DocResult> results = new ArrayList<DocResult>();

	/**
	 * Our searcher object
	 */
	private Searcher searcher;

	/**
	 * Our source hits object
	 */
	private Hits sourceHits;

	/**
	 * Iterator in our source hits object
	 */
	private Iterator<Hit> sourceHitsIterator;

	/**
	 * A partial DocResult, because we stopped iterating through the Hits.
	 * Pick this up when we continue iterating through it.
	 */
	private DocResult partialDocResult;

	public Searcher getSearcher() {
		return searcher;
	}

	public void add(DocResult r) {
		try {
			ensureAllResultsRead();
		} catch (InterruptedException e) {
			// Thread was interrupted; don't complete the operation but return
			// and let the caller detect and deal with the interruption.
			return;
		}
		results.add(r);
	}

	boolean sourceHitsFullyRead() {
		return sourceHits == null || !sourceHitsIterator.hasNext();
	}

	public DocResults(Searcher searcher, Hits hits) {
		this.searcher = searcher;
		try {
			sourceHits = hits;
			sourceHitsIterator = hits.iterator();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public DocResults(Searcher searcher, String field, SpanQuery query) {
		this(searcher, new Hits(searcher, field, query));
	}

	public DocResults(Searcher searcher, Scorer sc) {
		this.searcher = searcher;
		if (sc == null)
			return; // no matches, empty result set
		try {
			IndexReader indexReader = searcher.getIndexReader();
			while (true) {
				int docId;
				try {
					docId = sc.nextDoc();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				if (docId == DocIdSetIterator.NO_MORE_DOCS)
					break;

				Document d = indexReader.document(docId);
				DocResult dr = new DocResult(searcher, null, docId, d, sc.score());
				results.add(dr);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public DocResults(Searcher searcher, Query query) {
		this(searcher, searcher.findDocScores(query));
	}

	public DocResults(Searcher searcher) {
		this.searcher = searcher;
	}

	/**
	 * Get the list of results
	 * @return the list of results
	 * @deprecated Breaks optimizations. Use iterator or subList() instead.
	 */
	@Deprecated
	public List<DocResult> getResults() {
		try {
			ensureAllResultsRead();
		} catch (InterruptedException e) {
			// Thread was interrupted; don't complete the operation but return
			// the results we have.
			// Let caller detect and deal with interruption.
		}
		return results;
	}

	/**
	 * Sort the results using the given comparator.
	 *
	 * @param comparator
	 *            how to sort the results
	 */
	void sort(Comparator<DocResult> comparator) {
		try {
			ensureAllResultsRead();
		} catch (InterruptedException e) {
			// Thread was interrupted; just sort the results we have.
			// Let caller detect and deal with interruption.
		}
		Collections.sort(results, comparator);
	}

	/**
	 * Determines if there are at least a certain number of results
	 *
	 * This may be used if we don't want to process all results (which
	 * may be a lot) but we do need to know something about the size
	 * of the result set (such as for paging).
	 *
	 * @param lowerBound the number we're testing against
	 *
	 * @return true if the size of this set is at least lowerBound, false otherwise.
	 */
	public boolean sizeAtLeast(int lowerBound) {
		try {
			// Try to fetch at least this many hits
			ensureResultsRead(lowerBound);
		} catch (InterruptedException e) {
			// Thread was interrupted; abort operation
			// and let client decide what to do
		}

		return results.size() >= lowerBound;
	}

	/**
	 * Get the number of documents in this results set.
	 *
	 * Note that this returns the number of document results available;
	 * if there were so many hits that not all were retrieved (call
	 * maxHitsRetrieved()), you can find the grand total of documents
	 * by calling totalSize().
	 *
	 * @return the number of documents.
	 */
	public int size() {
		// Make sure we've collected all results and return the size of our result list.
		try {
			ensureAllResultsRead();
		} catch (InterruptedException e) {
			// Thread was interrupted; return size of the results we have.
			// Let caller detect and deal with interruption.
		}
		return results.size();
	}

	/**
	 * Get the total number of documents.
	 * This even counts documents that weren't retrieved because the
	 * set of hits was too large.
	 *
	 * @return the total number of documents.
	 */
	public int totalSize() {
		if (sourceHits == null)
			return size(); // no hits, just documents
		return sourceHits.totalNumberOfDocs();
	}

	/**
	 * Sort documents based on a document property.
	 * @param prop the property to sort on
	 * @param sortReverse true iff we want to sort in reverse.
	 */
	public void sort(DocProperty prop, boolean sortReverse) {
		Comparator<DocResult> comparator = new ComparatorDocProperty(prop);
		if (sortReverse) {
			comparator = new ReverseComparator<DocResult>(comparator);
		}
		sort(comparator);
	}

	/**
	 * Retrieve a sublist of hits.
	 * @param fromIndex first hit to include in the resulting list
	 * @param toIndex first hit not to include in the resulting list
	 * @return the sublist
	 */
	public List<DocResult> subList(int fromIndex, int toIndex) {
		try {
			ensureResultsRead(toIndex - 1);
		} catch (InterruptedException e) {
			// Thread was interrupted. We may not even have read
			// the first result in the sublist, so just return an empty list.
			return Collections.emptyList();
		}
		return results.subList(fromIndex, toIndex);
	}

	/**
	 * If we still have only partially read our Hits object,
	 * read the rest of it and add all the hits.
	 * @throws InterruptedException
	 */
	private void ensureAllResultsRead() throws InterruptedException {
		ensureResultsRead(-1);
	}

	/**
	 * If we still have only partially read our Hits object,
	 * read some more of it and add the hits.
	 *
	 * @param index the number of results we want to ensure have been read, or negative for all results
	 * @throws InterruptedException
	 */
	void ensureResultsRead(int index) throws InterruptedException {
		if (sourceHitsFullyRead())
			return;

		try {
			// Fill list of document results
			int doc = partialDocResult == null ? -1 : partialDocResult.getDocId();
			DocResult dr = partialDocResult;
			partialDocResult = null;

			@SuppressWarnings("resource")
			IndexReader indexReader = searcher == null ? null : searcher.getIndexReader();
			Thread currentThread = Thread.currentThread();
			while ( (index < 0 || results.size() <= index) && sourceHitsIterator.hasNext()) {

				if (currentThread.isInterrupted())
					throw new InterruptedException("Thread was interrupted while gathering hits");

				Hit hit = sourceHitsIterator.next();
				if (hit.doc != doc) {
					if (dr != null)
						results.add(dr);
					doc = hit.doc;
					dr = new DocResult(searcher, sourceHits.getConcordanceFieldName(), hit.doc,
							indexReader == null ? null : indexReader.document(hit.doc));
					dr.setContextField(sourceHits.getContextFieldPropName()); // make sure we remember what kind of
													// context we have, if any
				}
				dr.addHit(hit);
			}
			// add the final dr instance to the results collection
			if (dr != null) {
				if (sourceHitsIterator.hasNext())
					partialDocResult = dr; // not done, continue from here later
				else
					results.add(dr); // done
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Were all hits retrieved, or did we stop because there were too many?
	 * @return true if all hits were retrieved
	 * @deprecated renamed to maxHitsRetrieved()
	 */
	@Deprecated
	public boolean tooManyHits() {
		return maxHitsRetrieved();
	}

	/**
	 * Did we stop retrieving hits because we reached the maximum?
	 * @return true if we reached the maximum and stopped retrieving hits
	 */
	public boolean maxHitsRetrieved() {
		if (sourceHits == null)
			return false; // no hits, only docs
		return sourceHits.maxHitsRetrieved();
	}

	/**
	 * Did we stop counting hits because we reached the maximum?
	 * @return true if we reached the maximum and stopped counting hits
	 */
	public boolean maxHitsCounted() {
		if (sourceHits == null)
			return false; // no hits, only docs
		return sourceHits.maxHitsCounted();
	}

	/**
	 * Return an iterator over these hits.
	 *
	 * @return the iterator
	 */
	@Override
	public Iterator<DocResult> iterator() {
		// Construct a custom iterator that iterates over the hits in the hits
		// list, but can also take into account the Spans object that may not have
		// been fully read. This ensures we don't instantiate Hit objects for all hits
		// if we just want to display the first few.
		return new Iterator<DocResult>() {

			int index = -1;

			@Override
			public boolean hasNext() {
				// Do we still have hits in the hits list?
				try {
					ensureResultsRead(index + 1);
				} catch (InterruptedException e) {
					// Thread was interrupted. Act like we're done.
					// Let caller detect and deal with interruption.
					return false;
				}
				return index + 1 < results.size();
			}

			@Override
			public DocResult next() {
				// Check if there is a next, taking unread hits from Spans into account
				if (hasNext()) {
					index++;
					return results.get(index);
				}
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}

	public DocResult get(int i) {
		try {
			ensureResultsRead(i);
		} catch (InterruptedException e) {
			// Thread was interrupted. Required hit hasn't been gathered;
			// we will just return null.
		}
		if (i >= results.size())
			return null;
		return results.get(i);
	}

}
