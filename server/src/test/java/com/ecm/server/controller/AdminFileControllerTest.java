package com.ecm.server.controller;

import com.ecm.server.dto.response.FileResponse;
import com.ecm.server.exception.GlobalExceptionHandler;
import com.ecm.server.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminFileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private AdminFileController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void uploadImageReturnsCreatedEnvelope() throws Exception {
        UUID id = UUID.randomUUID();
        when(fileStorageService.uploadImage(any())).thenReturn(FileResponse.builder()
                .id(id)
                .originalName("hero.png")
                .mimeType("image/png")
                .sizeBytes(3)
                .publicUrl("http://localhost:8080/api/v1/files/" + id + "/content")
                .status("ACTIVE")
                .build());

        MockMultipartFile upload = new MockMultipartFile(
                "file", "hero.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/admin/files").file(upload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("CREATED"))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.mimeType").value("image/png"));
    }

    @Test
    void contentReturnsStoredImageWithInlineDisposition() throws Exception {
        UUID id = UUID.randomUUID();
        when(fileStorageService.open(id)).thenReturn(new FileStorageService.StoredFile(
                new ByteArrayResource("image".getBytes(StandardCharsets.UTF_8)),
                MediaType.IMAGE_PNG_VALUE,
                "hero.png"
        ));

        mockMvc.perform(get("/api/v1/files/{id}/content", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().string("image"));
    }
}
