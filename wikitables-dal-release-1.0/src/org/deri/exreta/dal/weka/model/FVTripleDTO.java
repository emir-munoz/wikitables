package org.deri.exreta.dal.weka.model;

import java.util.ArrayList;

public class FVTripleDTO
{
	private String						featureVector;
	private ArrayList<PredictionTriple>	candidateTriples;

	public FVTripleDTO()
	{
		this.featureVector = "";
		this.candidateTriples = new ArrayList<PredictionTriple>();
	}
	
	public FVTripleDTO(FVTripleDTO copy)
	{
		this.featureVector = copy.featureVector;
		this.candidateTriples = copy.candidateTriples;
	}

	public String getFeatureVector()
	{
		return featureVector;
	}

	public void setFeatureVector(String featureVector)
	{
		this.featureVector = featureVector;
	}

	public ArrayList<PredictionTriple> getCandidateTriples()
	{
		return candidateTriples;
	}

	public void setCandidateTriples(ArrayList<PredictionTriple> candidateTriples)
	{
		this.candidateTriples = candidateTriples;
	}

}
