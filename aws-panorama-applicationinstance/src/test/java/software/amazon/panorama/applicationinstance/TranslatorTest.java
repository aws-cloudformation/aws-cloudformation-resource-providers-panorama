package software.amazon.panorama.applicationinstance;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.panorama.model.ApplicationInstanceHealthStatus;
import software.amazon.awssdk.services.panorama.model.ApplicationInstanceStatus;
import software.amazon.awssdk.services.panorama.model.CreateApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceDetailsResponse;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.ListApplicationInstancesRequest;
import software.amazon.awssdk.services.panorama.model.RemoveApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.StatusFilter;
import software.amazon.awssdk.services.panorama.model.TagResourceRequest;
import software.amazon.awssdk.services.panorama.model.UntagResourceRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_ARN;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_DESCRIPTION;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_ID;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_NAME;
import static software.amazon.panorama.applicationinstance.Constants.CREATED_TIME;
import static software.amazon.panorama.applicationinstance.Constants.DEVICE_ARN;
import static software.amazon.panorama.applicationinstance.Constants.DEVICE_ID;
import static software.amazon.panorama.applicationinstance.Constants.LAST_UPDATED_TIME;
import static software.amazon.panorama.applicationinstance.Constants.MANIFEST_PAYLOAD;
import static software.amazon.panorama.applicationinstance.Constants.MANIFEST_PAYLOAD_OVERRIDES;
import static software.amazon.panorama.applicationinstance.Constants.RUNTIME_ROLE_ARN;
import static software.amazon.panorama.applicationinstance.Constants.STATUS_DESCRIPTION;

public class TranslatorTest {

    @Test
    void testTranslateToReadRequest() {
        ResourceModel model = ResourceModel.builder()
                .applicationInstanceId(APPLICATION_INSTANCE_ID)
                .build();
        DescribeApplicationInstanceRequest request = Translator.translateToReadRequest(model);
        assertThat(request.applicationInstanceId()).isEqualTo(APPLICATION_INSTANCE_ID);
    }

    @Test
    void testTranslateToDeleteRequest() {
        ResourceModel model = ResourceModel.builder()
                .applicationInstanceId(APPLICATION_INSTANCE_ID)
                .build();
        RemoveApplicationInstanceRequest request = Translator.translateToDeleteRequest(model);
        assertThat(request.applicationInstanceId()).isEqualTo(APPLICATION_INSTANCE_ID);
    }

    @Test
    void testTranslateToCreateRequest() {
        ResourceModel model = ResourceModel.builder()
                .manifestPayload(ManifestPayload.builder().payloadData(MANIFEST_PAYLOAD).build())
                .defaultRuntimeContextDevice(DEVICE_ARN)
                .build();
        CreateApplicationInstanceRequest request = Translator.translateToCreateRequest(model);
        assertThat(request.manifestPayload()).isEqualTo(software.amazon.awssdk.services.panorama.model.ManifestPayload.builder()
                .payloadData(MANIFEST_PAYLOAD).build());
        assertThat(request.defaultRuntimeContextDevice()).isEqualTo(DEVICE_ARN);
    }

    @Test
    void testTranslateFromReadResponse() {
        String key = "key";
        String value = "value";

        DescribeApplicationInstanceResponse describeApplicationInstanceResponse = DescribeApplicationInstanceResponse.builder()
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
                .build();

        DescribeApplicationInstanceDetailsResponse detailsResponse = DescribeApplicationInstanceDetailsResponse.builder()
                .applicationInstanceId(APPLICATION_INSTANCE_ID)
                .manifestPayload(software.amazon.awssdk.services.panorama.model.ManifestPayload.builder()
                        .payloadData(MANIFEST_PAYLOAD)
                        .build())
                .manifestOverridesPayload(software.amazon.awssdk.services.panorama.model.ManifestOverridesPayload.builder()
                        .payloadData(MANIFEST_PAYLOAD_OVERRIDES)
                        .build())
                .build();

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

        ResourceModel resourceModel  = Translator.translateFromReadResponse(describeApplicationInstanceResponse, detailsResponse);
        assertThat(resourceModel).isEqualTo(expectedModel);
    }

    @Test
    void testTranslateToListRequest()  {
        String nextToken = "nextToken";
        String statusFilter = StatusFilter.DEPLOYMENT_SUCCEEDED.toString();

        ResourceModel model = ResourceModel.builder()
                .deviceId(DEVICE_ID)
                .statusFilter(statusFilter)
                .build();

        ListApplicationInstancesRequest request = Translator.translateToListRequest(model.getDeviceId(), model.getStatusFilter(), nextToken);

        assertThat(request.deviceId()).isEqualTo(DEVICE_ID);
        assertThat(request.statusFilter().toString()).isEqualTo(statusFilter);
        assertThat(request.nextToken()).isEqualTo(nextToken);
    }

    @Test
    void testTranslateToTagResourceRequest() {
        Set<Tag> tags = ImmutableSet.of(
                Tag.builder().key("key1").value("value1").build(),
                Tag.builder().key("key2").value("value2").build()
        );

        TagResourceRequest request = Translator.translateToTagResourceRequest(tags, APPLICATION_INSTANCE_ARN);
        assertThat(request.tags().get("key1")).isEqualTo("value1");
        assertThat(request.tags().get("key2")).isEqualTo("value2");
        assertThat(request.resourceArn()).isEqualTo(APPLICATION_INSTANCE_ARN);
    }

    @Test
    void testTranslateToUntagResourceRequest() {
        Set<Tag> tags = ImmutableSet.of(
                Tag.builder().key("key1").value("value1").build(),
                Tag.builder().key("key2").value("value2").build()
        );

        UntagResourceRequest request = Translator.translateToUntagResourceRequest(tags, APPLICATION_INSTANCE_ARN);
        assertThat(request.tagKeys().size()).isEqualTo(2);
        assertThat(request.tagKeys()).contains("key1");
        assertThat(request.tagKeys()).contains("key2");
        assertThat(request.resourceArn()).isEqualTo(APPLICATION_INSTANCE_ARN);
    }

}
