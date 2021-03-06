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

import nl.inl.blacklab.search.Hit;
import nl.inl.blacklab.search.Searcher;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

/**
 * A hit property for grouping on by decade based on a stored field
 * in the corresponding Lucene document containing a year.
 */
public class HitPropertyDocumentDecade extends HitProperty {
	IndexReader reader;

	String fieldName;

	public HitPropertyDocumentDecade(String fieldName, IndexReader reader) {
		this.reader = reader;
		this.fieldName = fieldName;
	}

	public HitPropertyDocumentDecade(String fieldName, Searcher searcher) {
		this(fieldName, searcher.getIndexReader());
	}

	@Override
	public HitPropValueDecade get(Hit result) {
		try {
			Document d = reader.document(result.doc);
			String strYear = d.get(fieldName);
			int year = Integer.parseInt(strYear);
			year -= year % 10;
			return new HitPropValueDecade(year);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int compare(Object a, Object b) {
		try {
			Document d = reader.document(((Hit)a).doc);
			String strYearA = d.get(fieldName);
			if (strYearA == null)
				strYearA = "";
			d = reader.document(((Hit)b).doc);
			String strYearB = d.get(fieldName);
			if (strYearB == null)
				strYearB = "";
			if (strYearA.length() == 0) // sort missing year at the end
				return strYearB.length() == 0 ? 0 : 1;
			if (strYearB.length() == 0) // sort missing year at the end
				return -1;
			int aYear = Integer.parseInt(strYearA);
			aYear -= aYear % 10;
			int bYear = Integer.parseInt(strYearB);
			bYear -= bYear % 10;

			return aYear - bYear;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

//	@Override
//	public String getHumanReadable(Hit result) {
//		try {
//			Document d = reader.document(result.doc);
//			String strYear = d.get(fieldName);
//			int year = Integer.parseInt(strYear);
//			year -= year % 10;
//			return year + "-" + (year + 9);
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
//	}

	@Override
	public String getName() {
		return "decade";
	}
}
