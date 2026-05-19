package com.digitalbank.transaction.domain.model;

public enum TransactionStatus {

    PENDING("Pendente"),
    COMPLETED("Concluída"),
    FAILED("Falhou"),
    REVERSED("Estornada");

    private final String label;

    TransactionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
