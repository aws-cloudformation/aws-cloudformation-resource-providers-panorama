package software.amazon.panorama.packageversion;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.DescribePackageVersionRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageVersionResponse;
import software.amazon.awssdk.services.panorama.model.PackageVersionStatus;
import software.amazon.awssdk.services.panorama.model.PanoramaException;
import software.amazon.awssdk.services.panorama.model.RegisterPackageVersionRequest;
import software.amazon.awssdk.services.panorama.model.RegisterPackageVersionResponse;
import software.amazon.awssdk.services.panorama.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class RegisterPackageVersionBaseHandler extends BaseHandlerStd {
    private static final String OPERATION = "RegisterPackageVersion";
    protected static final BiFunction<ResourceModel, ProxyClient<PanoramaClient>, ResourceModel> EMPTY_CALL =
            (model, proxyClient) -> model;

    protected LoggerWrapper logger;

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param request the aws service request to create a resource
     * @param proxyClient the Panorama service client to make the call
     * @return RegisterPackageVersionResponse register PackageVersion response
     */
    protected RegisterPackageVersionResponse registerPackageVersion(
            final RegisterPackageVersionRequest request,
            final ProxyClient<PanoramaClient> proxyClient) {
        RegisterPackageVersionResponse registerPackageVersionResponse;
        try {
            registerPackageVersionResponse = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::registerPackageVersion);
        } catch (final PanoramaException e) {
            logger.error(String.format("API Exception is thrown from Panorama service. PackageId: %s, PackageVersion: %s, PatchVersion: %s. Request: %s",
                    request.packageId(), request.packageVersion(), request.patchVersion(), request.toString()));
            throw PanoramaExceptionTranslator.translateForAPIException(e,
                    OPERATION,
                    ResourceModel.TYPE_NAME,
                    String.format("%s-%s-%s", request.packageId(), request.packageVersion(), request.patchVersion()),
                    request.toString());
        }  catch (final AwsServiceException e) {
            /*
             * While the handler contract states that the handler must always return a progress event,
             * you may throw any instance of BaseHandlerException, as the wrapper map it to a progress event.
             * Each BaseHandlerException maps to a specific error code, and you should map service exceptions as closely as possible
             * to more specific error codes
             */
            logger.error(String.format("Exception happened when registering PackageVersion. PackageId: %s, PackageVersion: %s, PatchVersion: %s. Request: %s",
                    request.packageId(), request.packageVersion(), request.patchVersion(), request.toString()));
            throw new CfnGeneralServiceException(OPERATION, e);
        }

        logger.info(String.format("%s successfully created for PackageId: %s, PackageVersion: %s, PatchVersion: %s",
                ResourceModel.TYPE_NAME, request.packageId(), request.packageVersion(), request.patchVersion()));
        return registerPackageVersionResponse;
    }

    protected RegisterPackageVersionResponse checkExistenceAndRegisterPackageVersion(
            final RegisterPackageVersionRequest request,
            final ProxyClient<PanoramaClient> proxyClient) {

        DescribePackageVersionRequest describePackageVersionRequest =  DescribePackageVersionRequest.builder()
                .ownerAccount(request.ownerAccount())
                .packageId(request.packageId())
                .packageVersion(request.packageVersion())
                .patchVersion(request.patchVersion())
                .build();
        try {
            proxyClient.injectCredentialsAndInvokeV2(describePackageVersionRequest, proxyClient.client()::describePackageVersion);
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, String.format("%s-%s-%s",
                    describePackageVersionRequest.packageId(), describePackageVersionRequest.packageVersion(), describePackageVersionRequest.patchVersion()));
        } catch (ResourceNotFoundException e) {
            logger.info("Resource does not exist, creating...");
        }
        return registerPackageVersion(request, proxyClient);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> stabilize(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<PanoramaClient> proxyClient,
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        return proxy.initiate("AWS-Panorama-PackageVersion::stabilize", proxyClient, progress.getResourceModel(),
                progress.getCallbackContext())
                .translateToServiceRequest(Function.identity())
                .makeServiceCall(EMPTY_CALL)
                .stabilize((resourceModel, response, proxyInvocation, model, callbackContext) ->
                        isStabilized(proxyInvocation, model)).progress();
    }

    private boolean isStabilized(final ProxyClient<PanoramaClient> proxyClient,
                                 final ResourceModel model) {
        DescribePackageVersionRequest describePackageVersionRequest = Translator.translateToReadRequest(model);
        DescribePackageVersionResponse describePackageVersionResponse = proxyClient.injectCredentialsAndInvokeV2(describePackageVersionRequest,
                proxyClient.client()::describePackageVersion);

        PackageVersionStatus packageVersionStatus = describePackageVersionResponse.status();
        if (packageVersionStatus.equals(PackageVersionStatus.FAILED)) {
            logger.error(String.format("RegisterPackageVersion failed because PackageVersionStatus shows FAILED for PackageId: %s, PackageVersion: %s, PatchVersion: %s",
                    model.getPackageId(), model.getPackageVersion(), model.getPatchVersion()));
            throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME,
                    String.format("PackageId: %s, PackageVersion: %s, PatchVersion: %s", model.getPackageId(), model.getPackageVersion(), model.getPatchVersion()));
        }

        return packageVersionStatus.equals(PackageVersionStatus.REGISTER_COMPLETED);
    }
}
