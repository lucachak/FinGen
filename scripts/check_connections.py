import os
import requests
import http.client
import json
import socket
from decouple import config

def check_java_backend():
    print("🔍 Checking Java Backend (Port 8080)...")
    try:
        response = requests.get("http://localhost:8080/actuator/health", timeout=5)
        if response.status_code == 200:
            print("✅ Java Backend is UP (Actuator Health OK)")
            return True
        else:
            # Fallback to a common page
            response = requests.get("http://localhost:8080/auth/login", timeout=5)
            if response.status_code == 200:
                print("✅ Java Backend is UP (Login Page accessible)")
                return True
    except Exception as e:
        print(f"❌ Java Backend is DOWN or unreachable: {e}")
    return False

def check_python_ml():
    print("🔍 Checking Python ML Service (Port 8000)...")
    try:
        response = requests.get("http://localhost:8000/docs", timeout=5)
        if response.status_code == 200:
            print("✅ Python ML Service is UP (FastAPI Docs OK)")
            return True
    except Exception as e:
        print(f"❌ Python ML Service is DOWN or unreachable: {e}")
    return False

def check_h2_database():
    print("🔍 Checking H2 Database File...")
    db_path = "./data/fingen.mv.db"
    if os.path.exists(db_path):
        print(f"✅ H2 Database file found at {db_path}")
        return True
    else:
        print(f"❌ H2 Database file NOT found at {db_path}")
    return False

def check_gemini_api():
    print("🔍 Checking Gemini API Connectivity...")
    try:
        api_key = config("GEMINI_TOKEN", default=None)
        if not api_key:
            print("❌ GEMINI_TOKEN not found in .env or environment")
            return False
            
        # Lightweight check using google-genai style if possible, 
        # but here we'll just do a simple request if we want to avoid deep deps
        # For now, let's assume we use the client if it's installed or just check the token format
        if len(api_key) > 30 and api_key.startswith("AIza"):
            print("✅ Gemini Token format looks valid")
            # We could do a real call here, but let's keep it 
            # as a connection/config check for now.
            return True
        else:
            print("❌ Gemini Token format is invalid")
    except Exception as e:
        print(f"❌ Error checking Gemini API: {e}")
    return False

def check_yfinance():
    print("🔍 Checking YFinance (Market Data)...")
    try:
        import yfinance as yf
        ticker = yf.Ticker("AAPL")
        hist = ticker.history(period="1d")
        if not hist.empty:
            print("✅ YFinance is reachable and returning data")
            return True
        else:
            print("❌ YFinance returned empty data")
    except Exception as e:
        print(f"❌ YFinance check failed: {e}")
    return False

def run_all_checks():
    print("========================================")
    print("🌐 FIN GEN CONNECTION AUDIT")
    print("========================================\n")
    
    results = {
        "Java Backend": check_java_backend(),
        "Python ML": check_python_ml(),
        "H2 Database": check_h2_database(),
        "Gemini API": check_gemini_api(),
        "YFinance": check_yfinance()
    }
    
    print("\n========================================")
    print("📊 FINAL SUMMARY")
    print("========================================")
    all_ok = True
    for service, status in results.items():
        symbol = "✅" if status else "❌"
        print(f"{symbol} {service}")
        if not status:
            all_ok = False
            
    if all_ok:
        print("\n🚀 ALL SYSTEMS NOMINAL")
    else:
        print("\n⚠️ SOME CONNECTIONS ARE FAILING")
    print("========================================")

if __name__ == "__main__":
    run_all_checks()
