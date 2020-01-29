package karaoke;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.*;

import java.awt.event.*;
import java.time.Duration;
import java.util.Hashtable;

import javax.swing.filechooser.*;

import org.drjekyll.fontchooser.FontDialog;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import karaoke.TimerService.TimerListener;
import karaoke.WaveFormPane.WaveListener;

public class Main {
	private static JPanel displayPanel = new JPanel();

	private static JTextField pathField;
	private static JLabel playbackTimeLabel;

	private static String lengthTimeStr;

	private static WaveFormPane waveFormPane;

	private static String audioFilePath;

	public static final AudioPlayer player = new AudioPlayer();
	private static final Transcoder transcoder = new Transcoder();

	private static final WaveReactor waveReactor = new WaveReactor(player);

	private static final IntervalUpdater intervalUpdater = new IntervalUpdater();
	private static final TimerService timerService = new TimerService(intervalUpdater);

	private static final LyricsProcessor lyricsProcessor = new LyricsProcessor(displayPanel, player);

	private static final OutputSequencer outputSequencer = new OutputSequencer();

	public static void main(String args[]) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				initAndShowGUI();
			}
		});
	}

	private static void initAndShowGUI() {
		JFrame frame = new JFrame("Karaoke Sync");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1024, 720);

		Dimension minFrameSize = new Dimension(1024, 700);
		Dimension minSize = new Dimension(100, 50);

		frame.setMinimumSize(minFrameSize);

		// Creating the MenuBar and adding components.
		JMenuBar menuBar = new JMenuBar();
		JMenu m1 = new JMenu("File");
		JMenu m2 = new JMenu("Help");
		menuBar.add(m1);
		menuBar.add(m2);

		JMenuItem m11 = new JMenuItem("Open Project");
		JMenuItem m22 = new JMenuItem("Save Project");
		JMenuItem m33 = new JMenuItem(new AbstractAction("Export") {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Export");
				outputSequencer.sequence(lyricsProcessor, player);
			}
		});

		m1.add(m11);
		m1.add(m22);
		m1.add(m33);

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		JButton play = new JButton("Play");
		JButton stop = new JButton("Stop");
		JButton pause = new JButton("Pause");
		JButton set = new JButton("Set");
		JButton resetAll = new JButton("Reset All");
		JButton preview = new JButton("Preview");

		playbackTimeLabel = new JLabel("0:00/0:00");

		JSlider tempoSlider = new JSlider(JSlider.HORIZONTAL, 5, 20, 10);
		tempoSlider.setMajorTickSpacing(5);
		tempoSlider.setMinorTickSpacing(1);
		tempoSlider.setPaintTicks(true);
		tempoSlider.setPaintLabels(true);

		Hashtable<Integer, JLabel> labels = new Hashtable<>();
		labels.put(5, new JLabel("0.5"));
		labels.put(10, new JLabel("1"));
		labels.put(15, new JLabel("1.5"));
		labels.put(20, new JLabel("2.0"));
		tempoSlider.setLabelTable(labels);

		JPanel lyricsTopPanel = new JPanel();
		lyricsTopPanel.setLayout(new BoxLayout(lyricsTopPanel, BoxLayout.LINE_AXIS));
		JLabel lyricsLabel = new JLabel("Lyrics");
		String[] splitOptions = new String[] { "Word", "Character", "Phrase" };
		JComboBox<String> splitComboBox = new JComboBox<String>(splitOptions);
		lyricsTopPanel.add(lyricsLabel);
		lyricsTopPanel.add(Box.createHorizontalStrut(10));
		lyricsTopPanel.add(splitComboBox);

		JTextArea lyricsTextArea = new JTextArea();
		lyricsTextArea.setColumns(40);
		lyricsTextArea.setRows(20);
		lyricsTextArea.setLineWrap(true);

		JScrollPane lyricsScrollPane = new JScrollPane(lyricsTextArea);
		lyricsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		lyricsScrollPane.setPreferredSize(new Dimension(250, 250));

		displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.PAGE_AXIS));

		JScrollPane displayScrollPane = new JScrollPane(displayPanel);
		displayScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		displayScrollPane.setPreferredSize(new Dimension(250, 250));
		displayScrollPane.getVerticalScrollBar().setUnitIncrement(16);

		JPanel lyricsInputPanel = new JPanel();
		lyricsInputPanel.setLayout(new BoxLayout(lyricsInputPanel, BoxLayout.PAGE_AXIS));
		lyricsInputPanel.add(lyricsScrollPane);
		lyricsInputPanel.add(Box.createVerticalStrut(5));
		lyricsInputPanel.add(lyricsTopPanel);
		lyricsInputPanel.add(Box.createVerticalStrut(5));

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, lyricsInputPanel, displayScrollPane);
		splitPane.setContinuousLayout(true);

		JTextField widthTextField = new JTextField("1920");
		JTextField heightTextField = new JTextField("1080");

		JPanel widthPanel = new JPanel();
		widthPanel.setLayout(new BoxLayout(widthPanel, BoxLayout.LINE_AXIS));
		widthPanel.add(new JLabel("Width"));
		widthPanel.add(widthTextField);

		JPanel heightPanel = new JPanel();
		heightPanel.setLayout(new BoxLayout(heightPanel, BoxLayout.LINE_AXIS));
		heightPanel.add(new JLabel("Height"));
		heightPanel.add(heightTextField);

		JPanel fpsPanel = new JPanel();
		fpsPanel.setLayout(new BoxLayout(fpsPanel, BoxLayout.LINE_AXIS));
		String[] fpsOptions = new String[] { "24:1", "25:1", "30:1", "60:1" };
		JComboBox<String> fpsComboBox = new JComboBox<String>(fpsOptions);
		fpsPanel.add(new JLabel("FPS"));
		fpsPanel.add(fpsComboBox);

		JPanel dimensionsPanel = new JPanel();
		dimensionsPanel.setLayout(new BoxLayout(dimensionsPanel, BoxLayout.PAGE_AXIS));
		dimensionsPanel.add(widthPanel);
		dimensionsPanel.add(heightPanel);
		dimensionsPanel.add(fpsPanel);

		JButton browseSong = new JButton("Browse Song");
		JLabel pathLabel = new JLabel("Path");
		pathField = new JTextField("");
		pathField.setMinimumSize(new Dimension(20, 12));

		JPanel projectOptionsPanel = new JPanel();
		projectOptionsPanel.setLayout(new BoxLayout(projectOptionsPanel, BoxLayout.LINE_AXIS));
		projectOptionsPanel.add(browseSong);
		projectOptionsPanel.add(pathLabel);
		projectOptionsPanel.add(pathField);

		JButton browseFont = new JButton("Browse Font");
		JLabel fontPathLabel = new JLabel("Font");
		JTextField fontPathField = new JTextField("");
		fontPathField.setMinimumSize(new Dimension(20, 12));

		JPanel fontOptionsPanel = new JPanel();
		fontOptionsPanel.setLayout(new BoxLayout(fontOptionsPanel, BoxLayout.LINE_AXIS));
		fontOptionsPanel.add(browseFont);
		fontOptionsPanel.add(fontPathLabel);
		fontOptionsPanel.add(fontPathField);

		JPanel colorOptionsPanel = new JPanel();
		colorOptionsPanel.setLayout(new BoxLayout(colorOptionsPanel, BoxLayout.PAGE_AXIS));
		JPanel colorInsidePanel = new JPanel();
		colorInsidePanel.setLayout(new GridLayout(3, 2));
		JButton normalColor = new JButton("Normal");
		JButton outlineColor = new JButton("Normal Outline");
		JButton highlightColor = new JButton("Highlight");
		JButton highlightOutlineColor = new JButton("Highlight Outline");
		JButton backgroundColor = new JButton("Background");
		colorInsidePanel.add(normalColor);
		colorInsidePanel.add(outlineColor);
		colorInsidePanel.add(highlightColor);
		colorInsidePanel.add(highlightOutlineColor);
		colorInsidePanel.add(backgroundColor);

		colorOptionsPanel.add(new JLabel("Colors:"));
		colorOptionsPanel.add(colorInsidePanel);

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

		normalColor.setBackground(Color.white);
		outlineColor.setBackground(Color.black);
		highlightColor.setBackground(Color.blue);
		highlightOutlineColor.setBackground(Color.white);
		backgroundColor.setBackground(Color.green);

		outputSequencer.setNormal(normalColor.getBackground());
		outputSequencer.setOutline(outlineColor.getBackground());
		outputSequencer.setHighlight(highlightColor.getBackground());
		outputSequencer.setHighlightOutline(highlightOutlineColor.getBackground());
		outputSequencer.setBackground(backgroundColor.getBackground());

		normalColor.setForeground(Color.DARK_GRAY);
		outlineColor.setForeground(Color.DARK_GRAY);
		highlightColor.setForeground(Color.DARK_GRAY);
		highlightOutlineColor.setForeground(Color.DARK_GRAY);
		backgroundColor.setForeground(Color.DARK_GRAY);

		JPanel projectPanel = new JPanel();
		projectPanel.setLayout(new BoxLayout(projectPanel, BoxLayout.PAGE_AXIS));
		projectPanel.add(projectOptionsPanel);
		projectPanel.add(fontOptionsPanel);

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new GridLayout(0, 3));
		topPanel.add(projectPanel);
		topPanel.add(dimensionsPanel);
		topPanel.add(colorOptionsPanel);

		JPanel audioControlPanel = new JPanel();
		audioControlPanel.setLayout(new BoxLayout(audioControlPanel, BoxLayout.PAGE_AXIS));

		final JFXPanel fxPanel = new JFXPanel();
		fxPanel.setMinimumSize(minSize);

		JPanel syncPanel = new JPanel();
		syncPanel.add(playbackTimeLabel);
		syncPanel.add(play);
		syncPanel.add(pause);
		syncPanel.add(stop);
		syncPanel.add(set);
		syncPanel.add(resetAll);
		syncPanel.add(preview);

		syncPanel.add(tempoSlider);

		lyricsInputPanel.setMinimumSize(minSize);
		syncPanel.setMinimumSize(minSize);

		audioControlPanel.add(syncPanel);
		audioControlPanel.add(fxPanel);

		mainPanel.add(BorderLayout.NORTH, topPanel);
		mainPanel.add(BorderLayout.CENTER, splitPane);
		mainPanel.add(BorderLayout.SOUTH, audioControlPanel);

		frame.getContentPane().add(BorderLayout.NORTH, menuBar);
		frame.getContentPane().add(mainPanel);

		frame.setVisible(true);

		splitComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				lyricsProcessor.loadLyrics(lyricsTextArea.getText(), (String) splitComboBox.getSelectedItem());
			}
		});

		fpsComboBox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				outputSequencer.setFPS((String) splitComboBox.getSelectedItem());
			}
		});

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

		widthTextField.getDocument().addDocumentListener(new DocumentListener() {
			public void setWidth() {
				int width;
				try {
					width = Integer.parseInt(widthTextField.getText());

				} catch (NumberFormatException ex) {
					width = 1920;
				}
				outputSequencer.setWidth(width);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				setWidth();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				setWidth();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		});

		heightTextField.getDocument().addDocumentListener(new DocumentListener() {
			public void setHeight() {
				int height;
				try {
					height = Integer.parseInt(heightTextField.getText());

				} catch (NumberFormatException ex) {
					height = 1080;
				}
				outputSequencer.setWidth(height);
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				setHeight();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				setHeight();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
		});

		tempoSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				System.out.println("Slider: " + tempoSlider.getValue() / 10.0);
			}
		});

		play.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				player.play();
			}
		});

		pause.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				player.pause();
			}
		});

		stop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				player.stop();
				waveFormPane.stop();
			}
		});

		resetAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int n = JOptionPane.showConfirmDialog(frame, "Reset all synchronization markers?", "Reset all",
						JOptionPane.YES_NO_OPTION);
				System.out.println(n);
				if (n == 0) {
					lyricsProcessor.resetAllSyncMarkers();
				}
			}
		});

		set.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				long currentTimestamp = player.getPlaybackPosition();
				if (currentTimestamp < 0) {
					return;
				}
				lyricsProcessor.setTimestampForCurrentWord(currentTimestamp);
			}
		});

		browseSong.addActionListener(new ActionListener() {
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
						System.err.println("Error transcoding file");
						ex.printStackTrace();
					}
				}
			}
		});

		browseFont.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FontDialog dialog = new FontDialog((Frame) null, "Select Font", true);
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				dialog.setSelectedFont(lyricsProcessor.getSelectedFont());
				dialog.setVisible(true);
				if (!dialog.isCancelSelected()) {
					System.out.printf("Selected font is: %s%n", dialog.getSelectedFont());
					lyricsProcessor.loadFont(dialog.getSelectedFont());
					fontPathField.setText(dialog.getSelectedFont().getName());

					outputSequencer.setFont(dialog.getSelectedFont());
				}
			}
		});

		normalColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(null, "Choose normal color", normalColor.getBackground());
				if (newColor != null) {
					normalColor.setBackground(newColor);
					outputSequencer.setNormal(newColor);
				}
			}
		});

		outlineColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(null, "Choose normal outline color",
						outlineColor.getBackground());
				if (newColor != null) {
					outlineColor.setBackground(newColor);
					outputSequencer.setOutline(newColor);
				}
			}
		});

		highlightColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(null, "Choose highlight color",
						highlightColor.getBackground());
				if (newColor != null) {
					highlightColor.setBackground(newColor);
					outputSequencer.setHighlight(newColor);
				}
			}
		});

		highlightOutlineColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(null, "Choose highlight outline color",
						highlightOutlineColor.getBackground());
				if (newColor != null) {
					highlightOutlineColor.setBackground(newColor);
					outputSequencer.setHighlightOutline(newColor);
				}
			}
		});

		backgroundColor.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Color newColor = JColorChooser.showDialog(null, "Choose background color",
						backgroundColor.getBackground());
				if (newColor != null) {
					backgroundColor.setBackground(newColor);
					outputSequencer.setBackground(newColor);
				}
			}
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

		public WaveReactor(AudioPlayer audioPlayer) {
			this.audioPlayer = audioPlayer;
		}

		@Override
		public void setCurrentPosition(double x, double width) {
			System.out.println(x);
			audioPlayer.setPosition(x / width);
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

}
