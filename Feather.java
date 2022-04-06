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
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Feather {

	private JFrame frame;
	private boolean readyForNext;
	private boolean requestedToExit;
	private boolean goBack;

	private static String PARAKEET = "./Parakeets";
	private static String FOGGY = "./Foggy";
	private static String OTHER = "./Other";
	private static String BIRD = "./Birds";
	private static String MAMMAL = "./Mammals";
	private static String EMPTY = "./Empty";

	private static int SCREEN_WIDTH, SCREEN_HEIGHT;

	File[] folder;
	File lastMovedFile;
	int lastMovedIndex;

	public static void main(String[] args) {

		if (args.length > 0 && args[0].contains("h")) {
			displayHelp();
			return;
		}

		new Feather();
	}

	private Feather() {
		folder = new File(".").listFiles();

		if (folderContainsImages()) {
			
			constructDirectories();
			doMainRoutine();
//			displayExitStats(); // TODO collect stats during progress and display on exit

		} else {
			displayInvalidContentHelp();
		}
	}

	private void doMainRoutine() {
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

		readyForNext = true;
		requestedToExit = false;
		goBack = false;

		File currentFile;

		for (int index = 0; index < folder.length; index++) {

			if (goBack) {
				if (lastMovedFile != null) {
					index = lastMovedIndex;
					folder[index] = lastMovedFile;
				}
				goBack = false;
			}

			if (readyForNext) {
				currentFile = folder[index];

				try {
					if (currentFile.isFile() && isJpeg(currentFile.getName())) {
						readyForNext = false;

						previousImage = currentImage;

						Image image = ImageIO.read(currentFile);

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
						frame.addKeyListener(new SingleKeyEventListener(currentFile, index));
//						frame.addKeyListener(getKeyListener(currentFile, index));

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
	}

	private boolean folderContainsImages() {
		for (File file : folder) {
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
			Path moved = Files.move(file.toPath(), Paths.get(target + "/" + file.getName()));
			lastMovedFile = new File(moved.toString());
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

	/**
	 * implementation of {@link java.awt.event.KeyListener} which only allows a single keypress, ignoring all subsequent events
	 *
	 */
	class SingleKeyEventListener implements KeyListener {
		
		private File file;
		private int index;
		private boolean keyPressed;
		
		public SingleKeyEventListener(File file, int index) {
			this.file = file;
			this.index = index;
			keyPressed = false;
		}
		
		@Override
		public void keyReleased(KeyEvent e) {
			if (keyPressed) {
//				System.out.println("ignoring redundant keypress: " + e.getKeyCode());
				return;
			}
			
			switch (e.getKeyCode()) {
			case 80: // P
				moveFile(file, PARAKEET);
				break;
				
			case 66: // B
				moveFile(file, BIRD);
				break;
				
			case 69: // E
				moveFile(file, EMPTY);
				break;
				
			case 77: // M
				moveFile(file, MAMMAL);
				break;
				
			case 70: // F
				moveFile(file, FOGGY);
				break;
				
			case 79: // O
				moveFile(file, OTHER);
				break;
				
			case 83: // S - skip: do nothing with current file
				readyForNext = true;
				break;
				
			case 8: // backspace
				if (!goBack) {
					goBack = true;
					lastMovedIndex = index - 1; // TODO 'index - 1' will not always step back to correct position!
					readyForNext = true;
				} else {
					// TODO inform user there is only one level of undo
				}
				break;
				
			case 27: // escape
				requestedToExit = true;
				break;
			}
			
			// ensure no further keypresses are acted on
			keyPressed = true;
		}
		
		@Override
		public void keyTyped(KeyEvent e) {
		}
		
		@Override
		public void keyPressed(KeyEvent e) {
		}
	}
	
}
