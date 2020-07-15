package convex.gui.manager.windows;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

import convex.gui.manager.PeerManager;

@SuppressWarnings("serial")
public abstract class BaseWindow extends JPanel {

	protected final PeerManager manager;

	public BaseWindow(PeerManager manager) {
		super();
		this.manager = manager;
		setLayout(new BorderLayout());
	}

	public abstract String getTitle();

	public JFrame launch() {
		JFrame f = new JFrame(getTitle());
		f.getContentPane().add(this);
		f.pack();
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.setLocationRelativeTo(PeerManager.getFrame());
		return f;
	}
}
