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
package de.zbit.gui.tabs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JTable;
import javax.swing.JToolBar;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.TableResult;
import de.zbit.gui.BaseFrame.BaseAction;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUITools;
import de.zbit.gui.customcomponents.TableResultTableModel;
import de.zbit.gui.table.JTableFilter;
import de.zbit.io.csv.CSVWriter;
import de.zbit.io.filefilter.SBFileFilter;
import de.zbit.util.Species;
import de.zbit.util.objectwrapper.ValuePairUncomparable;

/**
 * A generic Integrator tab with a table on top.
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class IntegratorTabWithTable extends IntegratorTab<Collection<? extends TableResult>> {
  private static final long serialVersionUID = -8876183417528573116L;
  public static final transient Logger log = Logger.getLogger(IntegratorTabWithTable.class.getName());
  
  /**
   * The {@link JTable} holding visualized Names and Signals.
   */
  // do NOT set to null initially!!!!
  protected JTable table;
  
  /**
   * Listeners that must be informed, if the current table changes.
   */
  private Set<IntegratorTabWithTable> tableChangeListeners=null;
  
  /**
   * Having an iterator, pointing at an indice of #data is
   * much faster, if data is no List (overwriting methods sometimes
   * also use a non-list here).
   */
  private ValuePairUncomparable<Iterator<? extends TableResult>, Integer> currentDataIterator=null;

  /**
   * @param parent
   * @param data
   */
  public IntegratorTabWithTable(IntegratorUI parent, List<? extends TableResult> data) {
    super(parent, data);
  }
  
  public IntegratorTabWithTable(IntegratorUI parent, List<? extends TableResult> data, Species species) {
    super(parent, data, species);
  }
  
  
  /* (non-Javadoc)
   * @see de.zbit.gui.IntegratorTab#saveToFile()
   */
  @Override
  public File saveToFile() {
    final File f = GUITools.showSaveFileChooser(this, IntegratorUI.saveDir, SBFileFilter.createTSVFileFilter());
    if (f==null) return null;
    
    Runnable r = new Runnable() {
      @Override
      public void run() {
        try {
          //CSVwriteableIO.write(getData(), f.getAbsolutePath());
          synchronized (table) {
            final CSVWriter w = new CSVWriter(IntegratorUI.getInstance().getStatusBar().showProgress());
            w.write(table, f);
          }
          GUITools.showMessage("Saved table successfully to \"" + f.getPath() + "\".", IntegratorUI.appName);
        } catch (Throwable e) {
          GUITools.showErrorMessage(IntegratorUI.getInstance(), e);
        }
      }
    };
    IntegratorUITools.runInSwingWorker(r);
    
    // Unfortunately we can not check anymore wether it failed or succeeded.
    return f;
  }

  /* (non-Javadoc)
   * @see de.zbit.gui.BaseFrameTab#updateButtons(javax.swing.JMenuBar, javax.swing.JToolBar)
   */
  @Override
  public void updateButtons(JMenuBar menuBar, JToolBar... toolbar) {
    // Update the toolbar.
    if (toolbar!=null && toolbar.length>0) {
      createJToolBarItems(toolbar[0]);
    }
    
    // Enable and disable items
    if (isReady()) {
      GUITools.setEnabled(true, menuBar, BaseAction.FILE_SAVE_AS, BaseAction.FILE_CLOSE);
    } else {
      // Reading/Analyzing is still in progress.
      // If analyzing failed, tab will be closed automatically!
      GUITools.setEnabled(false, menuBar, BaseAction.FILE_SAVE_AS, BaseAction.FILE_CLOSE);
    }
  }
  
  public void createJToolBarItems(JToolBar bar) {
    String uniqueName = parent.getClass().getSimpleName() + parent.hashCode();
    if (bar.getName().equals(uniqueName)) return;
    bar.removeAll();
    bar.setName(uniqueName);
    
    createJToolBarItems(bar, true);
    GUITools.setOpaqueForAllElements(bar, false);
  }
  
  public void createJToolBarItems(JToolBar bar, boolean clearExistingToolbar) {
    if (clearExistingToolbar) bar.removeAll();
    //XXX: Place buttons here in overriding functions.
    // Overrider also updateButtons() with call to super() and enable /
    // disable buttons on toolbar.
  }
  

  /* (non-Javadoc)
   * @see de.zbit.gui.IntegratorTab#getVisualization()
   */
  @Override
  public JComponent getVisualization() {
    // null check is in createTable().
    //if (data==null) return null;
    
    if (table==null) {
      createTable();
    }
    
    return table;
  }

  /* (non-Javadoc)
   * @see de.zbit.gui.IntegratorTab#getObjectAt(int)
   */
  @Override
  public Object getObjectAt(int i) {
    if (data instanceof RandomAccess && data instanceof List) {
      return ((List<? extends TableResult>)data).get(i);
    } else {
      // Memorize an internal iterator (can only go forward)
      if (currentDataIterator==null || currentDataIterator.getB()>i) {
        // NOTE: index is a pointer on the NEXT element, not on the last. 
        currentDataIterator = new ValuePairUncomparable<Iterator<? extends TableResult>, Integer>(data.iterator(), 0);
      }
      // Go to current element
      Iterator<? extends TableResult> it = currentDataIterator.getA();
      Integer index = currentDataIterator.getB();
      Object ret = null;
      while (it.hasNext()) {
        ret = it.next();
        if (index==i) break;
        index++;
      }
      // Store current iterator position and return object
      currentDataIterator.setB(index+1);
      if (index==i) return ret; else return null;
    }
  }

  /* (non-Javadoc)
   * @see de.zbit.gui.IntegratorTab#getSelectedIndices()
   */
  @Override
  public int[] getSelectedIndices() {
    // Get selected items
    synchronized (table) {
      int[] selRows = table.getSelectedRows();
      
      // Map to view rows (account for sorted tables!)
      for (int i=0; i<selRows.length; i++) {
        selRows[i] = table.convertRowIndexToModel(selRows[i]);
      }
      
      return selRows;
    }
  }
  
  /**
   * Converts the given indices to the model and returns the actual underlying items.
   * @param selectedIndices
   * @return
   */
  public List<?> getSelectedItems(List<Integer> selectedIndices) {
    // Get selected rows
    if (selectedIndices==null) return null;
    
    synchronized (table) {
      List<Object> geneList = new ArrayList<Object>(selectedIndices.size());
      for (int i=0; i<selectedIndices.size(); i++) {
        geneList.add(getObjectAt(table.convertRowIndexToModel(selectedIndices.get(i))));
      }
      
      return geneList;
    }
  }

  protected void createTable() {
    if (data==null) return;
    
    // Also adds the enrichment right mouse menu
    synchronized (table==null?this:table) {
      table = TableResultTableModel.buildJTable(this);
    }
  }

  /**
   * Creates a new table for the given data.
   */
  public void rebuildTable() {
    createTable();
    super.init();
    fireTableChangeListeners();
  }
  
  /**
   * Invokes the {@link #rebuildTable()} method on all 
   * tabs in {@link #tableChangeListeners}.
   */
  private void fireTableChangeListeners() {
    /* If you wonder why this "might not work":
     * If data get's gene-centered, objects are COPIES! */
    if (tableChangeListeners == null) return;
    for (IntegratorTabWithTable tab : tableChangeListeners) {
      // Call rebuildTable() on listeners, avoid endless-loops.
      Set<IntegratorTabWithTable> otherListeners = tab.getTableChangeListeners();
      if (otherListeners==null || !(otherListeners.contains(this))) {
        tab.rebuildTable();
        tab.repaint();
      }
    }
  }

  /**
   * @return all tabs that listen to changes on this table.
   * May return null!
   */
  private Set<IntegratorTabWithTable> getTableChangeListeners() {
    return tableChangeListeners;
  }

  /**
   * If {@link #rebuildTable()} is invoked, on all <code>nsTab</code>s added
   * with this method, their {@link #rebuildTable()} is invoked, too.
   * <p>You must be careful not to create endless loops with this method!
   * @param nsTab
   */
  public void addTableChangeListener(IntegratorTabWithTable nsTab) {
    if (nsTab.equals(this)) return; // do not accept yourself
    if (tableChangeListeners == null) tableChangeListeners = new HashSet<IntegratorTabWithTable>();
    tableChangeListeners.add(nsTab);
  }
  
  /**
   * Try to put a resonable intial selection into a {@link JTableFilter}.
   * @param f
   */
  public void setDefaultInitialSelectionOfJTableFilter(JTableFilter f) {
    if (getExampleData() instanceof NameAndSignals) {
      NameAndSignals ns = ((NameAndSignals)getExampleData());
      List<Signal> signals = ns.getSignals();
      if (signals!=null && signals.size()>0) {
        Signal preselection = signals.get(0);
        // Search fold change
        Iterator<Signal> it = signals.iterator();
        while (it.hasNext() && !(preselection=it.next()).getType().equals(SignalType.FoldChange));
        if (!preselection.getType().equals(SignalType.FoldChange)) {
          // look for p-value
          it = signals.iterator();
          while (it.hasNext() && !(preselection=it.next()).getType().equals(SignalType.pValue));
        }
        
        // Set initial selection
        String signalColumnName = NameAndSignals.signal2columnName(preselection);
        if (ns instanceof EnrichmentObject) {
          if (ns.getColumnCount()>5) {
            signalColumnName = ((EnrichmentObject<?>)ns).getColumnName(5);
          } else {
            signalColumnName = "Q-value";
          }
        }
        if (preselection.getType().equals(SignalType.pValue)) {
          f.setInitialSelection(signalColumnName, "<", "0.05");
        } else {
          f.setInitialSelection(signalColumnName, "|>=|", "1.0");
        }
      }
    }
  }
}
