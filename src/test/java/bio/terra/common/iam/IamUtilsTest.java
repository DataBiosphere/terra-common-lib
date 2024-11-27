package bio.terra.common.iam;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IamUtilsTest {

	@Test
	public void isServiceAccountWithValidServiceAccountEmail() {
		assertTrue(IamUtils.isServiceAccount("user@gserviceaccount.com"));
	}

	@Test
	public void isServiceAccountWithInvalidServiceAccountEmail() {
		assertFalse(IamUtils.isServiceAccount("user@verily.com"));
	}

	@Test
	public void isServiceAccountWithWhitespaceInEmail() {
		assertFalse(IamUtils.isServiceAccount(" user@gserviceaccount.com"));
	}

	@Test
	public void isServiceAccountWithEmptyEmail() {
		assertFalse(IamUtils.isServiceAccount(""));
	}

	@Test
	public void isServiceAccountWithNullEmail() {
		assertFalse(IamUtils.isServiceAccount(null));
	}
}