package software.amazon.panorama.package_;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.CreatePackageRequest;
import software.amazon.awssdk.services.panorama.model.CreatePackageResponse;
import software.amazon.awssdk.services.panorama.model.DescribePackageRequest;
import software.amazon.awssdk.services.panorama.model.PanoramaException;
import software.amazon.awssdk.services.panorama.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.function.BiFunction;
import java.util.function.Function;


public class CreateHandler extends BaseHandlerStd {
    protected static final BiFunction<ResourceModel, ProxyClient<PanoramaClient>, ResourceModel> EMPTY_CALL =
            (model, proxyClient) -> model;

    private static final String OPERATION = "CreatePackage";
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
                        proxy.initiate("AWS-Panorama-Package::Create", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToCreateRequest)
                                .makeServiceCall(this::createNodePackageAccess)
                                .done(this::setPackageId)
                )
                .then(progress -> stabilize(proxy, proxyClient, progress))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param createPackageRequest the Panorama service request to create NodePackage
     * @param proxyClient the Panorama service client to make the call
     * @return create resource response
     */
    private CreatePackageResponse createNodePackageAccess(
            final CreatePackageRequest createPackageRequest,
            final ProxyClient<PanoramaClient> proxyClient
    ) {
        CreatePackageResponse createPackageResponse;
        try {
            createPackageResponse = proxyClient.injectCredentialsAndInvokeV2(createPackageRequest,
                    proxyClient.client()::createPackage);
        } catch(PanoramaException e) {
            logger.error(String.format("API Exception is thrown from Panorama service. PackageName: %s. Request: %s",
                    createPackageRequest.packageName(), createPackageRequest.toString()));
            throw PanoramaExceptionTranslator.translateForAPIException(e,
                    OPERATION,
                    ResourceModel.TYPE_NAME,
                    createPackageRequest.packageName(),
                    createPackageRequest.toString());
        } catch(AwsServiceException e) {
            logger.error(String.format("Exception happened when creating package. PackageName: %s. Request: %s",
                    createPackageRequest.packageName(), createPackageRequest.toString()));
            throw new CfnGeneralServiceException(OPERATION, e);
        }

        logger.info(String.format("%s with PackageName %s successfully created.", ResourceModel.TYPE_NAME, createPackageRequest.packageName()));
        return createPackageResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> stabilize(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<PanoramaClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        return proxy.initiate("AWS-Panorama-Package::stabilize", proxyClient, progress.getResourceModel(),
                progress.getCallbackContext())
                .translateToServiceRequest(Function.identity())
                .makeServiceCall(EMPTY_CALL)
                .stabilize((resourceModel, response, proxyInvocation, model, callbackContext) ->
                        isStabilized(proxyInvocation, model)).progress();
    }

    private boolean isStabilized(final ProxyClient<PanoramaClient> proxyClient, final ResourceModel model) {

        logger.info(String.format("Checking stabilization status for package %s with packageName %s", model.getPackageId(), model.getPackageName()));

        final DescribePackageRequest describePackageRequest = Translator.translateToReadRequest(model);

        try {
            proxyClient.injectCredentialsAndInvokeV2(describePackageRequest, proxyClient.client()::describePackage);
        } catch (ResourceNotFoundException e) {
            return false;
        } catch (PanoramaException e) {
            this.logger.error(String.format("API Exception is thrown from Panorama service. PackageId: %s. Request: %s",
                    describePackageRequest.packageId(), describePackageRequest.toString()));
            throw PanoramaExceptionTranslator.translateForAPIException(e,
                    OPERATION,
                    ResourceModel.TYPE_NAME,
                    describePackageRequest.packageId(),
                    describePackageRequest.toString());
        } catch(AwsServiceException e) {
            this.logger.error(String.format("Exception happened during DescribePackage. PackageId: %s. Request: %s",
                    model.getPackageId(), describePackageRequest.toString()));
            throw new CfnGeneralServiceException(OPERATION, e);
        }

        return true;
    }

    private ProgressEvent<ResourceModel, CallbackContext> setPackageId(CreatePackageRequest createPackageRequest,
                                                                       CreatePackageResponse createPackageResponse,
                                                                       ProxyClient<PanoramaClient> proxyClient,
                                                                       ResourceModel resourceModel,
                                                                       CallbackContext callbackContext) {
        resourceModel.setPackageId(createPackageResponse.packageId());
        return ProgressEvent.progress(resourceModel, callbackContext);
    }
}
