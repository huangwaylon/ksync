package karaoke;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;

import karaoke.OutputSequencer.ColorGroup;

public class ChineseSequencer {
	public static Logger log = Logger.getLogger(Main.class);

	private static final int leftAlignPadding = 10;

	private int width, height;

	private BufferedImage background, foreground, output;
	private Graphics2D backgroundGraphics, foregroundGraphics, outputGraphics;

	private static final RenderingHints renderingHints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

	public ChineseSequencer(int width, int height, Font font) {
		this.width = width;
		this.height = height;

		background = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		backgroundGraphics = background.createGraphics();
		backgroundGraphics.setRenderingHints(renderingHints);
		backgroundGraphics.setFont(font);

		foreground = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		foregroundGraphics = foreground.createGraphics();
		foregroundGraphics.setRenderingHints(renderingHints);
		foregroundGraphics.setFont(font);

		output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		outputGraphics = output.createGraphics();
		outputGraphics.setRenderingHints(renderingHints);
	}

	public BufferedImage draw(String phrase1, String phrase2, int w1, int w2, int h1, int fraction,
			ColorGroup colorGroup, boolean top) {
		// Clear foreground image.
		foregroundGraphics.setBackground(new Color(255, 255, 255, 0));
		foregroundGraphics.clearRect(0, 0, width, height);

		// Fill background image with solid color.
		backgroundGraphics.setPaint(colorGroup.getBackground());
		backgroundGraphics.fillRect(0, 0, width, height);
		backgroundGraphics.setPaint(colorGroup.getNormal());

		// Alignment.
		float y1 = height / 2;
		float y2 = (int) (y1 + 1.5 * h1);
		float x1 = leftAlignPadding;
		float x2 = width - leftAlignPadding - w2;

		// Draw the phrase for both layers.
		backgroundGraphics.drawString(phrase1, x1, y1);
		backgroundGraphics.drawString(phrase2, x2, y2);

		BufferedImage foregroundSubImage;
		if (top) {
			foregroundSubImage = foreground.getSubimage((int) x1, (int) y1 - h1, fraction, h1 * 2);
		} else {
			foregroundSubImage = foreground.getSubimage((int) x2, (int) y2 - h1, fraction, h1 * 2);
		}

		// Combine the two layers.
		outputGraphics.drawImage(background, 0, 0, null);
		outputGraphics.drawImage(foregroundSubImage, (int) x1, (int) y1 - h1, null);
		return output;
	}
}
