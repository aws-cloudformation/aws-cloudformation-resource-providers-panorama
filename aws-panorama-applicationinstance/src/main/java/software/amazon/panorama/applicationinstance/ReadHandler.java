package software.amazon.panorama.applicationinstance;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceDetailsRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceDetailsResponse;
import software.amazon.awssdk.services.panorama.model.ApplicationInstanceStatus;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.PanoramaException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
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

        final DescribeApplicationInstanceRequest describeApplicationInstanceRequest = Translator.translateToReadRequest(model);
        DescribeApplicationInstanceResponse describeApplicationInstanceResponse;

        try {
            describeApplicationInstanceResponse = proxyClient.injectCredentialsAndInvokeV2(describeApplicationInstanceRequest,
                    proxyClient.client()::describeApplicationInstance);
            if (ApplicationInstanceStatus.REMOVAL_SUCCEEDED.equals(describeApplicationInstanceResponse.status())) {
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, describeApplicationInstanceResponse.applicationInstanceId());
            }
        } catch (PanoramaException e) {
            this.logger.error(String.format("Exception happened when reading ApplicationInstance. ApplicationInstanceId: %s",
                    describeApplicationInstanceRequest.applicationInstanceId()));
            throw PanoramaExceptionTranslator.translateForAPIException(e,
                    "DescribeApplicationInstance",
                    ResourceModel.TYPE_NAME,
                    describeApplicationInstanceRequest.applicationInstanceId(),
                    describeApplicationInstanceRequest.toString());
        }  catch (AwsServiceException e) {
            this.logger.error(String.format("Exception happened when reading ApplicationInstance. ApplicationInstanceId: %s",
                    describeApplicationInstanceRequest.applicationInstanceId()));
            throw new CfnGeneralServiceException("DescribeApplicationInstance", e);
        }

        DescribeApplicationInstanceDetailsRequest describeApplicationInstanceDetailsRequest =
                Translator.translateToDescribeApplicationInstanceDetails(model);
        DescribeApplicationInstanceDetailsResponse describeApplicationInstanceDetailsResponse;

        try {
            describeApplicationInstanceDetailsResponse = proxyClient.injectCredentialsAndInvokeV2(describeApplicationInstanceDetailsRequest,
                    proxyClient.client()::describeApplicationInstanceDetails);
        } catch (PanoramaException e) {
            this.logger.error(String.format("Exception happened when reading ApplicationInstance details. ApplicationInstanceId: %s",
                    describeApplicationInstanceDetailsRequest.applicationInstanceId()));
            throw PanoramaExceptionTranslator.translateForAPIException(e,
                    "DescribeApplicationInstanceDetails",
                    ResourceModel.TYPE_NAME,
                    describeApplicationInstanceDetailsRequest.applicationInstanceId(),
                    describeApplicationInstanceDetailsRequest.toString());
        }  catch (AwsServiceException e) {
            this.logger.error(String.format("Exception happened when reading ApplicationInstance details. ApplicationInstanceId: %s",
                    describeApplicationInstanceDetailsRequest.applicationInstanceId()));
            throw new CfnGeneralServiceException("describeApplicationInstanceDetails", e);
        }

        return ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(describeApplicationInstanceResponse, describeApplicationInstanceDetailsResponse));
    }
}
