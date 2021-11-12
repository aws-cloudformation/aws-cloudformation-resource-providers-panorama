package software.amazon.panorama.packageversion;

import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class CreateHandler extends RegisterPackageVersionBaseHandler {

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
                        proxy.initiate("AWS-Panorama-PackageVersion::RegisterPackageVersion", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToCreateRequest)
                                .makeServiceCall(super::checkExistenceAndRegisterPackageVersion)
                                .done((registerPackageVersionRequest, registerPackageVersionResponse, client, resourceModel, context) -> ProgressEvent.progress(resourceModel, context))
                )
                .then(progress -> super.stabilize(proxy, proxyClient, progress))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
