package software.amazon.panorama.packageversion;

import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.DescribePackageVersionRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageVersionResponse;
import software.amazon.awssdk.services.panorama.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotUpdatableException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class UpdateHandler extends RegisterPackageVersionBaseHandler {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<PanoramaClient> proxyClient,
            final Logger logger) {
        super.logger = new LoggerWrapper(logger);

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-Panorama-PackageVersion::ValidateResourceExists", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDescribeRequestForUpdate)
                                .makeServiceCall(this::validateResourceUpdatable)
                                .progress()
                )
                .then(progress ->
                        proxy.initiate("AWS-Panorama-PackageVersion::RegisterPackageVersion", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToCreateRequest)
                                .makeServiceCall(super::registerPackageVersion)
                                .done((registerPackageVersionRequest, registerPackageVersionResponse, client, resourceModel, context) -> ProgressEvent.progress(resourceModel, context))
                )
                .then(progress -> super.stabilize(proxy, proxyClient, progress))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private DescribePackageVersionResponse validateResourceUpdatable(DescribePackageVersionRequest describePackageVersionRequest, ProxyClient<PanoramaClient> proxyClient) {
        DescribePackageVersionResponse describePackageVersionResponse;
        try {
            describePackageVersionResponse = proxyClient.injectCredentialsAndInvokeV2(describePackageVersionRequest, proxyClient.client()::describePackageVersion);
            if (describePackageVersionResponse.isLatestPatch()) {
                throw new CfnNotUpdatableException(ResourceModel.TYPE_NAME, String.format("%s-%s-%s",
                        describePackageVersionRequest.packageId(), describePackageVersionRequest.packageVersion(), describePackageVersionRequest.patchVersion()));
            }
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME,
                    String.format("%s-%s-%s", describePackageVersionRequest.packageId(), describePackageVersionRequest.packageVersion(), describePackageVersionRequest.patchVersion()), e);
        }
        return describePackageVersionResponse;
    }

}
