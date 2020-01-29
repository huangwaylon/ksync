package karaoke;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class OutputSequencer {
	private int width = 1920, height = 1080;
	private Font font = new Font("TimesRoman", Font.BOLD, 72);

	private String fps = "30:1";
	private int framesPerSecond = 30;

	private Color normal, outline, highlight, highlightOutline, background;

	public OutputSequencer() {

	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	public void setFPS(String fps) {
		this.fps = fps;

		if (fps.equals("30:1")) {
			framesPerSecond = 30;
		} else if (fps.equals("60:1")) {
			framesPerSecond = 60;
		} else if (fps.equals("24:1")) {
			framesPerSecond = 24;
		} else {
			framesPerSecond = 25;
		}
		System.out.println("Set FPS: " + fps + " framesPerSecond: " + framesPerSecond);
	}

	public void sequence(LyricsProcessor lyricsProcessor, AudioPlayer player) {
		long audioLength = player.getLength();
		System.out.println(audioLength);

		int totalFrames = (int) Math.ceil((audioLength * framesPerSecond) / 1e6);

		int currentPhraseIndex = -1;

		double microsecondsPerFrame = 1.0e6 / framesPerSecond;

		String[][] lyrics = lyricsProcessor.getLyrics();
		long[][] wordTimestamps = lyricsProcessor.getTimestamps();

		System.out.println("Sequence with audioLength: " + audioLength + " with FPS: " + framesPerSecond
				+ " with frames: " + totalFrames);

		// Background components.
		BufferedImage backgroundBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D backgroundGraphics = backgroundBufferedImage.createGraphics();
		RenderingHints renderingHints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		backgroundGraphics.setRenderingHints(renderingHints);
		backgroundGraphics.setFont(font);

		// Foreground components.
		BufferedImage foregroundBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D foregroundGraphics = foregroundBufferedImage.createGraphics();
		foregroundGraphics.setRenderingHints(renderingHints);

		foregroundGraphics.setFont(font);
		foregroundGraphics.setPaint(highlight);

		// Final image components.
		BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics finalGraphics = finalImage.getGraphics();

		// Draw background color.
		backgroundGraphics.setColor(background);

		for (int i = 0; i < totalFrames; i++) {
			System.out.println("Frame number: " + i);

			double totalTimeSoFar = microsecondsPerFrame * i;

			if (currentPhraseIndex < lyrics.length - 1 && totalTimeSoFar >= wordTimestamps[currentPhraseIndex + 1][0]) {
				currentPhraseIndex += 1;
			}

			String phrase = currentPhraseIndex < 0 ? "" : String.join(" ", lyrics[currentPhraseIndex]);
			genImage(i, phrase, backgroundGraphics, foregroundGraphics, finalGraphics, backgroundBufferedImage,
					foregroundBufferedImage, finalImage);
		}

		System.out.println("Done sequencing!");
	}

	private void genImage(int imageNum, String phrase, Graphics2D backgroundGraphics, Graphics2D foregroundGraphics,
			Graphics finalGraphics, BufferedImage backgroundBufferedImage, BufferedImage foregroundBufferedImage,
			BufferedImage finalImage) {
		backgroundGraphics.fillRect(0, 0, width, height);
		backgroundGraphics.setPaint(normal);

		FontMetrics fontMetrics = backgroundGraphics.getFontMetrics();
		int stringWidth = fontMetrics.stringWidth(phrase);
		int stringHeight = fontMetrics.getAscent();

		backgroundGraphics.drawString(phrase, (width - stringWidth) / 2, height / 2 + stringHeight / 4);
		foregroundGraphics.drawString(phrase, (width - stringWidth) / 2, height / 2 + stringHeight / 4);

		BufferedImage foregroundSubImage = foregroundBufferedImage.getSubimage(0, 0, 900, 1080);

		finalGraphics.drawImage(backgroundBufferedImage, 0, 0, null);
		finalGraphics.drawImage(foregroundSubImage, 0, 0, null);

		try {
			ImageIO.write(finalImage, "PNG",
					new File(String.format("/Users/waylonh/Downloads/test/image%08d.png", imageNum)));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
}
