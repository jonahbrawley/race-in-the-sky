import java.util.Vector;
import java.util.Random;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.awt.image.BufferedImage;
import java.awt.image.BufferStrategy;
import java.awt.image.ImageObserver;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.File;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.*;
import javax.swing.border.Border;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.util.concurrent.*;

import javax.swing.SwingUtilities;
import javax.swing.*;
import java.awt.*;

public class Racing {
	public Racing() {
		setup();
		initUI();
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			new Racing();
		});
	}

	private void initUI() {
		appFrame = new JFrame("The Amazing Race");
		appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		appFrame.setSize(WINWIDTH, WINHEIGHT);

		JPanel gamePanel = new GamePanel();
		gamePanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.ipady = 15;
		gbc.ipadx = 50;

			startButton = new MyButton("START RACE");
			startButton.addActionListener(new StartGame((GamePanel) gamePanel));
			setButtonAppearance(startButton);
			gamePanel.add(startButton, gbc);

			gbc.insets = new Insets(10, 0, 0, 0);
			
			quitButton = new MyButton("QUIT");
			quitButton.addActionListener(new QuitGame());
			setButtonAppearance(quitButton);
			gamePanel.add(quitButton, gbc);

			bindKey((JPanel) gamePanel, "UP");
			bindKey((JPanel) gamePanel, "DOWN");
			bindKey((JPanel) gamePanel, "LEFT");
			bindKey((JPanel) gamePanel, "RIGHT");

			bindKey((JPanel) gamePanel, "W");
			bindKey((JPanel) gamePanel, "A");
			bindKey((JPanel) gamePanel, "S");
			bindKey((JPanel) gamePanel, "D");

		gamePanel.setBackground(CELESTIAL);
		appFrame.getContentPane().add(gamePanel, "Center");
		appFrame.setVisible(true);

		BackgroundSound menu_theme = new BackgroundSound("menu.wav", true);
		menu_theme.play();
	}

	public static void setup() {
		XOFFSET = 0;
		YOFFSET = 0;
		WINWIDTH = 1000;
		WINHEIGHT = 800;
		endgame = false;

		p1width = 30;
		p1height = 30;
		p1originalX = (double) XOFFSET + ((double) WINWIDTH / 2.15) - (p1width / 2.0);
		p1originalY = (double) YOFFSET + ((double) WINHEIGHT / 1.48) - (p1height / 2.0);

		p2width = 30;
		p2height = 30;
		p2originalX = (double) XOFFSET + ((double) WINWIDTH / 2.15) - (p1width / 2.0);
		p2originalY = (double) YOFFSET + ((double) WINHEIGHT / 1.39) - (p1height / 2.0);

		try { // IO
			player1 = ImageIO.read( new File("car1.png") );
			player2 = ImageIO.read( new File("car2.png") );
			sky = ImageIO.read( new File("cloud_track" + File.separator + "sky.png") );
			dirt = ImageIO.read( new File("cloud_track" + File.separator + "dirt.png") );
			track = ImageIO.read( new File("cloud_track" + File.separator + "track.png") );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// -------- BUTTON ACTIONS --------
	private static class StartGame implements ActionListener {
		private final GamePanel gamePanel;

		public StartGame(GamePanel gamePanel) {
			this.gamePanel = gamePanel;
		}

		public void actionPerformed(ActionEvent ae) { // autechre
			startButton.setVisible(false);
			quitButton.setVisible(false);
			endgame = true;

			upPressed = false;
			downPressed = false;
			leftPressed = false;
			rightPressed = false;

			wPressed = false;
			sPressed = false;
			aPressed = false;
			dPressed = false;

			p1 = new ImageObject(p1originalX, p1originalY, p1width, p1height, 900);
			p2 = new ImageObject(p2originalX, p2originalY, p2width, p2height, 900);

			try { Thread.sleep(50); } catch (InterruptedException ie) { }

			endgame = false;
			gamePanel.setGameActive(true);
			gamePanel.startAnimation();

			Thread t1 = new Thread( new PlayerOneMover() );
			Thread t2 = new Thread( new PlayerTwoMover() );
			t1.start();
			t2.start();
		}
	}

	private static class QuitGame implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	}

	// -------- ANIMATION AND MOVEMENT --------
	public static class GamePanel extends JPanel {
		private Timer timer;
		private boolean gameActive;

		public GamePanel() {
			timer = new Timer(32, new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					if (gameActive) {
						repaint();
					}
				}
			});

			gameActive = false;
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (gameActive) {
				Graphics2D g2D = (Graphics2D) g;

				g2D.drawImage(sky, XOFFSET, YOFFSET, null);
				g2D.drawImage(dirt, XOFFSET, YOFFSET, null);
				g2D.drawImage(track, XOFFSET, YOFFSET, null);

				if (!p1dead) {
					g2D.drawImage(rotateImageObject(p1).filter(player1, null), (int)(p1.getX() + 0.5),
					(int)(p1.getY() + 0.5), null);
				}

				if (!p2dead) {
					g2D.drawImage(rotateImageObject(p2).filter(player2, null), (int)(p2.getX() + 0.5),
					(int)(p2.getY() + 0.5), null);
				}

				g2D.dispose();
			}
		}

		public void startAnimation() { timer.start(); }

		public void stopAnimation() { timer.stop(); }

		public void setGameActive(boolean active) { gameActive = active; }
	}

	// updating player one movement
	private static class PlayerOneMover implements Runnable {
		public PlayerOneMover() {
			velocitystep = 0.02; // aka accel
			rotatestep = 0.03;
			p1.maxvelocity = 2;
			brakingforce = 0.02;
		}

		public void run() {
			BackgroundSound fallSound = new BackgroundSound("stinky.wav", false);

			while (!endgame) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) { }

				if (isCollidingWithLayer(p1.getX(), p1.getY(), dirt)) {
					p1.maxvelocity = 0.8;
				} else {
					p1.maxvelocity = 2;
				}

				if (isCollidingWithLayer(p1.getX(), p1.getY(), sky) && !p1FallRecentlyPlayed) { // play fall sound
					// wait 3 sec, but do not pause rest of execution
					fallSound.play();
					p1FallRecentlyPlayed = true;
					ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                	scheduler.schedule(() -> p1FallRecentlyPlayed = false, 3, TimeUnit.SECONDS);
				}

				if (isCollidingWithLayer(p1.getX(), p1.getY(), sky)) {
					p1dead = true;
					try { Thread.sleep(3000); } catch (InterruptedException e) { }
					p1velocity = 0;
					placePlayerOnNearestTrack(p1, track);
					p1dead = false;
				}

				if (isCollidingWithPlayer(p1, p2)) {
					p2.maxvelocity += p1.maxvelocity;
				}

				if (upPressed == true) {
					if (p1velocity < p1.maxvelocity) {
						p1velocity = (p1velocity) + velocitystep;
					} else if (p1velocity >= p1.maxvelocity) { // ensure max vel not exceeded
						p1velocity = p1.maxvelocity;
					}
				}

				if (downPressed == true) {
					if (p1velocity < -1) { // ensure max rev speed
						p1velocity = -1;
					} else {
						p1velocity = p1velocity - brakingforce;
					}
				}

				if (leftPressed == true) {
					if (p1velocity < 0) {
						p1.rotate(-rotatestep);
					} else {
						p1.rotate(rotatestep);
					}
				}

				if (rightPressed == true) {
					if (p1velocity < 0) {
						p1.rotate(rotatestep);
					} else {
						p1.rotate(-rotatestep);
					}
				}

				// apply drag force
				if (!upPressed && !downPressed && !leftPressed && !rightPressed
					&& p1velocity != 0) {
					if ((p1velocity - 0.1) < 0) {
						p1velocity = 0;
					} else {
						p1velocity = p1velocity - 0.04; 
					}
				}

				p1.move(-p1velocity * Math.cos(p1.getAngle() - Math.PI / 2.0),
					p1velocity * Math.sin(p1.getAngle() - Math.PI / 2.0));
				p1.screenBounds(XOFFSET, WINWIDTH, YOFFSET, WINHEIGHT, p1.maxvelocity);
			}
		}
		private double velocitystep, rotatestep, brakingforce;
	}

	// updating player two movement
	private static class PlayerTwoMover implements Runnable {
		public PlayerTwoMover() {
			velocitystep = 0.02; // aka accel
			rotatestep = 0.03;
			p2.maxvelocity = 2;
			brakingforce = 0.02;
		}

		public void run() {
			BackgroundSound fallSound = new BackgroundSound("stinky.wav", false);

			while (!endgame) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) { }

				if (isCollidingWithLayer(p2.getX(), p2.getY(), dirt)) {
					p2.maxvelocity = 0.8;
				} else {
					p2.maxvelocity = 2;
				}

				if (isCollidingWithLayer(p2.getX(), p2.getY(), sky) && !p2FallRecentlyPlayed) { // play fall sound
					// wait 3 sec, but do not pause rest of execution
					fallSound.play();
					p2FallRecentlyPlayed = true;
					ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                	scheduler.schedule(() -> p2FallRecentlyPlayed = false, 3, TimeUnit.SECONDS);
				}

				if (isCollidingWithLayer(p2.getX(), p2.getY(), sky)) {
					p2dead = true;
					try { Thread.sleep(3000); } catch (InterruptedException e) { }
					p2velocity = 0;
					placePlayerOnNearestTrack(p2, track);
					p2dead = false;
				}

				if (isCollidingWithPlayer(p2, p1)) {
					p1.maxvelocity += p2.maxvelocity;
				}
				
				if (wPressed == true) {
					if (p2velocity < p2.maxvelocity) {
						p2velocity = (p2velocity) + velocitystep;
					} else if (p2velocity >= p2.maxvelocity) { // ensure max vel not exceeded
						p2velocity = p2.maxvelocity;
					}
				}

				if (sPressed == true) {
					if (p2velocity < -1) { // ensure max rev speed
						p2velocity = -1;
					} else {
						p2velocity = p2velocity - brakingforce;
					}
				}

				if (aPressed == true) {
					if (p2velocity < 0) {
						p2.rotate(-rotatestep);
					} else {
						p2.rotate(rotatestep);
					}
				}

				if (dPressed == true) {
					if (p2velocity < 0) {
						p2.rotate(rotatestep);
					} else {
						p2.rotate(-rotatestep);
					}
				}

				// apply drag force
				if (!wPressed && !sPressed && !aPressed && !dPressed
					&& p2velocity != 0) {
					if ((p2velocity - 0.1) < 0) {
						p2velocity = 0;
					} else {
						p2velocity = p2velocity - 0.04; 
					}
				}

				p2.move(-p2velocity * Math.cos(p2.getAngle() - Math.PI / 2.0),
					p2velocity * Math.sin(p2.getAngle() - Math.PI / 2.0));
				p2.screenBounds(XOFFSET, WINWIDTH, YOFFSET, WINHEIGHT, p2.maxvelocity);
			}
		}
		private double velocitystep, rotatestep, brakingforce;
	}

	private static boolean isCollidingWithLayer(double carX, double carY, BufferedImage img) {
	    int roundedX = (int) Math.round(carX);
	    int roundedY = (int) Math.round(carY);

	    int pixelColor = img.getRGB(roundedX, roundedY);

	    return (pixelColor & 0xFF000000) != 0;
	}

	private static boolean isCollidingWithPlayer(ImageObject x1, ImageObject x2) {
		if ( (x1.getX() == x2.getX()) && (x1.getY() == x2.getY()) ) {
			return true;
		}
		return false;
	}

	private static void placePlayerOnNearestTrack(ImageObject p1, BufferedImage currentTrackStrip) {
		double playerX = p1.getX();
		double playerY = p1.getY();

		// calculate the distances to each non-transparent pixel and find the nearest one
	    double nearestDistance = Double.MAX_VALUE;
	    int nearestPixelX = 0;
	    int nearestPixelY = 0;

	    for (int x = 0; x < currentTrackStrip.getWidth(); x++) {
	        for (int y = 0; y < currentTrackStrip.getHeight(); y++) {
	            if ((currentTrackStrip.getRGB(x, y) & 0xFF000000) != 0) {
	                double distance = calculateDistance(playerX, playerY, x, y);

	                // Check if this pixel is closer than the previous nearest one
	                if (distance < nearestDistance) {
	                    nearestDistance = distance;
	                    nearestPixelX = x;
	                    nearestPixelY = y;
	                }
	            }
	        }
	    }

	    p1.moveto(nearestPixelX, nearestPixelY);
	}

	// initiates key actions from panel key responses
	private static void bindKey(JPanel panel, String input) {
		panel.getInputMap(IFW).put(KeyStroke.getKeyStroke("pressed " + input), input + " pressed");
		panel.getActionMap().put(input + " pressed", new KeyPressed(input));

		panel.getInputMap(IFW).put(KeyStroke.getKeyStroke("released " + input), input + " released");
		panel.getActionMap().put(input + " released", new KeyReleased(input));
	}

	// monitors keypresses
	private static class KeyPressed extends AbstractAction {
		public KeyPressed() { action = ""; }

		public KeyPressed(String input) { action = input; }

		public void actionPerformed(ActionEvent e) {
			if (action.equals("UP")) { upPressed = true; }
			if (action.equals("DOWN")) { downPressed = true; }
			if (action.equals("LEFT")) { leftPressed = true; }
			if (action.equals("RIGHT")) { rightPressed = true; }

			if (action.equals("W")) { wPressed = true; }
			if (action.equals("S")) { sPressed = true; }
			if (action.equals("A")) { aPressed = true; }
			if (action.equals("D")) { dPressed = true; }
		}
	
		private String action;
	}

	// monitors keyreleases
	private static class KeyReleased extends AbstractAction {
		public KeyReleased() { action = ""; }

		public KeyReleased(String input) { action = input; }

		public void actionPerformed(ActionEvent e) {
			if (action.equals("UP")) { upPressed = false; }
			if (action.equals("DOWN")) { downPressed = false; }
			if (action.equals("LEFT")) { leftPressed = false; }
			if (action.equals("RIGHT")) { rightPressed = false; }

			if (action.equals("W")) { wPressed = false; }
			if (action.equals("S")) { sPressed = false; }
			if (action.equals("A")) { aPressed = false; }
			if (action.equals("D")) { dPressed = false; }
		}

		private String action;
	}

	// -------- UI AND APPEARANCE --------

	public static class BackgroundSound implements Runnable {
		private String file;
		private boolean loopAudio;

		public BackgroundSound(String file, Boolean isLoop) {
			this.file = file;
			this.loopAudio = isLoop;
		}

		public void play() {
	        Thread t = new Thread(this);
	        t.start();
    	}

    	public void run() {
    		playSound(file);
    	}

    	private void playSound(String file) {
    		File soundFile = new File(file);
	        AudioInputStream inputStream = null;
			
	        try { // get input stream
				Clip clip = AudioSystem.getClip();
				inputStream = AudioSystem.getAudioInputStream(soundFile);
				clip.open(inputStream);
				if (loopAudio) { 
					clip.loop(Clip.LOOP_CONTINUOUSLY); 
				} else {
					clip.start();
				}
	        } 
	        catch (Exception e) {
	            e.printStackTrace();
	        }
    	}
	}

	private static class MyButton extends JButton {
    	public MyButton(String text) {
    		super(text);
    	}

    	@Override
    	protected void paintComponent(Graphics g) {
    		Graphics2D g2 = (Graphics2D) g.create();
	        g2.setColor(getBackground());
	        g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 15, 15));  // Adjust the radius here
	        super.paintComponent(g);
	        g2.dispose();
    	}
    }

	private static void setButtonAppearance(JButton button) {
		button.setBorder(BorderFactory.createCompoundBorder(
			new RoundBorder(15, URANIAN),
			BorderFactory.createEmptyBorder(10, 20, 10, 20)
			));

		button.addMouseListener(new MouseAdapter(){

		    @Override
		    public void mousePressed(MouseEvent e) {
		        button.setBackground(HIGHLIGHT);
		    }

		    @Override
		    public void mouseReleased(MouseEvent e) {
		        button.setBackground(URANIAN);
		    }

		});

		
		button.setBackground(URANIAN);
		button.setForeground(Color.BLACK);
		button.setContentAreaFilled(false);
		button.setOpaque(false);
		button.setFocusPainted(false);
	}

	private static class RoundBorder implements Border { // Used for rounded buttons
        private int radius;
        private Color color;

        public RoundBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(color);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.draw(new RoundRectangle2D.Double(x, y, width - 1, height - 1, radius, radius));
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius, radius, radius, radius);
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }
    }

    // -------- UTILITY FUNCTIONS --------
	// moveable image objects
	private static class ImageObject {
		private double x, y, xwidth, yheight, angle, internalangle, comX, comY;
		public double maxvelocity;
		private Vector<Double> coords, triangles;

		public ImageObject() {}

		public ImageObject(double xinput, double yinput, double xwidthinput,
			double yheightinput, double angleinput) {
			x = xinput;
			y = yinput;
			xwidth = xwidthinput;
			yheight = yheightinput;
			angle = angleinput;
			internalangle = 0.0;
			coords = new Vector<Double>();
		}

		public double getX() { return x; }

		public double getY() { return y; }

		public double getWidth() { return xwidth; }

		public double getHeight() { return yheight; }

		public double getAngle() { return angle; }

		public double getInternalAngle() { return internalangle; }

		public void setAngle(double angleinput) { angle = angleinput; }

		public void setInternalAngle(double input) { internalangle = input; }

		public Vector<Double> getCoords() { return coords; }

		public void setCoords(Vector<Double> input) {
			coords = input;
			generateTriangles();
		}

		public void generateTriangles() {
			triangles = new Vector<Double>();
			// format: (0, 1), (2, 3), (4, 5) is x,y coords of triangle

			// get center point of all coords
			comX = getComX();
			comY = getComY();

			for (int i=0; i<coords.size(); i=i+2) {
				triangles.addElement(coords.elementAt(i));
				triangles.addElement(coords.elementAt(i+1));

				triangles.addElement(coords.elementAt( (i+2) % coords.size() ));
				triangles.addElement(coords.elementAt( (i+3) % coords.size() ));

				triangles.addElement(comX);
				triangles.addElement(comY);
			}
		}

		public void printTriangles() {
			for (int i=0; i < triangles.size(); i=i+6) {
				System.out.print("p0x: " + triangles.elementAt(i) + ", p0y " + triangles.elementAt(i+1));
				System.out.print(" p1x: " + triangles.elementAt(i+2) + ", p1y: " + triangles.elementAt(i+3));
				System.out.println(" p2x: " + triangles.elementAt(i+4) + ", p2y: " + triangles.elementAt(i+5));
			}
		}

		public double getComX() {
			double ret = 0;
			if (coords.size() > 0) {
				for (int i=0; i<coords.size(); i=i+2) { ret = ret + coords.elementAt(i); }
				ret = ret / (coords.size() / 2.0);
			}
			return ret;
		}

		public double getComY() {
			double ret = 0;
			if (coords.size() > 0) {
				for (int i=1; i<coords.size(); i=i+2) { ret = ret + coords.elementAt(i); }
				ret = ret / (coords.size() / 2.0);
			}
			return ret;
		}

		public void move(double xinput, double yinput) {
			x = x + xinput; 
			y = y + yinput;
		}

		public void moveto(double xinput, double yinput) {
			x = xinput; 
			y = yinput;
		}

		public void screenBounds(double leftEdge, double rightEdge, double topEdge, double bottomEdge, double velocity) {
			if (x < leftEdge) { 
				moveto(leftEdge, getY());
				velocity = velocity*0.9;
			}
			if (x + getWidth() > rightEdge) { 
				moveto(rightEdge - getWidth(), getY()); 
				velocity = velocity*0.9;
			}
			if (y < topEdge) { 
				moveto(getX(), topEdge); 
				velocity = velocity*0.9;
			}
			if (y + getHeight() > bottomEdge) { 
				moveto(getX(), bottomEdge - getHeight()); 
				velocity = velocity*0.9;
			}
		}

		public void rotate(double input) {
			angle = angle + input;
			while (angle > (Math.PI*2)) { angle = angle - (Math.PI*2); }
			while (angle < 0) { angle = angle + (Math.PI*2); }
		}

		public void spin(double input) {
			internalangle = internalangle + input;
			while (internalangle > (Math.PI*2)) { internalangle = internalangle - (Math.PI*2); }
			while (internalangle < 0) { internalangle = internalangle + (Math.PI*2); }
		}
	}

	// rotates ImageObject
	private static AffineTransformOp rotateImageObject(ImageObject obj) {
		AffineTransform at = AffineTransform.getRotateInstance(-obj.getAngle(),
			obj.getWidth()/2.0, obj.getHeight()/2.0);
		AffineTransformOp atop = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		return atop;
	}

	private static double calculateDistance(double x1, double y1, double x2, double y2) {
	    // Calculate Euclidean distance between two points
	    return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
	}

	// -------- GLOBAL VARIABLES --------
	private static Boolean endgame;
	private static Boolean upPressed, downPressed, leftPressed, rightPressed;
	private static Boolean wPressed, sPressed, aPressed, dPressed;
	private static Boolean p1FallRecentlyPlayed = false;
	private static Boolean p2FallRecentlyPlayed = false;
	private static Boolean p1dead = false;
	private static Boolean p2dead = false;

	private static JButton startButton, quitButton;

	private static Color CELESTIAL = new Color(49, 151, 199);
	private static Color HIGHLIGHT = new Color(110, 168, 195);
	private static Color URANIAN = new Color(164, 210, 232);

	private static int XOFFSET, YOFFSET, WINWIDTH, WINHEIGHT;

	private static ImageObject p1, p2; // player1 and player2 racecar object
	private static double p1width, p1height, p1originalX, p1originalY, p1velocity;
	private static double p2width, p2height, p2originalX, p2originalY, p2velocity;

	private static JFrame appFrame;

	private static final int IFW = JComponent.WHEN_IN_FOCUSED_WINDOW;

	private static BufferedImage sky, dirt, track;
	private static BufferedImage player1, player2; // TODO: add player2
}