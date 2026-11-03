USE master;
GO

IF DB_ID('AgendaBanda') IS NULL
BEGIN
    CREATE DATABASE AgendaBanda;
END
GO

IF EXISTS (SELECT 1 FROM sys.sql_logins WHERE name = 'agenda_user')
BEGIN
    ALTER LOGIN agenda_user
        WITH PASSWORD = 'agenda123',
        CHECK_POLICY = OFF,
        CHECK_EXPIRATION = OFF;
END
ELSE
BEGIN
    CREATE LOGIN agenda_user
        WITH PASSWORD = 'agenda123',
        CHECK_POLICY = OFF,
        CHECK_EXPIRATION = OFF;
END
GO

ALTER LOGIN agenda_user WITH DEFAULT_DATABASE = AgendaBanda;
GO

-- Preparacao da base dedicada de avaliacao: este login e dono somente de AgendaBanda.
-- A propriedade permite RESTORE da propria base, sem dbcreator ou sysadmin para a aplicacao.
ALTER AUTHORIZATION ON DATABASE::AgendaBanda TO agenda_user;
GO

USE master;
GO

-- Usa uma pasta ja preparada pelo servico SQL Server, sem criar diretorios no Java.
CREATE OR ALTER FUNCTION dbo.fn_agendabanda_diretorio_backup()
RETURNS NVARCHAR(4000)
AS
BEGIN
    DECLARE @diretorio NVARCHAR(4000)=CONVERT(NVARCHAR(4000),SERVERPROPERTY('InstanceDefaultBackupPath'));
    IF NULLIF(@diretorio,N'') IS NULL
        SET @diretorio=CONVERT(NVARCHAR(4000),SERVERPROPERTY('InstanceDefaultDataPath'));
    RETURN @diretorio;
END;
GO

-- Modulo restrito de inspecao: sem caminho livre, SQL dinamico ou comandos de escrita.
-- A permissao adicional de CREATE DATABASE existe apenas durante este modulo assinado.
CREATE OR ALTER PROCEDURE dbo.usp_agendabanda_inspecionar_backup
    @codigo UNIQUEIDENTIFIER, @operacao VARCHAR(10)
AS
BEGIN
    SET NOCOUNT ON;
    IF @codigo IS NULL OR @operacao IS NULL OR @operacao NOT IN ('VERIFY','HEADER','FILES')
        THROW 50001,N'Inspecao de backup invalida.',1;
    DECLARE @diretorio NVARCHAR(4000)=dbo.fn_agendabanda_diretorio_backup();
    IF NULLIF(@diretorio,N'') IS NULL
        THROW 50001,N'O SQL Server nao informou uma pasta padrao para backups.',1;
    DECLARE @arquivo NVARCHAR(4000)=@diretorio
        +CASE WHEN RIGHT(@diretorio,1) IN (N'/',N'\') THEN N'' ELSE N'/' END
        +N'agendabanda_'+LOWER(CONVERT(NVARCHAR(36),@codigo))+N'.bak';
    IF @operacao='VERIFY' RESTORE VERIFYONLY FROM DISK=@arquivo WITH CHECKSUM;
    ELSE IF @operacao='HEADER' RESTORE HEADERONLY FROM DISK=@arquivo;
    ELSE RESTORE FILELISTONLY FROM DISK=@arquivo;
END;
GO

-- Certificado exclusivo deste modulo. O login de certificado nao aceita conexoes.
IF EXISTS (SELECT 1 FROM sys.server_principals WHERE name=N'AgendaBanda_InspecaoBackup' AND type<>'C')
    THROW 50001,N'Existe um login diferente com o nome reservado para a inspecao de backups.',1;
IF CERT_ID(N'AgendaBanda_InspecaoBackup') IS NOT NULL
    AND EXISTS (
        SELECT 1 FROM sys.crypt_properties p JOIN sys.certificates c ON c.thumbprint=p.thumbprint
        WHERE c.name=N'AgendaBanda_InspecaoBackup'
          AND p.major_id<>OBJECT_ID(N'dbo.usp_agendabanda_inspecionar_backup'))
    THROW 50001,N'O certificado reservado esta sendo usado por outro modulo.',1;

IF EXISTS (SELECT 1 FROM sys.server_principals WHERE name=N'AgendaBanda_InspecaoBackup')
    DROP LOGIN AgendaBanda_InspecaoBackup;
IF CERT_ID(N'AgendaBanda_InspecaoBackup') IS NOT NULL
    DROP CERTIFICATE AgendaBanda_InspecaoBackup;

DECLARE @senha NVARCHAR(128)=CONVERT(NVARCHAR(36),NEWID())+N'aA1!';
DECLARE @comando NVARCHAR(MAX)=N'CREATE CERTIFICATE AgendaBanda_InspecaoBackup ENCRYPTION BY PASSWORD = '
    +QUOTENAME(@senha,N'''')+N' WITH SUBJECT = N''Inspecao restrita dos backups AgendaBanda'';';
EXEC sys.sp_executesql @comando;
CREATE LOGIN AgendaBanda_InspecaoBackup FROM CERTIFICATE AgendaBanda_InspecaoBackup;
GRANT CREATE ANY DATABASE TO AgendaBanda_InspecaoBackup;
SET @comando=N'ADD SIGNATURE TO dbo.usp_agendabanda_inspecionar_backup BY CERTIFICATE AgendaBanda_InspecaoBackup WITH PASSWORD = '
    +QUOTENAME(@senha,N'''')+N';';
EXEC sys.sp_executesql @comando;
GO

IF NOT EXISTS (SELECT 1 FROM sys.database_principals WHERE name=N'agenda_user')
    CREATE USER agenda_user FOR LOGIN agenda_user;
GRANT EXECUTE ON dbo.usp_agendabanda_inspecionar_backup TO agenda_user;
GRANT SELECT ON dbo.fn_agendabanda_diretorio_backup TO agenda_user;
GO

USE AgendaBanda;
GO
