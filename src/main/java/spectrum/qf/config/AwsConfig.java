package spectrum.qf.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsConfig {

    @Value(value = "${s3.accessKey}")
    private String accessKey;
    @Value(value = "${s3.secretKey}")
    private String awsSecretKey;
    @Value(value = "${s3.region}")
    private String awsRegion;
    @Value(value = "${s3.url}")
    private String endpoint;

    @Bean
    public AmazonS3 amazonS3client() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, awsSecretKey);

        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);

        System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
        System.setProperty("com.amazonaws.sdk.disableCertChecking", "true");
        System.setProperty("s3utils.disableSocket", "true");

        return AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, awsRegion))
                .withClientConfiguration(new ClientConfiguration().withSocketBufferSizeHints(0, 1024 * 1024))
                .build();
    }
}



