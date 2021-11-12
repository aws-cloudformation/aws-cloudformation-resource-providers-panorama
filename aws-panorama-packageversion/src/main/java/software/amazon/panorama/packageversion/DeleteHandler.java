package software.amazon.panorama.packageversion;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.DeregisterPackageVersionRequest;
import software.amazon.awssdk.services.panorama.model.DeregisterPackageVersionResponse;
import software.amazon.awssdk.services.panorama.model.DescribePackageVersionRequest;
import software.amazon.awssdk.services.panorama.model.PanoramaException;
import software.amazon.awssdk.services.panorama.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private static final String OPERATION = "DeregisterPackageVersion";
    private LoggerWrapper logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<PanoramaClient> proxyClient,
            final Logger logger) {

        this.logger = new LoggerWrapper(logger);

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-Panorama-PackageVersion::DeregisterPackageVersion", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .makeServiceCall(this::deletePackageVersion)
                                .stabilize(this::stabilizedOnDelete)
                                .done(this::setResourceModelToNullAndReturnSuccess));
    }

    /**
     * Implement client invocation of the delete request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param request the Panorama service request to deregister a resource
     * @param proxyClient the Panorama service client to make the call
     * @return delete resource response
     */
    private DeregisterPackageVersionResponse deletePackageVersion(
            final DeregisterPackageVersionRequest request,
            final ProxyClient<PanoramaClient> proxyClient) {
        DeregisterPackageVersionResponse deletePackageVersionResponse;
        try {
            deletePackageVersionResponse = proxyClient.injectCredentialsAndInvokeV2(
                    request, proxyClient.client()::deregisterPackageVersion);
        } catch (final PanoramaException e) {
            logger.error(String.format("API Exception is thrown from Panorama service. PackageId: %s, PackageVersion: %s, PatchVersion: %s. Request: %s",
                    request.packageId(), request.packageVersion(), request.patchVersion(), request.toString()));
            throw PanoramaExceptionTranslator.translateForAPIException(e,
                    OPERATION,
                    ResourceModel.TYPE_NAME,
                    String.format("%s-%s-%s", request.packageId(), request.packageVersion(), request.patchVersion()),
                    request.toString());
        } catch (final AwsServiceException e) {
            /*
             * While the handler contract states that the handler must always return a progress event,
             * you may throw any instance of BaseHandlerException, as the wrapper map it to a progress event.
             * Each BaseHandlerException maps to a specific error code, and you should map service exceptions as closely as possible
             * to more specific error codes
             */
            logger.error(String.format("Exception happened when deregistering PackageVersion. PackageId: %s, PackageVersion: %s, PatchVersion: %s. Request: %s",
                    request.packageId(), request.packageVersion(), request.patchVersion(), request.toString()));
            throw new CfnGeneralServiceException(OPERATION, e);
        }

        logger.info(String.format("%s successfully deleted for PackageId: %s, PackageVersion: %s, PatchVersion: %s",
                ResourceModel.TYPE_NAME, request.packageId(), request.packageVersion(), request.patchVersion()));
        return deletePackageVersionResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> setResourceModelToNullAndReturnSuccess(
            DeregisterPackageVersionRequest deregisterPackageVersionRequest,
            DeregisterPackageVersionResponse deregisterPackageVersionResponse,
            ProxyClient<PanoramaClient> proxyClient,
            ResourceModel resourceModel,
            CallbackContext callbackContext) {
        return ProgressEvent.defaultSuccessHandler(null);
    }

    private boolean stabilizedOnDelete(
            final DeregisterPackageVersionRequest deregisterPackageVersionRequest,
            final DeregisterPackageVersionResponse deregisterPackageVersionResponse,
            final ProxyClient<PanoramaClient> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext) {

        DescribePackageVersionRequest describePackageVersionRequest = Translator.translateToReadRequest(model);

        boolean stabilized;
        try {
            proxyClient.injectCredentialsAndInvokeV2(describePackageVersionRequest, proxyClient.client()::describePackageVersion);
            stabilized = false;
        } catch (ResourceNotFoundException e) {
            stabilized = true;
        }

        logger.info(String.format("%s [%s-%s-%s] deregistration has stabilized: %s",
                ResourceModel.TYPE_NAME, model.getPackageId(), model.getPackageVersion(), model.getPatchVersion(), stabilized));
        return stabilized;
    }
}
