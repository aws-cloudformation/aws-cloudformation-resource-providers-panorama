package software.amazon.panorama.package_;

public class Constants {
    public static String TEST_PACKAGE_NAME = "test-package-name";
    public static String TEST_PACKAGE_ID = "package-226t5xm5k6j6lahmcwy5ec6sky";
    public static String TEST_PACKAGE_ARN = "arn:aws:panorama:us-west-2:028663699634:package/package-226t5xm5k6j6lahmcwy5ec6sky";
    public static StorageLocation TEST_STORAGE_LOCATION = StorageLocation.builder()
            .bucket("test-bucket")
            .repoPrefixLocation("prefex/*")
            .binaryPrefixLocation("binaries/*")
            .manifestPrefixLocation("manifest/*")
            .generatedPrefixLocation("generated/*")
            .build();
}
