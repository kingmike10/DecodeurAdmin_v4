-- =========================================================
-- Projet de session - SMI1002 Hiver 2026
-- Base de donnees : Gestion de decodeurs TV
-- =========================================================


-- =========================================================
-- NETTOYAGE (si on re-execute le script)
-- =========================================================

BEGIN EXECUTE IMMEDIATE 'DROP TABLE JOURNAL_TRANSACTION CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE DECODEUR_CHAINES CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE DECODEUR CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE UTILISATEUR CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE CLIENT CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_CLIENT';      EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_DECODEUR';    EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_UTILISATEUR'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_JOURNAL';     EXCEPTION WHEN OTHERS THEN NULL; END;
/


-- =========================================================
-- SEQUENCES
-- Oracle n'a pas d'AUTO_INCREMENT comme MySQL,
-- on utilise des sequences pour generer les IDs
-- =========================================================

CREATE SEQUENCE SEQ_CLIENT     START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_DECODEUR   START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_UTILISATEUR START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_JOURNAL    START WITH 1 INCREMENT BY 1;


-- =========================================================
-- TABLES
-- =========================================================

-- Table CLIENT
-- Un client est un hotel ou une entreprise qui loue des decodeurs
CREATE TABLE CLIENT (
    ID_CLIENT      NUMBER(10)    NOT NULL,
    NOM_CLIENT     VARCHAR2(100) NOT NULL,
    ADRESSE        VARCHAR2(200) NOT NULL,
    DATE_CREATION  DATE DEFAULT SYSDATE,
    CONSTRAINT PK_CLIENT PRIMARY KEY (ID_CLIENT)
);

-- Table UTILISATEUR
-- Chaque client a un compte pour se connecter
-- L'admin a aussi un compte (sans ID_CLIENT associe)
CREATE TABLE UTILISATEUR (
    ID_UTILISATEUR        NUMBER(10)    NOT NULL,
    IDENTIFIANT           VARCHAR2(100) NOT NULL,
    MOT_DE_PASSE          VARCHAR2(100) NOT NULL,
    ROLE                  VARCHAR2(10)  NOT NULL,
    ID_CLIENT             NUMBER(10),
    CONSTRAINT PK_UTILISATEUR  PRIMARY KEY (ID_UTILISATEUR),
    CONSTRAINT UK_IDENTIFIANT  UNIQUE (IDENTIFIANT),
    CONSTRAINT FK_UTIL_CLIENT  FOREIGN KEY (ID_CLIENT) REFERENCES CLIENT(ID_CLIENT) ON DELETE CASCADE,
    CONSTRAINT CHK_ROLE        CHECK (ROLE IN ('ADMIN', 'CLIENT'))
);

-- Table DECODEUR
-- Un decodeur appartient a un client (ou est libre si ID_CLIENT est NULL)
CREATE TABLE DECODEUR (
    ID_DECODEUR  NUMBER(10)   NOT NULL,
    ADRESSE_IP   VARCHAR2(50) NOT NULL,
    ETAT         VARCHAR2(15) DEFAULT 'HORS_LIGNE',
    ID_CLIENT    NUMBER(10),
    CONSTRAINT PK_DECODEUR    PRIMARY KEY (ID_DECODEUR),
    CONSTRAINT UK_IP          UNIQUE (ADRESSE_IP),
    CONSTRAINT FK_DEC_CLIENT  FOREIGN KEY (ID_CLIENT) REFERENCES CLIENT(ID_CLIENT) ON DELETE SET NULL,
    CONSTRAINT CHK_ETAT       CHECK (ETAT IN ('EN_LIGNE', 'HORS_LIGNE'))
);

-- Table DECODEUR_CHAINES
-- Les chaines TV disponibles sur chaque decodeur
CREATE TABLE DECODEUR_CHAINES (
    ID_DECODEUR  NUMBER(10)    NOT NULL,
    CHAINE       VARCHAR2(100) NOT NULL,
    CONSTRAINT PK_CHAINE      PRIMARY KEY (ID_DECODEUR, CHAINE),
    CONSTRAINT FK_CHAINE_DEC  FOREIGN KEY (ID_DECODEUR) REFERENCES DECODEUR(ID_DECODEUR) ON DELETE CASCADE
);

-- Table JOURNAL_TRANSACTION
-- trace de toutes les transactions (INSERT, UPDATE, DELETE)
-- Permet de savoir qui a fait quoi et quand
CREATE TABLE JOURNAL_TRANSACTION (
    ID_JOURNAL      NUMBER(10)    NOT NULL,
    DATE_OPERATION  TIMESTAMP     DEFAULT SYSTIMESTAMP,
    TYPE_OPERATION  VARCHAR2(10)  NOT NULL,
    TABLE_MODIFIEE  VARCHAR2(50)  NOT NULL,
    DESCRIPTION     VARCHAR2(300),
    CONSTRAINT PKJ_TRANSACTION PRIMARY KEY (ID_JOURNAL)
);


-- =========================================================
-- TRIGGERS pour les cles primaires (auto-increment)
-- =========================================================

CREATE OR REPLACE TRIGGER TRG_CLIENT_PK
BEFORE INSERT ON CLIENT FOR EACH ROW
BEGIN
    SELECT SEQ_CLIENT.NEXTVAL INTO :NEW.ID_CLIENT FROM DUAL;
END;
/

CREATE OR REPLACE TRIGGER TRG_UTILISATEUR_PK
BEFORE INSERT ON UTILISATEUR FOR EACH ROW
BEGIN
    SELECT SEQ_UTILISATEUR.NEXTVAL INTO :NEW.ID_UTILISATEUR FROM DUAL;
END;
/

CREATE OR REPLACE TRIGGER TRG_DECODEUR_PK
BEFORE INSERT ON DECODEUR FOR EACH ROW
BEGIN
    SELECT SEQ_DECODEUR.NEXTVAL INTO :NEW.ID_DECODEUR FROM DUAL;
END;
/

CREATE OR REPLACE TRIGGER TRG_JOURNAL_PK
BEFORE INSERT ON JOURNAL_TRANSACTION FOR EACH ROW
BEGIN
    SELECT SEQ_JOURNAL.NEXTVAL INTO :NEW.ID_JOURNAL FROM DUAL;
END;
/


-- =========================================================
-- TRIGGERS pour le journal
-- Ces triggers enregistrent automatiquement toutes les
-- modifications dans la table JOURNAL_TRANSACTION
-- C'est comme un historique de toutes les transactions
-- =========================================================

CREATE OR REPLACE TRIGGER TRG_JOURNAL_CLIENT
AFTER INSERT OR UPDATE OR DELETE ON CLIENT
FOR EACH ROW
DECLARE
    v_type  VARCHAR2(10);
    v_desc  VARCHAR2(300);
BEGIN
    IF INSERTING THEN
        v_type := 'INSERT';
        v_desc := 'Nouveau client ajoute : ' || :NEW.NOM_CLIENT;
    ELSIF UPDATING THEN
        v_type := 'UPDATE';
        v_desc := 'Client modifie : ' || :NEW.NOM_CLIENT;
    ELSIF DELETING THEN
        v_type := 'DELETE';
        v_desc := 'Client supprime : ' || :OLD.NOM_CLIENT;
    END IF;

    INSERT INTO JOURNAL_TRANSACTION (TYPE_OPERATION, TABLE_MODIFIEE, DESCRIPTION)
    VALUES (v_type, 'CLIENT', v_desc);
END;
/

CREATE OR REPLACE TRIGGER TRG_JOURNAL_DECODEUR
AFTER INSERT OR UPDATE OR DELETE ON DECODEUR
FOR EACH ROW
DECLARE
    v_type  VARCHAR2(10);
    v_desc  VARCHAR2(300);
BEGIN
    IF INSERTING THEN
        v_type := 'INSERT';
        v_desc := 'Decodeur ajoute, IP = ' || :NEW.ADRESSE_IP;
    ELSIF UPDATING THEN
        v_type := 'UPDATE';
        v_desc := 'Decodeur modifie, IP = ' || :NEW.ADRESSE_IP || ', etat = ' || :NEW.ETAT;
        IF :OLD.ID_CLIENT IS NULL AND :NEW.ID_CLIENT IS NOT NULL THEN
            v_desc := 'Decodeur ' || :NEW.ADRESSE_IP || ' assigne au client #' || :NEW.ID_CLIENT;
        END IF;
        IF :OLD.ID_CLIENT IS NOT NULL AND :NEW.ID_CLIENT IS NULL THEN
            v_desc := 'Decodeur ' || :NEW.ADRESSE_IP || ' retire du client #' || :OLD.ID_CLIENT;
        END IF;
    ELSIF DELETING THEN
        v_type := 'DELETE';
        v_desc := 'Decodeur supprime, IP = ' || :OLD.ADRESSE_IP;
    END IF;

    INSERT INTO JOURNAL_TRANSACTION (TYPE_OPERATION, TABLE_MODIFIEE, DESCRIPTION)
    VALUES (v_type, 'DECODEUR', v_desc);
END;
/


-- =========================================================
-- INDEX 
-- =========================================================
CREATE INDEX IDX_DECODEUR_CLIENT ON DECODEUR(ID_CLIENT);

-- Index sur la date dans le journal (pour trier rapidement)
CREATE INDEX IDX_JOURNAL_DATE ON JOURNAL_TRANSACTION(DATE_OPERATION);


-- =========================================================
-- VUES
-- =========================================================

-- Vue qui montre les clients avec leurs decodeurs
CREATE OR REPLACE VIEW VUE_CLIENTS_DECODEURS AS
SELECT
    C.ID_CLIENT,
    C.NOM_CLIENT,
    C.ADRESSE,
    D.ID_DECODEUR,
    D.ADRESSE_IP,
    D.ETAT
FROM CLIENT C
LEFT JOIN DECODEUR D ON D.ID_CLIENT = C.ID_CLIENT
ORDER BY C.ID_CLIENT, D.ID_DECODEUR;

-- Vue qui montre seulement les decodeurs disponibles (non assignes)
CREATE OR REPLACE VIEW VUE_DECODEURS_LIBRES AS
SELECT ID_DECODEUR, ADRESSE_IP, ETAT
FROM DECODEUR
WHERE ID_CLIENT IS NULL;

-- Vue du journal pour avoir un affichage lisible
CREATE OR REPLACE VIEW VUE_JOURNAL AS
SELECT
    ID_JOURNAL,
    TO_CHAR(DATE_OPERATION, 'YYYY-MM-DD HH24:MI:SS') AS DATE_HEURE,
    TYPE_OPERATION,
    TABLE_MODIFIEE,
    DESCRIPTION
FROM JOURNAL_TRANSACTION
ORDER BY ID_JOURNAL DESC;


-- =========================================================
-- PROCEDURES STOCKEES
-- =========================================================

-- Procedure pour creer un client avec son compte utilisateur
-- On utilise COMMIT et ROLLBACK
-- Si une des deux insertions echoue, on annule tout
CREATE OR REPLACE PROCEDURE PROC_CREER_CLIENT(
    p_nom        IN VARCHAR2,
    p_adresse    IN VARCHAR2,
    p_identifiant IN VARCHAR2,
    p_mdp        IN VARCHAR2,
    p_message    OUT VARCHAR2
)
AS
    v_count     NUMBER;
    v_id_client NUMBER;
BEGIN
    -- Verifier si l'identifiant existe deja
    SELECT COUNT(*) INTO v_count
    FROM UTILISATEUR
    WHERE IDENTIFIANT = p_identifiant;

    IF v_count > 0 THEN
        p_message := 'ERREUR : cet identifiant est deja utilise';
        RETURN;
    END IF;

    -- Inserer le client
    INSERT INTO CLIENT (NOM_CLIENT, ADRESSE)
    VALUES (p_nom, p_adresse)
    RETURNING ID_CLIENT INTO v_id_client;

    -- Inserer l utilisateur lie au client
    INSERT INTO UTILISATEUR (IDENTIFIANT, MOT_DE_PASSE, ROLE, ID_CLIENT)
    VALUES (p_identifiant, p_mdp, 'CLIENT', v_id_client);

    COMMIT;
    p_message := 'OK : client cree avec succes (ID = ' || v_id_client || ')';

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        p_message := 'ERREUR : ' || SQLERRM;
END;
/

-- Procedure pour supprimer un client
-- On desassigne d'abord ses decodeurs avant de supprimer
CREATE OR REPLACE PROCEDURE PROC_SUPPRIMER_CLIENT(
    p_id_client IN NUMBER,
    p_message   OUT VARCHAR2
)
AS
    v_count  NUMBER;
    v_nb_dec NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM CLIENT WHERE ID_CLIENT = p_id_client;
    IF v_count = 0 THEN
        p_message := 'ERREUR : client introuvable';
        RETURN;
    END IF;

    -- Compter les decodeurs a liberer
    SELECT COUNT(*) INTO v_nb_dec FROM DECODEUR WHERE ID_CLIENT = p_id_client;

    -- Desassigner les decodeurs
    UPDATE DECODEUR SET ID_CLIENT = NULL WHERE ID_CLIENT = p_id_client;

    -- Supprimer le client (la cascade supprime aussi l utilisateur)
    DELETE FROM CLIENT WHERE ID_CLIENT = p_id_client;

    COMMIT;
    p_message := 'OK : client supprime, ' || v_nb_dec || ' decodeur(s) libere(s)';

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        p_message := 'ERREUR : ' || SQLERRM;
END;
/

-- Procedure pour assigner un decodeur a un client
--
-- Gestion de la concurrence :
-- Probleme : si deux administrateurs (sur deux postes differents)
-- voient le meme decodeur libre en meme temps et essaient tous les
-- deux de l'assigner, sans protection on aurait une perte de mise
-- a jour (lost update). Le decodeur se retrouverait assigne deux fois.
--
-- Solution : on utilise SELECT FOR UPDATE pour verrouiller la ligne
-- du decodeur des le debut de la transaction. L'autre poste devra
-- attendre que ce verrou soit libere (apres le COMMIT ou ROLLBACK).
-- C'est le principe du verrouillage exclusif
CREATE OR REPLACE PROCEDURE PROC_ASSIGNER_DECODEUR(
    p_id_decodeur IN NUMBER,
    p_id_client   IN NUMBER,
    p_message     OUT VARCHAR2
)
AS
    v_id_client_actuel NUMBER;
    v_count            NUMBER;
BEGIN
    -- SELECT FOR UPDATE : on pose un verrou exclusif sur cette ligne
    -- Si un autre poste essaie d'assigner le meme decodeur en meme temps,
    -- il sera bloque ici jusqu'a ce qu'on fasse COMMIT ou ROLLBACK.
    -- Cela empeche le probleme de perte de mise a jour.
    BEGIN
        SELECT ID_CLIENT INTO v_id_client_actuel
        FROM DECODEUR
        WHERE ID_DECODEUR = p_id_decodeur
        FOR UPDATE;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            p_message := 'ERREUR : decodeur introuvable';
            RETURN;
    END;

    -- Maintenant qu'on a le verrou, on verifie si le decodeur est libre
    -- Si l'autre poste avait deja assigne ce decodeur juste avant,
    -- on le verra ici et on retournera une erreur
    IF v_id_client_actuel IS NOT NULL THEN
        p_message := 'ERREUR : ce decodeur est deja assigne';
        ROLLBACK;
        RETURN;
    END IF;

    SELECT COUNT(*) INTO v_count FROM CLIENT WHERE ID_CLIENT = p_id_client;
    IF v_count = 0 THEN
        p_message := 'ERREUR : client introuvable';
        ROLLBACK;
        RETURN;
    END IF;

    UPDATE DECODEUR SET ID_CLIENT = p_id_client WHERE ID_DECODEUR = p_id_decodeur;

    -- COMMIT libere le verrou -> l'autre poste peut maintenant continuer
    COMMIT;
    p_message := 'OK : decodeur assigne au client';

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        p_message := 'ERREUR : ' || SQLERRM;
END;
/

-- Procedure pour retirer un decodeur d'un client
CREATE OR REPLACE PROCEDURE PROC_RETIRER_DECODEUR(
    p_id_decodeur IN NUMBER,
    p_message     OUT VARCHAR2
)
AS
    v_id_client NUMBER;
BEGIN
    BEGIN
        SELECT ID_CLIENT INTO v_id_client
        FROM DECODEUR WHERE ID_DECODEUR = p_id_decodeur;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            p_message := 'ERREUR : decodeur introuvable';
            RETURN;
    END;

    IF v_id_client IS NULL THEN
        p_message := 'ERREUR : ce decodeur n''est pas assigne';
        RETURN;
    END IF;

    UPDATE DECODEUR SET ID_CLIENT = NULL WHERE ID_DECODEUR = p_id_decodeur;
    COMMIT;
    p_message := 'OK : decodeur retire du client';

EXCEPTION
    WHEN OTHERS THEN
        ROLLBACK;
        p_message := 'ERREUR : ' || SQLERRM;
END;
/


-- =========================================================
-- GESTION DE LA CONCURRENCE
--
-- La base de donnees est sur un serveur partage.
-- Plusieurs administrateurs peuvent y acceder en meme temps
-- depuis des postes differents.
--
-- Probleme de perte de mise a jour (lost update) :

-- Solution utilisee : SELECT FOR UPDATE dans PROC_ASSIGNER_DECODEUR

--
-- Niveau d'isolation utilise : READ COMMITTED (defaut Oracle)
--   - Empeche les lectures impropres (dirty reads)
--   - Un admin ne peut pas lire les donnees non confirmees d'un autre
--   - tableau des niveaux d'isolation SQL
--
-- Note : Oracle utilise le multiversion + verrouillage
-- pour les lectures : pas de blocage en lecture, verrou seulement
-- en ecriture (clause FOR UPDATE).
-- =========================================================


-- =========================================================
-- RECUPERATION EN CAS DE PANNE
--
-- Il existe deux niveaux de recuperation :
--
-- NIVEAU 1 - Oracle gere automatiquement (section 14.7.3) :

-- NIVEAU 2 - Notre application protege avec COMMIT/ROLLBACK :

-- NIVEAU 3 - Notre JOURNAL_TRANSACTION :

-- =========================================================


-- =========================================================
-- DONNEES DE TEST
-- =========================================================

-- Compte administrateur
INSERT INTO UTILISATEUR (IDENTIFIANT, MOT_DE_PASSE, ROLE)
VALUES ('admin', 'admin1234', 'ADMIN');

-- Clients
INSERT INTO CLIENT (NOM_CLIENT, ADRESSE)
VALUES ('Hotel Le Gouverneur', '7 Rue Laviolette, Trois-Rivieres');

INSERT INTO CLIENT (NOM_CLIENT, ADRESSE)
VALUES ('Hotel Delta Trois-Rivieres', '1620 Rue Notre-Dame, Trois-Rivieres');

INSERT INTO CLIENT (NOM_CLIENT, ADRESSE)
VALUES ('Motel Universel', '2000 Boul. des Recollets, Trois-Rivieres');

-- Utilisateurs pour les clients
INSERT INTO UTILISATEUR (IDENTIFIANT, MOT_DE_PASSE, ROLE, ID_CLIENT)
VALUES ('hotel_gouv', 'pass123', 'CLIENT', 1);

INSERT INTO UTILISATEUR (IDENTIFIANT, MOT_DE_PASSE, ROLE, ID_CLIENT)
VALUES ('hotel_delta', 'pass456', 'CLIENT', 2);

INSERT INTO UTILISATEUR (IDENTIFIANT, MOT_DE_PASSE, ROLE, ID_CLIENT)
VALUES ('motel_univ', 'pass789', 'CLIENT', 3);

-- Decodeurs (12 decodeurs au total)
INSERT INTO DECODEUR (ADRESSE_IP, ETAT, ID_CLIENT) VALUES ('127.0.10.1',  'EN_LIGNE',   1);
INSERT INTO DECODEUR (ADRESSE_IP, ETAT, ID_CLIENT) VALUES ('127.0.10.2',  'EN_LIGNE',   1);
INSERT INTO DECODEUR (ADRESSE_IP, ETAT, ID_CLIENT) VALUES ('127.0.10.3',  'HORS_LIGNE', 1);
INSERT INTO DECODEUR (ADRESSE_IP, ETAT, ID_CLIENT) VALUES ('127.0.10.4',  'EN_LIGNE',   2);
INSERT INTO DECODEUR (ADRESSE_IP, ETAT, ID_CLIENT) VALUES ('127.0.10.5',  'HORS_LIGNE', 2);
INSERT INTO DECODEUR (ADRESSE_IP, ETAT, ID_CLIENT) VALUES ('127.0.10.6',  'EN_LIGNE',   3);
INSERT INTO DECODEUR (ADRESSE_IP, ETAT, ID_CLIENT) VALUES ('127.0.10.7',  'HORS_LIGNE', NULL);
INSERT INTO DECODEUR (ADRESSE_IP, ETAT, ID_CLIENT) VALUES ('127.0.10.8',  'HORS_LIGNE', NULL);
INSERT INTO DECODEUR (ADRESSE_IP, ETAT, ID_CLIENT) VALUES ('127.0.10.9',  'HORS_LIGNE', NULL);
INSERT INTO DECODEUR (ADRESSE_IP, ETAT, ID_CLIENT) VALUES ('127.0.10.10', 'HORS_LIGNE', NULL);
INSERT INTO DECODEUR (ADRESSE_IP, ETAT, ID_CLIENT) VALUES ('127.0.10.11', 'HORS_LIGNE', NULL);
INSERT INTO DECODEUR (ADRESSE_IP, ETAT, ID_CLIENT) VALUES ('127.0.10.12', 'HORS_LIGNE', NULL);

-- Chaines TV
INSERT INTO DECODEUR_CHAINES VALUES (1, 'TVA');
INSERT INTO DECODEUR_CHAINES VALUES (1, 'RDS');
INSERT INTO DECODEUR_CHAINES VALUES (1, 'CNN');
INSERT INTO DECODEUR_CHAINES VALUES (2, 'TVA');
INSERT INTO DECODEUR_CHAINES VALUES (2, 'V');
INSERT INTO DECODEUR_CHAINES VALUES (3, 'TVA');
INSERT INTO DECODEUR_CHAINES VALUES (4, 'RDS');
INSERT INTO DECODEUR_CHAINES VALUES (4, 'TSN');
INSERT INTO DECODEUR_CHAINES VALUES (5, 'DISCOVERY');
INSERT INTO DECODEUR_CHAINES VALUES (6, 'TVA');
INSERT INTO DECODEUR_CHAINES VALUES (6, 'CNN');

COMMIT;

-- Verification rapide
SELECT 'Clients   : ' || COUNT(*) AS INFO FROM CLIENT;
SELECT 'Decodeurs : ' || COUNT(*) AS INFO FROM DECODEUR;
SELECT 'Libres    : ' || COUNT(*) AS INFO FROM VUE_DECODEURS_LIBRES;
SELECT 'Journal   : ' || COUNT(*) AS INFO FROM JOURNAL_TRANSACTION;
