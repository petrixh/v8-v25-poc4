package de.symeda.sormas.backend;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

public class ExtendedH2Dialect extends H2Dialect {

	public final static String UNACCENT = "unaccent";
	public final static String ILIKE = "ilike";
	public final static String WINDOW_FIRST_VALUE_DESC = "window_first_value_desc";
	public final static String WINDOW_COUNT = "window_count";

	public final static String ARRAY_TO_STRING = "array_to_string";
	public final static String ARRAY_AGG = "array_agg";
	public final static String CONCAT_FUNCTION = "concat_function";
	public final static String GREATEST = "greatest";

	public ExtendedH2Dialect() {
		super();
	}

	public ExtendedH2Dialect(DialectResolutionInfo info) {
		super(info);
	}

	/**
	 * Hibernate 6+ registers SQL functions here instead of in the constructor. Same names, argument counts and SQL as with
	 * Hibernate 5.
	 */
	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);
		SqmFunctionRegistry registry = functionContributions.getFunctionRegistry();
		TypeConfiguration types = functionContributions.getTypeConfiguration();

		// needed because of hibernate bug: https://hibernate.atlassian.net/browse/HHH-11938
		registry.registerNamed("regexp_replace");
		// array_to_string returns text; without a type Hibernate 7 gives it the type of its first argument (an array)
		registry.namedDescriptorBuilder(ARRAY_TO_STRING).setInvariantType(types.getBasicTypeRegistry().resolve(StandardBasicTypes.STRING)).register();
		registry.namedDescriptorBuilder(CONCAT_FUNCTION, "concat").register();
		registry.registerNamed(ARRAY_AGG);
		// The function unaccent is specific to PostgreSQL
		// With H2 let's make sure it wont fail by making the function "unaccent" do nothing
		pattern(registry, types, UNACCENT, StandardBasicTypes.STRING, "?1", 1);
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

	/**
	 * Hibernate 6+ maps enums without {@code @Enumerated(STRING)} (ordinal) to TINYINT, which H2 does not know in PostgreSQL mode
	 * (<em>Unknown data type: "TINYINT"</em> while the test schema is created). Hibernate 5 created them as integer columns.
	 */
	@Override
	protected String columnType(int sqlTypeCode) {
		return sqlTypeCode == SqlTypes.TINYINT ? "integer" : super.columnType(sqlTypeCode);
	}

	/**
	 * {@code @Enumerated(STRING)} columns are varchar, as on PostgreSQL. H2Dialect maps them to H2's ENUM type, and Hibernate 7
	 * types expressions over them differently: a String-typed function (e.g. COALESCE) over an enum column is a String on
	 * PostgreSQL but the enum on H2, so a DTO constructor that fails on PostgreSQL passed the tests (ticket 2b-14).
	 */
	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);
		typeContributions.getTypeConfiguration().getJdbcTypeRegistry().addDescriptor(SqlTypes.ENUM, VarcharJdbcType.INSTANCE);
	}

	/**
	 * Fixes <em>JdbcSQLSyntaxErrorException: Values of types "BOOLEAN" and "INTEGER" are not comparable</em>.
	 */
	@Override
	public String toBooleanValueString(boolean bool) {
		return bool ? "TRUE" : "FALSE";
	}
}
