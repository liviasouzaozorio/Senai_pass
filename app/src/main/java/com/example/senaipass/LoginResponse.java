package com.example.senaipass;

// Esta classe precisa ter os mesmos nomes de variáveis que o seu Backend retorna!
public class LoginResponse {

    // Devem corresponder exatamente aos nomes retornados pelo server.js
    private String id_aluno;
    private String nome_completo;
    private String img_url;

    // Getters são OBRIGATÓRIOS para o Retrofit ler os dados do JSON
    public String getId_aluno() {
        return id_aluno;
    }

    public String getNome_completo() {
        return nome_completo;
    }

    public String getImg_url() {
        return img_url;
    }
}