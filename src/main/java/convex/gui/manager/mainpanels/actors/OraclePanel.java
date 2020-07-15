package convex.gui.manager.mainpanels.actors;

import java.awt.BorderLayout;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import convex.core.Init;
import convex.core.State;
import convex.core.data.Address;
import convex.core.data.MapEntry;
import convex.core.lang.RT;
import convex.core.lang.Reader;
import convex.core.util.Utils;
import convex.gui.components.ActionPanel;
import convex.gui.components.CodeLabel;
import convex.gui.components.DefaultReceiveAction;
import convex.gui.components.Toast;
import convex.gui.manager.PeerManager;
import convex.gui.manager.Toolkit;
import convex.gui.manager.mainpanels.WalletPanel;
import convex.gui.manager.models.OracleTableModel;
import convex.net.Message;
import convex.net.ResultConsumer;

@SuppressWarnings("serial")
public class OraclePanel extends JPanel {

	public static final Logger log = Logger.getLogger(OraclePanel.class.getName());

	Address oracleAddress = Init.ORACLE_ADDRESS;

	OracleTableModel tableModel = new OracleTableModel(PeerManager.getLatestState(), oracleAddress);
	JTable table = new JTable(tableModel);

	JScrollPane scrollPane = new JScrollPane(table);;

	long key = 1;

	public OraclePanel() {
		this.setLayout(new BorderLayout());

		// ===========================================
		// Top label
		add(new CodeLabel("Oracle at address: " + oracleAddress.toChecksumHex() + "\n" + "Executing as user: "
				+ Init.HERO.toChecksumHex()), BorderLayout.NORTH);

		// ===========================================
		// Central table
		PeerManager.getStateModel().addPropertyChangeListener(pc -> {
			State newState = (State) pc.getNewValue();
			tableModel.setState(newState);
		});

		// Column layouts
		DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
		leftRenderer.setHorizontalAlignment(JLabel.LEFT);
		table.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);
		table.getColumnModel().getColumn(0).setPreferredWidth(80);
		table.getColumnModel().getColumn(1).setCellRenderer(leftRenderer);
		table.getColumnModel().getColumn(1).setPreferredWidth(300);
		table.getColumnModel().getColumn(2).setCellRenderer(leftRenderer);
		table.getColumnModel().getColumn(2).setPreferredWidth(80);
		table.getColumnModel().getColumn(3).setCellRenderer(leftRenderer);
		table.getColumnModel().getColumn(3).setPreferredWidth(300);

		// fonts
		table.setFont(Toolkit.SMALL_MONO_FONT);
		table.getTableHeader().setFont(Toolkit.SMALL_MONO_FONT);

		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // useful in scroll pane
		scrollPane.getViewport().setBackground(null);
		add(scrollPane, BorderLayout.CENTER);

		// ============================================
		// Action buttons
		ActionPanel actionPanel = new ActionPanel();
		add(actionPanel, BorderLayout.SOUTH);

		JButton createButton = new JButton("Create...");
		actionPanel.add(createButton);
		createButton.addActionListener(e -> {
			String desc = JOptionPane.showInputDialog(this, "Enter Oracle description as plain text:");
			if ((desc == null) || (desc.isBlank())) return;

			Object code = Reader.read("(call \"" + oracleAddress.toHexString() + "\" " + "(register " + (key++)
					+ "  {:desc \"" + desc + "\" }))");
			execute(code);
		});

		JButton finaliseButton = new JButton("Finalise...");
		actionPanel.add(finaliseButton);
		finaliseButton.addActionListener(e -> {
			String value = JOptionPane.showInputDialog(this, "Enter final value:");
			if ((value == null) || (value.isBlank())) return;
			int ix = table.getSelectedRow();
			if (ix < 0) return;
			MapEntry<Object, Object> me = tableModel.getList().entryAt(ix);

			Object code = Reader.read(
					"(call \"" + oracleAddress.toHexString() + "\" " + "(provide " + me.getKey() + " " + value + "))");
			execute(code);
		});

		JButton makeMarketButton = new JButton("Make Market");
		actionPanel.add(makeMarketButton);
		makeMarketButton.addActionListener(e -> {
			int ix = table.getSelectedRow();
			if (ix < 0) return;
			MapEntry<Object, Object> me = tableModel.getList().entryAt(ix);
			Object key = me.getKey();
			log.info("Making market: " + key);

			String opts = JOptionPane
					.showInputDialog("Enter a list of possible values (forms, may separate with commas)");
			if ((opts == null) || opts.isBlank()) {
				Toast.display(scrollPane, "Prediction market making cancelled", Toast.INFO);
				return;
			}
			String outcomeString = "[" + opts + "]";

			String actorCode;
			try {
				actorCode = Utils.readResourceAsString("actors/prediction-market.con");
				String source = "(let [pmc " + actorCode + " ] " + "(deploy pmc " + " \""
						+ oracleAddress.toChecksumHex() + "\" " + " " + key + " " + " " + outcomeString + "))";
				Object code = Reader.read(source);
				PeerManager.execute(WalletPanel.HERO, code, createMarketAction);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		});
	}

	private void execute(Object code) {
		PeerManager.execute(WalletPanel.HERO, code, receiveAction);
	}

	private final ResultConsumer createMarketAction = new ResultConsumer() {
		@Override
		protected void handleResult(Object m) {
			if (m instanceof Address) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// ignore
				}
				Address addr = (Address) m;
				MarketsPanel.marketList.addElement(addr);
				showResult("Prediction market deployed: " + addr);
			} else {
				String resultString = "Expected Address but got: " + m;
				log.warning(resultString);
				Toast.display(scrollPane, resultString, Toast.FAIL);
			}
		}

		@Override
		protected void handleError(Message m) {
			showError(m);
		}
	};

	private final ResultConsumer receiveAction = new DefaultReceiveAction(scrollPane);

	private void showError(Message m) {
		Object em;
		try {
			em = RT.nth(m.getPayload(), 1);
		} catch (Exception e) {
			em = e.getMessage();
		}
		String resultString = "Error executing transaction: " + em;
		log.info(resultString);
		Toast.display(scrollPane, resultString, Toast.FAIL);
	}

	private void showResult(Object v) {
		String resultString = "Transaction executed successfully\n" + "Result: " + Utils.toString(v);
		log.info(resultString);
		Toast.display(scrollPane, resultString, Toast.SUCCESS);
	}
}
