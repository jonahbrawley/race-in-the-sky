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
		pi = 3.14159265368979;
		endgame = false;

		try {
			sunny_hill = ImageIO.read( new File("sunny_hill.png") );
		} catch (IOException e) {
			e.printStackTrace();
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

		myPanel.setBackground(CELESTIAL);
		appFrame.getContentPane().add(myPanel, "Center");
		appFrame.setVisible(true);
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

	private static class StartGame implements ActionListener {
		private final MyPanel panel;

		public StartGame(MyPanel panel) {
			this.panel = panel;
		}

		public void actionPerformed(ActionEvent e) {
			panel.startRace();
			startButton.setVisible(false);
			quitButton.setVisible(false);
			endgame = true;
			// actions here
			endgame = false;
			Thread t1 = new Thread( new Animate());
			t1.start();
		}
	}

	private static class QuitGame implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	}

	private static class Animate implements Runnable {
		public void run() {
			while (endgame == false) {
				appFrame.repaint();
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

	private static Boolean endgame;
	private static Boolean racestart;

	private static JButton startButton;
	private static JButton quitButton;

	private static Color CELESTIAL = new Color(49, 151, 199);
	private static Color HIGHLIGHT = new Color(110, 168, 195);
	private static Color URANIAN = new Color(164, 210, 232);

	private static int XOFFSET;
	private static int YOFFSET;
	private static int WINWIDTH;
	private static int WINHEIGHT;

	private static double pi;

	private static JFrame appFrame;

	private static BufferedImage sunny_hill;
}