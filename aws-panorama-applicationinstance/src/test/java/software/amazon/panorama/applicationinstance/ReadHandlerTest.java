package software.amazon.panorama.applicationinstance;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.ApplicationInstanceHealthStatus;
import software.amazon.awssdk.services.panorama.model.ApplicationInstanceStatus;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.panorama.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.panorama.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_DESCRIPTION;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_ID;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_NAME;
import static software.amazon.panorama.applicationinstance.Constants.CREATED_TIME;
import static org.mockito.Mockito.when;
import static software.amazon.panorama.applicationinstance.Constants.DEVICE_ARN;
import static software.amazon.panorama.applicationinstance.Constants.LAST_UPDATED_TIME;
import static software.amazon.panorama.applicationinstance.Constants.RUNTIME_ROLE_ARN;
import static software.amazon.panorama.applicationinstance.Constants.STATUS_DESCRIPTION;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<PanoramaClient> proxyClient;

    @Mock
    PanoramaClient panoramaClient;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        panoramaClient = mock(PanoramaClient.class);
        proxyClient = MOCK_PROXY(proxy, panoramaClient);
    }

    @AfterEach
    public void tear_down() {
        verifyNoMoreInteractions(panoramaClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final ReadHandler handler = new ReadHandler();
        final ResourceModel model = ResourceModel.builder()
                .applicationInstanceId(APPLICATION_INSTANCE_ID)
                .build();

        when(proxyClient.client().describeApplicationInstance(any(DescribeApplicationInstanceRequest.class)))
                .thenReturn(DescribeApplicationInstanceResponse.builder()
                        .applicationInstanceId(APPLICATION_INSTANCE_ID)
                        .name(APPLICATION_INSTANCE_NAME)
                        .description(APPLICATION_INSTANCE_DESCRIPTION)
                        .defaultRuntimeContextDevice(DEVICE_ARN)
                        .runtimeRoleArn(RUNTIME_ROLE_ARN)
                        .status(ApplicationInstanceStatus.DEPLOYMENT_SUCCEEDED)
                        .healthStatus(ApplicationInstanceHealthStatus.RUNNING)
                        .statusDescription(STATUS_DESCRIPTION)
                        .createdTime(Instant.ofEpochSecond(CREATED_TIME))
                        .lastUpdatedTime(Instant.ofEpochSecond(LAST_UPDATED_TIME))
                        .build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        ResourceModel expectedResourceModel = ResourceModel.builder()
                .applicationInstanceId(APPLICATION_INSTANCE_ID)
                .name(APPLICATION_INSTANCE_NAME)
                .description(APPLICATION_INSTANCE_DESCRIPTION)
                .defaultRuntimeContextDevice(DEVICE_ARN)
                .runtimeRoleArn(RUNTIME_ROLE_ARN)
                .status(ApplicationInstanceStatus.DEPLOYMENT_SUCCEEDED.toString())
                .healthStatus(ApplicationInstanceHealthStatus.RUNNING.toString())
                .statusDescription(STATUS_DESCRIPTION)
                .createdTime(CREATED_TIME)
                .lastUpdatedTime(LAST_UPDATED_TIME)
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(1)).describeApplicationInstance(any(DescribeApplicationInstanceRequest.class));
    }

    @Test
    public void handleRequest_WithTags() {
        final ReadHandler handler = new ReadHandler();
        final ResourceModel model = ResourceModel.builder()
                .applicationInstanceId(APPLICATION_INSTANCE_ID)
                .build();

        when(proxyClient.client().describeApplicationInstance(any(DescribeApplicationInstanceRequest.class)))
                .thenReturn(DescribeApplicationInstanceResponse.builder()
                        .applicationInstanceId(APPLICATION_INSTANCE_ID)
                        .name(APPLICATION_INSTANCE_NAME)
                        .description(APPLICATION_INSTANCE_DESCRIPTION)
                        .defaultRuntimeContextDevice(DEVICE_ARN)
                        .runtimeRoleArn(RUNTIME_ROLE_ARN)
                        .status(ApplicationInstanceStatus.DEPLOYMENT_SUCCEEDED)
                        .healthStatus(ApplicationInstanceHealthStatus.RUNNING)
                        .statusDescription(STATUS_DESCRIPTION)
                        .createdTime(Instant.ofEpochSecond(CREATED_TIME))
                        .lastUpdatedTime(Instant.ofEpochSecond(LAST_UPDATED_TIME))
                        .build());

        String key = "key";
        String value = "value";
        Map<String, String> tagsMap = new HashMap<>();
        tagsMap.put(key, value);
        when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
                .thenReturn(ListTagsForResourceResponse
                        .builder()
                        .tags(tagsMap)
                        .build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        ResourceModel expectedResourceModel = ResourceModel.builder()
                .applicationInstanceId(APPLICATION_INSTANCE_ID)
                .name(APPLICATION_INSTANCE_NAME)
                .description(APPLICATION_INSTANCE_DESCRIPTION)
                .defaultRuntimeContextDevice(DEVICE_ARN)
                .runtimeRoleArn(RUNTIME_ROLE_ARN)
                .status(ApplicationInstanceStatus.DEPLOYMENT_SUCCEEDED.toString())
                .healthStatus(ApplicationInstanceHealthStatus.RUNNING.toString())
                .statusDescription(STATUS_DESCRIPTION)
                .createdTime(CREATED_TIME)
                .lastUpdatedTime(LAST_UPDATED_TIME)
                .tags(Arrays.asList(software.amazon.panorama.applicationinstance.Tag.builder().key(key).value(value).build()))
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(1)).describeApplicationInstance(any(DescribeApplicationInstanceRequest.class));
    }

    @Test
    public void handleRequest_throws_CfnNotFoundException() {
        final ReadHandler handler = new ReadHandler();

        when(proxyClient.client().describeApplicationInstance(any(DescribeApplicationInstanceRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        final ResourceModel model = ResourceModel.builder()
                .applicationInstanceId(APPLICATION_INSTANCE_ID)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnNotFoundException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger)
        );
    }

    @Test
    public void handleRequest_throws_CfnGeneralServiceException() {
        final ReadHandler handler = new ReadHandler();

        when(proxyClient.client().describeApplicationInstance(any(DescribeApplicationInstanceRequest.class)))
                .thenThrow(AwsServiceException.builder().build());

        final ResourceModel model = ResourceModel.builder()
                .applicationInstanceId(APPLICATION_INSTANCE_ID)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnGeneralServiceException.class, () ->
                handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger)
        );
    }
}
