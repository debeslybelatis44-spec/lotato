# LOTATO PRO - Option B : Serveur HTTP Local (PrintBridge Server)

## 📋 Description
APK Android qui crée un **serveur HTTP sur `localhost:8787`** en arrière-plan.
Ton PWA `https://lotato1.onrender.com` envoie des requêtes POST à ce serveur
pour déclencher l'impression sur l'imprimante Sunmi V2S.

## 🗂️ Structure des fichiers
```
option-B-server/
├── .github/workflows/build.yml         ← GitHub Actions
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/lotato/printserver/
│       │   ├── MainActivity.kt         ← Interface on/off
│       │   ├── PrintService.kt         ← Service arrière-plan (sticky)
│       │   └── PrintHttpServer.kt      ← Serveur NanoHTTPD port 8787
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/strings.xml
│           └── values/themes.xml
├── build.gradle
├── settings.gradle
└── gradle/wrapper/gradle-wrapper.properties
```

## 🔌 Intégration dans ton PWA (lotato1.onrender.com)

```javascript
// ════════════════════════════════════════════════
// LOTATO PRO - Impression via serveur local
// ════════════════════════════════════════════════

const PRINT_SERVER = 'http://localhost:8787';

// Vérifier si le serveur est disponible
async function checkPrinter() {
  try {
    const res = await fetch(`${PRINT_SERVER}/status`, { signal: AbortSignal.timeout(2000) });
    const data = await res.json();
    return data.printer === 'connected';
  } catch {
    return false;
  }
}

// Impression texte simple
async function imprimerTexte(texte) {
  try {
    const res = await fetch(`${PRINT_SERVER}/print`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ type: 'text', text: texte })
    });
    return await res.json();
  } catch (e) {
    console.error('Serveur impression non disponible', e);
    return { success: false, error: e.message };
  }
}

// Impression ticket structuré
async function imprimerTicket(ticketData) {
  try {
    const res = await fetch(`${PRINT_SERVER}/print`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        type: 'ticket',
        header: ticketData.header || 'LOTATO PRO',
        lines: ticketData.lines || [],
        footer: ticketData.footer || new Date().toLocaleString('fr-HT')
      })
    });
    return await res.json();
  } catch (e) {
    console.error('Erreur impression', e);
    return { success: false, error: e.message };
  }
}

// ── Exemple d'utilisation ────────────────────────
// const ok = await checkPrinter();
// if (ok) {
//   await imprimerTicket({
//     header: "LOTATO PRO",
//     lines: [
//       "Agent: Jean Pierre",
//       "Numéro: 12-34-56",
//       "Mise: 500 HTG",
//       "Date: " + new Date().toLocaleDateString('fr-HT')
//     ],
//     footer: "Bonne chance!"
//   });
// }
```

## 🚀 Déploiement via GitHub

```bash
git init
git add .
git commit -m "Initial - LOTATO PrintBridge Option B"
git remote add origin https://github.com/TON-USERNAME/lotato-printbridge-b.git
git push -u origin main
```

→ Aller dans **Actions** → télécharger l'APK depuis **Artifacts**

### Release avec tag
```bash
git tag v1.0.0
git push origin v1.0.0
# → Release GitHub créée avec APK
```

## 📱 Utilisation sur Sunmi V2S
1. Installer l'APK
2. Ouvrir **LOTATO PrintBridge**
3. Appuyer sur **▶ DÉMARRER LE SERVEUR**
4. L'app reste en arrière-plan (notification persistante)
5. Ouvrir Chrome → aller sur `https://lotato1.onrender.com`
6. Chaque impression envoie une requête à `localhost:8787`

## ✅ Avantages Option B
- **Flexible** : ton PWA reste dans Chrome, pas besoin de WebView custom
- **Indépendant** : si tu changes l'URL, pas besoin de recompiler
- **Visible** : tu vois l'état du serveur dans l'app
- **Extensible** : d'autres apps peuvent aussi utiliser le serveur

## ⚠️ Inconvénients
- Deux apps à gérer (Chrome + PrintBridge)
- Il faut démarrer le serveur manuellement (ou au boot)
- Chrome doit autoriser les requêtes `localhost` (normalement OK sur Android)
