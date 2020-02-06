package karaoke;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import karaoke.LyricsProcessor.IndexSelectListener;

public class AudioPlayer implements LineListener, IndexSelectListener {
	private Clip audioClip;
	
	AudioListener listener;
	
	public AudioPlayer(AudioListener listener) {
		this.listener = listener;
	}

	public long loadAudio(String audioFilePath) {
		checkAndCloseAudio();

		try {
			File audioFile = new File(audioFilePath);
			AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
			AudioFormat format = audioStream.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);

			audioClip = (Clip) AudioSystem.getLine(info);

			audioClip.addLineListener(this);
			audioClip.open(audioStream);

			return audioClip.getMicrosecondLength();
		} catch (UnsupportedAudioFileException e) {
			System.out.println("The specified audio file is not supported.");
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			System.out.println("Audio line for playing back is unavailable.");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error playing the audio file.");
			e.printStackTrace();
		}
		return -1;
	}

	public boolean playOrPause() {
		if (audioClip == null) {
			return false;
		}
		if (audioClip.isRunning()) {
			System.out.println("Audio player pause.");
			audioClip.stop();
			return false;
		} else {
			System.out.println("Audio player play.");
			audioClip.start();
			return true;
		}
	}

	public void stop() {
		if (audioClip == null) {
			return;
		}
		System.out.println("Audio player stop.");
		audioClip.stop();
		audioClip.setMicrosecondPosition(0);
	}

	public void setRate(float rate) {
		if (audioClip == null) {
			return;
		}
	}

	public void setPosition(double position) {
		if (audioClip == null) {
			return;
		}
		audioClip.setFramePosition((int) (audioClip.getFrameLength() * position));
	}

	@Override
	public void update(LineEvent event) {
		LineEvent.Type type = event.getType();
		if (type == LineEvent.Type.START) {
			System.out.println("Playback started.");
		} else if (type == LineEvent.Type.STOP) {
			System.out.println("Playback completed.");
			listener.stopped();
		}
	}

	private void checkAndCloseAudio() {
		if (audioClip != null) {
			audioClip.close();
			audioClip = null;
		}
	}

	public long getLength() {
		if (audioClip == null) {
			return -1;
		}
		System.out.println("Audio player get length.");
		return audioClip.getMicrosecondLength();
	}

	public long getPlaybackPosition() {
		if (audioClip == null) {
			return -1;
		}
		return audioClip.getMicrosecondPosition();
	}

	@Override
	public void selectedPlaybackPosition(long playbackPosition) {
		if (audioClip == null) {
			return;
		}
		audioClip.setMicrosecondPosition(playbackPosition);
	}
	
	public interface AudioListener {
		public void stopped();
	}
}