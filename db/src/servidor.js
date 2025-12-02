// atualização concluída
const express = require('express');
const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const cors = require('cors');
const fs = require('fs');
const bcrypt = require('bcryptjs');
const multer = require('multer');

const app = express();

// Middlewares
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Views / static
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, '..', 'views'));
app.use(express.static(path.join(__dirname, '..', 'public')));

// cria uploads dir se não existir
const uploadsDir = path.join(__dirname, '..', 'public', 'uploads');
fs.mkdirSync(uploadsDir, { recursive: true });

// multer: storage, limits e fileFilter (apenas imagens, max 2MB)
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadsDir),
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname) || '';
    cb(null, Date.now() + ext);
  }
});
const upload = multer({
  storage,
  limits: { fileSize: 2 * 1024 * 1024 }, // 2MB
  fileFilter: (req, file, cb) => {
    if (!file.mimetype || !file.mimetype.startsWith('image/')) {
      return cb(new Error('Apenas imagens são permitidas'));
    }
    cb(null, true);
  }
});

// Inicializa/abre DB (use seu senaipass.db em db/senaipass.db)
const dbPath = path.join(__dirname, '..', 'senaipass.db');
const db = new sqlite3.Database(dbPath, (err) => {
  if (err) {
    console.error('Erro ao abrir DB:', err.message);
  } else {
    console.log('DB aberto em', dbPath);
  }
});

// Cria tabela se não existir - esquema compatível com seu DB atual (nome_aluno)
db.run(
  `CREATE TABLE IF NOT EXISTS aluno (
     id_aluno TEXT PRIMARY KEY,
     cpf TEXT UNIQUE,
     senha TEXT,
     img TEXT,
     nome_aluno TEXT
   )`,
  (err) => {
    if (err) console.error('Erro ao criar tabela:', err.message);
  }
);

// Rotas de página
app.get('/cadastro', (req, res) => res.render('cadastro'));
app.get('/login', (req, res) => res.render('login'));

// Rota de registro (multipart/form-data)
app.post('/api/aluno/register', upload.single('img'), (req, res) => {
  try {
    let { id_aluno, cpf, senha, nome_completo } = req.body || {};
    if (!id_aluno || !cpf || !senha) {
      return res.status(400).json({ error: 'id_aluno, cpf e senha são obrigatórios' });
    }

    // normaliza CPF (apenas dígitos) e valida tamanho simples
    cpf = String(cpf).replace(/\D/g, '');
    if (cpf.length !== 11) return res.status(400).json({ error: 'CPF inválido' });

    id_aluno = String(id_aluno).trim();
    const nome_aluno = (nome_completo || '').trim();

    db.get('SELECT * FROM aluno WHERE cpf = ? OR id_aluno = ?', [cpf, id_aluno], (err, existing) => {
      if (err) {
        console.error(err);
        return res.status(500).json({ error: 'Erro no servidor' });
      }
      if (existing) return res.status(409).json({ error: 'CPF ou id_aluno já cadastrado' });

      bcrypt.hash(senha, 12, (hashErr, hashed) => {
        if (hashErr) {
          console.error(hashErr);
          return res.status(500).json({ error: 'Erro ao hashear senha' });
        }

        const imgPath = req.file ? ('/uploads/' + path.basename(req.file.filename)) : null;

        const sql = `INSERT INTO aluno (id_aluno, cpf, senha, img, nome_aluno) VALUES (?, ?, ?, ?, ?)`;
        db.run(sql, [id_aluno, cpf, hashed, imgPath, nome_aluno], function(insertErr) {
          if (insertErr) {
            console.error('Erro ao inserir aluno:', insertErr.message);
            return res.status(500).json({ error: 'Erro ao inserir aluno' });
          }
          return res.json({ success: true, id: id_aluno });
        });
      });
    });
  } catch (e) {
    console.error(e);
    return res.status(500).json({ error: 'Erro no servidor' });
  }
});

// Lista alunos (sem senha) - mapeia nome_aluno para nome_completo
app.get('/api/aluno', (req, res) => {
  db.all('SELECT id_aluno, cpf, nome_aluno AS nome_completo, img FROM aluno', [], (err, rows) => {
    if (err) {
      console.error(err);
      return res.status(500).json({ error: 'Erro no servidor' });
    }
    return res.json(rows);
  });
});

// Login - normaliza CPF e compara hash
app.post('/api/aluno/login', (req, res) => {
  const { cpf, senha } = req.body || {};
  if (!cpf || !senha) return res.status(400).json({ error: 'cpf e senha são obrigatórios' });

  const cpfNorm = String(cpf).replace(/\D/g, '');
  const sql = 'SELECT * FROM aluno WHERE cpf = ? LIMIT 1';
  db.get(sql, [cpfNorm], (err, row) => {
    if (err) {
      console.error('Erro na query:', err && err.message);
      return res.status(500).json({ error: 'Erro no servidor' });
    }
    if (!row) return res.status(401).json({ error: 'CPF ou senha inválidos' });

    bcrypt.compare(senha, row.senha, (cmpErr, ok) => {
      if (cmpErr) {
        console.error('Erro no compare:', cmpErr);
        return res.status(500).json({ error: 'Erro no servidor' });
      }
      if (!ok) return res.status(401).json({ error: 'CPF ou senha inválidos' });

      const response = {
        id_aluno: row.id_aluno || '',
        nome_completo: row.nome_aluno || '',
        img_url: row.img || ''
      };
      return res.json(response);
    });
  });
});

// tratamento simples de erro de upload
app.use((err, req, res, next) => {
  if (err && err.message && err.message.includes('Apenas imagens')) {
    return res.status(400).json({ error: err.message });
  }
  if (err && err.code === 'LIMIT_FILE_SIZE') {
    return res.status(400).json({ error: 'Arquivo muito grande. Máx 2MB.' });
  }
  next(err);
});

// Inicia servidor
const port = process.env.PORT || 3000;
app.listen(port, () => {
  console.log(`Servidor rodando na porta ${port}`);
});
// ...existing code...