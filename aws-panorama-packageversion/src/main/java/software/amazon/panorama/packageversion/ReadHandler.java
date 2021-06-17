package software.amazon.panorama.packageversion;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.DescribePackageVersionRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageVersionResponse;
import software.amazon.awssdk.services.panorama.model.PanoramaException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private static final String OPERATION = "DescribePackageVersion";
    private LoggerWrapper logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<PanoramaClient> proxyClient,
            final Logger logger) {

        this.logger = new LoggerWrapper(logger);

        final ResourceModel model = request.getDesiredResourceState();
        final DescribePackageVersionRequest describePackageVersionRequest = Translator.translateToReadRequest(model);
        DescribePackageVersionResponse describePackageVersionResponse;
        try {
            describePackageVersionResponse = proxyClient.injectCredentialsAndInvokeV2(
                    describePackageVersionRequest, proxyClient.client()::describePackageVersion);
        } catch (final PanoramaException e) {
            this.logger.error(String.format("API Exception is thrown from Panorama service. PackageId: %s, PackageVersion: %s, PatchVersion: %s. Request: %s",
                    model.getPackageId(), model.getPackageVersion(), model.getPatchVersion(), request.toString()));

            throw PanoramaExceptionTranslator.translateForAPIException(e,
                    OPERATION,
                    ResourceModel.TYPE_NAME,
                    String.format("%s-%s-%s", describePackageVersionRequest.packageId(), describePackageVersionRequest.packageVersion(), describePackageVersionRequest.patchVersion()),
                    describePackageVersionRequest.toString());
        } catch (final AwsServiceException e) {
            /*
             * While the handler contract states that the handler must always return a progress event,
             * you may throw any instance of BaseHandlerException, as the wrapper map it to a progress event.
             * Each BaseHandlerException maps to a specific error code, and you should map service exceptions as closely as possible
             * to more specific error codes
             */
            this.logger.error(String.format("Exception happened when reading PackageVersion. PackageId: %s, PackageVersion: %s, PatchVersion: %s. Request: %s",
                    model.getPackageId(), model.getPackageVersion(), model.getPatchVersion(), request.toString()));
            throw new CfnGeneralServiceException("DescribePackageVersion", e);
        }

        return ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(describePackageVersionResponse));
    }
}
