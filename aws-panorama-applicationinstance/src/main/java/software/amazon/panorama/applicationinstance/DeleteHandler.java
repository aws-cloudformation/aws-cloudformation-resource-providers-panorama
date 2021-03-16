package software.amazon.panorama.applicationinstance;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.ApplicationInstanceStatus;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.PanoramaException;
import software.amazon.awssdk.services.panorama.model.RemoveApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.RemoveApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.ResourceNotFoundException;
import software.amazon.awssdk.services.panorama.model.ConflictException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Delay;

import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;

public class DeleteHandler extends BaseHandlerStd {
    private static final Constant STABILIZATION_DELAY = Constant.of()
            .timeout(Duration.ofDays(1L))
            .delay(Duration.ofMinutes(1))
            .build();

    private LoggerWrapper logger;
    private Delay delay;

    public DeleteHandler() {
        super();
        delay = STABILIZATION_DELAY;
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<PanoramaClient> proxyClient,
            final Logger logger
    ) {
        this.logger = new LoggerWrapper(logger);
        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-Panorama-ApplicationInstance::Remove", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .backoffDelay(delay)
                                .makeServiceCall(this::deleteApplicationInstance)
                                .stabilize(this::stabilizedOnDelete)
                                .done(this::setResourceModelToNullAndReturnSuccess)
                );
    }

    private ProgressEvent<ResourceModel, CallbackContext> setResourceModelToNullAndReturnSuccess(
            RemoveApplicationInstanceRequest removeApplicationInstanceRequest,
            RemoveApplicationInstanceResponse removeApplicationInstanceResponse,
            ProxyClient<PanoramaClient> proxyClient,
            ResourceModel resourceModel,
            CallbackContext callbackContext
    ) {
        return ProgressEvent.defaultSuccessHandler(null);
    }

    /**
     * Implement client invocation of the delete request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param removeApplicationInstanceRequest the aws service request to delete a resource
     * @param proxyClient the aws service client to make the call
     * @return delete resource response
     */
    private RemoveApplicationInstanceResponse deleteApplicationInstance(
            final RemoveApplicationInstanceRequest removeApplicationInstanceRequest,
            final ProxyClient<PanoramaClient> proxyClient
    ) {
        RemoveApplicationInstanceResponse removeApplicationInstanceResponse;
        try {
            removeApplicationInstanceResponse = proxyClient.injectCredentialsAndInvokeV2(
                    removeApplicationInstanceRequest, proxyClient.client()::removeApplicationInstance);
        }
        catch (final PanoramaException e){
            if (e instanceof ConflictException) {
                // removing an application instance in "Removal_Success" status will throw ConflictException, we need extra handling here
                DescribeApplicationInstanceRequest describeApplicationInstanceRequest = DescribeApplicationInstanceRequest.builder()
                        .applicationInstanceId(removeApplicationInstanceRequest.applicationInstanceId())
                        .build();
                DescribeApplicationInstanceResponse describeApplicationInstanceResponse = proxyClient.injectCredentialsAndInvokeV2(describeApplicationInstanceRequest, proxyClient.client()::describeApplicationInstance);
                ApplicationInstanceStatus applicationInstanceStatus = describeApplicationInstanceResponse.status();
                if (applicationInstanceStatus.equals(ApplicationInstanceStatus.REMOVAL_SUCCEEDED)) {
                    logger.info("ApplicationInstanceStatus.REMOVAL_SUCCEEDED!  throw CfnNotFoundException");
                    throw new CfnNotFoundException(ResourceModel.TYPE_NAME, removeApplicationInstanceRequest.applicationInstanceId(), e);
                }
            }
            logger.error(String.format("Exception happened when removing ApplicationInstance. ApplicationInstanceId: %s",
                    removeApplicationInstanceRequest.applicationInstanceId()));
            throw PanoramaExceptionTranslator.translateForAPIException(e,
                    "RemoveApplicationInstance",
                    ResourceModel.TYPE_NAME,
                    removeApplicationInstanceRequest.applicationInstanceId(),
                    removeApplicationInstanceRequest.toString());
        }  catch (final AwsServiceException e) {
            logger.error(String.format("Exception happened when removing ApplicationInstance. ApplicationInstanceId: %s",
                    removeApplicationInstanceRequest.applicationInstanceId()));
            throw new CfnGeneralServiceException("RemoveApplicationInstance", e);
        }

        logger.info(String.format("%s successfully removed with ApplicationInstanceId: %s.", ResourceModel.TYPE_NAME, removeApplicationInstanceRequest.applicationInstanceId()));
        return removeApplicationInstanceResponse;
    }

    /**
     * Stabilize removing ApplicationInstance
     *
     * @param removeApplicationInstanceRequest the aws service request to delete a resource
     * @param removeApplicationInstanceResponse the aws service response to delete a resource
     * @param proxyClient the aws service client to make the call
     * @param model resource model
     * @param callbackContext callback context
     * @return boolean state of stabilized or not
     */
    private boolean stabilizedOnDelete(
            final RemoveApplicationInstanceRequest removeApplicationInstanceRequest,
            final RemoveApplicationInstanceResponse removeApplicationInstanceResponse,
            final ProxyClient<PanoramaClient> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext
    ) {
        DescribeApplicationInstanceRequest describeApplicationInstanceRequest = DescribeApplicationInstanceRequest.builder()
                .applicationInstanceId(model.getApplicationInstanceId())
                .build();

        boolean stabilized = false;
        try {
            DescribeApplicationInstanceResponse describeApplicationInstanceResponse = proxyClient.injectCredentialsAndInvokeV2(describeApplicationInstanceRequest, proxyClient.client()::describeApplicationInstance);
            ApplicationInstanceStatus applicationInstanceStatus = describeApplicationInstanceResponse.status();
            if (applicationInstanceStatus.equals(ApplicationInstanceStatus.REMOVAL_FAILED)) {
                logger.error(String.format("ApplicationInstance removal failed with ApplicationInstanceId: %s", removeApplicationInstanceRequest.applicationInstanceId()));
                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, model.getApplicationInstanceId());
            }

            if (applicationInstanceStatus.equals(ApplicationInstanceStatus.REMOVAL_SUCCEEDED)) {
                stabilized = true;
            }
        } catch (ResourceNotFoundException e) {
            stabilized = true;
        }

        logger.info(String.format("%s deletion with ApplicationInstanceId %s has stabilized", ResourceModel.TYPE_NAME, model.getApplicationInstanceId()));
        return stabilized;
    }
}
