package org.deri.exreta.dal.enums;

/**
 * Enum for schemas used to create the predictor models.
 * 
 * @author Emir Muñoz <emir@emunoz.org>
 * @since 2013-03-11
 * 
 */
public enum ModelSchema
{
	NAIVE_BAYES("naive-bayes"), DECISION_TREES("bagging-dt"), RANDOM_FOREST("random-forest"), SIMPLE_LOGISTIC(
			"simple-logistic"), SVM("svm"), RATIO_32("ratio-32"), RATIO_34("ratio-34"), RATIO_36("ratio-36"), DEFAULT(
			"bagging-dt");

	private final String	model;

	ModelSchema(String model)
	{
		this.model = model;
	}

	public String getValue()
	{
		return model;
	}

	/**
	 * Return the corresponding ModelSchema according its value.
	 * 
	 * @param value Value of the ModelSchema.
	 * @return Corresponding ModelSchema enum.
	 */
	public static ModelSchema fromString(String value)
	{
		if (value != null)
		{
			for (ModelSchema b : ModelSchema.values())
			{
				if (value.equalsIgnoreCase(b.model))
					return b;
			}
		}
		return DEFAULT;
	}
}
