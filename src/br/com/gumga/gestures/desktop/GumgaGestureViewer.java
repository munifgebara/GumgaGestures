package br.com.gumga.gestures.desktop;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.openni.VideoFrameRef;
import org.openni.VideoStream;

import static com.primesense.nite.JointType.*;
import com.primesense.nite.JointType;
import com.primesense.nite.Point2D;
import com.primesense.nite.Skeleton;
import com.primesense.nite.SkeletonJoint;
import com.primesense.nite.SkeletonState;
import com.primesense.nite.UserData;
import com.primesense.nite.UserTracker;
import com.primesense.nite.UserTrackerFrameRef;

public class GumgaGestureViewer extends Component implements VideoStream.NewFrameListener, UserTracker.NewFrameListener {

	private final Runnable runnable;

	private VideoStream videoStream;
	private UserTracker userTracker;

	private transient VideoFrameRef videoFrame;
	private transient int[] imagePixels;
	private transient UserTrackerFrameRef userFrame;
	private Map<JointType, Point2D<Float>> screenCoordsJoints;

	private Robot robot;

	private String fraseEsquerda = "";
	private String fraseDireita = "";
	private long tempoUltimaPoseDireita;
	private long tempoUltimaPoseEsquerda;
	private long tempoAviso;
	private String mensagemAviso = "******";

	public GumgaGestureViewer(VideoStream videoStream, UserTracker userTracker) {
		runnable = (Runnable) Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.exclamation");
		screenCoordsJoints = new HashMap<>();
		this.videoStream = videoStream;
		this.userTracker = userTracker;
		this.setPreferredSize(new Dimension(640, 480));
		this.videoStream.addNewFrameListener(this);
		this.userTracker.addNewFrameListener(this);
		try {
			robot = new Robot();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		aviso("INICIO", 4000);
	}
	
	

	public Map<JointType, Point2D<Float>> getScreenCoordsJoints() {
		return screenCoordsJoints;
	}



	@Override
	public synchronized void onNewFrame(UserTracker arg0) {
		userFrame = userTracker.readFrame();
		screenCoordsJoints.clear();
		if (userFrame != null && !userFrame.getUsers().isEmpty()) {
			UserData userData = userFrame.getUsers().get(0);
			float z=100000;
			for (UserData ud : userFrame.getUsers()) {
				SkeletonJoint h = ud.getSkeleton().getJoint(HEAD);
				if (h!=null && h.getPosition().getZ()<z){
					userData=ud;
					z=h.getPosition().getZ();
				}
			}

			userTracker.startSkeletonTracking(userData.getId());
			if (userData.getSkeleton().getState() == SkeletonState.TRACKED) {
				Skeleton skeleton = userData.getSkeleton();
				processaGestos(skeleton);
				for (JointType key : JointType.values()) {
					Point2D<Float> convertJointCoordinatesToDepth = userTracker.convertJointCoordinatesToDepth(skeleton.getJoint(key).getPosition());
					screenCoordsJoints.put(key, converteTamhoTela(convertJointCoordinatesToDepth));
				}
			}

		}
	}

	private Point2D<Float> converteTamhoTela(Point2D<Float> pontoVelho) {
		float x = pontoVelho.getX() * getWidth() / 640;
		float y = pontoVelho.getY() * getHeight() / 480;
		try {
			Field fx = pontoVelho.getClass().getDeclaredField("mX");
			fx.setAccessible(true);
			fx.set(pontoVelho, x);
			Field fy = pontoVelho.getClass().getDeclaredField("mY");
			fy.setAccessible(true);
			fy.set(pontoVelho, y);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return pontoVelho;
	}

	@Override
	public synchronized void onFrameReady(VideoStream videoStream) {

		videoFrame = videoStream.readFrame();

		// preciso transformar este frame em um Buffer de Bytes
		ByteBuffer frameData = videoFrame.getData().order(ByteOrder.BIG_ENDIAN);

		// vou criar uma imagem de pixels
		imagePixels = new int[640 * 480];
		int pos = 0;
		while (frameData.remaining() > 0) {
			int red = (int) (frameData.get() & 0xFF);
			int green = (int) (frameData.get() & 0xFF);
			int blue = (int) (frameData.get() & 0xFF);
			imagePixels[pos++] = 0xFF000000 | red << 16 | green << 8 | blue;

		}
		repaint();

	}

	public void paint(Graphics g) {
		if (videoFrame == null) {
			return;
		}
		int width = videoFrame.getWidth();
		int height = videoFrame.getHeight();

		if (mostraVideo) {
			BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			bufferedImage.setRGB(0, 0, width, height, imagePixels, 0, width);
			g.drawImage(bufferedImage, 0, 0, this.getWidth(), this.getHeight(), null);
		} else {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, this.getWidth(), this.getHeight());
		}
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 8));

		desenhaEsqueleto((Graphics2D) g);
		desenhaGrid((Graphics2D) g);
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 40));
		//g.drawString(ultimaPoseMaoDireita, 5, 30);
		//g.drawString(fraseDireita, 5, 70);
		//g.drawString(ultimaPoseMaoEsquerda, 5, 110);
		//g.drawString(fraseEsquerda, 5, 150);
		desenhaAviso((Graphics2D) g);
	}

	public void desenhaAviso(Graphics2D g) {
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, this.getHeight()/10));
		if (System.currentTimeMillis() < tempoAviso) {
			g.drawString(mensagemAviso, 20, this.getHeight() - 5);
		}
	}

	private void desenhaEsqueleto(Graphics2D g2d) {
		JointType[] joints = { HEAD, NECK, LEFT_SHOULDER, RIGHT_SHOULDER, LEFT_HAND, RIGHT_HAND, TORSO };

		g2d.setColor(Color.RED);
		for (JointType key : joints) {
			Point2D<Float> point2d = screenCoordsJoints.get(key);
			if (point2d != null) {
				g2d.fillOval(point2d.getX().intValue(), point2d.getY().intValue(), 10, 10);
				g2d.drawString(key.toString(), point2d.getX(), point2d.getY());
			}
		}

		g2d.setColor(Color.GREEN);
		g2d.setStroke(new BasicStroke(5));
		if (screenCoordsJoints.get(HEAD) != null && screenCoordsJoints.get(NECK) != null) {
			int raio = Math.abs(screenCoordsJoints.get(HEAD).getY().intValue() - screenCoordsJoints.get(NECK).getY().intValue());
			g2d.drawOval(screenCoordsJoints.get(HEAD).getX().intValue() - raio / 2, screenCoordsJoints.get(HEAD).getY().intValue() - raio / 2, raio, raio);
		}
		desenhaLinha(g2d, HEAD, NECK);
		desenhaLinha(g2d, TORSO, NECK);
		desenhaLinha(g2d, NECK, LEFT_SHOULDER);
		desenhaLinha(g2d, LEFT_ELBOW, LEFT_SHOULDER);
		desenhaLinha(g2d, LEFT_ELBOW, LEFT_HAND);
		desenhaLinha(g2d, NECK, RIGHT_SHOULDER);
		desenhaLinha(g2d, RIGHT_ELBOW, RIGHT_SHOULDER);
		desenhaLinha(g2d, RIGHT_ELBOW, RIGHT_HAND);
		desenhaLinha(g2d, TORSO, LEFT_HIP);
		desenhaLinha(g2d, LEFT_KNEE, LEFT_HIP);
		desenhaLinha(g2d, LEFT_KNEE, LEFT_FOOT);
		desenhaLinha(g2d, TORSO, RIGHT_HIP);
		desenhaLinha(g2d, RIGHT_KNEE, RIGHT_HIP);
		desenhaLinha(g2d, RIGHT_KNEE, RIGHT_FOOT);
	}

	private void desenhaLinha(Graphics2D g2d, JointType origem, JointType destino) {
		Point2D<Float> o = screenCoordsJoints.get(origem);
		if (o == null) {
			return;
		}
		Point2D<Float> d = screenCoordsJoints.get(destino);
		if (d == null) {
			return;
		}
		g2d.drawLine(o.getX().intValue(), o.getY().intValue(), d.getX().intValue(), d.getY().intValue());

	}

	private void desenhaGrid(Graphics2D g2d) {
		g2d.setStroke(new BasicStroke(1));
		List<Integer> xs = new ArrayList<>();
		List<Integer> ys = new ArrayList<>();
		if (screenCoordsJoints.get(JointType.LEFT_SHOULDER) == null) {
			return;
		}
		if (screenCoordsJoints.get(JointType.RIGHT_SHOULDER) == null) {
			return;
		}
		if (screenCoordsJoints.get(JointType.TORSO) == null) {
			return;
		}
		if (screenCoordsJoints.get(JointType.HEAD) == null) {
			return;
		}
		if (screenCoordsJoints.get(JointType.NECK) == null) {
			return;
		}
		if (screenCoordsJoints.get(JointType.LEFT_HIP) == null) {
			return;
		}
		if (screenCoordsJoints.get(JointType.RIGHT_HIP) == null) {
			return;
		}

		xs.add(screenCoordsJoints.get(JointType.LEFT_SHOULDER).getX().intValue());
		xs.add(screenCoordsJoints.get(JointType.TORSO).getX().intValue());
		xs.add(screenCoordsJoints.get(JointType.RIGHT_SHOULDER).getX().intValue());

		ys.add(screenCoordsJoints.get(JointType.HEAD).getY().intValue());
		ys.add(screenCoordsJoints.get(JointType.NECK).getY().intValue());
		ys.add(screenCoordsJoints.get(JointType.TORSO).getY().intValue());
		ys.add((screenCoordsJoints.get(JointType.LEFT_HIP).getY().intValue() + screenCoordsJoints.get(JointType.RIGHT_HIP).getY().intValue()) / 2);

		for (Integer y : ys) {
			g2d.drawLine(xs.get(0), y, xs.get(2), y);
		}
		for (Integer x : xs) {
			g2d.drawLine(x, ys.get(0), x, ys.get(3));
		}
	}

	private int comparadorY(Skeleton skel, JointType jt1, JointType jt2, double d) {

		double valor1 = skel.getJoint(jt1).getPosition().getY();
		double valor2 = skel.getJoint(jt2).getPosition().getY();
		if (Math.abs(valor1 - valor2) < d) {
			return 0;
		}
		if (valor1 < valor2) {
			return -1;
		}
		return 1;
	}

	private int comparadorX(Skeleton skel, JointType jt1, JointType jt2, double d) {
		double valor1 = skel.getJoint(jt1).getPosition().getX();
		double valor2 = skel.getJoint(jt2).getPosition().getX();
		if (Math.abs(valor1 - valor2) < d) {
			return 0;
		}
		if (valor1 < valor2) {
			return -1;
		}
		return 1;
	}

	private String ultimaPoseMaoDireita = "";
	private String ultimaPoseMaoEsquerda = "";

	private boolean mostraVideo = true;

	private void processaGestos(Skeleton skel) {
		String poseCorrenteMaoDireita = "";
		String poseCorrenteMaoEsquerda = "";

		String poseCorrente = ">NEUTRA";
		if (comparadorY(skel, RIGHT_HAND, HEAD, 5) > 0 && comparadorX(skel, RIGHT_HAND, TORSO, 10) > 0) {
			poseCorrenteMaoDireita = ">MAO_DIREITA_ACIMA_DIREITA";
		}

		if (comparadorY(skel, RIGHT_HAND, HEAD, 5) > 0 && comparadorX(skel, RIGHT_HAND, TORSO, 10) < 0) {
			poseCorrenteMaoDireita = ">MAO_DIREITA_ACIMA_ESQUERDA";
		}

		if (comparadorY(skel, RIGHT_HAND, RIGHT_HIP, 5) > 0 && comparadorY(skel, RIGHT_HAND, HEAD, 10) < 0 && comparadorX(skel, RIGHT_HAND, RIGHT_ELBOW, 10) > 0) {
			poseCorrenteMaoDireita = ">MAO_DIREITA_CENTRO_DIREITA";
		}

		if (comparadorY(skel, RIGHT_HAND, RIGHT_HIP, 10) > 0 && comparadorY(skel, RIGHT_HAND, HEAD, 10) < 0 && comparadorX(skel, RIGHT_HAND, RIGHT_ELBOW, 10) < 0) {
			poseCorrenteMaoDireita = ">MAO_DIREITA_CENTRO_ESQUERDA";
		}

		if (comparadorY(skel, LEFT_HAND, JointType.HEAD, 5) > 0 && comparadorX(skel, JointType.LEFT_HAND, TORSO, 10) < 0) {
			poseCorrenteMaoEsquerda = ">MAO_ESQUERDA_ACIMA_ESQUERDA";
		}

		if (comparadorY(skel, LEFT_HAND, JointType.HEAD, 5) > 0 && comparadorX(skel, JointType.LEFT_HAND, TORSO, 10) > 0) {
			poseCorrenteMaoEsquerda = ">MAO_ESQUERDA_ACIMA_DIREITA";
		}

		if (comparadorY(skel, LEFT_HAND, JointType.LEFT_HIP, 5) > 0 && comparadorY(skel, LEFT_HAND, JointType.HEAD, 10) < 0 && comparadorX(skel, JointType.LEFT_HAND, LEFT_ELBOW, 10) < 0) {
			poseCorrenteMaoEsquerda = ">MAO_ESQUERDA_CENTRO_ESQUERDA";
		}

		if (comparadorY(skel, LEFT_HAND, JointType.LEFT_HIP, 5) > 0 && comparadorY(skel, LEFT_HAND, JointType.HEAD, 10) < 0 && comparadorX(skel, JointType.LEFT_HAND, LEFT_ELBOW, 10) > 0) {
			poseCorrenteMaoEsquerda = ">MAO_ESQUERDA_CENTRO_DIREITA";
		}
		if (!poseCorrenteMaoDireita.equals(ultimaPoseMaoDireita)) {
			tempoUltimaPoseDireita = System.currentTimeMillis();
			fraseDireita += poseCorrenteMaoDireita;
			ultimaPoseMaoDireita = poseCorrenteMaoDireita;
			if (interpretaFrases(fraseDireita)) {
				fraseDireita = "";
			}
		}
		if (System.currentTimeMillis() - tempoUltimaPoseDireita > 1000) {
			fraseDireita = poseCorrenteMaoDireita;
		}

		if (!poseCorrenteMaoEsquerda.equals(ultimaPoseMaoEsquerda)) {
			tempoUltimaPoseEsquerda = System.currentTimeMillis();
			ultimaPoseMaoEsquerda = poseCorrenteMaoEsquerda;
			fraseEsquerda += poseCorrenteMaoEsquerda;
			if (interpretaFrases(fraseEsquerda)) {
				fraseEsquerda = "";
			}
		}

		if (System.currentTimeMillis() - tempoUltimaPoseEsquerda > 1000) {
			fraseEsquerda = poseCorrenteMaoEsquerda;
		}

	}

	private boolean interpretaFrases(String frase) {
		if (frase.contains(">MAO_DIREITA_CENTRO_DIREITA>MAO_DIREITA_CENTRO_ESQUERDA")) {
			aviso("ESQUERDA", 2000);
			robot.keyPress(KeyEvent.VK_LEFT);
			robot.keyRelease(KeyEvent.VK_LEFT);
			return true;
		}
		if (frase.contains(">MAO_ESQUERDA_CENTRO_ESQUERDA>MAO_ESQUERDA_CENTRO_DIREITA")) {
			aviso("DIREITA", 2000);
			robot.keyPress(KeyEvent.VK_RIGHT);
			robot.keyRelease(KeyEvent.VK_RIGHT);
			return true;
		}
		if (frase.contains(">MAO_DIREITA_CENTRO_DIREITA>MAO_DIREITA_ACIMA_DIREITA")) {
			aviso("SOBE", 2000);
			robot.keyPress(KeyEvent.VK_PAGE_DOWN);
			robot.keyRelease(KeyEvent.VK_PAGE_DOWN);
			return true;
		}

		if (frase.contains(">MAO_ESQUERDA_ACIMA_ESQUERDA>MAO_ESQUERDA_CENTRO_ESQUERDA")) {
			aviso("DESCE", 2000);
			robot.keyPress(KeyEvent.VK_PAGE_UP);
			robot.keyRelease(KeyEvent.VK_PAGE_UP);
			return true;
		}
		return false;

	}

	private int avisoNr = 0;

	public void aviso(String aviso, long tempo) {
		avisoNr++;
		if (runnable != null)
			runnable.run();
		mensagemAviso = aviso + " " + avisoNr;
		tempoAviso = System.currentTimeMillis() + tempo;
	}

	public void setMostraVideo(boolean selected) {
		this.mostraVideo = selected;
	}

	public boolean isMostraVideo() {
		return this.mostraVideo;
	}

}
