package org.example.securitypractica;


import io.minio.*;
import io.minio.messages.Item;
import org.example.securitypractica.dto.ResourceDto;
import org.example.securitypractica.dto.ResourceType;
import org.example.securitypractica.exception.NotFoundException;
import org.example.securitypractica.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
class SecurityPracticaApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:RELEASE.2023-09-04T19-57-37Z");
    @Autowired
    private StorageService storageService;
    @Autowired
    private MinioClient minioClient;
    @Value("${minio.bucket-name}")
    private String bucketName;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("minio.endpoint", minio::getS3URL);
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @BeforeEach
    void clearStorage() {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );

            if (found) {
                Iterable<Result<Item>> results = minioClient.listObjects(
                        ListObjectsArgs.builder().bucket(bucketName).recursive(true).build()
                );

                for (Result<Item> result : results) {
                    Item item = result.get();
                    minioClient.removeObject(
                            RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(item.objectName())
                                    .build()
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Cleanup failed: " + e.getMessage());
        }
    }


    @Test
    void testUploadAndVerifyFile() {
        Long userId = 999L;
        String fileName = "my-notes.txt";
        String content = "Hello, world! This is a test file.";
        byte[] contentBytes = content.getBytes();

        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                fileName,
                "text/plain",
                contentBytes
        );

        storageService.uploadFiles("", List.of(mockFile), userId);

        ResourceDto result = storageService.getResource(fileName, userId);

        assertThat(result.name()).isEqualTo(fileName);
        assertThat(result.type()).isEqualTo(ResourceType.FILE);
        assertThat(result.size()).isEqualTo(contentBytes.length);
    }

    @Test
    void testRenameAndVerifyFile() {
        Long userId = 999L;
        String oldPath = "old-name.txt";
        String newName = "secret-plan.txt";
        byte[] contentBytes = "Top secret content".getBytes();

        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                oldPath,
                "text/plain",
                contentBytes
        );
        storageService.uploadFiles("", List.of(mockFile), userId);


        storageService.move(oldPath, newName, userId);


        ResourceDto newFile = storageService.getResource(newName, userId);
        assertThat(newFile.name()).isEqualTo(newName);
        assertThat(newFile.type()).isEqualTo(ResourceType.FILE);

        assertThatThrownBy(() ->
                storageService.getResource(oldPath, userId)
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    void testDeleteAndVerifyFile() {
        Long userId = 999L;
        String fileName = "goida.txt";
        byte[] contentBytes = "Gooiiida".getBytes();

        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                fileName,
                "text/plain",
                contentBytes
        );

        storageService.uploadFiles("", List.of(mockFile), userId);

        storageService.deleteResource(fileName, userId);

        assertThatThrownBy(() ->
                storageService.getResource(fileName, userId)
        ).isInstanceOf(NotFoundException.class);

    }

    @Test
    void testDeleteAndVerifyFolder() {
        Long userId = 999L;
        String path = "flowers/";

        String fileName = "flower.txt";
        byte[] contentBytes = "Flower".getBytes();

        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                fileName,
                "text/plain",
                contentBytes
        );

        storageService.createDirectory(path, userId);

        storageService.uploadFiles("flowers/", List.of(mockFile), userId);


        storageService.deleteResource(path, userId);

        assertThatThrownBy(() ->
                storageService.getResource(path, userId)
        ).isInstanceOf(NotFoundException.class);

        assertThatThrownBy(() ->
                storageService.getResource(path + "flower.txt", userId)
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    void testCreateAndCheckDirectory() {
        Long userId = 999L;
        String parentPath = "documents/";
        String childPath = "documents/workr/";

        storageService.createDirectory(parentPath, userId);

        storageService.createDirectory(childPath, userId);

        ResourceDto result = storageService.getResource(childPath, userId);

        assertThat(result.name()).isEqualTo("workr");
        assertThat(result.type()).isEqualTo(ResourceType.DIRECTORY);
    }

    @Test
    void testUserCannotAccessOthersFiles() {
        Long userId = 999L;
        Long otherUserId = 12L;
        String fileName = "my-notes.txt";
        String content = "Hello, world! This is a test file.";
        byte[] contentBytes = content.getBytes();

        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                fileName,
                "text/plain",
                contentBytes
        );

        storageService.uploadFiles("", List.of(mockFile), userId);

        assertThat(storageService.getResource(fileName, userId)).isNotNull();

        assertThatThrownBy(() ->
                storageService.getResource(fileName, otherUserId)
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    void testSearch() {
        Long userId = 999L;
        Long otherUserId = 12L;
        byte[] content = "test content".getBytes();

        MockMultipartFile targetFile = new MockMultipartFile("files", "filename.txt", "text/plain", content);
        MockMultipartFile noiseFile = new MockMultipartFile("files", "goida.txt", "text/plain", content);
        MockMultipartFile otherUserFile = new MockMultipartFile("files", "filename.txt", "text/plain", content);

        storageService.uploadFiles("", List.of(targetFile, noiseFile), userId);
        storageService.uploadFiles("", List.of(otherUserFile), otherUserId);

        var results = storageService.search("file", userId);

        assertThat(results).hasSize(1);

        assertThat(results.get(0).name()).isEqualTo("filename.txt");
    }

    @Test
    void testSearchCanFindTheDirectory() {
        Long userId = 999L;

        storageService.createDirectory("test/", userId);

        var results = storageService.search("test", userId);

        assertThat(results.get(0).name()).isEqualTo("test");

    }
}