package genius.project.orchestrate.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GracefulShutdownTestController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "orchestrate.test-endpoints.enabled=true")
class GracefulShutdownTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void slowRequestWaitsAndReturnsSuccess() throws Exception {
        mockMvc.perform(get("/__test/slow").param("seconds", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Completed after 1 second"));
    }

    @Test
    void rejectsDelayOutsideAllowedRange() throws Exception {
        mockMvc.perform(get("/__test/slow").param("seconds", "26"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("seconds must be between 1 and 25"));
    }
}
