package de.symeda.sormas.backend.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import de.symeda.sormas.api.i18n.I18nProperties;
import de.symeda.sormas.api.i18n.Validations;
import de.symeda.sormas.api.user.JurisdictionLevel;
import de.symeda.sormas.api.user.UserRight;
import de.symeda.sormas.api.user.UserRoleDto;
import de.symeda.sormas.api.utils.ValidationRuntimeException;
import de.symeda.sormas.backend.AbstractBeanTest;

public class UserRoleFacadeEjbTest extends AbstractBeanTest {

	@Test
	public void testChangeJurisdictionOfAssignedUserRole() {

		UserRoleDto userRole = UserRoleDto.build();
		userRole.setJurisdictionLevel(JurisdictionLevel.NATION);
		userRole.setCaption("Test user role");
		userRole = getUserRoleFacade().saveUserRole(userRole);

		creator.createUser(creator.createRDCF(), userRole.toReference());

		userRole.setJurisdictionLevel(JurisdictionLevel.COMMUNITY);
		UserRoleDto finalUserRole = userRole;
		assertThrowsWithMessage(
			ValidationRuntimeException.class,
			I18nProperties.getValidationError(Validations.jurisdictionChangeUserAssignment),
			() -> getUserRoleFacade().saveUserRole(finalUserRole));
	}

	/**
	 * The user rights sheet lists the required rights of each right in declaration order, so that the export is the same
	 * on every server start (the underlying set iterates in hash order).
	 */
	@Test
	public void testGenerateUserRolesDocumentListsRequiredRightsInDeclarationOrder() throws Exception {

		int multiMemberCells = 0;
		try (XSSFWorkbook workbook = new XSSFWorkbook(new File(getUserRoleFacade().generateUserRolesDocument()))) {
			Sheet rightsSheet = workbook.getSheetAt(1);
			assertEquals(UserRight.values().length, rightsSheet.getLastRowNum());
			for (Row row : rightsSheet) {
				if (row.getRowNum() == 0) {
					continue;
				}
				UserRight userRight = UserRight.valueOf(row.getCell(0).getStringCellValue());
				String cell = row.getCell(4).getStringCellValue();
				List<UserRight> listed =
					cell.isEmpty() ? Collections.emptyList() : Arrays.stream(cell.split(", ")).map(UserRight::valueOf).collect(Collectors.toList());

				assertEquals(UserRight.getRequiredUserRights(Collections.singleton(userRight)), new HashSet<>(listed), userRight.name());
				assertEquals(listed.stream().sorted().collect(Collectors.toList()), listed, userRight.name());
				if (listed.size() > 1) {
					multiMemberCells++;
				}
			}
		}
		assertTrue(multiMemberCells > 0);
	}
}
