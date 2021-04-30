package software.amazon.panorama.applicationinstance;

import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.panorama.model.AccessDeniedException;
import software.amazon.awssdk.services.panorama.model.ApplicationInstanceStatus;
import software.amazon.awssdk.services.panorama.model.CreateApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.CreateApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.InternalServerException;
import software.amazon.awssdk.services.panorama.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.panorama.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
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

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<PanoramaClient> proxyClient,
        final Logger logger
    ) {
        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-Panorama-ApplicationInstance::Create", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToCreateRequest)
                                .makeServiceCall(this::createApplicationInstance)
                                .done(this::setApplicationInstanceId)
                )
                .then(progress -> stabilize(proxy, proxyClient, progress))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param createApplicationInstanceRequest Panorama service request to create an ApplicationInstance
     * @param proxyClient Panorama client to make the call
     * @return create ApplicationInstance response
     */
    private CreateApplicationInstanceResponse createApplicationInstance(
            final CreateApplicationInstanceRequest createApplicationInstanceRequest,
            final ProxyClient<PanoramaClient> proxyClient
    ) {
        CreateApplicationInstanceResponse createApplicationInstanceResponse;
        try {
            createApplicationInstanceResponse = proxyClient.injectCredentialsAndInvokeV2(createApplicationInstanceRequest,
                    proxyClient.client()::createApplicationInstance);
        } catch (final ValidationException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
        } catch (final AccessDeniedException e) {
            throw new CfnAccessDeniedException(ResourceModel.TYPE_NAME, e);
        } catch (final ServiceQuotaExceededException e) {
            throw new CfnServiceLimitExceededException(e);
        } catch (final InternalServerException e) {
            throw new CfnInternalFailureException(e);
        } catch (final AwsServiceException e) {
            throw new CfnGeneralServiceException("CreateApplicationInstance", e);
        }

        logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
        return createApplicationInstanceResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> stabilize(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<PanoramaClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        return proxy.initiate("AWS-Panorama-ApplicationInstance::stabilize", proxyClient, progress.getResourceModel(),
                progress.getCallbackContext())
                .translateToServiceRequest(Function.identity())
                .makeServiceCall(EMPTY_CALL)
                .stabilize((resourceModel, response, proxyInvocation, model, callbackContext) ->
                        isStabilized(proxyInvocation, model)).progress();
    }

    private boolean isStabilized(final ProxyClient<PanoramaClient> proxyClient, final ResourceModel model) {
        DescribeApplicationInstanceRequest describeApplicationInstanceRequest = DescribeApplicationInstanceRequest.builder()
                .applicationInstanceId(model.getApplicationInstanceId())
                .build();

        DescribeApplicationInstanceResponse describeApplicationInstanceResponse = proxyClient.injectCredentialsAndInvokeV2(describeApplicationInstanceRequest,
                proxyClient.client()::describeApplicationInstance);

        ApplicationInstanceStatus applicationInstanceStatus = describeApplicationInstanceResponse.status();
        if (applicationInstanceStatus.equals(ApplicationInstanceStatus.DEPLOYMENT_FAILED)) {
            throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, model.getApplicationInstanceId());
        }

        return applicationInstanceStatus.equals(ApplicationInstanceStatus.DEPLOYMENT_SUCCEEDED);
    }

    private ProgressEvent<ResourceModel, CallbackContext> setApplicationInstanceId(CreateApplicationInstanceRequest createApplicationInstanceRequest,
                                                                CreateApplicationInstanceResponse createApplicationInstanceResponse,
                                                                ProxyClient<PanoramaClient> proxyClient,
                                                                ResourceModel resourceModel,
                                                                CallbackContext callbackContext
    ) {
        resourceModel.setApplicationInstanceId(createApplicationInstanceResponse.applicationInstanceId());
        return ProgressEvent.progress(resourceModel, callbackContext);
    }
}
