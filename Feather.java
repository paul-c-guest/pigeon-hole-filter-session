import java.awt.Color;
import java.awt.Cursor;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Feather {

	private JFrame frame;
	private boolean readyForNext;
	private boolean requestedToExit;

	private static String PARAKEET = "./Parakeets";
	private static String FOGGY = "./Foggy";
	private static String OTHER = "./Other";
	private static String BIRD = "./Birds";
	private static String MAMMAL = "./Mammals";
	private static String EMPTY = "./Empty";

	private static int SCREEN_WIDTH, SCREEN_HEIGHT;

	public static void main(String[] args) {

		if (args.length > 0 && args[0].contains("h")) {
			displayHelp();
			return;
		}

		new Feather();
	}

	private Feather() {
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice device = env.getDefaultScreenDevice();

		frame = new JFrame();
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setUndecorated(true);
		frame.getContentPane().setBackground(Color.BLACK);
		device.setFullScreenWindow(frame);

		BufferedImage nullCursorImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Cursor nullCursor = frame.getToolkit().createCustomCursor(nullCursorImage, new Point(), null);
		frame.getContentPane().setCursor(nullCursor);

		SCREEN_WIDTH = device.getDisplayMode().getWidth();
		SCREEN_HEIGHT = device.getDisplayMode().getHeight();

		JLabel currentImage = null;
		JLabel previousImage = null;

		File[] files = new File(".").listFiles();

		if (hasImages(files)) {
			constructDirectories();
		} else {
			displayInvalidContentHelp();
		}

		readyForNext = true;
		requestedToExit = false;

		for (int i = 0; i < files.length; i++) {

			if (readyForNext) {
				File file = files[i];
				try {
					if (file.isFile() && isJpeg(file.getName())) {
						readyForNext = false;

						previousImage = currentImage;

						Image image = ImageIO.read(file);

						int imageWidth = image.getWidth(null);
						int imageHeight = image.getHeight(null);
						double scaleFactor = getDownscaleFitFactor(imageWidth, imageHeight);

						imageWidth = (int) (imageWidth * scaleFactor);
						imageHeight = (int) (imageHeight * scaleFactor);

						image = image.getScaledInstance(imageWidth, imageHeight, Image.SCALE_DEFAULT);

						currentImage = new JLabel(new ImageIcon(image));
						frame.add(currentImage);

						if (previousImage != null) {
							frame.remove(previousImage);
						}

						for (KeyListener listener : frame.getKeyListeners()) {
							frame.removeKeyListener(listener);
						}
						frame.addKeyListener(getDefaultKeyListener(file));

						frame.setVisible(true);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			while (!requestedToExit && !readyForNext) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			if (requestedToExit) {
				break;
			}
		}

		frame.setVisible(false);
		frame.dispose();

		// TODO inform user of operation stats
	}

	private KeyListener getDefaultKeyListener(File file) {
		return new KeyListener() {

			@Override
			public void keyTyped(KeyEvent arg0) {
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				switch (e.getKeyChar()) {

				case 'p':
				case 'P':
					moveFile(file, PARAKEET);
					break;

				case 'm':
				case 'M':
					moveFile(file, MAMMAL);
					break;

				case 'b':
				case 'B':
					moveFile(file, BIRD);
					break;

				case 'f':
				case 'F':
					moveFile(file, FOGGY);
					break;

				case 'o':
				case 'O':
					moveFile(file, OTHER);
					break;

				case 'e':
				case 'E':
					moveFile(file, EMPTY);
					break;

				// skip
				case 's':
				case 'S':
					readyForNext = true;
					break;

				// exit clauses
				case 'x':
				case 'X':
				case 'q':
				case 'Q':
					requestedToExit = true;
					break;
				}
			}
		};
	}

	private boolean hasImages(File[] files) {
		for (File file : files) {
			if (isJpeg(file.getName())) {
				return true;
			}
		}
		return false;
	}

	private boolean isJpeg(String path) {
		String lowered = path.toLowerCase();
		return lowered.contains(".jpg") || lowered.contains(".jpeg");
	}

	private void moveFile(File file, String target) {
		try {
			Files.move(file.toPath(), Paths.get(target + "/" + file.getName()));
			readyForNext = true;

		} catch (IOException moveException) {
			moveException.printStackTrace();
		}
	}

	private double getDownscaleFitFactor(int imageWidth, int imageHeight) {
		double defaultFactor = 1d;
		double downscaleFactor = 1d;

		double scaleWidth = getScaleFactor(imageWidth, SCREEN_WIDTH);
		double scaleHeight = getScaleFactor(imageHeight, SCREEN_HEIGHT);
		downscaleFactor = (scaleWidth < scaleHeight) ? scaleWidth : scaleHeight;

		return (downscaleFactor < defaultFactor) ? downscaleFactor : defaultFactor;
	}

	private static double getScaleFactor(int imageDimension, int screenDimension) {
		double factor = 1d;
		if (imageDimension > screenDimension) {
			factor = (double) screenDimension / (double) imageDimension;
		}
		return (factor > 0) ? factor : 1d;
	}

	private void constructDirectories() {
		try {
			String[] directories = { PARAKEET, MAMMAL, OTHER, EMPTY, FOGGY, BIRD };

			for (String target : directories) {
				if (!new File(target).exists()) {
					Files.createDirectory(Paths.get(target));
				}
			}

		} catch (Exception dirException) {
			dirException.printStackTrace();
		}
	}

	private static void displayInvalidContentHelp() {
		System.out.println("there aren't any valid images to process in this folder");
		displayHelp();
	}

	private static void displayHelp() {
		System.out.println("run this in a folder of unsorted feeder camera images");
	}
}
