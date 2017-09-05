package org.matsim.core.router.costcalculators;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.utils.objectattributes.ObjectAttributes;

import java.util.Random;

/**
 * Computes the costs for the randomizing travel disutility.
 * This class encapsulates the consideration of subpopulations.
 */
class RandomizingTimeDistanceTravelDisutilityConfig {
	private static final Logger log = Logger.getLogger( RandomizingTimeDistanceTravelDisutilityConfig.class );

	private final String subpopulationAttributeName;
	private final ObjectAttributes personAttributes;

	private static int normalisationWrnCnt = 0;
	private final double sigma;

	private boolean doRandomize;

	private final TObjectDoubleMap<String> costsOfTime_s = new TObjectDoubleHashMap<>();
	private final TObjectDoubleMap<String> costsOfDistance_m = new TObjectDoubleHashMap<>();
	private final double normalization;

	private final double minCostOfTime_s;
	private final double minCostOfDistance_m;

	private final Random random;

	// "cache"
	private Person prevPerson = null;
	private double prevLognormalRnd = Double.NaN;
	private double prevCostOfTime_s = Double.NaN;
	private double prevCostOfDistance_m = Double.NaN;

	public RandomizingTimeDistanceTravelDisutilityConfig(
			String subpopulationAttributeName, ObjectAttributes personAttributes,
			PlanCalcScoreConfigGroup cnScoringGroup,
			String mode,
			double sigma) {
		this.subpopulationAttributeName = subpopulationAttributeName;
		this.personAttributes = personAttributes;
		this.sigma = sigma;
		doRandomize = sigma != 0.;

		if ( !doRandomize ) {
			this.normalization = 1;
			this.random = null;
		}
		else {
			this.random = MatsimRandom.getLocalInstance();

			this.normalization = 1. / Math.exp(sigma * sigma / 2);
			if (normalisationWrnCnt < 10) {
				normalisationWrnCnt++;
				log.info(" sigma: " + sigma + "; resulting normalization: " + normalization);
			}
		}

		double currMinTime = Double.POSITIVE_INFINITY;
		double currMinDist = Double.POSITIVE_INFINITY;
		for (PlanCalcScoreConfigGroup.ScoringParameterSet set : cnScoringGroup.getScoringParametersPerSubpopulation().values() ) {
			final String subpopulation = set.getSubpopulation();
			final PlanCalcScoreConfigGroup.ModeParams params = set.getModes().get(mode);

			if (params == null) {
				throw new NullPointerException(mode + " is not part of the valid mode parameters " + cnScoringGroup.getModes().keySet());
			}

			/* Usually, the travel-utility should be negative (it's a disutility) but the cost should be positive. Thus negate the utility.*/
			final double marginalCostOfTime_s = (-params.getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
			final double marginalCostOfDistance_m = -params.getMonetaryDistanceRate() * cnScoringGroup.getMarginalUtilityOfMoney()
					- params.getMarginalUtilityOfDistance();

			this.costsOfTime_s.put( subpopulation , marginalCostOfTime_s );
			this.costsOfDistance_m.put( subpopulation , marginalCostOfDistance_m );

			currMinDist = Math.min( currMinDist , marginalCostOfDistance_m );
			currMinTime = Math.min( currMinTime , marginalCostOfTime_s );
		}

		this.minCostOfDistance_m = currMinDist;
		this.minCostOfTime_s = currMinTime;
	}

	public double getLognormalRandom( Person person ) {
		switchPerson( person );
		if ( !doRandomize ) return 1;
		return prevLognormalRnd;
	}

	public double getCostOfTime_s( Person person ) {
		switchPerson( person );
		return prevCostOfTime_s;
	}

	public double getCostOfDistance_m( Person person ) {
		switchPerson( person );
		return prevCostOfDistance_m;
	}

	public double getMinCostOfTime_s() {
		return minCostOfTime_s;
	}

	public double getMinCostOfDistance_m() {
		return minCostOfDistance_m;
	}

	private void switchPerson(Person person ) {
		if ( person==null ) {
			throw new RuntimeException("you cannot use the randomzing travel disutility without person.  If you need this without a person, set"
					+ "sigma to zero.") ;
		}
		if ( person == prevPerson ) return;

		prevPerson = person;

		String subpop = (String) personAttributes.getAttribute( person.getId().toString() , subpopulationAttributeName );
		prevCostOfDistance_m = costsOfDistance_m.get( subpop );
		prevCostOfTime_s = costsOfTime_s.get( subpop );

		prevLognormalRnd = Math.exp(sigma * random.nextGaussian());
		prevLognormalRnd *= normalization;
		// this should be a log-normal distribution with sigma as the "width" parameter.   Instead of figuring out the "location"
		// parameter mu, I rather just normalize (which should be the same, see next). kai, nov'13

		/* The argument is something like this:<ul>
		 * <li> exp( mu + sigma * Z) with Z = Gaussian generates lognormal with mu and sigma.
		 * <li> The mean of this is exp( mu + sigma^2/2 ) .
		 * <li> If we set mu=0, the expectation value is exp( sigma^2/2 ) .
		 * <li> So in order to set the expectation value to one (which is what we want), we need to divide by exp( sigma^2/2 ) .
		 * </ul>
		 * Should be tested. kai, jan'14 */
	}
}
