package com.example.senaipass;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

    public interface AlunoService {

        @POST("/api/aluno/login")
        Call<LoginResponse> login(@Body LoginRequest request);
    }

