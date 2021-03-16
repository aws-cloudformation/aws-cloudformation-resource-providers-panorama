package software.amazon.panorama.package_;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.panorama.model.CreatePackageRequest;
import software.amazon.awssdk.services.panorama.model.DeletePackageRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageResponse;
import software.amazon.awssdk.services.panorama.model.StorageLocation;
import com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.services.panorama.model.TagResourceRequest;
import software.amazon.awssdk.services.panorama.model.UntagResourceRequest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.panorama.package_.Constants.TEST_PACKAGE_ARN;
import static software.amazon.panorama.package_.Constants.TEST_PACKAGE_ID;
import static software.amazon.panorama.package_.Constants.TEST_PACKAGE_NAME;

public class TranslatorTest {

    @Test
    void testTranslateToReadRequest() {
        ResourceModel resourceModel = ResourceModel
                .builder()
                .packageId(TEST_PACKAGE_ID)
                .build();
        DescribePackageRequest describePackageRequest = Translator.translateToReadRequest(resourceModel);
        assertThat(describePackageRequest.packageId()).isEqualTo(TEST_PACKAGE_ID);
    }

    @Test
    void testTranslateToDeleteRequest() {
        ResourceModel resourceModel = ResourceModel
                .builder()
                .packageId(TEST_PACKAGE_ID)
                .build();
        DeletePackageRequest deletePackageRequest = Translator.translateToDeleteRequest(resourceModel);
        assertThat(deletePackageRequest.packageId()).isEqualTo(TEST_PACKAGE_ID);
    }

    @Test
    void testTranslateToCreateRequest() {
        ResourceModel resourceModel = ResourceModel
                .builder()
                .packageName(TEST_PACKAGE_NAME)
                .build();
        CreatePackageRequest createPackageRequest = Translator.translateToCreateRequest(resourceModel);
        assertThat(createPackageRequest.packageName()).isEqualTo(TEST_PACKAGE_NAME);
    }

    @Test
    void testTranslateFromReadResponse() {

        final String KEY1 = "key1";
        final String KEY2 = "key2";
        final String VALUE1 = "value1";
        final String VALUE2 = "value2";

        final Map<String, String> tagsMap = ImmutableMap.<String, String>builder()
                .put(KEY1, VALUE1)
                .put(KEY2, VALUE2)
                .build();

        DescribePackageResponse response = DescribePackageResponse.builder()
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
                .build();

        ResourceModel model = Translator.translateFromReadResponse(response);
        assertThat(model.getPackageId()).isEqualTo(TEST_PACKAGE_ID);
        assertThat(model.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(model.getArn()).isEqualTo(TEST_PACKAGE_ARN);
        assertThat(model.getStorageLocation()).isEqualTo(
                software.amazon.panorama.package_.StorageLocation.builder()
                        .bucket("test-bucket")
                        .binaryPrefixLocation("binary-prefix")
                        .repoPrefixLocation("repo-prefix")
                        .generatedPrefixLocation("generated-prefix")
                        .manifestPrefixLocation("manifest-prefix")
                        .build()
        );
        assertThat(model.getTags()).isEqualTo(new HashSet<>(Arrays.asList(
                software.amazon.panorama.package_.Tag.builder().key(KEY1).value(VALUE1).build(),
                software.amazon.panorama.package_.Tag.builder().key(KEY2).value(VALUE2).build()
        )));
    }

    @Test
    void testTranslateToTagResourceRequest() {
        Set<Tag> tags = ImmutableSet.of(
                Tag.builder().key("key1").value("value1").build(),
                Tag.builder().key("key2").value("value2").build()
        );

        TagResourceRequest request = Translator.translateToTagResourceRequest(tags, TEST_PACKAGE_ARN);
        assertThat(request.tags().get("key1")).isEqualTo("value1");
        assertThat(request.tags().get("key2")).isEqualTo("value2");
        assertThat(request.resourceArn()).isEqualTo(TEST_PACKAGE_ARN);
    }

    @Test
    void testTranslateToUntagResourceRequest() {
        Set<Tag> tags = ImmutableSet.of(
                Tag.builder().key("key1").value("value1").build(),
                Tag.builder().key("key2").value("value2").build()
        );

        UntagResourceRequest request = Translator.translateToUntagResourceRequest(tags, TEST_PACKAGE_ARN);
        assertThat(request.tagKeys().size()).isEqualTo(2);
        assertThat(request.tagKeys()).contains("key1");
        assertThat(request.tagKeys()).contains("key2");
        assertThat(request.resourceArn()).isEqualTo(TEST_PACKAGE_ARN);
    }
}
