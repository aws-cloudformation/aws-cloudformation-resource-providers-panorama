package software.amazon.panorama.applicationinstance;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.panorama.model.CreateApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.ListApplicationInstancesRequest;
import software.amazon.awssdk.services.panorama.model.RemoveApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.StatusFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.panorama.applicationinstance.Constants.APPLICATION_INSTANCE_ID;
import static software.amazon.panorama.applicationinstance.Constants.DEVICE_ARN;
import static software.amazon.panorama.applicationinstance.Constants.DEVICE_ID;
import static software.amazon.panorama.applicationinstance.Constants.MANIFEST_PAYLOAD;

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
                .manifestPayload(MANIFEST_PAYLOAD)
                .defaultExecutionContextDevice(DEVICE_ARN)
                .build();
        CreateApplicationInstanceRequest request = Translator.translateToCreateRequest(model);
        assertThat(request.manifestPayload()).isEqualTo(MANIFEST_PAYLOAD);
        assertThat(request.defaultExecutionContextDevice()).isEqualTo(DEVICE_ARN);
    }

    @Test
    void testTranslateToListRequest()  {
        String nextToken = "nextToken";
        String statusFilter = StatusFilter.DEPLOYMENT_SUCCEEDED.toString();
        Integer maxResults = 10;

        ResourceModel model = ResourceModel.builder()
                .deviceId(DEVICE_ID)
                .statusFilter(statusFilter)
                .maxResults(maxResults)
                .build();

        ListApplicationInstancesRequest request = Translator.translateToListRequest(model.getDeviceId(), model.getStatusFilter(),
                model.getMaxResults(), nextToken);

        assertThat(request.deviceId()).isEqualTo(DEVICE_ID);
        assertThat(request.statusFilter().toString()).isEqualTo(statusFilter);
        assertThat(request.nextToken()).isEqualTo(nextToken);
        assertThat(request.maxResults()).isEqualTo(maxResults);
    }

}
