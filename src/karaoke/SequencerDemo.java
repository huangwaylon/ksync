package karaoke;

import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Stanislav Vitvitskyy
 * 
 */
public class SequencerDemo {
//	private static final Flag FLAG_FPS = Flag.flag("fps", "fps",
//			"Rational fps, i.e 25:1 (for 25fps), 30:1 (for 30fps), 30000:1001 (for 29.97fps), etc.");
//	private static final Flag FLAG_FRAMES = Flag.flag("num-frames", "num-frames", "Maximum frames to decode.");
//	private static final Flag FLAG_PATTERN = Flag.flag("input-pattern", "input-pattern",
//			"Input folder/frame%04.png pattern.");
//	private static final Flag[] FLAGS = new MainUtils.Flag[] { FLAG_FPS, FLAG_FRAMES, FLAG_PATTERN };

	public static void main(String[] args) throws IOException {
//		Cmd cmd = MainUtils.parseArguments(args, FLAGS);
//		if (cmd.argsLength() < 1) {
//			MainUtils.printHelpArgs(FLAGS, new String[] { "output file name" });
//			return;
//		}

		int maxFrames = 121;
		String fpsRaw = "25:1";
		String outDir = new File("/Users/waylonh/Downloads/test/image%08d.png").getAbsolutePath();
		FileChannelWrapper out = null;
		try {
			out = NIOUtils.writableChannel(MainUtils.tildeExpand("/Users/waylonh/Downloads/out.mp4"));
			AWTSequenceEncoder encoder = new AWTSequenceEncoder(out, Rational.parse(fpsRaw));
			int i = 0;
			for (; i < maxFrames; i++) {
				System.out.println("Iteration: " + i);

				File file = new File(String.format(outDir, i));
				if (!file.exists()) {
					System.out.println(file.getAbsolutePath());
					break;
				}
				BufferedImage image = ImageIO.read(file);
				encoder.encodeImage(image);
			}
			if (i > 0) {
				encoder.finish();
			} else {
				System.out.println("No frames output.");
			}
		} finally {
			NIOUtils.closeQuietly(out);
		}
	}
}