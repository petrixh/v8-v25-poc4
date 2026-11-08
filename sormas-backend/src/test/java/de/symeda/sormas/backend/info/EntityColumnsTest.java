/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2022 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.backend.info;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.caze.CaseDataDto;
import de.symeda.sormas.api.caze.ReinfectionDetail;
import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Strings;
import de.symeda.sormas.api.utils.Diseases;

public class EntityColumnsTest {

	private static class EntityColumnTestObject {

		@Diseases(value = {
			Disease.GIARDIASIS,
			Disease.CRYPTOSPORIDIOSIS }, hide = true)
		private String fieldWithHide;

		@Diseases({
			Disease.EVD,
			Disease.DENGUE })
		private String fieldWithoutHide;

		private String fieldWithoutAnnotation;

		private Map<ReinfectionDetail, Boolean> enumToBoolean;

		private Map<Boolean, ReinfectionDetail> booleanToEnum;
	}

	private String invokeGetDiseases(String fieldName) throws Exception {
		return invoke("getDiseases", EntityColumnTestObject.class, fieldName);
	}

	private String invoke(String methodName, Class<?> entityClass, String fieldName) throws Exception {
		EntityColumns entityColumns = new EntityColumns(null, false);
		Field field = entityClass.getDeclaredField(fieldName);
		FieldData fieldData = new FieldData(field, entityClass, "Test");
		Method method = EntityColumns.class.getDeclaredMethod(methodName, FieldData.class);
		method.setAccessible(true);
		return (String) method.invoke(entityColumns, fieldData);
	}

	private static String mapOf(String key, String value) {
		return String.format(I18nProperties.getString(Strings.mapOf), key, value);
	}

	/**
	 * The type of a Map field names its key and value types in declaration order. Both fields use the type variables of
	 * Map, so an order taken from a hash map of type variables gets one of the two wrong.
	 */
	@Test
	public void testGetFieldType_mapKeepsDeclaredTypeArgumentOrder() throws Exception {
		assertThat(invoke("getFieldType", EntityColumnTestObject.class, "enumToBoolean"), is(mapOf("ReinfectionDetail", "Boolean")));
		assertThat(invoke("getFieldType", EntityColumnTestObject.class, "booleanToEnum"), is(mapOf("Boolean", "ReinfectionDetail")));
		assertThat(
			invoke("getFieldType", CaseDataDto.class, CaseDataDto.REINFECTION_DETAILS),
			is(mapOf("ReinfectionDetail", "Boolean")));
	}

	@Test
	public void testGetDiseases_noAnnotation_returnsAll() throws Exception {
		assertThat(invokeGetDiseases("fieldWithoutAnnotation"), is("All"));
	}

	@Test
	public void testGetDiseases_withHideFalse_returnsListedDiseases() throws Exception {
		String result = invokeGetDiseases("fieldWithoutHide");
		assertThat(result, containsString(Disease.EVD.toShortString()));
		assertThat(result, containsString(Disease.DENGUE.toShortString()));
		assertThat(result, not(containsString(Disease.GIARDIASIS.toShortString())));
	}

	@Test
	public void testGetDiseases_withHideTrue_returnsAllDiseasesExceptListed() throws Exception {
		String result = invokeGetDiseases("fieldWithHide");
		assertThat(result, not(containsString(Disease.GIARDIASIS.toShortString())));
		assertThat(result, not(containsString(Disease.CRYPTOSPORIDIOSIS.toShortString())));
		assertThat(result, containsString(Disease.EVD.toShortString()));
		assertThat(result, containsString(Disease.DENGUE.toShortString()));
	}
}
