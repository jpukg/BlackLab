package nl.inl.blacklab.search.grouping;


public class HitPropValueInt extends HitPropValue {
	int value;

	public HitPropValueInt(int value) {
		this.value = value;
	}

	@Override
	public int compareTo(Object o) {
		return value - ((HitPropValueInt)o).value;
	}

	@Override
	public int hashCode() {
		return ((Integer)value).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return value == ((HitPropValueInt)obj).value;
	}

	@Override
	public String toString() {
		return value + "";
	}

	public static HitPropValue deserialize(String info) {
		return new HitPropValueInt(Integer.parseInt(info));
	}

	@Override
	public String serialize() {
		return "int:" + value;
	}

}
