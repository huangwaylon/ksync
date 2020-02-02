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

	private final int displayFontSize = 24;
	private final int outputFontSize = 72;

	private final Font timingFont = new Font("SansSerif", Font.PLAIN, 8);
	// Font used to display lyrics in editor.
	private Font displayFont = new Font("Serif", Font.PLAIN, displayFontSize);
	// Font used in final output.
	private Font outputFont = new Font("Serif", Font.PLAIN, outputFontSize);

	private final JPanel displayPanel;

	private String[][] lyrics;
	private long[][] wordTimestamps;

	// End point after the final word in lyrics.
	private long finalTimestamp;

	private JLabel[][] labels;
	private JLabel[][] timeLabels;

	private int phraseIndex, wordIndex;

	private IndexSelectListener indexSelectListener;

	public LyricsProcessor(IndexSelectListener indexSelectListener) {
		this.indexSelectListener = indexSelectListener;

		displayPanel = new JPanel();
		displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.PAGE_AXIS));
	}

	public void loadLyrics(String lyricsStr, String splitOption) {
		log.debug("Lyrics processor loading lyrics with split option: " + splitOption);

		String[] phrases = lyricsStr.split("[\\r\\n]+");

		lyrics = new String[phrases.length][];
		wordTimestamps = new long[phrases.length][];
		labels = new JLabel[phrases.length][];
		timeLabels = new JLabel[phrases.length][];

		String[] singleElement = new String[] { "" };
		for (int i = 0; i < phrases.length; i++) {
			String[] tempArray;
			if (splitOption.equals("Word")) {
				tempArray = phrases[i].split("\\s+");
			} else if (splitOption.equals("Phrase")) {
				tempArray = new String[] { phrases[i] };
			} else {
				String p = phrases[i].replaceAll("\\s+", "");
				tempArray = p.split("");
			}
			lyrics[i] = Stream.concat(Arrays.stream(tempArray), Arrays.stream(singleElement)).toArray(String[]::new);

			wordTimestamps[i] = new long[lyrics[i].length];
			labels[i] = new JLabel[lyrics[i].length];
			timeLabels[i] = new JLabel[lyrics[i].length];
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

			String[] phraseArr = lyrics[i];
			for (int j = 0; j < phraseArr.length; j++) {
				JPanel wordPanel = new JPanel();
				wordPanel.setLayout(new BoxLayout(wordPanel, BoxLayout.PAGE_AXIS));

				JLabel word = new JLabel(phraseArr[j]);
				word.setFont(displayFont);
				word.setOpaque(true);

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
						setCurrentIndex(pIndex, wIndex);

						// Double click.
						if (e.getClickCount() == 2) {
							final long playbackPosition = wordTimestamps[pIndex][wIndex];
							if (playbackPosition >= 0) {
								indexSelectListener.selectedPlaybackPosition(playbackPosition);
							}
						}
					}
				});

				wordPanel.add(word);
				wordPanel.add(wordTiming);

				phrase.add(wordPanel);
				phrase.add(Box.createRigidArea(new Dimension(5, 0)));

				labels[i][j] = word;
				timeLabels[i][j] = wordTiming;
			}
			displayPanel.add(phrase);
			displayPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		}

		displayPanel.revalidate();
		displayPanel.repaint();
	}

	private void setCurrentIndex(int pIndex, int wIndex) {
		if (wordTimestamps == null || lyrics == null || labels == null) {
			System.err.println("wordTimestamps, lyrics, or labels is null.");
			return;
		}

		if (pIndex < 0 || wIndex < 0) {
			return;
		}

		// Re-color current index.
		if (wordTimestamps[phraseIndex][wordIndex] > 0) {
			labels[phraseIndex][wordIndex].setBackground(Color.LIGHT_GRAY);
		} else {
			labels[phraseIndex][wordIndex].setBackground(Color.WHITE);
		}

		this.phraseIndex = pIndex;
		this.wordIndex = wIndex;

		System.out.println("indicies: " + phraseIndex + " " + wordIndex);

		labels[phraseIndex][wordIndex].setBackground(Color.YELLOW);
	}

	public void setTimestampForCurrentWordAndMoveToNext(long microseconds) {
		setTimestampForCurrentWord(microseconds);

		int[] nextPair = getNextIndexPair(phraseIndex, wordIndex, wordTimestamps);
		setCurrentIndex(nextPair[0], nextPair[1]);
	}

	private void setTimestampForCurrentWord(long microseconds) {
		if (wordTimestamps == null || lyrics == null || labels == null) {
			System.err.println("wordTimestamps, lyrics, or labels is null.");
			return;
		}

		if (wordIndex < 0 || phraseIndex < 0) {
			finalTimestamp = microseconds;
			return;
		}

		wordTimestamps[phraseIndex][wordIndex] = microseconds;

		labels[phraseIndex][wordIndex].setBackground(Color.LIGHT_GRAY);
		timeLabels[phraseIndex][wordIndex].setText(formatMicroseconds(microseconds));

		clearAllLess(microseconds);
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

				labels[i][j].setBackground(Color.WHITE);
				timeLabels[i][j].setText("");
			}
		}

		phraseIndex = 0;
		wordIndex = 0;
	}

	public boolean nudge(String amount, boolean all, boolean left) {
		log.debug("Lyrics processor nudging by " + amount + " milliseconds, all: " + all + " left: " + left);

		if (wordTimestamps == null || lyrics == null || labels == null) {
			log.warn("Lyrics processor can't nudge because lyrics is null.");
			return false;
		}

		int nudgeAmount;
		try {
			nudgeAmount = Integer.parseInt(amount);
		} catch (NumberFormatException ex) {
			ex.printStackTrace();
			return false;
		}
		if (nudgeAmount < 0) {
			return false;
		}

		nudgeAmount *= (left ? -1 : 1);
		// Milliseconds to microseconds.
		nudgeAmount *= 1000;

		if (all) {
			// Update the time stamp value of just a single word.
			for (int i = 0; i < wordTimestamps.length; i++) {
				for (int j = 0; j < wordTimestamps[i].length; j++) {
					if (wordTimestamps[i][j] >= 0) {
						long newTime = wordTimestamps[i][j] + nudgeAmount;

						wordTimestamps[i][j] = newTime;
						timeLabels[i][j].setText(formatMicroseconds(newTime));
					}
				}
			}
		} else {
			if (phraseIndex < 0 || wordIndex < 0) {
				return false;
			}
			// Update the time stamp value of just a single word.
			long newTime = wordTimestamps[phraseIndex][wordIndex] + nudgeAmount;
			setTimestampForCurrentWord(newTime);
			setCurrentIndex(phraseIndex, wordIndex);
		}
		return false;
	}

	// Return pair phraseIndex, wordIndex.
	public static int[] getNextIndexPair(int pIndex, int wIndex, long[][] timestamps) {
		if (timestamps == null) {
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

	public void update(long playbackPosition) {
		if (wordTimestamps == null || lyrics == null || labels == null) {
			return;
		}
	}

	private String formatMicroseconds(long microseconds) {
		Duration d = Duration.ofMillis(microseconds / 1000);
		return String.format("%d:%02d:%03d", d.toMinutesPart(), d.toSecondsPart(), d.toMillisPart());
	}

	public Font getDisplayFont() {
		return displayFont;
	}

	public Font getOutputFont() {
		return outputFont;
	}

	public interface IndexSelectListener {
		public void selectedPlaybackPosition(long playbackPosition);
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
						labels[pIndex][wIndex].setBackground(Color.WHITE);
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

	public String[][] getLyrics() {
		return lyrics;
	}

	public long[][] getTimestamps() {
		return wordTimestamps;
	}

	public long getFinalTimestamp() {
		return finalTimestamp;
	}

	public JPanel getDisplayPanel() {
		return displayPanel;
	}
}