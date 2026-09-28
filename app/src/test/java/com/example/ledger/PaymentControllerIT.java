package com.example.ledger;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
public class PaymentControllerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mockMvc;

    @Test
    void recordsPayment() throws Exception {
        mockMvc.perform(
                        post("/payments")
                                .contentType("application/json")
                                .content("""
                                {
                                  "merchantId": "MR-4471",
                                  "amountMinor": 128450,
                                  "currency": "GBP"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    void returnsSettlement() throws Exception {
        mockMvc.perform(
                        post("/payments")
                                .contentType("application/json")
                                .content("""
                            {
                              "merchantId": "MR-4471",
                              "amountMinor": 128450,
                              "currency": "GBP"
                            }
                            """)
                )
                .andExpect(status().isCreated());

        var result = mockMvc.perform(
                        get("/payments/settlement")
                                .param("merchantId", "MR-4471")
                )
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        assertThat(responseBody)
                .contains("\"amountOwed\":124469");
    }

    @Test
    void rejectsNegativeAmount() throws Exception {
        mockMvc.perform(
                        post("/payments")
                                .contentType("application/json")
                                .content("""
                            {
                              "merchantId": "MR-4471",
                              "amountMinor": -100,
                              "currency": "GBP"
                            }
                            """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsBlankMerchantId() throws Exception {
        mockMvc.perform(
                        post("/payments")
                                .contentType("application/json")
                                .content("""
                            {
                              "merchantId": "",
                              "amountMinor": 128450,
                              "currency": "GBP"
                            }
                            """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnknownMerchant() throws Exception {
        mockMvc.perform(
                        get("/payments/settlement")
                                .param("merchantId", "UNKNOWN")
                )
                .andExpect(status().isNotFound());
    }
}

