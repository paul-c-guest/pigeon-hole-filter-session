package filter;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
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
import javax.swing.border.LineBorder;

/**
 * display images in a folder one by one, waiting for keyboard presses to move
 * each to a target folder
 * 
 * @version 1.0
 * @author ballsies
 * 
 */
public class Filter {

	private JFrame frame;
	private boolean readyForNext;
	private boolean requestedToExit;
	private boolean goBack;

	private static Path PATH;

	private static String PARAKEET = "/Parakeets";
	private static String FOGGY = "/Foggy";
	private static String OTHER = "/Other";
	private static String BIRD = "/Birds";
	private static String MAMMAL = "/Mammals";
	private static String EMPTY = "/Empty";

	private static int SCREEN_WIDTH, SCREEN_HEIGHT;

	File[] folder;

	File currentFile;
	File lastMovedFile;

	int index;
	int lastMovedIndex;

	public static void main(String[] args) {

		if (args.length > 0 && args[0].contains("h")) {
			displayHelp();
			return;
		}

		// start filtering in the current command line directory
		new Filter(null);
	}

	/**
	 * 
	 * @param path target directory to use. can be {@link null}
	 */
	public Filter(Path path) {
		PATH = path;

		// use a path if provided, else look in local folder
		folder = PATH != null ? new File(path.toUri()).listFiles() : new File(".").listFiles();

		if (folder.length > 0 && folderContainsImages()) {
			constructDirectories();
			doMainRoutine();
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
		frame.setTitle("Image Filter");
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

		frame.addWindowFocusListener(getFocusListener(device));
		frame.addMouseMotionListener(getMouseListener(nullCursor));

		index = 0;
		while (index < folder.length) {

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

						BufferedImage buffered = ImageIO.read(currentFile);

						int imageWidth = buffered.getWidth();
						int imageHeight = buffered.getHeight();

						double scaleFactor = getDownscaleFitFactor(imageWidth, imageHeight);

						imageWidth = (int) (imageWidth * scaleFactor);
						imageHeight = (int) (imageHeight * scaleFactor);

						Image image = buffered.getScaledInstance(imageWidth, imageHeight, Image.SCALE_FAST);

						currentImage = new JLabel(new ImageIcon(image));
						currentImage.setLayout(new FlowLayout(FlowLayout.CENTER));

						JLabel filenameText = new JLabel(currentFile.getName());
						filenameText.setOpaque(true);
						filenameText.setBorder(new LineBorder(Color.BLACK, 8));
						filenameText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 18));
						filenameText.setBackground(Color.BLACK);
						filenameText.setForeground(Color.WHITE);
						currentImage.add(filenameText);

						frame.add(currentImage);

						if (previousImage != null) {
							frame.remove(previousImage);
						}

						for (KeyListener listener : frame.getKeyListeners()) {
							frame.removeKeyListener(listener);
						}
						frame.addKeyListener(new SingleKeyEventListener(currentFile, index));

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

			index++;
		}

		frame.setVisible(false);
		frame.dispose();
	}

	// TODO refactor using metadata extraction library used in ClusteredList
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

	private void moveFile(File file, String newDirectory) {
		try {
			String leadingPath = PATH != null ? PATH.toString() : ".";

			Path moveToPath = Path.of(leadingPath, newDirectory, file.getName());

			Path moved = Files.move(file.toPath(), moveToPath);

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
			String[] folders = { PARAKEET, MAMMAL, OTHER, EMPTY, FOGGY, BIRD };

			for (String folder : folders) {
				folder = PATH != null ? PATH.toString() + folder : "." + folder;
				if (!new File(folder).exists()) {
					Files.createDirectory(Paths.get(folder));
				}
			}

		} catch (Exception dirException) {
			dirException.printStackTrace();
		}
	}

	private static void displayInvalidContentHelp() {
		System.out.println("There aren't any images in this folder.");
		displayHelp();
	}

	private static void displayHelp() {
		System.out.println("Run this in a folder of unsorted images.");
	}

	private MouseMotionListener getMouseListener(Cursor nullCursor) {
		return new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent move) {
				if (frame.getContentPane().getCursor() == nullCursor) {
					new Thread(new Runnable() {
						public void run() {
							frame.getContentPane().setCursor(Cursor.getDefaultCursor());
							try {
								Thread.sleep(750);
								frame.getContentPane().setCursor(nullCursor);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}).start();
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
			}
		};
	}

	private WindowFocusListener getFocusListener(GraphicsDevice device) {
		return new WindowFocusListener() {

			@Override
			public void windowLostFocus(WindowEvent e) {
				frame.setExtendedState(JFrame.ICONIFIED);
				device.setFullScreenWindow(null);
			}

			@Override
			public void windowGainedFocus(WindowEvent e) {
				device.setFullScreenWindow(frame);
			}
		};
	}

	/**
	 * implementation of {@link java.awt.event.KeyListener KeyListener} which only
	 * allows a single valid keypress, ignoring any other events and all subsequent
	 * events.
	 */
	class SingleKeyEventListener implements KeyListener {

		private File file;
		private int index;
		private boolean validKeyPressed;

		public SingleKeyEventListener(File file, int index) {
			this.file = file;
			this.index = index;
			validKeyPressed = false;
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if (validKeyPressed) {
//				System.out.println("ignoring redundant keypress: " + e.getKeyCode());
				return;
			}

			switch (e.getKeyCode()) {
			case 80: // P
				validKeyPressed = true;
				moveFile(file, PARAKEET);
				break;

			case 66: // B
				validKeyPressed = true;
				moveFile(file, BIRD);
				break;

			case 69: // E
				validKeyPressed = true;
				moveFile(file, EMPTY);
				break;

			case 77: // M
				validKeyPressed = true;
				moveFile(file, MAMMAL);
				break;

			case 70: // F
				validKeyPressed = true;
				moveFile(file, FOGGY);
				break;

			case 79: // O
				validKeyPressed = true;
				moveFile(file, OTHER);
				break;

			case 8: // backspace
				if (lastMovedFile == null) {
//					System.out.println("ignoring invalid backspace");
					return;
				}

				validKeyPressed = true;

				if (!goBack) {
					goBack = true;
					lastMovedIndex = index - 1; // TODO 'index - 1' will not always step back to correct position?
					readyForNext = true;
				} else {
					// TODO inform user there is only one level of undo
				}
				break;

			case 27: // escape
				requestedToExit = true;
				break;
			}

		}

		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
		}
	}

}
