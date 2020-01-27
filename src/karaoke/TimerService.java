package karaoke;

import javafx.animation.AnimationTimer;

public class TimerService extends AnimationTimer {
	private TimerListener timerListener;

	private long previousNanos = 0;
	private boolean isPlaying;

	public TimerService(TimerListener timerListener) {
		this.timerListener = timerListener;
	}

	@Override
	public void handle(long nanos) {
		// Every 300 millisecond update
		if (isPlaying && nanos >= previousNanos + 1e8) {
			long diff = nanos - previousNanos;
			previousNanos = nanos;
			timerListener.update(diff);
		}
	}

	@Override
	public void start() {
		super.start();
	}

	@Override
	public void stop() {
		super.stop();
	}

	public void setPlaying(boolean isPlaying) {
		this.isPlaying = isPlaying;
	}

	public interface TimerListener {
		public void update(long diff);
	}
}