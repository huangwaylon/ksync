package karaoke;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

public class LyricsProcessor {
	public static Logger log = Logger.getLogger(Main.class);

	private static final Color normalColor = Color.white;
	private static final Color currentColor = Color.yellow;
	private static final Color doneColor = Color.lightGray;

	private final int displayFontSize = 24;
	private final int outputFontSize = 112;

	// Fonts for editor display and final output.
	private final Font timingFont = new Font("SansSerif", Font.PLAIN, 8);
	private Font displayFont = new Font("Serif", Font.PLAIN, displayFontSize);
	private Font outputFont = new Font("Serif", Font.PLAIN, outputFontSize);

	private final JPanel displayPanel;

	private JLabel[][] labels;
	private JLabel[][] timeLabels;

	private String[][] lyrics;
	private long[][] wordTimestamps;

	private int phraseIndex, wordIndex;

	private IndexSelectListener indexSelectListener;

	private int splitOptionValue = 0;

	public LyricsProcessor(IndexSelectListener indexSelectListener) {
		this.indexSelectListener = indexSelectListener;

		displayPanel = new JPanel();
		displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.PAGE_AXIS));
	}

	public void loadLyrics(String lyricsStr, String splitOption) {
		log.debug("Lyrics processor loading lyrics with split option: " + splitOption);

		// Split by newline.
		String[] phrases = lyricsStr.split("[\\r\\n]+");

		lyrics = new String[phrases.length][];
		wordTimestamps = new long[phrases.length][];

		labels = new JLabel[phrases.length][];

		timeLabels = new JLabel[phrases.length][];

		// Single element array for the end of every phrase.
		String[] singleElement = new String[] { "" };
		for (int i = 0; i < phrases.length; i++) {
			phrases[i] = phrases[i].strip();

			String[] tempArray;
			if (splitOption.equals("Word")) {
				tempArray = phrases[i].split("\\s+");
				splitOptionValue = 0;
			} else if (splitOption.equals("Phrase")) {
				tempArray = new String[] { phrases[i] };
				splitOptionValue = 1;
			} else {
				String p = phrases[i].replaceAll("\\s+", "");
				tempArray = p.split("");
				splitOptionValue = 2;
			}
			lyrics[i] = Stream.concat(Arrays.stream(tempArray), Arrays.stream(singleElement)).toArray(String[]::new);

			int phraseLength = lyrics[i].length;
			wordTimestamps[i] = new long[phraseLength];

			labels[i] = new JLabel[phraseLength];
			timeLabels[i] = new JLabel[phraseLength];

			Arrays.fill(wordTimestamps[i], -1);
		}
		setUpPanel();
		resetAllSyncMarkers();
	}

	public void loadFont(Font font) {
		log.debug("Lyrics processor loading font: " + font.getName());

		this.outputFont = font;
		this.displayFont = font.deriveFont((float) displayFontSize);
		setUpPanel();
	}

	private void setUpPanel() {
		log.debug("Lyrics processor setting up editor panel.");

		if (lyrics == null) {
			log.warn("Lyrics processor did not set up editor panel because lyrics was null.");
			return;
		}

		displayPanel.removeAll();

		for (int i = 0; i < lyrics.length; i++) {
			JPanel phrase = new JPanel();
			phrase.setLayout(new BoxLayout(phrase, BoxLayout.LINE_AXIS));

			JPanel previewPhrase = new JPanel();
			previewPhrase.setLayout(new BoxLayout(previewPhrase, BoxLayout.LINE_AXIS));

			String[] phraseArr = lyrics[i];
			for (int j = 0; j < phraseArr.length; j++) {
				JPanel wordPanel = new JPanel();
				wordPanel.setLayout(new BoxLayout(wordPanel, BoxLayout.PAGE_AXIS));

				JLabel wordLabel = new JLabel(phraseArr[j]);
				if (j == phraseArr.length - 1) {
					wordLabel.setText("<END>");
				}

				wordLabel.setFont(displayFont);
				wordLabel.setOpaque(true);

				JLabel wordTiming = new JLabel("");
				wordTiming.setFont(timingFont);

				long wordTimingVal = wordTimestamps[i][j];
				if (wordTimingVal >= 0) {
					wordTiming.setText(formatMicroseconds(wordTimingVal));
				}

				final int pIndex = i;
				final int wIndex = j;

				wordPanel.addMouseListener(new MouseListener() {
					@Override
					public void mouseReleased(MouseEvent e) {
					}

					@Override
					public void mousePressed(MouseEvent e) {
					}

					@Override
					public void mouseExited(MouseEvent e) {
					}

					@Override
					public void mouseEntered(MouseEvent e) {
					}

					@Override
					public void mouseClicked(MouseEvent e) {
						updateCurrentIndex(pIndex, wIndex);

						// Double click.
						if (e.getClickCount() == 2) {
							final long playbackPosition = wordTimestamps[pIndex][wIndex];
							if (playbackPosition >= 0) {
								indexSelectListener.selectedPlaybackPosition(playbackPosition);
							}
						}
					}
				});

				wordPanel.add(wordLabel);
				wordPanel.add(wordTiming);

				phrase.add(wordPanel);
				phrase.add(Box.createRigidArea(new Dimension(5, 0)));

				JLabel previewLabel = new JLabel(phraseArr[j]);
				previewLabel.setFont(displayFont);
				previewLabel.setOpaque(true);
				previewPhrase.add(previewLabel);
				previewPhrase.add(Box.createRigidArea(new Dimension(5, 0)));

				labels[i][j] = wordLabel;
				timeLabels[i][j] = wordTiming;

				if (phraseIndex == i && wordIndex == j) {
					labels[i][j].setBackground(currentColor);
				} else if (wordTimestamps[i][j] >= 0) {
					labels[i][j].setBackground(doneColor);
				} else {
					labels[i][j].setBackground(normalColor);
				}
			}
			displayPanel.add(phrase);
			displayPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		}

		displayPanel.revalidate();
		displayPanel.repaint();
	}

	public void resetAllSyncMarkers() {
		log.debug("Lyrics processor resetting all synchronization markers.");

		if (wordTimestamps == null || lyrics == null || labels == null) {
			log.warn("Lyrics processor could not reset all synchronization markers because lyrics was null");
			return;
		}

		for (int i = 0; i < wordTimestamps.length; i++) {
			for (int j = 0; j < wordTimestamps[i].length; j++) {
				wordTimestamps[i][j] = -1;

				if (phraseIndex == i && wordIndex == j) {
					labels[i][j].setBackground(currentColor);
				} else {
					labels[i][j].setBackground(normalColor);
				}
				timeLabels[i][j].setText("");
			}
		}

		phraseIndex = 0;
		wordIndex = 0;
	}

	public void setTimestampForCurrentWordAndMoveToNext(long microseconds) {
		setTimestampForCurrentWord(microseconds);

		int[] nextPair = getNextIndexPair(phraseIndex, wordIndex, wordTimestamps);
		if (nextPair[0] >= 0 && nextPair[1] >= 0) {
			updateCurrentIndex(nextPair[0], nextPair[1]);
		}
	}

	// Return pair phraseIndex, wordIndex.
	public static int[] getNextIndexPair(int pIndex, int wIndex, long[][] timestamps) {
		if (timestamps == null) {
			log.warn("Lyrics processor can't get next pair of indices because timestamps is null.");
			System.err.println("timestamps is null.");
			return null;
		}

		if (wIndex == timestamps[pIndex].length - 1) {
			if (pIndex == timestamps.length - 1) {
				return new int[] { -1, -1 };
			} else {
				return new int[] { pIndex + 1, 0 };
			}
		} else {
			return new int[] { pIndex, wIndex + 1 };
		}
	}

	public void updateCurrentIndex(int pIndex, int wIndex) {
		if (wordTimestamps == null || lyrics == null || labels == null) {
			System.err.println("wordTimestamps, lyrics, or labels is null.");
			return;
		}

		if (pIndex < 0 || wIndex < 0) {
			return;
		}

		// Re-color current index.
		if (wordTimestamps[phraseIndex][wordIndex] >= 0) {
			labels[phraseIndex][wordIndex].setBackground(doneColor);
		} else {
			labels[phraseIndex][wordIndex].setBackground(normalColor);
		}

		this.phraseIndex = pIndex;
		this.wordIndex = wIndex;

		labels[phraseIndex][wordIndex].setBackground(currentColor);

		System.out.println("Set current index: " + phraseIndex + " " + wordIndex);
	}

	private void setTimestampForCurrentWord(long microseconds) {
		if (wordTimestamps == null || lyrics == null || labels == null) {
			System.err.println("wordTimestamps, lyrics, or labels is null.");
			return;
		}
		wordTimestamps[phraseIndex][wordIndex] = microseconds;

		labels[phraseIndex][wordIndex].setBackground(doneColor);
		timeLabels[phraseIndex][wordIndex].setText(formatMicroseconds(microseconds));

		clearAllLess(microseconds);
	}

	private void clearAllLess(long playbackPosition) {
		if (wordTimestamps == null || lyrics == null || labels == null) {
			return;
		}
		int pIndex = 0;
		int wIndex = 0;

		boolean intersected = false;

		while (pIndex >= 0 && wIndex >= 0) {
			if (pIndex != phraseIndex || wIndex != wordIndex) {
				long otherTime = wordTimestamps[pIndex][wIndex];

				if (otherTime >= 0) {
					if ((!intersected && otherTime >= playbackPosition)
							|| (intersected && otherTime <= playbackPosition)) {
						wordTimestamps[pIndex][wIndex] = -1;
						timeLabels[pIndex][wIndex].setText("");
						labels[pIndex][wIndex].setBackground(normalColor);
					}
				}
			} else {
				intersected = true;
			}

			int[] indexPair = getNextIndexPair(pIndex, wIndex, wordTimestamps);
			pIndex = indexPair[0];
			wIndex = indexPair[1];
		}
	}

	public static int[] getNextPairWithTimestampSet(int pIndex, int wIndex, long[][] wordTimestamps) {
		while (true) {
			int[] nextPair = getNextIndexPair(pIndex, wIndex, wordTimestamps);

			pIndex = nextPair[0];
			wIndex = nextPair[1];

			if (pIndex < 0 || wIndex < 0 || wordTimestamps[pIndex][wIndex] >= 0) {
				return nextPair;
			}
		}
	}

	public void nudge(String amount, boolean all, boolean left) {
		log.debug("Lyrics processor nudging by " + amount + " milliseconds, all: " + all + " left: " + left);

		if (wordTimestamps == null || lyrics == null || labels == null) {
			log.warn("Lyrics processor can't nudge because lyrics is null.");
			return;
		}

		int nudgeAmount;
		try {
			nudgeAmount = Integer.parseInt(amount);
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
			log.error("Lyrics processor can't parse nudge amount: " + amount + " to an integer.");
			log.error(ex);
			return;
		}
		if (nudgeAmount < 0) {
			log.error("Lyrics processor can't nudge a negative amount: " + nudgeAmount);
			return;
		}

		// Milliseconds to microseconds and correct nudge direction.
		nudgeAmount *= (left ? -1 : 1) * 1000;

		if (all) {
			// Update the time stamp value of all words.
			for (int i = 0; i < wordTimestamps.length; i++) {
				for (int j = 0; j < wordTimestamps[i].length; j++) {
					if (wordTimestamps[i][j] >= 0) {
						long newTime = wordTimestamps[i][j] + nudgeAmount;

						if (newTime < 0) {
							wordTimestamps[i][j] = -1;
							timeLabels[i][j].setText("");

							if (i != phraseIndex || j != wordIndex) {
								labels[i][j].setBackground(normalColor);
							}
						} else {
							wordTimestamps[i][j] = newTime;
							timeLabels[i][j].setText(formatMicroseconds(newTime));
						}
					}
				}
			}
		} else {
			if (phraseIndex < 0 || wordIndex < 0) {
				return;
			}

			if (wordTimestamps[phraseIndex][wordIndex] < 0
					|| wordTimestamps[phraseIndex][wordIndex] + nudgeAmount < 0) {
				return;
			}

			// Update the time stamp value of just a single word.
			long newTime = wordTimestamps[phraseIndex][wordIndex] + nudgeAmount;
			setTimestampForCurrentWord(newTime);
		}
		return;
	}

	private static String formatMicroseconds(long microseconds) {
		Duration d = Duration.ofMillis(microseconds / 1000);
		return String.format("%d:%02d:%03d", d.toMinutesPart(), d.toSecondsPart(), d.toMillisPart());
	}
	
	public Font getDisplayFont() {
		return displayFont;
	}

	public Font getOutputFont() {
		return outputFont;
	}

	public String[][] getLyrics() {
		return lyrics;
	}

	public long[][] getTimestamps() {
		return wordTimestamps;
	}

	public JPanel getDisplayPanel() {
		return displayPanel;
	}

	public int getSplitOption() {
		return splitOptionValue;
	}

	public long[][] getWordTimestamps() {
		return wordTimestamps;
	}

	public String delimiter() {
		return splitOptionValue == 0 ? " " : "";
	}
	
	public interface IndexSelectListener {
		public void selectedPlaybackPosition(long playbackPosition);
	}
}
