package software.amazon.panorama.package_;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.ConflictException;
import software.amazon.awssdk.services.panorama.model.DeletePackageRequest;
import software.amazon.awssdk.services.panorama.model.DeletePackageResponse;
import software.amazon.awssdk.services.panorama.model.DescribePackageRequest;
import software.amazon.awssdk.services.panorama.model.PanoramaException;
import software.amazon.awssdk.services.panorama.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private static final String OPERATION = "DeletePackage";
    private LoggerWrapper logger;

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
                        proxy.initiate("AWS-Panorama-Package::Delete", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteRequest)
                                .makeServiceCall(this::deletePackage)
                                .stabilize(this::stabilizedOnDelete)
                                .done(this::setResourceModelToNullAndReturnSuccess));
    }

    private ProgressEvent<ResourceModel, CallbackContext> setResourceModelToNullAndReturnSuccess(
            DeletePackageRequest deletePackageRequest,
            DeletePackageResponse deletePackageResponse,
            ProxyClient<PanoramaClient> proxyClient,
            ResourceModel resourceModel,
            CallbackContext callbackContext
    ) {
        return ProgressEvent.defaultSuccessHandler(null);
    }

    /**
     * Implement client invocation of the delete request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param deletePackageRequest the aws service request to delete a resource
     * @param proxyClient the aws service client to make the call
     * @return delete resource response
     */
    private DeletePackageResponse deletePackage(
            final DeletePackageRequest deletePackageRequest,
            final ProxyClient<PanoramaClient> proxyClient
    ) {
        DeletePackageResponse deletePackageResponse;
        try {
            deletePackageResponse = proxyClient.injectCredentialsAndInvokeV2(
                    deletePackageRequest, proxyClient.client()::deletePackage);
        } catch (PanoramaException e) {
            logger.error(String.format("API Exception is thrown from Panorama service. PackageId: %s. Request: %s",
                    deletePackageRequest.packageId(), deletePackageRequest.toString()));
            throw PanoramaExceptionTranslator.translateForAPIException(e,
                    OPERATION,
                    ResourceModel.TYPE_NAME,
                    deletePackageRequest.packageId(),
                    deletePackageRequest.toString()
            );
        } catch(AwsServiceException e) {
            logger.error(String.format("Exception happened when deleting package. PackageId: %s. Request: %s",
                    deletePackageRequest.packageId(), deletePackageRequest.toString()));
            throw new CfnGeneralServiceException(OPERATION, e);
        }

        logger.info(String.format("%s with PackageId %s successfully deleted.", ResourceModel.TYPE_NAME, deletePackageRequest.packageId()));
        return deletePackageResponse;
    }

    private boolean stabilizedOnDelete(
            DeletePackageRequest deletePackageRequest,
            DeletePackageResponse deletePackageResponse,
            final ProxyClient<PanoramaClient> proxyClient,
            final software.amazon.panorama.package_.ResourceModel model,
            final software.amazon.panorama.package_.CallbackContext callbackContext) {

        logger.info(String.format("Checking delete stabilization status for package %s with packageName %s in delete handler", model.getPackageId(), model.getPackageName()));

        try {
            proxyClient.injectCredentialsAndInvokeV2(
                    deletePackageRequest, proxyClient.client()::deletePackage);
        } catch (ResourceNotFoundException e) {
            return true;
        } catch (ConflictException e) {
            return false;
        } catch (PanoramaException e) {
            throw PanoramaExceptionTranslator.translateForAPIException(e,
                    OPERATION,
                    ResourceModel.TYPE_NAME,
                    deletePackageRequest.packageId(),
                    deletePackageRequest.toString());
        } catch(AwsServiceException e) {
            logger.error(String.format("Exception happened during DescribePackage in delete handler. PackageId: %s. Request: %s",
                    model.getPackageId(), deletePackageRequest.toString()));
            throw new CfnGeneralServiceException(OPERATION, e);
        }
        return false;
    }
}
