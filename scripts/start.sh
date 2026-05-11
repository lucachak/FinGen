#!/bin/bash

# Inicia o Python em background
echo "🚀 Iniciando API de IA (Python)..."
uvicorn main:app --host 127.0.0.1 --port 8000 &

# Aguarda um pouco para o Python subir
sleep 5

# Inicia o Java em foreground
echo "☕ Iniciando App Principal (Java)..."
# Passamos a porta do Render para o Spring Boot se necessário, 
# mas ele já está configurado para 8080 ou via server.port
java -jar app.jar --server.port=${PORT:-8080}
