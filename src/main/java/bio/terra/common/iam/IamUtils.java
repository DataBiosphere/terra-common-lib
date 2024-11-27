package bio.terra.common.iam;

public final class IamUtils {

	/**
	 * Check whether the provided email address is a service account.
	 *
	 * @param email
	 * @return true if the email address is a service account
	 */
	public static boolean isServiceAccount(String email) {
		if (email == null) {
			return false;
		}
		return email.matches("\\S+@\\S*gserviceaccount\\.com$");
	}

}
