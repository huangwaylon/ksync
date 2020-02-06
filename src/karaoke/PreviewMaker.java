package karaoke;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import javax.swing.JPanel;

import org.apache.log4j.Logger;

public class PreviewMaker {
	public static Logger log = Logger.getLogger(Main.class);

	private PreviewPanel previewPanel;

	private boolean isPlaying;

	private LyricsProcessor lyricsProcessor;
	private OutputSequencer outputSequencer;

	public PreviewMaker(LyricsProcessor lyricsProcessor, OutputSequencer outputSequencer) {
		this.lyricsProcessor = lyricsProcessor;
		this.outputSequencer = outputSequencer;

		previewPanel = new PreviewPanel();
	}

	public void stop() {
		isPlaying = false;
	}

	public void play() {
		if (lyricsProcessor.getTimestamps() == null || !checkTimestamps(lyricsProcessor.getTimestamps())) {
			System.err.println("Check timestamps failed. Every word should have a timestamp.");
			log.error("Check timestamps failed. Every word should have a timestamp.");
			return;
		}
		isPlaying = true;
	}

	private boolean checkTimestamps(long[][] timestamps) {
		for (int i = 0; i < timestamps.length; i++) {

			if (timestamps[i].length <= 0) {
				return false;
			}

			for (int j = 0; j < timestamps[i].length; j++) {
				if (timestamps[i][j] < 0) {
					return false;
				}
			}
		}
		return true;
	}

	public static long[] phraseArrayFromTimestamps(long[][] timestamps) {
		long[] phraseArr = new long[timestamps.length];
		for (int i = 0; i < timestamps.length; i++) {
			phraseArr[i] = timestamps[i][0];
		}
		return phraseArr;
	}

	public static int indexForTimestamp(long[] timestamps, long timestamp) {
		if (timestamp < timestamps[0]) {
			return -1;
		}

		if (timestamp >= timestamps[timestamps.length - 1]) {
			return timestamps.length - 1;
		}

		int i = 0, j = timestamps.length, mid = 0;

		while (i < j) {
			mid = (i + j) / 2;

			if (timestamps[mid] == timestamp) {
				return mid;
			}

			if (timestamp < timestamps[mid]) {
				if (mid > 0 && timestamp > timestamps[mid - 1]) {
					return mid - 1;
				}

				// Repeat for left half.
				j = mid;
			} else {
				if (mid < timestamps.length - 1 && timestamp < timestamps[mid + 1]) {
					return mid;
				}

				// Repeat for right half.
				i = mid + 1;
			}
		}

		// Single element left after search.
		return mid;
	}

	public void update(long timestamp) {
		if (!isPlaying) {
			return;
		}

		long[][] timestamps = lyricsProcessor.getTimestamps();

		long[] phraseArr = phraseArrayFromTimestamps(timestamps);
		int phraseIndex = indexForTimestamp(phraseArr, timestamp);

		String phrase1, phrase2;
		int fraction;

		ChineseSequencer chineseSequencer = outputSequencer.getChineseSequencer();

		if (phraseIndex < 0) {
			phrase1 = "";
			phrase2 = "";

			fraction = 0;
		} else {
			int wordIndex = indexForTimestamp(timestamps[phraseIndex], timestamp);
			if (wordIndex < 0) {
				System.err.println("Word index < 0 is unexpected!");
				return;
			}
			int[] nextPair = LyricsProcessor.getNextIndexPair(phraseIndex, wordIndex, timestamps);
			if (nextPair[0] < 0) {
				phrase1 = "";
				phrase2 = "";

				fraction = 0;
			} else {
				String[][] lyrics = lyricsProcessor.getLyrics();
				String delimiter = lyricsProcessor.delimiter();
				phrase1 = String.join(delimiter, lyrics[phraseIndex]);
				if (phraseIndex < lyrics.length - 1) {
					phrase2 = String.join(delimiter, lyrics[phraseIndex + 1]);
				} else {
					phrase2 = "";
				}

				String subPhrase = String.join("", Arrays.asList(lyrics[phraseIndex]).subList(0, wordIndex));
				int subPhraseWidth = chineseSequencer.stringWidth(subPhrase)
						+ wordIndex * chineseSequencer.stringWidth(delimiter);

				long start = lyricsProcessor.getWordTimestamps()[phraseIndex][wordIndex];
				long diff = timestamp - start;
				long next = lyricsProcessor.getWordTimestamps()[nextPair[0]][nextPair[1]];
				int wordWidth = chineseSequencer.stringWidth(lyrics[phraseIndex][wordIndex]);

				fraction = (int) (subPhraseWidth + wordWidth * (double) (diff) / (next - start));
			}
		}

		BufferedImage output = chineseSequencer.draw(phrase1, phrase2, fraction, outputSequencer.getColorGroup(),
				phraseIndex % 2 == 0);
		previewPanel.setImage(output);
		previewPanel.repaint();
	}

	public JPanel getPreviewPanel() {
		return previewPanel;
	}

	public static class PreviewPanel extends JPanel {
		private static final long serialVersionUID = 1L;
		private BufferedImage image;

		public PreviewPanel() {

		}

		public void setImage(BufferedImage image) {
			this.image = image;
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(image, 0, 0, this.getWidth(), this.getHeight(), this);
		}

	}
}
