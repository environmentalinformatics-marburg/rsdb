package postgis.style;

import vectordb.style.Style;

public class RotatingArrayStyleProvider extends StyleProvider {
	
	private final Style[] styles;	
	
	public RotatingArrayStyleProvider(Style[] styles) {
		this.styles = styles;
	}
	
	@Override
	public Style getStyleByValue(int value) {
		int styleIndex = value % styles.length;
		Style style = styles[styleIndex];
		return style;
	}

	@Override
	public Style getStyle() {
		return styles[0];
	}
}
