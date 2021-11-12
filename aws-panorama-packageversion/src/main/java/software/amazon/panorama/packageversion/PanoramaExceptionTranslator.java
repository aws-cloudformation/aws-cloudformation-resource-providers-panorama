package software.amazon.panorama.packageversion;

import software.amazon.awssdk.services.panorama.model.AccessDeniedException;
import software.amazon.awssdk.services.panorama.model.ConflictException;
import software.amazon.awssdk.services.panorama.model.InternalServerException;
import software.amazon.awssdk.services.panorama.model.PanoramaException;
import software.amazon.awssdk.services.panorama.model.ResourceNotFoundException;
import software.amazon.awssdk.services.panorama.model.ValidationException;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;

public class PanoramaExceptionTranslator {

    public static BaseHandlerException translateForAPIException(final PanoramaException e,
                                                                final String operation,
                                                                final String resourceTypeName,
                                                                final String resourceIdentifier,
                                                                final String requestBody
    ) {
        if (e instanceof ValidationException) {
            return new CfnInvalidRequestException(requestBody, e);
        } else if (e instanceof ConflictException) {
            return new CfnResourceConflictException(resourceTypeName, resourceIdentifier,
                    String.format("%s already existed", resourceIdentifier), e);
        } else if (e instanceof AccessDeniedException) {
            return new CfnAccessDeniedException(e);
        } else if (e instanceof InternalServerException) {
            return new CfnInternalFailureException(e);
        } else if (e instanceof ResourceNotFoundException) {
            return new CfnNotFoundException(resourceTypeName, resourceIdentifier, e);
        }

        return new CfnGeneralServiceException(operation, e);
    }
}
