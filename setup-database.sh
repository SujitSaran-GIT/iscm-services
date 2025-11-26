#!/bin/bash

# Database Setup Script for ISCM IAM Service
# This script will create the database and enable migrations

echo "=== ISCM Database Setup ==="

# Check if PostgreSQL is running
if ! command -v psql &> /dev/null; then
    echo "❌ PostgreSQL is not installed or not in PATH"
    echo "Please install PostgreSQL and ensure psql is available"
    exit 1
fi

echo "✅ PostgreSQL is available"

# Database configuration
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-iscm_iam}
DB_USER=${DB_USER:-postgres}
DB_PASSWORD=${DB_PASSWORD:-Saran@2002}

echo "Database Configuration:"
echo "  Host: $DB_HOST"
echo "  Port: $DB_PORT"
echo "  Name: $DB_NAME"
echo "  User: $DB_USER"

# Test PostgreSQL connection
echo "Testing PostgreSQL connection..."
if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d postgres -c "SELECT 1;" > /dev/null 2>&1; then
    echo "✅ PostgreSQL connection successful"
else
    echo "❌ Cannot connect to PostgreSQL"
    echo "Please check:"
    echo "  - PostgreSQL is running"
    echo "  - Connection parameters are correct"
    echo "  - User has necessary permissions"
    exit 1
fi

# Create database if it doesn't exist
echo "Creating database $DB_NAME..."
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d postgres -c "CREATE DATABASE $DB_NAME;" 2>/dev/null || echo "Database $DB_NAME already exists"

# Verify database exists
if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1;" > /dev/null 2>&1; then
    echo "✅ Database $DB_NAME is ready"
else
    echo "❌ Cannot connect to database $DB_NAME"
    exit 1
fi

# Create UUID extension if needed
echo "Creating UUID extension..."
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";" || echo "UUID extension already exists"

echo ""
echo "✅ Database setup completed successfully!"
echo ""
echo "Next steps:"
echo "1. Start the IAM service (Liquibase will create tables automatically)"
echo "2. After successful startup, you can disable Liquibase by setting:"
echo "   spring.liquibase.enabled=false"
echo ""
echo "Run the IAM service with:"
echo "cd iam-service"
echo "mvn spring-boot:run"