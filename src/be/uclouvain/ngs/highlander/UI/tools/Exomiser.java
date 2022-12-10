/*****************************************************************************************
 *
 * Highlander - Copyright (C) <2012-2020> <Université catholique de Louvain (UCLouvain)>
 * 	
 * List of the contributors to the development of Highlander: see LICENSE file.
 * Description and complete License: see LICENSE file.
 * 	
 * This program (Highlander) is free software: 
 * you can redistribute it and/or modify it under the terms of the 
 * GNU General Public License as published by the Free Software Foundation, 
 * either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program (see COPYING file).  If not, 
 * see <http://www.gnu.org/licenses/>.
 * 
 *****************************************************************************************/

package be.uclouvain.ngs.highlander.UI.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.MultipartPostMethod;

import be.uclouvain.ngs.highlander.Highlander;
import be.uclouvain.ngs.highlander.Resources;
import be.uclouvain.ngs.highlander.Resources.Palette;
import be.uclouvain.ngs.highlander.Tools;
import be.uclouvain.ngs.highlander.UI.dialog.AskListOfHPOTermDialog;
import be.uclouvain.ngs.highlander.UI.misc.WaitingPanel;
import be.uclouvain.ngs.highlander.UI.misc.WrapLayout;
import be.uclouvain.ngs.highlander.database.Results;
import be.uclouvain.ngs.highlander.database.HighlanderDatabase.Schema;
import be.uclouvain.ngs.highlander.datatype.AnalysisFull;
import be.uclouvain.ngs.highlander.datatype.HPOTerm;
import be.uclouvain.ngs.highlander.datatype.Report;

/**
 * @author Raphael Helaers
 */

public class Exomiser extends JFrame {

public enum Mode {SAMPLE,FAMILY}
	
	private final Set<String> samples;
	private String family;
	private final Set<FamilyMember> familyMembers = new TreeSet<>();
	private Report exomiserReport;
	private AnalysisFull analysis;
	private Mode mode;

	private JTextField txtFieldOutputDir;
	
	private JPanel advancedParamatersPanel;
  private JSpinner spiner_AUTOSOMAL_DOMINANT = new JSpinner(new SpinnerNumberModel(0.1, 0.0, 100.0, 0.1));
  private JSpinner spiner_AUTOSOMAL_RECESSIVE_HOM_ALT = new JSpinner(new SpinnerNumberModel(0.1, 0.0, 100.0, 0.1));
  private JSpinner spiner_AUTOSOMAL_RECESSIVE_COMP_HET = new JSpinner(new SpinnerNumberModel(2.0, 0.0, 100.0, 0.1));
  private JSpinner spiner_X_DOMINANT = new JSpinner(new SpinnerNumberModel(0.1, 0.0, 100.0, 0.1));
  private JSpinner spiner_X_RECESSIVE_HOM_ALT = new JSpinner(new SpinnerNumberModel(0.1, 0.0, 100.0, 0.1));
  private JSpinner spiner_X_RECESSIVE_COMP_HET = new JSpinner(new SpinnerNumberModel(2.0, 0.0, 100.0, 0.1));
  private JSpinner spiner_MITOCHONDRIAL = new JSpinner(new SpinnerNumberModel(0.2, 0.0, 100.0, 0.1));
  private JCheckBox check_AUTOSOMAL_DOMINANT = new JCheckBox("Autosomal dominant max MAF:", true);
  private JCheckBox check_AUTOSOMAL_RECESSIVE = new JCheckBox("Autosomal recessive", true);
  private JCheckBox check_X_DOMINANT = new JCheckBox("X dominant max MAF:", true);
  private JCheckBox check_X_RECESSIVE = new JCheckBox("X recessive", true);
  private JCheckBox check_MITOCHONDRIAL = new JCheckBox("Mitochondrial max MAF:", true);
	
  private JSpinner spiner_maxFrequency = new JSpinner(new SpinnerNumberModel(2.0, 0.0, 100.0, 0.1));
  private final String[] possibleFrequencySources = new String[] {
  	  "THOUSAND_GENOMES",
  	  "TOPMED",
  	  "UK10K",
  	  "ESP_AFRICAN_AMERICAN", 
  	  "ESP_EUROPEAN_AMERICAN",
  	  "ESP_ALL",
  	  "EXAC_AFRICAN_INC_AFRICAN_AMERICAN", 
  	  "EXAC_AMERICAN",
  	  "EXAC_SOUTH_ASIAN", 
  	  "EXAC_EAST_ASIAN",
  	  "EXAC_FINNISH", 
  	  "EXAC_NON_FINNISH_EUROPEAN",
  	  "EXAC_OTHER",
  	  "GNOMAD_E_AFR",
  	  "GNOMAD_E_AMR",
  	  "GNOMAD_E_ASJ",
  	  "GNOMAD_E_EAS",
  	  "GNOMAD_E_FIN",
  	  "GNOMAD_E_NFE",
  	  "GNOMAD_E_OTH",
  	  "GNOMAD_E_SAS",
  	  "GNOMAD_G_AFR",
  	  "GNOMAD_G_AMR",
  	  "GNOMAD_G_ASJ",
  	  "GNOMAD_G_EAS",
  	  "GNOMAD_G_FIN",
  	  "GNOMAD_G_NFE",
  	  "GNOMAD_G_OTH",
  	  "GNOMAD_G_SAS",
  };
  private final String[] defaultFrequencySources = new String[] {
  		"THOUSAND_GENOMES",
  		"TOPMED",
  		"UK10K",
  		"ESP_AFRICAN_AMERICAN", 
  		"ESP_EUROPEAN_AMERICAN",
  		"ESP_ALL",
  		"EXAC_AFRICAN_INC_AFRICAN_AMERICAN", 
  		"EXAC_AMERICAN",
  		"EXAC_SOUTH_ASIAN", 
  		"EXAC_EAST_ASIAN",
  		"EXAC_FINNISH", 
  		"EXAC_NON_FINNISH_EUROPEAN",
  		"EXAC_OTHER",
  		"GNOMAD_E_AFR",
  		"GNOMAD_E_AMR",
  		"GNOMAD_E_EAS",
  		"GNOMAD_E_FIN",
  		"GNOMAD_E_NFE",
  		"GNOMAD_E_OTH",
  		"GNOMAD_E_SAS",
  		"GNOMAD_G_AFR",
  		"GNOMAD_G_AMR",
  		"GNOMAD_G_EAS",
  		"GNOMAD_G_FIN",
  		"GNOMAD_G_NFE",
  		"GNOMAD_G_OTH",
  		"GNOMAD_G_SAS",
  };
  private final String[] possibleFrequencySourcesDescription = new String[] {
  		"Thousand Genomes project - http://www.1000genomes.org/",
  		"TOPMed - https://www.nhlbi.nih.gov/science/precision-medicine-activities",
  		"UK10K - http://www.uk10k.org/",
  		"ESP project - http://evs.gs.washington.edu/EVS/",
  		"ESP project - http://evs.gs.washington.edu/EVS/",
  		"ESP project - http://evs.gs.washington.edu/EVS/",
  		"ExAC project http://exac.broadinstitute.org/about",
  		"ExAC project http://exac.broadinstitute.org/about",
  		"ExAC project http://exac.broadinstitute.org/about",
  		"ExAC project http://exac.broadinstitute.org/about",
  		"ExAC project http://exac.broadinstitute.org/about",
  		"ExAC project http://exac.broadinstitute.org/about",
  		"ExAC project http://exac.broadinstitute.org/about",
  		"gnomAD Exomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Exomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Exomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Exomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Exomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Exomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Exomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Exomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Genomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Genomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Genomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Genomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Genomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Genomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Genomes - http://gnomad.broadinstitute.org/",
  		"gnomAD Genomes - http://gnomad.broadinstitute.org/",
  };
  private List<JCheckBox> check_frequency_sources = new ArrayList<>();

  private final String[] possiblePathogenicitySources = new String[] {
  		"MUTATION_TASTER", 
  		"POLYPHEN", 
  		"SIFT", 
  		"REVEL", 
  		"MVP", 
  		"CADD",
  		"REMM",
  };
  private final String[] defaultPathogenicitySources = new String[] {
  		"MUTATION_TASTER", 
  		"REVEL", 
  		"MVP", 
  		"CADD",
  		"REMM",
  };
  private final String[] possiblePathogenicitySourcesDescription = new String[] {
  		"https://www.mutationtaster.org/", 
  		"http://genetics.bwh.harvard.edu/pph2/", 
  		"https://sift.bii.a-star.edu.sg/", 
  		"https://sites.google.com/site/revelgenomics/?pli=1", 
  		"https://github.com/ShenLab/missense", 
  		"https://cadd.gs.washington.edu/",
  		"https://charite.github.io/software-remm-score.html<br>REMM is trained on non-coding regulatory regions",
  };
  private List<JCheckBox> check_pathogenicity_sources = new ArrayList<>();
                          
  private final String[] possibleEffectSources = new String[] {
  		"CHROMOSOME_NUMBER_VARIATION",
  		"TRANSCRIPT_ABLATION",
  		"EXON_LOSS_VARIANT",
  		"FRAMESHIFT_ELONGATION",
  		"FRAMESHIFT_TRUNCATION",
  		"FRAMESHIFT_VARIANT",
  		"INTERNAL_FEATURE_ELONGATION",
  		"FEATURE_TRUNCATION",
  		"MNV",
  		"COMPLEX_SUBSTITUTION",
  		"STOP_GAINED",
  		"STOP_LOST",
  		"START_LOST",
  		"SPLICE_ACCEPTOR_VARIANT",
  		"SPLICE_DONOR_VARIANT",
  		"RARE_AMINO_ACID_VARIANT",
  		"_SMALLEST_HIGH_IMPACT",
  		"MISSENSE_VARIANT",
  		"INFRAME_INSERTION",
  		"DISRUPTIVE_INFRAME_INSERTION",
  		"INFRAME_DELETION",
  		"DISRUPTIVE_INFRAME_DELETION",
  		"FIVE_PRIME_UTR_TRUNCATION",
  		"THREE_PRIME_UTR_TRUNCATION",
  		"SPLICE_REGION_VARIANT",
  		"_SMALLEST_MODERATE_IMPACT",
  		"STOP_RETAINED_VARIANT",
  		"INITIATOR_CODON_VARIANT",
  		"SYNONYMOUS_VARIANT",
  		"CODING_TRANSCRIPT_INTRON_VARIANT",
  		"NON_CODING_TRANSCRIPT_EXON_VARIANT",
  		"NON_CODING_TRANSCRIPT_INTRON_VARIANT",
  		"FIVE_PRIME_UTR_PREMATURE_START_CODON_GAIN_VARIANT",
  		"FIVE_PRIME_UTR_EXON_VARIANT",
  		"THREE_PRIME_UTR_EXON_VARIANT",
  		"FIVE_PRIME_UTR_INTRON_VARIANT",
  		"THREE_PRIME_UTR_INTRON_VARIANT",
  		"_SMALLEST_LOW_IMPACT",
  		"DIRECT_TANDEM_DUPLICATION",
  		"CUSTOM",
  		"UPSTREAM_GENE_VARIANT",
  		"DOWNSTREAM_GENE_VARIANT",
  		"INTERGENIC_VARIANT",
  		"TF_BINDING_SITE_VARIANT",
  		"REGULATORY_REGION_VARIANT",
  		"CONSERVED_INTRON_VARIANT",
  		"INTRAGENIC_VARIANT",
  		"CONSERVED_INTERGENIC_VARIANT",
  		"STRUCTURAL_VARIANT",
  		"CODING_SEQUENCE_VARIANT",
  		"INTRON_VARIANT",
  		"EXON_VARIANT",
  		"SPLICING_VARIANT",
  		"MIRNA",
  		"GENE_VARIANT",
  		"CODING_TRANSCRIPT_VARIANT",
  		"NON_CODING_TRANSCRIPT_VARIANT",
  		"TRANSCRIPT_VARIANT",
  		"INTERGENIC_REGION",
  		"CHROMOSOME",
  		"SEQUENCE_VARIANT",
  };
  private final String[] defaultEffectSources = new String[] {
  		"FIVE_PRIME_UTR_EXON_VARIANT",
  		"FIVE_PRIME_UTR_INTRON_VARIANT",
  		"THREE_PRIME_UTR_EXON_VARIANT",
  		"THREE_PRIME_UTR_INTRON_VARIANT",
  		"NON_CODING_TRANSCRIPT_EXON_VARIANT",
  		"NON_CODING_TRANSCRIPT_INTRON_VARIANT",
  		"CODING_TRANSCRIPT_INTRON_VARIANT",
  		"UPSTREAM_GENE_VARIANT",
  		"DOWNSTREAM_GENE_VARIANT",
  		"INTERGENIC_VARIANT",
  		"REGULATORY_REGION_VARIANT",
  };
  private final String[] possibleEffectSourcesDescription = new String[] {
  		"[SO:1000182] - A kind of chromosome variation where the chromosome complement is not an exact multiple of the haploid number (is a chromosome_variation).",
  		"[SO:0001893] - A feature ablation whereby the deleted region includes a transcript feature (is a: feature_ablation)",
  		"[SO:0001572] - A sequence variant whereby an exon is lost from the transcript (is a (is a: SPLICING_VARIANT), TRANSCRIPT_VARIANT ).",
  		"[SO:0001909] - A frameshift variant that causes the translational reading frame to be extended relative to the reference feature (is a FRAMESHIFT_VARIANT, internal_feature_elongation).",
  		"[SO:0001910] - A frameshift variant that causes the translational reading frame to be shortened relative to the reference feature (is a FRAMESHIFT_VARIANT, internal_feature_truncation).",
  		"[SO:0001589] -A sequence variant which causes a disruption of the translational reading frame, because the number of nucleotides inserted or deleted is not a multiple of threee (is a: protein_altering_variant).<br> Used for frameshift variant for the case where there is no stop codon any more and the rare case in which the transcript length is retained.",
  		"[SO:0001908] - A sequence variant that causes the extension of a genomic feature from within the feature rather than from the terminus of the feature, with regard to the reference sequence.<br> In Exomiser, used to annotate a COMPLEX_SUBSTITUTION that does not lead to a frameshift and increases the transcript length.",
  		"[SO:0001906] - A sequence variant that causes the reduction of a genomic feature, with regard to the reference sequence (is a: feature_variant).<br> The term INTERNAL_FEATURE_TRUNCATION would be more fitting but is not available in SO.<br> In Exomiser, used to annotate a COMPLEX_SUBSTITUTION that does not lead to a frameshift and decreases the transcript length.",
  		"[SO:0002007] - An MNV is a multiple nucleotide variant (substitution) in which the inserted sequence is the same length as the replaced sequence (is a: substitution).",
  		"[SO:1000005] - When no simple or well defined DNA mutation event describes the observed DNA change, the keyword \"complex\" should be used.<br> Usually there are multiple equally plausible explanations for the change (is a: substitution).<br> Used together with INTERNAL_FEATURE_ELONGATION or FEATURE_TRUNCATION to describe an variant that does not lead to a frameshift but a changed transcript length.<br> Used together with FRAMESHIFT_ELONGATION or FRAMESHIFT_TRUNCATION if the substitution leads to a frameshift variant.",
  		"[SO:0001587] - A sequence variant whereby at least one base of a codon is changed, resulting in a premature stop codon, leading to a shortened transcript (is a: nonsynonymous_variant, feature_truncation).",
  		"[SO:0001578] - A sequence variant where at least one base of the terminator codon (stop) is changed, resulting in an elongated transcript (is a: nonsynonymous variant, terminator_codon_variant, feature_elongation)",
  		"[SO:0002012] - A codon variant that changes at least one base of the canonical start codon (is a: initiator_codon_variant).",
  		"[SO:0001574] - A splice variant that changes the 2 base region at the 3' end of an intron (is a SPLICE_REGION_VARIANT).",
  		"[SO:0001575] - A splice variant that changes the 2 base pair region at the 5' end of an intron (is a SPLICE_REGION_VARIANT).",
  		"[SO:0002008] - A sequence variant whereby at least one base of a codon encoding a rare amino acid is changed, resulting in a different encoded amino acid (children: selenocysteine_loss, pyrrolysine_loss).",
  		"Marker for smallest VariantEffect with PutativeImpact.<br>HIGH impact.",
  		"[SO:0001583] - A sequence variant, that changes one or more bases, resulting in a different amino acid sequence but where the length is preserved.",
  		"[SO:0001821] - An inframe non synonymous variant that inserts bases into in the coding sequence (is a: inframe_indel, internal_feature_elongation).",
  		"[SO:0001824] - An inframe increase in cds length that inserts one or more codons into the coding sequence within an existing codon (is a: INFRAME_INSERTION).",
  		"[SO:0001822] - An inframe non synonymous variant that deletes bases from the coding sequence (is a: inframe_indel, feature_truncation).",
  		"[SO:0001826] - An inframe decrease in cds length that deletes bases from the coding sequence starting within an existing codon (is a: INFRAME_DELETION).",
  		"[SO:0002013] - A sequence variant that causes the reduction of a the 5'UTR with regard to the reference sequence (is a: FIVE_PRIME_UTR_EXON_VARIANT or FIVE_PRIME_UTR_INTRON_VARIANT).<br> Exomiser does not yield use this at the moment.",
  		"[SO:0002015] - A sequence variant that causes the reduction of a the 3' UTR with regard to the reference sequence (is a: FIVE_PRIME_UTR_EXON_VARIANT or FIVE_PRIME_UTR_INTRON_VARIANT).<br> Exomiser does not yield use this at the moment.",
  		"[SO:0001630] - A sequence variant in which a change has occurred within the region of the splice site, either within 1-3 bases of the exon or 3-8 bases of the intron (is a: SPLICING_VARIANT).",
  		"Marker for smallest VariantEffect with PutativeImpact.<br>MODERATE impact.",
  		"[SO:0001567] - A sequence variant where at least one base in the terminator codon is changed, but the terminator remains (is a: SYNONYMOUS_VARIANT, terminator_codon_variant).",
  		"[SO:0001582] - A codon variant that changes at least one base of the first codon of a transcript (is a: CODING_SEQUENCE_VARIANT, children: start_retained_variant, start_lost).",
  		"[SO:0001819] - A sequence variant where there is no resulting change to the encoded amino acid (is a: CODING_SEQUENCE_VARIANT, children: start_retained_variant, stop_retained_variant).",
  		"[SO:0001969] - A sequence variant that changes non-coding intro sequence in a non-coding transcript (is a: CODING_TRANSCRIPT_VARIANT, INTRON_VARIANT).",
  		"[SO:0001792] - A sequence variant that changes non-coding exon sequence in a non-coding transcript (is a: NON_CODING_TRANSCRIPT_VARIANT, EXON_VARIANT).",
  		"[SO:0001970] - A sequence variant that changes non-coding intro sequence in a non-coding transcript (is a: NON_CODING_TRANSCRIPT_VARIANT, INTRON_VARIANT).",
  		"[SO:0001983] - A 5' UTR variant where a premature start codon is introduced, moved or lost (is a: FIVE_PRIME_UTR_EXON_VARIANT or FIVE_PRIME_UTR_INTRON_VARIANT).",
  		"[SO:0002092] - A UTR variant of the 5' UTR (is a: 5_prime_UTR_variant; is a: UTR_variant).",
  		"[SO:0002089] - A UTR variant of the 3' UTR (is a: 3_prime_UTR_variant; is a: UTR_variant).",
  		"[SO:0002091] - A UTR variant between 5' UTRs (is a: 5_prime_UTR_variant; is a: UTR_variant).",
  		"[SO:0002090] - A UTR variant between 3' UTRs (is a: 3_prime_UTR_variant; is a: UTR_variant).",
  		"Marker for smallest VariantEffect with PutativeImpact.<br>LOW impact.",
  		"[SO:1000039] - A tandem duplication where the individual regions are in the same orientation (is a: tandem_duplication).<br> In Exomiser used, as an additional marker to describe that an insertion is a duplication.",
  		"Variant in a user-specified custom region.",
  		"[SO:0001631] - A sequence variant located 5' of a gene (is a: INTERGENIC_VARIANT).",
  		"[SO:0001632] - A sequence variant located 3' of a gene (is a: INTERGENIC_VARIANT).",
  		"[SO:0001628] - A sequence variant located in the intergenic region, between genes (is a: feature_variant).",
  		"[SO:0001782] - A sequence variant located within a transcription factor binding site (is a: REGULATORY_REGION_VARIANT).",
  		"[SO:0001566] - A sequence variant located within a regulatory region (is a: feature_variant).",
  		"[SO:0002018] - A transcript variant occurring within a conserved region of an intron (is a: INTRON_VARIANT).",
  		"[SO:0002011] - A variant that occurs within a gene but falls outside of all transcript features.<br> This occurs when alternate transcripts of a gene do not share overlapping sequence (is a: TRANSCRIPT_VARIANT ).",
  		"[SO:0002017] - A sequence variant located in a conserved intergenic region, between genes (is a: INTERGENIC_VARIANT).",
  		"[SO:0001537] - A sequence variant that changes one or more sequence features (is a: sequence variant).",
  		"[SO:0001580] - A sequence variant that changes the coding sequence (is a: CODING_TRANSCRIPT_VARIANT, EXON_VARIANT).<br> Sequence Ontology does not have a term CODING_TRANSCRIPT_EXON_VARIANT, so we use this.",
  		"[SO:0001627] - A transcript variant occurring within an intron (is a: TRANSCRIPT_VARIANT).<br> Exomiser uses CODING_TRANSCRIPT_INTRON_VARIANT and NON_CODING_TRANSCRIPT_INTRON_VARIANT instead.",
  		"[SO:0001791] - A sequence variant that changes exon sequence (is a: TRANSCRIPT_VARIANT).",
  		"[SO:0001568] - A sequence variant that changes the process of splicing (is a: GENE_VARIANT).",
  		"[SO:0000276] - Variant affects a miRNA (is a: miRNA_primary_transcript, small_regulatory_ncRNA).",
  		"[SO:0001564] - A sequence variant where the structure of the gene is changed (is a: feature_variant).",
  		"[SO:0001968] - A transcript variant of a protein coding gene (is a: TRANSCRIPT_VARIANT).",
  		"[SO:0001619] - (is a: TRANSCRIPT_VARIANT).<br> Used for marking splicing variants as non-coding.",
  		"[SO:0001576] - A sequence variant that changes the structure of the transcript (is a: GENE_VARIANT).<br> TRANSCRIPT_VARIANT, /** SO: (is a: GENE_VARIANT)).",
  		"[SO:0000605] - A region containing or overlapping no genes that is bounded on either side by a gene, or bounded by a gene and the end of the chromosome (is a: biological_region).",
  		"[SO:0000340] - Structural unit composed of a nucleic acid molecule which controls its own replication through the interaction of specific proteins at one or more origins of replication (is a: replicon).",
  		"[SO:0001060] - Top level term for variants, can be used for marking \"unknown effect\".",
  };  
  private List<JCheckBox> check_effect_sources = new ArrayList<>();
                            
	private List<JButton> missingResults = new ArrayList<JButton>();
	private List<JButton> runningJobs = new ArrayList<JButton>();
	
	private Set<HPOTerm> hpoTerms = new TreeSet<>();

	static private WaitingPanel waitingPanel;
	
	private boolean frameVisible = true;

	private enum Sex {UNKNOWN_SEX, FEMALE, MALE, OTHER_SEX};
	private enum AffectedStatus {MISSING, UNAFFECTED, AFFECTED};
	private class FamilyMember implements Comparable<FamilyMember> {
		String sample;
		String mother = "UNAVAILABLE";
		String father = "UNAVAILABLE";
		Sex sex = Sex.UNKNOWN_SEX;
		AffectedStatus affectedStatus = AffectedStatus.MISSING;
		boolean proband = false;
		boolean include = true;
		
		@Override
		public String toString(){
			return sample;
		}

		@Override
		public int hashCode() {
			return sample.hashCode();
		}

		@Override
		public boolean equals(Object obj){
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof FamilyMember))
				return false;
			return compareTo((FamilyMember)obj) == 0;
		}

		@Override
		public int compareTo (FamilyMember r) {
			return sample.compareTo(r.sample);
		}

	}
	
	public Exomiser(Report exomiserReport, AnalysisFull analysis, Set<String> samples, Mode mode) {
		super();
		this.samples = new TreeSet<String>(samples);
		this.exomiserReport = exomiserReport;
		this.analysis = analysis;
		this.mode = mode;
		if (mode == Mode.FAMILY) {
			String sample = samples.iterator().next();
			try {
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT family "
						+ "FROM projects JOIN projects_analyses USING (project_id) "
						+ "WHERE sample = '"+sample+"' AND analysis = '"+analysis+"'")) {
					if (res.next()){
						family = res.getString(1);
					}
				}
				try (Results res = Highlander.getDB().select(Schema.HIGHLANDER,
						"SELECT `sample` "
						+ "FROM `projects` "
						+ "JOIN `projects_analyses` USING (`project_id`) "
						+ "WHERE `family` = '"+family+"' AND analysis = '"+analysis+"'"
						)){
					while (res.next()){
						FamilyMember member = new FamilyMember();
						member.sample = res.getString("sample");
						if (member.sample.equalsIgnoreCase(sample)) member.proband = true;
						familyMembers.add(member);
					}
				}
			}catch (Exception ex){
				Tools.exception(ex);
				JOptionPane.showMessageDialog(Exomiser.this,  "Problem when fetching other family members", "Exomiser",
						JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));				
			}
		}
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int height = screenSize.height - (int)(screenSize.height*0.06);
		int width = height;
		setSize(new Dimension(width,height));		
		initUI();
		pack();
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (frameVisible){
					//copy the list of buttons in a new one to avoid concurrent modification when a button is removed from the list
					List<JButton> list = new ArrayList<JButton>(runningJobs);
					for (JButton button : list) {
						long when  = System.currentTimeMillis();
						ActionEvent event = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "Check sample status", when, 0);
						for (ActionListener listener : button.getActionListeners()) {
							listener.actionPerformed(event);
						}
					}
					try{
						Thread.sleep(30_000);
					}catch (InterruptedException ex){
						Tools.exception(ex);
					}
				}
			}
		}, "Exomiser.checkForSampleStatus").start();
	}

	private void initUI(){
		setTitle("Exomiser - " + mode + " mode");
		setIconImage(Resources.getScaledIcon(Resources.iExomiser, 64).getImage());

		setLayout(new BorderLayout());

		getContentPane().add(getPanelControls(), BorderLayout.SOUTH);

		JPanel center = new JPanel(new GridBagLayout());
		getContentPane().add(new JScrollPane(center), BorderLayout.CENTER);

		int a=0;

		center.add(getPanelDownload(), new GridBagConstraints(0, a++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(20, 20, 0, 20), 0, 0));
		
		center.add(getPanelHPO(center), new GridBagConstraints(0, a++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(20, 20, 0, 20), 0, 0));
		
		advancedParamatersPanel = getPanelAdvancedParameters();
		final JToggleButton advancedButton = new JToggleButton("Set advanced parameters for Exomiser", Resources.getScaledIcon(Resources.iSortAsc, 24));
		advancedButton.setSelectedIcon(Resources.getScaledIcon(Resources.iSortDesc, 24));
		advancedButton.setToolTipText("Show/Hide section");
		advancedButton.setRolloverEnabled(false);
		advancedButton.setSelected(false);		
		advancedParamatersPanel.setVisible(false);
		advancedButton.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if (arg0.getStateChange() == ItemEvent.SELECTED){
					advancedButton.setSelected(true);
					advancedParamatersPanel.setVisible(true);
				}else if (arg0.getStateChange() == ItemEvent.DESELECTED){
					advancedButton.setSelected(false);
					advancedParamatersPanel.setVisible(false);
				}
			}
		});

		center.add(advancedButton, new GridBagConstraints(0, a++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(20, 20, 0, 20), 0, 0));
		center.add(advancedParamatersPanel, new GridBagConstraints(0, a++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 20, 0, 20), 0, 0));
		
		if (mode == Mode.SAMPLE) {
			JPanel panel_analysis = new JPanel(new GridBagLayout());
			panel_analysis.setBorder(new TitledBorder(null, analysis.toString(), TitledBorder.LEADING, TitledBorder.TOP, null, null));
			center.add(panel_analysis, new GridBagConstraints(0, a++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(20, 20, 0, 20), 0, 0));
			int y=0;
			for (String sample : samples) {
				panel_analysis.add(getPanel(sample, y), new GridBagConstraints(0, y, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
				y++;
			}
			center.add(new JPanel(), new GridBagConstraints(0, a++, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
		}else if (mode == Mode.FAMILY) {
			center.add(getPanelFamilyProband(), new GridBagConstraints(0, a++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(20, 20, 0, 20), 0, 0));
			JPanel panel_family = new JPanel(new GridBagLayout());
			panel_family.setBorder(new TitledBorder(null, "Family " + family, TitledBorder.LEADING, TitledBorder.TOP, null, null));
			center.add(panel_family, new GridBagConstraints(0, a++, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(20, 20, 0, 20), 0, 0));
			int y=0;
			for (FamilyMember member : familyMembers) {
				if (!member.proband) {
					panel_family.add(getPanel(member, y), new GridBagConstraints(0, y, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));
					y++;					
				}
			}
			center.add(new JPanel(), new GridBagConstraints(0, a++, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0));			
		}
		waitingPanel = new WaitingPanel();
		setGlassPane(waitingPanel);
	}

	protected void processWindowEvent(WindowEvent e) {
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			frameVisible = false;
			dispose();
		}
	}
	
	private JPanel getPanelControls() {
		JPanel panel = new JPanel();
		
		if (mode == Mode.SAMPLE) {
			JButton downloadReportButton = new JButton("Download all available reports", Resources.getScaledIcon(Resources.iDownload, 24));
			downloadReportButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					for (String file : exomiserReport.getFiles()) {
						if (file.toLowerCase().contains(mode.toString().toLowerCase()) && file.contains(".xlsx")) {
							new Thread(new Runnable() {
								@Override
								public void run() {
									SwingUtilities.invokeLater(new Runnable() {
										public void run() {
											waitingPanel.setVisible(true);
											waitingPanel.start();
										}
									});
									try {
										File localDir = new File(txtFieldOutputDir.getText());
											for (String sample : samples) {
												if (isResultsAvailable(sample)) {
													String output = localDir + "/" + sample + file;
													try {
														Tools.httpDownload(exomiserReport.getUrlForFile(file, getProject(sample), sample), new File(output));
													}catch(IOException ex) {
														Tools.exception(ex);
														JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Exomiser",
																JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
													}																							
												}
											}
										JOptionPane.showMessageDialog(new JFrame(), "All reports downloaded in " + txtFieldOutputDir.getText(), "Download all available reports",	JOptionPane.PLAIN_MESSAGE, Resources.getScaledIcon(Resources.iDownload,64));
									}catch (Exception ex) {
										Tools.exception(ex);
										JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Exomiser",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
									}
									SwingUtilities.invokeLater(new Runnable() {
										public void run() {
											waitingPanel.setVisible(false);
											waitingPanel.stop();
										}
									});
								}
							}).start();
						}
					}
				}
			});
			panel.add(downloadReportButton);			

			JButton runAllMissingButton = new JButton("Run Exomiser on all missing", Resources.getScaledIcon(Resources.iRun, 24));
			runAllMissingButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					for (JButton button : missingResults) {
						new Thread(new Runnable() {
							@Override
							public void run() {
								long when  = System.currentTimeMillis();
								ActionEvent event = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "Launch Exomiser", when, 0);
								for (ActionListener listener : button.getActionListeners()) {
									listener.actionPerformed(event);
								}
							}
						}).start();
					}
				}
			});
			panel.add(runAllMissingButton);
		}
	
		JButton btnClose = new JButton("Close", Resources.getScaledIcon(Resources.iCross, 24));
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				frameVisible = false;
				dispose();
			}
		});
		panel.add(btnClose);
		
		return panel; 
	}
	
	private JPanel getPanelDownload() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel("Download directory"), new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(2, 0, 0, 0), 0, 0));
		txtFieldOutputDir = new JTextField(Tools.getHomeDirectory().toString());
		panel.add(txtFieldOutputDir, new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2, 5, 0, 5), 0, 0));
		JButton browseDir = new JButton(Resources.getScaledIcon(Resources.iFolder, 24));
		browseDir.setPreferredSize(new Dimension(32,32));
		browseDir.setToolTipText("Browse");
		browseDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						JFileChooser chooser = new JFileChooser(Tools.getHomeDirectory().toString());
						chooser.setDialogTitle("Select the download directory");
						chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
						chooser.setMultiSelectionEnabled(false);
						Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize() ;
						Dimension windowSize = chooser.getSize() ;
						chooser.setLocation(Math.max(0, (screenSize.width - windowSize.width) / 2),
								Math.max(0, (screenSize.height - windowSize.height) / 2)) ;
						int dirChooserRes = chooser.showOpenDialog(Exomiser.this) ;
						if (dirChooserRes == JFileChooser.APPROVE_OPTION) {
							txtFieldOutputDir.setText(chooser.getSelectedFile().getAbsolutePath());
						}
					}
				}, "Exomiser.browseDir").start();
			}
		});
		panel.add(browseDir, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(2, 0, 0, 0), 0, 0));		
		return panel;
	}
	
	private JPanel getPanelHPO(JPanel container) {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new TitledBorder(null, "Phenotype", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		JPanel hpoTopPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		panel.add(hpoTopPanel, BorderLayout.NORTH);
		JPanel hpoCenterPanel = new JPanel(new WrapLayout(WrapLayout.LEADING));
		panel.add(hpoCenterPanel, BorderLayout.CENTER);
		JButton addHPOButton = new JButton("Add phenotype", Resources.getScaledIcon(Resources.i2dPlus, 24));
		addHPOButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					public void run(){
						AskListOfHPOTermDialog askHPO = new AskListOfHPOTermDialog(analysis.getReference(), hpoTerms);
						Tools.centerWindow(askHPO, false);
						askHPO.setVisible(true);
						if (askHPO.getSelection() != null){
							hpoTerms.clear();
							hpoCenterPanel.removeAll();
							for (HPOTerm hpo : askHPO.getSelection()) {
								addPhenotype(hpo, hpoCenterPanel);
								panel.validate();
								panel.repaint();
								container.validate();
								container.repaint();															
							}
						}
					}
				}, "Exomiser.addPhenotype").start();
			}
		});
		hpoTopPanel.add(addHPOButton);
		return panel;
	}
	
	private JPanel getPanelAdvancedParameters() {
		JPanel panel = new JPanel(new GridBagLayout());
		int a = 0;

		JPanel inheritanceModes = new JPanel(new GridBagLayout());
		inheritanceModes.setBorder(new TitledBorder(null, "Inheritance Modes", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		String inheritanceModesGeneralTooltip = "<html>"
				+ "<p><b>Inheritance Modes</b></p>"
				+ "<p>These are the default settings, with values representing the maximum minor allele frequency in percent (%) permitted for an allele to be considered as a causative candidate under that mode of inheritance.<br>"
				+ "If you just want to analyse a sample under a single inheritance mode, unselect the others.<br>"
				+ "For AUTOSOMAL_RECESSIVE or X_RECESSIVE ensure <b>both</b> relevant HOM_ALT and COMP_HET modes are present.</p>"
				+ "</html>";
		spiner_AUTOSOMAL_DOMINANT.setToolTipText(inheritanceModesGeneralTooltip);
		spiner_AUTOSOMAL_RECESSIVE_HOM_ALT.setToolTipText(inheritanceModesGeneralTooltip);
		spiner_AUTOSOMAL_RECESSIVE_COMP_HET.setToolTipText(inheritanceModesGeneralTooltip);
		spiner_X_DOMINANT.setToolTipText(inheritanceModesGeneralTooltip);
		spiner_X_RECESSIVE_HOM_ALT.setToolTipText(inheritanceModesGeneralTooltip);
		spiner_X_RECESSIVE_COMP_HET.setToolTipText(inheritanceModesGeneralTooltip);
		spiner_MITOCHONDRIAL.setToolTipText(inheritanceModesGeneralTooltip);
		check_AUTOSOMAL_DOMINANT.setToolTipText(inheritanceModesGeneralTooltip);
		check_AUTOSOMAL_RECESSIVE.setToolTipText(inheritanceModesGeneralTooltip);
		check_X_DOMINANT.setToolTipText(inheritanceModesGeneralTooltip);
		check_X_RECESSIVE.setToolTipText(inheritanceModesGeneralTooltip);
		check_MITOCHONDRIAL.setToolTipText(inheritanceModesGeneralTooltip);
		int b = 0;
		JPanel panel_AUTOSOMAL_DOMINANT = new JPanel(new FlowLayout(FlowLayout.LEADING));
		panel_AUTOSOMAL_DOMINANT.add(check_AUTOSOMAL_DOMINANT);
		panel_AUTOSOMAL_DOMINANT.add(spiner_AUTOSOMAL_DOMINANT);
		panel_AUTOSOMAL_DOMINANT.add(new JLabel("%"));
		inheritanceModes.add(panel_AUTOSOMAL_DOMINANT, new GridBagConstraints(0, b++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		JPanel panel_AUTOSOMAL_RECESSIVE_HOM_ALT = new JPanel(new FlowLayout(FlowLayout.LEADING));
		panel_AUTOSOMAL_RECESSIVE_HOM_ALT.add(check_AUTOSOMAL_RECESSIVE);
		panel_AUTOSOMAL_RECESSIVE_HOM_ALT.add(new JLabel("homozygous alternative max MAF:"));
		panel_AUTOSOMAL_RECESSIVE_HOM_ALT.add(spiner_AUTOSOMAL_RECESSIVE_HOM_ALT);
		panel_AUTOSOMAL_RECESSIVE_HOM_ALT.add(new JLabel("%"));
		inheritanceModes.add(panel_AUTOSOMAL_RECESSIVE_HOM_ALT, new GridBagConstraints(0, b++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		JPanel panel_AUTOSOMAL_RECESSIVE_COMP_HET = new JPanel(new FlowLayout(FlowLayout.LEADING));
		panel_AUTOSOMAL_RECESSIVE_COMP_HET.add(new JLabel("       Autosomal recessive compound heterozygous max MAF:"));
		panel_AUTOSOMAL_RECESSIVE_COMP_HET.add(spiner_AUTOSOMAL_RECESSIVE_COMP_HET);
		panel_AUTOSOMAL_RECESSIVE_COMP_HET.add(new JLabel("%"));
		inheritanceModes.add(panel_AUTOSOMAL_RECESSIVE_COMP_HET, new GridBagConstraints(0, b++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		JPanel panel_X_DOMINANT = new JPanel(new FlowLayout(FlowLayout.LEADING));
		panel_X_DOMINANT.add(check_X_DOMINANT);
		panel_X_DOMINANT.add(spiner_X_DOMINANT);
		panel_X_DOMINANT.add(new JLabel("%"));
		inheritanceModes.add(panel_X_DOMINANT, new GridBagConstraints(0, b++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		JPanel panel_X_RECESSIVE_HOM_ALT = new JPanel(new FlowLayout(FlowLayout.LEADING));
		panel_X_RECESSIVE_HOM_ALT.add(check_X_RECESSIVE);
		panel_X_RECESSIVE_HOM_ALT.add(new JLabel("homozygous alternative max MAF:"));
		panel_X_RECESSIVE_HOM_ALT.add(spiner_X_RECESSIVE_HOM_ALT);
		panel_X_RECESSIVE_HOM_ALT.add(new JLabel("%"));
		inheritanceModes.add(panel_X_RECESSIVE_HOM_ALT, new GridBagConstraints(0, b++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		JPanel panel_X_RECESSIVE_COMP_HET = new JPanel(new FlowLayout(FlowLayout.LEADING));
		panel_X_RECESSIVE_COMP_HET.add(new JLabel("       X recessive compound heterozygous max MAF:"));
		panel_X_RECESSIVE_COMP_HET.add(spiner_X_RECESSIVE_COMP_HET);
		panel_X_RECESSIVE_COMP_HET.add(new JLabel("%"));
		inheritanceModes.add(panel_X_RECESSIVE_COMP_HET, new GridBagConstraints(0, b++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		JPanel panel_MITOCHONDRIAL = new JPanel(new FlowLayout(FlowLayout.LEADING));
		panel_MITOCHONDRIAL.add(check_MITOCHONDRIAL);
		panel_MITOCHONDRIAL.add(spiner_MITOCHONDRIAL);
		panel_MITOCHONDRIAL.add(new JLabel("%"));
		inheritanceModes.add(panel_MITOCHONDRIAL, new GridBagConstraints(0, b++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		panel.add(inheritanceModes, new GridBagConstraints(0, a++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		
		JPanel frequencyPanel = new JPanel(new BorderLayout());
		frequencyPanel.setBorder(new TitledBorder(null, "Frequency sources", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		JPanel frequencyValues = new JPanel(new WrapLayout(WrapLayout.LEADING));
		frequencyValues.add(new JLabel("Max frequency:"));
		frequencyValues.add(spiner_maxFrequency);
		frequencyValues.add(new JLabel("%"));
		frequencyPanel.add(frequencyValues, BorderLayout.NORTH);
		JPanel frequencySources = new JPanel(new WrapLayout(WrapLayout.LEADING));
		for (int i=0 ; i < possibleFrequencySources.length ; i++) {
			String source = possibleFrequencySources[i];
			JCheckBox box = new JCheckBox(source);
			box.setToolTipText("<html><p><b>"+box.getText()+"</b></p><p>" + possibleFrequencySourcesDescription[i] + "</p></html>");
			if (Arrays.asList(defaultFrequencySources).contains(source)) {
				box.setSelected(true);
			}
			check_frequency_sources.add(box);
			frequencySources.add(box);
		}
		frequencyPanel.add(frequencySources, BorderLayout.SOUTH);
		panel.add(frequencyPanel, new GridBagConstraints(0, a++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));

		JPanel pathogenicitySources = new JPanel(new WrapLayout(WrapLayout.LEADING));
		pathogenicitySources.setBorder(new TitledBorder(null, "Pathogenicity sources", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		for (int i=0 ; i < possiblePathogenicitySources.length ; i++) {
			String source = possiblePathogenicitySources[i];
			JCheckBox box = new JCheckBox(source);
			box.setToolTipText("<html><p><b>"+box.getText()+"</b></p><p>" + possiblePathogenicitySourcesDescription[i] + "</p></html>");
			if (Arrays.asList(defaultPathogenicitySources).contains(source)) {
				box.setSelected(true);
			}
			check_pathogenicity_sources.add(box);
			pathogenicitySources.add(box);
		}
		panel.add(pathogenicitySources, new GridBagConstraints(0, a++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		
		JPanel effectSources = new JPanel(new WrapLayout(WrapLayout.LEADING));
		effectSources.setBorder(new TitledBorder(null, "Exclude variants with effect", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		for (int i=0 ; i < possibleEffectSources.length ; i++) {
			String source = possibleEffectSources[i];
			JCheckBox box = new JCheckBox(source);
			box.setToolTipText("<html><p><b>"+box.getText()+"</b></p><p>" + possibleEffectSourcesDescription[i] + "</p></html>");
			if (Arrays.asList(defaultEffectSources).contains(source)) {
				box.setSelected(true);
			}
			check_effect_sources.add(box);
			effectSources.add(box);
		}
		panel.add(effectSources, new GridBagConstraints(0, a++, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		
		return panel;
	}
	
	private void addPhenotype(HPOTerm term, JPanel container) {
		if (!hpoTerms.contains(term)) {
			JPanel panel = new JPanel(new BorderLayout());
			panel.setBorder(new TitledBorder(null, term.getOntologyId(), TitledBorder.LEADING, TitledBorder.TOP, null, null));
			hpoTerms.add(term);
			JLabel label = new JLabel(term.getName());
			label.setToolTipText(term.getHTMLDescription());
			panel.add(label, BorderLayout.CENTER);
			panel.setToolTipText(term.getHTMLDescription());
			JButton removeHPOButton = new JButton(Resources.getScaledIcon(Resources.i2dMinus, 24));
			removeHPOButton.setToolTipText("Remove this phenotype");
			removeHPOButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					new Thread(new Runnable(){
						public void run(){
							hpoTerms.remove(term);
							container.remove(panel);
							container.validate();
							container.repaint();
						}
					}, "Exomiser.removePhenotype").start();
				}
			});
			panel.add(removeHPOButton, BorderLayout.WEST);
			container.add(panel);
			container.validate();
			container.repaint();			
		}
	}
	
	private JPanel getPanel(String sample, int pos) {
		JPanel panel = new JPanel(new GridBagLayout());
		if (pos%2 == 0) panel.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Indigo));
		else panel.setBackground(Resources.getTableOddRowBackgroundColor(Palette.Indigo));
		boolean resultsAvailable = isResultsAvailable(sample);
		JLabel label = new JLabel(sample, (resultsAvailable ? Resources.getScaledIcon(Resources.iShinyBallGreen, 24) : Resources.getScaledIcon(Resources.iShinyBallRed, 24)), SwingConstants.LEADING);
		panel.add(label, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 20, 2, 40), 0, 0));
		setControlsButtons(panel, sample, label, resultsAvailable);
		/*
		JPanel fill = new JPanel();
		if (pos%2 == 0) fill.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Indigo));
		else fill.setBackground(Resources.getTableOddRowBackgroundColor(Palette.Indigo));			
		panel.add(fill, new GridBagConstraints(3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));	
		*/
		return panel;
	}

	private JPanel getPanelFamilyProband() {
		FamilyMember proband = getFamilyProband();
		JPanel panel = getPanel(proband, -1);
		boolean resultsAvailable = isResultsAvailable(proband.sample);
		JLabel label = new JLabel(proband.sample, (resultsAvailable ? Resources.getScaledIcon(Resources.iShinyBallGreen, 24) : Resources.getScaledIcon(Resources.iShinyBallRed, 24)), SwingConstants.LEADING);
		panel.add(label, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 20, 2, 40), 0, 0));
		setControlsButtons(panel, proband.sample, label, isResultsAvailable(proband.sample));
		return panel;
	}

	private JPanel getPanel(FamilyMember member, int pos) {
		JPanel panel = new JPanel(new GridBagLayout());
		int x = 0;
		if (pos < 0) {
			panel.setBorder(new TitledBorder(null, "Proband", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			panel.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Orange));
			x++;
		}else {
			if (pos%2 == 0) panel.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Indigo));
			else panel.setBackground(Resources.getTableOddRowBackgroundColor(Palette.Indigo));		
			JCheckBox box = new JCheckBox(member.sample, member.include);
			box.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					member.include = box.isSelected();
				}
			});
			panel.add(box, new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 20, 2, 40), 0, 0));			
		}
		String[] familySamples = new String[familyMembers.size()];
		int i=0;
		familySamples[i++] = "UNAVAILABLE";
		for (FamilyMember m : familyMembers) {
			if (!m.sample.equals(member.sample)) {
				familySamples[i++] = m.sample;				
			}
		}
		panel.add(new JLabel("Mother"), new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
		JComboBox<String> boxMother = new JComboBox<String>(familySamples);
		boxMother.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				member.mother = boxMother.getSelectedItem().toString();
			}
		});
		panel.add(boxMother, new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 10), 0, 0));			
		panel.add(new JLabel("Father"), new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
		JComboBox<String> boxFather = new JComboBox<String>(familySamples);
		boxFather.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				member.father = boxFather.getSelectedItem().toString();
			}
		});
		panel.add(boxFather, new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 10), 0, 0));			
		panel.add(new JLabel("Sex"), new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
		JComboBox<Sex> boxSex = new JComboBox<Sex>(Sex.values());
		boxSex.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				member.sex = Sex.valueOf(boxSex.getSelectedItem().toString());
			}
		});
		panel.add(boxSex, new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 10), 0, 0));			
		panel.add(new JLabel("Affected Status"), new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
		JComboBox<AffectedStatus> boxAffected = new JComboBox<AffectedStatus>(AffectedStatus.values());
		boxAffected.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				member.affectedStatus = AffectedStatus.valueOf(boxAffected.getSelectedItem().toString());
			}
		});
		panel.add(boxAffected, new GridBagConstraints(x++, 0, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 2, 2, 10), 0, 0));			
		JPanel fill = new JPanel();
		if (pos < 0) {
			fill.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Orange));
		}else {
			if (pos%2 == 0) fill.setBackground(Resources.getTableEvenRowBackgroundColor(Palette.Indigo));
			else fill.setBackground(Resources.getTableOddRowBackgroundColor(Palette.Indigo));			
		}
		panel.add(fill, new GridBagConstraints(x++, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));			

		return panel;
	}
	
	private void setControlsButtons(JPanel panel, String sample, JLabel sampleLabel, boolean resultsAvailable) {
		if (resultsAvailable) {
			JButton viewHTMLButton = new JButton("View results", Resources.getScaledIcon(Resources.iExomiser, 24));
			viewHTMLButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					for (String file : exomiserReport.getFiles()) {
						if (file.toLowerCase().contains(mode.toString().toLowerCase()) && file.contains("html")) {
							new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										Tools.openURL(exomiserReport.getUrlForFile(file, getProject(sample), sample).toURI());
									}catch (Exception ex) {
										Tools.exception(ex);
										JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Exomiser",
												JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
									}
								}
							}).start();
						}
					}
				}
			});
			panel.add(viewHTMLButton, new GridBagConstraints(10, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
			JButton downloadReportButton = new JButton("Download report", Resources.getScaledIcon(Resources.iDownload, 24));
			downloadReportButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					for (String file : exomiserReport.getFiles()) {
						if (file.toLowerCase().contains(mode.toString().toLowerCase()) && file.contains(".xlsx")) {
							new Thread(new Runnable() {
								@Override
								public void run() {
									try {
										File localDir = new File(txtFieldOutputDir.getText());
										String output = localDir + "/" + sample + file;
										try {
											Tools.httpDownload(exomiserReport.getUrlForFile(file, getProject(sample), sample), new File(output));
										}catch(IOException ex) {
											Tools.exception(ex);
											JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Exomiser",
													JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
										}
									}catch (Exception ex) {
										Tools.exception(ex);
										JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Exomiser",
												JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
									}
								}
							}).start();
						}
					}
				}
			});
			panel.add(downloadReportButton, new GridBagConstraints(11, 0, 1, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 30), 0, 0));
		}else {
			JButton launchExomiserButton = new JButton("Run Exomiser", Resources.getScaledIcon(Resources.iRun, 24));
			launchExomiserButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									waitingPanel.setVisible(true);
									waitingPanel.start();
								}
							});
							if (runExomiser(sample)) {
								sampleLabel.setIcon(Resources.getScaledIcon(Resources.iShinyBallPink, 24));
								panel.remove(launchExomiserButton);
								missingResults.remove(launchExomiserButton);
								JButton checkExomiserStatusButton = new JButton("Check status", Resources.getScaledIcon(Resources.iUpdater, 24));
								runningJobs.add(checkExomiserStatusButton);
								checkExomiserStatusButton.setToolTipText("<html>You'll be warn by email when results are available<br>You can use this button to check if results are available.<br>If they are, the ball will become green and new buttons will replace this one.<br>Exomiser can take between 20 minutes and 1 hour to finish depending on the sample.</html>");
								checkExomiserStatusButton.addActionListener(new ActionListener() {
									@Override
									public void actionPerformed(ActionEvent e) {
										if (isResultsAvailable(sample)) {
											sampleLabel.setIcon(Resources.getScaledIcon(Resources.iShinyBallGreen, 24));
											panel.remove(checkExomiserStatusButton);
											runningJobs.remove(checkExomiserStatusButton);
											setControlsButtons(panel, sample, sampleLabel, true);
											panel.validate();								
										}
									}
								});
								panel.add(checkExomiserStatusButton, new GridBagConstraints(10, 0, 2, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 30), 0, 0));
								panel.validate();	
							}
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									waitingPanel.setVisible(true);
									waitingPanel.stop();
								}
							});
						}
					}).start();
				}
			});
			missingResults.add(launchExomiserButton);
			panel.add(launchExomiserButton, new GridBagConstraints(10, 0, 2, 1, 0.0, 0.0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2, 2, 2, 30), 0, 0));
		}
	}
	
	private boolean isResultsAvailable(String sample) {
		for (String file : exomiserReport.getFiles()) {
			if (file.toLowerCase().contains(mode.toString().toLowerCase())) {
				try {
					URL url = exomiserReport.getUrlForFile(file, getProject(sample), sample);
					if (!Tools.exists(url.toString())) {
						return false;
					}
				}catch (MalformedURLException ex) {
					Tools.exception(ex);
					return false;
				}				
			}
		}
		return true;
	}

	private String getProject(String sample) {
		String project = "unknown_run";
		try {
			try (Results res = Highlander.getDB().select(Schema.HIGHLANDER, "SELECT run_label "
					+ "FROM projects JOIN projects_analyses USING (project_id) "
					+ "WHERE sample = '"+sample+"' AND analysis = '"+analysis+"'")) {
				if (res.next()){
					project = res.getString(1);
				}
			}
		}catch(Exception ex) {
			Tools.exception(ex);
		}
		return project;
	}
	
	private String getGenomeAssembly() {
		if (analysis.getReference().getSpecies().equalsIgnoreCase("homo_sapiens")) {
			if (analysis.getReference().getGenomeVersion() == 37) {
				return "hg19";
			}else {
				return "hg"+analysis.getReference().getGenomeVersion();				
			}
		}else {
			return analysis.getReference().getName();
		}
	}
	
	private boolean verifyPedigree() {
		for (FamilyMember member : familyMembers) {
			if (member.mother.equals(member.father) && !member.mother.equals("UNAVAILABLE")) {
				JOptionPane.showMessageDialog(Exomiser.this, member.sample + " - mother and father cannot be the same sample", "Exomiser - verifying pedigree",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return false;
			}
			if (!member.mother.equals("UNAVAILABLE")) {
				if (!getFamilyMember(member.mother).include) {
					JOptionPane.showMessageDialog(Exomiser.this, member.sample + " - mother set to " + member.mother + ", but you have excluded this sample from the pedigree", "Exomiser - verifying pedigree",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					return false;					
				}
			}
			if (!member.father.equals("UNAVAILABLE")) {
				if (!getFamilyMember(member.father).include) {
					JOptionPane.showMessageDialog(Exomiser.this, member.sample + " - father set to " + member.father + ", but you have excluded this sample from the pedigree", "Exomiser - verifying pedigree",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
					return false;					
				}
			}
		}
		return true;
	}
	
	private FamilyMember getFamilyMember(String sample) {
		for (FamilyMember member : familyMembers) {
			if (member.sample.equals(sample)) return member;
		}
		return null;
	}
	
	private FamilyMember getFamilyProband() {
		for (FamilyMember member : familyMembers) {
			if (member.proband) return member;
		}
		return null;
	}
	
	private int getIncludedFamilySize() {
		int size = 0;
		for (FamilyMember member : familyMembers) {
			if (member.include) size++;
		}
		return size;
	}
	
	private File createYML(String sample) throws Exception {
		File yml = File.createTempFile(sample, ".yml");
		try (FileWriter fw = new FileWriter(yml)){
			fw.write("---\n");
			fw.write("analysis:\n");
			if (mode == Mode.SAMPLE || getIncludedFamilySize() == 1) {
				fw.write("    genomeAssembly: "+getGenomeAssembly()+"\n");
				fw.write("    vcf: \""+sample+".vcf\"\n");
				fw.write("    ped:\n");
				fw.write("    proband:\n");
				fw.write("    hpoIds: [\n");
				for (Iterator<HPOTerm> it = hpoTerms.iterator() ; it.hasNext() ; ) {
					HPOTerm term = it.next();
					fw.write("        '"+term.getOntologyId()+"'");
					if (it.hasNext()) fw.write(",");
					fw.write("\n");
				}
				fw.write("    ]\n");				
			}
			fw.write("    inheritanceModes: {\n");
			if (check_AUTOSOMAL_DOMINANT.isSelected()) fw.write("        AUTOSOMAL_DOMINANT: "+spiner_AUTOSOMAL_DOMINANT.getValue().toString()+",\n");
			if (check_AUTOSOMAL_RECESSIVE.isSelected()) fw.write("        AUTOSOMAL_RECESSIVE_HOM_ALT: "+spiner_AUTOSOMAL_RECESSIVE_HOM_ALT.getValue().toString()+",\n");
			if (check_AUTOSOMAL_RECESSIVE.isSelected()) fw.write("        AUTOSOMAL_RECESSIVE_COMP_HET: "+spiner_AUTOSOMAL_RECESSIVE_COMP_HET.getValue().toString()+",\n");
			if (check_X_DOMINANT.isSelected()) fw.write("        X_DOMINANT: "+spiner_X_DOMINANT.getValue().toString()+",\n");
			if (check_X_RECESSIVE.isSelected()) fw.write("        X_RECESSIVE_HOM_ALT: "+spiner_X_RECESSIVE_HOM_ALT.getValue().toString()+",\n");
			if (check_X_RECESSIVE.isSelected()) fw.write("        X_RECESSIVE_COMP_HET: "+spiner_X_RECESSIVE_COMP_HET.getValue().toString()+",\n");
			if (check_MITOCHONDRIAL.isSelected()) fw.write("        MITOCHONDRIAL: "+spiner_MITOCHONDRIAL.getValue().toString()+",\n");
			fw.write("    }\n");
			fw.write("    analysisMode: PASS_ONLY\n");
			fw.write("    frequencySources: [\n");
			for (Iterator<JCheckBox> it = check_frequency_sources.iterator() ; it.hasNext() ; ) {
				JCheckBox box = it.next();
				if (box.isSelected()) {
					fw.write("        "+box.getText());					
					if (it.hasNext()) fw.write(",");
					fw.write("\n");
				}
			}
			fw.write("    ]\n");
			fw.write("    pathogenicitySources: [\n");
			for (Iterator<JCheckBox> it = check_pathogenicity_sources.iterator() ; it.hasNext() ; ) {
				JCheckBox box = it.next();
				if (box.isSelected()) {
					fw.write("        "+box.getText());					
					if (it.hasNext()) fw.write(",");
					fw.write("\n");
				}
			}
			fw.write("    ]\n");
			fw.write("    steps: [\n");
			fw.write("        failedVariantFilter: { },\n");
			fw.write("        variantEffectFilter: {\n");
			fw.write("            remove: [\n");
			for (Iterator<JCheckBox> it = check_effect_sources.iterator() ; it.hasNext() ; ) {
				JCheckBox box = it.next();
				if (box.isSelected()) {
					fw.write("                "+box.getText());					
					if (it.hasNext()) fw.write(",");
					fw.write("\n");
				}
			}
			fw.write("            ]\n");
			fw.write("        },\n");
			fw.write("        frequencyFilter: {maxFrequency: "+spiner_maxFrequency.getValue().toString()+"},\n");
			fw.write("        pathogenicityFilter: {keepNonPathogenic: true},\n");
			fw.write("        inheritanceFilter: {},\n");
			fw.write("        omimPrioritiser: {},\n");
			fw.write("        hiPhivePrioritiser: {}\n");
			fw.write("    ]\n");
			fw.write("outputOptions:\n");
			fw.write("    outputContributingVariantsOnly: false\n");
			fw.write("    numGenes: 0\n");
			fw.write("    outputPrefix: "+sample+".exomiser."+mode.toString().toLowerCase()+"\n");
			fw.write("    outputFormats: [HTML, TSV_GENE, TSV_VARIANT]\n");
		}
		return yml;
	}

	private File createamilyPhenoPacket(String family) throws Exception {
		File yml = File.createTempFile(family, ".yml");
		try (FileWriter fw = new FileWriter(yml)){
			FamilyMember proband = getFamilyProband();
			fw.write("---\n");
			fw.write("id: "+family+"\n");
			fw.write("proband:\n");
			fw.write("  subject:\n");
			fw.write("    id: "+proband.sample+"\n");
			fw.write("    sex: "+proband.sex+"\n");
			fw.write("  phenotypicFeatures:\n");
			for (Iterator<HPOTerm> it = hpoTerms.iterator() ; it.hasNext() ; ) {
				HPOTerm term = it.next();
				fw.write("    - type:\n");
				fw.write("        id: "+term.getOntologyId()+"\n");
				fw.write("        label: "+term.getName()+"\n");
			}
			fw.write("pedigree:\n");
			fw.write("  persons:\n");
			for (FamilyMember m : familyMembers) {
				if (m.include) {
					fw.write("    - individualId: "+m.sample+"\n");
					if (!m.father.equals("UNAVAILABLE")) fw.write("      paternalId: "+m.father+"\n");
					if (!m.mother.equals("UNAVAILABLE")) fw.write("      maternalId: "+m.mother+"\n");
					fw.write("      sex: "+m.sex+"\n");
					fw.write("      affectedStatus: "+m.affectedStatus+"\n");
				}
			}
			fw.write("htsFiles:\n");
			fw.write("  - uri: "+proband.sample+".vcf\n");
			fw.write("    htsFormat: VCF\n");
			fw.write("    genomeAssembly: "+getGenomeAssembly()+"\n");
			fw.write("metaData:\n");
			fw.write("  created: '"+Instant.now().toString()+"'\n");
			fw.write("  createdBy: "+Highlander.getLoggedUser().getUsername()+"\n");
			fw.write("  resources:\n");
			fw.write("    - id: hp\n");
			fw.write("      name: human phenotype ontology\n");
			fw.write("      url: http://purl.obolibrary.org/obo/hp.owl\n");
			try {
				fw.write("      version: hp/releases/20"+Highlander.getDB().getSchemaName(analysis.getReference(), Schema.HPO).substring(4).replace('_', '-')+"\n");
			}catch(Exception ex) {
				fw.write("      version: hp/releases/2022-10-05\n");			
			}
			fw.write("      namespacePrefix: HP\n");
			fw.write("      iriPrefix: 'http://purl.obolibrary.org/obo/HP_'\n");
			fw.write("  phenopacketSchemaVersion: 1.0\n");
		}
		return yml;
	}

	private boolean runExomiser(String sample) {
		if (Highlander.getParameters().getUrlForPhpScripts() == null) {
			JOptionPane.showMessageDialog(new JFrame(),  "Launching Exomiser impossible: you should configure 'server > php' parameter in settings.xml", "Exomiser",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			return false;
		}
		if (hpoTerms.isEmpty()) {
			JOptionPane.showMessageDialog(new JFrame(),  "Launching Exomiser impossible: you should select at least one phenotype", "Exomiser",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			return false;
		}
		if (mode == Mode.FAMILY && getIncludedFamilySize() > 1 && !verifyPedigree()) {
			return false;
		}
		try {
			File yml = createYML(sample);
			File ped = File.createTempFile("nofamily", ".yml");
			if (mode == Mode.FAMILY && getIncludedFamilySize() > 1) {
				ped = createamilyPhenoPacket(family);
			}			
			HttpClient httpClient = new HttpClient();
			boolean bypass = false;
			if (System.getProperty("http.nonProxyHosts") != null) {
				for (String host : System.getProperty("http.nonProxyHosts").split("\\|")) {
					if ((Highlander.getParameters().getUrlForPhpScripts()+"/exomiser.php").toLowerCase().contains(host.toLowerCase())) bypass = true;
				}
			}
			if (!bypass && System.getProperty("http.proxyHost") != null) {
				try {
					HostConfiguration hostConfiguration = httpClient.getHostConfiguration();
					hostConfiguration.setProxy(System.getProperty("http.proxyHost"), Integer.parseInt(System.getProperty("http.proxyPort")));
					httpClient.setHostConfiguration(hostConfiguration);
					if (System.getProperty("http.proxyUser") != null && System.getProperty("http.proxyPassword") != null) {
						// Credentials credentials = new UsernamePasswordCredentials(System.getProperty("http.proxyUser"), System.getProperty("http.proxyPassword"));
						// Windows proxy needs specific credentials with domain ... if proxy user is in the form of domain\\user, consider it's windows
						String user = System.getProperty("http.proxyUser");
						Credentials credentials;
						if (user.contains("\\")) {
							credentials = new NTCredentials(user.split("\\\\")[1], System.getProperty("http.proxyPassword"), System.getProperty("http.proxyHost"), user.split("\\\\")[0]);
						}else {
							credentials = new UsernamePasswordCredentials(user, System.getProperty("http.proxyPassword"));
						}
						httpClient.getState().setProxyCredentials(null, System.getProperty("http.proxyHost"), credentials);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			String otherSamples = "";
			for (FamilyMember member : familyMembers) {
				if (member.include && !member.proband) {
					otherSamples += " " + member.sample;
				}
			}
			MultipartPostMethod post = new MultipartPostMethod(Highlander.getParameters().getUrlForPhpScripts()+"/exomiser.php");
			post.addParameter("analysis", analysis.toString());
			post.addParameter("sample", sample);
			post.addParameter("family", otherSamples);
			post.addParameter("mode", mode.toString().toLowerCase());
			post.addParameter("email", Highlander.getLoggedUser().getEmail());
			post.addParameter("yml", yml);
			post.addParameter("ped", ped);
			int httpRes = httpClient.executeMethod(post); 
			if (httpRes == 200) {
				StringBuilder sb = new StringBuilder();
				try (InputStreamReader isr = new InputStreamReader(post.getResponseBodyAsStream())){
					try (BufferedReader br = new BufferedReader(isr)){
						String line = null;
						while(((line = br.readLine()) != null)) {
							System.out.println(line);				
							sb.append(line+"\n");
							if (line.contains("*exitcode^1*")) {
								throw new Exception(sb.toString());
							}
						}
					}
				}
				return true;
			}else {
				JOptionPane.showMessageDialog(new JFrame(),  "Cannot launch Exomiser on the server, HTTP error " + httpRes, "Exomiser",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
				return false;
			}
		}catch (Exception ex){
			Tools.exception(ex);
			JOptionPane.showMessageDialog(new JFrame(),  Tools.getMessage("Error", ex), "Exomiser",	JOptionPane.ERROR_MESSAGE, Resources.getScaledIcon(Resources.iCross,64));
			return false;
		}		
	}
}
