package com.artbazaar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full context: runs the Flyway migrations, and Hibernate's ddl-auto=validate
 * fails here if any entity drifts from the schema.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArtBazaarApplicationTests {

    @Autowired
    MockMvc mvc;

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void categoriesAreSeededAndPublic() throws Exception {
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].slug").value("paintings"));
    }

    @Test
    void unknownCategoryReturns404Json() throws Exception {
        mvc.perform(get("/api/categories/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category 'nope' not found"));
    }

    @Test
    void protectedEndpointReturns401Json() throws Exception {
        mvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void adminEndpointRejectsAnonymous() throws Exception {
        mvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}
