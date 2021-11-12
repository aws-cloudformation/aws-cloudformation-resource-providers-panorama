package software.amazon.panorama.package_;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.DescribePackageRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageResponse;
import software.amazon.awssdk.services.panorama.model.PanoramaException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private static final String OPERATION = "DescribePackage";
    private LoggerWrapper logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<PanoramaClient> proxyClient,
            final Logger logger) {

        this.logger = new LoggerWrapper(logger);

        final ResourceModel model = request.getDesiredResourceState();
        final DescribePackageRequest describePackageRequest = Translator.translateToReadRequest(model);

        DescribePackageResponse describePackageResponse;

        try {
            describePackageResponse = proxyClient.injectCredentialsAndInvokeV2(describePackageRequest,
                    proxyClient.client()::describePackage);
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

        return ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(describePackageResponse));
    }
}
