import java.util.Vector;
import java.util.Random;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.JComboBox;

import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.File;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.Graphics;
import java.awt.Graphics2D;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Color;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.*;
import javax.swing.border.Border;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import javax.swing.*;
import java.awt.*;

public class Racing {
	public Racing() {
		setup();
	}

	public static void setup() {
		appFrame = new JFrame("The Amazing Race");
		XOFFSET = 0;
		YOFFSET = 0;
		WINWIDTH = 1000;
		WINHEIGHT = 800;

		endgame = false;

		p1width = 30;
		p1height = 30;
		p1originalX = (double) XOFFSET + ((double) WINWIDTH / 2.0) - (p1width / 2.0);
		p1originalY = (double) YOFFSET + ((double) WINHEIGHT / 2.0) - (p1height / 2.0);

		try { // IO
			sunny_hill = ImageIO.read( new File("sunny_hill.png") );
			player1 = ImageIO.read( new File("car1.png") );
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static class BackgroundMusic implements Runnable {
		private String file = "menu.wav";

		public BackgroundMusic() {}

		public BackgroundMusic(String file) {
			this.file = file;
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
				clip.loop(Clip.LOOP_CONTINUOUSLY);
	        } 
	        catch (Exception e) {
	            e.printStackTrace();
	        }
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

	private static class MyPanel extends JPanel {
		private boolean racestart = false;

	    @Override
	    protected void paintComponent(Graphics g) {
	        super.paintComponent(g);
	        if (racestart) {
	        	Graphics2D g2D = (Graphics2D) g;
	        	g2D.drawImage(sunny_hill, XOFFSET, YOFFSET, null);
	        }
	    }

	    public void startRace() {
	    	racestart = true;
	    	repaint();
	    }
	}

	private static class Animate implements Runnable {
		public void run() {
			while (endgame == false) {
				//appFrame.repaint();

				playerDraw();

				try {
					Thread.sleep(32);
				} catch (InterruptedException e) {

				}
			}
		}
	}

	private static void backgroundDraw() {
		Graphics g = appFrame.getGraphics();
		Graphics2D g2D = (Graphics2D) g;
		g2D.drawImage(sunny_hill, XOFFSET, YOFFSET, null);
	}
	
	// thread responsible for updating player movement
	private static class PlayerMover implements Runnable {
		public PlayerMover() {
			velocitystep = 0.01;
			rotatestep = 0.01;
		}

		public void run() {
			while (endgame == false) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {

				}

				if (upPressed == true) {
					p1velocity = p1velocity + velocitystep;
				}
				if (downPressed == true) {
					p1velocity = p1velocity - velocitystep;
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

				p1.move(-p1velocity * Math.cos(p1.getAngle() - Math.PI / 2.0),
					p1velocity * Math.sin(p1.getAngle() - Math.PI / 2.0));
				p1.screenWrap(XOFFSET, XOFFSET + WINWIDTH, YOFFSET, YOFFSET + WINHEIGHT);
			}
		}
		private double velocitystep, rotatestep;
	}
	
	// moveable image objects
	private static class ImageObject {
		private double x, y, xwidth, yheight, angle, internalangle, comX, comY;
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

		public void screenWrap(double leftEdge, double rightEdge, double topEdge, double bottomEdge) {
			if (x > rightEdge) { moveto(leftEdge, getY()); }
			if (x < leftEdge) { moveto(rightEdge, getY()); }
			if (y > bottomEdge) { moveto(getX(), topEdge); }
			if (y < topEdge) { moveto(getX(), bottomEdge); }
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

	// draws racecar graphic for p1 ImageObject
	private static void playerDraw() {
		Graphics g = appFrame.getGraphics();
		Graphics2D g2D = (Graphics2D) g;
		g2D.drawImage(rotateImageObject(p1).filter(player1, null), (int)(p1.getX() + 0.5),
			(int)(p1.getY() + 0.5), null);
	}

	// initiates key actions from panel key responses
	private static void bindKey(MyPanel panel, String input) {
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
		}

		private String action;
	}

	private static class StartGame implements ActionListener {
		private final MyPanel panel;

		public StartGame(MyPanel panel) {
			this.panel = panel;
		}

		public void actionPerformed(ActionEvent ae) {
			panel.startRace();
			startButton.setVisible(false);
			quitButton.setVisible(false);
			endgame = true;

			upPressed = false;
			downPressed = false;
			leftPressed = false;
			rightPressed = false;

			p1 = new ImageObject(p1originalX, p1originalY, p1width, p1height, 0.0);
			p1velocity = 0.0;

			try { Thread.sleep(50); } catch (InterruptedException ie) { }

			endgame = false;
			Thread t1 = new Thread( new Animate() );
			Thread t2 = new Thread( new PlayerMover() );
			t1.start();
			t2.start();
		}
	}

	private static class QuitGame implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	}

	public static void main(String[] args) {
		setup();
		appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		appFrame.setSize(WINWIDTH, WINHEIGHT);

		JPanel myPanel = new MyPanel();
		myPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.ipady = 15;
		gbc.ipadx = 50;

			startButton = new MyButton("START RACE");
			startButton.addActionListener(new StartGame((MyPanel) myPanel));
			setButtonAppearance(startButton);
			myPanel.add(startButton, gbc);

			gbc.insets = new Insets(10, 0, 0, 0);
			
			quitButton = new MyButton("QUIT");
			quitButton.addActionListener(new QuitGame());
			setButtonAppearance(quitButton);
			myPanel.add(quitButton, gbc);

			bindKey((MyPanel) myPanel, "UP");
			bindKey((MyPanel) myPanel, "DOWN");
			bindKey((MyPanel) myPanel, "LEFT");
			bindKey((MyPanel) myPanel, "RIGHT");

		myPanel.setBackground(CELESTIAL);
		appFrame.getContentPane().add(myPanel, "Center");
		appFrame.setVisible(true);

		BackgroundMusic menu_theme = new BackgroundMusic("menu.wav");
		menu_theme.play();
	}

	private static Boolean endgame, racestart;
	private static Boolean upPressed, downPressed, leftPressed, rightPressed;

	private static JButton startButton, quitButton;

	private static Color CELESTIAL = new Color(49, 151, 199);
	private static Color HIGHLIGHT = new Color(110, 168, 195);
	private static Color URANIAN = new Color(164, 210, 232);

	private static int XOFFSET, YOFFSET, WINWIDTH, WINHEIGHT;

	private static ImageObject p1, p2; // player1 and player2 racecar object
	private static double p1width, p1height, p1originalX, p1originalY, p1velocity;

	private static JFrame appFrame;

	private static final int IFW = JComponent.WHEN_IN_FOCUSED_WINDOW;

	private static BufferedImage sunny_hill, player1; // TODO: add player2
}