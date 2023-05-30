package bio.terra.common.flagsmith;

import bio.terra.common.exception.InternalServerErrorException;

public class FlagsmithFeatureFetchingException extends InternalServerErrorException {

  public FlagsmithFeatureFetchingException(String message, Exception e) {
    super(message, e);
  }
}
