package karaoke;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.*;

import java.awt.event.*;
import java.time.Duration;

import javax.swing.filechooser.*;

import org.apache.log4j.Logger;
import org.drjekyll.fontchooser.FontDialog;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import karaoke.OutputSequencer.SequencerListener;
import karaoke.TimerService.TimerListener;
import karaoke.WaveFormPane.WaveListener;

public class Main {
	public static Logger log = Logger.getLogger(Main.class);

	private static final int DIALOG_PADDING = 10;

	private static JTextField pathField;
	private static JLabel playbackTimeLabel;

	private static String lengthTimeStr;

	private static WaveFormPane waveFormPane;

	private static String audioFilePath;

	public static final AudioPlayer player = new AudioPlayer();
	private static final Transcoder transcoder = new Transcoder();

	private static final LyricsProcessor lyricsProcessor = new LyricsProcessor(player);

	private static final WaveReactor waveReactor = new WaveReactor(player, lyricsProcessor);

	private static final IntervalUpdater intervalUpdater = new IntervalUpdater();
	private static final TimerService timerService = new TimerService(intervalUpdater);

	private static final OutputSequencer outputSequencer = new OutputSequencer(lyricsProcessor, player);

	public static void main(String args[]) {
		log.debug("Starting karaoke sync program.");

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				initAndShowGUI();
			}
		});
	}

	private static void initAndShowGUI() {
		log.debug("Initializing and showing GUI.");

		Dimension minFrameSize = new Dimension(1024, 700);
		Dimension minSize = new Dimension(150, 50);

		JFrame frame = new JFrame("Karaoke Sync");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setMinimumSize(minFrameSize);
		frame.setSize(1024, 720);

		// Lyrics options panel.
		JPanel lyricsOptPanel = new JPanel();
		lyricsOptPanel.setLayout(new BoxLayout(lyricsOptPanel, BoxLayout.LINE_AXIS));
		JLabel lyricsLabel = new JLabel("Lyrics");
		String[] splitOptions = new String[] { "Word", "Character", "Phrase" };
		JComboBox<String> splitComboBox = new JComboBox<String>(splitOptions);
		lyricsOptPanel.add(lyricsLabel);
		lyricsOptPanel.add(splitComboBox);
		lyricsOptPanel.setMaximumSize(new Dimension(200, 80));

		JTextArea lyricsTextArea = new JTextArea();
		lyricsTextArea.setColumns(40);
		lyricsTextArea.setRows(20);
		lyricsTextArea.setLineWrap(true);

		JScrollPane lyricsScrollPane = new JScrollPane(lyricsTextArea);
		lyricsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		lyricsScrollPane.setPreferredSize(new Dimension(250, 250));

		JPanel lyricsInputPanel = new JPanel();
		lyricsInputPanel.setLayout(new BoxLayout(lyricsInputPanel, BoxLayout.PAGE_AXIS));
		lyricsInputPanel.add(lyricsScrollPane);
		lyricsInputPanel.add(Box.createVerticalStrut(5));
		lyricsInputPanel.add(lyricsOptPanel);
		lyricsInputPanel.add(Box.createVerticalStrut(5));

		// Lyrics editor display panel.
		JScrollPane displayScrollPane = new JScrollPane(lyricsProcessor.getDisplayPanel());
		displayScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		displayScrollPane.setPreferredSize(new Dimension(250, 250));
		displayScrollPane.getVerticalScrollBar().setUnitIncrement(16);

		JScrollPane previewScrollPane = new JScrollPane(lyricsProcessor.getPreviewPanel());
		previewScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		previewScrollPane.setPreferredSize(new Dimension(250, 250));
		previewScrollPane.getVerticalScrollBar().setUnitIncrement(16);

		JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, displayScrollPane, previewScrollPane);
		rightSplitPane.setContinuousLayout(true);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lyricsInputPanel, rightSplitPane);
		splitPane.setContinuousLayout(true);

		// Top pane for displaying audio path.
		pathField = new JTextField("File > Import MP3");
		pathField.setEditable(false);

		JPanel projectOptionsPanel = new JPanel();
		projectOptionsPanel.setLayout(new BoxLayout(projectOptionsPanel, BoxLayout.LINE_AXIS));
		projectOptionsPanel.add(new JLabel("Audio File Path:"));
		projectOptionsPanel.add(pathField);

		// Audio control panel.
		JPanel audioControlPanel = new JPanel();
		audioControlPanel.setLayout(new BoxLayout(audioControlPanel, BoxLayout.PAGE_AXIS));

		final JFXPanel fxPanel = new JFXPanel();

		// Buttons for play back control.
		JButton play = new JButton("Play/Pause");
		JButton stop = new JButton("Stop");
		JButton set = new JButton("Set");
		JButton resetAll = new JButton("Reset All");

		playbackTimeLabel = new JLabel("0:00/0:00");

		// Buttons for adjustment controls.
		JButton nudgeLeft = new JButton("<");
		JButton nudgeRight = new JButton(">");
		JButton nudgeAllLeft = new JButton("< All");
		JButton nudgeAllRight = new JButton("> All");

		JTextField nudgeAmount = new JTextField("20");

		JPanel syncPanel = new JPanel();
		syncPanel.add(playbackTimeLabel);
		syncPanel.add(play);
		syncPanel.add(stop);
		syncPanel.add(set);
		syncPanel.add(resetAll);

		syncPanel.add(nudgeLeft);
		syncPanel.add(nudgeRight);
		syncPanel.add(nudgeAllLeft);
		syncPanel.add(nudgeAllRight);
		syncPanel.add(nudgeAmount);

		audioControlPanel.add(syncPanel);
		audioControlPanel.add(fxPanel);

		// Set minimum sizes.
		lyricsInputPanel.setMinimumSize(minSize);
		syncPanel.setMinimumSize(minSize);
		fxPanel.setMinimumSize(minSize);

		// Main panel with all contents.
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(BorderLayout.NORTH, projectOptionsPanel);
		mainPanel.add(BorderLayout.CENTER, splitPane);
		mainPanel.add(BorderLayout.SOUTH, audioControlPanel);

		// Creating the MenuBar and adding components.
		JMenuBar menuBar = new JMenuBar();
		JMenu mFile = new JMenu("File");
		JMenu mEdit = new JMenu("Edit");
		JMenu m2 = new JMenu("Help");
		menuBar.add(mFile);
		menuBar.add(mEdit);
		menuBar.add(m2);

		JMenuItem mImportSong = new JMenuItem(new AbstractAction("Import MP3") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				// create an object of JFileChooser class
				JFileChooser j = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

				// invoke the showsOpenDialog function to show the save dialog
				int r = j.showOpenDialog(null);

				// if the user selects a file
				if (r == JFileChooser.APPROVE_OPTION) {
					String filePath = j.getSelectedFile().getAbsolutePath();

					if (!filePath.endsWith(".mp3")) {
						JOptionPane.showMessageDialog(frame, "Please select an MP3 file");
						return;
					}

					audioFilePath = filePath;
					pathField.setText(audioFilePath);

					try {
						String outputFile = transcoder.processNonWaveFile(audioFilePath, "mp3");
						final long length = player.loadAudio(outputFile);

						lengthTimeStr = "/" + formatMicroseconds(length);

						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								waveFormPane.loadAudio(audioFilePath, length);
							}
						});
					} catch (Exception ex) {
						log.error("Main: error transcoding file");
						log.error(ex);
					}
				}
			}
		});
		JMenuItem m11 = new JMenuItem(new AbstractAction("Open Project") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Open Project");
			}
		});
		JMenuItem m22 = new JMenuItem(new AbstractAction("Save Project") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Save Project");
			}
		});
		JMenuItem m33 = new JMenuItem(new AbstractAction("Export") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Export");

				JProgressBar dialogProgressBar = new JProgressBar(0, 100);
				dialogProgressBar.setIndeterminate(false);

				JDialog progressDialog = new JDialog(frame, "Export progress", true);
				progressDialog.add(BorderLayout.CENTER, dialogProgressBar);
				progressDialog.add(BorderLayout.NORTH, new JLabel("Exporting..."));
				progressDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				progressDialog.setSize(300, 75);
				progressDialog.setLocationRelativeTo(frame);

				outputSequencer.export(new SequencerProgressChecker(progressDialog, dialogProgressBar));
				progressDialog.setVisible(true);
			}
		});

		mFile.add(mImportSong);
		mFile.add(new JSeparator());
		mFile.add(m11);
		mFile.add(m22);
		mFile.add(new JSeparator());
		mFile.add(m33);

		JMenuItem mDimensions = new JMenuItem(new AbstractAction("Video width and height") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				Dimension preferredSize = new Dimension(250, 120);

				JDialog mDialog = new JDialog(frame, "Video width and height");
				mDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				mDialog.setLocationRelativeTo(frame);
				mDialog.setPreferredSize(preferredSize);
				mDialog.setMinimumSize(preferredSize);
				JPanel gridPanel = new JPanel();
				gridPanel.setLayout(new GridLayout(2, 2));

				JTextField widthTextField = new JTextField(String.format("%d", outputSequencer.getWidth()));
				JTextField heightTextField = new JTextField(String.format("%d", outputSequencer.getHeight()));

				gridPanel.add(new JLabel("Width"));
				gridPanel.add(widthTextField);

				gridPanel.add(new JLabel("Height"));
				gridPanel.add(heightTextField);

				JButton doneButton = new JButton("Done");
				doneButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							int newWidth = Integer.parseInt(widthTextField.getText());
							int newHeight = Integer.parseInt(heightTextField.getText());

							outputSequencer.setWidth(newWidth);
							outputSequencer.setHeight(newHeight);

							log.debug("Set width and height to " + newWidth + " " + newHeight);
						} catch (NumberFormatException ex) {
							log.error("Could not parse width " + widthTextField.getText() + " or height "
									+ heightTextField.getText() + " to integers.");
							log.error(ex);
						}
						mDialog.dispose();
					}
				});

				JPanel dialogPanel = new JPanel();
				dialogPanel.setBorder(BorderFactory.createEmptyBorder(DIALOG_PADDING, DIALOG_PADDING, DIALOG_PADDING,
						DIALOG_PADDING));
				dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.PAGE_AXIS));
				dialogPanel.add(gridPanel);
				dialogPanel.add(doneButton);

				mDialog.add(dialogPanel);

				mDialog.pack();
				mDialog.setVisible(true);
			}
		});

		JMenuItem mColors = new JMenuItem(new AbstractAction("Text colors") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				JDialog dialog = new JDialog(frame, "Text colors");
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog.setLocationRelativeTo(frame);

				JPanel colorPanel = new JPanel();
				colorPanel.setBorder(BorderFactory.createEmptyBorder(DIALOG_PADDING, DIALOG_PADDING, DIALOG_PADDING,
						DIALOG_PADDING));
				colorPanel.setLayout(new GridLayout(5, 2));
				JButton normalColor = new JButton();
				JButton outlineColor = new JButton();
				JButton highlightColor = new JButton();
				JButton highlightOutlineColor = new JButton();
				JButton backgroundColor = new JButton();
				colorPanel.add(new JLabel("Normal"));
				colorPanel.add(normalColor);
				colorPanel.add(new JLabel("Outline"));
				colorPanel.add(outlineColor);
				colorPanel.add(new JLabel("Highlight"));
				colorPanel.add(highlightColor);
				colorPanel.add(new JLabel("Highlight Outline"));
				colorPanel.add(highlightOutlineColor);
				colorPanel.add(new JLabel("Background"));
				colorPanel.add(backgroundColor);

				normalColor.setBorderPainted(false);
				highlightColor.setBorderPainted(false);
				outlineColor.setBorderPainted(false);
				highlightOutlineColor.setBorderPainted(false);
				backgroundColor.setBorderPainted(false);

				normalColor.setOpaque(true);
				highlightColor.setOpaque(true);
				outlineColor.setOpaque(true);
				highlightOutlineColor.setOpaque(true);
				backgroundColor.setOpaque(true);

				normalColor.setBackground(outputSequencer.getNormal());
				outlineColor.setBackground(outputSequencer.getOutline());
				highlightColor.setBackground(outputSequencer.getHighlight());
				highlightOutlineColor.setBackground(outputSequencer.getHighlightOutline());
				backgroundColor.setBackground(outputSequencer.getBackground());

				normalColor.setForeground(Color.DARK_GRAY);
				outlineColor.setForeground(Color.DARK_GRAY);
				highlightColor.setForeground(Color.DARK_GRAY);
				highlightOutlineColor.setForeground(Color.DARK_GRAY);
				backgroundColor.setForeground(Color.DARK_GRAY);

				normalColor.addActionListener(evt -> {
					Color newColor = JColorChooser.showDialog(frame, "Choose normal color",
							normalColor.getBackground());
					if (newColor != null) {
						normalColor.setBackground(newColor);
						outputSequencer.setNormal(newColor);
					}
				});

				outlineColor.addActionListener(evt -> {
					Color newColor = JColorChooser.showDialog(frame, "Choose outline color",
							outlineColor.getBackground());
					if (newColor != null) {
						outlineColor.setBackground(newColor);
						outputSequencer.setOutline(newColor);
					}
				});

				highlightColor.addActionListener(evt -> {
					Color newColor = JColorChooser.showDialog(frame, "Choose highlight color",
							highlightColor.getBackground());
					if (newColor != null) {
						highlightColor.setBackground(newColor);
						outputSequencer.setHighlight(newColor);
					}
				});

				highlightOutlineColor.addActionListener(evt -> {
					Color newColor = JColorChooser.showDialog(frame, "Choose highlight outline color",
							highlightOutlineColor.getBackground());
					if (newColor != null) {
						highlightOutlineColor.setBackground(newColor);
						outputSequencer.setHighlightOutline(newColor);
					}
				});

				backgroundColor.addActionListener(evt -> {
					Color newColor = JColorChooser.showDialog(frame, "Choose background color",
							backgroundColor.getBackground());
					if (newColor != null) {
						backgroundColor.setBackground(newColor);
						outputSequencer.setBackground(newColor);
					}
				});

				dialog.add(colorPanel);
				dialog.pack();
				dialog.setVisible(true);
			}
		});

		JMenuItem mFonts = new JMenuItem(new AbstractAction("Text font and size") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				FontDialog dialog = new FontDialog(frame, "Select text font and size", true);
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog.setSelectedFont(lyricsProcessor.getOutputFont());
				dialog.setVisible(true);
				if (!dialog.isCancelSelected()) {
					lyricsProcessor.loadFont(dialog.getSelectedFont());
				}
			}
		});

		JMenuItem mAlignment = new JMenuItem(new AbstractAction("Text alignment") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String[] options = new String[] { "Center", "Left" };
				String selectedValue = (String) JOptionPane.showInputDialog(frame, "Set alignment to",
						"Video text alignment", JOptionPane.PLAIN_MESSAGE, null, options,
						outputSequencer.getAlignment());

				if ((selectedValue != null) && (selectedValue.length() > 0)) {
					outputSequencer.setAlignment(selectedValue);
				}
			}
		});

		JMenuItem mFPS = new JMenuItem(new AbstractAction("Frames per second") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String[] options = new String[] { "24:1", "25:1", "30:1", "60:1", "12:1", "15:1" };
				String selectedValue = (String) JOptionPane.showInputDialog(frame, "Set FPS to",
						"Video frames per second", JOptionPane.PLAIN_MESSAGE, null, options, outputSequencer.getFPS());

				if ((selectedValue != null) && (selectedValue.length() > 0)) {
					outputSequencer.setFPS(selectedValue);
				}
			}
		});

		JMenuItem mLines = new JMenuItem(new AbstractAction("Number of lines") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				String[] options = new String[] { "1", "2" };
				String selectedValue = (String) JOptionPane.showInputDialog(frame, "Set number of lines to",
						"Number of lines", JOptionPane.PLAIN_MESSAGE, null, options,
						outputSequencer.getNumberOfLines());

				if ((selectedValue != null) && (selectedValue.length() > 0)) {
					outputSequencer.setNumberOfLines(selectedValue);
				}
			}
		});

		mEdit.add(mDimensions);
		mEdit.add(new JSeparator());
		mEdit.add(mFPS);
		mEdit.add(new JSeparator());
		mEdit.add(mFonts);
		mEdit.add(mAlignment);
		mEdit.add(mColors);
		mEdit.add(new JSeparator());
		mEdit.add(mLines);

		frame.getContentPane().add(BorderLayout.NORTH, menuBar);
		frame.getContentPane().add(mainPanel);

		frame.setVisible(true);

		lyricsTextArea.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				lyricsProcessor.loadLyrics(lyricsTextArea.getText(), (String) splitComboBox.getSelectedItem());
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				lyricsProcessor.loadLyrics(lyricsTextArea.getText(), (String) splitComboBox.getSelectedItem());
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		});

		splitComboBox.addItemListener(
				ev -> lyricsProcessor.loadLyrics(lyricsTextArea.getText(), (String) splitComboBox.getSelectedItem()));

		play.addActionListener(ev -> player.playOrPause());

		stop.addActionListener(ev -> {
			player.stop();
			waveFormPane.stop();
			lyricsProcessor.setPlaybackPosition(0);
		});

		nudgeLeft.addActionListener(ev -> lyricsProcessor.nudge(nudgeAmount.getText(), false, true));
		nudgeRight.addActionListener(ev -> lyricsProcessor.nudge(nudgeAmount.getText(), false, false));
		nudgeAllLeft.addActionListener(ev -> lyricsProcessor.nudge(nudgeAmount.getText(), true, true));
		nudgeAllRight.addActionListener(ev -> lyricsProcessor.nudge(nudgeAmount.getText(), true, false));

		resetAll.addActionListener(ev -> {
			int n = JOptionPane.showConfirmDialog(frame, "Reset all synchronization markers?", "Reset all",
					JOptionPane.YES_NO_OPTION);
			if (n == 0) {
				lyricsProcessor.resetAllSyncMarkers();
			}
		});

		set.addActionListener(ev -> {
			long currentTimestamp = player.getPlaybackPosition();
			if (currentTimestamp < 0) {
				return;
			}
			lyricsProcessor.setTimestampForCurrentWordAndMoveToNext(currentTimestamp);
		});

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				initFX(fxPanel);
				timerService.start();
				timerService.setPlaying(true);
			}
		});
	}

	private static void initFX(JFXPanel fxPanel) {
		// This method is invoked on the JavaFX thread
		Scene scene = createScene();
		fxPanel.setScene(scene);
	}

	private static Scene createScene() {
		waveFormPane = new WaveFormPane(520, 32, waveReactor);

		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(waveFormPane);
		borderPane.boundsInLocalProperty().addListener(l -> {
			waveFormPane.setWidth(borderPane.getWidth());
			waveFormPane.setHeight(borderPane.getHeight());
		});

		Scene scene = new Scene(borderPane, 600, 40);
		return (scene);
	}

	private static class WaveReactor implements WaveListener {
		private AudioPlayer audioPlayer;
		private LyricsProcessor processor;

		public WaveReactor(AudioPlayer audioPlayer, LyricsProcessor processor) {
			this.audioPlayer = audioPlayer;
			this.processor = processor;
		}

		@Override
		public void setCurrentPosition(double x, double width) {
			System.out.println(x);
			audioPlayer.setPosition(x / width);

			processor.setPlaybackPosition(audioPlayer.getPlaybackPosition());
		}
	}

	private static String formatMicroseconds(long microseconds) {
		Duration d = Duration.ofMillis(microseconds / 1000);
		return String.format("%d:%02d", d.toMinutesPart(), d.toSecondsPart());
	}

	private static class IntervalUpdater implements TimerListener {
		@Override
		public void update(long diff) {
			long playbackPosition = player.getPlaybackPosition();
			if (playbackPosition < 0) {
				return;
			}

			playbackTimeLabel.setText(formatMicroseconds(playbackPosition) + lengthTimeStr);
			waveFormPane.update(playbackPosition);
			lyricsProcessor.update(playbackPosition);
		}
	}

	private static class SequencerProgressChecker implements SequencerListener {
		private JDialog dialog;
		private JProgressBar progressBar;

		public SequencerProgressChecker(JDialog dialog, JProgressBar progressBar) {
			this.dialog = dialog;
			this.progressBar = progressBar;
		}

		@Override
		public void done() {
			System.out.println("SequencerProgressChecker done!");
			dialog.setVisible(false);
		}

		@Override
		public void setProgress(double progress) {
			progressBar.setValue((int) (100 * progress));
		}
	}
}
