import java.util.Vector;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.File;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.border.Border;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.*;
import java.awt.event.*;
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

			// Create and add the image label
		    ImageIcon titleImage = new ImageIcon("title.png");
		    JLabel titleLabel = new JLabel(titleImage);
		    gamePanel.add(titleLabel, gbc);

		    // Add a rigid area to create space between the image and buttons
		    gamePanel.add(Box.createRigidArea(new Dimension(0, 10)), gbc);

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
		
		if (SOUNDS_ENABLED) {
			menu_theme.play();
		}
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

		nonAlphaSkyMap = initNonAlphaPixelMap(sky);
		nonAlphaDirtMap = initNonAlphaPixelMap(dirt);
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
			//gamePanel.setGameActive(true);
			gameActive = true;
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
	// Proper way to display graphics is by overriding JPanel's paintComponent method
	public static class GamePanel extends JPanel {
		private Timer timer;

		public GamePanel() {
			timer = new Timer(32, new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					if (gameActive || GameOver) {
						repaint();
					}
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (gameActive) {
				Graphics2D g2D = (Graphics2D) g;

				g2D.drawImage(sky, XOFFSET, YOFFSET, null); // draw track in layers
				g2D.drawImage(dirt, XOFFSET, YOFFSET, null);
				g2D.drawImage(track, XOFFSET, YOFFSET, null);

				// draw information (curr_lap, speed, etc) at top
				g2D.setFont(new Font("SansSerif", Font.PLAIN, 18));
				
				g2D.setColor(P1BLUE); // p1 info
				g2D.drawString("Lap: " + p1_curr_lap, 15, 15); // current lap
				g2D.drawString("Best time: " + p1BestTime, 15, 35); // best lap time
				g2D.drawString("Speed: " + Math.round(p1velocity*50) + "v/s", 15, 55); // speed

				g2D.setColor(P2RED); // p2 info
				g2D.drawString("Lap: " + p2_curr_lap, WINWIDTH-130, 15);
				g2D.drawString("Best time: " + p2BestTime, WINWIDTH-130, 35);
				g2D.drawString("Speed: " + Math.round(p2velocity*50)  + "v/s", WINWIDTH-130, 55); // speed

				// dont draw player objects if they are "dead" (for 3 seconds)
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

			if (GameOver) {
				Graphics2D g2D = (Graphics2D) g;
				if (p1won) {
					g2D.setFont(new Font("SansSerif", Font.PLAIN, 36));
					g2D.setColor(P1BLUE);
					g2D.drawString("Playa ONE won !!!!!!!!!!!! :DDDDDDDD", 50, 50);
				}
				if (p2won) {
					g2D.setColor(P2RED);
					g2D.setFont(new Font("SansSerif", Font.PLAIN, 36));
					g2D.drawString("Playa TWO won !!!!!!!!!!!! :DDDDDDDD", 50, 50);
				}
				g2D.dispose();
			}
		}

		public void startAnimation() { timer.start(); }

		public void stopAnimation() { timer.stop(); }

		//public void setGameActive(boolean active) { gameActive = active; }
	}

	// updating player one movement
	private static class PlayerOneMover implements Runnable {
		LapTimeTracker timeTracker = new LapTimeTracker();

		public PlayerOneMover() {
			velocitystep = 0.02; // aka accel
			rotatestep = 0.03;
			p1.maxvelocity = 2;
			brakingforce = 0.02;

			refreshPoints(p1_lap_progress);
			p1_curr_lap = 3;

			timeTracker.startLap(true); // is p1
		}

		public void run() {
			BackgroundSound fallSound = new BackgroundSound("stinky.wav", false);

			while (!endgame) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) { }

				// did they finish lap
				checkLapProgress(p1.getX(), p1.getY(), p1_lap_progress, p1_curr_lap, timeTracker);

				isCollidingWithSky = (isCollidingWithLayer(p1.getX(), p1.getY(), 0.65, nonAlphaSkyMap));
				isCollidingWithDirt = (isCollidingWithLayer(p1.getX(), p1.getY(), 0.50, nonAlphaDirtMap));

				if (isCollidingWithSky && !p1FallRecentlyPlayed && SOUNDS_ENABLED) { // play fall sound
					// wait 3 sec, but do not pause rest of execution
					fallSound.play();
					p1FallRecentlyPlayed = true;
					ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                	scheduler.schedule(() -> p1FallRecentlyPlayed = false, 3, TimeUnit.SECONDS);
				}

				if (isCollidingWithSky) {
					p1dead = true;
					try { Thread.sleep(3000); } catch (InterruptedException e) { }
					p1velocity = 0;
					placePlayerOnNearestTrack(p1, track);
					p1dead = false;
				}

				isCollidingWithPlayer(); // is it?

				if (isCollidingWithDirt) {
					p1.maxvelocity = 0.8;
				} else {
					p1.maxvelocity = 2;
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
		private boolean isCollidingWithSky, isCollidingWithDirt;
	}

	// updating player two movement
	private static class PlayerTwoMover implements Runnable {
		LapTimeTracker timeTracker = new LapTimeTracker();

		public PlayerTwoMover() {
			velocitystep = 0.02; // aka accel
			rotatestep = 0.03;
			p2.maxvelocity = 2;
			brakingforce = 0.02;

			refreshPoints(p2_lap_progress);
			p2_curr_lap = 3;

			timeTracker.startLap(false); // is not p1
		}

		public void run() {
			BackgroundSound fallSound = new BackgroundSound("stinky.wav", false);

			while (!endgame) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) { }

				checkLapProgress(p2.getX(), p2.getY(), p2_lap_progress, p2_curr_lap, timeTracker);

				isCollidingWithSky = (isCollidingWithLayer(p2.getX(), p2.getY(), 0.65, nonAlphaSkyMap));
				isCollidingWithDirt = (isCollidingWithLayer(p2.getX(), p2.getY(), 0.50, nonAlphaDirtMap));

				if (isCollidingWithSky && !p2FallRecentlyPlayed && SOUNDS_ENABLED) { // play fall sound
					// wait 3 sec, but do not pause rest of execution
					fallSound.play();
					p2FallRecentlyPlayed = true;
					ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                	scheduler.schedule(() -> p2FallRecentlyPlayed = false, 3, TimeUnit.SECONDS);
				}

				if (isCollidingWithSky) {
					p2dead = true;
					try { Thread.sleep(3000); } catch (InterruptedException e) { }
					p2velocity = 0;
					placePlayerOnNearestTrack(p2, track);
					p2dead = false;
				}

				isCollidingWithPlayer();

				if (isCollidingWithDirt) {
					p2.maxvelocity = 0.8;
				} else {
					p2.maxvelocity = 2;
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
		private boolean isCollidingWithSky, isCollidingWithDirt;
	}

	private static void refreshPoints(ArrayList<Point> lapProgress) {
		lapProgress.clear();
		for (Point point : CHECKPOINT_ORDER) {
			lapProgress.add((Point) point.clone());
		}
	}

	private static void checkLapProgress(Double x, Double y, ArrayList<Point> lapProgress, Integer currLap, LapTimeTracker tracker) {
		Point nextPoint = lapProgress.get(0);

		if ((nextPoint.x == cp1.x)) { // has yet to cross cp1, ... etc
			if ( (y <= nextPoint.y) && (x < finish.x) ) { // make sure player is on left of finish
				lapProgress.remove(0); // update lapProgress
				System.out.println("Someone passed cp1");
			}
		} else if (nextPoint.x == cp2.x) {
			if (x >= nextPoint.x) {
				lapProgress.remove(0);
				System.out.println("Someone passed cp2");
			}
		} else if (nextPoint.x == cp3.x) {
			if (y >= nextPoint.y) {
				lapProgress.remove(0);
				System.out.println("Someone passed cp3");
			}
		} else if (nextPoint.x == cp4.x) {
			if (x <= nextPoint.x) {
				lapProgress.remove(0);
				System.out.println("Someone passed cp4");
			}
		} else if (nextPoint.x == finish.x) {
			// allow player to cross finish line
			if (x <= nextPoint.x) {
				if (lapProgress == p1_lap_progress) {
					tracker.endLap(true);
					tracker.startLap(true);
					p1_curr_lap += 1;
				} else if (lapProgress == p2_lap_progress) {
					tracker.endLap(false);
					tracker.startLap(false);
					p2_curr_lap += 1;
				}
				System.out.println("Done with lap " + currLap + "! Now on lap " + (currLap+1));
				refreshPoints(lapProgress);
			}
		}
		if (p1_curr_lap == 4) {
			p1won = true;
			gameActive = false;
			GameOver = true;
		}
		if (p2_curr_lap == 4) {
			p2won = true;
			gameActive = false;
			GameOver = true;
		}
	}

	private static boolean isCollidingWithLayer(double carX, double carY, double threshold, Map<Integer, Boolean> map) {
		int carWidth = 30;
        int carHeight = 30;

        // bounding box for the car
        Rectangle carBounds = new Rectangle((int) carX, (int) carY, carWidth, carHeight);

        int totalPixels = carBounds.width * carBounds.height;
        int collidedPixels = 0;
		int x;
		int y;

        // iterate over the pixels within the car's bounding box
        for (x = carBounds.x; x < carBounds.x + carBounds.width; x++) {
            for (y = carBounds.y; y < carBounds.y + carBounds.height; y++) {
                // check if the pixel is in the map
                int key = x + y * WINWIDTH;
                if (map.containsKey(key)) {
                    collidedPixels++;
                }
            }
        }

        // check if the percentage of collided pixels is greater than the threshold
        double collisionPercentage = (double) collidedPixels / totalPixels;
        return collisionPercentage >= threshold;
    }

	private static void isCollidingWithPlayer() {
		double xdiff = p2.getX() - p1.getX();
		double ydiff = p2.getY() - p1.getY();
		double magnitude = Math.sqrt(xdiff * xdiff + ydiff * ydiff);
		xdiff /= magnitude;
		ydiff /= magnitude;

		if ( magnitude <= 15 ) {
			if (p1velocity < p2velocity) {
				p1velocity = 0; // GET RUN OVER
				p1.rotate(50);
			} else {
				p2velocity = 0;
				p2.rotate(50);
			}
		}
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

	private static class LapTimeTracker {
		public void startLap(boolean p1) {
			if (p1) {
				p1LapStartTime = System.currentTimeMillis();
			} else {
				p2LapStartTime = System.currentTimeMillis();
			}
		}

		public void endLap(boolean p1) {
			if (p1) {
				long millis = p1LapStartTime - System.currentTimeMillis();
				p1LapTimes.add( Math.abs(millis / 1000.0) );
				System.out.println(p1LapTimes);
				p1BestTime = Collections.min(p1LapTimes).toString();
			} else {
				long millis = p2LapStartTime - System.currentTimeMillis();
				p2LapTimes.add( Math.abs(millis / 1000.0) );
				System.out.println(p2LapTimes);
				p2BestTime = Collections.min(p2LapTimes).toString();
			}
		}
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
		private double x, y, xwidth, yheight, angle;
		public double maxvelocity;

		public ImageObject(double xinput, double yinput, double xwidthinput,
			double yheightinput, double angleinput) {
			x = xinput;
			y = yinput;
			xwidth = xwidthinput;
			yheight = yheightinput;
			angle = angleinput;
		}

		public double getX() { return x; }

		public double getY() { return y; }

		public double getWidth() { return xwidth; }

		public double getHeight() { return yheight; }

		public double getAngle() { return angle; }

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

	// init the non-alpha pixel map during setup
    private static Map<Integer, Boolean> initNonAlphaPixelMap(BufferedImage img) {
        Map<Integer, Boolean> map = new HashMap<>();

        for (int x = 0; x < WINWIDTH; x++) {
            for (int y = 0; y < WINHEIGHT; y++) {
                int pixelColor = img.getRGB(x, y);
                int key = x+y*WINWIDTH;

                // put non-alpha pixels
                if ((pixelColor & 0xFF000000) != 0) {
                    map.put(key, true);
                }
            }
        }

		return map;
    }

	// -------- GLOBAL VARIABLES --------
	private static Boolean endgame;
	private static Boolean GameOver = false;
	private static boolean gameActive = false;
	private static Boolean upPressed, downPressed, leftPressed, rightPressed;
	private static Boolean wPressed, sPressed, aPressed, dPressed;
	private static Boolean p1FallRecentlyPlayed = false;
	private static Boolean p2FallRecentlyPlayed = false;
	private static Boolean p1dead = false;
	private static Boolean p2dead = false;
	private static Boolean SOUNDS_ENABLED = false;

	private static JButton startButton, quitButton;

	private static Point cp1 = new Point(75, 420);
	private static Point cp2 = new Point(258, 233);
	private static Point cp3 = new Point(693, 423);
	private static Point cp4 = new Point(540, 708);
	private static Point finish = new Point(444, 556);
	private static final ArrayList<Point> CHECKPOINT_ORDER = new ArrayList<Point>(Arrays.asList(
		cp1, cp2, cp3, cp4, finish));
	private static ArrayList<Point> p1_lap_progress = new ArrayList<Point>();
	private static ArrayList<Point> p2_lap_progress = new ArrayList<Point>();
	private static Integer p1_curr_lap, p2_curr_lap;

	private static long p1LapStartTime = 0;
	private static ArrayList<Double> p1LapTimes = new ArrayList<>();

	private static long p2LapStartTime = 0;
	private static ArrayList<Double> p2LapTimes = new ArrayList<>();

	private static String p1BestTime = "???";
	private static String p2BestTime = "???";

	private static boolean p1won = false;
	private static boolean p2won = false;

	private static Color CELESTIAL = new Color(49, 151, 199);
	private static Color HIGHLIGHT = new Color(110, 168, 195);
	private static Color URANIAN = new Color(164, 210, 232);
	private static Color P1BLUE = new Color(29, 78, 153);
	private static Color P2RED = new Color(213, 50, 50);

	private static int XOFFSET, YOFFSET, WINWIDTH, WINHEIGHT;

	private static ImageObject p1, p2;
	private static double p1width, p1height, p1originalX, p1originalY, p1velocity;
	private static double p2width, p2height, p2originalX, p2originalY, p2velocity;

	private static JFrame appFrame;

	private static final int IFW = JComponent.WHEN_IN_FOCUSED_WINDOW;

	private static BufferedImage sky, dirt, track;
	private static Map<Integer, Boolean> nonAlphaSkyMap;
	private static Map<Integer, Boolean> nonAlphaDirtMap;
	private static BufferedImage player1, player2;
}