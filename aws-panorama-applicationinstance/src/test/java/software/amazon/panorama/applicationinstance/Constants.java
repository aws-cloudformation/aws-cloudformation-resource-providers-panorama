package software.amazon.panorama.applicationinstance;

public class Constants {
    public static final String APPLICATION_INSTANCE_ID = "test_application_instance_id";
    public static final String APPLICATION_INSTANCE_NAME = "test_application_instance_name";
    public static final String APPLICATION_INSTANCE_DESCRIPTION = "test_application_instance_description";
    public static final String DEVICE_ARN = "arn:aws:panorama:us-east-1:028663699634:device/automation-default";
    public static final String DEVICE_ID = "automation-default";
    public static final String MANIFEST_PAYLOAD = "{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}";
    public static final String MANIFEST_PAYLOAD_OVERRIDES = "{\"name\":\"Henry\"}";
    public static final String RUNTIME_ROLE_ARN = "arn:aws:iam::627146104544:role/service-role/AWSPanoramaApplianceRole";
    public static final String STATUS_DESCRIPTION = "Application Instance status is pending";
    public static final Integer CREATED_TIME = 1618354585;
    public static final Integer LAST_UPDATED_TIME = 1618354585;
}
