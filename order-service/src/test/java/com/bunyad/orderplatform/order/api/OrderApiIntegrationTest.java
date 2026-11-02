package com.bunyad.orderplatform.order.api;

import com.bunyad.orderplatform.order.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderApiIntegrationTest {

    private static final String VALID_ORDER = """
            {
              "customerId": "customer-42",
              "lines": [
                { "productCode": "SKU-1", "quantity": 2, "unitPrice": 15.00 },
                { "productCode": "SKU-2", "quantity": 1, "unitPrice": 9.90 }
              ]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void placesAndFetchesOrder() throws Exception {
        String id = placeOrder();

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(39.90))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    @Test
    void confirmsThenCancelsOrder() throws Exception {
        String id = placeOrder();

        mockMvc.perform(post("/api/orders/{id}/confirm", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/orders/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void returnsConflictWhenConfirmingCancelledOrder() throws Exception {
        String id = placeOrder();
        mockMvc.perform(post("/api/orders/{id}/cancel", id)).andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/{id}/confirm", id))
                .andExpect(status().isConflict());
    }

    @Test
    void returnsNotFoundForUnknownOrder() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "customerId": "", "lines": [] }
                                """))
                .andExpect(status().isBadRequest());
    }

    private String placeOrder() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
