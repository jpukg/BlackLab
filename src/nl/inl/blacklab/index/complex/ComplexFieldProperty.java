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
package nl.inl.blacklab.index.complex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.TermVector;

/**
 * A property in a complex field. See ComplexField for details.
 */
public class ComplexFieldProperty {

	/** How a property is to be indexed with respect to case and diacritics sensitivity. */
	public enum SensitivitySetting {
		ONLY_SENSITIVE,                 // only index case- and diacritics-sensitively
		ONLY_INSENSITIVE,               // only index case- and diacritics-insensitively
		SENSITIVE_AND_INSENSITIVE,      // case+diac sensitive as well as case+diac insensitive
		CASE_AND_DIACRITICS_SEPARATE	// all four combinations (sens, insens, case-insens, diac-insens)
	}

	/**
	 * Add a value to the property.
	 * @param value value to add
	 */
	final public void addValue(String value) {
		addValue(value, 1);
	}

	protected boolean includeOffsets;

	/**
	 *  Term values for this property.
	 */
	protected List<String> values = new ArrayList<String>();

	/** Token position increments. This allows us to index multiple terms at a single token position (just
	 *  set the token increments of the additional tokens to 0). */
	protected List<Integer> increments = new ArrayList<Integer>();

	/** Position of the last value added
	 */
	protected int position = -1;

	/**
	 * A property may be indexed in different ways (alternatives). This specifies names and filters
	 * for each way.
	 */
	private Map<String, TokenFilterAdder> alternatives = new HashMap<String, TokenFilterAdder>();

	/** The main alternative (the one that gets character offsets if desired) */
	private String mainAlternative;

	/** The property name */
	private String propName;

	/** Does this property get its own forward index? */
	private boolean hasForwardIndex = true;

	/** To keep memory usage down, we make sure we only store 1 copy of each string value */
	private Map<String, String> storedValues = new HashMap<String, String>();

	/**
	 * Construct a ComplexFieldProperty object with the default alternative
	 * @param name property name
	 * @param includeOffsets whether to include character offsets in the main alternative
	 * @deprecated Use constructor with SensitivitySetting parameter
	 */
	@Deprecated
	public ComplexFieldProperty(String name, boolean includeOffsets) {
		this(name, (TokenFilterAdder)null, includeOffsets);
	}

	/**
	 * Construct a ComplexFieldProperty object with the default alternative
	 * @param name property name
	 * @param filterAdder what filter(s) to add, or null if none
	 * @param includeOffsets whether to include character offsets in the main alternative
	 * @deprecated Use constructor with SensitivitySetting parameter
	 */
	@Deprecated
	public ComplexFieldProperty(String name, TokenFilterAdder filterAdder,
			boolean includeOffsets) {
		super();
		propName = name;
		alternatives.put(ComplexFieldUtil.getDefaultMainAlternativeName(), filterAdder);
		this.includeOffsets = includeOffsets;
	}

	/**
	 * Construct a ComplexFieldProperty object with the default alternative
	 * @param name property name
	 * @param sensitivity ways to index this property, with respect to case- and
	 *   diacritics-sensitivity.
	 * @param includeOffsets whether to include character offsets in the main alternative
	 */
	public ComplexFieldProperty(String name, SensitivitySetting sensitivity,
			boolean includeOffsets) {
		super();
		propName = name;

		mainAlternative = null;
		if (sensitivity != SensitivitySetting.ONLY_INSENSITIVE) {
			// Add sensitive alternative
			alternatives.put(ComplexFieldUtil.SENSITIVE_ALT_NAME, null);
			mainAlternative = ComplexFieldUtil.SENSITIVE_ALT_NAME;
		}
		if (sensitivity != SensitivitySetting.ONLY_SENSITIVE) {
			// Add insensitive alternative
			alternatives.put(ComplexFieldUtil.INSENSITIVE_ALT_NAME, new DesensitizerAdder(true, true));
			if (mainAlternative == null)
				mainAlternative = ComplexFieldUtil.INSENSITIVE_ALT_NAME;
		}
		if (sensitivity == SensitivitySetting.CASE_AND_DIACRITICS_SEPARATE) {
			// Add case-insensitive and diacritics-insensitive alternatives
			alternatives.put(ComplexFieldUtil.CASE_INSENSITIVE_ALT_NAME, new DesensitizerAdder(true, false));
			alternatives.put(ComplexFieldUtil.DIACRITICS_INSENSITIVE_ALT_NAME, new DesensitizerAdder(false, true));
		}

		this.includeOffsets = includeOffsets;
	}

	TokenStream getTokenStream(String altName, List<Integer> startChars, List<Integer> endChars) {
		TokenStream ts;
		if (includeOffsets)
			ts = new TokenStreamWithOffsets(values, increments, startChars, endChars);
		else
			ts = new TokenStreamFromList(values, increments);
		TokenFilterAdder filterAdder = alternatives.get(altName);
		if (filterAdder != null)
			return filterAdder.addFilters(ts);
		return ts;
	}

	TermVector getTermVectorOption(String altName) {
		if (includeOffsets && altName.equals(mainAlternative)) {
			// Main alternative of a property may get character offsets
			// (if it's the main property of a complex field)
			return TermVector.WITH_POSITIONS_OFFSETS;
		}

		// Named alternatives and additional properties don't get character offsets
		return TermVector.WITH_POSITIONS;
	}

	public void addToLuceneDoc(Document doc, String fieldName, List<Integer> startChars,
			List<Integer> endChars) {
		for (String altName : alternatives.keySet()) {
			doc.add(new Field(ComplexFieldUtil.propertyField(fieldName, propName, altName),
					getTokenStream(altName, startChars, endChars), getTermVectorOption(altName)));
		}
	}

	public void addAlternative(String altName, TokenFilterAdder filterAdder) {
		alternatives.put(altName, filterAdder);
	}

	public List<String> getValues() {
		return Collections.unmodifiableList(values);
	}

	public List<Integer> getPositionIncrements() {
		return Collections.unmodifiableList(increments);
	}

	public int lastValuePosition() {
		return position;
	}

	public String getName() {
		return propName;
	}

	public boolean hasForwardIndex() {
		return hasForwardIndex;
	}

	public void setForwardIndex(boolean b) {
		hasForwardIndex = b;
	}

	public void addValue(String value, int increment) {
		// Make sure we don't keep duplicates of strings in memory, but re-use earlier instances.
		String storedValue = storedValues.get(value);
		if (storedValue == null) {
			storedValues.put(value, value);
			storedValue = value;
		}

		// Special case: if previous value was the empty string and position increment is 0,
		// replace the previous value. This is convenient to keep all the properties synched
		// up while indexing (by adding a dummy empty string if we don't have a value for a
		// property), while still being able to add a value to this position later (for example,
		// when we encounter an XML close tag.
		int lastIndex = values.size() - 1;
		if (lastIndex >= 0 && values.get(lastIndex).length() == 0 && increment == 0) {
			// Change the last value but don't change the increment.
			values.set(lastIndex, storedValue);
			return;
		}

		values.add(storedValue);
		increments.add(increment);
		position += increment; // keep track of position of last token

	}

	public void clear() {
		values.clear();
		increments.clear();
		position = -1;

		// In theory, we don't need to clear the cached values between documents, but
		// for large data sets, this would keep getting larger and larger, so we do
		// it anyway.
		storedValues.clear();
	}
}
