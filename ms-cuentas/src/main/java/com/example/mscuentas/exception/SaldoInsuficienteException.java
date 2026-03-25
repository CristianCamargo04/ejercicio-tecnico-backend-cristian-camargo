package com.example.mscuentas.exception;

public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException() {
        super("Saldo no disponible");
    }
}
