package karaoke;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.util.Random;

import javax.sound.sampled.UnsupportedAudioFileException;

import ws.schild.jave.AudioAttributes;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.EncoderProgressListener;
import ws.schild.jave.EncodingAttributes;
import ws.schild.jave.MultimediaInfo;
import ws.schild.jave.MultimediaObject;

public class Transcoder {
	private static final CopyOption[] OPTIONS = new CopyOption[] { COPY_ATTRIBUTES, REPLACE_EXISTING };

	private Encoder encoder = new Encoder();
	private ConvertProgressListener listener = new ConvertProgressListener();

	private File tempFile;

	public String processNonWaveFile(String fileAbsolutePath, String fileFormat)
			throws IOException, UnsupportedAudioFileException, EncoderException {
		Random random = new Random();
		int randomN = random.nextInt(99999);

		// Create temporary files.
		File temporalDecodedFile = File.createTempFile("decoded_" + randomN, ".wav");
		File temporalCopiedFile = File.createTempFile("original_" + randomN, "." + fileFormat);
		tempFile = temporalDecodedFile;

		// Delete temporary files on exit.
		temporalDecodedFile.deleteOnExit();
		temporalCopiedFile.deleteOnExit();

		// Create a temporary path.
		Files.copy(new File(fileAbsolutePath).toPath(), temporalCopiedFile.toPath(), OPTIONS);

		// Transcode to .wav.
		transcodeToWav(temporalCopiedFile, temporalDecodedFile);

		// Delete temporary files.
		temporalCopiedFile.delete();
		return temporalDecodedFile.getAbsolutePath();
	}

	private void transcodeToWav(File sourceFile, File destinationFile) throws EncoderException {
		try {
			AudioAttributes audio = new AudioAttributes();
			audio.setCodec("pcm_s16le");
			audio.setChannels(2);
			audio.setSamplingRate(44100);

			EncodingAttributes attributes = new EncodingAttributes();
			attributes.setFormat("wav");
			attributes.setAudioAttributes(audio);

			encoder.encode(new MultimediaObject(sourceFile), destinationFile, attributes, listener);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteTempFile() {
		if (tempFile != null) {
			tempFile.delete();
			tempFile = null;
		}
	}

	private static class ConvertProgressListener implements EncoderProgressListener {
		public ConvertProgressListener() {
		}

		public void progress(int p) {
			double progress = p / 1000.00;
			System.out.println(progress);
		}

		public void message(String m) {
		}

		public void sourceInfo(MultimediaInfo m) {
		}
	}
}