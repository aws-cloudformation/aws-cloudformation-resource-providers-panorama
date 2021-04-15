package software.amazon.panorama.applicationinstance;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.AccessDeniedException;
import software.amazon.awssdk.services.panorama.model.ConflictException;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.InternalServerException;
import software.amazon.awssdk.services.panorama.model.ResourceNotFoundException;
import software.amazon.awssdk.services.panorama.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<PanoramaClient> proxyClient,
        final Logger logger
    ) {
        this.logger = logger;
        return proxy.initiate("AWS-Panorama-ApplicationInstance::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((describeApplicationInstanceRequest, client) -> {
                    DescribeApplicationInstanceResponse describeApplicationInstanceResponse;

                    try {
                        describeApplicationInstanceResponse = client.injectCredentialsAndInvokeV2(describeApplicationInstanceRequest, client.client()::describeApplicationInstance);
                    } catch (ConflictException e) {
                        throw new CfnResourceConflictException(e);
                    } catch (ValidationException e) {
                        throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
                    } catch (AccessDeniedException e) {
                        throw new CfnAccessDeniedException(ResourceModel.TYPE_NAME, e);
                    } catch (ResourceNotFoundException e) {
                        throw new CfnNotFoundException(ResourceModel.TYPE_NAME, describeApplicationInstanceRequest.applicationInstanceId(), e);
                    } catch (InternalServerException e) {
                        throw new CfnInternalFailureException(e);
                    } catch (AwsServiceException e) {
                        throw new CfnGeneralServiceException("DescribeApplicationInstance", e);
                    }

                    logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                    return describeApplicationInstanceResponse;
                })
                .done(response -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(response)));
    }
}
