package br.com.gumga.gestures.desktop;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.print.attribute.SetOfIntegerSyntax;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JSeparator;

public class MainWindow extends JFrame implements ActionListener, MouseListener {

	private enum Tamanho {
		PEQUENO, MEDIO, GRANDE
	};

	private enum Canto {
		INFERIOR_DIREITO, INFERIOR_ESQUERDO, SUPERIOR_ESQUERDO, SUPERIOR_DIREITO
	};

	private int originalX;
	private int originalY;

	private Tamanho tamanho = Tamanho.GRANDE;
	private Canto canto = Canto.INFERIOR_DIREITO;
	private GumgaGestureViewer ggv;
	JPopupMenu popup;
	JCheckBoxMenuItem sempre;
	JCheckBoxMenuItem video;
	JMenuItem sair;

	
	public MainWindow(GumgaGestureViewer ggv) {
		super("Gumga Gestures");
		this.ggv=ggv;
		setUndecorated(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		addMouseListener(this);
		criaMenu();
	}
	

	private void criaMenu() {
		

	    popup = new JPopupMenu();
	    popup.add(new JLabel("  Gumga Gestures"));
	    sempre = new JCheckBoxMenuItem("Sempre Visível",false);
	    sempre.addActionListener(this);
	    popup.add(sempre);
	    video = new JCheckBoxMenuItem("Mostra Video",false);
	    video.addActionListener(this);
	    popup.add(video);
	    popup.add(new JSeparator());
	    sair = new JMenuItem("Sair");
	    sair.addActionListener(this);
	    popup.add(sair);
	    
	    
	    MouseListener popupListener = new PopupListener();
	    this.addMouseListener(popupListener);
	    this.addMouseListener(popupListener);
	
			
	}
	class PopupListener extends MouseAdapter {
	    public void mousePressed(MouseEvent e) {
	        maybeShowPopup(e);
	    }

	    public void mouseReleased(MouseEvent e) {
	        maybeShowPopup(e);
	    }

	    private void maybeShowPopup(MouseEvent e) {
	        if (e.isPopupTrigger()) {
	            popup.show(e.getComponent(),e.getX(), e.getY());
	        }
	    }
	}

	public void colocaNoCanto() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (originalX == 0) {
			originalX = this.getWidth();
			originalY = this.getHeight();
		}
		if (tamanho == Tamanho.GRANDE) {
			setSize(new Dimension(originalX, originalY));
		}
		if (tamanho == Tamanho.MEDIO) {
			setSize(new Dimension(originalX / 2, originalY / 2));
		}
		if (tamanho == Tamanho.PEQUENO) {
			setSize(new Dimension(originalX / 3, originalY / 3));
		}
		if (canto==Canto.INFERIOR_DIREITO) {
			setLocation(screenSize.width - this.getWidth(), screenSize.height - this.getHeight());
		}
		if (canto==Canto.INFERIOR_ESQUERDO) {
			setLocation(0, screenSize.height - this.getHeight());
		}
		if (canto==Canto.SUPERIOR_ESQUERDO) {
			setLocation(0,0);
		}
		if (canto==Canto.SUPERIOR_DIREITO) {
			setLocation(screenSize.width - this.getWidth(), 0);
		}

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource()==video){
			ggv.setMostraVideo(!ggv.isMostraVideo());
		}
		if (e.getSource()==sempre){
			setAlwaysOnTop(!isAlwaysOnTop());
		}
		if (e.getSource()==sair){
			System.exit(0);

		}

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getButton()==MouseEvent.BUTTON1 && e.getClickCount() == 2) {
			if (tamanho == Tamanho.GRANDE) {
				tamanho = Tamanho.MEDIO;
			} else if (tamanho == Tamanho.MEDIO) {
				tamanho = Tamanho.PEQUENO;
			}
			else if (tamanho == Tamanho.PEQUENO) {
				tamanho = Tamanho.GRANDE;
				if (canto == Canto.INFERIOR_DIREITO) {
					canto = Canto.INFERIOR_ESQUERDO;
				} else if (canto == Canto.INFERIOR_ESQUERDO) {
					canto = Canto.SUPERIOR_ESQUERDO;
				} else if (canto == Canto.SUPERIOR_ESQUERDO) {
					canto = Canto.SUPERIOR_DIREITO;
				} else if (canto == Canto.SUPERIOR_DIREITO) {
					canto = Canto.INFERIOR_DIREITO;
				}
			}
			colocaNoCanto();
		}

	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

}
