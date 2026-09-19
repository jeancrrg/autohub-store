package com.autohubstore.catalogservice.acceptance.config;

import com.autohubstore.catalogservice.CatalogServiceApplication;

import io.cucumber.spring.CucumberContextConfiguration;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@CucumberContextConfiguration
@SpringBootTest(classes = CatalogServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CucumberConfig {

    private static final int REDIS_PORT = 6379;

    private static final PostgreSQLContainer POSTGRES_CONTAINER =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("catalog_db")
                    .withUsername("catalog_user")
                    .withPassword("catalog_pass");

    private static final KafkaContainer KAFKA_CONTAINER =
            new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

    private static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(REDIS_PORT);

    private static final MinIOContainer MINIO_CONTAINER =
            new MinIOContainer(DockerImageName.parse("minio/minio:latest"));

    private static final String BUCKET_NAME = "catalog-images";

    static {
        POSTGRES_CONTAINER.start();
        KAFKA_CONTAINER.start();
        REDIS_CONTAINER.start();
        MINIO_CONTAINER.start();
        createBucket();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(REDIS_PORT));
        registry.add("spring.minio.endpoint", MINIO_CONTAINER::getS3URL);
        registry.add("spring.minio.access-key", MINIO_CONTAINER::getUserName);
        registry.add("spring.minio.secret-key", MINIO_CONTAINER::getPassword);
        registry.add("spring.minio.bucket", () -> BUCKET_NAME);
    }

    private static void createBucket() {
        try {
            MinioClient bootstrapClient = MinioClient.builder()
                    .endpoint(MINIO_CONTAINER.getS3URL())
                    .credentials(MINIO_CONTAINER.getUserName(), MINIO_CONTAINER.getPassword())
                    .build();
            bootstrapClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET_NAME).build());
        }
        catch (MinioException ex) {
            throw new IllegalStateException("Falha ao preparar bucket de teste do MinIO", ex);
        }
    }

}
