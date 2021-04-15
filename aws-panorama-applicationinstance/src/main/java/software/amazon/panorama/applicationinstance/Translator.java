package software.amazon.panorama.applicationinstance;

import software.amazon.awssdk.services.panorama.model.CreateApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.ListApplicationInstancesRequest;
import software.amazon.awssdk.services.panorama.model.ListApplicationInstancesResponse;
import software.amazon.awssdk.services.panorama.model.RemoveApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.StatusFilter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - ApplicationInstance request construction
 *  - object translation to/from Panorama sdk
 *  - resource model construction for read/list handlers
 */
public class Translator {

  /**
   * Request to create an ApplicationInstance
   * @param model resource model
   * @return CreateApplicationInstanceRequest the aws service request to create a resource
   */
  static CreateApplicationInstanceRequest translateToCreateRequest(final ResourceModel model) {
    final CreateApplicationInstanceRequest.Builder builder = CreateApplicationInstanceRequest.builder()
            .manifestPayload(model.getManifestPayload())
            .defaultExecutionContextDevice(model.getDefaultExecutionContextDevice());

    if (model.getName() != null) {
      builder.name(model.getName());
    }

    if (model.getDescription() != null) {
      builder.description(model.getDescription());
    }

    if (model.getManifestOverridesPayload() != null) {
      builder.manifestOverridesPayload(model.getManifestOverridesPayload());
    }

    if (model.getExecutionRoleArn() != null) {
      builder.executionRoleArn(model.getExecutionRoleArn());
    }

    return builder.build();
  }

  /**
   * Request to read ApplicationInstance
   * @param model resource model
   * @return DescribeApplicationInstanceRequest the request to describe an ApplicationInstance
   */
  static DescribeApplicationInstanceRequest translateToReadRequest(final ResourceModel model) {
    return DescribeApplicationInstanceRequest.builder()
            .applicationInstanceId(model.getApplicationInstanceId())
            .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param response the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final DescribeApplicationInstanceResponse response) {
    ResourceModel.ResourceModelBuilder builder = ResourceModel.builder()
            .name(response.name())
            .description(response.description())
            .applicationInstanceId(response.applicationInstanceId())
            .defaultExecutionContextDevice(response.defaultExecutionContextDevice())
            .executionRoleArn(response.executionRoleArn())
            .manifestPayload(response.manifestPayload())
            .manifestOverridesPayload(response.manifestOverridesPayload())
            .status(response.statusAsString())
            .statusDescription(response.statusDescription())
            .createdTime(Long.valueOf(response.createdTime().getEpochSecond()).intValue())
            .lastUpdatedTime(Long.valueOf(response.lastUpdatedTime().getEpochSecond()).intValue());

    if (response.manifestOverridesPayload() != null && !response.manifestOverridesPayload().isEmpty()) {
      builder.manifestOverridesPayload(response.manifestOverridesPayload());
    }
    return builder.build();
  }

  /**
   * Request to delete an ApplicationInstance
   * @param model resource model
   * @return RemoveApplicationInstanceRequest the request to delete an ApplicationInstance
   */
  static RemoveApplicationInstanceRequest translateToDeleteRequest(final ResourceModel model) {
    return RemoveApplicationInstanceRequest.builder()
            .applicationInstanceId(model.getApplicationInstanceId())
            .build();
  }

  /**
   * Request to list ApplicationInstances
   *
   * @param deviceId device id to filter the ApplicationInstances
   * @param statusFilter status to filter the ApplicationInstances
   * @param maxResults max number of ApplicationInstances in this request
   * @param nextToken nextToken to start list ApplicationInstances
   * @return ListApplicationInstancesRequest to list ApplicationInstances
   */
  static ListApplicationInstancesRequest translateToListRequest(
          final String deviceId,
          final String statusFilter,
          final Integer maxResults,
          final String nextToken
  ) {
    final ListApplicationInstancesRequest.Builder builder = ListApplicationInstancesRequest.builder()
            .nextToken(nextToken)
            .maxResults(maxResults);

    if (deviceId != null) {
      builder.deviceId(deviceId);
    }

    if (statusFilter != null) {
      builder.statusFilter(StatusFilter.valueOf(statusFilter));
    }

    return builder.build();
  }

  /**
   * Translates resource objects from Panorama response into a resource model (primary identifier only)
   * @param listApplicationInstancesResponse Panorama ListApplicationInstancesResponse
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListResponse(final ListApplicationInstancesResponse listApplicationInstancesResponse) {
    return streamOfOrEmpty(listApplicationInstancesResponse.applicationInstances())
        .map(resource -> ResourceModel.builder()
                .applicationInstanceId(resource.applicationInstanceId())
                .build()
        ).collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }
}
