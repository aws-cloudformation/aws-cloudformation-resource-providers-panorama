package software.amazon.panorama.applicationinstance;

import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.ListApplicationInstancesRequest;
import software.amazon.awssdk.services.panorama.model.ListApplicationInstancesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<PanoramaClient> proxyClient,
            final Logger logger
    ) {
        final ResourceModel model = request.getDesiredResourceState();
        final String deviceId = model.getDeviceId();
        final String statusFilter = model.getStatusFilter();

        final ListApplicationInstancesRequest listApplicationInstancesRequest =
                Translator.translateToListRequest(deviceId, statusFilter, request.getNextToken());

        ListApplicationInstancesResponse listApplicationInstancesResponse = proxy.injectCredentialsAndInvokeV2(listApplicationInstancesRequest,
                proxyClient.client()::listApplicationInstances);

        String nextToken = listApplicationInstancesResponse.nextToken();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(Translator.translateFromListResponse(listApplicationInstancesResponse))
                .nextToken(nextToken)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
