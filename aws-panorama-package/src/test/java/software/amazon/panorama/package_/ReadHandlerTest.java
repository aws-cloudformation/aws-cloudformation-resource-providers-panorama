package software.amazon.panorama.package_;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.DescribePackageRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageResponse;
import software.amazon.awssdk.services.panorama.model.StorageLocation;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import com.google.common.collect.ImmutableMap;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.panorama.package_.Constants.TEST_PACKAGE_ARN;
import static software.amazon.panorama.package_.Constants.TEST_PACKAGE_ID;
import static software.amazon.panorama.package_.Constants.TEST_PACKAGE_NAME;

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
                .packageId(TEST_PACKAGE_ID)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

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

        verify(proxyClient.client(), times(1)).describePackage(any(DescribePackageRequest.class));
    }

    @Test
    public void handleRequest_WithTags() {
        final ReadHandler handler = new ReadHandler();
        final String KEY1 = "key1";
        final String KEY2 = "key2";
        final String VALUE1 = "value1";
        final String VALUE2 = "value2";

        final Map<String, String> tagsMap = ImmutableMap.<String, String>builder()
                .put(KEY1, VALUE1)
                .put(KEY2, VALUE2)
                .build();

        final Set<Tag> tags = tagsMap.entrySet().stream()
                .map(tag ->
                        Tag.builder()
                                .key(tag.getKey())
                                .value(tag.getValue())
                                .build()
                )
                .collect(Collectors.toSet());

        final ResourceModel model = ResourceModel.builder()
                .packageId(TEST_PACKAGE_ID)
                .tags(tags)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().describePackage(any(DescribePackageRequest.class)))
                .thenReturn(DescribePackageResponse.builder()
                        .packageId(TEST_PACKAGE_ID)
                        .packageName(TEST_PACKAGE_NAME)
                        .arn(TEST_PACKAGE_ARN)
                        .storageLocation(StorageLocation.builder()
                                .bucket("test-bucket")
                                .binaryPrefixLocation("binary-prefix")
                                .repoPrefixLocation("repo-prefix")
                                .generatedPrefixLocation("generated-prefix")
                                .manifestPrefixLocation("manifest-prefix")
                                .build())
                        .tags(tagsMap)
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
                .tags(tags)
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(1)).describePackage(any(DescribePackageRequest.class));
    }
}
