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
package nl.inl.blacklab.index;

/**
 * Used to report progress while indexing, so we can give feedback to the user.
 */
public class IndexListener {
	protected long indexStartTime;

	protected long optimizeStartTime;

	protected long closeStartTime;

	protected long indexTime = 0;

	protected long optimizeTime = 0;

	protected long closeTime = 0;

	// / How many documents have been processed?
	protected int docsDone = 0;

	// / How many characters have been processed?
	protected long charsProcessed = 0;

	// / How many tokens (words) have been processed?
	protected long tokensProcessed = 0;

	// / How many files have been processed?
	protected long filesProcessed = 0;

	protected long createTime;

	protected long totalTime;

	/**
	 * Started processing a file.
	 *
	 * Synchronized to allow parallel indexing.
	 *
	 * @param name
	 *            name of the file
	 */
	public synchronized void fileStarted(String name) {
		//
	}

	public synchronized void fileDone(String name) {
		filesProcessed++;
	}

	/**
	 * Some number of characters has been processed.
	 *
	 * (this should be called regularly by the indexing class, so we can provide accurate progress
	 * reports)
	 *
	 * Synchronized to allow parallel indexing.
	 *
	 * @param charsDone
	 *            the number of characters processed since the last call
	 */
	public synchronized void charsDone(long charsDone) {
		charsProcessed += charsDone;
	}

	/**
	 * We started processing a document
	 *
	 * Synchronized to allow parallel indexing.
	 *
	 * @param name
	 *            name of the document
	 */
	public synchronized void documentStarted(String name) {
		//
	}

	/**
	 * A document was processed.
	 *
	 * Synchronized to allow parallel indexing.
	 *
	 * @param name
	 *            name of the document
	 */
	public synchronized void documentDone(String name) {
		docsDone++;
	}

	/**
	 * Get the number of files processed so far.
	 *
	 * @return the number of files processed so far
	 */
	public synchronized long getFilesProcessed() {
		return filesProcessed;
	}

	/**
	 * Get the number of characters processed so far.
	 *
	 * @return the number of characters processed so far
	 */
	public synchronized long getCharsProcessed() {
		return charsProcessed;
	}

	/**
	 * Get the number of characters processed so far.
	 *
	 * @return the number of characters processed so far
	 */
	public synchronized long getTokensProcessed() {
		return tokensProcessed;
	}

	/**
	 * Get the number of documents processed so far.
	 *
	 * @return the number of documents processed so far
	 */
	public synchronized long getDocsDone() {
		return docsDone;
	}

	/**
	 * The indexing process started
	 */
	public void indexStart() {
		indexStartTime = System.currentTimeMillis();
	}

	/**
	 * The indexing process ended
	 */
	public void indexEnd() {
		indexTime = System.currentTimeMillis() - indexStartTime;
	}

	/**
	 * The close process started
	 */
	public void closeStart() {
		closeStartTime = System.currentTimeMillis();
	}

	/**
	 * The close process ended
	 */
	public void closeEnd() {
		closeTime = System.currentTimeMillis() - closeStartTime;
	}

	public long getIndexTime() {
		return indexTime;
	}

	public long getOptimizeTime() {
		return optimizeTime;
	}

	public long getCloseTime() {
		return closeTime;
	}

	public void indexerCreated(Indexer indexer) {
		createTime = System.currentTimeMillis();
	}

	public void indexerClosed() {
		totalTime = System.currentTimeMillis() - createTime;
	}

	public long getTotalTime() {
		return totalTime;
	}

	public void luceneDocumentAdded() {
		//
	}

	public void tokensDone(int n) {
		tokensProcessed += n;
	}

}
