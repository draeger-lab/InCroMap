/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.beans.EventHandler;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.miRNA.miRNAtargets;
import de.zbit.gui.actions.TranslatorTabActions;
import de.zbit.gui.actions.listeners.KEGGPathwayActionListener;
import de.zbit.gui.prefs.IntegratorIOOptions;
import de.zbit.gui.prefs.PathwayVisualizationOptions;
import de.zbit.gui.tabs.IntegratorTab;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.integrator.ReaderCache;
import de.zbit.io.DNAMethylationReader;
import de.zbit.io.NameAndSignalReader;
import de.zbit.io.ProteinModificationReader;
import de.zbit.io.mRNAReader;
import de.zbit.io.miRNAReader;
import de.zbit.kegg.KEGGtranslatorOptions;
import de.zbit.kegg.Translator;
import de.zbit.kegg.gui.TranslatorPanel;
import de.zbit.kegg.io.KEGGtranslatorIOOptions.Format;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.parser.Species;
import de.zbit.util.StringUtil;
import de.zbit.util.ValuePair;
import de.zbit.util.ValueTriplet;
import de.zbit.util.logging.LogUtil;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;
import de.zbit.visualization.VisualizeMicroRNAdata;

/**
 * A GUI for the Integrator application
 * @author Clemens Wrzodek
 */
public class IntegratorUI extends BaseFrame {
  private static final long serialVersionUID = 5599356490171620598L;
  public static final transient Logger log = Logger.getLogger(IntegratorUI.class.getName());
  
  /**
   * The name of the application
   */
  public final static String appName = "Integrator";
  
  /**
   * The version of {@link #appName}
   */
  public final static String appVersion = "0.0.1";
  
  /**
   * A simple light blue color. Used e.g. in {@link PathwayVisualizationOptions}
   * and in {@link VisualizeMicroRNAdata}.
   */
  public static final Color LIGHT_BLUE = new Color(0,153,255);
  
  /**
   * Since only one instance is allowed to show at a time,
   * this is a convenient method to get the current instance.
   */
  public static IntegratorUI instance = null;

  /**
   * Since only one instance is allowed to show at a time,
   * this is a convenient method to get the current instance.
   */
  public static IntegratorUI getInstance() {
    return instance;
  }
  
  /**
   * This is the main component
   */
  private JTabbedPane tabbedPane;
  
  /**
   * This map is required, because KEGGtranslator has no dynamic changing
   * Toolbar!
   */
  private Map<TranslatorPanel, TranslatorTabActions> translatorActionMap = new HashMap<TranslatorPanel, TranslatorTabActions>();
  
  /**
   * Holds a duplicate of the list of recently opened files.
   * Strangely, this does not work if it is not static.
   */
  private static JMenu fileHistoryDuplicate=null;
  
  /**
   * Default directory path's for saving and opening files. Only init them
   * once. Other classes should use these variables.
   */
  public static String openDir, saveDir;
  
  /**
   * preferences is holding all project specific preferences
   */
  private SBPreferences prefsIO;
  
  /**
   * This label should always display the species of a currently selected tab.
   */
  private JLabel speciesLabel;
  
  public static enum Action implements ActionCommand {
    /**
     * {@link Action} to save the currently opened tabs.
     */
    WORKSPACE_SAVE,
    /**
     * {@link Action} to load opened tabs from another session.
     */
    WORKSPACE_LOAD,
    /**
     * {@link Action} to load data with the {@link mRNAReader}.
     */
    LOAD_MRNA,
    /**
     * {@link Action} to load data with the {@link miRNAReader}.
     */
    LOAD_MICRO_RNA,
    /**
     * {@link Action} to load data with the {@link ProteinModificationReader}.
     */
    LOAD_PROTEIN_MODIFICATION,
    /**
     * {@link Action} to load data with the {@link DNAMethylationReader}
     */
    LOAD_DNA_METHYLATION,
    /**
     * {@link Action} to let the user enter a custom gene list.
     */
    INPUT_GENELIST,
    /**
     * {@link Action} to show raw {@link miRNAtargets}.
     */
    SHOW_MICRO_RNA_TARGETS,
    /**
     * {@link Action} to show a pathway selection dialog. 
     */
    NEW_PATHWAY,
    /**
     * Creates pictures for many observations and pathways.
     */
    BATCH_PATHWAY_VISUALIZATION,
    /**
     * Show different data of different types in one pathway.
     */
    INTEGRATED_HETEROGENEOUS_DATA_VISUALIZATION,
    /**
     * Show an integrated TreeTable of various data types.
     */
    INTEGRATED_TABLE;
    
    
    /*
     * (non-Javadoc)
     * 
     * @see de.zbit.gui.ActionCommand#getName()
     */
    public String getName() {
      switch (this) {
      case WORKSPACE_SAVE:
        return "Save workspace";
      case WORKSPACE_LOAD:
        return "Load workspace";
      case LOAD_MRNA:
        return "Load mRNA data";
      case LOAD_MICRO_RNA:
        return "Load microRNA data";
      case LOAD_PROTEIN_MODIFICATION:
        return "Load protein modification data";
      case LOAD_DNA_METHYLATION:
        return "Load DNA methylation data";
      case INPUT_GENELIST:
        return "Manually enter a genelist";
      case SHOW_MICRO_RNA_TARGETS:
        return "Show microRNA targets";
      case NEW_PATHWAY:
        return "Show pathway";
      case INTEGRATED_TABLE:
        return "Integrate heterogeneous data";
        
      default:
        return StringUtil.firstLetterUpperCase(toString().toLowerCase().replace('_', ' '));
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see de.zbit.gui.ActionCommand#getToolTip()
     */
    public String getToolTip() {
      switch (this) {
        case WORKSPACE_SAVE:
          return "Save all currently opened tabs to disk.";
        case WORKSPACE_LOAD:
          return "Load opened tabs from a previous session.";
        case LOAD_MRNA:
          return "Load processed mRNA data from file.";
        case LOAD_MICRO_RNA:
          return "Load processed microRNA data from file.";
        case LOAD_PROTEIN_MODIFICATION:
          return "Load processed protein modification expression data from a file";
        case LOAD_DNA_METHYLATION:
          return "Load processed and gene-centered DNA methylation data from a file"; // TODO: stimmt das?
        case INPUT_GENELIST:
          return "Manually enter a list of genes.";
        case SHOW_MICRO_RNA_TARGETS:
          return "Show raw microRNA targets for an organism.";
        case NEW_PATHWAY:
          return "Download and visualize a KEGG pathway.";
        case BATCH_PATHWAY_VISUALIZATION:
          return "Batch color nodes in various pathways according to observations and save these pictures as images.";
        case INTEGRATED_HETEROGENEOUS_DATA_VISUALIZATION:
          return "Visualize heterogeneous data from different datasets in one pathway.";
        case INTEGRATED_TABLE:
          return "Integrate multiple heterogeneous datasets from different platforms by building a gene-centered tree.";
          
        default:
          return "";
      }
    }
    
  }
  
  /**
   * @return list of command-line options.
   */
  public static List<Class<? extends KeyProvider>> getStaticCommandLineOptions() {
    List<Class<? extends KeyProvider>> configList = new LinkedList<Class<? extends KeyProvider>>();
    configList.add(IntegratorIOOptions.class);
    configList.add(GUIOptions.class);
    configList.add(PathwayVisualizationOptions.class);
    return configList;
  }
  
  /**
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {
    LogUtil.initializeLogging(Level.INFO,(String[])null);
    // Set default values for KEGGtranslator
    KEGGtranslatorOptions.REMOVE_ORPHANS.setDefaultValue(false);
    KEGGtranslatorOptions.REMOVE_WHITE_GENE_NODES.setDefaultValue(false);
    // SBProperties props = 
    SBPreferences.analyzeCommandLineArguments(getStaticCommandLineOptions(), args);
    
    // Set the often used KeggTranslator methods to use this appName as application name
    Translator.APPLICATION_NAME = appName;
    Translator.VERSION_NUMBER = appVersion;
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        IntegratorUI ui = new IntegratorUI();
        ui.setVisible(true);
        GUITools.hideSplashScreen();
        ui.toFront();
        
        try {
          mRNAReader r = mRNAReader.getExampleReader();
          ui.addTab(new NameAndSignalsTab(ui, r.read("mRNA_data_new.txt"), IntegratorUITools.organisms.get(1)), "Example_mRNA");
          
          miRNAReader r2 = new miRNAReader(1,0);
          r2.addSignalColumn(25, SignalType.FoldChange, "Ctnnb1"); // 25-28 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
          r2.addSignalColumn(29, SignalType.pValue, "Ctnnb1"); // 29-32 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
          ui.addTab(new NameAndSignalsTab(ui, r2.read("miRNA_data.txt"), IntegratorUITools.organisms.get(1)), "Example_miRNA");
          
        } catch (Exception e) {e.printStackTrace();}
        
      }
    });
    
  }
  
  public IntegratorUI() {
    super();
    
    // init preferences
    initPreferences();
    
    // Load reader cache
    ReaderCache.getCache();
    
    // TODO: Icons and preferences
    /*File file = new File(prefsIO.get(KEGGtranslatorIOOptions.INPUT));
    openDir = file.isDirectory() ? file.getAbsolutePath() : file.getParent();
    file = new File(prefsIO.get(KEGGtranslatorIOOptions.OUTPUT));
    saveDir = file.isDirectory() ? file.getAbsolutePath() : file.getParent();*/
    
    // Depending on the current OS, we should add the following image
    // icons: 16x16, 32x32, 48x48, 128x128 (MAC), 256x256 (Vista).
    /*int[] resolutions=new int[]{16,32,48,128,256};
    List<Image> icons = new LinkedList<Image>();
    for (int res: resolutions) {
      Object icon = UIManager.get("KEGGtranslatorIcon_"+res);
      if ((icon != null) && (icon instanceof ImageIcon)) {
        icons.add(((ImageIcon) icon).getImage());
      }
    }
    setIconImages(icons);*/
    
    initStatusBarSpeciesLabel();
    
    instance = this;
  }
  
  /**
   * Init preferences, if not already done.
   */
  private void initPreferences() {
    if (prefsIO == null) {
      prefsIO = SBPreferences.getPreferencesFor(IntegratorIOOptions.class);
    }
  }
  
  /**
   * Places a new label on the {@link #getStatusBar()}s right side,
   * displaying the species of each selected tab.
   */
  private void initStatusBarSpeciesLabel() {
    // Create a smaller panel for the statusBar
    Dimension panelSize = new Dimension(200, 15);
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    panel.setOpaque(false);
    panel.setPreferredSize(panelSize);
    speciesLabel = new JLabel();
    speciesLabel.setOpaque(false);
    speciesLabel.setHorizontalTextPosition(JLabel.RIGHT);
    panel.add(speciesLabel);
    
    statusBar.add(panel, BorderLayout.EAST);
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#closeFile()
   */
  @Override
  public boolean closeFile() {
    int idx = tabbedPane.getSelectedIndex();
    if (idx>=0) {
      return closeTab(idx);
    }
    return false;
  }
  
  /**
   * Close the given tab from the {@link #tabbedPane}.
   * @param tab
   * @return true if and only if the tab has been removed from the tabbedPane.
   */
  public boolean closeTab(Component tab) {
    return closeTab(getTabIndex(tab));
  }
  
  /**
   * Cloase tab at specified index.
   * @param tabIndex
   * @return
   */
  public boolean closeTab(int tabIndex) {
    try {
      // Remove tab-related action
      if (tabbedPane.getComponentAt(tabIndex) instanceof TranslatorPanel) {
        translatorActionMap.remove(tabbedPane.getComponentAt(tabIndex));
      }
      // Close tab
      tabbedPane.removeTabAt(tabIndex);
      
      updateButtons();
      return true;
    } catch (Exception e) {
      log.log(Level.WARNING, "Could not close tab.", e);
    }
    return false;
  }
  
  /**
   * Returns the index of the <code>tab</code> in the
   * {@link #tabbedPane}.
   * @param tab
   * @return -1 if not found, else the index.
   */
  public int getTabIndex(Component tab) {
    if (tab==null) return -1;
    for (int i=0; i<tabbedPane.getTabCount(); i++) {
      Component c = tabbedPane.getComponentAt(i);
      if (c!=null && c.equals(tab)) {
        return i;
      }
    }
    return -1;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#additionalFileMenuItems()
   */
  @Override
  protected JMenuItem[] additionalFileMenuItems() {
    return new JMenuItem[] {
        /* TOO difficult to save workspace...
         * GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "saveWorkspace"),
          Action.WORKSPACE_SAVE, UIManager.getIcon("ICON_SAVE_16"), KeyStroke.getKeyStroke('W',InputEvent.CTRL_DOWN_MASK),
          'W', false)*/
          };
  }
  
  @Override
  protected JMenu[] additionalMenus() {
    
    /*
     * Import data tab.
     */
    JMenu importData = new JMenu("Import data");
    
    JMenuItem lmRNA = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openMRNAfile"),
        Action.LOAD_MRNA, UIManager.getIcon("ICON_OPEN_16"));
      
    JMenuItem lmiRNA = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openMiRNAfile"),
        Action.LOAD_MICRO_RNA, UIManager.getIcon("ICON_OPEN_16"));
    
    JMenuItem lprotMod = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openProteinModificationExpressionFile"),
      Action.LOAD_PROTEIN_MODIFICATION, UIManager.getIcon("ICON_OPEN_16"));
    
    JMenuItem ldnaM = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openDNAmethylationFile"),
      Action.LOAD_DNA_METHYLATION, UIManager.getIcon("ICON_OPEN_16"));
      
    JMenuItem genelist = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "showInputGenelistDialog"),
        Action.INPUT_GENELIST, UIManager.getIcon("ICON_OPEN_16"));

    JMenuItem miRNAtargets = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "showMicroRNAtargets"),
        Action.SHOW_MICRO_RNA_TARGETS, UIManager.getIcon("ICON_GEAR_16"));

    JMenuItem newPathway = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openPathwayTab"),
        Action.NEW_PATHWAY, UIManager.getIcon("ICON_GEAR_16"));

    importData.add(lmRNA);
    importData.add(lmiRNA);
    importData.add(lprotMod);
    importData.add(ldnaM);
    importData.addSeparator();
    importData.add(genelist);
    importData.add(miRNAtargets);
    importData.add(newPathway);
    
    /*
     * Tools tab.
     */
    JMenu tools = new JMenu("Tools");
    tools.add(GUITools.createJMenuItem(EventHandler.create(ActionListener.class, new IntegratorUITools(), "showBatchPathwayDialog"),
      Action.BATCH_PATHWAY_VISUALIZATION, UIManager.getIcon("ICON_GEAR_16")));
    tools.add(GUITools.createJMenuItem(EventHandler.create(ActionListener.class, new IntegratorUITools(), "showIntegratedVisualizationDialog"),
      Action.INTEGRATED_HETEROGENEOUS_DATA_VISUALIZATION, UIManager.getIcon("ICON_GEAR_16")));
    
    tools.add(GUITools.createJMenuItem(EventHandler.create(ActionListener.class, new IntegratorUITools(), "showIntegratedTreeTableDialog"),
      Action.INTEGRATED_TABLE, UIManager.getIcon("ICON_GEAR_16")));
    
    
    
    return new JMenu[]{importData, tools};
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#createJToolBar()
   */
  @Override
  protected JToolBar createJToolBar() {
    final JToolBar r = new JToolBar(appName+" toolbar", JToolBar.HORIZONTAL);
    r.setLayout(new GridLayout());
    return createJToolBarForNoTabs(r);
  }
  
  /**
   * Place buttons for loading items, etc. on the {@link JToolBar}.
   * This is intended to show up when no tab is selected, i.e., the
   * {@link #tabbedPane} is empty.
   * @param r
   * @return
   */
  protected JToolBar createJToolBarForNoTabs(JToolBar r) {
    
    // Place buttons, depending on current panel
    Object o = getCurrentlySelectedPanel();
    if (o!=null) {
      if (o instanceof BaseFrameTab) {
        ((BaseFrameTab)o).updateButtons(getJMenuBar(), r);
      } else {
        log.severe("Implement toolbar for " + o.getClass());
      }
    } else {
      if (r.getName().equals(getClass().getSimpleName())) return r; //Already done.
      r.removeAll();
      r.setName(getClass().getSimpleName());
      
      // Create several loadData buttons
      JPopupMenu load = new JPopupMenu("Load data");
      JMenuItem lmRNA = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openMRNAfile"),
        Action.LOAD_MRNA, UIManager.getIcon("ICON_OPEN_16"));
      load.add(lmRNA);
      JMenuItem lmiRNA = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openMiRNAfile"),
        Action.LOAD_MICRO_RNA, UIManager.getIcon("ICON_OPEN_16"));
      load.add(lmiRNA);
      JMenuItem lprotMod = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openProteinModificationExpressionFile"),
        Action.LOAD_PROTEIN_MODIFICATION, UIManager.getIcon("ICON_OPEN_16"));
      load.add(lprotMod);
      JMenuItem ldnaM = GUITools.createJMenuItem(EventHandler.create(ActionListener.class, this, "openDNAmethylationFile"),
        Action.LOAD_DNA_METHYLATION, UIManager.getIcon("ICON_OPEN_16"));
      load.add(ldnaM);
      load.addSeparator();
      if (fileHistoryDuplicate==null) {
        fileHistoryDuplicate = createFileHistory();
      }
      load.add(fileHistoryDuplicate);
      JDropDownButton loadDataButton = new JDropDownButton("Load data", UIManager.getIcon("ICON_OPEN_16"), load);
      
      JButton genelist = GUITools.createJButton(EventHandler.create(ActionListener.class, this, "showInputGenelistDialog"),
        Action.INPUT_GENELIST, UIManager.getIcon("ICON_OPEN_16"));
      
      JButton miRNAtargets = GUITools.createJButton(EventHandler.create(ActionListener.class, this, "showMicroRNAtargets"),
        Action.SHOW_MICRO_RNA_TARGETS, UIManager.getIcon("ICON_GEAR_16"));
      
      JButton newPathway = GUITools.createJButton(EventHandler.create(ActionListener.class, this, "openPathwayTab"),
        Action.NEW_PATHWAY, UIManager.getIcon("ICON_GEAR_16"));
      
      r.add(loadDataButton);
      r.add(genelist);
      r.add(miRNAtargets);
      r.add(newPathway);
    }
    
    GUITools.setOpaqueForAllElements(r, false);
    return r;
  }
  
  /**
   * Add a single file to the history.
   * @param f
   */
  private void addToFileHistory(File... f) {
    if (f==null || f.length<1) return;
    List<File> l = new LinkedList<File>();
    for (File file : f) {
      if (file!=null) l.add(file);
    }
    if (l.size()<1) return;
    
    addToFileHistory(l, fileHistoryDuplicate);
  }
  
  public void openMRNAfile() {
    openFile(null, mRNAReader.class);
  }

  public void openProteinModificationExpressionFile() {
    openFile(null, ProteinModificationReader.class);
  }
  
  public void openDNAmethylationFile() {
    openFile(null, DNAMethylationReader.class);
  }
  
  public void openMiRNAfile() {
    openFile(null, miRNAReader.class);
  }
  
  /**
   * Shows a custom input genelist dialog to the user and adds this
   * list of genes to the {@link #tabbedPane}.
   */
  public void showInputGenelistDialog() {
    ValueTriplet<Species, IdentifierType, String> input = IntegratorUITools.showInputGeneListDialog();
    if (input==null) return; // Cancelled
    
    // Initialize reader
    NameAndSignalReader<mRNA> nsreader = new mRNAReader(0, input.getB(), input.getA());
    
    // Show import dialog and read data
    String[] genes = input.getC().split("\n");
    addTab(new NameAndSignalsTab(this, nsreader, genes, input.getA()), "Custom genelist (" + genes.length + " genes)");
    getStatusBar().showProgress(nsreader.getProgressBar());
    
  }
  
  /**
   * Asks the user for an organism and opens a tab with all known microRNA targets
   * for this organism.
   */
  public void showMicroRNAtargets() {
    ValuePair<miRNAtargets, Species> vp = IntegratorUITools.loadMicroRNAtargets(null);
    if (vp==null || vp.getA()==null) return; // Cancel pressed

    // Add as tab and annotate symbols in background
    NameAndSignalsTab nsTab = new NameAndSignalsTab(this, vp.getA(), vp.getB());
    addTab(nsTab, "miRNA targets (" + vp.getB().getCommonName() + ")");
    nsTab.getActions().showGeneSymbols_InNewThread();
  }
  
  public void openPathwayTab() {
    //Create the translator panel
    KEGGPathwayActionListener kpal = new KEGGPathwayActionListener(null);
    TranslatorPanel pwTab = new TranslatorPanel(Format.JPG, kpal);
    pwTab.addPropertyChangeListener(kpal);
    addTab(pwTab, "Pathway");    
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#createMainComponent()
   */
  @Override
  protected Component createMainComponent() {
    ImageIcon logo = new ImageIcon(IntegratorUI.class.getResource("img/logo.jpg"));
    tabbedPane = new JTabbedLogoPane(logo);
    
    // Change active buttons, based on selection.
    tabbedPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        updateButtons();
      }
    });
    return tabbedPane;
  }
  
  /**
   * Enables and disables buttons in the menu, depending on the current tabbed
   * pane content.
   */
  public void updateButtons() {
    GUITools.setEnabled(false, getJMenuBar(), BaseAction.FILE_SAVE, BaseAction.FILE_CLOSE);
    BaseFrameTab o = getCurrentlySelectedPanel();
    Species currentSpecies = null;
    if (o != null) {
      if (o instanceof TranslatorPanel) {
        
        // Try to get species from graph panel
//        String specKegg = TranslatorTools.getOrganismKeggAbbrFromGraph((Graph2D) ((TranslatorPanel) o).getDocument());
//        if (specKegg!=null) {
//          currentSpecies = Species.search(IntegratorUITools.organisms, specKegg, Species.KEGG_ABBR);
//        }
        currentSpecies = TranslatorTabActions.getSpeciesOfPathway((TranslatorPanel) o, IntegratorUITools.organisms);
        // ----
        
        TranslatorTabActions actions = translatorActionMap.get(o);
        if (actions==null) {
          actions = new TranslatorTabActions((TranslatorPanel)o);
          translatorActionMap.put((TranslatorPanel) o, actions);
        }
        actions.createJToolBarItems(toolBar);
        actions.updateToolbarButtons(toolBar);
      } else if (o instanceof IntegratorTab) {
        currentSpecies = ((IntegratorTab<?>) o).getSpecies(false);
      }
      
      o.updateButtons(getJMenuBar(), getJToolBar());
    } else {
      // Reset toolbar
      createJToolBarForNoTabs(toolBar);
    }
    
    // Display the current species.
    String specLabel = currentSpecies!=null?"Species: " + currentSpecies.getName():"";
    if (speciesLabel!=null) speciesLabel.setText(specLabel);
  }
  
  /**
   * @return the currently selected panel from the
   *         {@link #tabbedPane}, or null if either no or no valid selection
   *         exists.
   */
  private BaseFrameTab getCurrentlySelectedPanel() {
    if ((tabbedPane == null) || (tabbedPane.getSelectedIndex() < 0)) {
      return null;
    }
    Object o = ((JTabbedPane) tabbedPane).getSelectedComponent();
    
    if (o instanceof BaseFrameTab) {
      return (BaseFrameTab)o;
    } else {
      log.severe("Let " +  o.getClass() + " implement BaseFrameTab!");
      return null;
    }
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#exit()
   */
  @Override
  public void exit() {
    // Close the app and save caches.
    setVisible(false);
    try {
      Translator.saveCache();
      ReaderCache.saveIfRequired();
      
      SBProperties props = new SBProperties();
      if (openDir != null && openDir.length() > 1) {
        props.put(GUIOptions.OPEN_DIR, openDir);
      }
      if (saveDir != null && saveDir.length() > 1) {
        props.put(GUIOptions.SAVE_DIR, saveDir);
      }
      if (props.size()>0) {
        SBPreferences.saveProperties(GUIOptions.class, props);
      }
        
    } catch (BackingStoreException exc) {
      exc.printStackTrace();
      // Unimportant error... don't bother the user here.
      // GUITools.showErrorMessage(this, exc);
    }
    dispose();
    System.exit(0);
    
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#getApplicationName()
   */
  @Override
  public String getApplicationName() {
    return appName;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#getCommandLineOptions()
   */
  @SuppressWarnings("unchecked")
  @Override
  public Class<? extends KeyProvider>[] getCommandLineOptions() {
    return getStaticCommandLineOptions().toArray(new Class[0]);
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#getDottedVersionNumber()
   */
  @Override
  public String getDottedVersionNumber() {
    return appVersion;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#getMaximalFileHistorySize()
   */
  @Override
  public short getMaximalFileHistorySize() {
    return 10;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#getURLAboutMessage()
   */
  @Override
  public URL getURLAboutMessage() {
    // TODO Auto-generated method stub
    log.severe("NOT YET IMPLEMENTED!");
    return null;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#getURLLicense()
   */
  @Override
  public URL getURLLicense() {
    // TODO Auto-generated method stub
    log.severe("NOT YET IMPLEMENTED!");
    return null;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#getURLOnlineHelp()
   */
  @Override
  public URL getURLOnlineHelp() {
    // TODO Auto-generated method stub
    log.severe("NOT YET IMPLEMENTED!");
    return null;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#getURLOnlineUpdate()
   */
  @Override
  public URL getURLOnlineUpdate() {
    // TODO Auto-generated method stub
    log.severe("NOT YET IMPLEMENTED!");
    return null;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#openFile(java.io.File[])
   */
  @Override
  protected File[] openFile(File... files) {
    return openFile(files, (Class<?>) null);
  }
  
  protected File[] openFile(File[] files, Class<?>... reader) {

    // Ask input file
    if ((files == null) || (files.length < 1)) {
      files = GUITools.openFileDialog(this, openDir, false, true,
        JFileChooser.FILES_ONLY, (FileFilter[]) null);
    }
    if ((files == null) || (files.length < 1)) return files;
    else {
      // Update openDir.
      if (files[0].getParent().length()>0) {
        openDir = files[0].getParent();
      }
    }
    
    // Ask file format
    if ( (reader == null) || (reader.length < 1) || (reader.length==1 && reader[0]==null)) {
      reader = new Class[1];
      reader[0] = IntegratorUITools.createInputDataTypeChooser();
      if (reader[0]==null) return null; // Cancel pressed
    }
    
    // Read all files and add tabs
    for (int i=0; i<files.length; i++) {
      try {
        // Initialize reader
        Class<?> r = i<reader.length?reader[i]:reader[0];
        NameAndSignalReader<? extends NameAndSignals> nsreader = (NameAndSignalReader<?>) r.newInstance();
        
        // Show import dialog and read data
        addTab(new NameAndSignalsTab(this, nsreader, files[i].getPath()), files[i].getName());
        getStatusBar().showProgress(nsreader.getProgressBar());
        
      } catch (Exception e) {
        GUITools.showErrorMessage(this, e);
        files[i]=null; // Dont add to history.
      }
    }
    
    /* History has to be logged here, beacuse 
     * 1) if this method is invoked by e.g. "openMRNAfile()", it is not logged and
     * 2) the additionalHistory has to be changed too.
     */
    addToFileHistory(files);
    
    return files;
  }
  
  /**
   * Add a new tab to the {@link #tabbedPane}
   * @param tab
   * @param name
   */
  public void addTab(Component tab, String name) {
    addTab(tab,name,null);
  }
  
  /**
   * Add a new tab to the {@link #tabbedPane}
   * @param tab
   * @param name
   * @param toolTip
   */
  public void addTab(Component tab, String name, String toolTip) {
    tab.setName(name);
    tabbedPane.addTab(name, null, tab, toolTip);
    tabbedPane.setSelectedComponent(tab);
  }

  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#saveFile()
   */
  @Override
  public void saveFile() {
    Object o = getCurrentlySelectedPanel();
    if (o==null) return;
    
    if (o instanceof BaseFrameTab) {
      File f = ((BaseFrameTab)o).saveToFile();
      if (f!=null) {
        // Update saveDir.
        saveDir = f.getParent();
      }
      
    } else {
      log.severe("Please implement saveFile for " + o.getClass());
    }
  }

  /**
   * @return the current {@link #tabbedPane}.
   */
  public JTabbedPane getTabbedPane() {
    return tabbedPane;
  }
  
}
