package software.amazon.panorama.package_;

import com.google.common.collect.Sets;
import software.amazon.awssdk.services.panorama.PanoramaClient;
import software.amazon.awssdk.services.panorama.model.DescribePackageRequest;
import software.amazon.awssdk.services.panorama.model.DescribePackageResponse;
import software.amazon.awssdk.services.panorama.model.ResourceNotFoundException;
import software.amazon.awssdk.services.panorama.model.TagResourceRequest;
import software.amazon.awssdk.services.panorama.model.UntagResourceRequest;
import software.amazon.awssdk.services.panorama.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


public class UpdateHandler extends BaseHandlerStd {
    private LoggerWrapper logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<PanoramaClient> proxyClient,
            final Logger logger) {

        this.logger = new LoggerWrapper(logger);

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-Panorama-Package::ValidateResourceExists", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToReadRequest)
                                .makeServiceCall(this::validateResourceExists)
                                .progress()
                )
                .then(progress -> updateTags(proxyClient, progress, request))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private DescribePackageResponse validateResourceExists(DescribePackageRequest describePackageRequest, ProxyClient<PanoramaClient> proxyClient) {
        DescribePackageResponse describePackageResponse;
        try {
            describePackageResponse = proxyClient.injectCredentialsAndInvokeV2(describePackageRequest, proxyClient.client()::describePackage);
        } catch (ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME,
                    String.format("PackageId:%s", describePackageRequest.packageId()), e);
        }
        return describePackageResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> updateTags(final ProxyClient<PanoramaClient> proxyClient,
                                                                     final ProgressEvent<ResourceModel, CallbackContext> progress,
                                                                     final ResourceHandlerRequest<ResourceModel> request) {
        ResourceModel currResourceModel = request.getDesiredResourceState();
        ResourceModel prevResourceModel = request.getPreviousResourceState();

        CallbackContext callbackContext = progress.getCallbackContext();
        Set<Tag> currentTags;
        if (currResourceModel.getTags() != null) {
            currentTags = currResourceModel.getTags().stream().collect(Collectors.toSet());
        } else {
            currentTags = new HashSet<>();
        }

        Set<Tag> existingTags = new HashSet<>();
        if (prevResourceModel != null && prevResourceModel.getTags() != null) {
            existingTags = prevResourceModel.getTags().stream().collect(Collectors.toSet());
        }

        final DescribePackageRequest describePackageRequest = Translator.translateToReadRequest(currResourceModel);
        DescribePackageResponse describePackageResponse = proxyClient.injectCredentialsAndInvokeV2(describePackageRequest,
                proxyClient.client()::describePackage);
        String arn = describePackageResponse.arn();

        final Set<Tag> tagsToAdd = Sets.difference(currentTags, existingTags);
        if (!tagsToAdd.isEmpty()) {
            TagResourceRequest tagResourceRequest = Translator.translateToTagResourceRequest(tagsToAdd, arn);
            try {
                proxyClient.injectCredentialsAndInvokeV2(tagResourceRequest, proxyClient.client()::tagResource);
            } catch (ValidationException e) {
                throw new CfnInvalidRequestException(e.getMessage(), e);
            }
        }

        final Set<Tag> tagsToRemove = Sets.difference(existingTags, currentTags);
        if (!tagsToRemove.isEmpty()) {
            UntagResourceRequest untagResourceRequest = Translator.translateToUntagResourceRequest(tagsToRemove, arn);
            try {
                proxyClient.injectCredentialsAndInvokeV2(untagResourceRequest, proxyClient.client()::untagResource);
            } catch (ValidationException e) {
                throw new CfnInvalidRequestException(e.getMessage(), e);
            }
        }
        return ProgressEvent.progress(currResourceModel, callbackContext);
    }
}
