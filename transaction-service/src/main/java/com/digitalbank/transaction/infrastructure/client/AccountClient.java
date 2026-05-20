package com.digitalbank.transaction.infrastructure.client;

import com.digitalbank.transaction.infrastructure.client.dto.AccountSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "account-service")
public interface AccountClient {

    @GetMapping("/api/v1/accounts/{id}")
    AccountSummary findById(@PathVariable UUID id);
}
