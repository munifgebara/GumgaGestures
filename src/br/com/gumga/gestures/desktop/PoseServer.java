//https://127.0.0.1:8000/pose

package br.com.gumga.gestures.desktop;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;

import com.primesense.nite.JointType;
import com.primesense.nite.Point2D;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class PoseServer {

	private GumgaGestureViewer ggv;

	public PoseServer(GumgaGestureViewer ggv) {
		this.ggv = ggv;
		initServer();
	}

	public void initServer() {
		
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(18000), 0);
			server.createContext("/pose", new PoseHandler());
			server.setExecutor(null); // creates a default executor
			server.start();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
	}

	class PoseHandler implements HttpHandler {

		@Override
		public void handle(HttpExchange he) throws IOException {
			final int TEMPO = 75;
			he.getResponseHeaders().set("Content-Type", "text/event-stream");
			he.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
			he.getResponseHeaders().set("Access-Control-Allow-Method", "POST,  GET, PUT, DELETE, OPTIONS,HEAD");
			he.sendResponseHeaders(200, 0);
			try {
				while (true) {
					StringBuilder sb = new StringBuilder(1024);
					Map<JointType, Point3D> pontos = ggv.getSkel3d();
					sb.append("data: {");
					for (JointType key : pontos.keySet()) {
						Point3D point3d = pontos.get(key);
						if (point3d != null) {
							sb.append("\"" + key.name() + "\":{\"x\":\"" + point3d.x 
									+ "\",\"y\":\"" + point3d.y  
									+ "\",\"z\":\"" + point3d.z 
									+ "\"},");
						}
					}
					if (ggv.getTempoAviso()<System.currentTimeMillis()){
					   sb.append("\"aviso\":\"" + ggv.getMensagemAviso() + "\",");
					}
					sb.append("\"status\":\"" + (pontos.size() == 0 ? "NO" : "OK") + "\"");
					sb.append("}\n\n");

					byte[] bytes = sb.toString().getBytes();
					OutputStream responseBody = he.getResponseBody();
					responseBody.write(bytes);
					responseBody.flush();
					Thread.sleep(TEMPO);
				}
			} catch (

			Exception ex) {
				ex.printStackTrace();
			}
			he.getResponseBody().close();
			he.close();
		}

	}

}
