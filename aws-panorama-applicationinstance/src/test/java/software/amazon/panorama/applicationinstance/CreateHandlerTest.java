package software.amazon.panorama.applicationinstance;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;

import com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.AccessDeniedException;
import software.amazon.awssdk.services.panorama.model.ApplicationInstanceHealthStatus;
import software.amazon.awssdk.services.panorama.model.ApplicationInstanceStatus;
import software.amazon.awssdk.services.panorama.model.CreateApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.CreateApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceDetailsRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceDetailsResponse;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.InternalServerException;
import software.amazon.awssdk.services.panorama.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.panorama.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_DESCRIPTION;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_ID;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_NAME;
import static software.amazon.panorama.applicationinstance.Constants.CREATED_TIME;
import static software.amazon.panorama.applicationinstance.Constants.DEVICE_ARN;
import static software.amazon.panorama.applicationinstance.Constants.RUNTIME_ROLE_ARN;
import static software.amazon.panorama.applicationinstance.Constants.LAST_UPDATED_TIME;
import static software.amazon.panorama.applicationinstance.Constants.MANIFEST_PAYLOAD;
import static software.amazon.panorama.applicationinstance.Constants.MANIFEST_PAYLOAD_OVERRIDES;
import static software.amazon.panorama.applicationinstance.Constants.STATUS_DESCRIPTION;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

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
        verify(panoramaClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(panoramaClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .name(APPLICATION_INSTANCE_NAME)
                .description(APPLICATION_INSTANCE_DESCRIPTION)
                .manifestPayload(ManifestPayload.builder().payloadData(MANIFEST_PAYLOAD).build())
                .manifestOverridesPayload(ManifestOverridesPayload.builder().payloadData(MANIFEST_PAYLOAD_OVERRIDES).build())
                .runtimeRoleArn(RUNTIME_ROLE_ARN)
                .defaultRuntimeContextDevice(DEVICE_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().createApplicationInstance(any(CreateApplicationInstanceRequest.class)))
                .thenReturn(CreateApplicationInstanceResponse.builder().applicationInstanceId(APPLICATION_INSTANCE_ID).build());
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

        ResourceModel expectedResourceModel = ResourceModel.builder()
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
        assertThat(response.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(1)).createApplicationInstance(any(CreateApplicationInstanceRequest.class));
        verify(proxyClient.client(), times(2)).describeApplicationInstance(any(DescribeApplicationInstanceRequest.class));
    }

    @Test
    public void handleRequest_Tags() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .name(APPLICATION_INSTANCE_NAME)
                .description(APPLICATION_INSTANCE_DESCRIPTION)
                .manifestPayload(ManifestPayload.builder().payloadData(MANIFEST_PAYLOAD).build())
                .manifestOverridesPayload(ManifestOverridesPayload.builder().payloadData(MANIFEST_PAYLOAD_OVERRIDES).build())
                .runtimeRoleArn(RUNTIME_ROLE_ARN)
                .defaultRuntimeContextDevice(DEVICE_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().createApplicationInstance(any(CreateApplicationInstanceRequest.class)))
                .thenReturn(CreateApplicationInstanceResponse.builder().applicationInstanceId(APPLICATION_INSTANCE_ID).build());

        String key = "key";
        String value = "value";
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

        ResourceModel expectedResourceModel = ResourceModel.builder()
                .applicationInstanceId(APPLICATION_INSTANCE_ID)
                .name(APPLICATION_INSTANCE_NAME)
                .description(APPLICATION_INSTANCE_DESCRIPTION)
                .manifestPayload(ManifestPayload.builder().payloadData(MANIFEST_PAYLOAD).build())
                .manifestOverridesPayload(ManifestOverridesPayload.builder().payloadData(MANIFEST_PAYLOAD_OVERRIDES).build())
                .defaultRuntimeContextDevice(DEVICE_ARN)
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
        assertThat(response.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(1)).createApplicationInstance(any(CreateApplicationInstanceRequest.class));
        verify(proxyClient.client(), times(2)).describeApplicationInstance(any(DescribeApplicationInstanceRequest.class));
    }

    @Test
    public void handleRequest_throws_CfnInvalidRequestException() {
        final CreateHandler handler = new CreateHandler();

        when(proxyClient.client().createApplicationInstance(any(CreateApplicationInstanceRequest.class)))
                .thenThrow(ValidationException.builder().build());

        final ResourceModel model = ResourceModel.builder()
                .name(APPLICATION_INSTANCE_NAME)
                .description(APPLICATION_INSTANCE_DESCRIPTION)
                .manifestPayload(ManifestPayload.builder().payloadData(MANIFEST_PAYLOAD).build())
                .manifestOverridesPayload(ManifestOverridesPayload.builder().payloadData(MANIFEST_PAYLOAD_OVERRIDES).build())
                .runtimeRoleArn(RUNTIME_ROLE_ARN)
                .defaultRuntimeContextDevice(DEVICE_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInvalidRequestException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_throws_CfnAccessDeniedException() {
        final CreateHandler handler = new CreateHandler();

        when(proxyClient.client().createApplicationInstance(any(CreateApplicationInstanceRequest.class)))
                .thenThrow(AccessDeniedException.builder().build());

        final ResourceModel model = ResourceModel.builder()
                .name(APPLICATION_INSTANCE_NAME)
                .description(APPLICATION_INSTANCE_DESCRIPTION)
                .manifestPayload(ManifestPayload.builder().payloadData(MANIFEST_PAYLOAD).build())
                .manifestOverridesPayload(ManifestOverridesPayload.builder().payloadData(MANIFEST_PAYLOAD_OVERRIDES).build())
                .runtimeRoleArn(RUNTIME_ROLE_ARN)
                .defaultRuntimeContextDevice(DEVICE_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnAccessDeniedException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_throws_CfnServiceLimitExceededException() {
        final CreateHandler handler = new CreateHandler();

        when(proxyClient.client().createApplicationInstance(any(CreateApplicationInstanceRequest.class)))
                .thenThrow(ServiceQuotaExceededException.builder().build());

        final ResourceModel model = ResourceModel.builder()
                .name(APPLICATION_INSTANCE_NAME)
                .description(APPLICATION_INSTANCE_DESCRIPTION)
                .manifestPayload(ManifestPayload.builder().payloadData(MANIFEST_PAYLOAD).build())
                .manifestOverridesPayload(ManifestOverridesPayload.builder().payloadData(MANIFEST_PAYLOAD_OVERRIDES).build())
                .runtimeRoleArn(RUNTIME_ROLE_ARN)
                .defaultRuntimeContextDevice(DEVICE_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnServiceLimitExceededException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_throws_CfnInternalFailureException() {
        final CreateHandler handler = new CreateHandler();

        when(proxyClient.client().createApplicationInstance(any(CreateApplicationInstanceRequest.class)))
                .thenThrow(InternalServerException.builder().build());

        final ResourceModel model = ResourceModel.builder()
                .name(APPLICATION_INSTANCE_NAME)
                .description(APPLICATION_INSTANCE_DESCRIPTION)
                .manifestPayload(ManifestPayload.builder().payloadData(MANIFEST_PAYLOAD).build())
                .manifestOverridesPayload(ManifestOverridesPayload.builder().payloadData(MANIFEST_PAYLOAD_OVERRIDES).build())
                .runtimeRoleArn(RUNTIME_ROLE_ARN)
                .defaultRuntimeContextDevice(DEVICE_ARN)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnInternalFailureException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }
}
