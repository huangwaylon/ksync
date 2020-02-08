package karaoke;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextLayout;
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
	private Font font;

	private final BasicStroke stroke1 = new BasicStroke(12);
	private final BasicStroke stroke2 = new BasicStroke(15);

	public ChineseSequencer(int width, int height, Font font) {
		this.width = width;
		this.height = height;
		this.font = font;

		setWidthAndHeight(width, height);
		setFont(font);
	}

	@Override
	public BufferedImage draw(String phrase1, String phrase2, int fraction, ColorGroup colorGroup, boolean top) {
		// Clear foreground image.
		foregroundGraphics.setBackground(new Color(255, 255, 255, 0));
		foregroundGraphics.clearRect(0, 0, width, height);

		// Fill background image with solid color.
		backgroundGraphics.setPaint(colorGroup.getBackground());
		backgroundGraphics.fillRect(0, 0, width, height);
		backgroundGraphics.setPaint(colorGroup.getNormal());

		foregroundGraphics.setPaint(colorGroup.getHighlight());

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
			backgroundGraphics.setStroke(stroke1);
			backgroundGraphics.setColor(colorGroup.getOutline());

			foregroundGraphics.setStroke(stroke2);
			foregroundGraphics.setColor(colorGroup.getHighlightOutline());

			if (phrase1.length() > 0) {
				TextLayout tl1 = new TextLayout(phrase1, font, backgroundGraphics.getFontRenderContext());
				Shape outline1 = tl1.getOutline(null);

				backgroundGraphics.translate(x1, y1);
				backgroundGraphics.draw(outline1);
				backgroundGraphics.translate(-x1, -y1);

				foregroundGraphics.translate(x1, y1);
				foregroundGraphics.draw(outline1);
				foregroundGraphics.translate(-x1, -y1);
			}

			if (phrase2.length() > 0) {
				TextLayout tl2 = new TextLayout(phrase2, font, backgroundGraphics.getFontRenderContext());
				Shape outline2 = tl2.getOutline(null);

				backgroundGraphics.translate(x2, y2);
				backgroundGraphics.draw(outline2);
				backgroundGraphics.translate(-x2, -y2);

				foregroundGraphics.translate(x2, y2);
				foregroundGraphics.draw(outline2);
				foregroundGraphics.translate(-x2, -y2);
			}

			backgroundGraphics.setColor(colorGroup.getNormal());
			foregroundGraphics.setColor(colorGroup.getHighlight());

			// Draw the phrase for both layers.
			backgroundGraphics.drawString(phrase1, x1, y1);
			backgroundGraphics.drawString(phrase2, x2, y2);

			foregroundGraphics.drawString(phrase1, x1, y1);
			foregroundGraphics.drawString(phrase2, x2, y2);

			outputGraphics.drawImage(background, 0, 0, null);

			if (fraction > 0) {
//				System.out.print(String.format("top %f %f %f %f %f fraction %d %d p1 %s p2 %s", y1 - textHeight, x1, y1,
//						x2, y2, fraction, (int) (textHeight * 1.5), phrase1, phrase2));
				try {
					int shiftX = (int) x1 - 5;

					foregroundSubImage = foreground.getSubimage(shiftX, (int) (y1 - textHeight * 1.2), fraction + 10,
							(int) (textHeight * 1.6));
					outputGraphics.drawImage(foregroundSubImage, shiftX, (int) (y1 - textHeight * 1.2), null);
				} catch (RasterFormatException ex) {
					ex.printStackTrace();
					log.error(ex);
					log.error("Raster format exception! " + ex.toString());
				}
			}
		} else {
			backgroundGraphics.setStroke(stroke1);
			backgroundGraphics.setColor(colorGroup.getOutline());

			foregroundGraphics.setStroke(stroke2);
			foregroundGraphics.setColor(colorGroup.getHighlightOutline());

			if (phrase2.length() > 0) {
				TextLayout tl1 = new TextLayout(phrase2, font, backgroundGraphics.getFontRenderContext());
				Shape outline1 = tl1.getOutline(null);

				backgroundGraphics.translate(x1, y1);
				backgroundGraphics.draw(outline1);
				backgroundGraphics.translate(-x1, -y1);

				foregroundGraphics.translate(x1, y1);
				foregroundGraphics.draw(outline1);
				foregroundGraphics.translate(-x1, -y1);
			}

			if (phrase1.length() > 0) {
				TextLayout tl2 = new TextLayout(phrase1, font, backgroundGraphics.getFontRenderContext());
				Shape outline2 = tl2.getOutline(null);

				backgroundGraphics.translate(x2, y2);
				backgroundGraphics.draw(outline2);
				backgroundGraphics.translate(-x2, -y2);

				foregroundGraphics.translate(x2, y2);
				foregroundGraphics.draw(outline2);
				foregroundGraphics.translate(-x2, -y2);
			}

			backgroundGraphics.setColor(colorGroup.getNormal());
			foregroundGraphics.setColor(colorGroup.getHighlight());

			// Draw the phrase for both layers.
			backgroundGraphics.drawString(phrase2, x1, y1);
			backgroundGraphics.drawString(phrase1, x2, y2);

			foregroundGraphics.drawString(phrase2, x1, y1);
			foregroundGraphics.drawString(phrase1, x2, y2);

			// Combine the two layers.
			outputGraphics.drawImage(background, 0, 0, null);
			if (fraction > 0) {
//				System.out.print(String.format("bot %f %f %f %f %f fraction %d %d p1 %s p2 %s", y2 - textHeight, x1, y1,
//						x2, y2, fraction, (int) (textHeight * 1.5), phrase1, phrase2));
				try {
					int shiftX = (int) x2 - 5;

					foregroundSubImage = foreground.getSubimage(shiftX, (int) (y2 - textHeight * 1.2), fraction + 10,
							(int) (textHeight * 1.6));
					outputGraphics.drawImage(foregroundSubImage, shiftX, (int) (y2 - textHeight * 1.2), null);
				} catch (RasterFormatException ex) {
					ex.printStackTrace();
					log.error(ex);
					log.error("Raster format exception! " + ex.toString());
				}
			}
		}
		return output;
	}

	public int stringWidth(String input) {
		return fontMetrics.stringWidth(input);
	}

	public void setWidthAndHeight(int width, int height) {
		this.width = width;
		this.height = height;

		background = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		backgroundGraphics = background.createGraphics();
		backgroundGraphics.setRenderingHints(renderingHints);

		foreground = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		foregroundGraphics = foreground.createGraphics();
		foregroundGraphics.setRenderingHints(renderingHints);

		output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		outputGraphics = output.createGraphics();
		outputGraphics.setRenderingHints(renderingHints);
	}

	public void setFont(Font font) {
		this.font = font;
		backgroundGraphics.setFont(font);
		foregroundGraphics.setFont(font);
		fontMetrics = backgroundGraphics.getFontMetrics();
	}
}
