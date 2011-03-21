package com.compomics.peptizer.util.agents;

import com.compomics.peptizer.interfaces.Agent;
import com.compomics.peptizer.util.AgentReport;
import com.compomics.peptizer.util.PeptideIdentification;
import com.compomics.peptizer.util.datatools.interfaces.PeptizerPeptideHit;
import com.compomics.peptizer.util.enumerator.AgentVote;
import com.compomics.peptizer.util.enumerator.SearchEngineEnum;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
/**
 * Created by IntelliJ IDEA.
 * User: kenny
 * Date: 14-sep-2007
 * Time: 15:30:28
 */

/**
 * Class description: --------
 * This class was developed to inspect for mass tolerance error (Da) (experimental vs theory).
 */
public class DeltaMassPPMAgent extends Agent {
    // Class specific log4j logger for DeltaMassPPMAgent instances.
    private static Logger logger = Logger.getLogger(DeltaMassPPMAgent.class);

    /**
     * Identifies the allowed mass tolerance.
     */
    public static final String TOLERANCE = "tolerance";

    /**
     * Identifies the allowed mass tolerance.
     */
    public static final String C13 = "c13";


    public DeltaMassPPMAgent() {
        // Init the general Agent settings.
        initialize(new String[]{TOLERANCE});
        initialize(new String[]{C13});
        SearchEngineEnum[] searchEngines = {};
        compatibleSearchEngine = searchEngines;
    }

    /**
     * INSPECTION ---------- The inspection is the core of an Agent since this logic leads to the Agent's vote. This
     * method returns an array of AgentVote objects, reflecting this Agent's idea whether to select or not to select the
     * peptide hypothesis. All Agent Implementations must also create and store AgentReport for each peptide
     * hypothesis.
     *
     * @param aPeptideIdentification PeptideIdentification that has to be inspected.
     * @return AgentVote[] as a vote upon inspection for each the confident peptide hypothesises. <ul> <li>AgentVotes[0]
     *         gives the inspection result on PeptideHit 1</li> <li>AgentVotes[1] gives the inspection result on
     *         PeptideHit 2</li> <li>AgentVotes[n] gives the inspection result on PeptideHit n+1</li> </ul> Where the
     *         different AgentVotes can be: <ul> <li>a vote approving the selection of the peptide hypothesis.</li>
     *         <li>a vote indifferent to the selection.</li> <li>a vote objecting to select the peptide hypothesis.</li>
     *         </ul>
     *         <p/>
     *         Votes positive for selection if the mass error is greater then the allowed tolerance (Ppm).
     */
    public AgentVote[] inspect(PeptideIdentification aPeptideIdentification) {

        // Localize the Dummy property.
        double lTolerance = Double.parseDouble((String) (this.iProperties.get(TOLERANCE)));

        // Localize the C13 Da tolerance property.
        boolean lC13Tolerance = Boolean.parseBoolean((String) (this.iProperties.get(C13)));


        AgentVote[] lAgentVotes = new AgentVote[aPeptideIdentification.getNumberOfConfidentPeptideHits()];
        for (int i = 0; i < lAgentVotes.length; i++) {

            // Make Agent Report!
            iReport = new AgentReport(getUniqueID());

            // 1. Get the nth confident PeptideHit.
            PeptizerPeptideHit lPeptideHit = aPeptideIdentification.getPeptideHit(i);


            AgentVote lVote = null;

            double lDeltaMass = lPeptideHit.getDeltaMass();
            double lPeptideMassByMillion = lPeptideHit.getTheoMass() / 1000000.0;
            double lPpmError = lDeltaMass / lPeptideMassByMillion;

            // The resulting Inspection score.
            if (Math.abs(lPpmError) >= lTolerance) {
                if (lC13Tolerance) {

                    lPeptideMassByMillion = (lPeptideHit.getTheoMass() - 1) / 1000000.0; // Assuming this is a C13 peak, minus -1 to simulate the C12 precursor mass.
                    lPpmError = (Math.abs(lDeltaMass) - 1) / lPeptideMassByMillion;

                    // Ok, can this be a identified C13 precursor?
                    if (lPpmError >= lTolerance) {
                        lVote = AgentVote.POSITIVE_FOR_SELECTION;
                    } else {
                        // If error is 1.01Da, then 1.01-1=0.01Da is within tolerance boundaries.
                        lVote = AgentVote.NEUTRAL_FOR_SELECTION;
                    }
                } else {
                    // Error is larger then Agent tolerance, and c13 fix is not specified.
                    lVote = AgentVote.POSITIVE_FOR_SELECTION;
                }
            } else {
                lVote = AgentVote.NEUTRAL_FOR_SELECTION;
            }

            lAgentVotes[i] = lVote;

            // Build the report!
            // Agent Result.
            // Build the report!
            // Agent Result.
            iReport.addReport(AgentReport.RK_RESULT, lAgentVotes[i]);

            // TableRow information.
            iReport.addReport(AgentReport.RK_TABLEDATA, (new BigDecimal(lPpmError)).setScale(4, BigDecimal.ROUND_HALF_DOWN));

            // Attribute Relation File Format
            iReport.addReport(AgentReport.RK_ARFF, lPpmError);

            aPeptideIdentification.addAgentReport(i + 1, getUniqueID(), iReport);
        }
        return lAgentVotes;

    }

    /**
     * Returns a description for the Agent. Note that html tags are used to stress properties. Use in tooltips and
     * configuration settings. Fill in an agent description. Report on purpose and a minor on actual implementation.
     *
     * @return String description of the DummyAgent.
     */
    public String getDescription() {
        return "<html>Inspects for the mass error (Ppm) of the peptide. <b>Votes 'Positive_for_selection' if the mass error is greater then the allowed tolerance ( " + this.iProperties.get(TOLERANCE) + ")</b>. Votes 'Neutral_for_selection' if less.</html>";
    }
}
