import os
import subprocess
import json
import re
import sys

def run_ux_audit():
    print("🎨 Running Core UX Audit (Psychological Laws, Typography, Visual Effects)...")
    try:
        # Resolve the absolute path to the ux_audit script
        base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        audit_script = os.path.join(base_dir, ".agent", "skills", "frontend-design", "scripts", "ux_audit.py")
        templates_dir = os.path.join(base_dir, "src", "main", "resources", "templates")
        
        result = subprocess.run(
            [sys.executable, audit_script, templates_dir, "--json"],
            capture_output=True, text=True
        )
        
        if result.returncode != 0:
            # It returns 1 if issues are found, which is fine for us
            pass
            
        return json.loads(result.stdout)
    except Exception as e:
        print(f"❌ Failed to run UX Audit: {e}")
        return None

def check_zen_serenity_compliance():
    print("🌿 Checking Zen Serenity Design System Compliance...")
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    layout_path = os.path.join(base_dir, "src", "main", "resources", "templates", "layout", "layout.html")
    
    compliance = {
        "Brand Color (#0F766E)": False,
        "Zen Tokens": False,
        "Lucide Icons": False,
        "Glassmorphism": False,
        "No Purple": True
    }
    
    if not os.path.exists(layout_path):
        print(f"❌ Layout file not found: {layout_path}")
        return compliance

    with open(layout_path, "r", encoding="utf-8") as f:
        content = f.read()
        
        if "#0F766E" in content.upper():
            compliance["Brand Color (#0F766E)"] = True
        if "zen-bg" in content or "zen-t1" in content:
            compliance["Zen Tokens"] = True
        if "lucide.createIcons()" in content:
            compliance["Lucide Icons"] = True
        if "glass-panel" in content or "backdrop-blur" in content:
            compliance["Glassmorphism"] = True
            
        # Additional Purple check (beyond ux_audit)
        purple_matches = re.findall(r'#8B5CF6|#A855F7|violet|purple', content, re.I)
        if purple_matches:
            compliance["No Purple"] = False
            
    return compliance

def check_emojis_vs_icons():
    print("💎 Scanning for Emojis (Should be Lucide Icons)...")
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    templates_dir = os.path.join(base_dir, "src", "main", "resources", "templates")
    
    # Common emojis used in financial apps
    emojis = ["💰", "📈", "📉", "🏦", "💵", "💸", "💳"]
    found_emojis = []
    
    for root, _, files in os.walk(templates_dir):
        for file in files:
            if file.endswith(".html"):
                path = os.path.join(root, file)
                with open(path, "r", encoding="utf-8", errors="replace") as f:
                    content = f.read()
                    for emoji in emojis:
                        if emoji in content:
                            found_emojis.append(f"{file} ({emoji})")
                            
    return found_emojis

def main():
    print("========================================")
    print("✨ FIN GEN UI/UX AUDIT (PRO MAX)")
    print("========================================\n")
    
    # 1. UX Audit
    report = run_ux_audit()
    if report:
        print(f"✅ Checked {report['files_checked']} template files.")
        print(f"⚠️ {len(report['warnings'])} UX Warnings found.")
        print(f"❌ {len(report['issues'])} Critical UX Issues found.")
        
        if report['issues']:
            print("\nCritical Issues Highlights:")
            for issue in report['issues'][:5]:
                print(f"  - {issue}")
    
    print("\n----------------------------------------")
    
    # 2. Brand Compliance
    brand = check_zen_serenity_compliance()
    for check, status in brand.items():
        symbol = "✅" if status else "❌"
        print(f"{symbol} {check}")
        
    print("\n----------------------------------------")
    
    # 3. Emoji Check
    emojis = check_emojis_vs_icons()
    if emojis:
        print(f"⚠️ {len(emojis)} Emojis found. Consider replacing with Lucide Icons.")
        for item in emojis[:5]:
            print(f"  - {item}")
    else:
        print("✅ No legacy emojis found in templates.")
        
    print("\n========================================")
    print("🏁 UI/UX AUDIT COMPLETE")
    print("========================================")

if __name__ == "__main__":
    main()
