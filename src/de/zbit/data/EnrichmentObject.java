/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.analysis.enrichment.AbstractEnrichment;
import de.zbit.data.Signal.MergeType;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.math.EnrichmentPvalue;
import de.zbit.math.HypergeometricTest;
import de.zbit.util.Utils;

/**
 * An EnrichmentObject is a result of an enrichment analysis
 * {@link AbstractEnrichment}.
 * @author Clemens Wrzodek
 */
public class EnrichmentObject<EnrichIDType> extends NameAndSignals {
  private static final long serialVersionUID = -803654750176179631L;
  public static final transient Logger log = Logger.getLogger(EnrichmentObject.class.getName());

  /**
   * Enriched genes from GeneList in current Class (e.g., pathway or GO term)
   */
  private int c_enriched;
  
  /**
   * Total genes in source GeneList.
   */
  private int c_total;
  
  /**
   * Total genes in current Class (e.g., pathway or GO term)
   */
  private int b_subset;
  
  /**
   * Total genes in genome
   */
  private int b_total;
  
  /**
   * All genes from the source list in this class.
   */
  private Collection<?> genesInClass;
  
  /**
   * The pValue calculator
   */
  private EnrichmentPvalue pValCalculator;
  
  /**
   * This is the key, under which the identifier is stored in the {@link #addData(String, Object)} list.
   */
  public final static String idKey = "id";
  
  /**
   * @see #EnrichmentObject(String, String, int, int, int, int, double, double, Collection)
   * @param name
   */
  private EnrichmentObject(String name) {
    super(name);
  }
  
  /**
   * @see #EnrichmentObject(String, String, int, int, int, int, double, double, Collection)
   * @param name
   * @param c_enriched
   * @param c_total
   */
  private EnrichmentObject(String name, int c_enriched, int c_total) {
    this(name);
    this.c_enriched=c_enriched;
    this.c_total=c_total;
  }
  
  /**
   * Initialize a new enrichment holder object.
   * This will initialize a new {@link EnrichmentPvalue} calculator and thus, is slower
   * than using another constructor with a fixed pValue calculator.
   * @see #EnrichmentObject(String, String, int, int, int, int, double, double, Collection)
   * @param name
   * @param identifier
   * @param c_enriched
   * @param c_total
   * @param b_subset
   * @param b_total
   * @param genesInClass
   */
  public EnrichmentObject(String name, EnrichIDType identifier, int c_enriched, int c_total, 
    int b_subset, int b_total, Collection<?> genesInClass) {
    this(name, identifier, c_enriched, c_total, b_subset, b_total, Double.NaN, Double.NaN, genesInClass);
    
  }
  
  /**
   * Initialize a new enrichment holder object.
   * @see #EnrichmentObject(String, String, int, int, int, int, double, double, Collection)
   * @param name
   * @param identifier
   * @param c_enriched
   * @param c_total
   * @param b_subset
   * @param b_total
   * @param pValCalculator Calculator class that is used to calculate the pValue. This has to
   * be initialized for this problem size (i.e. c_total and b_total!)
   * @param genesInClass
   */
  public EnrichmentObject(String name, EnrichIDType identifier, int c_enriched, int c_total, 
    int b_subset, int b_total, EnrichmentPvalue pValCalculator, Collection<?> genesInClass) {
    this(name, identifier, c_enriched, c_total, b_subset, b_total, Double.NaN, Double.NaN, genesInClass);
    
    this.setPValCalculator(pValCalculator);
  }
  
  
  /**
   * Initialize a new enrichment holder object.
   * @param name Enrichment class name (e.g., pathway name)
   * @param identifier Enrichment class identifier (e.g., kegg identifier)
   * @param c_enriched Enriched genes from GeneList in current Class (e.g., pathway or GO term)
   * @param c_total Total genes in source GeneList.
   * @param b_subset Total genes in current Class (e.g., pathway or GO term)
   * @param b_total Total genes in Genome
   * @param pValue the pValue
   * @param qValue The qValue (after, e.g., {@link PathwayEnrichment#BenjaminiHochberg_pVal_adjust(java.util.List)}
   * statistical correction).
   * @param genesInClass a collection of all {@link mRNA}s (or other gene identifiers) of all genes
   * from the source list in this enrichment class.
   */
  public EnrichmentObject(String name, EnrichIDType identifier, int c_enriched, int c_total, 
    int b_subset, int b_total, double pValue, double qValue, Collection<?> genesInClass) {
    this(name, c_enriched, c_total);
    this.b_subset = b_subset;
    this.b_total=b_total;
    this.genesInClass = genesInClass;
    if (identifier!=null) addData(idKey, identifier);
    
    if (!Double.isNaN(qValue)) setQValue(qValue);
    if (!Double.isNaN(pValue)) setPValue(pValue);
  }
  
  
  /**
   * @see #pValue
   * @return the pValue
   */
  public Number getPValue() {
    // Try to get the cached pValue
    Signal sig = getSignal(SignalType.pValue, defaultExperimentName);
    Double pVal = sig!=null?sig.getSignal().doubleValue():null;
    
    // Calculate the pValue if it is not cached
    if (pVal == null || Double.isNaN(pVal)) {
      if (pValCalculator==null) initDefaultPvalueCalculator();
      
      if (pValCalculator!=null) {
        pVal = pValCalculator.getPvalue(b_subset, c_enriched);
        setPValue(pVal);
      }
    }
    
    return pVal;
  }

  /**
   * Init a new hypergeometric test
   */
  private void initDefaultPvalueCalculator() {
    if (b_total>0 && c_total>0) {
      pValCalculator = new HypergeometricTest(b_total, c_total);
    }
  }

  /**
   * @see #pValue
   * @param pValue the pValue to set
   */
  private void setPValue(double pValue) {
    unsetPValue();
    addSignal(pValue, defaultExperimentName, SignalType.pValue);
  }

  private void unsetPValue() {
    removeSignals(defaultExperimentName, SignalType.pValue);
  }
  private void unsetQValue() {
    removeSignals(defaultExperimentName, SignalType.qValue);
  }
  
  /**
   * Set the {@link EnrichmentPvalue} calculator object. This automatically
   * invokes a re-calculation of the current pValue.
   * @param pValCalculator
   */
  public void setPValCalculator(EnrichmentPvalue pValCalculator) {
    // Check if the calculator is valid.
    if (pValCalculator.getGeneListSize()!=c_total || pValCalculator.getGenomeSize()!=b_total) {
      log.log(Level.WARNING, "Tried to set a wrong calculator (total list sizes don't match).");
    } else {
      if (this.pValCalculator==null || !this.pValCalculator.equals(pValCalculator)) {
        this.pValCalculator = pValCalculator;
        
        // Recalculate the pValue
        unsetPValue();
        getPValue();
      }
    }
  }

  /**
   * @return the {@link EnrichmentPvalue} calculator that is used to calculate the pValues
   * in this object.
   */
  public EnrichmentPvalue getPValCalculator() {
    return pValCalculator;
  }

  /**
   * @see #qValue
   * @return the qValue
   */
  public Number getQValue() {
    return getSignalValue(SignalType.qValue, defaultExperimentName);
  }

  /**
   * @see #qValue
   * @param value the qValue to set
   */
  public void setQValue(double qValue) {
    unsetQValue();
    addSignal(qValue, defaultExperimentName, SignalType.qValue);
  }

  /**
   * @see #genesInClass
   * @return the genesInClass
   */
  public Collection<?> getGenesInClass() {
    return genesInClass;
  }

  /**
   * @see #genesInClass
   * @param genesInClass the genesInClass to set
   */
  public void setGenesInClass(Collection<?> genesInClass) {
    this.genesInClass = genesInClass;
  }

  /**
   * @see #c_enriched
   * @return the NumberOfEnrichedGenesInClass
   */
  public int getNumberOfEnrichedGenesInClass() {
    return c_enriched;
  }

  /**
   * @see #c_total
   * @return the total number of genes in source list
   */
  public int getTotalGenesInSourceList() {
    return c_total;
  }

  /**
   * @see #b_subset
   * @return the total number of genes in current class
   */
  public int getTotalGenesInClass() {
    return b_subset;
  }

  /**
   * @see #b_total
   * @return the total number of genes in the genome
   */
  public int getTotalGenesInGenome() {
    return b_total;
  }
  
  /**
   * @return the identifier
   */
  @SuppressWarnings("unchecked")
  public EnrichIDType getIdentifier() {
    EnrichIDType o = (EnrichIDType) getData(idKey);
    return (o!=null?o:null);
  }

  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignal#toString()
   */
  @Override
  public String toString() {
    return Arrays.deepToString(toResultArray());
  }
  
  public Object[] toResultArray() {
    
    Object[] ret = new Object[7];
    
    ret[0] = getIdentifier();
    ret[1] = getName();
    ret[2] = new Ratio(getNumberOfEnrichedGenesInClass(), getTotalGenesInSourceList());
    ret[3] = new Ratio(getTotalGenesInClass(), getTotalGenesInGenome());
    ret[4] = getPValue();
    ret[5] = getQValue();
    ret[6] = getGenesInClass();
    
    return ret;
  }
  

  /* (non-Javadoc)
   * @see de.zbit.data.NameAndSignal#merge(java.util.Collection, de.zbit.data.NameAndSignal, de.zbit.data.Signal.MergeType)
   */
  @SuppressWarnings("unchecked")
  @Override
  protected <T extends NameAndSignals> void merge(Collection<T> source,
    T target, MergeType m) {
    EnrichmentObject t = (EnrichmentObject) target;
    
    Object anyObjectFromGenesInClass = null;
    
    List<Integer> c_enriched = new ArrayList<Integer> (source.size());
    List<Integer> c_total = new ArrayList<Integer> (source.size());
    List<Integer> b_subset = new ArrayList<Integer> (source.size());
    List<Integer> b_total = new ArrayList<Integer> (source.size());
    Set<Object> genesInClass = new HashSet<Object>();
    for (T o: source) {
      EnrichmentObject e = (EnrichmentObject) o;
      c_enriched.add(e.c_enriched);
      c_total.add(e.c_total);
      b_subset.add(e.b_subset);
      b_total.add(e.b_total);
      genesInClass.addAll(e.genesInClass);
      if (anyObjectFromGenesInClass==null && e.genesInClass.size()>0)
        anyObjectFromGenesInClass = e.genesInClass.iterator().next();
    }
    
    t.c_enriched = (int) Utils.round(Signal.calculate(m, c_enriched), 0);
    t.c_total = (int) Utils.round(Signal.calculate(m, c_total), 0);
    t.b_subset = (int) Utils.round(Signal.calculate(m, b_subset), 0);
    t.b_total = (int) Utils.round(Signal.calculate(m, b_total), 0);
    
    // Do Not always call mergeAbstract. If list contains geneIDs, they will
    // be averaged, what is really senseless. Though make a type checking!
    if (anyObjectFromGenesInClass instanceof NameAndSignals) {
      t.genesInClass=(Collection<?>) NameAndSignals.mergeAbstract(genesInClass, m);
    } else {
      t.genesInClass=genesInClass;
    }
    
    // Remark: pValue will be dynamically re-calculated.
    // QValue is LOST and has to be re-calculated!
    // XXX: One could also check if this eqals (or is close to) the mean!
    // In this case, this section could be removed.
    t.unsetPValue();
    t.unsetQValue();
    t.initDefaultPvalueCalculator(); // b_toatl changed!
    
  }
  
  
  
}
