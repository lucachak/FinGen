import requests
from bs4 import BeautifulSoup
import time
import json

BASE_URL = "http://localhost:8080"
LOGIN_URL = f"{BASE_URL}/auth/login"
ONBOARDING_SUBMIT_URL = f"{BASE_URL}/app/onboarding/submit"
CONTA_SAVE_URL = f"{BASE_URL}/app/financeiro/contas/salvar"
CONTA_LIST_URL = f"{BASE_URL}/app/financeiro/contas"
DASHBOARD_URL = f"{BASE_URL}/app/dashboard"

def test_fingen_resilience():
    session = requests.Session()
    
    print("🚀 Iniciando Teste de Resiliência FinGen...")

    # 1. Acessar página de login para setar cookies iniciais
    print("--- 1. Preparando Sessão ---")
    session.get(LOGIN_URL)
    
    # 2. Realizar Login (O login ignora CSRF conforme SecurityConfig)
    print("--- 2. Realizando Login ---")
    login_data = {
        "email": "lucas@admin.com",
        "password": "lucas"
    }
    # Adicionamos timeout para evitar hangs infinitos
    try:
        response = session.post(LOGIN_URL, data=login_data, allow_redirects=False, timeout=10)
        
        # O AuthController redireciona para /app/dashboard em caso de sucesso
        if response.status_code == 302:
            redirect_url = response.headers.get('Location')
            print(f"✅ Login bem-sucedido. Redirecionando para: {redirect_url}")
            response = session.get(f"{BASE_URL}{redirect_url}" if redirect_url.startswith('/') else redirect_url, timeout=10)
        elif response.status_code == 200:
            print("⚠️ Login retornou 200 (não esperado redirecionamento 302).")
        else:
            print(f"❌ Falha no login. Status: {response.status_code}")
            return
    except requests.exceptions.Timeout:
        print("❌ Timeout durante o login. O servidor pode estar processando lentamente.")
        return
    except Exception as e:
        print(f"❌ Erro durante o login: {e}")
        return
        
    print("✅ Login realizado com sucesso!")

    # Capturar CSRF Token do Cookie (Spring usa XSRF-TOKEN por padrão com CookieCsrfTokenRepository)
    csrf_token = session.cookies.get('XSRF-TOKEN')
    if not csrf_token:
        # Tenta pegar de uma meta tag se injetamos no layout
        soup = BeautifulSoup(response.text, 'html.parser')
        meta_csrf = soup.find('meta', {'name': '_csrf'})
        if meta_csrf:
            csrf_token = meta_csrf['content']
            
    if csrf_token:
        print(f"✅ CSRF Token detectado: {csrf_token[:8]}...")
    else:
        print("⚠️ CSRF Token não encontrado nos cookies ou meta tags.")

    # 3. Testar Onboarding (Simular submissão)
    print("--- 3. Testando Onboarding ---")
    
    onboarding_data = {
        "rendaMensal": 2000.0,
        "moradia": "PAIS",
        "transporte": "BIKE",
        "contasFixas": [],
        "assinaturas": []
    }
    
    # Headers para simular o fetch do navegador
    # Spring Security com CookieCsrfTokenRepository espera X-XSRF-TOKEN
    headers = {
        "X-XSRF-TOKEN": session.cookies.get('XSRF-TOKEN', csrf_token),
        "Content-Type": "application/json"
    }
    
    response = session.post(ONBOARDING_SUBMIT_URL, json=onboarding_data, headers=headers)
    print(f"Status Onboarding: {response.status_code}")
    if response.status_code == 200:
        print("✅ Onboarding concluído sem erros 500!")
    else:
        print(f"⚠️ Alerta Onboarding: {response.text}")

    # 4. Criar Lançamento Manual
    print("--- 4. Criando Nova Conta (Lançamento Manual) ---")
    
    # Tentar descobrir uma categoria real acessando a página de nova conta
    response = session.get(f"{BASE_URL}/app/financeiro/contas/nova")
    soup = BeautifulSoup(response.text, 'html.parser')
    
    form_csrf = soup.find('input', {'name': '_csrf'})['value']
    
    # Buscar o primeiro ID de categoria disponível no select
    cat_select = soup.find('select', {'name': 'categoria.id'})
    cat_id = "1"
    if cat_select:
        first_option = cat_select.find('option', value=True)
        if first_option and first_option['value']:
            cat_id = first_option['value']
            print(f"📂 Usando categoria ID: {cat_id}")
    
    conta_data = {
        "descricao": "Teste de Resiliência Antigravity",
        "valor": "123.45",
        "tipo": "DESPESA",
        "dataVencimento": time.strftime("%Y-%m-%d"),
        "paga": "false",
        "categoria.id": cat_id,
        "_csrf": form_csrf
    }
    
    # Enviar como form data padrão
    response = session.post(CONTA_SAVE_URL, data=conta_data, allow_redirects=True)
    if response.status_code == 200:
        print(f"✅ Requisição de salvamento enviada (URL final: {response.url})")
    else:
        print(f"❌ Falha ao salvar conta: {response.status_code}")

    # 5. Verificar se a conta aparece na lista
    print("--- 5. Verificando Persistência na Lista ---")
    response = session.get(CONTA_LIST_URL)
    if "Teste de Resiliência Antigravity" in response.text:
        print("✅ SUCESSO! O lançamento foi encontrado e associado ao usuário.")
    else:
        print("❌ ERRO: O lançamento não foi encontrado na listagem.")
        # Opcional: imprimir o corpo para debug se falhar muito
        # print(response.text[:1000])

    # 6. Dashboard Health Check
    print("--- 6. Verificando Saúde do Dashboard ---")
    response = session.get(DASHBOARD_URL)
    if response.status_code == 200 and "Zen" in response.text:
        print("✅ Dashboard operacional!")
    else:
        print(f"❌ Dashboard com problemas: {response.status_code}")

    print("\n🏁 Teste Concluído!")

if __name__ == "__main__":
    test_fingen_resilience()
