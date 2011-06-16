/**
 *
 * @author Clemens Wrzodek
 */
package de.zbit.analysis.enrichment;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.data.EnrichmentObject;
import de.zbit.data.NameAndSignals;
import de.zbit.data.Signal;
import de.zbit.data.Signal.SignalType;
import de.zbit.data.mRNA.mRNA;
import de.zbit.mapper.AbstractMapper;
import de.zbit.mapper.MappingUtils;
import de.zbit.mapper.MappingUtils.IdentifierType;
import de.zbit.mapper.enrichment.EnrichmentMapper;
import de.zbit.math.BenjaminiHochberg;
import de.zbit.math.Correction;
import de.zbit.math.EnrichmentPvalue;
import de.zbit.math.HypergeometricTest;
import de.zbit.parser.Species;
import de.zbit.util.AbstractProgressBar;

/**
 * Abstract enrichment class to test a list of genes for enrichments
 * (e.g., PathwayEnrichment, GO-Term enrichments, ...).
 * 
 * @author Clemens Wrzodek
 *
 * @param <EnrichIDType> The identifier type of your enrichment terms.
 * This is mostly a simply string.
 */
public abstract class AbstractEnrichment <EnrichIDType> {
  public static final transient Logger log = Logger.getLogger(AbstractEnrichment.class.getName());
  
  /**
   * Mapping from GeneID 2 Enrichment class ids (e.g., KEGG Pathway ID) 
   */
  protected EnrichmentMapper<Integer, EnrichIDType> geneID2enrich_ID=null;
    
  /**
   * Mapping from Enrichment class ID to Name (e.g. KEGG Pathway Name)
   */
  protected AbstractMapper<EnrichIDType, String> enrich_ID2Name=null;
  
  /**
   * pValue 2 qValue FDR correction method to correct pValues
   * for multiple testing.
   */
  private Correction qVal = new BenjaminiHochberg();
  
  /**
   * Progress Bar (mainly for downloading and reading mapping flat files).
   */
  protected AbstractProgressBar prog;
  
  /**
   * The {@link Species}
   */
  protected Species species;

  /**
   * Create a new enrichment analysis.
   * @param geneID2enrich_ID see {@link #geneID2enrich_ID}
   * @param enrich_IDID2Name see {@link #enrich_ID2Name}
   * @param spec see {@link #species}
   * @param prog see {@link #prog}
   * @throws IOException thrown, if the species of your mappers don't match,
   * or if one of the mappers is null and the application could not initialize
   * the mapper, because of an {@link IOException}.
   */
  public AbstractEnrichment(EnrichmentMapper<Integer, EnrichIDType> geneID2enrich_ID, AbstractMapper<EnrichIDType, String> enrich_IDID2Name, Species spec, AbstractProgressBar prog) throws IOException {
    super();
    this.geneID2enrich_ID = geneID2enrich_ID;
    this.enrich_ID2Name = enrich_IDID2Name;
    this.prog = prog;
    this.species = spec;
    
    // Eventually initialize null variables
    initializeEnrichmentMappings();
  }
  /** @see #AbstractEnrichment(EnrichmentMapper, AbstractMapper, Species, AbstractProgressBar)
   */
  public AbstractEnrichment(EnrichmentMapper<Integer, EnrichIDType> geneID2enrich_ID, AbstractMapper<EnrichIDType, String> enrich_IDID2Name, AbstractProgressBar prog) throws IOException {
    this(geneID2enrich_ID, enrich_IDID2Name, null, prog);
  }
  /** @see #AbstractEnrichment(EnrichmentMapper, AbstractMapper, Species, AbstractProgressBar)
   */
  public AbstractEnrichment(Species spec, AbstractProgressBar prog) throws IOException {
    this(null,null, spec, prog);
  }
  /** @see #AbstractEnrichment(EnrichmentMapper, AbstractMapper, Species, AbstractProgressBar)
   */
  public AbstractEnrichment(EnrichmentMapper<Integer, EnrichIDType> geneID2enrich_ID, AbstractMapper<EnrichIDType, String> enrich_IDID2Name) throws IOException {
    this (geneID2enrich_ID,enrich_IDID2Name, null);
  }
  /** @see #AbstractEnrichment(EnrichmentMapper, AbstractMapper, Species, AbstractProgressBar)
   */
  public AbstractEnrichment(Species spec) throws IOException {
    this (spec, null);
  }
  /** @see #AbstractEnrichment(EnrichmentMapper, AbstractMapper, Species, AbstractProgressBar)
   */
  public AbstractEnrichment(EnrichmentMapper<Integer, EnrichIDType> geneID2enrich_ID, AbstractProgressBar prog) throws IOException {
    this(geneID2enrich_ID,null,prog);
  }
  /** @see #AbstractEnrichment(EnrichmentMapper, AbstractMapper, Species, AbstractProgressBar)
   */
  public AbstractEnrichment(EnrichmentMapper<Integer, EnrichIDType> geneID2enrich_ID) throws IOException {
    this(geneID2enrich_ID,null,null);
  }
  
  /**
   * Initialize the {@link #geneID2enrich_ID} and {@link #enrich_sourceID2enrichment_class_id} mappings.
   * <p>This method should
   * <ul><li>Check if one of the two mappings is <code>null</code> and if so, initialize the mapping
   * <li>If initializing a new mapping, the ProgressBar {@link #prog} should be used
   * <li>If initializing a new mapping, the {@link #species} should be used
   * <li>Eventually check if all mappings are compatible (e.g., mappings for
   * the same species) and throw an {@link IOException} if not
   * </ul></p>
   * @throws IOException
   */
  protected abstract void initializeEnrichmentMappings() throws IOException;
  
  /**
   * @return a human readable name for this enrichment type.
   */
  public abstract String getName();
  
  /**
   * Maps all given genes to a enrichment object (e.g., pathway) centered view.
   * 
   * 
   * <p>The Type of the returned {@link Map#values()} depends on the type of the input geneList.
   * If your input list consists of {@link mRNA}, the {@link Map#values()} will also contain
   * {@link mRNA}s, else it will always contain {@link Integer}s, representing the Gene ID!
   * 
   * @param <T> A type that is mappable to GeneID (speciefied by idType).
   * @param geneList
   * @param idType
   * @return a mapping from EnrichedObjects (e.g., Pathways) to [preferable mRNAs from geneList, else: GeneIDs from the geneList].
   */
  @SuppressWarnings("unchecked")
  private <T> Map<EnrichIDType, Set<?>> getContainedEnrichments(Collection<T> geneList, IdentifierType idType) {

    // Initialize mapper from InputID to GeneID
    AbstractMapper<String, Integer> mapper=null;
    if (idType!=null && !idType.equals(IdentifierType.NCBI_GeneID)) {
      try {
        mapper = MappingUtils.initialize2GeneIDMapper(idType, prog, species);
      } catch (IOException e) {
        log.log(Level.WARNING, "Could not read mapping file to map your gene identifiers to Entrez GeneIDs.", e);
        return null;
      }
    }
    
    // Mapping from (e.g., Pathway) 2 Genes from geneList contained in this pathway.
    Map<EnrichIDType, Set<?>> enrichClass2Genes = new HashMap<EnrichIDType, Set<?>>();
    for(T gene: geneList) {
      
      // Get Entrez gene ID of gene
      Integer geneID;
      mRNA mr = null;
      if (gene instanceof mRNA) {
        geneID = ((mRNA)gene).getGeneID();
        mr = ((mRNA)gene);
      } else if (mapper != null){
        try {
          geneID = mapper.map(gene.toString());
        } catch (Exception e) {
          log.log(Level.WARNING, "Could not map " + gene, e);
          continue;
        }
      } else if (Integer.class.isAssignableFrom(gene.getClass())) {
        geneID = (Integer) gene;
      } else if (idType.equals(IdentifierType.NCBI_GeneID)) {
        geneID = Integer.parseInt(gene.toString());
      } else {
        log.log(Level.WARNING, "Could not get Entrez Gene ID for " + gene);
        geneID = -1;
      }
      if (geneID<=0) continue;
      
      // Get pathways, in which this gene is contained
      Collection<EnrichIDType> pws=null;
      try {
        // Map Gene_id id 2 pathways in which this gene is contained
        pws = geneID2enrich_ID.map(geneID);
      } catch (Exception e) {
        log.log(Level.WARNING, "Could not get Enrichment objects for " + geneID, e);
      }
      
      // Add to list
      if (pws!=null && pws.size()>0) {
        for (EnrichIDType pw : pws) {
          // Ensure that PW is in our map
          Set pwGenes = enrichClass2Genes.get(pw);
          if (pwGenes==null) {
            if (mr!=null) {
              pwGenes = new HashSet<mRNA>();
            } else {
              pwGenes = new HashSet<Integer>();
            }
            enrichClass2Genes.put(pw, pwGenes);
          }
          
          // Add current gene to pw list
          pwGenes.add(mr!=null?mr:geneID);
        }
      }
      
    }
    
    return enrichClass2Genes;
  }
  
  /**
   * Returns enriched classes (e.g., pathways). If you have an array of genes, please use
   * {@link Arrays#asList(Object...)} 
   * <p>Note: {@link mRNA}s without {@link mRNA#getGeneID()} are NOT being
   * removed and NOT ignored. Thus, they are counted to the totalGeneList
   * size and have an influence on the pValue. If you remove all genes / probes
   * that have no assigned geneID, you might get better pValues !
   * @param <T>
   * @param geneList
   * @return
   */
  public <T> List<EnrichmentObject<EnrichIDType>> getEnrichments(Collection<mRNA> geneList) {
    return getEnrichments(geneList, null);
  }
  
  /**
   * Returns enriched classes (e.g., pathways). If you have an array of genes, please use
   * {@link Arrays#asList(Object...)}
   * @param <T>
   * @param geneList
   * @param idType
   */
  public <T> List<EnrichmentObject<EnrichIDType>> getEnrichments(Collection<T> geneList, IdentifierType idType) {
    
    // Map enriched objects on gene list
    Map<EnrichIDType, Set<?>> pwList = getContainedEnrichments(geneList, idType);
    
    // Init the enriched id 2 readable name mapping (e.g. Kegg Pathway ID 2 Kegg Pathway Name mapping)
    if (enrich_ID2Name==null) {
      enrich_ID2Name = getDefaultEnrichmentID2NameMapping();
    }
    
    // Initialize pValue calculations and ProgressBar
    EnrichmentPvalue pval = new HypergeometricTest(geneID2enrich_ID.getGenomeSize(), geneList.size());
    if (prog!=null) {
      prog.reset();
      prog.setNumberOfTotalCalls(pwList.size());
    }
    
    // Create EnrichmentObjects
    List<EnrichmentObject<EnrichIDType>> ret = new LinkedList<EnrichmentObject<EnrichIDType>>();
    for (Map.Entry<EnrichIDType, Set<?>> entry : pwList.entrySet()) {
      if (prog!=null) prog.DisplayBar();
      
      // KEGG Pathway id 2 Pathway Name
      String pw_name=entry.getKey().toString();
      if (enrich_ID2Name!=null && enrich_ID2Name.isReady()) {
        try {
          pw_name = enrich_ID2Name.map(entry.getKey());
        } catch (Exception e) {
          if (enrich_ID2Name!=null) {
            log.log(Level.WARNING, String.format("Could not map Enrichment id 2 name: %s", entry.getKey()), e);
          }
        }
      }
      
      // Total # genes in pw
      int pwSize=geneID2enrich_ID.getEnrichmentClassSize(entry.getKey());
      
      // Create result object
      EnrichmentObject<EnrichIDType> o = new EnrichmentObject<EnrichIDType>(pw_name,entry.getKey(),
          entry.getValue().size(), geneList.size(), pwSize, geneID2enrich_ID.getGenomeSize(),
          pval, entry.getValue());
      ret.add(o);
    }
    
    // Correct pValues
    if (ret.size()>0 && qVal!=null) {
      qVal.setQvalue(ret);
    }
    
    // Initially sort returned list by pValue
    Collections.sort(ret, Signal.getComparator(NameAndSignals.defaultExperimentName, SignalType.pValue));
    
    return ret;
  }
  
  
  /**
   * Create a new mapper to map Enrichment IDs (e.g., KEGG Pathway IDs) to
   * Names (e.g., actual human readable name of the pathway).
   * @see #setEnrichmentID2NameMapping(AbstractMapper)
   * @see #enrich_ID2Name
   * @return a mapper from Enrichment identifier (e.g., "GO:01234" or "path:hsa00214")
   * to human reabable names (e.g., "Glycolysis").
   */
  protected abstract AbstractMapper<EnrichIDType, String> getDefaultEnrichmentID2NameMapping();
  
  /**
   * Set the mapper to map from enrichment object id (e.g., kegg pathway id)
   * to a human reable description (e.g., "Glycolysis").
   * <p>This is an alternate method to not having to create this mapping with
   * {@link #getDefaultEnrichmentID2NameMapping()}, if it is already available.
   * @param map
   */
  public void setEnrichmentID2NameMapping(AbstractMapper<EnrichIDType, String> map) {
    enrich_ID2Name = map;
  }
  
  /**
   * Set a new FDR {@link Correction} method.
   * @param c
   */
  public void setFDRCorrectionMethod(Correction c) {
    this.qVal = c;
  }
  
}
