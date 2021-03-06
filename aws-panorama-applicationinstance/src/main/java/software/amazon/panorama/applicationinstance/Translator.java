package software.amazon.panorama.applicationinstance;

import software.amazon.awssdk.services.panorama.model.ApplicationInstanceStatus;
import software.amazon.awssdk.services.panorama.model.CreateApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceDetailsRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceDetailsResponse;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.DescribeApplicationInstanceResponse;
import software.amazon.awssdk.services.panorama.model.ListApplicationInstancesRequest;
import software.amazon.awssdk.services.panorama.model.ListApplicationInstancesResponse;
import software.amazon.awssdk.services.panorama.model.ManifestOverridesPayload;
import software.amazon.awssdk.services.panorama.model.ManifestPayload;
import software.amazon.awssdk.services.panorama.model.RemoveApplicationInstanceRequest;
import software.amazon.awssdk.services.panorama.model.StatusFilter;
import software.amazon.awssdk.services.panorama.model.TagResourceRequest;
import software.amazon.awssdk.services.panorama.model.UntagResourceRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
            .manifestPayload(ManifestPayload.builder()
                    .payloadData(model.getManifestPayload().getPayloadData())
                    .build())
            .defaultRuntimeContextDevice(model.getDefaultRuntimeContextDevice());

    if (model.getName() != null) {
      builder.name(model.getName());
    }

    if (model.getDescription() != null) {
      builder.description(model.getDescription());
    }

    if (model.getManifestOverridesPayload() != null) {
      builder.manifestOverridesPayload(ManifestOverridesPayload.builder()
              .payloadData(model.getManifestOverridesPayload().getPayloadData())
              .build());
    }

    if (model.getApplicationInstanceIdToReplace() != null) {
      builder.applicationInstanceIdToReplace(model.getApplicationInstanceIdToReplace());
    }

    if (model.getRuntimeRoleArn() != null) {
      builder.runtimeRoleArn(model.getRuntimeRoleArn());
    }

    if (model.getTags() != null && !model.getTags().isEmpty()) {
      Map<String, String> tagMap = new HashMap<>();
      for (Tag tag : model.getTags()) {
        tagMap.put(tag.getKey(), tag.getValue());
      }
      builder.tags(tagMap);
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
   * Request to read ApplicationInstance
   * @param model resource model
   * @return DescribeApplicationInstanceDetailsRequest the request to describe an ApplicationInstance details
   */
  static DescribeApplicationInstanceDetailsRequest translateToDescribeApplicationInstanceDetails(final ResourceModel model) {
    return DescribeApplicationInstanceDetailsRequest.builder()
            .applicationInstanceId(model.getApplicationInstanceId())
            .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param response the aws service describe resource response
   * @param detailsResponse the response that describes the details of ApplicationInstance
   * @return
   */
  static ResourceModel translateFromReadResponse(final DescribeApplicationInstanceResponse response,
                                                 final DescribeApplicationInstanceDetailsResponse detailsResponse) {
    ResourceModel.ResourceModelBuilder builder = ResourceModel.builder()
            .name(response.name())
            .description(response.description())
            .applicationInstanceId(response.applicationInstanceId())
            .applicationInstanceIdToReplace(response.applicationInstanceIdToReplace())
            .defaultRuntimeContextDevice(response.defaultRuntimeContextDevice())
            .defaultRuntimeContextDeviceName(response.defaultRuntimeContextDeviceName())
            .runtimeRoleArn(response.runtimeRoleArn())
            .status(response.statusAsString())
            .healthStatus(response.healthStatusAsString())
            .statusDescription(response.statusDescription())
            .createdTime(Long.valueOf(response.createdTime().getEpochSecond()).intValue())
            .lastUpdatedTime(Long.valueOf(response.lastUpdatedTime().getEpochSecond()).intValue())
            .arn(response.arn());

    if (response.hasTags()) {
      builder.tags(response.tags().entrySet()
              .stream()
              .map(tag ->
                      Tag.builder()
                              .key(tag.getKey())
                              .value(tag.getValue())
                              .build()
              )
              .collect(Collectors.toSet()));
    }

    if (detailsResponse.manifestPayload() != null) {
      builder.manifestPayload(software.amazon.panorama.applicationinstance.ManifestPayload.builder()
              .payloadData(detailsResponse.manifestPayload().payloadData())
              .build());
    }

    if (detailsResponse.manifestOverridesPayload() != null) {
      builder.manifestOverridesPayload(software.amazon.panorama.applicationinstance.ManifestOverridesPayload.builder()
              .payloadData(detailsResponse.manifestOverridesPayload().payloadData())
              .build());
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
   * @param nextToken nextToken to start list ApplicationInstances
   * @return ListApplicationInstancesRequest to list ApplicationInstances
   */
  static ListApplicationInstancesRequest translateToListRequest(
          final String deviceId,
          final String statusFilter,
          final String nextToken
  ) {
    final ListApplicationInstancesRequest.Builder builder = ListApplicationInstancesRequest.builder()
            .nextToken(nextToken);

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
            .filter(applicationInstance -> !ApplicationInstanceStatus.REMOVAL_SUCCEEDED.equals(applicationInstance.status()))
            .map(applicationInstance -> ResourceModel.builder()
                    .name(applicationInstance.name())
                    .description(applicationInstance.description())
                    .applicationInstanceId(applicationInstance.applicationInstanceId())
                    .defaultRuntimeContextDevice(applicationInstance.defaultRuntimeContextDevice())
                    .defaultRuntimeContextDeviceName(applicationInstance.defaultRuntimeContextDeviceName())
                    .status(applicationInstance.statusAsString())
                    .healthStatus(applicationInstance.healthStatusAsString())
                    .statusDescription(applicationInstance.statusDescription())
                    .createdTime(Long.valueOf(applicationInstance.createdTime().getEpochSecond()).intValue())
                    .arn(applicationInstance.arn())
                    .build()
            ).collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
            .map(Collection::stream)
            .orElseGet(Stream::empty);
  }

  static UntagResourceRequest translateToUntagResourceRequest(Set<Tag> tags, String arn) {
    return UntagResourceRequest
            .builder()
            .resourceArn(arn)
            .tagKeys(tags.stream().map(x -> x.getKey()).collect(Collectors.toList()))
            .build();
  }

  static TagResourceRequest translateToTagResourceRequest(Set<Tag> tags, String arn) {
    return TagResourceRequest
            .builder()
            .resourceArn(arn)
            .tags(tags.stream().collect( Collectors.toMap(Tag::getKey,
                    Tag::getValue)))
            .build();
  }

}
