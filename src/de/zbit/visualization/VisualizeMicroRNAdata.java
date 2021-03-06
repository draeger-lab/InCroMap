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
package de.zbit.visualization;

import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import y.base.Edge;
import y.base.Node;
import y.view.Graph2D;
import y.view.NodeLabel;
import y.view.NodeRealizer;
import y.view.ShapeNodeRealizer;
import de.zbit.data.NameAndSignals;
import de.zbit.data.miRNA.miRNA;
import de.zbit.data.miRNA.miRNAtarget;
import de.zbit.graph.io.def.GraphMLmaps;
import de.zbit.gui.GUITools;
import de.zbit.gui.IntegratorUI;
import de.zbit.gui.IntegratorUITools;
import de.zbit.integrator.GraphMLmapsExtended;
import de.zbit.kegg.io.KEGG2yGraph;
import de.zbit.util.TranslatorTools;

/**
 * A special class to visualize lists of {@link miRNA}s in {@link Graph2D}s.
 * 
 * <p><i>Note:<br/>
 * Due to yFiles license requirements, we have to obfuscate this class
 * in the JAR release of this application. Thus, this class
 * can not be found by using the class name.<br/> If you can provide us
 * with a proof of possessing a yFiles license yourself, we can send you
 * an unobfuscated release of Integrator.</i></p>
 * 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class VisualizeMicroRNAdata {
  public static final transient Logger log = Logger.getLogger(VisualizeMicroRNAdata.class.getName());
  
  /**
   * Interaction type for edges from miRNA nodes to mRNA(/Gene) nodes.
   */
  private static final String MIRNA_MRNA_INTERACTION_TYPE = "binding/association";
  
  /**
   * This is the graph on which all operations are performed.
   */
  Graph2D graph;
  
  /**
   * Tools used in various methods.
   */
  TranslatorTools tools;
  
  public VisualizeMicroRNAdata(Graph2D graph) {
    super();
    this.graph = graph;
    this.tools  = new TranslatorTools(graph);
  }


  /**
   * Adds microRNAs and target relationships to graph.
   * <p>You should call the removeTargets() method before this one. Else, the resulting
   * graph could be a mixture of new and only targets.
   * @param data
   * @return number of created (or already existing) microRNA nodes in the {@link #graph},
   * corresponding to the <code>data</code>.
   */
  public int addMicroRNAsToGraph(Collection<? extends miRNA> data) {
    return addMicroRNAsToGraph(data, false);
  }
  /**
   * 
   * @param data
   * @param silent
   * @return
   * @see #addMicroRNAsToGraph(Collection)
   */
  public int addMicroRNAsToGraph(Collection<? extends miRNA> data, boolean silent) {
    // MergeType does NOT make any difference, because signals of input data are not processed
    data = NameAndSignals.geneCentered(data, IntegratorUITools.getMergeTypeSilent());
    
    Map<Integer, List<Node>> gi2n_map = tools.getGeneID2NodeMap();
    tools.ensureMapExists(GraphMLmapsExtended.NODE_NAME, true);
    Map<Object, List<Node>> mi2n_map = tools.getReverseMap(GraphMLmapsExtended.NODE_NAME);
    Set<Node> nodesToLayout = new HashSet<Node>();
    
    
    // Optional step: count number of nodes to add
    // and warn user if more than 30 nodes!
    // Return -1, so the parent method does not ask for target annotation
    if (!silent && !countNodesAndIssueWarning(data, gi2n_map, 30)) return -1;
    
    // Add a node for each miRNA to the graph.
    int visualizedMiRNAs = 0;
    for (miRNA m : data) {
      if (!m.hasTargets()) continue;
      for (miRNAtarget t: m.getUniqueTargets()) { // Merges duplicate target gene ids
        // Look if we have targets in the current graph
        List<Node> targetNodes = gi2n_map.get(t.getTarget());
        if (targetNodes!=null && targetNodes.size()>0) {
          visualizedMiRNAs++;
          
          // we have a microRNA that has targets that are contained in the current graph.
           List<Node> list = mi2n_map.get(m.getTrimmedMiRNAName());
          if (list==null || list.size()<1) {
            Node n2 = createMicroRNANode(m);
            list = new LinkedList<Node>();
            list.add(n2);
            mi2n_map.put(m, list);
            nodesToLayout.addAll(list);
          }
          
          // Create edges from miRNA node to all targets.
          for (Node target: targetNodes) {
            for (Node source: list) { // This should always be a list with exactly one node.
              if (!graph.containsEdge(source, target)) {
                Edge e = graph.createEdge(source, target);
                graph.getRealizer(e).setLineColor(Color.GRAY);
                tools.setInfo(e, GraphMLmaps.EDGE_DESCRIPTION, t.getSource());
                tools.setInfo(e, GraphMLmaps.EDGE_TYPE, MIRNA_MRNA_INTERACTION_TYPE);
              }
            }
          }
        }
      }
      
    }
    
    // Layout new added nodes.
    tools.layoutNodeSubset(nodesToLayout);
    
    return visualizedMiRNAs;
  }

  /**
   * Counts the number of nodes that will be added to the graph
   * and asks the user if he really wants to proceed, if more than
   * <code>thresholdForWarning</code> nodes will be added.
   * @param data {@link miRNA}s to create nodes for
   * @param gi2n_map geneId to node mapping. Can be created with {@link TranslatorTools#getGeneID2NodeMap()}
   * @param thresholdForWarning threshold, defining when to issue
   * a warning, representing the minimum number of miRNA nodes
   * @return true if the user wants to CONTINUE the operation 
   */
  public boolean countNodesAndIssueWarning(Collection<? extends miRNA> data,
      Map<Integer, List<Node>> gi2n_map, int thresholdForWarning) {
    // Optional step: count number of nodes to add
    // and warn user if more than 30 nodes!
    int count=0;
    for (miRNA m : data) {
      if (!m.hasTargets()) continue;
      // Look if we have targets in the current graph
      for (miRNAtarget t: m.getTargets()) {
        List<Node> targetNodes = gi2n_map.get(t.getTarget());
        if (targetNodes!=null && targetNodes.size()>0) {
          count++;
          break;
        }
      }
    }
    if (count>=thresholdForWarning) {
      int a = GUITools.showQuestionMessage(IntegratorUI.getInstance(), 
        String.format("WARNING: %s nodes will be added for the selected microRNA data.\n\n" +
          "This %s result in a confusing graph. If you visualize miRNA expression later, it is recommended to remove not differentially expressed miRNA nodes.\n\nDo you want to continue?", count,
          count>150?"will":"might"), "Add microRNA nodes", JOptionPane.YES_NO_OPTION);
      if (a!=JOptionPane.YES_OPTION) return false;
    }
    //--------------
    return true;
  }


  /**
   * Returns OR CREATES and returns a node, for the given microRNA.
   * @param mi2n_map up-to-date map from microRNA name to actual node. May be generated by {@link #getRNA2NodeMap()}.
   * @param mirna microRNA to retrieve (or create if not exists) the node for.
   * @return {@link Node} for the miRNA.
   */
  @Deprecated
  public Node getMicroRNAnode(Map<String, List<Node>> mi2n_map, miRNA mirna) {
    String nameInMap = mirna.getName().toUpperCase().trim();
    List<Node> n = mi2n_map.get(nameInMap);
    
    // Create new node and add to new list and map.
    if (n==null || n.size()<1) {
      
      Node n2 = createMicroRNANode(mirna);
      n = new LinkedList<Node>();
      n.add(n2);
      mi2n_map.put(nameInMap, n);
      return n2;
    } else {
      
      // Fetch existing node.
      return getMicroRNAnode(mi2n_map, mirna, graph);
    }
  }
  
  /**
   * Get the node, corresponding to the {@link miRNA}
   * @param mi2n_map map created with {@link TranslatorTools#getRNA2NodeMap()}
   * @param mirna actual {@link miRNA} object to get the corresponding node for
   * @param graph parent graph
   * @return Node if existent in list, or null
   */
  @Deprecated
  public static Node getMicroRNAnode(Map<String, List<Node>> mi2n_map, miRNA mirna, Graph2D graph) {
    String nameInMap = mirna.getName().toUpperCase().trim();
    List<Node> n = mi2n_map.get(nameInMap);
    
    if (n==null || n.size()<1) {
      return null;
    } else if (n.size()==1) {
      return n.get(0);
    } else if (n.size()>1) {
      
      // Splitted nodes are assigned the "getUniqueLabel()".
      String label = mirna.getUniqueLabel();
      Node parent=n.get(0); // Any node !=null
      for (Node n2: n) {
        if (graph.getLabelText(n2).equals(label)) {
          return n2;
        } else if (graph.getHierarchyManager().isGroupNode(n2)) {
          parent = n2;
        }

      }
      
      // In doubt, return group node. This will be kept and splitted nodes
      // may be removed! - This should actually never happen!
      return parent;
    } else {
      // impossible case.
      return null;
    }
  }



  /**
   * @param mirna
   * @return a {@link Node} corresponding to the given <code>mirna</code>.
   */
  private Node createMicroRNANode(miRNA mirna) {
    String label = mirna.getTrimmedMiRNAName();
    
    NodeRealizer nr = new ShapeNodeRealizer(ShapeNodeRealizer.TRIANGLE);
    nr.setFillColor(IntegratorUI.LIGHT_BLUE);
    
    NodeLabel nl = new NodeLabel(label);//nr.createNodeLabel(); nl.setText(label);
    nl.setFontSize(10);
    nl.setTextColor(Color.BLACK);
    nr.setLabel(nl);
    
    // Store hyperlinks in the node-label (and later on in the node itself).
    String link = String.format("http://www.mirbase.org/cgi-bin/mirna_entry.pl?id=%s", mirna.getPrecursorName());
    if (link!=null && link.length()!=0) {
      try {
        // Convert to URL, because type is inferred of the submitted object
        // and URL is better than STRING.
        nl.setUserData(new URL(link));
      } catch (MalformedURLException e1) {
        nl.setUserData(link);
      }
    }
    
    nr.setWidth(15);
    nr.setHeight(15);
    
    // Create node and set annotations
    Node n = graph.createNode(nr);
    tools.setInfo(n, GraphMLmaps.NODE_LABEL, mirna.getName());
    tools.setInfo(n, GraphMLmaps.NODE_TYPE, TranslatorTools.RNA_TYPE);
    tools.setInfo(n, GraphMLmaps.NODE_URL, link);
    tools.setInfo(n, GraphMLmaps.NODE_COLOR, KEGG2yGraph.ColorToHTML(nr.getFillColor()) );
    tools.setInfo(n, GraphMLmaps.NODE_NAME, nl.getText());
    tools.setInfo(n, GraphMLmaps.NODE_SIZE, String.format("%s|%s", (int)nr.getWidth(), (int)nr.getHeight()));
    tools.setInfo(n, GraphMLmapsExtended.NODE_IS_MIRNA, true);
    // Do only set NS for colored/visualized signals.
    //tools.setInfo(n, GraphMLmapsExtended.NODE_NAME_AND_SIGNALS, mirna);
    if (mirna.getID()>0) { // GraphMLmaps MUST ALWAYS be strings! 
      tools.setInfo(n, GraphMLmaps.NODE_GENE_ID, Integer.toString(mirna.getID()));
    }
    
    return n;
  }

}
