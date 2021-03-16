package software.amazon.panorama.packageversion;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.panorama.model.DeregisterPackageVersionRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageVersionRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageVersionResponse;
import software.amazon.awssdk.services.panorama.model.PackageVersionStatus;
import software.amazon.awssdk.services.panorama.model.RegisterPackageVersionRequest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.panorama.packageversion.Constants.TEST_OWNER_ACCOUNT;
import static software.amazon.panorama.packageversion.Constants.TEST_PACKAGE_ARN;
import static software.amazon.panorama.packageversion.Constants.TEST_PACKAGE_ID;
import static software.amazon.panorama.packageversion.Constants.TEST_PACKAGE_NAME;
import static software.amazon.panorama.packageversion.Constants.TEST_PACKAGE_STATUS_DESCRIPTION;
import static software.amazon.panorama.packageversion.Constants.TEST_PACKAGE_VERSION;
import static software.amazon.panorama.packageversion.Constants.TEST_PATCH_VERSION;

public class TranslatorTest {

    @Test
    void testTranslateToReadRequest() {
        ResourceModel model = ResourceModel.builder()
                .packageId(TEST_PACKAGE_ID)
                .packageVersion(TEST_PACKAGE_VERSION)
                .patchVersion(TEST_PATCH_VERSION)
                .build();
        DescribePackageVersionRequest request = Translator.translateToReadRequest(model);
        assertThat(request.packageId()).isEqualTo(TEST_PACKAGE_ID);
        assertThat(request.packageVersion()).isEqualTo(TEST_PACKAGE_VERSION);
        assertThat(request.patchVersion()).isEqualTo(TEST_PATCH_VERSION);
    }

    @Test
    void testTranslateToDeleteRequest() {
        ResourceModel model = ResourceModel.builder()
                .packageId(TEST_PACKAGE_ID)
                .packageVersion(TEST_PACKAGE_VERSION)
                .patchVersion(TEST_PATCH_VERSION).build();
        DeregisterPackageVersionRequest request = Translator.translateToDeleteRequest(model);
        assertThat(request.packageId()).isEqualTo(TEST_PACKAGE_ID);
        assertThat(request.packageVersion()).isEqualTo(TEST_PACKAGE_VERSION);
        assertThat(request.patchVersion()).isEqualTo(TEST_PATCH_VERSION);
    }

    @Test
    void testTranslateToCreateRequest() {
        ResourceModel model = ResourceModel.builder()
                .packageId(TEST_PACKAGE_ID)
                .packageVersion(TEST_PACKAGE_VERSION)
                .patchVersion(TEST_PATCH_VERSION)
                .build();
        RegisterPackageVersionRequest request = Translator.translateToCreateRequest(model);
        assertThat(request.packageId()).isEqualTo(TEST_PACKAGE_ID);
        assertThat(request.packageVersion()).isEqualTo(TEST_PACKAGE_VERSION);
        assertThat(request.patchVersion()).isEqualTo(TEST_PATCH_VERSION);
    }

    @Test
    void testTranslateToDescribeRequestForUpdate() {
        ResourceModel model = ResourceModel.builder()
                .packageId(TEST_PACKAGE_ID)
                .packageVersion(TEST_PACKAGE_VERSION)
                .patchVersion(TEST_PATCH_VERSION)
                .ownerAccount(TEST_OWNER_ACCOUNT)
                .build();
        DescribePackageVersionRequest request = Translator.translateToDescribeRequestForUpdate(model);
        assertThat(request.packageId()).isEqualTo(TEST_PACKAGE_ID);
        assertThat(request.packageVersion()).isEqualTo(TEST_PACKAGE_VERSION);
        assertThat(request.ownerAccount()).isEqualTo(TEST_OWNER_ACCOUNT);
        assertThat(request.patchVersion()).isEqualTo(TEST_PATCH_VERSION);
    }

    @Test
    void testTranslateFromReadResponse() {
        DescribePackageVersionResponse response = DescribePackageVersionResponse.builder()
                .ownerAccount(TEST_OWNER_ACCOUNT)
                .packageId(TEST_PACKAGE_ID)
                .packageArn(TEST_PACKAGE_ARN)
                .packageName(TEST_PACKAGE_NAME)
                .packageVersion(TEST_PACKAGE_VERSION)
                .patchVersion(TEST_PATCH_VERSION)
                .isLatestPatch(true)
                .status(PackageVersionStatus.REGISTER_COMPLETED)
                .statusDescription(TEST_PACKAGE_STATUS_DESCRIPTION)
                .registeredTime(Instant.ofEpochSecond(1000))
                .build();

        ResourceModel model = Translator.translateFromReadResponse(response);
        assertThat(model.getOwnerAccount()).isEqualTo(TEST_OWNER_ACCOUNT);
        assertThat(model.getPackageId()).isEqualTo(TEST_PACKAGE_ID);
        assertThat(model.getPackageArn()).isEqualTo(TEST_PACKAGE_ARN);
        assertThat(model.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(model.getPackageVersion()).isEqualTo(TEST_PACKAGE_VERSION);
        assertThat(model.getPatchVersion()).isEqualTo(TEST_PATCH_VERSION);
        assertThat(model.getIsLatestPatch()).isTrue();
        assertThat(model.getMarkLatest()).isTrue();
        assertThat(model.getStatus()).isEqualTo(PackageVersionStatus.REGISTER_COMPLETED.toString());
        assertThat(model.getStatusDescription()).isEqualTo(TEST_PACKAGE_STATUS_DESCRIPTION);
        assertThat(model.getRegisteredTime()).isEqualTo(1000);
    }
}
