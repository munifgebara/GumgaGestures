package br.com.gumga.gestures.desktop;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.openni.Device;
import org.openni.DeviceInfo;
import org.openni.OpenNI;
import org.openni.PixelFormat;
import org.openni.SensorType;
import org.openni.VideoMode;
import org.openni.VideoStream;

import com.primesense.nite.NiTE;
import com.primesense.nite.UserTracker;

public class Program {

	private List<DeviceInfo> deviceList; // lista dos dispositivos encontrados
	private Device device; // referencia ao Kinect que sera usado
	private VideoStream videoStream; // objeto que manipula a captura de frames
	private VideoMode mode; // modo de captura que será adotado
	private UserTracker userTracker; // objeto para mapeamento do usuario

	private MainWindow mainWindow; // telinha que irá conter a interface
									// exibir o video

	private Properties properties;

	public Program() {
		loadProperties();
		//validaInstancia();

		try {
			OpenNI.initialize(); // efetivar o "Load" do driver
			System.out.println("DEBUG: OpenNI initialized");
			NiTE.initialize(); // efetivar o "Load" do driver de UserTracking
			System.out.println("DEBUG: NiTE initialized");

			deviceList = OpenNI.enumerateDevices(); // recupere todos os
													// dispositivos
													// reconhecidos na maquina
			if (deviceList.isEmpty()) {
				JOptionPane.showMessageDialog(null,"Kinect não encontrado, verifique os drivers.");
				System.exit(1);
			}
			// listando dispositivos encontrados
			for (DeviceInfo info : deviceList) {
				System.out.println("DEBUG: Found " + info.getName() + "( " + info.getVendor() + " )");
			}

			// obtendo o dispositivo específico da lista
			device = Device.open(deviceList.get(0).getUri());
			System.out.println("DEBUG: Kinect Opened " + deviceList.get(0).getUri());

			// inicio o objeto indicando captura de frames coloridos (RGB)
			videoStream = VideoStream.create(device, SensorType.COLOR);

			// vou recuperar temporariamente todos os modos suportados no kinect
			List<VideoMode> tmpVideoMode = videoStream.getSensorInfo().getSupportedVideoModes();
			for (VideoMode mode : tmpVideoMode) {
				System.out.println("       Video Mode " + mode.getPixelFormat() + " " + mode.getResolutionX() + "," + mode.getResolutionY() + " - " + mode.getFps() + "fps");
				// insiro a resolução necessária no meu video stream
				if (mode.getPixelFormat() == PixelFormat.RGB888 && mode.getResolutionX() == 640 && mode.getResolutionY() == 480) {
					this.mode = mode;
					System.out.println("DEBUG: " + mode.getPixelFormat() + " inserted");
				}
			}

			videoStream.setVideoMode(this.mode);
			videoStream.start();
			System.out.println("DEBUG: Video Stream started!");

			userTracker = UserTracker.create();
			System.out.println("DEBUG: User Tracker Created!");

			// criei o CoreGame (agora vai!!!)
			final GumgaGestureViewer ggv = new GumgaGestureViewer(videoStream, userTracker);
			mainWindow = new MainWindow(ggv);
			final JPanel painel=new JPanel(new FlowLayout());
			painel.setVisible(false);
			final JCheckBox videoCheckBox = new JCheckBox("Video",true);
			videoCheckBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					ggv.setMostraVideo(videoCheckBox.isSelected());
				}
			});
			final JCheckBox sempreCheckBox = new JCheckBox("Visível",false);
			sempreCheckBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					mainWindow.setAlwaysOnTop(sempreCheckBox.isSelected());
				}
			});
			JButton btFechar=new JButton("X");
			btFechar.setBorder(null);
			btFechar.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					System.exit(0);
				}
			});
			((FlowLayout)painel.getLayout()).setAlignment(FlowLayout.RIGHT);
			painel.setBorder(null);

			painel.add(new JLabel("Gumga"));
			painel.add(videoCheckBox);
			painel.add(sempreCheckBox);
			painel.add(btFechar);
			
			mainWindow.add(painel,BorderLayout.BEFORE_FIRST_LINE);
			mainWindow.add(ggv,BorderLayout.CENTER);
			mainWindow.pack();
			mainWindow.colocaNoCanto();
			mainWindow.setVisible(true);
			//new PoseServer(ggv);
			new HttpsPoseServer(ggv);
			//new PoseServer(ggv);

		} catch (Exception ex) {
			System.err.println("APPLICATION: " + ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	

	private void validaInstancia() {
		String tk = properties.getProperty("gumgaToken");
		String id = properties.getProperty("instanceId");
		String securityUrl = properties.getProperty("securityUrl");
		try {
			String resp=getHTML("http://192.168.25.250/security-api/api/instance/"+id+"?gumgaToken="+tk);
			if (!resp.contains("GUMGAGESTURES")){
				JOptionPane.showMessageDialog(null, "Software não autorizado no segurança");
				System.out.println("Software não autorizado no segurança");
				System.exit(1);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	private void loadProperties() {
		properties = new Properties();
		try {
			File file = new File(System.getProperty("user.home") + "/gumgafiles/gumga-gestures.properties");
			if (file.exists()) {
				System.out.println ("Arquivo existe");
				InputStream is=new FileInputStream(file);
				properties.load(is);
			} else {
				System.out.println ("Arquivo novo ");
				properties.setProperty("gumgaToken", "SEM_TOKEN");
				properties.setProperty("instanceId", "0");
				properties.setProperty("securityUrl","http://192.168.25.250/security-api");
			}
			FileOutputStream fo = new FileOutputStream(file);
			properties.store(fo, "Arquivo de configurações do GUMGA-GESTURES");
			fo.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public void stop() {
		videoStream.stop();
		OpenNI.shutdown();
		NiTE.shutdown();
	}
	
	public String getHTML(String urlToRead) throws Exception {
	      StringBuilder result = new StringBuilder();
	      URL url = new URL(urlToRead);
	      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	      conn.setRequestMethod("GET");
	      BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	      String line;
	      while ((line = rd.readLine()) != null) {
	         result.append(line);
	      }
	      rd.close();
	      return result.toString();
	   }

	public static void main(String args[]) {
		new Program();
	}

}
