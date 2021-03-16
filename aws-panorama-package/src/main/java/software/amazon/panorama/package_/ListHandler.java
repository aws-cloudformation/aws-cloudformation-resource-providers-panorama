package software.amazon.panorama.package_;

import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.ListPackagesRequest;
import software.amazon.awssdk.services.panorama.model.ListPackagesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandlerStd {
    private static Integer MAX_RESULTS = 10;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<PanoramaClient> proxyClient,
            final Logger logger) {

        final ListPackagesRequest listPackagesRequest = Translator.translateToListRequest(request.getNextToken(), MAX_RESULTS);

        ListPackagesResponse listPackagesResponse = proxy.injectCredentialsAndInvokeV2(listPackagesRequest, proxyClient.client()::listPackages);

        String nextToken = listPackagesResponse.nextToken();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(Translator.translateFromListResponse(listPackagesResponse))
                .nextToken(nextToken)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
