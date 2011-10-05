package wrims.schematic.jdiagram;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Float;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import wrims.schematic.MainFrame;
import wrims.schematic.element.Element;

import com.mindfusion.diagramming.Align;
import com.mindfusion.diagramming.AttachToNode;
import com.mindfusion.diagramming.Behavior;
import com.mindfusion.diagramming.Diagram;
import com.mindfusion.diagramming.DiagramAdapter;
import com.mindfusion.diagramming.DiagramItem;
import com.mindfusion.diagramming.DiagramItemList;
import com.mindfusion.diagramming.DiagramNode;
import com.mindfusion.diagramming.DiagramNodeList;
import com.mindfusion.diagramming.DiagramView;
import com.mindfusion.diagramming.Group;
import com.mindfusion.diagramming.LinkValidationEvent;
import com.mindfusion.diagramming.NodeEvent;
import com.mindfusion.diagramming.NodeValidationEvent;
import com.mindfusion.diagramming.Overview;
import com.mindfusion.diagramming.SelectionStyle;
import com.mindfusion.diagramming.ShapeNode;
import com.mindfusion.diagramming.TextFormat;
import com.mindfusion.diagramming.XmlException;

/**
 * View schematic
 * 
 * @author psandhu
 * 
 */
public class SchematicViewer extends JPanel {

	private static final Object VALUE_TEXT = "__VT__";
	private DiagramView diagramView;
	private Diagram diagram;
	private String filename;
	private Action zoomInAction, zoomOutAction;
	private Overview overview;
	private ElementTask clickTask;
	private boolean enableClick;
	private float zoomFactor = 1.15f;
	protected Rectangle2D.Float lastVisibleRect;
	private MainFrame schematic;
	private boolean showValueBoxes;

	/**
	 * Add diagram viewer to a scrollpane
	 */
	public SchematicViewer() {
		PanelWithCollapsibleInsetPanel panel = new PanelWithCollapsibleInsetPanel(
				true);
		panel.collapse();
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		this.add(panel);

		diagram = new Diagram();
		diagramView = new DiagramView();
		diagramView.setDiagram(diagram);
		diagramView.setBehavior(Behavior.Modify);
		panel.getMainPanel().add(new JScrollPane(diagramView));

		overview = new Overview();
		overview.setDiagramView(diagramView);
		overview.setFitAll(true);
		panel.getInsetPanel().add(overview);

		diagram.getSelection().setStyle(SelectionStyle.SelectionHandles);
		diagram.getSelection().setAllowMultipleSelection(true);
		diagram.addDiagramListener(new DiagramAdapter() {
			@Override
			public void viewportChanged() {
				refreshValues(false);
			}

			@Override
			public void linkModifying(LinkValidationEvent arg0) {
				arg0.cancelDrag();
			}

			@Override
			public void nodeModifying(NodeValidationEvent arg0) {
				arg0.cancelDrag();
			}

			@Override
			public void nodeClicked(NodeEvent e) {
				if (!enableClick) {
					return;
				}
				DiagramNode node = e.getNode();
				Element el = createElement(node);
				final Element fel = el;
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						if (clickTask != null) {
							clickTask.run(fel);
						}
					}
				});
			}

			private Element createElement(DiagramNode node) {
				String name = node.getTextToEdit();
				if (!(node instanceof ShapeNode))
					return null;
				ShapeNode shapeNode = (ShapeNode) node;
				Color color = shapeNode.getPen().getColor();
				String shapeId = shapeNode.getShape().getId();
				Element el = new Element(name, 0);
				if (shapeId.equals("Alternative") && color.getBlue() == 255) {
					el = new Element(name, Element.RESERVOIR);
				} else if (shapeId.equals("Rectangle")
						&& color.getBlue() == 183 && color.getGreen() == 183) {
					el = new Element(name, Element.INFLOW);
				} else if (shapeId.equals("Rectangle")
						&& color.toString().equals(
								"java.awt.Color[r=62,g=79,b=206]")) {
					el = new Element(name, Element.RESERVOIR);
				} else if (shapeId.equals("Ellipse")
						&& color.toString().equals(
								"java.awt.Color[r=0,g=0,b=255]")) {
					el = new Element(name, Element.DEMAND);
				} else if (shapeId.equals("Rectangle")
						&& color.toString().equals(
								"java.awt.Color[r=255,g=0,b=0]")) {
					el = new Element(name, Element.DEMAND);
				} else if (shapeId.equals("Rectangle")
						&& color.toString().equals(
								"java.awt.Color[r=0,g=0,b=255]")) {
					el = new Element(name, Element.CHANNEL);
				}
				return el;
			}

		});

		createActions();
	}

	public void setEnableClick(boolean click) {
		enableClick = true;
	}

	public boolean isEnableClick() {
		return enableClick;
	}

	public void setClickTask(ElementTask r) {
		this.clickTask = r;
	}

	public Diagram getDiagram() {
		return diagram;
	}

	@SuppressWarnings("serial")
	public void createActions() {
		zoomInAction = new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				overview.suspendRepaint();
				diagramView.suspendRepaint();
				Rectangle2D.Float rect = diagramView.deviceToDoc(diagramView
						.getVisibleRect());
				rect.setRect(rect.x + rect.width / 2 * (1 - 1 / zoomFactor),
						rect.y + rect.height / 2 * (1 - 1 / zoomFactor),
						rect.width / zoomFactor, rect.height / zoomFactor);
				diagramView.zoomToFit(rect);
				diagramView.resumeRepaint();
				overview.resumeRepaint();
			}

		};
		zoomInAction.putValue(Action.SHORT_DESCRIPTION, "zoom in");
		zoomInAction.putValue(Action.SMALL_ICON, ImageUtil
				.createImageIcon("/wrims/schematic/images/ZoomIn16.gif"));

		zoomOutAction = new AbstractAction() {

			@Override
			public void actionPerformed(ActionEvent e) {
				overview.suspendRepaint();
				diagramView.suspendRepaint();
				Rectangle2D.Float rect = diagramView.deviceToDoc(diagramView
						.getVisibleRect());
				rect.setRect(rect.x - rect.width / 2 * (zoomFactor - 1), rect.y
						- rect.height / 2 * (zoomFactor - 1), rect.width
						* zoomFactor, rect.height * zoomFactor);
				diagramView.zoomToFit(rect);
				diagramView.resumeRepaint();
				overview.resumeRepaint();
			}

		};
		zoomOutAction.putValue(Action.SHORT_DESCRIPTION, "zoom out");
		zoomOutAction.putValue(Action.SMALL_ICON, ImageUtil
				.createImageIcon("/wrims/schematic/images/ZoomOut16.gif"));

	}

	public Action getZoomInAction() {
		return zoomInAction;
	}

	public Action getZoomOutAction() {
		return zoomOutAction;
	}

	public Action getZoomNormalAction() {
		// TODO Auto-generated method stub
		return null;
	}

	public Action getZoomToFitAction() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Load schematic from binary or xml format (if file name ends with .xml)
	 * 
	 * @param filename
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws XmlException
	 */
	public void load(String filename) throws FileNotFoundException,
			IOException, XmlException {
		if (filename.endsWith(".xml")) {
			diagram.loadFromXml(filename);
		} else {
			diagram.loadFrom(filename);
		}
		this.filename = filename;
	}

	public Hashtable<String, Object> getVisibleNodes() {
		Rectangle2D.Float visibleRect = diagramView.deviceToDoc(diagramView
				.getVisibleRect());
		return getVisibleNodes(visibleRect);
	}

	public Hashtable<String, Object> getVisibleNodes(
			Rectangle2D.Float visibleRect) {
		Hashtable<String, Object> variables = new Hashtable<String, Object>();
		Iterable<ShapeNode> items = diagram.items(ShapeNode.class);
		for (ShapeNode item : items) {
			if (!item.getBounds().intersects(visibleRect))
				continue;
			variables.put(((ShapeNode) item).getTextToEdit(), item);
		}
		return variables;

	}

	public void refreshValues(boolean force) {
		if (!showValueBoxes || schematic == null) {
			return;
		}
		Rectangle2D.Float visibleRect = diagramView.deviceToDoc(diagramView
				.getVisibleRect());
		if (!force && visibleRect.equals(lastVisibleRect)) {
			return;
		}
		if (lastVisibleRect == null) {
			lastVisibleRect = visibleRect;
		}
		// System.out.println("viewportChanged: "+visibleRect+" on "+new
		// Date());
		clearValueBoxes(lastVisibleRect);
		lastVisibleRect = visibleRect;
		schematic.updateValues();
	}

	public void setShowValueBoxes(boolean show) {
		showValueBoxes = show;
		if (showValueBoxes) {
			refreshValues(true);
		} else {
			clearAllValueBoxes();
		}
	}

	protected void clearValueBoxes(Float visibleRect) {
		if (visibleRect == null) {
			return;
		}
		Hashtable<String, Object> visibleNodes = getVisibleNodes(visibleRect);
		DiagramNodeList nodes = diagram.getNodes();
		for (Object o : visibleNodes.values()) {
			if (o instanceof ShapeNode) {
				ShapeNode shapeNode = (ShapeNode) o;
				Object id = shapeNode.getId();
				if (id != null && id.equals(VALUE_TEXT)) {
					nodes.remove(shapeNode);
				}
			}
		}
	}

	protected void clearAllValueBoxes() {
		DiagramNode node;
		while ((node = diagram.findNodeById(VALUE_TEXT)) != null) {
			diagram.getNodes().remove(node);
		}
	}

	public Vector<String> getSelectedNames() {
		DiagramItemList items = diagram.getSelection().getItems();
		Vector<String> names = new Vector<String>();
		for (DiagramItem item : items) {
			if (item instanceof ShapeNode) {
				names.add(((ShapeNode) item).getTextToEdit());
			}
		}
		return names;
	}

	public void saveAs(String filename) {
		try {
			diagram.saveToXml(filename);
			this.filename = filename;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void save() {
		try {
			diagram.saveToXml(this.filename);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getProperties() {
		// TODO Auto-generated method stub

	}

	public Vector<String> getDisplayedElementNames() {
		Iterable<ShapeNode> items = diagram.items(ShapeNode.class);
		Vector<String> names = new Vector<String>();
		for (ShapeNode item : items) {
			names.add(item.getTextToEdit());
		}
		return names;
	}

	/**
	 * Set values based on the map of values of name and values
	 * 
	 * @param values
	 */
	public void setValues(Hashtable<String, String>[] values) {
		if (!showValueBoxes) {
			return;
		}
		Hashtable<String, Object> visibleNodes = getVisibleNodes();
		Enumeration<String> variables = visibleNodes.keys();
		for (int studyId = 0; studyId < values.length; studyId++) {
			Hashtable<String, String> valuesInStudy = values[studyId];
			for (String name : valuesInStudy.keySet()) {
				Object object = visibleNodes.get(name);
				if (object == null) {
					continue;
				}
				String value = valuesInStudy.get(name);
				value = truncateAfterDecimal(value, 2);
				if (object instanceof ShapeNode) {
					ShapeNode shapeNode = (ShapeNode) object;
					Group subordinateGroup = shapeNode.getSubordinateGroup();
					if (subordinateGroup != null) {
						DiagramNodeList attachedNodes = subordinateGroup
								.getAttachedNodes();
						if (attachedNodes.size() < studyId + 1) {
							createTextNode(studyId, value, shapeNode);
						} else {
							DiagramNode diagramNode = attachedNodes
									.get(studyId);
							diagramNode.setEditedText(value);
						}
					} else {
						createTextNode(studyId, value, shapeNode);
					}
				}
				/*
				 * System.out.println("Setting studyId: " + studyId + " : " +
				 * name + " : " + object + " -> value: " + value);
				 */
			}
		}
	}

	private void createTextNode(int studyId, String value, ShapeNode shapeNode) {
		Float r = shapeNode.getBounds();
		Rectangle2D.Float r2 = null;
		TextFormat tf = null;
		int attachPos = AttachToNode.BottomLeft;
		switch (studyId) {
		case 1:
			attachPos = AttachToNode.BottomRight;
			r2 = new Rectangle2D.Float(r.x + r.width / 2, r.y + r.height / 2,
					r.width / 2, r.height / 2);
			tf = new TextFormat(Align.Far, Align.Far);
			break;
		case 2:
			attachPos = AttachToNode.TopLeft;
			r2 = new Rectangle2D.Float(r.x, r.y, r.width / 2, r.height / 2);
			tf = new TextFormat(Align.Near, Align.Near);
			break;
		case 3:
			attachPos = AttachToNode.TopRight;
			r2 = new Rectangle2D.Float(r.x + r.width / 2, r.y + r.height / 2,
					r.width / 2, r.height / 2);
			tf = new TextFormat(Align.Far, Align.Near);
			break;
		default:
			attachPos = AttachToNode.BottomLeft;
			r2 = new Rectangle2D.Float(r.x, r.y + r.height / 2, r.width / 2,
					r.height / 2);
			tf = new TextFormat(Align.Near, Align.Far);
			break;
		}
		ShapeNode transparentTextNode = diagram.getFactory()
				.createShapeNode(r2);
		transparentTextNode.setId(VALUE_TEXT);
		transparentTextNode.setTransparent(true);
		transparentTextNode.setText(value);
		transparentTextNode.setFont(shapeNode.getFont());
		transparentTextNode.setPen(shapeNode.getPen());
		transparentTextNode.setBrush(shapeNode.getBrush());
		transparentTextNode.setTextFormat(tf);
		transparentTextNode.attachTo(shapeNode, attachPos);
	}

	private String truncateAfterDecimal(String value, int i) {
		if (value == null) {
			return null;
		}
		String[] fields = value.split("\\s");
		if (fields.length < 2) {
			return value;
		}
		return String.format("%." + i + "f %s", Double.parseDouble(fields[0]),
				fields[1]);
	}

	public void setSchematic(MainFrame schematic) {
		this.schematic = schematic;
	}

	public void findInView(String text) {
		if (diagram == null || text == null) {
			return;
		}
		text = text.toLowerCase();
		DiagramNodeList nodes = diagram.getNodes();
		for (DiagramNode n : nodes) {
			String nodeText = n.getTextToEdit();
			if (nodeText != null && nodeText.toLowerCase().contains(text)) {
				diagramView.bringIntoView(n);
				return;
			}
		}
	}
}