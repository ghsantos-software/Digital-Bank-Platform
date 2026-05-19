package com.digitalbank.transaction.application.mapper;

import com.digitalbank.transaction.application.dto.TransactionResponse;
import com.digitalbank.transaction.domain.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "typeLabel",   expression = "java(transaction.getType().getLabel())")
    @Mapping(target = "statusLabel", expression = "java(transaction.getStatus().getLabel())")
    TransactionResponse toResponse(Transaction transaction);
}
