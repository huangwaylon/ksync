package karaoke;

import java.awt.Color;

public class ColorGroup {
	private Color normal = Color.white;
	private Color outline = Color.black;
	private Color highlight = Color.blue;
	private Color highlightOutline = Color.white;
	private Color background = Color.green;

	public void setNormal(Color normal) {
		this.normal = normal;
	}

	public void setOutline(Color outline) {
		this.outline = outline;
	}

	public void setHighlight(Color highlight) {
		this.highlight = highlight;
	}

	public void setHighlightOutline(Color highlightOutline) {
		this.highlightOutline = highlightOutline;
	}

	public void setBackground(Color background) {
		this.background = background;
	}

	public Color getNormal() {
		return normal;
	}

	public Color getOutline() {
		return outline;
	}

	public Color getHighlight() {
		return highlight;
	}

	public Color getHighlightOutline() {
		return highlightOutline;
	}

	public Color getBackground() {
		return background;
	}
}
