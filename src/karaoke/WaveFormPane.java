package karaoke;

import javafx.scene.paint.Color;
import karaoke.TimerService.TimerListener;
import karaoke.WaveFormService.WaveServiceListener;

public class WaveFormPane extends ResizableCanvas implements TimerListener, WaveServiceListener {
	private float[] waveData;

	private final Color backgroundColor = Color.web("#252525");
	private final Color foregroundColor = Color.ORANGE;
	private final Color transparentForeground;
	private final Color mouseXColor = Color.rgb(255, 255, 255, 0.7);

	private int width, height;

	private int playbackPosition = 0;
	private int mouseXPosition = -1;

	private final WaveFormService waveService;

	private WaveListener waveListener;

	private long audioFileLength;

	public WaveFormPane(int width, int height, WaveListener waveListener) {
		this.width = width;
		this.height = height;
		this.waveListener = waveListener;

		this.setWidth(width);
		this.setHeight(height);

		waveService = new WaveFormService(this);

		waveData = new float[width];

		transparentForeground = Color.rgb((int) (foregroundColor.getRed() * 255),
				(int) (foregroundColor.getGreen() * 255), (int) (foregroundColor.getBlue() * 255), 0.3);

		widthProperty().addListener((observable, oldValue, newValue) -> {
			this.width = Math.round(newValue.floatValue());
			clear();
			recomputeWaveForm();
		});

		heightProperty().addListener((observable, oldValue, newValue) -> {
			this.height = Math.round(newValue.floatValue());
			clear();
			recomputeWaveForm();
		});

		setOnMouseMoved(m -> this.setMouseXPosition((int) m.getX()));
		setOnMouseDragged(m -> this.setMouseXPosition((int) m.getX()));
		setOnMouseExited(m -> this.setMouseXPosition(-1));
		setOnMouseClicked(m -> {
			this.waveListener.setCurrentPosition(m.getX(), this.getWidth());
			playbackPosition = (int) m.getX();
		});
	}

	public void stop() {
		playbackPosition = 0;
	}

	public void clear() {
		// Draw a background rectangle.
		gc.setFill(backgroundColor);
		gc.fillRect(0, 0, width, height);

		// Draw a horizontal line.
		gc.setStroke(foregroundColor);
		gc.strokeLine(0, height / 2, width, height / 2);
	}

	public void drawWaveForm() {
		// Draw a background rectangle.
		gc.setFill(backgroundColor);
		gc.fillRect(0, 0, width, height);

		// Draw the waveform.
		gc.setStroke(foregroundColor);
		if (waveData != null)
			for (int i = 0; i < waveData.length; i++) {
				int value = (int) (waveData[i] * height);
				int y1 = (height - 2 * value) / 2;
				int y2 = y1 + 2 * value;
				gc.strokeLine(i, y1, i, y2);
			}

		// Draw a semi-transparent Rectangle.
		gc.setFill(transparentForeground);
		gc.fillRect(0, 0, playbackPosition, height);

		// Draw a vertical line.
		gc.setFill(Color.WHITE);
		gc.fillOval(playbackPosition, 0, 1, height);

		// Draw a vertical line.
		if (mouseXPosition != -1) {
			gc.setFill(mouseXColor);
			gc.fillRect(mouseXPosition, 0, 3, height);
		}
	}

	private void setMouseXPosition(int mouseXPosition) {
		this.mouseXPosition = mouseXPosition;
	}

	public interface WaveListener {
		public void setCurrentPosition(double x, double width);
	}

	public void loadAudio(String filePath, long length) {
		audioFileLength = length;

		waveService.loadAudio(filePath);
		recomputeWaveForm();
	}

	private void recomputeWaveForm() {
		waveService.startService(width);
	}

	public void update(long currentPlaybackPosition) {
		playbackPosition = (int) (width * ((double) currentPlaybackPosition / audioFileLength));
		drawWaveForm();
	}

	@Override
	public void waveDataComplete(float[] waveData) {
		clear();
		this.waveData = waveData;
		drawWaveForm();
	}
}
