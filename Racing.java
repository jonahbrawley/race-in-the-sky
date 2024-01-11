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

import javax.imageio.ImageIO;

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
		YOFFSET = 40;
		WINWIDTH = 500;
		WINHEIGHT = 500;
		pi = 3.14159265368979;
		endgame = false;
	}

	public static void main(String[] args) {
		setup();
		appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		appFrame.setSize(400, 400);

		JPanel myPanel = new JPanel();

		JButton testButton = new JButton("Karen tregaskin");
		myPanel.add(testButton);

		appFrame.getContentPane().add(myPanel, "Center");
		appFrame.setVisible(true);
	}

	private static Boolean endgame;

	private static int XOFFSET;
	private static int YOFFSET;
	private static int WINWIDTH;
	private static int WINHEIGHT;

	private static double pi;

	private static JFrame appFrame;
}