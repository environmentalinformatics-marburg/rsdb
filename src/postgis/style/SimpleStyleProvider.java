package postgis.style;

import vectordb.style.Style;

public class SimpleStyleProvider extends StyleProvider {
	
	private final Style style;	
	
	public SimpleStyleProvider(Style style) {
		this.style = style;
	}
	
	@Override
	public Style getStyleByValue(int value) {
		return style;
	}

	@Override
	public Style getStyle() {
		return style;
	}
}
