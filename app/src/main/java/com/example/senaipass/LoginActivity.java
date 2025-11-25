package com.example.senaipass;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {

    private EditText editCpf;
    private EditText editSenha;
    private Button btnLogin;

    // IP do HOST/Backend para o Emulador Android
    // Seu servidor Node.js/Express deve estar rodando em http://localhost:3000
    private static final String BASE_URL = "http://10.0.2.2:3000/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Captura as Views do XML
        editCpf = findViewById(R.id.cpf);
        editSenha = findViewById(R.id.senha);
        btnLogin = findViewById(R.id.entrar);

        // 2. Inicializa Retrofit (O tradutor/conector da rede)
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        final AlunoService service = retrofit.create(AlunoService.class);

        // 3. Define a Ação do Botão
        btnLogin.setOnClickListener(v -> {
            String cpf = editCpf.getText().toString().trim();
            String senha = editSenha.getText().toString().trim();

            if (cpf.isEmpty() || senha.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Preencha o CPF e a Senha.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 4. Cria o Request e Faz a Chamada de Rede Assíncrona
            LoginRequest request = new LoginRequest(cpf, senha);
            Call<LoginResponse> call = service.login(request);

            call.enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // Login bem-sucedido!
                        LoginResponse aluno = response.body();
                        Toast.makeText(LoginActivity.this, "Bem-vindo(a), " + aluno.getNome_completo().split(" ")[0] + ".", Toast.LENGTH_LONG).show();

                        // 5. Navega para HomeActivity, PASSANDO os dados essenciais
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                        intent.putExtra("ALUNO_ID", aluno.getId_aluno()); // ID para gerar o QR Code
                        intent.putExtra("ALUNO_NOME", aluno.getNome_completo());
                        intent.putExtra("ALUNO_IMG_URL", aluno.getImg_url()); // URL para carregar a imagem

                        startActivity(intent);
                        finish(); // Fecha a tela de login
                    } else {
                        // Resposta de erro do servidor (ex: 401 Unauthorized)
                        Toast.makeText(LoginActivity.this, "CPF ou Senha inválidos.", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    // Erro de conexão (servidor Node.js desligado, 10.0.2.2 errado, etc.)
                    Log.e("LOGIN_API", "Erro de Conexão: " + t.getMessage());
                    Toast.makeText(LoginActivity.this, "Erro de conexão. Verifique se o servidor Node.js está rodando.", Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}