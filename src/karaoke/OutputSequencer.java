package karaoke;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;
import org.jcodec.common.tools.MainUtils;

public class OutputSequencer {
	public static Logger log = Logger.getLogger(Main.class);

	private int width = 1920, height = 1080;
	private String fps = "30:1";
	private String alignment = "Center";
	private int framesPerSecond = 30;
	private String numberOfLines = "2";

	private Color normal = Color.white, outline = Color.black, highlight = Color.blue, highlightOutline = Color.white,
			background = Color.green;

	public OutputSequencer() {

	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setFPS(String fps) {
		log.debug("Output sequencer set FPS: " + fps);

		this.fps = fps;
		if (fps.equals("30:1")) {
			framesPerSecond = 30;
		} else if (fps.equals("60:1")) {
			framesPerSecond = 60;
		} else if (fps.equals("24:1")) {
			framesPerSecond = 24;
		} else if (fps.equals("12:1")) {
			framesPerSecond = 12;
		} else if (fps.equals("15:1")) {
			framesPerSecond = 15;
		} else {
			framesPerSecond = 25;
		}
		System.out.println("Set FPS: " + fps + " framesPerSecond: " + framesPerSecond);
	}

	public void export(LyricsProcessor lyricsProcessor, AudioPlayer player, SequencerListener listener) {
		log.debug("Output sequencer starting export.");

		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			log.debug("Output sequencer starting thread: " + Thread.currentThread().getName());
			System.out.println("starting thread " + Thread.currentThread().getName());
			try {
				sequence(lyricsProcessor, player, listener);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			listener.done();
		});
	}

	public void sequence(LyricsProcessor lyricsProcessor, AudioPlayer player, SequencerListener listener)
			throws IOException {
		FileChannelWrapper out = null;
		out = NIOUtils.writableChannel(MainUtils.tildeExpand("/Users/waylonh/Downloads/out.mp4"));
		AWTSequenceEncoder encoder = new AWTSequenceEncoder(out, Rational.parse(fps));

		RenderingHints renderingHints = new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// Background components.
		BufferedImage backgroundBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D backgroundGraphics = backgroundBufferedImage.createGraphics();
		backgroundGraphics.setRenderingHints(renderingHints);
		backgroundGraphics.setFont(lyricsProcessor.getOutputFont());

		// Foreground components.
		BufferedImage foregroundBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D foregroundGraphics = foregroundBufferedImage.createGraphics();
		foregroundGraphics.setRenderingHints(renderingHints);
		foregroundGraphics.setFont(lyricsProcessor.getOutputFont());
		foregroundGraphics.setPaint(highlight);

		// Second line components
		BufferedImage secondBufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D secondGraphics = secondBufferedImage.createGraphics();
		secondGraphics.setRenderingHints(renderingHints);
		secondGraphics.setFont(lyricsProcessor.getOutputFont());
		secondGraphics.setPaint(normal);

		// Final image components.
		BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics finalGraphics = finalImage.getGraphics();

		long audioLength = player.getLength();
		int totalFrames = (int) Math.ceil((audioLength * framesPerSecond) / 1e6);

		int phraseIndex = -1, wordIndex = -1;
		int nextPhraseIndex = 0, nextWordIndex = 0;

		double microsecondsPerFrame = 1.0e6 / framesPerSecond;

		String[][] lyrics = lyricsProcessor.getLyrics();
		long[][] wordTimestamps = lyricsProcessor.getTimestamps();

		// Computed for the current phrase.
		FontMetrics fontMetrics = backgroundGraphics.getFontMetrics();
		String phrase = "";
		String nextPhrase = "";

		if (lyrics.length >= 1) {
			nextPhrase = String.join(" ", lyrics[0]);
		}

		int spaceWidth = fontMetrics.stringWidth(" ");
		int wordWidth = 0;
		int totalWidthSoFar = 0;

		int phraseWidth = spaceWidth;
		int phraseHeight = fontMetrics.getAscent();

		// Computed for the current word.
		int widthPerFrame = 0;
		int frames = 0;

		// Initial next time stamp is that of the first word.
		long nextTimestamp = wordTimestamps[nextPhraseIndex][nextWordIndex];

		long start = System.currentTimeMillis();

		System.out.println("Sequence with audioLength: " + audioLength + " with FPS: " + framesPerSecond
				+ " with frames: " + totalFrames);

		System.out.println("nextTimestamp: " + nextTimestamp + " spaceWidth: " + spaceWidth);

		for (int i = 0; i < totalFrames; i++) {
			System.out.println("Frame number: " + i);
			double totalTimeSoFar = microsecondsPerFrame * i;

			// Check if we've moved onto the next element.
			if (totalTimeSoFar > nextTimestamp) {
				// Check if we've reached the end of our lyrics lists.
				if (nextPhraseIndex < 0 || nextWordIndex < 0) {
					phrase = "";
					phraseWidth = 1;

					nextTimestamp = player.getLength();
					break;
				}
				int lastP = phraseIndex;

				phraseIndex = nextPhraseIndex;
				wordIndex = nextWordIndex;

				long currTimestamp = wordTimestamps[phraseIndex][wordIndex];

				// Check to see if we've moved onto a new phrase.
				if (phraseIndex > lastP) {
					phrase = String.join(" ", lyrics[phraseIndex]);
					totalWidthSoFar = 0;

					phraseWidth = fontMetrics.stringWidth(phrase);
				}

				String currentWord = lyrics[phraseIndex][wordIndex];
				wordWidth = fontMetrics.stringWidth(currentWord);
				totalWidthSoFar += wordWidth + (wordIndex == 0 ? 0 : spaceWidth);

				System.out.print("get next pair");
				int[] nextPair = getNextPair(phraseIndex, wordIndex, wordTimestamps);
				nextPhraseIndex = nextPair[0];
				nextWordIndex = nextPair[1];
				System.out.println(" done");

				// If there is no phrase and word index available, use the final time stamp.
				if (nextPhraseIndex < 0 || nextWordIndex < 0) {
					long finalTimestamp = lyricsProcessor.getFinalTimestamp();
					if (finalTimestamp < 0) {
						nextTimestamp = player.getLength();
					} else {
						nextTimestamp = finalTimestamp;
					}
				} else {
					nextTimestamp = wordTimestamps[nextPhraseIndex][nextWordIndex];
				}

				if (phraseIndex + 1 < lyrics.length) {
					nextPhrase = String.join(" ", lyrics[phraseIndex + 1]);
				} else {
					nextPhrase = "";
				}

				long timeDiff = nextTimestamp - currTimestamp;
				widthPerFrame = (int) Math.round(wordWidth / (timeDiff / microsecondsPerFrame));

				// Reset frames for this word.
				frames = 0;

				System.out.println("currentWord: " + currentWord + " totalWidthSoFar: " + totalWidthSoFar
						+ " widthPerFrame: " + widthPerFrame);
			}

			int width = widthPerFrame * frames + totalWidthSoFar - wordWidth;
			if (width <= 0) {
				width = 1;
			}

			genImage(i, phrase, nextPhrase, backgroundGraphics, foregroundGraphics, secondGraphics, finalGraphics,
					backgroundBufferedImage, foregroundBufferedImage, secondBufferedImage, finalImage, phraseWidth,
					phraseHeight, width);
			encoder.encodeImage(finalImage);

			frames += 1;

			listener.setProgress((double) i / totalFrames);
		}
		System.out.println("Done sequencing!");
		System.out.println("Total time: " + (System.currentTimeMillis() - start) / 1000.0);

		encoder.finish();
		NIOUtils.closeQuietly(out);
	}

	// Keep searching for a non-negative time stamp until one is found, else return
	// -1, -1.
	private int[] getNextPair(int pIndex, int wIndex, long[][] wordTimestamps) {
		while (true) {
			int[] nextPair = LyricsProcessor.getNextIndexPair(pIndex, wIndex, wordTimestamps);

			if (pIndex < 0 || wIndex < 0) {
				return nextPair;
			}

			long timestamp = wordTimestamps[pIndex][wIndex];
			if (timestamp >= 0) {
				return nextPair;
			}

			pIndex = nextPair[0];
			wIndex = nextPair[1];
		}
	}

	private void genImage(int imageNum, String phrase, String nextPhrase, Graphics2D backgroundGraphics,
			Graphics2D foregroundGraphics, Graphics2D secondGraphics, Graphics finalGraphics,
			BufferedImage backgroundBufferedImage, BufferedImage foregroundBufferedImage,
			BufferedImage secondBufferedImage, BufferedImage finalImage, int phraseWidth, int phraseHeight,
			int phraseFractionWidth) {
		// Clear foreground image.
		foregroundGraphics.setBackground(new Color(255, 255, 255, 0));
		foregroundGraphics.clearRect(0, 0, width, height);

		// Clear second line image.
		secondGraphics.setBackground(new Color(255, 255, 255, 0));
		secondGraphics.clearRect(0, 0, width, height);

		// Fill background image with solid color.
		backgroundGraphics.setPaint(background);
		backgroundGraphics.fillRect(0, 0, width, height);
		backgroundGraphics.setPaint(normal);

		// Align to center.
		float x = (width - phraseWidth) / 2;
		float y = height / 2 + phraseHeight / 4;

		// Draw the phrase for both layers.
		backgroundGraphics.drawString(phrase, x, y);
		foregroundGraphics.drawString(phrase, x, y);
		secondGraphics.drawString(nextPhrase, x, (int) (y + 1.5 * phraseHeight));

		// Cut the foreground slice.
		BufferedImage foregroundSubImage = foregroundBufferedImage.getSubimage((int) x, (int) y - phraseHeight,
				phraseFractionWidth, phraseHeight * 2);

		// Combine the two layers.
		finalGraphics.drawImage(backgroundBufferedImage, 0, 0, null);
		finalGraphics.drawImage(foregroundSubImage, (int) x, (int) y - phraseHeight, null);
		finalGraphics.drawImage(secondBufferedImage, 0, 0, null);

		System.out.println("x: " + x + " y: " + y + " phraseFractionWidth: " + phraseFractionWidth);

//		try {
//			ImageIO.write(finalImage, "PNG",
//					new File(String.format("/Users/waylonh/Downloads/test/test%08d.png", imageNum)));
//		} catch (IOException ie) {
//			ie.printStackTrace();
//		}
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

	public interface SequencerListener {
		public void done();

		public void setProgress(double progress);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public String getFPS() {
		return fps;
	}

	public String getAlignment() {
		return alignment;
	}

	public void setAlignment(String alignment) {
		this.alignment = alignment;
	}

	public String getNumberOfLines() {
		return numberOfLines;
	}

	public void setNumberOfLines(String numberOfLines) {
		this.numberOfLines = numberOfLines;
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
