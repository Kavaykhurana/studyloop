package com.studyloop;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studyloop.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class SnapIntegrationTest extends IntegrationTestBase {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};

    @Test
    void aValidImageIsTranscribedIntoAnEditableDoubt() throws Exception {
        var user = fx.tiet("Asha Learner");
        mvc.perform(multipart("/api/ai/snap").file(new MockMultipartFile("image", "q.jpg", "image/jpeg", JPEG))
                        .header("Authorization", fx.bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Transcribed doubt"))
                .andExpect(jsonPath("$.subjectId").isNumber())
                .andExpect(jsonPath("$.confidence").value("MEDIUM"));
        mvc.perform(multipart("/api/ai/snap").file(new MockMultipartFile("image", "q.png", "image/png", PNG))
                        .header("Authorization", fx.bearer(user)))
                .andExpect(status().isOk());
    }

    @Test
    void theFileSignatureDecidesNotTheDeclaredContentType() throws Exception {
        var user = fx.tiet("Asha Learner");
        byte[] notAnImage = "this is plain text pretending to be a photo".getBytes();
        mvc.perform(multipart("/api/ai/snap").file(new MockMultipartFile("image", "x.jpg", "image/jpeg", notAnImage))
                        .header("Authorization", fx.bearer(user)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_IMAGE"));
    }

    @Test
    void snapRequiresSignIn() throws Exception {
        mvc.perform(multipart("/api/ai/snap").file(new MockMultipartFile("image", "q.jpg", "image/jpeg", JPEG)))
                .andExpect(status().isUnauthorized());
    }
}
