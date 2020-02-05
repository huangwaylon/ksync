package karaoke;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;

import org.apache.log4j.Logger;

import karaoke.OutputSequencer.ImageGenerator;

public class ChineseSequencer implements ImageGenerator {
	public static Logger log = Logger.getLogger(Main.class);

	public static int leftAlignPadding = 150;

	private int width, height;

	private BufferedImage background, foreground, output;
	private Graphics2D backgroundGraphics, foregroundGraphics, outputGraphics;

	private static final RenderingHints renderingHints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

	private FontMetrics fontMetrics;

	private boolean top = true;
	private String phrase = null;

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

		fontMetrics = backgroundGraphics.getFontMetrics();
	}

	@Override
	public BufferedImage draw(String phrase1, String phrase2, int fraction, ColorGroup colorGroup) {
		// Clear foreground image.
		foregroundGraphics.setBackground(new Color(255, 255, 255, 0));
		foregroundGraphics.clearRect(0, 0, width, height);

		// Fill background image with solid color.
		backgroundGraphics.setPaint(colorGroup.getBackground());
		backgroundGraphics.fillRect(0, 0, width, height);
		backgroundGraphics.setPaint(colorGroup.getNormal());

		foregroundGraphics.setPaint(colorGroup.getHighlight());

		if (phrase == null || !phrase1.equals(phrase)) {
			top = !top;
			phrase = phrase1;
		}

		int textHeight = fontMetrics.getAscent();
		int widthOfBottomLine;
		if (top) {
			widthOfBottomLine = fontMetrics.stringWidth(phrase2);
		} else {
			widthOfBottomLine = fontMetrics.stringWidth(phrase1);
		}

		// Alignment.
		float y1 = height / 2;
		float y2 = (int) (y1 + 2.0 * textHeight);
		float x1 = leftAlignPadding;
		float x2 = width - leftAlignPadding - widthOfBottomLine;

		BufferedImage foregroundSubImage;
		if (top) {
			// Draw the phrase for both layers.
			backgroundGraphics.drawString(phrase1, x1, y1);
			backgroundGraphics.drawString(phrase2, x2, y2);

			foregroundGraphics.drawString(phrase1, x1, y1);
			foregroundGraphics.drawString(phrase2, x2, y2);

			outputGraphics.drawImage(background, 0, 0, null);

			if (fraction > 0) {
				System.out.print(String.format("top %f %f %f %f %f fraction %d %d p1 %s p2 %s", y1 - textHeight, x1, y1,
						x2, y2, fraction, textHeight * 2, phrase1, phrase2));
				try {
					foregroundSubImage = foreground.getSubimage((int) x1, (int) y1 - textHeight, fraction,
							textHeight * 2);
					outputGraphics.drawImage(foregroundSubImage, (int) x1, (int) y1 - textHeight, null);
				} catch (RasterFormatException ex) {
					ex.printStackTrace();
					log.error(ex);
					log.error("Raster format exception! " + ex.toString());
				}
			}
		} else {
			// Draw the phrase for both layers.
			backgroundGraphics.drawString(phrase2, x1, y1);
			backgroundGraphics.drawString(phrase1, x2, y2);

			foregroundGraphics.drawString(phrase2, x1, y1);
			foregroundGraphics.drawString(phrase1, x2, y2);

			// Combine the two layers.
			outputGraphics.drawImage(background, 0, 0, null);
			if (fraction > 0) {
				System.out.print(String.format("bot %f %f %f %f %f fraction %d %d p1 %s p2 %s", y2 - textHeight, x1, y1,
						x2, y2, fraction, textHeight * 2, phrase1, phrase2));

				try {
					foregroundSubImage = foreground.getSubimage((int) x2, (int) y2 - textHeight, fraction,
							textHeight * 2);
					outputGraphics.drawImage(foregroundSubImage, (int) x2, (int) y2 - textHeight, null);
				} catch (RasterFormatException ex) {
					ex.printStackTrace();
					log.error(ex);
					log.error("Raster format exception! " + ex.toString());
				}
			}
		}

		System.out.println("out");
		return output;
	}

	public int stringWidth(String input) {
		return fontMetrics.stringWidth(input);
	}
}
