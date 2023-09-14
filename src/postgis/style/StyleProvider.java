package postgis.style;

import vectordb.style.Style;

public abstract class StyleProvider {

	public abstract Style getStyleByValue(int value);
	public abstract Style getStyle();
}