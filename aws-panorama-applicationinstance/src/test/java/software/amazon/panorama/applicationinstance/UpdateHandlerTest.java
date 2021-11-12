package software.amazon.panorama.applicationinstance;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.ApplicationInstanceHealthStatus;
import software.amazon.awssdk.services.panorama.model.ApplicationInstanceStatus;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceDetailsRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceDetailsResponse;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.ResourceNotFoundException;
import software.amazon.awssdk.services.panorama.model.TagResourceRequest;
import software.amazon.awssdk.services.panorama.model.TagResourceResponse;
import software.amazon.awssdk.services.panorama.model.UntagResourceRequest;
import software.amazon.awssdk.services.panorama.model.UntagResourceResponse;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_DESCRIPTION;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_ID;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_NAME;
import static software.amazon.panorama.applicationinstance.Constants.CREATED_TIME;
import static software.amazon.panorama.applicationinstance.Constants.DEVICE_ARN;
import static software.amazon.panorama.applicationinstance.Constants.LAST_UPDATED_TIME;
import static software.amazon.panorama.applicationinstance.Constants.MANIFEST_PAYLOAD;
import static software.amazon.panorama.applicationinstance.Constants.MANIFEST_PAYLOAD_OVERRIDES;
import static software.amazon.panorama.applicationinstance.Constants.RUNTIME_ROLE_ARN;
import static software.amazon.panorama.applicationinstance.Constants.STATUS_DESCRIPTION;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {
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

    @Test
    public void handleRequest_addTags() {
        final UpdateHandler handler = new UpdateHandler();
        String key = "key";
        String value = "value";

        final ResourceModel model = ResourceModel.builder()
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
                .tags(new HashSet<>(Collections.singletonList(Tag.builder().key(key).value(value).build())))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().tagResource(any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());

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
                        .tags(ImmutableMap.of(key, value))
                        .build());

        when(proxyClient.client().describeApplicationInstanceDetails(any(DescribeApplicationInstanceDetailsRequest.class)))
                .thenReturn(DescribeApplicationInstanceDetailsResponse.builder()
                        .applicationInstanceId(APPLICATION_INSTANCE_ID)
                        .manifestPayload(software.amazon.awssdk.services.panorama.model.ManifestPayload.builder()
                                .payloadData(MANIFEST_PAYLOAD)
                                .build())
                        .manifestOverridesPayload(software.amazon.awssdk.services.panorama.model.ManifestOverridesPayload.builder()
                                .payloadData(MANIFEST_PAYLOAD_OVERRIDES)
                                .build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        ResourceModel expectedModel = ResourceModel.builder()
                .applicationInstanceId(APPLICATION_INSTANCE_ID)
                .name(APPLICATION_INSTANCE_NAME)
                .description(APPLICATION_INSTANCE_DESCRIPTION)
                .manifestPayload(ManifestPayload.builder().payloadData(MANIFEST_PAYLOAD).build())
                .manifestOverridesPayload(ManifestOverridesPayload.builder().payloadData(MANIFEST_PAYLOAD_OVERRIDES).build())
                .defaultRuntimeContextDevice(DEVICE_ARN)
                .runtimeRoleArn(RUNTIME_ROLE_ARN)
                .status(ApplicationInstanceStatus.DEPLOYMENT_SUCCEEDED.toString())
                .healthStatus(ApplicationInstanceHealthStatus.RUNNING.toString())
                .statusDescription(STATUS_DESCRIPTION)
                .createdTime(CREATED_TIME)
                .lastUpdatedTime(LAST_UPDATED_TIME)
                .tags(new HashSet<>(Collections.singletonList(Tag.builder().key(key).value(value).build())))
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_removeTags() {
        final UpdateHandler handler = new UpdateHandler();
        String key = "key";
        String value = "value";

        final ResourceModel model = ResourceModel.builder()
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

        final ResourceModel prevModel = ResourceModel.builder()
                .tags(new HashSet<>(Collections.singletonList(Tag.builder().key(key).value(value).build())))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .previousResourceState(prevModel)
                .build();

        when(proxyClient.client().untagResource(any(UntagResourceRequest.class)))
                .thenReturn(UntagResourceResponse.builder().build());

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

        when(proxyClient.client().describeApplicationInstanceDetails(any(DescribeApplicationInstanceDetailsRequest.class)))
                .thenReturn(DescribeApplicationInstanceDetailsResponse.builder()
                        .applicationInstanceId(APPLICATION_INSTANCE_ID)
                        .manifestPayload(software.amazon.awssdk.services.panorama.model.ManifestPayload.builder()
                                .payloadData(MANIFEST_PAYLOAD)
                                .build())
                        .manifestOverridesPayload(software.amazon.awssdk.services.panorama.model.ManifestOverridesPayload.builder()
                                .payloadData(MANIFEST_PAYLOAD_OVERRIDES)
                                .build())
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        ResourceModel expectedModel = ResourceModel.builder()
                .applicationInstanceId(APPLICATION_INSTANCE_ID)
                .name(APPLICATION_INSTANCE_NAME)
                .description(APPLICATION_INSTANCE_DESCRIPTION)
                .manifestPayload(ManifestPayload.builder().payloadData(MANIFEST_PAYLOAD).build())
                .manifestOverridesPayload(ManifestOverridesPayload.builder().payloadData(MANIFEST_PAYLOAD_OVERRIDES).build())
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
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void handleRequest_throwsCfnNotFoundException() {
        final UpdateHandler handler = new UpdateHandler();

        when(proxyClient.client().describeApplicationInstance(any(DescribeApplicationInstanceRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        String key = "key";
        String value = "value";
        final ResourceModel model = ResourceModel.builder()
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
                .tags(new HashSet<>(Collections.singletonList(Tag.builder().key(key).value(value).build())))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        });
        verify(proxyClient.client(), times(1)).describeApplicationInstance(any(DescribeApplicationInstanceRequest.class));

        verify(panoramaClient, atLeastOnce()).serviceName();
    }
}
