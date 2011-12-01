/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of Integrator, a program integratively analyze
 * heterogeneous microarray datasets. This includes enrichment-analysis,
 * pathway-based visualization as well as creating special tabular
 * views and many other features. Please visit the project homepage at
 * <http://www.cogsys.cs.uni-tuebingen.de/software/Integrator> to
 * obtain the latest version of Integrator.
 *
 * Copyright (C) 2011 by the University of Tuebingen, Germany.
 *
 * Integrator is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package de.zbit.data.methylation;

import java.util.logging.Logger;

import de.zbit.data.GeneID;
import de.zbit.data.NSwithProbesAndRegion;

/**
 * A generic class to hold DNA methylation data with annotated genes
 * (gene-based, with geneID), current probe position and Signals. 
 * @author Clemens Wrzodek
 * @version $Rev$
 */
public class DNAmethylation extends NSwithProbesAndRegion {
  private static final long serialVersionUID = -6002300790004775432L;
  public static final transient Logger log = Logger.getLogger(DNAmethylation.class.getName());
  
  public DNAmethylation(String geneName) {
    this (geneName, GeneID.default_geneID);
  }
  
  /**
   * @param geneName
   * @param geneID
   */
  public DNAmethylation(String geneName, Integer geneID) {
    // Yes, we could have included the probe name.
    // But since it is not used anywhere, we removed it.
    super(null, geneName, geneID);
    unsetProbeName();
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#clone()
   */
  @Override
  protected Object clone() throws CloneNotSupportedException {
    DNAmethylation nm = new DNAmethylation(name, getGeneID());
    return super.clone(nm);
  }
    
}
