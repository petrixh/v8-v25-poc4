package de.symeda.sormas.backend;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.type.PostgreSQLCastingJsonJdbcType;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import de.symeda.sormas.backend.vaccination.VaccinationService;

public class ExtendedPostgreSQL94Dialect extends PostgreSQLDialect {

	public final static String SIMILARITY_OPERATOR = "similarity_operator";
	public final static String ARRAY_CONTAINS_TEXT = "array_contains_operator";
	public final static String JSON_EXTRACT_PATH_TEXT = "json_extract_path_text";
	public final static String ARRAY_TO_STRING = "array_to_string";
	public final static String ARRAY_AGG = "array_agg";
	// forces the use of the concat function, by default hibernate uses `||` operator
	public final static String CONCAT_FUNCTION = "concat_function";
	public static final String DATE = "date";
	public final static String UNACCENT = "unaccent";
	public final static String ILIKE = "ilike";
	public final static String WINDOW_FIRST_VALUE_DESC = "window_first_value_desc";
	public final static String WINDOW_COUNT = "window_count";
	public final static String GREATEST = "greatest";
	public final static String TIMESTAMP_SUBTRACT_14_DAYS = "timestamp_subtract_14_days";
	public final static String AT_END_OF_DAY = "at_end_of_day";
	public final static String FULLTEXT_SEARCH = "fulltext_search";
	public final static String EMPTY_TO_NULL = "empty_to_null";

	public ExtendedPostgreSQL94Dialect() {
		super();
	}

	public ExtendedPostgreSQL94Dialect(DialectResolutionInfo info) {
		super(info);
	}

	/**
	 * Hibernate 6+ registers SQL functions here instead of in the constructor (the dialect is the {@code FunctionContributor}).
	 * Every function keeps the name, argument count and SQL it had with Hibernate 5. Functions registered without a type
	 * return the type of their first argument, as {@code StandardSQLFunction} did.
	 */
	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);
		SqmFunctionRegistry registry = functionContributions.getFunctionRegistry();
		TypeConfiguration types = functionContributions.getTypeConfiguration();

		// needed because of hibernate bug: https://hibernate.atlassian.net/browse/HHH-11938
		registry.registerNamed("regexp_replace");
		registry.registerNamed(ARRAY_TO_STRING);
		registry.namedDescriptorBuilder(CONCAT_FUNCTION, "concat").register();
		registry.registerNamed(ARRAY_AGG);
		pattern(registry, types, SIMILARITY_OPERATOR, StandardBasicTypes.BOOLEAN, "?1 % ?2", 2);
		pattern(registry, types, UNACCENT, StandardBasicTypes.STRING, "unaccent(?1)", 1);
		pattern(registry, types, ILIKE, StandardBasicTypes.BOOLEAN, "?1 ILIKE ?2", 2);
		pattern(
			registry,
			types,
			WINDOW_FIRST_VALUE_DESC,
			StandardBasicTypes.STRING,
			"FIRST_VALUE(?1) OVER (PARTITION BY ?2 ORDER BY ?3 DESC RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)",
			3);
		pattern(
			registry,
			types,
			WINDOW_COUNT,
			StandardBasicTypes.LONG,
			"COUNT(?1) OVER (PARTITION BY ?2 RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)",
			2);
		registry.registerNamed(GREATEST);
		pattern(registry, types, ARRAY_CONTAINS_TEXT, StandardBasicTypes.BOOLEAN, "?1 @> array[?2]::text[]", 2);
		pattern(registry, types, JSON_EXTRACT_PATH_TEXT, StandardBasicTypes.STRING, "json_extract_path_text(?1, ?2)", 2);
		pattern(
			registry,
			types,
			TIMESTAMP_SUBTRACT_14_DAYS,
			StandardBasicTypes.DATE,
			"?1 - interval '" + VaccinationService.REPORT_DATE_RELEVANT_DAYS + " days'",
			1);
		pattern(registry, types, AT_END_OF_DAY, StandardBasicTypes.DATE, "?1::date + interval '1 day' - interval '1 microsecond'", 1);
		pattern(registry, types, FULLTEXT_SEARCH, StandardBasicTypes.BOOLEAN, "?1 @@ plainto_tsquery('simple', unaccent(?2))", 2);
		pattern(registry, types, EMPTY_TO_NULL, StandardBasicTypes.STRING, "NULLIF(TRIM(?1), '')", 1);
	}

	/**
	 * JSON values are bound as {@code json} instead of Hibernate 7's default {@code jsonb}, as hibernate-types did: {@code json} and
	 * {@code text} columns keep the JSON text as written (PostgreSQL casts {@code json} to {@code jsonb} or {@code text} on assignment).
	 */
	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);
		typeContributions.getTypeConfiguration().getJdbcTypeRegistry().addDescriptor(SqlTypes.JSON, PostgreSQLCastingJsonJdbcType.JSON_INSTANCE);
	}

	private static void pattern(
		SqmFunctionRegistry registry,
		TypeConfiguration types,
		String name,
		BasicTypeReference<?> type,
		String pattern,
		int argumentCount) {

		BasicType<?> basicType = types.getBasicTypeRegistry().resolve(type);
		registry.patternDescriptorBuilder(name, pattern).setInvariantType(basicType).setExactArgumentCount(argumentCount).register();
	}
}
