package karaoke;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.Duration;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class LyricsProcessor {
	private final JPanel displayPanel;
	private Font font;

	private final Font timingFont = new Font("SansSerif", Font.PLAIN, 8);

	private String[][] lyrics;
	private long[][] wordTimestamps;
	private JLabel[][] labels;
	private JLabel[][] timeLabels;

	private int phraseIndex, wordIndex;

	private boolean noWordsSet;

	private IndexSelectListener indexSelectListener;

	public LyricsProcessor(JPanel displayPanel, IndexSelectListener indexSelectListener) {
		this.displayPanel = displayPanel;
		this.indexSelectListener = indexSelectListener;

		this.font = new Font("Serif", Font.PLAIN, 20);
	}

	public void loadLyrics(String lyricsStr, String splitOption) {
		String[] phrases = lyricsStr.split("[\\r\\n]+");

		lyrics = new String[phrases.length][];
		wordTimestamps = new long[phrases.length][];
		labels = new JLabel[phrases.length][];
		timeLabels = new JLabel[phrases.length][];

		for (int i = 0; i < phrases.length; i++) {
			if (splitOption.equals("Word")) {
				lyrics[i] = phrases[i].split("\\s+");
			} else if (splitOption.equals("Phrase")) {
				lyrics[i] = new String[] { phrases[i] };
			} else {
				String p = phrases[i].replaceAll("\\s+", "");
				lyrics[i] = p.split("");
			}

			wordTimestamps[i] = new long[lyrics[i].length];
			labels[i] = new JLabel[lyrics[i].length];
			timeLabels[i] = new JLabel[lyrics[i].length];
		}

		setUpPanel();
		resetAllSyncMarkers();
	}

	public void loadFont(Font font) {
		this.font = font;
		setUpPanel();
	}

	private void setUpPanel() {
		if (lyrics == null) {
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
				word.setFont(font);
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

	public void setTimestampForCurrentWord(long microseconds) {
		if (wordTimestamps == null || lyrics == null || labels == null) {
			System.err.println("wordTimestamps, lyrics, or labels is null.");
			return;
		}

		if (wordIndex < 0 || phraseIndex < 0) {
			return;
		}

		noWordsSet = false;

		wordTimestamps[phraseIndex][wordIndex] = microseconds;
		labels[phraseIndex][wordIndex].setBackground(Color.LIGHT_GRAY);
		timeLabels[phraseIndex][wordIndex].setText(formatMicroseconds(microseconds));

		clearAllLess(microseconds);

		int[] nextPair = getNextIndexPair(phraseIndex, wordIndex);
		setCurrentIndex(nextPair[0], nextPair[1]);
	}

	public void resetAllSyncMarkers() {
		if (wordTimestamps == null || lyrics == null || labels == null) {
			System.err.println("wordTimestamps, lyrics, or labels is null.");
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

		noWordsSet = true;
	}

	private void updateCurrentWord(int pIndex, int wIndex) {

	}

	// Return pair phraseIndex, wordIndex.
	private int[] getNextIndexPair(int pIndex, int wIndex) {
		if (wordTimestamps == null || lyrics == null || labels == null) {
			System.err.println("wordTimestamps, lyrics, or labels is null.");
			return null;
		}
		if (wIndex == wordTimestamps[pIndex].length - 1) {
			if (pIndex == wordTimestamps.length - 1) {
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
			System.err.println("wordTimestamps, lyrics, or labels is null.");
			return;
		}
	}

	private String formatMicroseconds(long microseconds) {
		Duration d = Duration.ofMillis(microseconds / 1000);
		return String.format("%d:%02d:%03d", d.toMinutesPart(), d.toSecondsPart(), d.toMillisPart());
	}

	public Font getSelectedFont() {
		return font;
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

			int[] indexPair = getNextIndexPair(pIndex, wIndex);
			pIndex = indexPair[0];
			wIndex = indexPair[1];
		}
	}
}
