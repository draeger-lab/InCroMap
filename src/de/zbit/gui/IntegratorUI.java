/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.gui;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.gui.prefs.PreferencesPanel;
import de.zbit.integrator.IntegratorIOOptions;
import de.zbit.io.mRNAReader;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.parser.Species;
import de.zbit.util.Utils;
import de.zbit.util.prefs.KeyProvider;
import de.zbit.util.prefs.Option;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;

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
   * This is the main component
   */
  private JTabbedPane tabbedPane;
  
  
  /**
   * Default directory path's for saving and opening files. Only init them
   * once. Other classes should use these variables.
   */
  public static String openDir, saveDir;
  
  /**
   * preferences is holding all project specific preferences
   */
  private SBPreferences prefsIO;
  
  
  public static List<Class<? extends KeyProvider>> getStaticCommandLineOptions() {
    List<Class<? extends KeyProvider>> configList = new LinkedList<Class<? extends KeyProvider>>();
    configList.add(IntegratorIOOptions.class);
    configList.add(GUIOptions.class);
    return configList;
  }
  
  /**
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {
    SBProperties props = SBPreferences.analyzeCommandLineArguments(
      getStaticCommandLineOptions(), args);
    
    // TODO: Test Class as option type:
    // - Command line help text AND F1 help text for readability
    // - Test if submitting as argument works correctly
    // - Test if default value (if no argument) works correctly
    // - Test JLabeledComponent and enhance String (toSimpleName()).
    
    
    //    Class t = Option.getClassFromRange(IntegratorIOOptions.READER, miRNAReader.class.getSimpleName());
    //    System.out.println(t);
    //    System.out.println(IntegratorIOOptions.READER.getRange().isInRange(miRNAReader.class));
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        IntegratorUI ui = new IntegratorUI();
        ui.setVisible(true);
        GUITools.hideSplashScreen();
        ui.toFront();
      }
    });
  }
  
  public IntegratorUI() {
    super();
    
    // init preferences
    initPreferences();
    
    // TODO: ...
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
  }
  
  /**
   * Init preferences, if not already done.
   */
  private void initPreferences() {
    if (prefsIO == null) {
      prefsIO = SBPreferences.getPreferencesFor(IntegratorIOOptions.class);
    }
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#closeFile()
   */
  @Override
  public boolean closeFile() {
    // TODO Auto-generated method stub
    log.severe("NOT YET IMPLEMENTED!");
    return false;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#createJToolBar()
   */
  @Override
  protected JToolBar createJToolBar() {
    final JToolBar r = new JToolBar(appName+" toolbar", JToolBar.HORIZONTAL);
    // TODO: Create toolbar
    return r;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#createMainComponent()
   */
  @Override
  protected Component createMainComponent() {
    // TODO: Make and use a logo
    //ImageIcon logo = new ImageIcon(IntegratorUI.class.getResource("img/Logo2.png"));
    //tabbedPane = new JTabbedLogoPane(logo);
    
    tabbedPane = new JTabbedPane();
    
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
  private void updateButtons() {
    /* TODO: Implement common interface. See todo in getCurrentlySelectedPanel().
    GUITools.setEnabled(false, getJMenuBar(), null);
    Object o = getCurrentlySelectedPanel();
    if (o != null) {
      o.updateButtons(getJMenuBar());
    }*/
  }
  
  /**
   * @return the currently selected TranslatorPanel from the
   *         {@link #tabbedPane}, or null if either no or no valid selection
   *         exists.
   */
  @SuppressWarnings("unused")
  private Object getCurrentlySelectedPanel() {
    // TODO: Implement and return a common interface for all possible tabs.
    // This interface should also include a "updateButtons(getJMenuBar());" Method.
    if ((tabbedPane == null) || (tabbedPane.getSelectedIndex() < 0)) {
      return null;
    }
    Object o = ((JTabbedPane) tabbedPane).getSelectedComponent();
    /*if ((o == null) || !(o instanceof TranslatorPanel)) {
      return null;
    }*/
    return ((Object) o);
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#exit()
   */
  @Override
  public void exit() {
    // Close all tab. If user want's to save a tab first, cancel the closing
    // process.
    /*while (tabbedPane.getTabCount() > 0) {
      if (!closeTab(0)) {
        return;
      }
    }*/
    
    // Close the app and save caches.
    setVisible(false);
    /*try {
      Translator.saveCache();
      
      SBProperties props = new SBProperties();
      File f = getInputFile(toolBar);
      if (f != null && KEGGtranslatorIOOptions.INPUT.getRange().isInRange(f)) {
        props.put(KEGGtranslatorIOOptions.INPUT, f);
      }
      props.put(KEGGtranslatorIOOptions.FORMAT, getOutputFileFormat(toolBar));
      SBPreferences.saveProperties(KEGGtranslatorIOOptions.class, props);
      
      props.clear();
      props.put(GUIOptions.OPEN_DIR, openDir);
      if (saveDir != null && saveDir.length() > 1) {
        props.put(GUIOptions.SAVE_DIR, saveDir);
      }
      SBPreferences.saveProperties(GUIOptions.class, props);
      
    } catch (BackingStoreException exc) {
      exc.printStackTrace();
      // Unimportant error... don't bother the user here.
      // GUITools.showErrorMessage(this, exc);
    }*/
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
    // TODO Auto-generated method stub
    log.severe("NOT YET IMPLEMENTED!");
    
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
    
    // Ask file format
    if ( (reader == null) || (reader.length < 1)) {
      reader = new Class[1];
      JLabeledComponent outputFormat = (JLabeledComponent) PreferencesPanel.getJComponentForOption(IntegratorIOOptions.READER, prefsIO, null);
      outputFormat.setTitle("Please select the input data type");
      JOptionPane.showMessageDialog(this, outputFormat, getApplicationName(), JOptionPane.QUESTION_MESSAGE);
      if (Class.class.isAssignableFrom(outputFormat.getSelectedItem().getClass())) {
        reader[0] = (Class<?>) outputFormat.getSelectedItem();
      } else {
        reader[0] = Option.getClassFromRange(IntegratorIOOptions.READER, outputFormat.getSelectedItem().toString());
      }
    }
    
    //Z:\workspace\Integrator\mRNA_data_new.txt
      // TODO: Create a new ColumnChooser that allows importing FC/pVal or combinations of multiple observations (like ingenuity).
    for (int i=0; i<files.length; i++) {
      JPanel jp = new JPanel(new VerticalLayout());
      jp.add(IntegratorGUITools.getOrganismSelector());
      Class<?> r = i<reader.length?reader[i]:reader[0];
      CSVImporter i = new CSVImporter(jp, f.getPath(), reader[i])
    }
    
    
      for (int i=0; i<files.length; i++) {
        Species species;
        try {
          species = Species.search((List<Species>)Species.loadFromCSV("species.txt"), "mouse", -1);
          // TODO: Create dialog for species, identifierType and ColumnSelector.
          if (mRNAReader.class.isAssignableFrom(reader[i])) {
            mRNAReader r = new mRNAReader(3, IdentifierType.GeneID, species);
            r.addSecondIdentifier(1, IdentifierType.Symbol);
            r.addAdditionalData(0, "probe_name");
            r.addAdditionalData(2, "description");
            r.addSignalColumn(27, SignalType.FoldChange, "Ctnnb1"); // 27-30 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
            r.addSignalColumn(31, SignalType.pValue, "Ctnnb1"); // 31-34 = Cat/Ras/Cat_vs_Ras/Cat_vs_Ras_KONTROLLEN
            
            //r.progress = new ProgressBar(0);
            Collection<mRNA> c = r.read(files[i]);
            ArrayList<mRNA> list = new ArrayList<mRNA>(c);
            TableResultTableModel<mRNA> table = new TableResultTableModel<mRNA>(list);
            tabbedPane.addTab(files[i].getName(), new JTable(table));
          } 
        } catch (Exception e) {
          e.printStackTrace();
        }
            
      }
    
    
    // Translate
    for (File f : files) {
      //createNewTab(f, format);
    }
    return files;
  }
  
  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrame#saveFile()
   */
  @Override
  public void saveFile() {
    // TODO Auto-generated method stub
    log.severe("NOT YET IMPLEMENTED!");
  }
  
}
