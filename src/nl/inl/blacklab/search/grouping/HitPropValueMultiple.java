package nl.inl.blacklab.search.grouping;

import nl.inl.blacklab.search.Searcher;
import nl.inl.util.ArrayUtil;

public class HitPropValueMultiple extends HitPropValue {
	HitPropValue[] value;

	public HitPropValueMultiple(HitPropValue[] value) {
		this.value = value;
	}

	@Override
	public int compareTo(Object o) {
		return ArrayUtil.compareArrays(value, ((HitPropValueMultiple)o).value);
	}

	@Override
	public int hashCode() {
		int result = 0;
		for (HitPropValue v: value) {
			result ^= v.hashCode();
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (HitPropValue v: value) {
			if (b.length() > 0)
				b.append(" / ");
			b.append(v.toString());
		}
		return b.toString();
	}

	public static HitPropValueMultiple deserialize(Searcher searcher, String info) {
		String[] strValues = info.split(";;");
		HitPropValue[] values = new HitPropValue[strValues.length];
		int i = 0;
		for (String strValue: strValues) {
			values[i] = HitPropValue.deserialize(searcher, strValue);
			i++;
		}
		return new HitPropValueMultiple(values);
	}

	@Override
	public String serialize() {
		StringBuilder b = new StringBuilder();
		for (HitPropValue v: value) {
			if (b.length() > 0)
				b.append(";;"); // different separator than HitPropValueContext*!
			b.append(v.serialize());
		}
		return "mul:" + b.toString();
	}
}
