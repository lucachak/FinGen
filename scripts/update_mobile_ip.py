import socket
import re
import os

def get_local_ip():
    try:
        # Tenta conectar a um IP externo (não envia dados) para descobrir a interface ativa
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "127.0.0.1"

def update_flutter_config(ip):
    file_path = "mobile/lib/shared/api/api_provider.dart"
    if not os.path.exists(file_path):
        print(f"Erro: Arquivo {file_path} não encontrado.")
        return

    with open(file_path, "r") as f:
        content = f.read()

    # Regex para encontrar a linha da baseUrl (com prefixo /api/v1/)
    pattern = r'const baseUrl = "http://.*:8080/api/v1/?";'
    new_line = f'const baseUrl = "http://{ip}:8080/api/v1/";'
    
    if re.search(pattern, content):
        new_content = re.sub(pattern, new_line, content)
        with open(file_path, "w") as f:
            f.write(new_content)
        print(f"✅ IP atualizado com sucesso para: {ip}")
    else:
        print("⚠️ Não foi possível encontrar a linha da baseUrl no formato esperado.")

if __name__ == "__main__":
    current_ip = get_local_ip()
    update_flutter_config(current_ip)
