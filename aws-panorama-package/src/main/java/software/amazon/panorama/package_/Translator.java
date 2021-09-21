package software.amazon.panorama.package_;

import software.amazon.awssdk.services.panorama.model.CreatePackageRequest;
import software.amazon.awssdk.services.panorama.model.DeletePackageRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageResponse;
import software.amazon.awssdk.services.panorama.model.ListPackagesRequest;
import software.amazon.awssdk.services.panorama.model.ListPackagesResponse;
import software.amazon.awssdk.services.panorama.model.TagResourceRequest;
import software.amazon.awssdk.services.panorama.model.UntagResourceRequest;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a Package AccessPoint
   * @param model resource model
   * @return CreatePackageRequest the Panorama request to create a Package
   */
  static CreatePackageRequest translateToCreateRequest(final ResourceModel model) {
    final CreatePackageRequest.Builder builder = CreatePackageRequest
            .builder()
            .packageName(model.getPackageName());

    if (model.getTags() != null && !model.getTags().isEmpty()) {
      builder.tags(model.getTags().stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue)));
    }

    return builder.build();
  }

  /**
   * Request to get a Package AccessPoint
   * @param model resource model
   * @return DescribePackageRequest the Panorama request to get a Package AccessPoint
   */
  static DescribePackageRequest translateToReadRequest(final ResourceModel model) {
    final DescribePackageRequest.Builder builder = DescribePackageRequest
            .builder()
            .packageId(model.getPackageId());
    return builder.build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param response the Panorama service DescribePackageResponse
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final DescribePackageResponse response) {
    ResourceModel.ResourceModelBuilder builder = ResourceModel.builder()
            .packageName(response.packageName())
            .packageId(response.packageId())
            .arn(response.arn())
            .storageLocation(StorageLocation.builder()
                    .bucket(response.storageLocation().bucket())
                    .binaryPrefixLocation(response.storageLocation().binaryPrefixLocation())
                    .generatedPrefixLocation(response.storageLocation().generatedPrefixLocation())
                    .manifestPrefixLocation(response.storageLocation().manifestPrefixLocation())
                    .repoPrefixLocation(response.storageLocation().repoPrefixLocation())
                    .build());

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

    return builder.build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeletePackageRequest translateToDeleteRequest(final ResourceModel model) {
    final DeletePackageRequest.Builder builder = DeletePackageRequest
            .builder()
            .packageId(model.getPackageId());
    return builder.build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return ListPackagesRequest the Panorama service request to list resources within aws account
   */
  static ListPackagesRequest translateToListRequest(final String nextToken, int maxResults) {
    final ListPackagesRequest.Builder builder = ListPackagesRequest
            .builder()
            .nextToken(nextToken)
            .maxResults(maxResults);
    return builder.build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param listPackagesResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListResponse(final ListPackagesResponse listPackagesResponse) {
    return streamOfOrEmpty(listPackagesResponse.packages())
            .map(resource -> ResourceModel.builder()
                    .packageId(resource.packageId())
                    .build())
            .collect(Collectors.toList());
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
