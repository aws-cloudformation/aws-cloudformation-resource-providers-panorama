package software.amazon.panorama.package_;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.DescribePackageRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageResponse;
import software.amazon.awssdk.services.panorama.model.ResourceNotFoundException;
import software.amazon.awssdk.services.panorama.model.StorageLocation;
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
import static software.amazon.panorama.package_.Constants.TEST_PACKAGE_ARN;
import static software.amazon.panorama.package_.Constants.TEST_PACKAGE_ID;
import static software.amazon.panorama.package_.Constants.TEST_PACKAGE_NAME;

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
                .packageId(TEST_PACKAGE_ID)
                .tags(new HashSet<>(Collections.singletonList(Tag.builder().key(key).value(value).build())))
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().tagResource(any(TagResourceRequest.class)))
                .thenReturn(TagResourceResponse.builder().build());

        when(proxyClient.client().describePackage(any(DescribePackageRequest.class)))
                .thenReturn(DescribePackageResponse.builder()
                        .packageId(TEST_PACKAGE_ID)
                        .packageName(TEST_PACKAGE_NAME)
                        .storageLocation(StorageLocation.builder()
                                .bucket("test-bucket")
                                .binaryPrefixLocation("binary-prefix")
                                .repoPrefixLocation("repo-prefix")
                                .generatedPrefixLocation("generated-prefix")
                                .manifestPrefixLocation("manifest-prefix")
                                .build())
                        .arn(TEST_PACKAGE_ARN)
                        .tags(ImmutableMap.of(key, value))
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        ResourceModel expectedModel = ResourceModel.builder()
                .packageId(TEST_PACKAGE_ID)
                .packageName(TEST_PACKAGE_NAME)
                .storageLocation(software.amazon.panorama.package_.StorageLocation.builder()
                        .bucket("test-bucket")
                        .binaryPrefixLocation("binary-prefix")
                        .repoPrefixLocation("repo-prefix")
                        .generatedPrefixLocation("generated-prefix")
                        .manifestPrefixLocation("manifest-prefix")
                        .build())
                .arn(TEST_PACKAGE_ARN)
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
                .packageId(TEST_PACKAGE_ID)
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

        when(proxyClient.client().describePackage(any(DescribePackageRequest.class)))
                .thenReturn(DescribePackageResponse.builder()
                        .packageId(TEST_PACKAGE_ID)
                        .packageName(TEST_PACKAGE_NAME)
                        .storageLocation(StorageLocation.builder()
                                .bucket("test-bucket")
                                .binaryPrefixLocation("binary-prefix")
                                .repoPrefixLocation("repo-prefix")
                                .generatedPrefixLocation("generated-prefix")
                                .manifestPrefixLocation("manifest-prefix")
                                .build())
                        .arn(TEST_PACKAGE_ARN)
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        ResourceModel expectedModel = ResourceModel.builder()
                .packageId(TEST_PACKAGE_ID)
                .packageName(TEST_PACKAGE_NAME)
                .storageLocation(software.amazon.panorama.package_.StorageLocation.builder()
                        .bucket("test-bucket")
                        .binaryPrefixLocation("binary-prefix")
                        .repoPrefixLocation("repo-prefix")
                        .generatedPrefixLocation("generated-prefix")
                        .manifestPrefixLocation("manifest-prefix")
                        .build())
                .arn(TEST_PACKAGE_ARN)
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

        when(proxyClient.client().describePackage(any(DescribePackageRequest.class)))
                .thenThrow(ResourceNotFoundException.builder().build());

        final ResourceModel model = ResourceModel.builder()
                .packageId(TEST_PACKAGE_ID)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThrows(CfnNotFoundException.class, () -> {
            handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);
        });
        verify(proxyClient.client(), times(1)).describePackage(any(DescribePackageRequest.class));

        verify(panoramaClient, atLeastOnce()).serviceName();
    }
}
