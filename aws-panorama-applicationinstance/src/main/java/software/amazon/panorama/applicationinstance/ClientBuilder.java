package software.amazon.panorama.applicationinstance;

import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

  public static PanoramaClient getClient() {
    return PanoramaClient.builder()
              .httpClient(LambdaWrapper.HTTP_CLIENT)
              .build();
  }
}
