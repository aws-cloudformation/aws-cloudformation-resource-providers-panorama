package software.amazon.panorama.packageversion;

import java.time.Duration;
import java.time.Instant;

import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.DescribePackageVersionRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageVersionResponse;
import software.amazon.awssdk.services.panorama.model.PackageVersionStatus;
import software.amazon.awssdk.services.panorama.model.RegisterPackageVersionRequest;
import software.amazon.awssdk.services.panorama.model.RegisterPackageVersionResponse;
import software.amazon.awssdk.services.panorama.model.ResourceNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.panorama.packageversion.Constants.TEST_PACKAGE_ID;
import static software.amazon.panorama.packageversion.Constants.TEST_PACKAGE_NAME;
import static software.amazon.panorama.packageversion.Constants.TEST_PACKAGE_VERSION;
import static software.amazon.panorama.packageversion.Constants.TEST_PATCH_VERSION;

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
                .packageId(TEST_PACKAGE_ID)
                .packageVersion(TEST_PACKAGE_VERSION)
                .patchVersion(TEST_PATCH_VERSION)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().registerPackageVersion(any(RegisterPackageVersionRequest.class)))
                .thenReturn(RegisterPackageVersionResponse.builder().build());

        when(proxyClient.client().describePackageVersion(any(DescribePackageVersionRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build())
                .thenReturn(DescribePackageVersionResponse.builder()
                        .packageId(TEST_PACKAGE_ID)
                        .packageName(TEST_PACKAGE_NAME)
                        .packageVersion(TEST_PACKAGE_VERSION)
                        .patchVersion(TEST_PATCH_VERSION)
                        .status(PackageVersionStatus.REGISTER_COMPLETED)
                        .registeredTime(Instant.ofEpochSecond(1000000000L))
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        ResourceModel expectedModel = ResourceModel.builder()
                .packageId(TEST_PACKAGE_ID)
                .packageName(TEST_PACKAGE_NAME)
                .packageVersion(TEST_PACKAGE_VERSION)
                .patchVersion(TEST_PATCH_VERSION)
                .status(PackageVersionStatus.REGISTER_COMPLETED.toString())
                .registeredTime(1000000000)
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
    public void handleRequest_resource_already_exists() {
        final CreateHandler handler = new CreateHandler();

        final ResourceModel model = ResourceModel.builder()
                .packageId(TEST_PACKAGE_ID)
                .packageVersion(TEST_PACKAGE_VERSION)
                .patchVersion(TEST_PATCH_VERSION)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().describePackageVersion(any(DescribePackageVersionRequest.class)))
                .thenReturn(DescribePackageVersionResponse.builder()
                        .packageId(TEST_PACKAGE_ID)
                        .packageName(TEST_PACKAGE_NAME)
                        .packageVersion(TEST_PACKAGE_VERSION)
                        .patchVersion(TEST_PATCH_VERSION)
                        .status(PackageVersionStatus.REGISTER_COMPLETED)
                        .registeredTime(Instant.ofEpochSecond(1000000000L))
                        .build());

        assertThrows(CfnAlreadyExistsException.class, () -> {
            handler.handleRequest(proxy, request, new software.amazon.panorama.packageversion.CallbackContext(), proxyClient, logger);
        });
    }
}
