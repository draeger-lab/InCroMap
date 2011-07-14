/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import de.zbit.analysis.enrichment.AbstractEnrichment;
import de.zbit.data.LabeledObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.miRNA.miRNAtargets;
import de.zbit.gui.CSVImporterV2.CSVImporterV2;
import de.zbit.io.OpenFile;
import de.zbit.io.SBFileFilter;
import de.zbit.kegg.Translator;
import de.zbit.kegg.gui.PathwaySelector;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.mapper.GeneID2GeneSymbolMapper;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.parser.Species;
import de.zbit.util.StringUtil;
import de.zbit.util.Utils;
import de.zbit.util.ValuePair;
import de.zbit.util.ValueTriplet;

/**
 * @author Clemens Wrzodek
 */

@SuppressWarnings("unchecked")
public class IntegratorGUITools {
  public static final transient Logger log = Logger.getLogger(IntegratorGUITools.class.getName());
  
  static {
    // Load list of acceptable species
    List<Species> l=null;
    try {
      l =(List<Species>) Utils.loadGZippedObject(OpenFile.searchFileAndGetInputStream("species/hmr_species_list.dat"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    organisms = l;
  }
  
  /**
   * Supported organisms.
   */
  public static final List<Species> organisms;
  //new String[]{"Homo sapiens (human)", "Mus musculus (mouse)", "Rattus norvegicus (rat)"};
  
  
  
  /**
   * Centralized method to create unified {@link JLabel}s.
   * @param s
   * @return {@link JLabel}
   */
  public static JLabel createJLabel(String s) {
    return new JLabel(s);    
  }
  
  /**
   * Show an organism selector panel to the user.
   * @return
   */
  public static JLabeledComponent getOrganismSelector() {
    JLabeledComponent l = new JLabeledComponent("Please select your organism",true,organisms);
    // Make a flexible layout
    l.setLayout(new FlowLayout());
    l.setPreferredSize(null);
    GUITools.createTitledPanel(l, "Organism selection");
    return l;
  }
  
  public static Species showOrganismSelectorDialog(Component parent) {
    JLabeledComponent organismSelector = IntegratorGUITools.getOrganismSelector();
    int ret = GUITools.showAsDialog(parent, organismSelector, "Please select your species", true);
    if (ret == JOptionPane.OK_OPTION) {
      return (Species) organismSelector.getSelectedItem();
    }
    return null;
  }
  
  public static JLabeledComponent getIdentifierSelector() {
    JLabeledComponent l = new JLabeledComponent("Please select the used identifier",true,IdentifierType.values());
    l.setSelectedItem(IdentifierType.GeneSymbol);
    // Make a flexible layout
    l.setLayout(new FlowLayout());
    l.setPreferredSize(null);
    GUITools.createTitledPanel(l, "Identifier selection");
    return l;
  }
  
  /**
   * Show the {@link CSVImporterV2} dialog.
   * @param parent parent {@link Frame} or {@link Dialog}
   * @param c {@link CSVImporterV2}
   * @param additionalComponent e.g., speciesSelector from {@link #getOrganismSelector()}
   * @return true if ok has been pressed.
   * @throws IOException 
   */
  public static boolean showCSVImportDialog(Component parent, CSVImporterV2 c, JComponent additionalComponent) throws IOException {
    c.setRenameButtonCaption("Edit observation names");
    c.setPreferredSize(new java.awt.Dimension(800, 450));
    
    // Customize the north-dialog.
    if (additionalComponent!=null) {
      JPanel jp = new JPanel(new BorderLayout());
      jp.add(additionalComponent, BorderLayout.NORTH);
      jp.add(c.getOptionalPanel(), BorderLayout.CENTER);
      c.add(jp, BorderLayout.NORTH);
    }
    
    return CSVImporterV2.showDialog(parent, c);
  }
  
  
  /**
   * Create a popup menu that allows a selection of available
   * {@link AbstractEnrichment}s.
   * @param l
   * @return
   */
  public static JPopupMenu createEnrichmentPopup(EnrichmentActionListener l) {
    JPopupMenu enrichment = new JPopupMenu("Enrichments");
    createEnrichmentPopup(l, enrichment);
    return enrichment;
  }
  
  /**
   * Append enrichment analysis {@link JMenuItem}s to the given
   * {@link JPopupMenu},
   * @param l
   * @param append
   * @return append
   */
  public static JPopupMenu createEnrichmentPopup(EnrichmentActionListener l, JPopupMenu append) {
    
    JMenuItem jm = new JMenuItem("Pathway enrichment");
    jm.setActionCommand(EnrichmentActionListener.KEGG_ENRICHMENT);
    append.add(jm);
    jm.addActionListener(l);
    
    jm = new JMenuItem("Gene ontology enrichment");
    jm.setActionCommand(EnrichmentActionListener.GO_ENRICHMENT);
    append.add(jm);
    jm.addActionListener(l);
    
    return append;
  }
  
  /**
   * Add a "Visualize pathway" {@link JMenuItem} to the given
   * {@link JPopupMenu}.
   * @param l
   * @param append
   * @return
   */
  public static JPopupMenu createKeggPathwayPopup(KEGGPathwayActionListener l, JPopupMenu append) {
    JMenuItem showPathway = GUITools.createJMenuItem(l,
        KEGGPathwayActionListener.VISUALIZE_PATHWAY,
        UIManager.getIcon("ICON_GEAR_16"));
    
    append.add(showPathway);
    
    return append;
  }
  
  /**
   * Add a right mouse popup menu to a JComponent.
   * @param component
   * @param popup
   */
  public static void addRightMousePopup(JComponent component, final JPopupMenu popup) {
    class PopClickListener extends MouseAdapter {
      public void mousePressed(MouseEvent e){
        if (e.isPopupTrigger()) doPop(e);
      }
      public void mouseReleased(MouseEvent e){
        if (e.isPopupTrigger()) doPop(e);
      }
      private void doPop(MouseEvent e){
        popup.show(e.getComponent(), e.getX(), e.getY());
      }
    }
    component.addMouseListener(new PopClickListener());
  }
  
  /**
   * Put the {@link JComponent} on a {@link JScrollPane}.
   * @param jc
   * @return
   */
  public static JScrollPane putOnScrollPane(JComponent jc) {
    // Put all on a scroll  pane
    final JScrollPane scrollPane = new JScrollPane(jc, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    
    // When resizing, try to optimize table size.
    if (jc instanceof JTable) {
      IntegratorTab.applyTableConstraints((JTable)jc, scrollPane);
    }
    
    return scrollPane;
  }
  
  /**
   * Execute the {@link Runnable} in an external thread.
   * @param r
   */
  public static void runInSwingWorker(final Runnable r) {
    final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
      public Void doInBackground() {
        try {
          r.run();
        } catch (Throwable t) {
          GUITools.showErrorMessage(null, t);
        }
        return null;
      }
      protected void done() {
        //hideTemporaryPanel();
        //getParentWindow().setEnabled(true);
      }
    };
    
    worker.execute();
  }

  /**
   * Return a priority for an {@link IdentifierType}.
   * NCBI_GeneIDs have the lowest priority, followed by
   * Unique identifiers (ensembl, refseq, etc.) and
   * non-unique identifiers (gene symobls, etc.) have
   * the highest priority.
   * @param type
   * @return
   */
  public static Integer getPriority(IdentifierType type) {
    if (type.equals(IdentifierType.NCBI_GeneID)) return 0;
    else if (type.equals(IdentifierType.Ensembl)) return 1;
    else if (type.equals(IdentifierType.KeggGenes)) return 1;
    else if (type.equals(IdentifierType.RefSeq)) return 1;
    else if (type.equals(IdentifierType.GeneSymbol)) return 2;
    else if (type.equals(IdentifierType.Unknown)) return 3;
    else {
      log.log(Level.SEVERE, "Please implement priority for " + type);
      return 3;
    }
  }

  /**
   * Create a new {@link JLabeledComponent} that lets the user choose a signal from
   * the contained signals in the given <code>ns</code>.
   * @param <T>
   * @param ns
   * @return
   */
  public static <T extends NameAndSignals> JLabeledComponent createSelectExperimentBox(T ns) {
    JLabeledComponent jc = new JLabeledComponent("Select an observation",true,new String[]{"temp"});
    return createSelectExperimentBox(jc, ns);
  }
  
  /**
   * Replace the signals in the existing experiment box by the given one.
   * @param <T>
   * @param jc
   * @param ns
   * @return
   */
  public static <T extends NameAndSignals> JLabeledComponent createSelectExperimentBox(JLabeledComponent jc, T ns) {
    Collection<ValuePair<String, SignalType>> c = ns.getSignalNames();
    jc.setHeaders(c);
    return jc;
  }
  
  /**
   * Shows a signal selection box.
   * @param <T>
   * @param ui
   * @return ValueTriplet of (TabIndex In {@link IntegratorUI#getTabbedPane()}, ExperimentName, {@link SignalType}) or null.  
   */
  public static <T extends NameAndSignals> ValueTriplet<NameAndSignalsTab, String, SignalType> showSelectExperimentBox(IntegratorUI ui) {
    return showSelectExperimentBox(ui, null);
  }
  /**
   * Shows a signal selection box.
   * @param <T>
   * @param ui
   * @param initialSelection
   * @return ValueTriplet of (TabIndex In {@link IntegratorUI#getTabbedPane()}, ExperimentName, {@link SignalType}) or null.
   */
  @SuppressWarnings("rawtypes")
  public static <T extends NameAndSignals> ValueTriplet<NameAndSignalsTab, String, SignalType> showSelectExperimentBox(IntegratorUI ui, IntegratorTab initialSelection) {
    return showSelectExperimentBox(ui, initialSelection, null);
  }
  

  /**
   * @return a list with tab names and actual tabs for every {@link NameAndSignalsTab} that contains
   * {@link NameAndSignals} objects with signals.
   */
  public static List<LabeledObject<NameAndSignalsTab>> getNameAndSignalTabsWithSignals() {
    IntegratorUI ui = IntegratorUI.getInstance();
    List<LabeledObject<NameAndSignalsTab>> datasets = new LinkedList<LabeledObject<NameAndSignalsTab>>();
    for (int i=0; i<ui.getTabbedPane().getTabCount(); i++) {
      Component c = ui.getTabbedPane().getComponentAt(i);
      if (c instanceof NameAndSignalsTab) {
        //Class<?> cl = ((NameAndSignalsTab)c).getDataContentType(); 
        //if (cl.equals(mRNA.class) || cl.equals(miRNA.class)) {
        if (((NameAndSignalsTab)c).getSourceTab()==null && // Data has not been derived, but read from disk!
            ((NameAndSignals)((NameAndSignalsTab)c).getExampleData()).hasSignals()) {
          datasets.add(new LabeledObject<NameAndSignalsTab>(
              ui.getTabbedPane().getTitleAt(i), (NameAndSignalsTab) c));
        }
      }
    }
    return datasets;
  }
  
  /**
   * Shows a signal selection box.
   * @param <T>
   * @param ui
   * @param initialSelection
   * @param dialogTitle
   * @return ValueTriplet of (TabIndex In {@link IntegratorUI#getTabbedPane()}, ExperimentName, {@link SignalType}) or null.
   */
  @SuppressWarnings("rawtypes")
  public static <T extends NameAndSignals> ValueTriplet<NameAndSignalsTab, String, SignalType> showSelectExperimentBox(IntegratorUI ui, IntegratorTab initialSelection, String dialogTitle) {
    final JPanel jp = new JPanel(new BorderLayout());
    int initialSelIdx=0;
    
    // Create a list of available datasets and get initial selection.
    List<LabeledObject<NameAndSignalsTab>> datasets = getNameAndSignalTabsWithSignals();
    for (int i=0; i<datasets.size(); i++) {
      Component c = datasets.get(i).getObject();
      if (initialSelection!=null && c.equals(initialSelection)) {
        initialSelIdx=i;
        break;
      }
    }
    if (datasets.size()<1) {
      GUITools.showMessage("Could not find any input datasets with observations.", ui.getApplicationName());
      return null;
    } else {
      final JLabeledComponent dataSelect = new JLabeledComponent("Select a dataset",true,datasets);
      dataSelect.setSelectedItem(datasets.get(initialSelIdx));
      
      // Add the dataset selector to a panel
      jp.add (dataSelect, BorderLayout.CENTER);
      
      // Add action listener to let user choose experiment from dataset
      final JLabeledComponent selExpBox = createSelectExperimentBox((NameAndSignals)((NameAndSignalsTab)datasets.get(initialSelIdx).getObject()).getExampleData());
      jp.add(selExpBox, BorderLayout.SOUTH);
      dataSelect.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          NameAndSignalsTab tab = (NameAndSignalsTab) ((LabeledObject)dataSelect.getSelectedItem()).getObject();
          createSelectExperimentBox(selExpBox, (NameAndSignals)tab.getExampleData());
        }
      });
      
      // Show and evaluate dialog
      if (dialogTitle==null) dialogTitle = UIManager.getString("OptionPane.titleText");
      int ret = JOptionPane.showConfirmDialog(ui, jp, dialogTitle, JOptionPane.OK_CANCEL_OPTION);
      if (ret==JOptionPane.OK_OPTION) {
        ValuePair<String, SignalType> expSignal = (ValuePair<String, SignalType>) selExpBox.getSelectedItem();
        return new ValueTriplet<NameAndSignalsTab, String, SignalType>(
            (NameAndSignalsTab) ((LabeledObject)dataSelect.getSelectedItem()).getObject(),
            expSignal.getA(), expSignal.getB());
      } else {
        return null;
      }
      
    }
  }
  
  /**
   * Shows a dialog that requests a gene list from the user.
   * @return Selected {@link Species}, selected {@link IdentifierType} and list of genes (newline separated)
   * or <code>null</code> if canceled.
   */
  public static ValueTriplet<Species, IdentifierType, String> showInputGeneListDialog() {
    
    // Ask user for species and Identifiertype in the north.
    JPanel north = new JPanel(new BorderLayout());
    JLabeledComponent organism = getOrganismSelector();
    JLabeledComponent identifier = getIdentifierSelector();
    north.add(organism, BorderLayout.NORTH);
    north.add(identifier, BorderLayout.CENTER);
    north.add(new JLabel("Please enter a list of genes, separated by new lines."), BorderLayout.SOUTH);
    
    // Create the main panel
    JPanel p = new JPanel(new BorderLayout());
    p.add(north, BorderLayout.NORTH);
    
    // Input gene list in the center
    final JTextArea text = new JTextArea (10, 50);
    JScrollPane scrollPane = new JScrollPane(text);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    p.add(scrollPane, BorderLayout.CENTER);
    
    // Allow reading genes from input file via button on bottom of the list
    JButton readFromFile = new JButton("Read from file", UIManager.getIcon("ICON_OPEN_16"));
    readFromFile.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        File file = GUITools.openFileDialog(IntegratorUI.getInstance(), 
          IntegratorUI.openDir, true, JFileChooser.FILES_ONLY, (FileFilter)null);
        if (file!=null) {
          try {
            BufferedReader reader = OpenFile.openFile(file.getPath());
            StringBuffer buff = new StringBuffer();
            String line;
            while ((line=reader.readLine())!=null) {
              buff.append(line);
              buff.append(StringUtil.newLine());
            }
            text.setText(buff.toString());
          } catch (IOException e1) {
            GUITools.showErrorMessage(IntegratorUI.getInstance(), e1);
          }
        }
      }
    });
    p.add(readFromFile, BorderLayout.SOUTH);
    
    // Show dialog and return input string.
    int ret = JOptionPane.showConfirmDialog(IntegratorUI.getInstance(), p, "Please enter a list of genes", JOptionPane.OK_CANCEL_OPTION);
    if (ret==JOptionPane.OK_OPTION) {
      return new ValueTriplet<Species, IdentifierType, String>((Species)organism.getSelectedItem(), 
          (IdentifierType)identifier.getSelectedItem(), 
          text.getText());
    } else {
      return null;
    }
    
  }

  /**
   * Returns a 2GeneID mapping for the given <code>species</code>.
   * <p>Every created instance is cached for later usage.
   * @param species
   * @return 
   */
  public static GeneID2GeneSymbolMapper get2GeneSymbolMapping(Species species) {
    String key = GeneID2GeneSymbolMapper.class.getSimpleName().concat(species.getCommonName());
    Object mapper = UIManager.get(key);
    if (mapper==null) {
      try {
        mapper = new GeneID2GeneSymbolMapper(species.getCommonName());
      } catch (IOException e) {
        GUITools.showErrorMessage(IntegratorUI.getInstance(), e);
      }
      if (mapper!=null) {
        UIManager.put(key, mapper);
      }
    }
    return (GeneID2GeneSymbolMapper) mapper;
  }
  
  /**
   * Load and filter microRNA targets.
   * @param species if null, user will be asked for a species.
   * @return
   */
  public static ValuePair<miRNAtargets, Species> loadMicroRNAtargets(Species species) {
    
    // Initialize panel and place organism selector on top.
    JPanel p = new JPanel();
    LayoutHelper lh = new LayoutHelper(p);
    JLabeledComponent orgSel = null;
    if (species==null) {
      orgSel = getOrganismSelector();
      lh.add(orgSel);
    }
    
    // Create only experimental button
    final JCheckBox onlyExperimental = new JCheckBox("Only experimentally validated targets");
    onlyExperimental.setSelected(false);
    lh.add(onlyExperimental);
    
    // One button per data source
    JPanel dbs = new JPanel(new GridLayout(6, 1));
    final JCheckBox miRecords = new JCheckBox("miRecords v3", true);
    final JCheckBox miRTarBase = new JCheckBox("miRTarBase", true);
    final JCheckBox tarBase = new JCheckBox("TarBase V5.0", true);
    
    final JCheckBox DIANA = new JCheckBox("DIANA - microT v3.0", true);
    final JCheckBox ElMMo = new JCheckBox("ElMMo v4", true);
    final JCheckBox TargetScan = new JCheckBox("TargetScan v5.1", true);
    DIANA.setToolTipText("Predicted \"" + DIANA.getText() + "\" targets. Only high confidence targets are included.");
    ElMMo.setToolTipText("Predicted \"" + ElMMo.getText() + "\" targets. Only high confidence targets are included.");
    TargetScan.setToolTipText("Predicted \"" + TargetScan.getText() + "\" targets. Only high confidence targets are included.");
    
    dbs.add(miRecords); dbs.add(miRTarBase); dbs.add(tarBase);
    dbs.add(DIANA); dbs.add(ElMMo); dbs.add(TargetScan);
    dbs.setBorder(BorderFactory.createTitledBorder("Select databases to load"));
    lh.add(dbs);
    
    // Enable and disable predictions on only-experimental click
    onlyExperimental.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        boolean state=true;
        if (onlyExperimental.isSelected()) {
          state=false;
        }
        DIANA.setEnabled(state);
        ElMMo.setEnabled(state);
        TargetScan.setEnabled(state);
      }
    });
    
    // Ask user
    int ret = JOptionPane.showConfirmDialog(IntegratorUI.getInstance(), p, 
        "Please select microRNA targets to load.", JOptionPane.OK_CANCEL_OPTION);
    if (ret==JOptionPane.OK_OPTION) {
      if (orgSel!=null) species = (Species) orgSel.getSelectedItem();
      log.info("Loading microRNA target file for " + species + ".");
      
      // Load targets
      miRNAtargets t_all;
      // XXX: Show loading message here.
      try {
        t_all = (miRNAtargets) Utils.loadGZippedObject(
          OpenFile.searchFileAndGetInputStream("miRNA_targets/" + species.getNCBITaxonID() + "_HC.dat"));
        if (t_all==null) throw new IOException("Could not read miRNA target file.");
      } catch (IOException e) {
        GUITools.showErrorMessage(IntegratorUI.getInstance(), e);
        return null;
      }
      
      // Filter targets
      if (onlyExperimental.isSelected()) t_all.filterTargetsOnlyExperimental();
      if (!miRecords.isSelected()) t_all.removeTargetsFrom("miRecords");
      if (!miRTarBase.isSelected()) t_all.removeTargetsFrom("miRTarBase");
      if (!tarBase.isSelected()) t_all.removeTargetsFrom("TarBase");
      if (!DIANA.isSelected()) t_all.removeTargetsFrom("DIANA");
      if (!ElMMo.isSelected()) t_all.removeTargetsFrom("ElMMo");
      if (!TargetScan.isSelected()) t_all.removeTargetsFrom("TargetScan");
      
      log.info(StatusBar.defaultText);
      return new ValuePair<miRNAtargets, Species>(t_all, species);
    }
    return null;
  }

  /**
   * Lets the user choose pathways and observations and a output format. Batch creates a 
   * picture for every pathway and observation.
   */
  public static void showBatchPathwayDialog() {

    // Create a list of available datasets and signals.
    List<LabeledObject<NameAndSignalsTab>> datasets = getNameAndSignalTabsWithSignals();
    if (datasets.size()<1) {
      GUITools.showMessage("Could not find any input datasets with observations.", IntegratorUI.appName);
      return;
    }
    List<LabeledObject<ValuePair<NameAndSignalsTab, ValuePair<String, SignalType>>>> labeledSignals
      = new ArrayList<LabeledObject<ValuePair<NameAndSignalsTab, ValuePair<String, SignalType>>>>();
    
    // Looks complicated but isn't. Create a list with labels and pair of NSTab and Signal in nsTab.
    for (LabeledObject<NameAndSignalsTab> l: datasets) {
      for (ValuePair<String, SignalType> sigVp: ((NameAndSignals)l.getObject().getExampleData()).getSignalNames() ) {
        String label = String.format("%s: %s [%s]", l.getLabel(), sigVp.getA(), sigVp.getB());
        labeledSignals.add(new LabeledObject<ValuePair<NameAndSignalsTab, ValuePair<String, SignalType>>>(
            label, new ValuePair<NameAndSignalsTab, ValuePair<String, SignalType>>(l.getObject(), sigVp)));
      }
    }
      
      
      
    // Initialize panel and place organism selector on top.
    JPanel p = new JPanel();
    LayoutHelper lh = new LayoutHelper(p);
    
    // Create experiments list
    final JList experiments = new JList(labeledSignals.toArray());
    experiments.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    JScrollPane expScroll = new JScrollPane(experiments);
    expScroll.setMaximumSize(new Dimension(320,240));
    expScroll.setBorder(BorderFactory.createTitledBorder("Select observation(s)"));
    lh.add(expScroll);
    
    // Create pathway list
    final JList pathways = new JList(new String[]{"Please wait, loading list of pathways."});
    pathways.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    pathways.setEnabled(false);
    
    JScrollPane pwScroll = new JScrollPane(pathways);
    pwScroll.setMaximumSize(new Dimension(320,240));
    pwScroll.setBorder(BorderFactory.createTitledBorder("Select pathways(s)"));
    lh.add(pwScroll);
    
    final List<LabeledObject<String>> pwName = new ArrayList<LabeledObject<String>>();
    Thread loadPathways = new Thread() {
      @Override
      public void run() {
        // Get reference pathway list
        Map<String, String> temp=null;
        try {
          temp = PathwaySelector.getPathways(null, Translator.getFunctionManager());
        } catch (IOException e) {
          GUITools.showErrorMessage(null, e, "Could not get list of available KEGG pathways.");
        }
        
        // Create list, put pathway name on front and sort list
        if (temp!=null) {
          for (Map.Entry<String, String> pw : temp.entrySet()) {
            pwName.add(new LabeledObject<String>(pw.getValue(), pw.getKey()));
          }
          Collections.sort(pwName);
          
          // Clear existing list
          pathways.removeAll();
          
          // Set to model
          pathways.setModel(new AbstractListModel() {
            private static final long serialVersionUID = 1L;
            public int getSize() { return pwName.size(); }
            public Object getElementAt(int i) { return pwName.get(i); }
          });
        }
        
        pathways.setEnabled(pwName.size()>0);
        GUITools.packParentWindow(pathways);
      }
      
    };
    loadPathways.start();
    
    // Output Format
    final JComboBox fileFormat = new JComboBox(TranslatorPanel.getGraphMLfilefilter().toArray());
    fileFormat.setBorder(BorderFactory.createTitledBorder("Select output file format"));
    lh.add(fileFormat);
    
    
    // Ask user
    int ret = JOptionPane.showConfirmDialog(IntegratorUI.getInstance(), p, 
        "Please select the data to visualize", JOptionPane.OK_CANCEL_OPTION);
    if (ret==JOptionPane.OK_OPTION) {
      if (pathways.isEnabled() && pathways.getSelectedIndices().length>0 &&
        experiments.getSelectedIndices().length>0 && fileFormat.getSelectedIndex()>=0) {
      File outputDir = GUITools.saveFileDialog(IntegratorUI.getInstance(), IntegratorUI.saveDir, false, false, true, 
        JFileChooser.DIRECTORIES_ONLY, (FileFilter[])null);
      if (outputDir!=null) {
        // Create result arrays
        ValuePair<?, ?>[] exps = new ValuePair<?, ?>[experiments.getSelectedIndices().length];
        for (int i=0; i<exps.length; i++)
          exps[i] = labeledSignals.get(experiments.getSelectedIndices()[i]).getObject();
        
        String[] refPWids = new String[pathways.getSelectedIndices().length];
        for (int i=0; i<refPWids.length; i++)
          refPWids[i] = pwName.get(pathways.getSelectedIndices()[i]).getObject();
        
        Signal2PathwayTools.batchCreatePictures(
          (ValuePair<NameAndSignalsTab, ValuePair<String, SignalType>>[]) exps,
          refPWids,
          ((SBFileFilter)fileFormat.getSelectedItem()).getExtension(),
          outputDir);
        
      }
      } else {
        GUITools.showMessage("Could not continue: invalid selection.", IntegratorUI.appName);
      }
    }
    return; 
  }
    
}












