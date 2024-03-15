package qiwifiless3.demo.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
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

    @Value(value = "${aws.accessKey}")
    private String accessKey;
    @Value(value = "${aws.secretKey}")
    private String awsSecretKey;
    @Value(value = "${aws.region}")
    private String awsRegion;
    @Value(value = "${aws.url.storage}")
    private String endpoint;
    @Value(value = "${aws.bucket.name}")
    private String bucketName;

    @Bean
    public AmazonS3 amazonS3client() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, awsSecretKey);

        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);

        //AmazonS3Client con = new AmazonS3Client(credentials, clientConfig);
        //S3ClientOptions options = S3ClientOptions.builder().setPathStyleAccess(true).build();
        //con.setS3ClientOptions(options);
        //con.setEndpoint(endpoint);

        AmazonS3 s3client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, awsRegion))
                .build();

        return s3client;

        //      AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
        //                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, Regions.DEFAULT_REGION.getName()))
        //                .build();


        //return AmazonS3ClientBuilder.standard()
        //        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        //        .withRegion(awsRegion)
        //        .build();
    }
}



