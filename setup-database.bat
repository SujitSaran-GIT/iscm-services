@echo off
REM Database Setup Script for ISCM IAM Service (Windows)

echo === ISCM Database Setup ===

REM Check if PostgreSQL is available
psql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ PostgreSQL is not installed or not in PATH
    echo Please install PostgreSQL and ensure psql is available
    pause
    exit /b 1
)

echo ✅ PostgreSQL is available

REM Database configuration
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=iscm_iam
set DB_USER=postgres
set DB_PASSWORD=Saran@2002

echo Database Configuration:
echo   Host: %DB_HOST%
echo   Port: %DB_PORT%
echo   Name: %DB_NAME%
echo   User: %DB_USER%

REM Test PostgreSQL connection
echo Testing PostgreSQL connection...
set PGPASSWORD=%DB_PASSWORD%
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "SELECT 1;" >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Cannot connect to PostgreSQL
    echo Please check:
    echo   - PostgreSQL is running
    echo   - Connection parameters are correct
    echo   - User has necessary permissions
    pause
    exit /b 1
)

echo ✅ PostgreSQL connection successful

REM Create database if it doesn't exist
echo Creating database %DB_NAME%...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "CREATE DATABASE %DB_NAME%;" >nul 2>&1
if %errorlevel% neq 0 (
    echo Database %DB_NAME% already exists
) else (
    echo Database %DB_NAME% created successfully
)

REM Verify database exists
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "SELECT 1;" >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Cannot connect to database %DB_NAME%
    pause
    exit /b 1
)

echo ✅ Database %DB_NAME% is ready

REM Create UUID extension if needed
echo Creating UUID extension...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";" >nul 2>&1
echo UUID extension ready

echo.
echo ✅ Database setup completed successfully!
echo.
echo Next steps:
echo 1. Start the IAM service (Liquibase will create tables automatically)
echo 2. After successful startup, you can disable Liquibase by setting:
echo    spring.liquibase.enabled=false
echo.
echo Run the IAM service with:
echo cd iam-service
echo mvn spring-boot:run

pause