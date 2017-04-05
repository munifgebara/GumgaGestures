package br.com.gumga.gestures.desktop;

import java.io.*;
import java.net.InetSocketAddress;
import java.lang.*;
import com.sun.net.httpserver.HttpsServer;
import java.security.KeyStore;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import com.primesense.nite.JointType;
import com.sun.net.httpserver.*;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

import javax.net.ssl.SSLContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsExchange;


public class HttpsPoseServer {

	private GumgaGestureViewer ggv;

	public HttpsPoseServer(GumgaGestureViewer ggv) {
		this.ggv = ggv;
		initServer();
	}

	public void initServer() {
		
		try {
            // setup the socket address
            InetSocketAddress address = new InetSocketAddress(8000);

            // initialise the HTTPS server
            HttpsServer httpsServer = HttpsServer.create(address, 0);
            SSLContext sslContext = SSLContext.getInstance("TLS");

            // initialise the keystore
            char[] password = "password".toCharArray();
            KeyStore ks = KeyStore.getInstance("JKS");
            String userDir=System.getProperty("user.home");
            FileInputStream fis = new FileInputStream(userDir+"/poseserverkey.jks");
            ks.load(fis, password);

            // setup the key manager factory
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, password);

            // setup the trust manager factory
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            // setup the HTTPS context and parameters
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    try {
                        // initialise the SSL context
                        SSLContext c = SSLContext.getDefault();
                        SSLEngine engine = c.createSSLEngine();
                        params.setNeedClientAuth(false);
                        params.setCipherSuites(engine.getEnabledCipherSuites());
                        params.setProtocols(engine.getEnabledProtocols());

                        // get the default parameters
                        SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                        params.setSSLParameters(defaultSSLParameters);

                    } catch (Exception ex) {
                        System.out.println("Failed to create HTTPS port");
                    }
                }
            });

			
			
			
			
			
	//		HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
            httpsServer.createContext("/pose", new PoseHandler());
            httpsServer.setExecutor(null); // creates a default executor
            httpsServer.start();
		} catch (Exception ex) {
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
		}

	}

}
