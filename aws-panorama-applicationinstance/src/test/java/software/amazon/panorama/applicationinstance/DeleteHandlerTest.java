package software.amazon.panorama.applicationinstance;

import java.time.Duration;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.AccessDeniedException;
import software.amazon.awssdk.services.panorama.model.ApplicationInstanceStatus;
import software.amazon.awssdk.services.panorama.model.ConflictException;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.InternalServerException;
import software.amazon.awssdk.services.panorama.model.RemoveApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.RemoveApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.ResourceNotFoundException;
import software.amazon.awssdk.services.panorama.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnResourceConflictException;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_DESCRIPTION;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_ID;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_NAME;
import static software.amazon.panorama.applicationinstance.Constants.DEVICE_ARN;
import static software.amazon.panorama.applicationinstance.Constants.MANIFEST_PAYLOAD;
import static software.amazon.panorama.applicationinstance.Constants.MANIFEST_PAYLOAD_OVERRIDES;
import static software.amazon.panorama.applicationinstance.Constants.RUNTIME_ROLE_ARN;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {

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
        final DeleteHandler handler = new DeleteHandler();

        final ResourceModel model = ResourceModel.builder()
                .applicationInstanceId(APPLICATION_INSTANCE_ID)
                .build();

        when(proxyClient.client().removeApplicationInstance(any(RemoveApplicationInstanceRequest.class)))
                .thenReturn(RemoveApplicationInstanceResponse.builder().build());

        when(proxyClient.client().describeApplicationInstance(any(DescribeApplicationInstanceRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(1)).removeApplicationInstance(any(RemoveApplicationInstanceRequest.class));
        verify(proxyClient.client(), times(1)).describeApplicationInstance(any(DescribeApplicationInstanceRequest.class));
    }

    @Test
    public void handleRequest_throws_CfnInvalidRequestException() {
        final DeleteHandler handler = new DeleteHandler();

        when(proxyClient.client().removeApplicationInstance(any(RemoveApplicationInstanceRequest.class)))
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
        final DeleteHandler handler = new DeleteHandler();

        when(proxyClient.client().removeApplicationInstance(any(RemoveApplicationInstanceRequest.class)))
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
    public void handleRequest_throws_CfnResourceConflictException() {
        final DeleteHandler handler = new DeleteHandler();

        when(proxyClient.client().removeApplicationInstance(any(RemoveApplicationInstanceRequest.class)))
                .thenThrow(ConflictException.builder().build());
        when(proxyClient.client().describeApplicationInstance(any(DescribeApplicationInstanceRequest.class)))
                .thenReturn(DescribeApplicationInstanceResponse.builder().status(ApplicationInstanceStatus.REMOVAL_IN_PROGRESS).build());

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

        assertThrows(CfnResourceConflictException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_throws_CfnInternalFailureException() {
        final DeleteHandler handler = new DeleteHandler();

        when(proxyClient.client().removeApplicationInstance(any(RemoveApplicationInstanceRequest.class)))
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

    @Test
    public void handleRequest_throws_CfnNotFoundException() {
        final DeleteHandler handler = new DeleteHandler();

        when(proxyClient.client().removeApplicationInstance(any(RemoveApplicationInstanceRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

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

        assertThrows(CfnNotFoundException.class, () -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }
}
