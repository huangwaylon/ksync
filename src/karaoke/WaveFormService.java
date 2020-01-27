package karaoke;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.util.Random;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import ws.schild.jave.AudioAttributes;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.EncoderProgressListener;
import ws.schild.jave.EncodingAttributes;
import ws.schild.jave.MultimediaInfo;
import ws.schild.jave.MultimediaObject;

public class WaveFormService extends Service<Boolean> {
	private static final double WAVEFORM_HEIGHT_COEFFICIENT = 1.3; // This fits the waveform to the swing node height
	private static final CopyOption[] OPTIONS = new CopyOption[] { COPY_ATTRIBUTES, REPLACE_EXISTING };

	private final WaveServiceListener waveServiceListener;
	private final ConvertProgressListener progressListener;

	private final Encoder encoder;

	private float[] resultingWaveData;
	private int[] wavAmplitudes;

	private int currentWidth;
	private String filePath;

	public WaveFormService(WaveServiceListener waveServiceListener) {
		this.waveServiceListener = waveServiceListener;

		encoder = new Encoder();
		progressListener = new ConvertProgressListener();

		setOnSucceeded(s -> done());
		setOnFailed(f -> failure());
		setOnCancelled(c -> failure());
	}

	public void loadAudio(String filePath) {
		System.out.println("WaveFromService load audio " + filePath);
		this.filePath = filePath;

		wavAmplitudes = null;
	}

	public void startService(int width) {
		System.out.println("WaveFromService start.");
		if (filePath == null) {
			return;
		}
		
		this.currentWidth = width;
		
		restart();
	}

	@Override
	protected Task<Boolean> createTask() {
		return new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				try {
					if (wavAmplitudes != null) {
						resultingWaveData = processFromWavAmplitudes(wavAmplitudes);
					} else {
						resultingWaveData = processFromNonWavFile("mp3");
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					return false;
				}
				return true;
			}

			private float[] processFromNonWavFile(String fileFormat)
					throws IOException, UnsupportedAudioFileException, EncoderException {
				int randomN = new Random().nextInt(99999);

				File temporalDecodedFile = File.createTempFile("decoded_" + randomN, ".wav");
				File temporalCopiedFile = File.createTempFile("original_" + randomN, "." + fileFormat);

				// Delete temporary Files on exit.
				temporalDecodedFile.deleteOnExit();
				temporalCopiedFile.deleteOnExit();

				// Create a temporary path.
				Files.copy(new File(filePath).toPath(), temporalCopiedFile.toPath(), OPTIONS);

				// Transcode to .wav.
				transcodeToWav(temporalCopiedFile, temporalDecodedFile);

				// Avoid creating amplitudes again for the same file.
				if (wavAmplitudes == null) {
					wavAmplitudes = getWavAmplitudes(temporalDecodedFile);
				}

				// Delete temporary files.
				temporalDecodedFile.delete();
				temporalCopiedFile.delete();

				return processFromWavAmplitudes(wavAmplitudes);
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

					encoder.encode(new MultimediaObject(sourceFile), destinationFile, attributes, progressListener);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}

			private int[] getWavAmplitudes(File file) throws UnsupportedAudioFileException, IOException {
				System.out.println("Calculating WAV amplitudes.");

				try (AudioInputStream input = AudioSystem.getAudioInputStream(file)) {
					AudioFormat baseFormat = input.getFormat();

					Encoding encoding = AudioFormat.Encoding.PCM_UNSIGNED;
					float sampleRate = baseFormat.getSampleRate();
					int numChannels = baseFormat.getChannels();

					AudioFormat decodedFormat = new AudioFormat(encoding, sampleRate, 16, numChannels, numChannels * 2,
							sampleRate, false);
					int available = input.available();

					// Get the PCM Decoded Audio Input Stream.
					try (AudioInputStream pcmDecodedInput = AudioSystem.getAudioInputStream(decodedFormat, input)) {
						final int BUFFER_SIZE = 4096; // this is actually bytes
						byte[] buffer = new byte[BUFFER_SIZE];

						// Now get the average to a smaller array.
						int maximumArrayLength = 100000;
						int[] finalAmplitudes = new int[maximumArrayLength];
						int samplesPerPixel = available / maximumArrayLength;

						// Variables to calculate finalAmplitudes array.
						int currentSampleCounter = 0;
						int arrayCellPosition = 0;
						float currentCellValue = 0.0f;

						// Read all the available data on chunks.
						while (pcmDecodedInput.readNBytes(buffer, 0, BUFFER_SIZE) > 0)
							for (int i = 0; i < buffer.length - 1; i += 2) {

								// Calculate the value.
								int arrayCellValue = (int) (((((buffer[i + 1] << 8) | buffer[i] & 0xff) << 16) / 32767)
										* WAVEFORM_HEIGHT_COEFFICIENT);

								if (currentSampleCounter != samplesPerPixel) {
									currentSampleCounter++;
									currentCellValue += Math.abs(arrayCellValue);
								} else {
									// Avoid ArrayIndexOutOfBoundsException
									if (arrayCellPosition != maximumArrayLength)
										finalAmplitudes[arrayCellPosition] = finalAmplitudes[arrayCellPosition
												+ 1] = (int) currentCellValue / samplesPerPixel;

									currentSampleCounter = 0;
									currentCellValue = 0;
									arrayCellPosition += 2;
								}
							}
						return finalAmplitudes;
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				// Error if we've reached this point.
				return null;
			}

			private float[] processFromWavAmplitudes(int[] sourcePcmData) {
				System.out.println("Processing from WAV amplitudes.");

				float[] waveData = new float[currentWidth];
				int samplesPerPixel = sourcePcmData.length / currentWidth;

				for (int w = 0; w < currentWidth; w++) {
					int currentIndex = w * samplesPerPixel;

					float nValue = 0.0f;
					for (int sample = 0; sample < samplesPerPixel; sample++) {
						nValue += (Math.abs(sourcePcmData[currentIndex + sample]) / 65536.0f);
					}
					waveData[w] = nValue / samplesPerPixel;
				}
				return waveData;
			}
		};
	}

	public void done() {
		waveServiceListener.waveDataComplete(resultingWaveData);
	}

	private void failure() {
	}

	public class ConvertProgressListener implements EncoderProgressListener {
		int current = 1;

		public ConvertProgressListener() {
		}

		public void message(String m) {
		}

		public void progress(int p) {
			double progress = p / 1000.00;
			System.out.println(progress);
		}

		public void sourceInfo(MultimediaInfo m) {
		}
	}

	public interface WaveServiceListener {
		public void waveDataComplete(float[] waveData);
	}
}
