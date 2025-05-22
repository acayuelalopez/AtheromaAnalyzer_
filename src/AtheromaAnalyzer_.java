import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;
import java.util.regex.PatternSyntaxException;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.measure.SplineFitter;
import ij.plugin.ChannelSplitter;
import ij.plugin.ImageCalculator;
import ij.plugin.ImagesToStack;
import ij.plugin.PlugIn;
import ij.plugin.RoiEnlarger;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.morphology.Morphology;
import inra.ijpb.morphology.Strel;
import inra.ijpb.morphology.strel.SquareStrel;

public class AtheromaAnalyzer_ implements PlugIn, Measurements {

	String ATHEROMAANALYZER_IMAGES_DEFAULT_PATH;
	Preferences prefImages;
	JLabel imageLabel;
	TextField textImages;
	JTable tableImages, tableTotal, tableIndividual;
	DefaultTableModel modelImages, modelIndividual, modelTotal;
	JScrollPane jScrollPaneImages, jScrollPaneIndividual, jScrollPaneTotal;
	ImagePlus[] imps, channels, arrayOfImages, impGSlices;
	ImageIcon[] icons;
	ImagePlus imp, impTotal, impMontage, impFinal, impMeanMeasure;
	RoiManager rm;
	Roi[] roisR, roisGTotal, roisRTotal;
	int indexMax;
	List<Double> sliceList, xrefRoiList, yrefRoiList, xnearRoiList, ynearRoiList, minDistanceList, totalRefRoisList;
	List<Double> minDistanceListDouble;
	Object[][] dataSlice, dataSliceToUse, dataS, dataFilter;
	JComboBox<String> comboChannel, comboBackground;
	String[] columnHeadersS;
	Object[] chIndividual = new Object[] { "Plaque", "Aorta ID", "Plaque ID", "Aorta Area", "Plaque Mask Area",
			"Area %", "Aorta Intensity", "Plaque Intensity", "Intensity %", "Image" };
	JRadioButton Erosion, Dilation;
	JSpinner filterMorpho;
	JToggleButton buttonSpline;
	List<ImagePlus> imagesToSave;
	double meanColor, meanMain;

	public void run(String arg0) {
		IJ.setTool("rectangle");

		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (Exception e) {
			// If Nimbus is not available, you can set the GUI to another look and feel.
		}
		showGUI();

	}

	public void showGUI() {

		ATHEROMAANALYZER_IMAGES_DEFAULT_PATH = "images_path";
		prefImages = Preferences.userRoot();
		imageLabel = new JLabel();
		imageLabel.setBorder(BorderFactory.createTitledBorder(""));
		imageLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
		JButton buttonRefresh = new JButton("");
		ImageIcon iconRefresh = createImageIcon("images/refresh.png");
		Icon iconRefreshCell = new ImageIcon(iconRefresh.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
		buttonRefresh.setIcon(iconRefreshCell);
		JButton buttonOpenImage = new JButton("");
		ImageIcon iconOpenImage = createImageIcon("images/openimage.png");
		Icon iconOpenImageCell = new ImageIcon(iconOpenImage.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
		buttonOpenImage.setIcon(iconOpenImageCell);
		JButton buttonBrowse = new JButton("");
		ImageIcon iconBrowse = createImageIcon("images/browse.png");
		Icon iconBrowseCell = new ImageIcon(iconBrowse.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
		buttonBrowse.setIcon(iconBrowseCell);
		JButton buttonProcess = new JButton("");
		ImageIcon iconProcess = createImageIcon("images/process.png");
		Icon iconProcessCell = new ImageIcon(iconProcess.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
		buttonProcess.setIcon(iconProcessCell);
		JButton buttonInsert = new JButton("");
		ImageIcon iconInsert = createImageIcon("images/insert.png");
		Icon iconInsertCell = new ImageIcon(iconInsert.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
		buttonInsert.setIcon(iconInsertCell);
		JButton buttonRemove = new JButton("");
		ImageIcon iconRemove = createImageIcon("images/remove.png");
		Icon iconRemoveCell = new ImageIcon(iconRemove.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
		buttonRemove.setIcon(iconRemoveCell);
		JButton saveButton = new JButton("");
		ImageIcon iconSave = createImageIcon("images/save.png");
		Icon iconSaveCell = new ImageIcon(iconSave.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
		saveButton.setIcon(iconSaveCell);
		JToggleButton buttonFH = new JToggleButton("");
		ImageIcon iconFH = createImageIcon("images/freehand.png");
		Icon iconFHCell = new ImageIcon(iconFH.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
		buttonFH.setIcon(iconFHCell);
		buttonSpline = new JToggleButton("");
		ImageIcon iconSpline = createImageIcon("images/spline.png");
		Icon iconSplineCell = new ImageIcon(iconSpline.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
		buttonSpline.setIcon(iconSplineCell);

		JButton csvButton = new JButton("");
		ImageIcon iconCsv = createImageIcon("images/csv.png");
		Icon csvCell = new ImageIcon(iconCsv.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
		csvButton.setIcon(csvCell);
		csvButton.setToolTipText("Click this button to export table data as csv.file.");
		Erosion = new JRadioButton(" Erosion", true);
		Dilation = new JRadioButton(" Dilation");
		JPanel panelED = new JPanel(new FlowLayout(FlowLayout.LEFT));
		ButtonGroup bgroup = new ButtonGroup();
		bgroup.add(Erosion);
		bgroup.add(Dilation);
		panelED.add(Erosion);
		panelED.add(Dilation);
		JPanel filtersMorpho = new JPanel(new FlowLayout(FlowLayout.LEFT));
		filterMorpho = new JSpinner(new SpinnerNumberModel(0, 0, 5000, 1));
		filterMorpho.setPreferredSize(new Dimension(60, 20));
		JSlider sliderMorpho = new JSlider(0, 25, 10);
		sliderMorpho.setValue(0);
		sliderMorpho.setPreferredSize(new Dimension(150, 15));
		JLabel filterMorphoLabel = new JLabel(" +/- :  ");
		filtersMorpho.add(filterMorphoLabel);
		filtersMorpho.add(sliderMorpho);
		filtersMorpho.add(Box.createHorizontalStrut(2));
		filtersMorpho.add(filterMorpho);
		JButton morphoButton = new JButton("");
		ImageIcon iconMorpho = createImageIcon("images/morpho.png");
		Icon morphoOkCell = new ImageIcon(iconMorpho.getImage().getScaledInstance(15, 15, Image.SCALE_SMOOTH));
		morphoButton.setIcon(morphoOkCell);
		morphoButton.setToolTipText("Click this button to erode/dilate rois.");

		textImages = (TextField) new TextField(15);
		textImages.setText(prefImages.get(ATHEROMAANALYZER_IMAGES_DEFAULT_PATH, ""));
		DirectoryListener listenerImages = new DirectoryListener("Browse for movies...  ", textImages,
				JFileChooser.FILES_AND_DIRECTORIES);
		buttonBrowse.addActionListener(listenerImages);
		JPanel panelImagesDirect = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panelImagesDirect.add(textImages);
		panelImagesDirect.add(buttonBrowse);
		JPanel panelPicture = new JPanel();
		panelPicture.setLayout(new BoxLayout(panelPicture, BoxLayout.Y_AXIS));
		JPanel bLabel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		bLabel.add(imageLabel);
		String itemsChannel[] = new String[] { "Red", "Green", "Blue" };
		String itemsBackground[] = new String[] { "Dark", "White" };
		comboChannel = new JComboBox<>(itemsChannel);
		comboChannel.setSelectedIndex(0);
		comboBackground = new JComboBox<>(itemsBackground);
		comboBackground.setSelectedIndex(0);
		JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		comboPanel.add(comboBackground);
		comboPanel.add(comboChannel);
		comboPanel.add(buttonProcess);
		JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
		JSeparator separator1 = new JSeparator(SwingConstants.VERTICAL);
		Dimension dime = separator.getPreferredSize();
		dime.height = buttonBrowse.getPreferredSize().height;
		separator.setPreferredSize(dime);
		separator1.setPreferredSize(dime);
		bLabel.add(separator);
		bLabel.add(panelImagesDirect);
		bLabel.add(buttonRefresh);
		bLabel.add(buttonOpenImage);
		bLabel.add(Box.createHorizontalStrut(5));
		bLabel.add(separator1);
		bLabel.add(comboPanel);
		// bLabel.add(buttonFH);
		imagesToSave = new ArrayList<ImagePlus>();
		tableImages = new JTable();
		modelImages = new DefaultTableModel();
		tableImages.setModel(modelImages);
		jScrollPaneImages = new JScrollPane(tableImages);
		jScrollPaneImages.setPreferredSize(new Dimension(650, 200));
		tableTotal = new JTable();
		modelTotal = new DefaultTableModel();
		modelTotal.setColumnIdentifiers(chIndividual);
		tableTotal.setModel(modelTotal);
		jScrollPaneTotal = new JScrollPane(tableTotal);
		jScrollPaneTotal.setPreferredSize(new Dimension(650, 200));
		tableIndividual = new JTable();
		modelIndividual = new DefaultTableModel();
		modelIndividual.setColumnIdentifiers(chIndividual);
		tableIndividual.setModel(modelIndividual);
		modelTotal = new DefaultTableModel(chIndividual, 0) {

			@Override
			public Class<?> getColumnClass(int column) {
				if (getRowCount() != 0) {
					Object value = getValueAt(0, column);
					if (value != null) {
						return getValueAt(0, column).getClass();
					}
				}
				return super.getColumnClass(column);
			}

			public boolean isCellEditable(int row, int col) {
				return false;
			}

		};

		tableTotal.setModel(modelTotal);
		tableTotal.setSelectionBackground(new Color(229, 255, 204));
		tableTotal.setSelectionForeground(new Color(0, 102, 0));
		tableTotal.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableTotal.setRowHeight(60);
		tableTotal.setAutoCreateRowSorter(true);
		tableTotal.getTableHeader().setDefaultRenderer(new SimpleHeaderRenderer());
		for (int u = 0; u < tableTotal.getColumnCount(); u++)
			tableTotal.getColumnModel().getColumn(u).setPreferredWidth(150);
		JScrollPane jScrollPaneIndividual = new JScrollPane(tableIndividual);
		jScrollPaneIndividual.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jScrollPaneIndividual.setPreferredSize(new Dimension(650, 100));
		modelIndividual = new DefaultTableModel(chIndividual, 1) {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int column) {
				if (getRowCount() > 0) {
					Object value = getValueAt(0, column);
					if (value != null) {
						return getValueAt(0, column).getClass();
					}
				}

				return super.getColumnClass(column);
			}

			public boolean isCellEditable(int row, int col) {
				return false;
			}

		};
		tableIndividual.setModel(modelIndividual);
		tableIndividual.setSelectionBackground(new Color(229, 255, 204));
		tableIndividual.setSelectionForeground(new Color(0, 102, 0));
		// tableIndividual.setDefaultRenderer(ImageIcon.class, centerRenderer);
		tableIndividual.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableIndividual.setRowHeight(60);
		tableIndividual.setAutoCreateRowSorter(true);
		tableIndividual.getTableHeader().setDefaultRenderer(new SimpleHeaderRenderer());
		for (int u = 0; u < tableIndividual.getColumnCount(); u++)
			tableIndividual.getColumnModel().getColumn(u).setPreferredWidth(150);
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		imagePanel.add(jScrollPaneImages);
		JPanel slicePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		slicePanel.add(jScrollPaneIndividual);
		JPanel sPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		sPanel.add(jScrollPaneTotal);
		JPanel insertPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JPanel modifyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		modifyPanel.add(buttonFH);
		modifyPanel.add(buttonSpline);
		modifyPanel.add(panelED);
		modifyPanel.add(filtersMorpho);
		modifyPanel.add(morphoButton);
		JSeparator separator3 = new JSeparator(SwingConstants.VERTICAL);
		Dimension dime3 = separator3.getPreferredSize();
		dime3.height = buttonBrowse.getPreferredSize().height;
		separator3.setPreferredSize(dime3);
		insertPanel.add(buttonInsert);
		insertPanel.add(buttonRemove);
		insertPanel.add(separator3);
		insertPanel.add(csvButton);
		insertPanel.add(saveButton);
		panelPicture.add(imagePanel);
		panelPicture.add(Box.createVerticalStrut(3));
		panelPicture.add(bLabel);
		panelPicture.add(Box.createVerticalStrut(3));
		panelPicture.add(slicePanel);
		panelPicture.add(Box.createVerticalStrut(3));
		panelPicture.add(modifyPanel);
		panelPicture.add(Box.createVerticalStrut(3));
		panelPicture.add(insertPanel);
		panelPicture.add(Box.createVerticalStrut(3));
		panelPicture.add(sPanel);
		mainPanel.add(imagePanel);
		mainPanel.add(panelPicture);

		JFrame frame = new JFrame();
		frame.setTitle("Atheroma Analyzer");
		frame.setResizable(false);
		frame.add(mainPanel);
		frame.pack();
		frame.setSize(660, 800);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);

		sliderMorpho.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				filterMorpho.setValue(sliderMorpho.getValue());
			}
		});

		filterMorpho.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				sliderMorpho.setValue((int) filterMorpho.getValue());
			}
		});
		buttonFH.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent itemEvent) {
				int status = itemEvent.getStateChange();
				if (status == ItemEvent.SELECTED)
					freeHandAction();

				if (status == ItemEvent.DESELECTED)
					updateFreeHandAction();
			}
		});
		buttonSpline.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent itemEvent) {
				int status = itemEvent.getStateChange();
				if (rm.getRoi(0).getType() == Roi.FREEROI) {
					if (status == ItemEvent.SELECTED)
						splineAction();

					if (status == ItemEvent.DESELECTED)
						updateSplineAction();
				} else {
					IJ.error(rm.getRoi(0).getName() + " is " + rm.getRoi(0).getTypeAsString()
							+ "and should be FreeHand type");
					return;
				}

			}
		});
		csvButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				csvExport();

			}
		});
		saveButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				saveImages();

			}
		});
		morphoButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				if (Dilation.isSelected() == true)
					morphoDilationAction();

				if (Erosion.isSelected() == true)
					morphoErosionAction();

			}
		});
		buttonInsert.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addRowAction();
			}
		});
		buttonRemove.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				removeRowAction();
			}
		});
		buttonRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshAction();
			}
		});

		buttonProcess.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				processTool();
			}
		});

		buttonOpenImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				List<ImagePlus> impAnalClose = new ArrayList<ImagePlus>();
				int[] IDs = WindowManager.getIDList();
				if (IDs != null)
					for (int i = 0; i < IDs.length; i++)
						impAnalClose.add(WindowManager.getImage(IDs[i]));

				if (tableImages.getSelectedRow() != -1) {
					if (IDs != null)
						for (int i = 0; i < IDs.length; i++)
							impAnalClose.get(i).hide();
					imp = imps[tableImages.getSelectedRow()];
				}
				if (imp == null)
					IJ.error("Please, select an image within the main directory.");
				if (imp != null) {
					imp.show();
					String title = imp.getShortTitle();
					imageLabel.setText(title);

				}

			}
		});

	}

	public static Image getScaledImage(Image srcImg, int w, int h) {
		BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = resizedImg.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(srcImg, 0, 0, w, h, null);
		g2.dispose();
		return resizedImg;
	}

	public void morphoDilationAction() {

		IJ.selectWindow(imageLabel.getText());
		impMontage.setRoi(rm.getRoi(0));
		IJ.run(impMontage, "Enlarge...", String.format("enlarge=%d", (int) filterMorpho.getValue()));
		Roi roiNew = impMontage.getRoi();
		rm.setRoi(roiNew, 0);
		modelIndividual.setValueAt(rm.getRoi(0).getStatistics().area, tableIndividual.convertRowIndexToModel(0),
				tableIndividual.convertColumnIndexToModel(3));
		modelIndividual.setValueAt(
				Math.round(((rm.getRoi(1).getStatistics().area * 100) / rm.getRoi(0).getStatistics().area) * 1000.0)
						/ 1000.0,
				tableIndividual.convertRowIndexToModel(0), tableIndividual.convertColumnIndexToModel(5));
		impMeanMeasure.setRoi(rm.getRoi(0));
		meanMain = rm.getRoi(0).getStatistics().mean;
		modelIndividual.setValueAt(Math.round(meanMain * 1000.0) / 1000.0, tableIndividual.convertRowIndexToModel(0),
				tableIndividual.convertColumnIndexToModel(6));
		modelIndividual.setValueAt(Math.round(((meanColor * 100) / meanMain) * 1000.0) / 1000.0,
				tableIndividual.convertRowIndexToModel(0), tableIndividual.convertColumnIndexToModel(8));

	}

	public void morphoErosionAction() {

		IJ.selectWindow(imageLabel.getText());
		impMontage.setRoi(rm.getRoi(0));
		IJ.run(impMontage, "Enlarge...", String.format("enlarge=%d", (~((int) filterMorpho.getValue() - 1))));
		Roi roiNew = impMontage.getRoi();
		rm.setRoi(roiNew, 0);
		modelIndividual.setValueAt(rm.getRoi(0).getStatistics().area, tableIndividual.convertRowIndexToModel(0),
				tableIndividual.convertColumnIndexToModel(3));
		modelIndividual.setValueAt(
				Math.round(((rm.getRoi(1).getStatistics().area * 100) / rm.getRoi(0).getStatistics().area) * 1000.0)
						/ 1000.0,
				tableIndividual.convertRowIndexToModel(0), tableIndividual.convertColumnIndexToModel(5));
		impMeanMeasure.setRoi(rm.getRoi(0));
		meanMain = rm.getRoi(0).getStatistics().mean;
		modelIndividual.setValueAt(Math.round(meanMain * 1000.0) / 1000.0, tableIndividual.convertRowIndexToModel(0),
				tableIndividual.convertColumnIndexToModel(6));
		modelIndividual.setValueAt(Math.round(((meanColor * 100) / meanMain) * 1000.0) / 1000.0,
				tableIndividual.convertRowIndexToModel(0), tableIndividual.convertColumnIndexToModel(8));

	}

	public void addRowAction() {

		Object[] rowData = new Object[10];

		for (int i = 0; i < modelIndividual.getColumnCount(); i++)
			rowData[i] = tableIndividual.getModel().getValueAt(tableIndividual.convertRowIndexToModel(0), i);

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);

		if (modelTotal.getRowCount() == 0) {
			modelTotal.addRow(rowData);
		} else {
			if (rowData[1].toString()
					.equals(modelTotal.getValueAt(tableTotal.convertRowIndexToModel(modelTotal.getRowCount() - 1),
							tableTotal.convertColumnIndexToModel(1))) == true
					&& (double) rowData[3] == (double) modelTotal.getValueAt(
							tableTotal.convertRowIndexToModel(modelTotal.getRowCount() - 1),
							tableTotal.convertColumnIndexToModel(3)))
				return;
			if (rowData[1].toString()
					.equals(modelTotal.getValueAt(tableTotal.convertRowIndexToModel(modelTotal.getRowCount() - 1),
							tableTotal.convertColumnIndexToModel(1))) == true
					&& (double) rowData[3] != (double) modelTotal.getValueAt(
							tableTotal.convertRowIndexToModel(modelTotal.getRowCount() - 1),
							tableTotal.convertColumnIndexToModel(3)))
				modelTotal.addRow(rowData);

			if (rowData[1].toString()
					.equals(modelTotal.getValueAt(tableTotal.convertRowIndexToModel(modelTotal.getRowCount() - 1),
							tableTotal.convertColumnIndexToModel(1))) == false)
				modelTotal.addRow(rowData);
		}

		tableTotal.setDefaultRenderer(String.class, centerRenderer);
		tableTotal.setDefaultRenderer(Double.class, centerRenderer);

	}

	public void removeRowAction() {

		int selectedIndex = tableTotal.getSelectedRow();
		if (selectedIndex == -1) {
			IJ.error("You should select a row to be removed");
			return;
		}
		if (selectedIndex != -1) {
			modelTotal.removeRow(tableTotal.getSelectedRow());
			modelTotal.fireTableDataChanged();
		}

	}

	public void freeHandAction() {
		IJ.selectWindow(imageLabel.getText());
		IJ.setTool("freehand");

	}

	public void splineAction() {

		IJ.selectWindow(imageLabel.getText());
		impMontage.setRoi(rm.getRoi(0));
		// IJ.run(impMontage, "Enlarge...", "enlarge=1");
		IJ.run(impMontage, "Fit Spline", "");

	}

	public void updateFreeHandAction() {

		Roi roiNew = impMontage.getRoi();
		rm.setRoi(roiNew, 0);
		modelIndividual.setValueAt(rm.getRoi(0).getStatistics().area, tableIndividual.convertRowIndexToModel(0),
				tableIndividual.convertColumnIndexToModel(3));
		modelIndividual.setValueAt(
				Math.round(((rm.getRoi(1).getStatistics().area * 100) / rm.getRoi(0).getStatistics().area) * 1000.0)
						/ 1000.0,
				tableIndividual.convertRowIndexToModel(0), tableIndividual.convertColumnIndexToModel(5));
		impMeanMeasure.setRoi(rm.getRoi(0));
		meanMain = rm.getRoi(0).getStatistics().mean;
		modelIndividual.setValueAt(Math.round(meanMain * 1000.0) / 1000.0, tableIndividual.convertRowIndexToModel(0),
				tableIndividual.convertColumnIndexToModel(6));
		modelIndividual.setValueAt(Math.round(((meanColor * 100) / meanMain) * 1000.0) / 1000.0,
				tableIndividual.convertRowIndexToModel(0), tableIndividual.convertColumnIndexToModel(8));

	}

	public void updateSplineAction() {

		Roi roiNew = impMontage.getRoi();
		rm.setRoi(roiNew, 0);
		modelIndividual.setValueAt(rm.getRoi(0).getStatistics().area, tableIndividual.convertRowIndexToModel(0),
				tableIndividual.convertColumnIndexToModel(3));
		modelIndividual.setValueAt(
				Math.round(((rm.getRoi(1).getStatistics().area * 100) / rm.getRoi(0).getStatistics().area) * 1000.0)
						/ 1000.0,
				tableIndividual.convertRowIndexToModel(0), tableIndividual.convertColumnIndexToModel(5));
		impMeanMeasure.setRoi(rm.getRoi(0));
		meanMain = rm.getRoi(0).getStatistics().mean;
		modelIndividual.setValueAt(Math.round(meanMain * 1000.0) / 1000.0, tableIndividual.convertRowIndexToModel(0),
				tableIndividual.convertColumnIndexToModel(6));
		modelIndividual.setValueAt(Math.round(((meanColor * 100) / meanMain) * 1000.0) / 1000.0,
				tableIndividual.convertRowIndexToModel(0), tableIndividual.convertColumnIndexToModel(8));

	}

	public void csvExport() {
		JFrame parentFrame = new JFrame();

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setDialogTitle("Select a Directory to save data");
		int userSelection = fileChooser.showSaveDialog(parentFrame);
		File fileToSave = null;
		if (userSelection == JFileChooser.APPROVE_OPTION)
			fileToSave = fileChooser.getSelectedFile();

		try {

			FileWriter csv = new FileWriter(new File(fileToSave.getAbsolutePath() + File.separator
					+ textImages.getText().substring(textImages.getText().lastIndexOf(File.separator))
					+ "Table_data.csv"));

			for (int i = 0; i < modelTotal.getColumnCount(); i++) {
				csv.write(modelTotal.getColumnName(i) + ",");
			}

			csv.write("\n");

			for (int i = 0; i < tableTotal.getRowCount(); i++) {
				for (int j = 0; j < tableTotal.getColumnCount(); j++) {

					csv.write(
							tableTotal.getModel().getValueAt(tableTotal.convertRowIndexToModel(i), j).toString() + ",");
				}
				csv.write("\n");
			}

			csv.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void saveImages() {
		JFrame parentFrame = new JFrame();
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setDialogTitle("Select a Directory to save data");
		int userSelection = fileChooser.showSaveDialog(parentFrame);
		File fileToSave = null;
		if (userSelection == JFileChooser.APPROVE_OPTION) {
			fileToSave = fileChooser.getSelectedFile();

			ImagePlus[] impsToSave = null;
			for (ImagePlus image : imagesToSave) {
				File directImages = new File(fileToSave.getAbsolutePath() + File.separator + image.getShortTitle());
				if (!directImages.exists()) {
					boolean results = false;

					try {
						directImages.mkdir();
						results = true;
					} catch (SecurityException se) {
						// handle it
					}
				}
				impsToSave = stack2images(image);
				for (int i = 0; i < impsToSave.length; i++)
					IJ.saveAs(impsToSave[i], "Tiff", directImages.getAbsolutePath() + File.separator
							+ image.getShortTitle() + "-" + (i + 1) + ".tif");
			}
		}
	}

	public void refreshAction() {

		prefImages.put(ATHEROMAANALYZER_IMAGES_DEFAULT_PATH, textImages.getText());
		File imageFolder = new File(textImages.getText());
		File[] listOfFiles = imageFolder.listFiles();
		String[] imageTitles = new String[listOfFiles.length];
		imps = new ImagePlus[imageTitles.length];
		icons = new ImageIcon[imps.length];
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile())
				imageTitles[i] = listOfFiles[i].getName();
			imps[i] = IJ.openImage(textImages.getText() + File.separator + imageTitles[i]);
			icons[i] = new ImageIcon(getScaledImage(imps[i].getImage(), 90, 60));

		}
		Object[] columnNames = new Object[] { "Image", "Title", "Extension" };
		Object[][] dataTImages = new Object[imps.length][columnNames.length];
		for (int i = 0; i < dataTImages.length; i++)
			for (int j = 0; j < dataTImages[i].length; j++)
				dataTImages[i][j] = "";
		modelImages = new DefaultTableModel(dataTImages, columnNames) {

			@Override
			public Class<?> getColumnClass(int column) {
				if (getRowCount() > 0) {
					Object value = getValueAt(0, column);
					if (value != null) {
						return getValueAt(0, column).getClass();
					}
				}

				return super.getColumnClass(column);
			}

			public boolean isCellEditable(int row, int col) {
				return false;
			}

		};
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		tableImages.setModel(modelImages);
		tableImages.setSelectionBackground(new Color(229, 255, 204));
		tableImages.setSelectionForeground(new Color(0, 102, 0));
		tableImages.setDefaultRenderer(String.class, centerRenderer);
		tableImages.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableImages.setRowHeight(60);
		tableImages.setAutoCreateRowSorter(true);
		tableImages.getTableHeader().setDefaultRenderer(new SimpleHeaderRenderer());

		for (int i = 0; i < modelImages.getRowCount(); i++) {
			modelImages.setValueAt(icons[i], i, tableImages.convertColumnIndexToModel(0));
			modelImages.setValueAt(imps[i].getShortTitle(), i, tableImages.convertColumnIndexToModel(1));
			modelImages.setValueAt(imps[i].getTitle().substring(imps[i].getTitle().lastIndexOf(".")), i,
					tableImages.convertColumnIndexToModel(2));
		}
		tableImages.getColumnModel().getColumn(0).setPreferredWidth(100);
		tableImages.getColumnModel().getColumn(1).setPreferredWidth(450);
		tableImages.getColumnModel().getColumn(2).setPreferredWidth(100);
		Object[][] dataTotal = new Object[1][chIndividual.length];
		for (int i = 0; i < dataTotal.length; i++)
			for (int j = 0; j < dataTotal[i].length; j++)
				dataTotal[i][j] = "";

	}

	public void processTool() {
		if (rm != null)
			rm.reset();
		Roi roiToCrop = imp.getRoi();
		if (roiToCrop == null) {
			IJ.error("Please, select a region of interest");
			return;
		}
		if (roiToCrop != null) {
			ImageProcessor ip = imp.getProcessor();
			ip.setRoi(roiToCrop);
			ImageProcessor ipCrop = ip.crop();
			impFinal = new ImagePlus(imp.getTitle() + "-cropped", ipCrop);
			imp.hide();
			ImageCalculator ic = new ImageCalculator();
			ImagePlus[] channels = ChannelSplitter.split(impFinal);
			// String title = IJ.getImage().getShortTitle();
			ImagePlus impAverage = null;
			ImagePlus impColor = null;

			if (comboBackground.getSelectedIndex() == 0) {
				if (comboChannel.getSelectedIndex() == 0) {
					impAverage = ic.run("Average create", channels[1], channels[2]);
					impColor = ic.run("Subtract create", channels[0], impAverage);

				}
				if (comboChannel.getSelectedIndex() == 1) {
					impAverage = ic.run("Average create", channels[0], channels[2]);
					impColor = ic.run("Subtract create", channels[1], impAverage);
				}
				if (comboChannel.getSelectedIndex() == 2) {
					impAverage = ic.run("Average create", channels[0], channels[1]);
					impColor = ic.run("Subtract create", channels[2], impAverage);
				}
				impColor.setTitle(imp.getShortTitle() + "-" + "ColorSelection");
				// IJ.run(impColor, "Auto Threshold", "method=Otsu ignore_black white");
				IJ.run(impColor, "Auto Threshold", "method=MaxEntropy ignore_black white");
				IJ.run(impColor, "Create Selection", "");

			}
			if (comboBackground.getSelectedIndex() == 1) {

				if (comboChannel.getSelectedIndex() == 0) {
					impAverage = ic.run("Average create", channels[1], channels[2]);
					impColor = ic.run("Subtract create", channels[0], impAverage);
				}
				if (comboChannel.getSelectedIndex() == 1) {
					impAverage = ic.run("Average create", channels[0], channels[2]);
					impColor = ic.run("Subtract create", channels[1], impAverage);
				}
				if (comboChannel.getSelectedIndex() == 2) {
					impAverage = ic.run("Average create", channels[0], channels[1]);
					impColor = ic.run("Subtract create", channels[2], impAverage);
				}

				impColor.setTitle(imp.getShortTitle() + "-" + "ColorSelection");
				IJ.run(impColor, "Auto Threshold", "method=Otsu ignore_black white");
				IJ.run(impColor, "Create Selection", "");

			}
			rm = RoiManager.getInstance();

			if (null == rm)
				rm = new RoiManager();
			Roi roiColor = impColor.getRoi();
			RoiEnlarger.enlarge(roiColor, 2.0);
			ShapeRoi roiColorShape = new ShapeRoi(roiColor);
			Roi[] roisColor = roiColorShape.getRois();

			ByteProcessor maskColor = impColor.createThresholdMask();
			ImageProcessor maskColorProcessor = maskColor.duplicate();
			ImagePlus maskColorImp = new ImagePlus(imp.getShortTitle() + "-" + "MaskColor", maskColorProcessor);
			double subtractPixels = roiColor.getStatistics().area;

			if (comboBackground.getSelectedIndex() == 1) {

				if (comboChannel.getSelectedIndex() == 0)
					impTotal = ic.run("Subtract create", channels[0], impAverage);

				if (comboChannel.getSelectedIndex() == 1)
					impTotal = ic.run("Subtract create", channels[1], impAverage);

				if (comboChannel.getSelectedIndex() == 2)
					impTotal = ic.run("Subtract create", channels[2], impAverage);

				impTotal.setTitle(imp.getShortTitle() + "-" + "TotalSelection");
				IJ.run(impTotal, "Invert LUT", "");
				IJ.run(impTotal, "Auto Threshold", "method=Huang ignore_white");

			}
			if (comboBackground.getSelectedIndex() == 0) {

				if (comboChannel.getSelectedIndex() == 0)
					impTotal = channels[0].duplicate();

				if (comboChannel.getSelectedIndex() == 1)
					impTotal = channels[1].duplicate();

				if (comboChannel.getSelectedIndex() == 2)
					impTotal = channels[2].duplicate();

				impTotal.setTitle(imp.getShortTitle() + "-" + "TotalSelection");
				// IJ.run(impTotal, "Morphological Filters",
				// String.format("operation=Opening element=Disk radius=%d", (int)
				// Double.parseDouble("2")));

				// impTotal = WindowManager.getImage(imp.getShortTitle() + "-" +
				// "TotalSelection-Opening");

				impTotal = new ImagePlus(imp.getShortTitle() + "-" + "TotalSelection-Opening",
						Morphology.opening(impTotal.getProcessor(), SquareStrel.fromDiameter(2)));
				IJ.run(impTotal, "Invert LUT", "");
				IJ.run(impTotal, "Auto Threshold", "method=Default ignore_white");

			}
			ImagePlus impTotalMain = impTotal.duplicate();
			IJ.run(impTotalMain, "Fill Holes", "");
			IJ.run(impTotalMain, "Create Selection", "");
			IJ.run(impTotal, "Create Selection", "");
			Roi roiTotalMain = impTotalMain.getRoi();
			RoiEnlarger.enlarge(roiTotalMain, 3.0);
			Roi roiTotalMinor = impTotal.getRoi();
			ShapeRoi roiTotalShapeMain = new ShapeRoi(roiTotalMain);
			ShapeRoi roiTotalShapeMinor = new ShapeRoi(roiTotalMinor);
			Roi[] roisTotalMain = roiTotalShapeMain.getRois();
			Roi[] roisTotalMinor = roiTotalShapeMinor.getRois();
			double[] roisTotalArea = new double[roisTotalMain.length];
			for (int j = 0; j < roisTotalMain.length; j++)
				roisTotalArea[j] = roisTotalMain[j].getStatistics().area;
			int maxIndex = getIndexOfLargest(roisTotalArea);
			int maxIndex2 = getIndex2OfLargest(roisTotalArea);
			Roi roiTotalMax = null;
			if (comboBackground.getSelectedIndex() == 0) {
				if (roisTotalMain[maxIndex2].getStatistics().area >= (0.4
						* roisTotalMain[maxIndex].getStatistics().area) == true)
					roiTotalMax = new ShapeRoi(roisTotalMain[maxIndex]).and(new ShapeRoi(roisTotalMain[maxIndex2]))
							.shapeToRoi();
				if (roisTotalMain[maxIndex2].getStatistics().area >= (0.4
						* roisTotalMain[maxIndex].getStatistics().area) == false)
					roiTotalMax = roisTotalMain[maxIndex];
			}
			if (comboBackground.getSelectedIndex() == 1)
				roiTotalMax = roisTotalMain[maxIndex];
			RoiEnlarger.enlarge(roiTotalMax, 4.0);
			Roi inverseRoi = roiTotalMain.getInverse(channels[0]);
			channels[0].setRoi(inverseRoi);
			double meanInverse = inverseRoi.getStatistics().mean;

			rm = RoiManager.getInstance();

			if (null == rm)
				rm = new RoiManager();

			List<Roi> roiTotalSmall = new ArrayList<Roi>();
			ShapeRoi xorRoi = null;
			Roi finalRoiToAddTotal = null;

			ShapeRoi shapeRoiMain = new ShapeRoi(roiTotalMax);
			// int indexS = 0;

			for (int j = 0; j < roisTotalMinor.length; j++) {

				if (shapeRoiMain.contains(roisTotalMinor[j].getBounds().x,
						roisTotalMinor[j].getBounds().y) == Boolean.TRUE) {
					channels[0].setRoi(roisTotalMinor[j]);

					if (roisTotalMinor[j].getStatistics().min <= meanInverse
							&& roisTotalMinor[j].getStatistics().area >= 0.02
									* roisTotalMain[maxIndex].getStatistics().area)
						roiTotalSmall.add(roisTotalMinor[j]);
				}
			}
			if (roiTotalSmall.size() >= 1) {

				for (int x = 0; x < roiTotalSmall.size(); x++) {
					xorRoi = shapeRoiMain.xor(new ShapeRoi(roiTotalSmall.get(x)));
				}
				finalRoiToAddTotal = xorRoi.shapeToRoi();
				rm.addRoi(finalRoiToAddTotal);

			}
			if (roiTotalSmall.size() < 1) {
				finalRoiToAddTotal = roiTotalMax;
				rm.addRoi(finalRoiToAddTotal);
			}

			ShapeRoi roiColorSR = new ShapeRoi(roiColor);
			ShapeRoi roiTotalSR = new ShapeRoi(roiTotalMax);
			ShapeRoi roiColorDefSR = roiColorSR.and(roiTotalSR);
			Roi roiColorDef = roiColorDefSR.shapeToRoi();
			rm.addRoi(roiColorDef);
			impMeanMeasure = impFinal.duplicate();
			impMeanMeasure.setRoi(roiColorDef);
			meanColor = roiColorDef.getStatistics().mean;
			ShapeRoi finalRoiToAddTotalxOrSR = new ShapeRoi(finalRoiToAddTotal);
			Roi finalRoiToAddTotalVer = finalRoiToAddTotalxOrSR.xor(new ShapeRoi(roiColorDef)).shapeToRoi();
			impMeanMeasure.setRoi(finalRoiToAddTotalVer);
			meanMain = finalRoiToAddTotalVer.getStatistics().mean;
			impMeanMeasure.setRoi(finalRoiToAddTotalVer);
			ByteProcessor maskTotal = impTotal.createThresholdMask();
			ImageProcessor maskTotalProcessor = maskTotal.duplicate();
			ImagePlus maskTotalImp = new ImagePlus(imp.getTitle() + "-" + "MaskTotal", maskTotalProcessor);
			// double addPixels = roiTotal.getStatistics().area;

			impTotal.hide();
			impColor.hide();

			List<ImagePlus> listOfImages = new ArrayList<ImagePlus>();

			listOfImages.add(impFinal);
			listOfImages.add(maskColorImp);
			listOfImages.add(maskTotalImp);

			ImagePlus[] arrayOfImages = new ImagePlus[listOfImages.size()];
			listOfImages.toArray(arrayOfImages);
			impMontage = ImagesToStack.run(arrayOfImages);
			impMontage.setTitle(imp.getShortTitle());
			impMontage.show();

			imagesToSave.add(impMontage);

			DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
			centerRenderer.setHorizontalAlignment(JLabel.CENTER);

			dataS = new Object[1][10];
			// Collections.sort(minDistanceListDouble);

			ImageIcon icon = new ImageIcon(
					getScaledImage(new ImagePlus("Mask-", rm.getRoi(1).getMask()).getImage(), 75, 65));
			// tableIndividual.setValueAt(icon, 0, 0);
			for (int i = 0; i < dataS.length; i++)
				for (int j = 0; j < dataS[i].length; j++)
					dataS[i][j] = "";

			modelIndividual.setDataVector(dataS, chIndividual);
			tableIndividual.setDefaultRenderer(String.class, centerRenderer);
			tableIndividual.setDefaultRenderer(Double.class, centerRenderer);
			tableIndividual.setRowHeight(60);
			tableIndividual.setAutoCreateRowSorter(true);
			tableIndividual.getTableHeader().setDefaultRenderer(new SimpleHeaderRenderer());
			for (int u = 0; u < tableIndividual.getColumnCount(); u++)
				tableIndividual.getColumnModel().getColumn(u).setPreferredWidth(150);
			modelIndividual.setValueAt(icon, tableIndividual.convertRowIndexToModel(0),
					tableIndividual.convertColumnIndexToModel(0));
			modelIndividual.setValueAt(rm.getRoi(0).getName(), tableIndividual.convertRowIndexToModel(0),
					tableIndividual.convertColumnIndexToModel(1));
			modelIndividual.setValueAt(rm.getRoi(1).getName(), tableIndividual.convertRowIndexToModel(0),
					tableIndividual.convertColumnIndexToModel(2));
			modelIndividual.setValueAt(rm.getRoi(0).getStatistics().area, tableIndividual.convertRowIndexToModel(0),
					tableIndividual.convertColumnIndexToModel(3));
			modelIndividual.setValueAt(rm.getRoi(1).getStatistics().area, tableIndividual.convertRowIndexToModel(0),
					tableIndividual.convertColumnIndexToModel(4));
			modelIndividual.setValueAt(
					Math.round(((Double) (rm.getRoi(1).getStatistics().area * 100.0)
							/ (Double) rm.getRoi(0).getStatistics().area) * 1000.0) / 1000.0,
					tableIndividual.convertRowIndexToModel(0), tableIndividual.convertColumnIndexToModel(5));
			modelIndividual.setValueAt(Math.round(meanMain * 1000.0) / 1000.0,
					tableIndividual.convertRowIndexToModel(0), tableIndividual.convertColumnIndexToModel(6));
			modelIndividual.setValueAt(Math.round(meanColor * 1000.0) / 1000.0,
					tableIndividual.convertRowIndexToModel(0), tableIndividual.convertColumnIndexToModel(7));
			modelIndividual.setValueAt(Math.round(((Double) (meanColor * 100.0) / (Double) meanMain) * 1000.0) / 1000.0,
					tableIndividual.convertRowIndexToModel(0), tableIndividual.convertColumnIndexToModel(8));
			modelIndividual.setValueAt(imageLabel.getText(), tableIndividual.convertRowIndexToModel(0),
					tableIndividual.convertColumnIndexToModel(9));

		}
	}

	public static ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = AtheromaAnalyzer_.class.getResource(path);
		if (imgURL != null) {
			return new ImageIcon(imgURL);
		} else {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
	}

	public ImagePlus[] stack2images(ImagePlus imp) {
		String sLabel = imp.getTitle();
		String sImLabel = "";
		ImageStack stack = imp.getStack();

		int sz = stack.getSize();
		int currentSlice = imp.getCurrentSlice(); // to reset ***

		DecimalFormat df = new DecimalFormat("0000"); // for title
		ImagePlus[] arrayOfImages = new ImagePlus[imp.getStack().getSize()];
		for (int n = 1; n <= sz; ++n) {
			imp.setSlice(n); // activate next slice ***

			// Get current image processor from stack. What ever is
			// used here should do a COPY pixels from old processor to
			// new. For instance, ImageProcessor.crop() returns copy.
			ImageProcessor ip = imp.getProcessor(); // ***
			ImageProcessor newip = ip.createProcessor(ip.getWidth(), ip.getHeight());
			newip.setPixels(ip.getPixelsCopy());

			// Create a suitable label, using the slice label if possible
			sImLabel = imp.getStack().getSliceLabel(n);
			if (sImLabel == null || sImLabel.length() < 1) {
				sImLabel = "slice" + df.format(n) + "_" + sLabel;
			}
			// Create new image corresponding to this slice.
			ImagePlus im = new ImagePlus(sImLabel, newip);
			im.setCalibration(imp.getCalibration());
			arrayOfImages[n - 1] = im;

			// Show this image.
			// imp.show();
		}
		// Reset original stack state.
		imp.setSlice(currentSlice); // ***
		if (imp.isProcessor()) {
			ImageProcessor ip = imp.getProcessor();
			ip.setPixels(ip.getPixels()); // ***
		}
		imp.setSlice(currentSlice);
		return arrayOfImages;
	}

	public static Double findMin(List<Double> list) {

		// check list is empty or not
		if (list == null || list.size() == 0) {
			return Double.MAX_VALUE;
		}

		// create a new list to avoid modification
		// in the original list
		List<Double> sortedlist = new ArrayList<>(list);

		// sort list in natural order
		Collections.sort(sortedlist);

		// first element in the sorted list
		// would be minimum
		return sortedlist.get(0);
	}

	public static double distance(double x1, double y1, double x2, double y2) {
		double x = Math.pow(x2 - x1, 2);
		double y = Math.pow(y2 - y1, 2);
		return Math.sqrt(x + y);
	}

	// public static double[] nearestPoint(double[] coordinate, double[][] points) {
	public static int nearestPoint(double[] coordinate, double[][] points) {
		final int X = 0;
		final int Y = 1;
		int indexFound = 0;
		double[] closestPoint = points[0];
		double closestDist = distance(coordinate[X], coordinate[Y], closestPoint[X], closestPoint[Y]);

		// Traverse the array
		for (int i = 0; i < points.length; i++) {
			double dist = distance(coordinate[X], coordinate[Y], points[i][X], points[i][Y]);
			if (dist < closestDist && dist != 0.0) {
				closestDist = dist;
				closestPoint = points[i];
				indexFound = i;
			}
		}

		// return closestPoint;
		return indexFound;
	}

	public double sum(List<Double> a) {
		if (a.size() > 0) {
			int sum = 0;

			for (Double i : a) {
				sum += i;
			}
			return sum;
		}
		return 0;
	}

	public double mean(List<Double> a) {
		double sum = sum(a);
		double mean = 0;
		mean = sum / (a.size() * 1.0);
		return mean;
	}

	public double median(List<Double> a) {
		int middle = a.size() / 2;

		if (a.size() % 2 == 1) {
			return a.get(middle);
		} else {
			return (a.get(middle - 1) + a.get(middle)) / 2.0;
		}
	}

	public double sd(List<Double> a) {
		int sum = 0;
		double mean = mean(a);

		for (Double i : a)
			sum += Math.pow((i - mean), 2);
		return Math.sqrt(sum / (a.size() - 1)); // sample
	}

	public int getIndexOfLargest(double[] array) {

		if (array == null || array.length == 0)
			return -1; // null or empty

		int largest = 0;
		for (int i = 1; i < array.length; i++) {
			if (array[i] > array[largest])
				largest = i;
		}
		return largest; // position of the first largest found
	}

	private static int getIndex2OfLargest(double[] array) {
		Arrays.sort(array);
		int index = array.length - 1;
		while (array[index] == array[array.length - 1]) {
			index--;
		}
		return index;
	}

}
