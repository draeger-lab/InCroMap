/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/InCroMAP> to
 * obtain the latest version of Integrator.
 *
 * Copyright (C) 2011-2015 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Icon;
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
import de.zbit.data.EnrichmentObject;
import de.zbit.data.HeterogeneousData;
import de.zbit.data.HeterogeneousNS;
import de.zbit.data.NameAndSignals;
import de.zbit.data.PairedNS;
import de.zbit.data.Signal;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.compound.Compound;
import de.zbit.data.genes.GenericGene;
import de.zbit.data.mRNA.mRNA;
import de.zbit.data.methylation.DNAmethylation;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtargets;
import de.zbit.data.protein.ProteinModificationExpression;
import de.zbit.graph.gui.TranslatorPanel;
import de.zbit.gui.actions.NameAndSignalTabActions;
import de.zbit.gui.actions.NameAndSignalTabActions.NSAction;
import de.zbit.gui.actions.listeners.EnrichmentActionListener;
import de.zbit.gui.actions.listeners.EnrichmentActionListener.Enrichments;
import de.zbit.gui.actions.listeners.ExportPathwayData;
import de.zbit.gui.actions.listeners.KEGGPathwayActionListener;
import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.gui.dialogs.IntegratedEnrichmentDialog;
import de.zbit.gui.dialogs.IntegrationDialog;
import de.zbit.gui.layout.LayoutHelper;
import de.zbit.gui.prefs.IntegratorIOOptions;
import de.zbit.gui.prefs.MergeTypeOptions;
import de.zbit.gui.prefs.SignalOptionPanel;
import de.zbit.gui.tabs.IntegratorChartTab;
import de.zbit.gui.tabs.IntegratorTab;
import de.zbit.gui.tabs.NameAndSignalsTab;
import de.zbit.integrator.NameAndSignal2PWTools;
import de.zbit.io.CompoundReader;
import de.zbit.io.DNAmethylationReader;
import de.zbit.io.GenericGeneBasedDataReader;
import de.zbit.io.GenericGeneReader;
import de.zbit.io.NameAndSignalReader;
import de.zbit.io.OpenFile;
import de.zbit.io.ProteinModificationReader;
import de.zbit.io.SNPReader;
import de.zbit.io.SerializableTools;
import de.zbit.io.mRNAReader;
import de.zbit.io.mRNATimeSeriesReader;
import de.zbit.io.miRNAReader;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.kegg.Translator;
import de.zbit.kegg.gui.OrganismSelector;
import de.zbit.kegg.gui.PathwaySelector;
import de.zbit.kegg.gui.TranslatorGraphPanel;
import de.zbit.mapper.GeneID2GeneSymbolMapper;
import de.zbit.mapper.MappingUtils.IdentifierClass;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.mapper.compounds.InChIKey2CompoundNameMapper;
import de.zbit.mapper.compounds.KeggCompound2InChIKeyMapper;
import de.zbit.util.ArrayUtils;
import de.zbit.util.Species;
import de.zbit.util.StringUtil;
import de.zbit.util.objectwrapper.LabeledObject;
import de.zbit.util.objectwrapper.ValuePair;
import de.zbit.util.objectwrapper.ValueTriplet;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.visualization.VisualizeDataInPathway;

/**
 * Various tools that are required mainly for the {@link IntegratorUI}.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
@SuppressWarnings("unchecked")
public class IntegratorUITools {
  public static final transient Logger log = Logger.getLogger(IntegratorUITools.class.getName());
  
  static {
    // Load list of acceptable species
    List<Species> l=null;
//    try {
//      l =(List<Species>) Utils.loadGZippedObject(OpenFile.searchFileAndGetInputStream("species/hmr_species_list.dat"));
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
    l = new ArrayList<Species>(3);
    l.add( new Species("Homo sapiens", "_HUMAN", "Human", "hsa", 9606) );
    l.add( new Species("Mus musculus", "_MOUSE", "Mouse", "mmu", 10090) );
    l.add( new Species("Rattus norvegicus", "_RAT", "Rat", "rno", 10116) );
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
  
  /**
   * @return {@link Species} from current selector or null if none selected
   */
  public static Species getSpeciesFromSelector(OrganismSelector orgSel) {
    String abbr = orgSel==null?null:orgSel.getSelectedOrganismAbbreviation();
    return Species.search(IntegratorUITools.organisms, abbr, Species.KEGG_ABBR);
  }
  
  public static Species showOrganismSelectorDialog(Component parent) {
    JLabeledComponent organismSelector = IntegratorUITools.getOrganismSelector();
    int ret = GUITools.showAsDialog(parent, organismSelector, "Please select your species", true);
    if (ret == JOptionPane.OK_OPTION) {
      return (Species) organismSelector.getSelectedItem();
    }
    return null;
  }
  
  public static JLabeledComponent getIdentifierSelector(IdentifierClass clas) {
    JLabeledComponent l = new JLabeledComponent("Please select an identifier",true,IdentifierType.getAllIdentifers(clas));
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
  	
    return IntegratorUITools.showCSVImportDialog(parent, c, additionalComponent, 800, 400);
  }
  
  
  /**
   * Show the {@link CSVImporterV2} dialog.
   * @param parent parent {@link Frame} or {@link Dialog}
   * @param c {@link CSVImporterV2}
   * @param additionalComponent e.g., speciesSelector from {@link #getOrganismSelector()}
   * @return true if ok has been pressed.
   * @throws IOException 
   */
  public static boolean showCSVImportDialog(Component parent, CSVImporterV2 c, JComponent additionalComponent, int width, int height) throws IOException {
    c.setRenameButtonCaption("Edit observation names");
    c.setPreferredSize(new java.awt.Dimension(width, height));
    
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
//    
//    JMenuItem jm = new JMenuItem("Pathway enrichment");
//    jm.setActionCommand(EnrichmentActionListener.KEGG_ENRICHMENT);
//    append.add(jm);
//    jm.addActionListener(l);
//    
//    jm = new JMenuItem("Gene ontology enrichment");
//    jm.setActionCommand(EnrichmentActionListener.GO_ENRICHMENT);
//    append.add(jm);
//    jm.addActionListener(l);
//    
//    jm = new JMenuItem("MSigDB enrichments");
//    jm.setActionCommand(EnrichmentActionListener.MSIGDB_ENRICHMENT);
//    jm.setToolTipText("<html><body>Perform an enrichment, based on a gene set from "+
//      "<a href=http://www.broadinstitute.org/gsea/>http://www.broadinstitute.org/gsea/</a>"+
//      "</body></html>");
//    append.add(jm);
//    jm.addActionListener(l);
    append.add(GUITools.createJMenuItem(l, Enrichments.KEGG_ENRICHMENT));
    append.add(GUITools.createJMenuItem(l, Enrichments.GO_ENRICHMENT));
    append.add(GUITools.createJMenuItem(l, Enrichments.MSIGDB_ENRICHMENT));
    
    return append;
  }
  
  /**
   * Lets the user choose a {@link NameAndSignalReader} that should be
   * used to read his data.
   * @return any Class, derived from {@link NameAndSignalReader}.
   */
  public static Class<?> createInputDataTypeChooser() {
    return createInputDataTypeChooser(null);
  }
  /**
   * Lets the user choose a {@link NameAndSignalReader} that should be
   * used to read his data.
   * @param file select a reader for the given file.
   * @return any Class, derived from {@link NameAndSignalReader}.
   */
  @SuppressWarnings("rawtypes")
  public static Class<?> createInputDataTypeChooser(File file) {
    
    // Build a custom list with LabeledObject
    Vector<LabeledObject<Class<?>>> itemsForModel = new Vector<LabeledObject<Class<?>>>();
    List<Class> values = IntegratorIOOptions.READER.getRange().getAllAcceptableValues();
    List<String[]> hitwords = new ArrayList<String[]>(values.size());
    for (Class<?> value: values) {
      if (value.equals(mRNAReader.class)) {
        itemsForModel.add(0,new LabeledObject<Class<?>>("messenger RNA", value));
        hitwords.add(0, new String[]{"mrna", "messenger"});
      } else if (value.equals(miRNAReader.class)) {
        itemsForModel.add(Math.min(itemsForModel.size(), 1), new LabeledObject<Class<?>>("micro RNA", value));
        hitwords.add(Math.min(hitwords.size(), 1), new String[]{"mirna", "microrna", "micro rna"});
      } else if (value.equals(ProteinModificationReader.class)) {
        itemsForModel.add(Math.min(itemsForModel.size(), 2), new LabeledObject<Class<?>>("Protein modification data", value));
        hitwords.add(Math.min(hitwords.size(), 2), new String[]{"protein"});
      } else if (value.equals(DNAmethylationReader.class)) {
        itemsForModel.add(itemsForModel.size(), new LabeledObject<Class<?>>("DNA methylation data", value));
        hitwords.add(hitwords.size(), new String[]{"dnam", "methylation"});
      } else if (value.equals(mRNATimeSeriesReader.class)) {
        itemsForModel.add(itemsForModel.size(), new LabeledObject<Class<?>>("mRNA times series data", value));
        hitwords.add(hitwords.size(), new String[]{"timeseries"});
      } else if (value.equals(SNPReader.class)) {
        itemsForModel.add(itemsForModel.size(), new LabeledObject<Class<?>>("SNP or GWAS data", value));
        hitwords.add(hitwords.size(), new String[]{"snp", "gwas"});
      } else if (value.equals(CompoundReader.class)) {
        itemsForModel.add(itemsForModel.size(), new LabeledObject<Class<?>>("Metabolite data", value));
        hitwords.add(hitwords.size(), new String[]{"compound", "metab", "metabolite", "metabolomics", "spectrometry"});
      } else if (value.equals(GenericGeneBasedDataReader.class)) {
        itemsForModel.add(itemsForModel.size(), new LabeledObject<Class<?>>("Generic gene-based data", value));
        hitwords.add(hitwords.size(), new String[]{});
      } else if (value.equals(GenericGeneReader.class)) {
        itemsForModel.add(itemsForModel.size(), new LabeledObject<Class<?>>("Generic region-based data", value));
        hitwords.add(hitwords.size(), new String[]{});
      } else {
        itemsForModel.add(itemsForModel.size(), new LabeledObject<Class<?>>(value.getSimpleName(), value));
        hitwords.add(hitwords.size(), new String[]{value.getSimpleName().replace("Reader", "").toLowerCase()});
      }
    }
    
    // Build the actual component
    JLabeledComponent outputFormat = new JLabeledComponent("Please select the input data type", true, itemsForModel);
    outputFormat.setSortHeaders(false);
    outputFormat.setHeaders(itemsForModel);
    
    // Try to make a reasonable default choice
    if (file!=null) {
      String name = file.getName().toLowerCase();
      for (int i=0; i<itemsForModel.size(); i++) {
        if (StringUtil.containsAny(hitwords.get(i), name)>=0) {
          outputFormat.setSelectedValue(i);
          break;
        }
      }
    }
    
    // Let user choose
    String title = file!=null?String.format("Open '%s'", file.getName()):IntegratorUI.appName;
    int button = JOptionPane.showOptionDialog(IntegratorUI.getInstance(), outputFormat, title,
      JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
    
    // Return chosen class
    if (button!=JOptionPane.OK_OPTION) return null;
    LabeledObject<Class<?>> selected = (LabeledObject<Class<?>>) outputFormat.getSelectedItem();
    return selected.getObject();
  }
  
  /**
   * Creates or appends microRNA target annotation and removal options.
   * @param l an {@link ActionListener} that performs those actions
   * @param append null to create a new {@link JPopupMenu} or any existing
   * menu, for appending to it.
   * @return append
   */
  public static JPopupMenu createMiRNAtargetPopup(ActionListener l, JPopupMenu append) {
    // Eventually create a new PopUpMenu
    if (append==null) {
      append = new JPopupMenu("Targets");
    }
    
    // Annotate targets
    append.add(GUITools.createJMenuItem(l, NSAction.ANNOTATE_TARGETS, UIManager.getIcon("ICON_PENCIL_16")));
    append.add(GUITools.createJMenuItem(l, NSAction.REMOVE_TARGETS, UIManager.getIcon("ICON_TRASH_16")));
    
    return append;
  }
  
  /**
   * Add a "Visualize pathway" {@link JMenuItem} to the given
   * {@link JPopupMenu}.
   * @param l
   * @param append if not null, will append the created {@link JMenuItem} to
   * this menu.
   * @return the just created menu item.
   */
  public static JMenuItem createKeggPathwayPopup(KEGGPathwayActionListener l, JPopupMenu append) {
    JMenuItem showPathway = GUITools.createJMenuItem(l,
        KEGGPathwayActionListener.VISUALIZE_PATHWAY,
        UIManager.getIcon("ICON_PATHWAY_16"));
    
    if (append!=null) {
      append.add(showPathway);
    }
    
    return showPathway;
  }
  
  /**
   * Add a "Export pathway data" {@link JMenuItem} to the given
   * {@link JPopupMenu}.
   * 
   * @param l
   * @param append
   * @return
   */
  public static JMenuItem createExportPathwayDataPathwayPopup(ExportPathwayData l, JPopupMenu append) {
    JMenuItem showPathway = GUITools.createJMenuItem(l,
        ExportPathwayData.PATHWAY_EXPORT,
        UIManager.getIcon("ICON_SAVE_16"));
    
    if (append!=null) {
      append.add(showPathway);
    }
    
    return showPathway;
  }
  
  
  public static JPopupMenu createVisualizeGenomicRegionPopup(NameAndSignalTabActions l, JPopupMenu append) {
    JMenuItem showPathway = GUITools.createJMenuItem(l,
      NameAndSignalTabActions.NSAction.PLOT_REGION,
        UIManager.getIcon("ICON_PENCIL_16"));
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
          GUITools.showErrorMessage(IntegratorUI.getInstance(), t);
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
  	//Gene ID priorities
    if (type.equals(IdentifierType.NCBI_GeneID)) return 0;
    else if (type.equals(IdentifierType.Ensembl)) return 1;
    else if (type.equals(IdentifierType.KeggGenes)) return 1;
    else if (type.equals(IdentifierType.RefSeq)) return 1;
    else if (type.equals(IdentifierType.UniProt)) return 1;
    
    else if (type.equals(IdentifierType.Affymetrix)) return 1;
    else if (type.equals(IdentifierType.Illumina)) return 1;
    else if (type.equals(IdentifierType.Agilent)) return 1;
    
    else if (type.equals(IdentifierType.GeneSymbol)) return 2;
    else if (type.equals(IdentifierType.UnknownGene)) return 3;
    
    
    //Compound ID priorities
    else if (type.equals(IdentifierType.InChIKey)) return 0;
    else if (type.equals(IdentifierType.LIPIDMAPS)) return 1;
    else if (type.equals(IdentifierType.HMDB)) return 1;
    else if (type.equals(IdentifierType.KeggCompound)) return 1;
    else if (type.equals(IdentifierType.CHEBI)) return 1;
    else if (type.equals(IdentifierType.PC_compound)) return 1;
    
    else if (type.equals(IdentifierType.CompoundName)) return 2;
    else if (type.equals(IdentifierType.UnknownCompound)) return 3;
   
    else {
      log.log(Level.SEVERE, "Please implement priority for " + type);
      return 3;
    }
  }

  /**
   * Create a new {@link JLabeledComponent} that lets the user choose a signal from
   * the contained signals in the given <code>ns</code>.
   * @param <T>
   * @param ns any {@link NameAndSignals} with {@link Signal}s.
   * @return
   */
  public static <T extends NameAndSignals> JLabeledComponent createSelectExperimentBox(T ns) {
    JLabeledComponent jc = new JLabeledComponent("Select an observation",true,new String[]{"N/A"});
    return ns==null?jc:createSelectExperimentBox(jc, ns);
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
    if (ns instanceof EnrichmentObject) {
      c = NameAndSignals.getSignalNames(NameAndSignal2PWTools.getSignals(ns));
    }
    // Sort them
    List<ValuePair<String, SignalType>> l;
    if (c instanceof List) {
      l = (List<ValuePair<String, SignalType>>) c;
    } else {
      l = new ArrayList<ValuePair<String,SignalType>>(c);
    }
    Collections.sort(l);
    
    jc.setHeaders(l);
    return jc;
  }
  
  /**
   * Replace the signals in the existing experiment box by the (filtered) given one.
   * @param <T>
   * @param jc
   * @param ns
   * @param onlyIncludeThisType only takes signals from this {@link SignalType}.
   * @return
   */
  public static <T extends NameAndSignals> JLabeledComponent createSelectExperimentBox(JLabeledComponent jc, T ns, SignalType onlyIncludeThisType) {
    List<ValuePair<String, SignalType>> list = new ArrayList<ValuePair<String, SignalType>>(ns.getSignalNames());
    for (int i=0; i<list.size(); i++) {
      if (!list.get(i).getB().equals(onlyIncludeThisType)) {
        list.remove(i);
        i--;
      }
    }
    jc.setHeaders(list);
    return jc;
  }
  
  /**
   * Shows a signal selection box.
   * <p>Note: {@link DNAmethylation} is filtered for p-values only!</p>
   * @param <T>
   * @return ValueTriplet of (TabIndex In {@link IntegratorUI#getTabbedPane()}, ExperimentName, {@link SignalType}) or null.  
   */
  public static <T extends NameAndSignals> ValueTriplet<NameAndSignalsTab, String, SignalType> showSelectExperimentBox() {
    return showSelectExperimentBox( null);
  }
  /**
   * Shows a signal selection box.
   * <p>Note: {@link DNAmethylation} is filtered for p-values only!</p>
   * @param <T>
   * @param initialSelection
   * @return ValueTriplet of (TabIndex In {@link IntegratorUI#getTabbedPane()}, ExperimentName, {@link SignalType}) or null.
   */
  @SuppressWarnings("rawtypes")
  public static <T extends NameAndSignals> ValueTriplet<NameAndSignalsTab, String, SignalType> showSelectExperimentBox(IntegratorTab initialSelection) {
    return showSelectExperimentBox(initialSelection, null, (Species)null);
  }
  

  /**
   * @return a list with tab names and actual tabs for every {@link NameAndSignalsTab} that contains
   * {@link NameAndSignals} objects with signals.
   */
  public static List<LabeledObject<NameAndSignalsTab>> getNameAndSignalTabsWithSignals() {
    return getNameAndSignalTabsWithSignals(null, (Class<? extends NameAndSignals>)null);
  }
  
  /**
   * Create a filtered List of available {@link NameAndSignalsTab}s, that contain {@link Signal}s.
   * @param species if not null, only tabs for that species will be returned.
   * @param onlyDataTypes if not null, only tabs that contain on of these
   * data types will be returned.
   * @return
   */
  public static List<LabeledObject<NameAndSignalsTab>> getNameAndSignalTabsWithSignals(Species species, Class<? extends NameAndSignals>... onlyDataTypes) {
    return getNameAndSignalTabs(species, true, onlyDataTypes);
  }
  
  /**
   * Create a filtered List of available {@link NameAndSignalsTab}s, that match the given restrictions
   * @param species if not null, only tabs for that species will be returned.
   * @param onlyWithSignals if true, only returns tabs, that contain {@link NameAndSignals} with {@link Signal}s-
   * @param onlyDataTypes if not null, only tabs that contain on of these
   * data types will be returned.
   * @return
   */
  public static List<LabeledObject<NameAndSignalsTab>> getNameAndSignalTabs(Species species, boolean onlyWithSignals, Class<? extends NameAndSignals>... onlyDataTypes) {
    if (onlyDataTypes!=null && ((onlyDataTypes.length==1 && onlyDataTypes[0]==null) || onlyDataTypes.length==0 )) onlyDataTypes = null;
    IntegratorUI ui = IntegratorUI.getInstance();
    List<LabeledObject<NameAndSignalsTab>> datasets = new LinkedList<LabeledObject<NameAndSignalsTab>>();
    boolean containsEnrichment = ArrayUtils.contains(onlyDataTypes, EnrichmentObject.class);
    for (int i=0; i<ui.getTabbedPane().getTabCount(); i++) {
      Component c = ui.getTabbedPane().getComponentAt(i);
      if (c instanceof NameAndSignalsTab) {
        // Filter
        if (species!=null) {
          Species spec2 = ((NameAndSignalsTab)c).getSpecies(false);
          if (spec2==null || !species.equals(spec2)) continue;
        }
        if (onlyDataTypes!=null && !ArrayUtils.contains(onlyDataTypes, ((NameAndSignalsTab)c).getDataContentType())) continue;
        
        //Class<?> cl = ((NameAndSignalsTab)c).getDataContentType(); 
        //if (cl.equals(mRNA.class) || cl.equals(miRNA.class)) {
        if ((((NameAndSignalsTab)c).getSourceTab()==null || containsEnrichment) && // Data has not been derived, but read from disk!
            ((NameAndSignals)((NameAndSignalsTab)c).getExampleData())!=null && // is not currently reading data
            (!onlyWithSignals || (onlyWithSignals && ((NameAndSignals)((NameAndSignalsTab)c).getExampleData()).hasSignals()))) {
          String title = ui.getTabbedPane().getTitleAt(i);
          // Unfortunately is misleading for integrated enrichments
//          if (((NameAndSignalsTab)c).getDataContentType().equals(EnrichmentObject.class)) {
//            // For enrichment-tabs, create label like "Pathway enrichment of 'mRNA-tab1'".
//            if (((NameAndSignalsTab) c).getSourceTab()!=null) {
//              String otherTitle = ((IntegratorTab)((NameAndSignalsTab) c).getSourceTab()).getTabName();
//              title = String.format("%s of '%s'", title, otherTitle);
//            }
//          }
          datasets.add(new LabeledObject<NameAndSignalsTab>(
              title, (NameAndSignalsTab) c));
        }
      }
    }
    return datasets;
  }
  
  /**
   * Remove all {@link NameAndSignalsTab}s from the given list, that do not
   * contain signals of a specific Type.
   * <p>This is especially helpful if you want the user to select any
   * e.g. methylation data set with p-values.
   * @param toFilter
   * @param nsType only filter tabs of this type
   * @param onlyIncludeTabsWithThisType remove all tabs of type <code>nsType</code>
   * that contain <b>no</b> {@link Signal} of {@link SignalType} <code>onlyIncludeTabsWithThisType</code>.
   */
  public static void filterNSTabs(List<LabeledObject<NameAndSignalsTab>> toFilter, Class<DNAmethylation> nsType, SignalType onlyIncludeTabsWithThisType) {
    for (int i=0; i<toFilter.size(); i++) {
      LabeledObject<NameAndSignalsTab> tab = toFilter.get(i);
      if (!tab.getObject().getDataContentType().equals(nsType)) continue;
      
      List<Signal> signals = ((NameAndSignals)tab.getObject().getExampleData()).getSignals();
      boolean found = false;
      if (signals!=null) {
        for (Signal sig: signals) {
          if (sig.getType().equals(onlyIncludeTabsWithThisType)) {
            found = true;
            break;
          }
        }
      }
      if (!found) {
        toFilter.remove(i);
        i--;
      }
    }
  }
  
  /**
   * @return all opened pathway tabs ({@link TranslatorPanel}s).
   */
  public static List<LabeledObject<TranslatorGraphPanel>> getTranslatorTabs() {
    IntegratorUI ui = IntegratorUI.getInstance();
    List<LabeledObject<TranslatorGraphPanel>> datasets = new LinkedList<LabeledObject<TranslatorGraphPanel>>();
    for (int i=0; i<ui.getTabbedPane().getTabCount(); i++) {
      Component c = ui.getTabbedPane().getComponentAt(i);
      if (c instanceof TranslatorPanel) {
        // Create a nicer label
        String name = String.format("%s (tab:\"%s\")", ((TranslatorGraphPanel) c).getTitle(), ui.getTabbedPane().getTitleAt(i));
        datasets.add(new LabeledObject<TranslatorGraphPanel>(
            name, (TranslatorGraphPanel) c));
      }
    }
    return datasets;
  }
  
  public static List<LabeledObject<IntegratorTab<?>>> getNameAndSignalTabs(boolean excludeCurrentlySelected, 
    Collection<Class<?>> excludeDatatypes, Collection<Class<?>> includeDatatypes) {
    
    ArrayList<Class<?>> ns = new ArrayList<Class<?>>(1);
    ns.add(NameAndSignalsTab.class);
    if (excludeDatatypes==null) excludeDatatypes = new ArrayList<Class<?>>();
    excludeDatatypes.add(Object.class);
    
    return getTabs(excludeCurrentlySelected, ns, excludeDatatypes, includeDatatypes);
  }
  
  /**
   * Get defined tabs from the current {@link IntegratorUI#instance}.
   * @param excludeCurrentlySelected if true, excludes the currently selected tab.
   * @param filterForTabType only include tabs from the given classes (the class of
   * the tab component is compared to this list), if null, all <code>IntegratorTab</code>
   * tabs are included.
   * @param excludeDatatypes the {@link IntegratorTab#getDataContentType()} is compared and
   * if it is contained in this list, this tab is excluded. If null, all are included.
   * @param includeDatatypes the {@link IntegratorTab#getDataContentType()} is compared and
   * only if it is in this list, it is included, if null, all tabs are included.
   * @return list with tab names and actual tabs.
   */
  public static List<LabeledObject<IntegratorTab<?>>> getTabs(boolean excludeCurrentlySelected, Collection<Class<?>> filterForTabType, 
    Collection<Class<?>> excludeDatatypes, Collection<Class<?>> includeDatatypes) {
    
    IntegratorUI ui = IntegratorUI.getInstance();
    List<LabeledObject<IntegratorTab<?>>> datasets = new LinkedList<LabeledObject<IntegratorTab<?>>>();
    for (int i=0; i<ui.getTabbedPane().getTabCount(); i++) {
      Component c = ui.getTabbedPane().getComponentAt(i);
      if (excludeCurrentlySelected && ui.getTabbedPane().getSelectedIndex()==i) continue;
      if (c instanceof IntegratorTab<?>) {
        if (filterForTabType==null || filterForTabType.contains(c.getClass())) {
          // Reading / processing in progress. Tab is not ready!
          if (((IntegratorTab<?>)c).getExampleData()==null) continue;
          Class<?> dt = ((IntegratorTab<?>)c).getDataContentType();
          if (excludeDatatypes!=null && excludeDatatypes.contains(dt)) continue;
          if (includeDatatypes==null || includeDatatypes.contains(dt)) {
            datasets.add(new LabeledObject<IntegratorTab<?>>(
                ui.getTabbedPane().getTitleAt(i), (IntegratorTab<?>) c));
          }
        }
      }
    }
    return datasets;
  }
  
  /**
   * Shows a signal selection box.
   *
   * @param <T>
   * @param ui
   * @param initialSelection
   * @param dialogTitle
   * @return ValueTriplet of (TabIndex In {@link IntegratorUI#getTabbedPane()}, ExperimentName, {@link SignalType}) or null.
   */
  @SuppressWarnings("rawtypes")
  public static <T extends NameAndSignals> ValueTriplet<NameAndSignalsTab, String, SignalType> showSelectExperimentBox(
    IntegratorTab initialSelection, String dialogTitle, Species filterForSpecies) {
    // Create a list of available datasets
    List<LabeledObject<NameAndSignalsTab>> datasets = getNameAndSignalTabsWithSignals(filterForSpecies);
    
    if (datasets==null || datasets.size()<1) {
      GUITools.showMessage(String.format("Could not find any %s datasets with observations.", 
        filterForSpecies==null?"input":filterForSpecies.getName()), IntegratorUI.appName);
      return null;
    }
    
    // Remove all DNA methylation datasets that contain no p-value signals
    //IntegratorUITools.filterNSTabs(datasets, DNAmethylation.class, SignalType.pValue);//Keyword: DNAm-pValue
    
    return showSelectExperimentBox(initialSelection, dialogTitle, datasets);
  }
  /**
   * Shows a signal selection box.
   * 
   * @param <T>
   * @param initialSelection
   * @param dialogTitle
   * @param datasets datasets to show in selection box
   * @return ValueTriplet of (TabIndex In {@link IntegratorUI#getTabbedPane()}, ExperimentName, {@link SignalType}) or null.
   */
  @SuppressWarnings("rawtypes")
  public static <T extends NameAndSignals> ValueTriplet<NameAndSignalsTab, String, SignalType> showSelectExperimentBox(
    IntegratorTab initialSelection, String dialogTitle, List<LabeledObject<NameAndSignalsTab>> datasets) {
    IntegratorUI ui = IntegratorUI.getInstance();
    final JPanel jp = new JPanel(new BorderLayout());
    int initialSelIdx=0;
    
    // Get initial selection.
    if (initialSelection!=null) {
      for (int i=0; i<datasets.size(); i++) {
        Component c = datasets.get(i).getObject();
        if (c.equals(initialSelection)) {
          initialSelIdx=i;
          break;
        }
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
      NameAndSignals ns = (NameAndSignals)((NameAndSignalsTab)datasets.get(initialSelIdx).getObject()).getExampleData();
      final JLabeledComponent selExpBox = createSelectExperimentBox(ns);
      IntegratorUITools.modifyExperimentBoxForDNAMethylation(selExpBox, ns);// <- E.g. only show p-values for DNA methylation data
      jp.add(selExpBox, BorderLayout.SOUTH);
      dataSelect.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          NameAndSignalsTab tab = (NameAndSignalsTab) ((LabeledObject)dataSelect.getSelectedItem()).getObject();
          NameAndSignals ns = (NameAndSignals)tab.getExampleData();
          createSelectExperimentBox(selExpBox, ns);
          IntegratorUITools.modifyExperimentBoxForDNAMethylation(selExpBox, ns);// <- E.g. only show p-values for DNA methylation data
        }
      });
      
      // Show and evaluate dialog
      if (dialogTitle==null) dialogTitle = UIManager.getString("OptionPane.titleText");
      int ret = JOptionPane2.showConfirmDialogResizable(ui, jp, dialogTitle, JOptionPane.OK_CANCEL_OPTION);
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
   * Creates a box, that lets the user choose one EXISTING pathway tab.
   * @param ui
   * @return
   */
  public static JLabeledComponent createSelectPathwayTabBox(IntegratorUI ui) {
    
    // Create a list of available datasets and get initial selection.
    List<LabeledObject<TranslatorGraphPanel>> datasets = getTranslatorTabs();
    if (datasets.size()<1) {
      return null;
    } else {
      final JLabeledComponent dataSelect = new JLabeledComponent("Select a tab",true,datasets);
      return dataSelect;
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
    JLabeledComponent identifier = getIdentifierSelector(IdentifierClass.Gene);
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
          IntegratorUI.openDir = file.getParent();
          
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
    int ret = JOptionPane2.showConfirmDialogResizable(IntegratorUI.getInstance(), p, "Please enter a list of genes", JOptionPane.OK_CANCEL_OPTION);
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
    if (species==null) return null;
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
   * Returns a 2CompoundName mapping.
   * <p>Every created instance is cached for later usage.
   * @return 
   */
  public static InChIKey2CompoundNameMapper get2CompoundNameMapping() {
    String key = InChIKey2CompoundNameMapper.class.getSimpleName();
    Object mapper = UIManager.get(key);
    if (mapper==null) {
      try {
        mapper = new InChIKey2CompoundNameMapper();
      } catch (IOException e) {
        GUITools.showErrorMessage(IntegratorUI.getInstance(), e);
      }
      if (mapper!=null) {
        UIManager.put(key, mapper);
      }
    }
    return (InChIKey2CompoundNameMapper) mapper;
  }
  
  /**
   * Returns a KeggCompound2InChIKey mapping.
   * <p>Every created instance is cached for later usage.
   * @return 
   */
  public static KeggCompound2InChIKeyMapper getKegg2InChIKeyMapping() {
    // ZU speicherlastig, daher kein Caching hierf�r.
  	/*String key = KeggCompound2InChIKeyMapper.class.getSimpleName();
    Object mapper = UIManager.get(key);
    if (mapper==null) {
      try {
        mapper = new KeggCompound2InChIKeyMapper();
      } catch (IOException e) {
        GUITools.showErrorMessage(IntegratorUI.getInstance(), e);
      }
      if (mapper!=null) {
        UIManager.put(key, mapper);
      }
    }*/
  	KeggCompound2InChIKeyMapper mapper = null;
  	try {
      mapper = new KeggCompound2InChIKeyMapper();
    } catch (IOException e) {
      GUITools.showErrorMessage(IntegratorUI.getInstance(), e);
    }
    return mapper;
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
    
    // For rat, no Diana, Targetscan and no TarBase is available
    boolean isRat = (species!=null && species.getCommonName().equalsIgnoreCase("Rat"));
    
    // Create only experimental button
    final JCheckBox onlyExperimental = new JCheckBox("Only experimentally validated targets");
    onlyExperimental.setSelected(false);
    lh.add(onlyExperimental);
    
    // One button per data source
    JPanel dbs = new JPanel(new GridLayout(0,1));
    final JCheckBox miRecords = new JCheckBox("miRecords v3", true);
    final JCheckBox miRTarBase = new JCheckBox("miRTarBase 2.4", true);
    final JCheckBox tarBase = new JCheckBox("TarBase V5.0c", true);
    
    final JCheckBox DIANA = new JCheckBox("DIANA - microT v4.0", false);
    final JCheckBox ElMMo = new JCheckBox("ElMMo v5", false);
    final JCheckBox TargetScan = new JCheckBox("TargetScan v5.2", false);
    DIANA.setToolTipText("Predicted \"" + DIANA.getText() + "\" targets. Only high confidence targets are included.");
    ElMMo.setToolTipText("Predicted \"" + ElMMo.getText() + "\" targets. Only high confidence targets are included.");
    TargetScan.setToolTipText("Predicted \"" + TargetScan.getText() + "\" targets. Only high confidence targets are included.");
    
    dbs.add(miRecords); dbs.add(miRTarBase); if (!isRat) dbs.add(tarBase);
    dbs.add(ElMMo); if (!isRat) {dbs.add(DIANA);  dbs.add(TargetScan);}
    dbs.setBorder(BorderFactory.createTitledBorder("Select databases to load"));
    lh.add(dbs);
    
    // Enable and disable predictions on only-experimental click
    if (isRat) {
      tarBase.setSelected(false); DIANA.setSelected(false); TargetScan.setSelected(false);
    }
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
    onlyExperimental.setSelected(true);
    
    // Ask user
    int ret = JOptionPane2.showConfirmDialogResizable(IntegratorUI.getInstance(), p, 
        "Please select microRNA targets to load.", JOptionPane.OK_CANCEL_OPTION);
    if (ret==JOptionPane.OK_OPTION) {
      if (orgSel!=null) species = (Species) orgSel.getSelectedItem();
      log.info("Loading microRNA target file for " + species + ".");
      
      boolean isExperimentalSelected = miRecords.isSelected()||miRTarBase.isSelected()||tarBase.isSelected();
      boolean isPredictedSelected = !onlyExperimental.isSelected() && (DIANA.isSelected()||ElMMo.isSelected()||TargetScan.isSelected());
      
      // Load targets
      miRNAtargets t_all=null;
      // XXX: Show loading message here.
      try {
        if (isExperimentalSelected) {
          log.fine("Loading experimental miRNA targets");
          t_all = (miRNAtargets) SerializableTools.loadGZippedObject(
            OpenFile.searchFileAndGetInputStream("miRNA_targets/" + species.getNCBITaxonID() + ".dat"));
        } if (isPredictedSelected) {
          log.fine("Loading predicted miRNA targets");
          miRNAtargets t = (miRNAtargets) SerializableTools.loadGZippedObject(
            OpenFile.searchFileAndGetInputStream("miRNA_targets/" + species.getNCBITaxonID() + "_HC.dat"));
          if (t_all==null) t_all = t; else t_all.addAll(t);
        }
        if (t_all==null) throw new IOException("Could not read miRNA target file or no targets have been selected.");
      } catch (IOException e) {
        GUITools.showErrorMessage(IntegratorUI.getInstance(), e);
        return null;
      }
      
      // Filter targets
      if (isPredictedSelected && onlyExperimental.isSelected()) t_all.filterTargetsOnlyExperimental();
      if (isPredictedSelected && !DIANA.isSelected()) t_all.removeTargetsFrom("DIANA");
      if (isPredictedSelected && !TargetScan.isSelected()) t_all.removeTargetsFrom("TargetScan");
      if (isPredictedSelected && !ElMMo.isSelected()) t_all.removeTargetsFrom("ElMMo");
      if (isExperimentalSelected && !miRecords.isSelected()) t_all.removeTargetsFrom("miRecords");
      if (isExperimentalSelected && !miRTarBase.isSelected()) t_all.removeTargetsFrom("miRTarBase");
      if (isExperimentalSelected && !tarBase.isSelected()) t_all.removeTargetsFrom("TarBase");

      
      log.info(StatusBar.defaultText);
      return new ValuePair<miRNAtargets, Species>(t_all, species);
    }
    return null;
  }

  /**
   * Lets the user choose pathways and observations and a output format. Batch creates a 
   * picture for every pathway and observation.
   */
  @SuppressWarnings("rawtypes")
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
    final JList pathways = new JList<>(new String[]{"Please wait, loading list of pathways."});
    pathways.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    pathways.setEnabled(false);
    
    // Allow writing and restoring a pathway selection
    final Preferences prefs = Preferences.userRoot().node(IntegratorUITools.class.getName());
    final String selectionKey = "LAST_BATCH_SELECTION";
    
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
          
          // Try to restore some previous selection
          String last = prefs.get(selectionKey, null);
          if (last!=null &&last.length()>0) {
            String[] sel = last.split(Pattern.quote("|"));
            List<Integer> selIndices = new ArrayList<Integer>();
            for (String s: sel ){
              int pos = Collections.binarySearch(pwName, new LabeledObject<String>(s, null));
              if (pos>=0) selIndices.add(pos);
            }
            if (selIndices.size()>0) {
              pathways.setSelectedIndices(ArrayUtils.toIntArray(selIndices.toArray(new Integer[0])));
            }
          }
          
        }
        
        pathways.setEnabled(pwName.size()>0);
        GUITools.packParentWindow(pathways);
      }
      
    };
    loadPathways.start();
    
    // Output Format
    final JComboBox fileFormat = new JComboBox(TranslatorGraphPanel.getGraphMLfilefilter().toArray());
    fileFormat.setBorder(BorderFactory.createTitledBorder("Select output file format"));
    lh.add(fileFormat);
    
    
    // Ask user
    int ret = JOptionPane2.showConfirmDialogResizable(IntegratorUI.getInstance(), p, 
      "Please select the data to visualize", JOptionPane.OK_CANCEL_OPTION);
    if (ret==JOptionPane.OK_OPTION) {
      if (pathways.isEnabled() && pathways.getSelectedIndices().length>0 &&
          experiments.getSelectedIndices().length>0 && fileFormat.getSelectedIndex()>=0) {
        
        // Store pathway selection
        Runnable store = new Runnable() {
          @Override
          public void run() {
            try {
              StringBuilder b = new StringBuilder();
              for (int i: pathways.getSelectedIndices()) {
                if (b.length()>0) b.append('|');
                b.append(pwName.get(i).getLabel());
              }
              prefs.put(selectionKey, b.toString());
              prefs.flush();
            } catch (Exception e) {
              log.log(Level.WARNING,"Could not store custom selection prefs.",e);
            }
          }
        };
        new Thread(store).start();
        //----
        
        // Get output directory
        File outputDir = GUITools.saveFileDialog(IntegratorUI.getInstance(), IntegratorUI.saveDir, false, false, true, 
          JFileChooser.DIRECTORIES_ONLY, (FileFilter[])null);
        if (outputDir!=null) {
          IntegratorUI.saveDir = outputDir.getPath();
          
          // Create result arrays
          ValuePair<?, ?>[] exps = new ValuePair<?, ?>[experiments.getSelectedIndices().length];
          for (int i=0; i<exps.length; i++)
            exps[i] = labeledSignals.get(experiments.getSelectedIndices()[i]).getObject();
          
          String[] refPWids = new String[pathways.getSelectedIndices().length];
          for (int i=0; i<refPWids.length; i++)
            refPWids[i] = pwName.get(pathways.getSelectedIndices()[i]).getObject();
          
          VisualizeDataInPathway.batchCreatePictures(
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

  /**
   * @return the user-approved {@link MergeType}.
   */
  public static MergeType getMergeType() {
    return getMergeType((SignalType)null);
  }
  /**
   * @param forAutoInference if automatic mergeType is selected an
   * appropriate {@link MergeType} is selected automatically,
   * dependent on this {@link SignalType}.
   * @return the user-approved {@link MergeType}. This is either 
   * a valid (directly usable) {@link MergeType} OR {@link MergeType#Automatic}.
   */
  public static MergeType getMergeType(SignalType forAutoInference) {
    MergeType m = MergeTypeOptions.GENE_CENTER_SIGNALS_BY.getDefaultValue();
    
    // Look if "remember my decision" is set and take it without asking
    SBPreferences prefs = SBPreferences.getPreferencesFor(MergeTypeOptions.class);
    if (MergeTypeOptions.REMEMBER_GENE_CENTER_DECISION.getValue(prefs)) {
      try {
        m = MergeTypeOptions.GENE_CENTER_SIGNALS_BY.getValue(prefs);
        if (m.equals(MergeType.Automatic)) m = autoInferMergeType(forAutoInference);
        if (!m.equals(MergeType.AskUser)) return m;
      } catch (Throwable t) {}
    }
    
    // Show asking dialog and force user to press ok!
    try {
      int ok = JOptionPane.CANCEL_OPTION;
      SignalOptionPanel sop = new SignalOptionPanel();
      while (ok != JOptionPane.OK_OPTION ||
          MergeTypeOptions.GENE_CENTER_SIGNALS_BY.getValue(prefs).equals(MergeType.AskUser)) {
        ok = JOptionPane2.showConfirmDialogResizable(IntegratorUI.getInstance(), sop, 
          "Please choose how to merge multiple probes", JOptionPane.OK_CANCEL_OPTION);
      }
      sop.persist();
      m = MergeTypeOptions.GENE_CENTER_SIGNALS_BY.getValue(prefs);
    } catch (Exception e) {
      GUITools.showErrorMessage(IntegratorUI.getInstance(), e);
    }
    
    // Ensure a valid return value
    if (m.equals(MergeType.AskUser) || m.equals(MergeType.Automatic)) {
      boolean wasAskUser = m.equals(MergeType.AskUser);
      if (forAutoInference==null) {
        m = MergeType.Automatic; 
      } else {
        // Auto-inference of adequate merge type
        m = autoInferMergeType(forAutoInference);
      }
      if (wasAskUser) {
        log.warning(String.format("For some reason, MergeType was still AskUser. Changed to %s.", m.toString()));
      }
    }
    
    return m;
  }

  /**
   * This method should NOT be preferred to {@link #getMergeType()}. It returns the
   * MergeType, stored currently in the settings and does NEVER ask the user.
   * <p>This method should be used, if the merged signals are not important, i.e.
   * the whole {@link MergeType} is somehow unimportant.
   * @param prefs {@link SBPreferences} to the the current value from
   * @return
   */
  public static MergeType getMergeTypeSilent(SBPreferences prefs) {
    return getMergeTypeSilent(prefs, (SignalType)null);
  }
  /**
   * Please see {@link #getMergeTypeSilent(SBPreferences)}
   * @param prefs 
   * @param forAutoInference if {@link MergeType#Automatic} is selected,
   * returns a value dependent on the {@link SignalType}.
   * @return Valid (directly usable) {@link MergeType} OR
   * {@link MergeType#Automatic}.
   * @see #getMergeTypeSilent(SBPreferences)
   */
  public static MergeType getMergeTypeSilent(SBPreferences prefs, SignalType forAutoInference) {
    MergeType m = MergeTypeOptions.GENE_CENTER_SIGNALS_BY.getDefaultValue();
    
    try {
      m = MergeTypeOptions.GENE_CENTER_SIGNALS_BY.getValue(prefs);
    } catch (Throwable t) {}
    
    // Silent => Auto-infer a valid merge type
    if (m.equals(MergeType.AskUser) || m.equals(MergeType.Automatic)) {
      if (forAutoInference==null) {
        m = MergeType.Automatic; 
      } else {
        // Auto-inference of adequate merge type
        m = autoInferMergeType(forAutoInference);
      }
    }
    
    return m;
  }

  /**
   * Return a good {@link MergeType}, dependent on a {@link SignalType}.
   * This will return MaxDistanceToZero for FoldChanges and
   * Minimum for pValues. Else, always Mean is returned.
   * @param forAutoInference
   * @return
   */
  public static MergeType autoInferMergeType(SignalType forAutoInference) {
    MergeType m;
    if (forAutoInference == null) {
      m = MergeType.Mean;
    } else if (forAutoInference.equals(SignalType.FoldChange)) {
      m = MergeType.MaximumDistanceToZero;
    } else if (forAutoInference.equals(SignalType.pValue)) {
      m = MergeType.Minimum;
    } else {
      m = MergeType.Mean;
    }
    return m;
  }
  
  /**
   * Please see {@link #getMergeTypeSilent()}.
   * @param forAutoInference if {@link MergeType#Automatic} is selected,
   * returns a value dependent on the {@link SignalType}.
   * @return
   * @see #getMergeTypeSilent()
   */
  public static MergeType getMergeTypeSilent(SignalType forAutoInference) {
    SBPreferences prefs = SBPreferences.getPreferencesFor(MergeTypeOptions.class);
    return getMergeTypeSilent(prefs, forAutoInference);
  }
  
  /**
   * This method should NOT be preferred to {@link #getMergeType()}. It returns the
   * MergeType, stored currently in the settings and does NEVER ask the user.
   * <p>This method should be used, if the merged signals are not important, i.e.
   * the whole {@link MergeType} is somehow unimportant.
   * @return
   */
  public static MergeType getMergeTypeSilent() {
    return getMergeTypeSilent((SignalType)null);
  }
  
  /**
   * Shows a dialog that lets the user chooser a Pathway
   * and one dataset per data type.
   */
  public static void showIntegratedVisualizationDialog() {
    IntegrationDialog.showAndEvaluateIntegratedVisualizationDialog();
  }
  
  /**
   * Shows a dialog and creates a TreeTable with one row/node per Gene
   * and subnodes for each datatype (mRNA, miRNA, etc.) and subNodes
   * with each probe.
   */
  public static void showIntegratedTreeTableDialog() {
    IntegrationDialog.showAndEvaluateIntegratedTreeTableDialog();
  }
  
  /**
   * Shows a dialog that let's the user choose all optiions for an
   * integrated enrichment and adds a new tab to the current IntegratorUI. 
   */
  public static void showIntegratedEnrichmentDialog() {
    IntegratedEnrichmentDialog.showAndEvaluateIntegratedEnrichmentDialog();
  }

  /**
   * @param tab
   * @return
   */
  public static Icon inferIconForTab(Component tab) {
    if (tab instanceof TranslatorPanel) {
      return UIManager.getIcon("ICON_PATHWAY_16");
      
    } else if (tab instanceof IntegratorChartTab) {
      return UIManager.getIcon("ICON_PENCIL_16");
      
    } else if (tab instanceof IntegratorTab &&
        ((IntegratorTab<?>)tab).isReady() ) {
      Class<?> type = ((IntegratorTab<?>) tab).getDataContentType();
      if (mRNA.class.isAssignableFrom(type) || 
          GenericGene.class.isAssignableFrom(type)) {
        return UIManager.getIcon("ICON_MRNA_16");
      } else if (miRNA.class.isAssignableFrom(type)) {
        return UIManager.getIcon("ICON_MIRNA_16");
      } else if (ProteinModificationExpression.class.isAssignableFrom(type)) {
        return UIManager.getIcon("ICON_PROTEIN_16");
      } else if (DNAmethylation.class.isAssignableFrom(type)) {
        return UIManager.getIcon("ICON_DNAM_16");
      } else if (Compound.class.isAssignableFrom(type)) {
        return UIManager.getIcon("ICON_COMPOUND_16");
        
      } else if (EnrichmentObject.class.isAssignableFrom(type)) {
        return UIManager.getIcon("ICON_GEAR_16");
        
      } else if (PairedNS.class.isAssignableFrom(type) ||
          HeterogeneousNS.class.isAssignableFrom(type) ||
          HeterogeneousData.class.isAssignableFrom(type)) {
        return UIManager.getIcon("IntegratorIcon_16_straight");
      }
    }
    // No icon in doubt.
    return null;
  }

  /**
   * Only includes p-value signals if <code>ns</code> is
   * of type {@link DNAmethylation}.
   * <p>Please check <code>expSel</code> afterwards, as it
   * now might be empty (i.e. contain no signals!).
   * @param expSel
   * @param ns
   */
  @SuppressWarnings("unused")
  public static void modifyExperimentBoxForDNAMethylation(
    JLabeledComponent expSel, NameAndSignals ns) {
    if (true) return; //Keyword: DNAm-pValue
    // Only show p-values for DNA methylation data
    if (ns instanceof DNAmethylation) IntegratorUITools.createSelectExperimentBox(expSel,ns,SignalType.pValue);
    //if (expSel.getHeaders()==null || expSel.getHeaders().length<1) expSel = null;
  }

  /**
   * Let's the user choose a pathway-enrichment tab and p-value. 
   * @param spec
   * @param dialogTitle
   * @return ValueTriplet of (TabIndex In {@link IntegratorUI#getTabbedPane()}, ExperimentName, {@link SignalType}) or null.
   */
  public static ValueTriplet<NameAndSignalsTab, String, SignalType> showSelectPathwayEnrichmentBox(Species spec, String dialogTitle) {
    List<LabeledObject<NameAndSignalsTab>> tabs = IntegratorUITools.getNameAndSignalTabsWithSignals(spec, EnrichmentObject.class);
    Iterator<LabeledObject<NameAndSignalsTab>> it = tabs.iterator();
    while (it.hasNext()) {
      try {
        if (!((EnrichmentObject<?>)it.next().getObject().getExampleData()).isKEGGenrichment()) {
          it.remove();
        }
      } catch (Exception e ){} // not important
    }
    if (tabs==null || tabs.size()<1) {
      GUITools.showMessage("Could not find any pathway enrichment tabs" + 
        (spec==null?"":(" for "+spec.getName())) + ". Please perform a pathway enrichment first.", IntegratorUI.appName);
      return null;
    }
    return IntegratorUITools.showSelectExperimentBox(null, dialogTitle,tabs);
  }

  /**
   * Groups signals with same organism and name. Also checks
   * that each data type one occurs once per grouped instance.
   * @param observations
   * @return
   */
  public static Collection<List<ValueTriplet<NameAndSignalsTab, String, SignalType>>> groupCompatibleSignals(
    List<ValuePair<NameAndSignalsTab, ValuePair<String, SignalType>>> observations) {
    
    // Create a map to map all signals with same species and signal Name to one slot
    Map<Integer, List<ValueTriplet<NameAndSignalsTab, String, SignalType>>> groups =
      new HashMap<Integer, List<ValueTriplet<NameAndSignalsTab, String, SignalType>>>();
    // Do not add the same data type twice
    Set<ValueTriplet<NameAndSignalsTab, String, SignalType>> duplicates =
      new HashSet<ValueTriplet<NameAndSignalsTab, String, SignalType>>();
    for (ValuePair<NameAndSignalsTab, ValuePair<String, SignalType>> obs : observations) {
      int key = obs.getA().getSpecies().getKeggAbbr().hashCode() + obs.getB().getA().hashCode();
      ValueTriplet<NameAndSignalsTab, String, SignalType> triplet = new ValueTriplet<NameAndSignalsTab, String, SignalType>(
          obs.getA(),obs.getB().getA(),obs.getB().getB());
      
      // is data type already contained?
      boolean added = false;
      if (groups.get(key)!=null) {
        Class<?> content = obs.getA().getDataContentType();
        for (ValueTriplet<NameAndSignalsTab, ?, ?> vt : groups.get(key)) {
          if (vt.getA().getDataContentType().equals(content)) {
            duplicates.add(triplet);
            added = true;
            break;
          }
        }
      }
      // data type is not yet contained.
      if (!added) miRNA.addToList(groups, key, triplet);
    }
    
    // Merge with duplicates / add duplicates as single items...
    ArrayList<List<ValueTriplet<NameAndSignalsTab, String, SignalType>>> l = 
      new ArrayList<List<ValueTriplet<NameAndSignalsTab, String, SignalType>>> (groups.values());
    for (ValueTriplet<NameAndSignalsTab, String, SignalType> vt: duplicates) {
      List<ValueTriplet<NameAndSignalsTab, String, SignalType>> item = 
        new LinkedList<ValueTriplet<NameAndSignalsTab, String, SignalType>>();
      item.add(vt);
      l.add(item);
    }
    
    return l;
  }
    
  
}

