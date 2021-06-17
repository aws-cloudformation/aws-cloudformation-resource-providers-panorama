package software.amazon.panorama.packageversion;

import software.amazon.awssdk.services.panorama.model.DeregisterPackageVersionRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageVersionRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageVersionResponse;
import software.amazon.awssdk.services.panorama.model.RegisterPackageVersionRequest;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return RegisterPackageVersionRequest the Panorama service request to create a resource
   */
  static RegisterPackageVersionRequest translateToCreateRequest(final ResourceModel model) {
    RegisterPackageVersionRequest.Builder builder = RegisterPackageVersionRequest.builder()
            .packageId(model.getPackageId())
            .packageVersion(model.getPackageVersion())
            .patchVersion(model.getPatchVersion());

    if (model.getOwnerAccount() != null) {
      builder.ownerAccount(model.getOwnerAccount());
    }

    if (model.getMarkLatest() != null) {
      builder.markLatest(model.getMarkLatest());
    }

    return builder.build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return DescribePackageVersionRequest the Panorama service request to describe a resource
   */
  static DescribePackageVersionRequest translateToReadRequest(final ResourceModel model) {
    DescribePackageVersionRequest.Builder builder = DescribePackageVersionRequest.builder()
            .packageId(model.getPackageId())
            .packageVersion(model.getPackageVersion());

    if (model.getPatchVersion() != null) {
      builder.patchVersion(model.getPatchVersion());
    }

    if (model.getOwnerAccount() != null) {
      builder.ownerAccount(model.getOwnerAccount());
    }

    return builder.build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return DescribePackageVersionRequest the Panorama service request to describe a resource
   */
  static DescribePackageVersionRequest translateToDescribeRequestForUpdate(final ResourceModel model) {
    DescribePackageVersionRequest.Builder builder = DescribePackageVersionRequest.builder()
            .packageId(model.getPackageId())
            .packageVersion(model.getPackageVersion())
            .patchVersion(model.getPatchVersion());

    if (model.getOwnerAccount() != null) {
      builder.ownerAccount(model.getOwnerAccount());
    }

    return builder.build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param response the Panorama service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final DescribePackageVersionResponse response) {
    return ResourceModel.builder()
            .ownerAccount(response.ownerAccount())
            .packageId(response.packageId())
            .packageArn(response.packageArn())
            .packageName(response.packageName())
            .packageVersion(response.packageVersion())
            .patchVersion(response.patchVersion())
            .isLatestPatch(response.isLatestPatch())
            .markLatest(response.isLatestPatch())
            .status(response.statusAsString())
            .statusDescription(response.statusDescription())
            .registeredTime(Long.valueOf(response.registeredTime().getEpochSecond()).intValue())
            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return DeregisterPackageVersionRequest the Panorama service request to delete a resource
   */
  static DeregisterPackageVersionRequest translateToDeleteRequest(final ResourceModel model) {
    DeregisterPackageVersionRequest.Builder builder = DeregisterPackageVersionRequest.builder()
            .packageId(model.getPackageId())
            .packageVersion(model.getPackageVersion())
            .patchVersion(model.getPatchVersion());

    if (model.getOwnerAccount() != null) {
      builder.ownerAccount(model.getOwnerAccount());
    }

    if (model.getUpdatedLatestPatchVersion() != null) {
      builder.updatedLatestPatchVersion(model.getUpdatedLatestPatchVersion());
    }

    return builder.build();
  }
}
