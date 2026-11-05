DROP DATABASE IF EXISTS cinema;
CREATE DATABASE cinema CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE cinema;


CREATE TABLE filme (
    id_filme      INT AUTO_INCREMENT PRIMARY KEY,
    titulo        VARCHAR(150) NOT NULL,
    duracao_min   INT          NOT NULL,
    classificacao VARCHAR(10)  NOT NULL,
    CONSTRAINT ck_filme_duracao CHECK (duracao_min > 0)
) ENGINE=InnoDB;


CREATE TABLE genero (
    id_genero INT AUTO_INCREMENT PRIMARY KEY,
    nome      VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB;


CREATE TABLE filme_genero (
    id_filme  INT NOT NULL,
    id_genero INT NOT NULL,
    PRIMARY KEY (id_filme, id_genero),
    CONSTRAINT fk_fg_filme  FOREIGN KEY (id_filme)  REFERENCES filme (id_filme)   ON DELETE CASCADE,
    CONSTRAINT fk_fg_genero FOREIGN KEY (id_genero) REFERENCES genero (id_genero) ON DELETE CASCADE
) ENGINE=InnoDB;


CREATE TABLE sala (
    id_sala    INT AUTO_INCREMENT PRIMARY KEY,
    nome       VARCHAR(50) NOT NULL UNIQUE,
    capacidade INT         NOT NULL,
    CONSTRAINT ck_sala_capacidade CHECK (capacidade > 0)
) ENGINE=InnoDB;



CREATE TABLE assento (
    id_assento INT AUTO_INCREMENT PRIMARY KEY,
    id_sala    INT     NOT NULL,
    fileira    CHAR(1) NOT NULL,
    numero     INT     NOT NULL,
    CONSTRAINT fk_assento_sala
        FOREIGN KEY (id_sala) REFERENCES sala (id_sala)
        ON DELETE CASCADE,
    CONSTRAINT uq_assento_posicao UNIQUE (id_sala, fileira, numero)
) ENGINE=InnoDB;

CREATE TABLE sessao (
    id_sessao   INT AUTO_INCREMENT PRIMARY KEY,
    id_filme    INT           NOT NULL,
    id_sala     INT           NOT NULL,
    data_hora   DATETIME      NOT NULL,
    preco       DECIMAL(8,2)  NOT NULL,
    CONSTRAINT fk_sessao_filme
        FOREIGN KEY (id_filme) REFERENCES filme (id_filme),
    CONSTRAINT fk_sessao_sala
        FOREIGN KEY (id_sala) REFERENCES sala (id_sala),
    CONSTRAINT ck_sessao_preco CHECK (preco >= 0),
    CONSTRAINT uq_sessao_sala_horario UNIQUE (id_sala, data_hora)
) ENGINE=InnoDB;


CREATE TABLE ingresso (
    id_ingresso INT AUTO_INCREMENT PRIMARY KEY,
    id_sessao   INT          NOT NULL,
    id_assento  INT          NOT NULL,
    valor_pago  DECIMAL(8,2) NOT NULL,
    data_venda  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ingresso_sessao
        FOREIGN KEY (id_sessao) REFERENCES sessao (id_sessao)
        ON DELETE CASCADE,
    CONSTRAINT fk_ingresso_assento
        FOREIGN KEY (id_assento) REFERENCES assento (id_assento),
    CONSTRAINT ck_ingresso_valor CHECK (valor_pago >= 0),
    CONSTRAINT uq_ingresso_sessao_assento UNIQUE (id_sessao, id_assento)
) ENGINE=InnoDB;


INSERT INTO genero (nome) VALUES
    ('Ação'), ('Aventura'), ('Animação'), ('Comédia'), ('Drama'), ('Ficção Científica'),
    ('Terror'), ('Suspense'), ('Romance'), ('Fantasia'), ('Documentário'), ('Musical');

INSERT INTO filme (titulo, duracao_min, classificacao) VALUES
    ('Interestelar',    169, '10'),
    ('Divertida Mente',  95, 'L'),
    ('Parasita',        132, '16');

INSERT INTO filme_genero (id_filme, id_genero)
SELECT f.id_filme, g.id_genero
  FROM (SELECT 'Interestelar' AS titulo, 'Ficção Científica' AS genero UNION ALL
        SELECT 'Interestelar', 'Aventura' UNION ALL
        SELECT 'Interestelar', 'Drama' UNION ALL
        SELECT 'Divertida Mente', 'Animação' UNION ALL
        SELECT 'Divertida Mente', 'Comédia' UNION ALL
        SELECT 'Parasita', 'Suspense' UNION ALL
        SELECT 'Parasita', 'Drama' UNION ALL
        SELECT 'Parasita', 'Comédia') x
  JOIN filme  f ON f.titulo = x.titulo
  JOIN genero g ON g.nome   = x.genero;

INSERT INTO sala (nome, capacidade) VALUES
    ('Sala 1', 6),
    ('Sala 2', 4);

INSERT INTO assento (id_sala, fileira, numero) VALUES
    (1,'A',1),(1,'A',2),(1,'A',3),
    (1,'B',1),(1,'B',2),(1,'B',3),
    (2,'A',1),(2,'A',2),
    (2,'B',1),(2,'B',2);

INSERT INTO sessao (id_filme, id_sala, data_hora, preco) VALUES
    (1, 1, '2026-10-10 19:00:00', 32.00),
    (2, 2, '2026-10-10 16:00:00', 25.00),
    (3, 1, '2026-10-10 21:30:00', 32.00);

INSERT INTO ingresso (id_sessao, id_assento, valor_pago) VALUES
    (1, 1, 32.00),
    (1, 2, 32.00),
    (2, 7, 25.00);
