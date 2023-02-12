DROP TABLE IF EXISTS uzytkownik CASCADE;
DROP TABLE IF EXISTS pytanie CASCADE;
DROP TABLE IF EXISTS odpowiedz;
CREATE TABLE uzytkownik (
    id_uzytkownika serial PRIMARY KEY,
    nazwa_uzytkownika VARCHAR (25) UNIQUE NOT NULL,
    haslo CHAR(64) NOT NULL,
    numer_indeksu VARCHAR(10) NOT NULL,
	czy_admin BOOL NOT NULL
);
CREATE TABLE pytanie
(
	id_pytania serial PRIMARY KEY,
	tresc TEXT NOT NULL,
	obraz TEXT 
);
CREATE TABLE odpowiedz (
    id_odpowiedzi serial PRIMARY KEY,
    odpowiedz_id_pytania INTEGER NOT NULL,
    tresc TEXT NOT NULL,
    czy_poprawna BOOL NOT NULL,
    CONSTRAINT fk_odpowiedz FOREIGN KEY(odpowiedz_id_pytania) REFERENCES pytanie(id_pytania)
);
