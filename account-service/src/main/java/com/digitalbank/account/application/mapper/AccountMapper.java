package com.digitalbank.account.application.mapper;

import com.digitalbank.account.application.dto.AccountResponse;
import com.digitalbank.account.domain.model.Account;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountResponse toResponse(Account account);
}
