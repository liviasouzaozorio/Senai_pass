// ...existing code...
package com.example.senaipass;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HomeActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSION = 1001;

    // Mantenha o mesmo BASE_URL usado no LoginActivity
    private static final String BASE_URL = "http://172.18.43.249:3000";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Recebe os extras enviados pelo LoginActivity
        String alunoId = getIntent().getStringExtra("ALUNO_ID");
        String alunoNome = getIntent().getStringExtra("ALUNO_NOME");
        String alunoImgUrl = getIntent().getStringExtra("ALUNO_IMG_URL");

        ImageView fotoAluno = findViewById(R.id.foto_aluno);
        ImageView qrcodeImg = findViewById(R.id.qrcodeimg);

        // Gera QR code com o id_aluno e mostra em qrcodeimg
        if (alunoId != null && !alunoId.isEmpty() && qrcodeImg != null) {
            try {
                int size = 600; // tamanho do QR em pixels
                BitMatrix bitMatrix = new MultiFormatWriter().encode(alunoId, BarcodeFormat.QR_CODE, size, size);
                BarcodeEncoder encoder = new BarcodeEncoder();
                Bitmap bitmapQR = encoder.createBitmap(bitMatrix);
                qrcodeImg.setImageBitmap(bitmapQR);
            } catch (WriterException e) {
                e.printStackTrace();
            }
        }

        // Carrega a imagem do aluno (img_url) do servidor e coloca em foto_aluno
        if (alunoImgUrl != null && !alunoImgUrl.isEmpty() && fotoAluno != null) {
            final String imgFullUrl = alunoImgUrl.startsWith("http")
                    ? alunoImgUrl
                    : BASE_URL + (alunoImgUrl.startsWith("/") ? alunoImgUrl : "/" + alunoImgUrl);

            // Carregamento simples em background (substituir por Glide/Picasso se preferir)
            new Thread(() -> {
                InputStream is = null;
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(imgFullUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    is = conn.getInputStream();
                    final Bitmap bmp = BitmapFactory.decodeStream(is);
                    runOnUiThread(() -> fotoAluno.setImageBitmap(bmp));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try { if (is != null) is.close(); } catch (IOException ignored) {}
                    if (conn != null) conn.disconnect();
                }
            }).start();
        }

        findViewById(R.id.BaixarPdf).setOnClickListener(v -> {
            // Em Android 10+ (API 29) usamos MediaStore e normalmente não precisamos da permissão WRITE_EXTERNAL_STORAGE.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_PERMISSION);
                    return;
                }
            }
            gerarPdfDaTela();
        });
    }

    private Bitmap captureViewBitmap(View view) {
        int width = view.getWidth();
        int height = view.getHeight();
        if (width == 0 || height == 0) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            width = dm.widthPixels;
            height = dm.heightPixels;
            view.layout(0, 0, width, height);
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private void gerarPdfDaTela() {
        // 1. Pega a view principal
        View content = findViewById(R.id.home);
        if (content == null) {
            Toast.makeText(this, "Erro: view principal não encontrada.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Captura bitmap da tela (inclui qrcodeimg e foto_aluno)
        Bitmap bitmap = captureViewBitmap(content);

        // 3. Cria documento PDF com as mesmas dimensões do bitmap
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        page.getCanvas().drawBitmap(bitmap, 0, 0, null);
        document.finishPage(page);

        String fileName = "Carteirinha_SENAI.pdf";
        OutputStream out = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SenaiPass");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new IOException("Não foi possível criar arquivo via MediaStore");
                out = getContentResolver().openOutputStream(uri);
            } else {
                File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File folder = new File(directory, "SenaiPass");
                if (!folder.exists()) folder.mkdirs();
                File file = new File(folder, fileName);
                out = new FileOutputStream(file);
            }

            if (out == null) throw new IOException("OutputStream nulo ao salvar PDF");
            document.writeTo(out);

            String msg = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? "PDF salvo em Downloads/SenaiPass"
                    : "PDF salvo em: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/SenaiPass/" + fileName;
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao gerar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try { if (out != null) out.close(); } catch (IOException ignored) {}
            document.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                gerarPdfDaTela();
            } else {
                Toast.makeText(this, "Permissão necessária para salvar o PDF.", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
// ...existing code...
