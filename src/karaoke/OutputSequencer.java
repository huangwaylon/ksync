package karaoke;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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

	private ColorGroup colorGroup = new ColorGroup();

	private LyricsProcessor lyricsProcessor;
	private AudioPlayer player;

	private boolean centerAligned;

	ChineseSequencer chineseSequencer;

	public OutputSequencer(LyricsProcessor lyricsProcessor, AudioPlayer player) {
		this.lyricsProcessor = lyricsProcessor;
		this.player = player;

		chineseSequencer = new ChineseSequencer(width, height, lyricsProcessor.getOutputFont());
	}

	public void export(SequencerListener listener, String outputDirectory) {
		log.debug("Output sequencer starting export.");

		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			log.debug("Output sequencer starting thread: " + Thread.currentThread().getName() + " and exporting to: "
					+ outputDirectory);
			System.out.println(
					"starting thread " + Thread.currentThread().getName() + " and exporting to: " + outputDirectory);
			try {
				sequence(outputDirectory, lyricsProcessor, player, listener);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			listener.done();
		});
	}

	public void sequence(String outputDirectory, LyricsProcessor lyricsProcessor, AudioPlayer player,
			SequencerListener listener) throws IOException {
		long start = System.currentTimeMillis();

		String date = new SimpleDateFormat("yyyyMMdd_HH_mm_ss").format(new Date());
		String outputPath = Paths.get(outputDirectory, String.format("karaoke_output_%s.mp4", date)).toString();
		FileChannelWrapper out = NIOUtils.writableChannel(MainUtils.tildeExpand(outputPath));
		AWTSequenceEncoder encoder = new AWTSequenceEncoder(out, Rational.parse(fps));

		int totalFrames = (int) Math.ceil((player.getLength() * framesPerSecond) / 1.0e6);
		double microsecondsPerFrame = 1.0e6 / framesPerSecond;

		String[][] lyrics = lyricsProcessor.getLyrics();
		long[][] wordTimestamps = lyricsProcessor.getTimestamps();

		int phraseIndex = -1, wordIndex = -1;
		int nextPhraseIndex = 0, nextWordIndex = 0;

		String currentPhrase = "";
		String followingPhrase = "";

		// Computed for the current word.
		int widthPerFrame = 0;
		int framesForWord = 0;

		String delimiter = lyricsProcessor.getSplitOption() == 0 ? " " : "";

		// Initial next time stamp is that of the first word.
		long nextTimestamp = wordTimestamps[0][0];

		System.out.println("Sequence with audioLength: " + player.getLength() + " with FPS: " + framesPerSecond
				+ " with frames: " + totalFrames + " nextTimestamp: " + nextTimestamp);

		for (int i = 0; i < totalFrames; i++) {
			System.out.println("Frame number: " + i);
			double totalTimeSoFar = microsecondsPerFrame * i;

			// Check if we've moved onto the next element.
			if (totalTimeSoFar >= nextTimestamp) {
				int lastPhraseIndex = phraseIndex;
				phraseIndex = nextPhraseIndex;
				wordIndex = nextWordIndex;

				// Check to see if we've moved onto a new phrase.
				if (phraseIndex > lastPhraseIndex) {
					currentPhrase = String.join(delimiter, lyrics[phraseIndex]);
				}

				// Now check for the next phrase.
				int[] nextPair = LyricsProcessor.getNextPairWithTimestampSet(phraseIndex, wordIndex, wordTimestamps);
				nextPhraseIndex = nextPair[0];
				nextWordIndex = nextPair[1];

				// If there is no phrase and word index available, fix final phrase.
				if (nextPhraseIndex < 0 || nextWordIndex < 0) {
					System.out.println("Final reached");
					nextTimestamp = Long.MAX_VALUE;
					followingPhrase = "";

					currentPhrase = "";
					widthPerFrame = 0;
				} else {
					if (phraseIndex == lyrics.length - 1) { // hack for phrase.
						followingPhrase = "";
					} else if (phraseIndex > lastPhraseIndex) { // Moved onto new phrase?
						followingPhrase = String.join(delimiter, lyrics[phraseIndex + 1]);
					}
					nextTimestamp = wordTimestamps[nextPhraseIndex][nextWordIndex];
					long timeDiff = nextTimestamp - wordTimestamps[phraseIndex][wordIndex];
					int wordWidth = chineseSequencer.stringWidth(lyrics[phraseIndex][wordIndex]);
					widthPerFrame = (int) Math.round(wordWidth / (timeDiff / microsecondsPerFrame));
				}

				// Reset frames for this word.
				framesForWord = 0;

				System.out
						.println("currentWord: " + lyrics[phraseIndex][wordIndex] + " widthPerFrame: " + widthPerFrame);
			}

			int subPhraseWidth = 0;
			if (phraseIndex >= 0 && wordIndex > 0 && nextTimestamp != Long.MAX_VALUE) { // hack for last word.
				String subPhrase = String.join("", Arrays.asList(lyrics[phraseIndex]).subList(0, wordIndex));
				subPhraseWidth = chineseSequencer.stringWidth(subPhrase)
						+ wordIndex * chineseSequencer.stringWidth(delimiter);
			}

			int width = widthPerFrame * framesForWord + subPhraseWidth;

			encoder.encodeImage(chineseSequencer.draw(currentPhrase, followingPhrase, width, colorGroup));

			framesForWord += 1;
			listener.setProgress((double) i / totalFrames);
		}
		System.out.println("Done sequencing!");
		System.out.println("Total time: " + (System.currentTimeMillis() - start) / 1000.0);

		encoder.finish();
		NIOUtils.closeQuietly(out);
	}

//		try {
//			ImageIO.write(finalImage, "PNG",
//					new File(String.format("/Users/waylonh/Downloads/test/test%08d.png", imageNum)));
//		} catch (IOException ie) {
//			ie.printStackTrace();
//		}

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

		centerAligned = alignment.equals("Center");
	}

	public void setWidthAndHeight(int width, int height) {
		this.width = width;
		this.height = height;

		chineseSequencer = new ChineseSequencer(width, height, lyricsProcessor.getOutputFont());
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

	public String getNumberOfLines() {
		return numberOfLines;
	}

	public void setNumberOfLines(String numberOfLines) {
		this.numberOfLines = numberOfLines;
	}

	public ColorGroup getColorGroup() {
		return colorGroup;
	}

	public interface ImageGenerator {
		public BufferedImage draw(String phrase1, String phrase2, int fraction, ColorGroup colorGroup);
	}

	public interface SequencerListener {
		public void done();

		public void setProgress(double progress);
	}
}
