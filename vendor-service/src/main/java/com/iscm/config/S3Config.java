// package com.iscm.config;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// import java.net.URI;

// @Configuration
// public class S3Config {
    
//     @Value("${aws.s3.endpoint:}")
//     private String s3Endpoint;
    
//     @Value("${aws.s3.region:us-east-1}")
//     private String region;
    
//     @Value("${aws.s3.access-key}")
//     private String accessKey;
    
//     @Value("${aws.s3.secret-key}")
//     private String secretKey;
    
//     @Bean
//     public S3Client s3Client() {
//         var builder = S3Client.builder()
//             .region(Region.of(region))
//             .credentialsProvider(StaticCredentialsProvider.create(
//                 AwsBasicCredentials.create(accessKey, secretKey)
//             ));
        
//         // For MinIO or localstack
//         if (s3Endpoint != null && !s3Endpoint.isEmpty()) {
//             builder.endpointOverride(URI.create(s3Endpoint));
//         }
        
//         return builder.build();
//     }
// }