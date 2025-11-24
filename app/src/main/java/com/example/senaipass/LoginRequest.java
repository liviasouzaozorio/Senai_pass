package com.example.senaipass;

// O Retrofit usa esta classe para montar o JSON que será enviado no POST.
public class LoginRequest {
    private String cpf;
    private String senha;

    // Construtor necessário para criar o objeto no LoginActivity
    public LoginRequest(String cpf, String senha) {
        this.cpf = cpf;
        this.senha = senha;
    }

    // Não precisa de getters/setters, apenas o construtor é suficiente para o Retrofit enviar os dados.
}