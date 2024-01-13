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

		JButton startButton = new JButton("START RACE");
		startButton.addActionListener(new StartGame((MyPanel) myPanel, startButton));
		myPanel.add(startButton);

		appFrame.getContentPane().add(myPanel, "Center");
		appFrame.setVisible(true);
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
		private final JButton startButton;

		public StartGame(MyPanel panel, JButton startButton) {
			this.panel = panel;
			this.startButton = startButton;
		}

		public void actionPerformed(ActionEvent e) {
			panel.startRace();
			startButton.setVisible(false);
			endgame = true;
			// actions here
			endgame = false;
			Thread t1 = new Thread( new Animate());
			t1.start();
		}
	}

	private static class Animate implements Runnable {
		public void run() {
			// while (endgame == false) {
			// 	backgroundDraw();
			// 	try {
			// 		Thread.sleep(32);
			// 	} catch (InterruptedException e) {

			// 	}
			// }
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

	private static int XOFFSET;
	private static int YOFFSET;
	private static int WINWIDTH;
	private static int WINHEIGHT;

	private static double pi;

	private static JFrame appFrame;

	private static BufferedImage sunny_hill;
}