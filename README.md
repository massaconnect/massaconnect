# MassaPay - Secure Massa Blockchain Wallet

<div align="center">

![MassaPay Logo](screenshots/icono.png)

**Your keys. Your crypto. Your freedom.**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Version](https://img.shields.io/badge/Version-1.2.1-blue.svg)](https://github.com/massawallet/massapay/releases)

[Download APK](https://github.com/massawallet/massapay/releases/latest) | [Privacy Policy](PRIVACY_POLICY_EN.md) | [Report Bug](https://github.com/massawallet/massapay/issues)

</div>

---

## 🆕 What's New in v1.1.0

### ✨ New Features

- **🥩 Staking (Rolls)**
  - Buy Rolls with MAS (1 Roll = 100 MAS)
  - Sell Rolls to get MAS back (3 cycles delay)
  - View current rolls and deferred credits
  - Real-time staking info from blockchain

- **📊 Portfolio**
  - View all your tokens in one place
  - Real USD values from live price feeds
  - Support for MAS, WMAS, USDC.e, DAI.e, WETH.e, DUSA
  - Total portfolio value in Dashboard

- **🔄 Enhanced Swap**
  - Multi-path routing (5 different routes tried)
  - Better success rate for token pairs
  - Improved DAI.e routing through USDC.e
  - WRAP/UNWRAP MAS ↔ WMAS support

### 🐛 Bug Fixes

- Fixed balance display in Staking screen
- Fixed roll count calculation (max instead of sum)
- Fixed LockScreen logo in dark mode
- Fixed deferred credits parsing
- Improved token price accuracy

---

## About MassaPay

MassaPay is a **secure, self-custodial cryptocurrency wallet** for the Massa blockchain. Built with privacy and security as core principles, MassaPay gives you complete control over your digital assets.

---

## 📱 Screenshots

<p align="center">
  <img src="screenshots/1 (1).jpeg" width="180" alt="Lock Screen"/>
  <img src="screenshots/1 (2).jpeg" width="180" alt="Dashboard"/>
  <img src="screenshots/1 (3).jpeg" width="180" alt="Portfolio"/>
  <img src="screenshots/1 (4).jpeg" width="180" alt="Send"/>
  <img src="screenshots/1 (5).jpeg" width="180" alt="Receive"/>
</p>

<p align="center">
  <img src="screenshots/1 (6).jpeg" width="180" alt="Swap"/>
  <img src="screenshots/1 (7).jpeg" width="180" alt="Staking"/>
  <img src="screenshots/1 (8).jpeg" width="180" alt="Massa Statistics"/>
  <img src="screenshots/1 (9).jpeg" width="180" alt="Settings"/>
  <img src="screenshots/1 (10).jpeg" width="180" alt="NFT Gallery"/>
</p>

---

## ✨ Key Features

### 💰 Wallet
- **Self-Custodial**: You own your keys, you own your crypto
- **Multi-Account Support**: Manage multiple wallets
- **Real-time Balance**: MAS balance with USD value
- **Transaction History**: View all your transactions

### 🔄 DeFi
- **Token Swap**: Exchange tokens via DUSA DEX
- **Multi-path Routing**: 5 different routes for best rates
- **WRAP/UNWRAP**: Convert MAS ↔ WMAS
- **Staking**: Buy and sell Rolls for network participation

### 📊 Portfolio
- **All Tokens**: View MAS, WMAS, USDC.e, DAI.e, WETH.e, DUSA
- **Live Prices**: Real-time USD values
- **Total Value**: Combined portfolio worth

### 🖼️ NFTs
- **NFT Gallery**: View your NFT collection
- **MNS Domains**: Massa Name Service support
- **NFT Details**: View metadata and images

### 🌐 DApps
- **Built-in Browser**: Access Massa DApps
- **MassaStation Compatible**: Works with Massa ecosystem
- **Transaction Signing**: Approve DApp transactions

### 🔒 Security
- **AES-256 Encryption**: Bank-level security
- **Biometric Auth**: Fingerprint and face recognition
- **PIN Protection**: 6-digit PIN backup
- **No Data Collection**: Complete privacy

---

## 🗺️ Roadmap

### ✅ Version 1.0.0 (Released)
- [x] Wallet creation and import (12/24-word seed)
- [x] Send and receive MAS
- [x] Transaction history
- [x] Biometric authentication
- [x] Dark/Light themes
- [x] QR code scanning
- [x] Price tracking (USD/EUR)

### ✅ Version 1.1.0 (Current Release)
- [x] **Staking support** (Buy/Sell Rolls)
- [x] **Portfolio view** with all tokens
- [x] **Enhanced Swap** with multi-path routing
- [x] **DApp browser** improvements
- [x] **NFT gallery** support
- [x] Multi-account support
- [x] Real-time USD values for all tokens

### 🔜 Version 1.2.0 (Planned)
- [ ] WalletConnect v2 integration
- [ ] Hardware wallet support (Ledger)
- [ ] Push notifications for transactions
- [ ] Address book / contacts
- [ ] Export transaction history (CSV)

### 🔮 Future Versions
- [ ] Multi-language support (ES, FR, DE, PT)
- [ ] iOS version
- [ ] Widget for home screen
- [ ] Recurring payments
- [ ] Token price alerts

---

## 🛠️ Technology Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Networking | Retrofit + OkHttp |
| Storage | EncryptedSharedPreferences |
| Crypto | BouncyCastle + Android Keystore |
| Blockchain | Massa (Ed25519, BLAKE3) |

---

## 📦 Project Structure

```
massapay/
├── app/              # Main application module
├── core/             # Core models and utilities
├── network/          # Massa API and repositories
├── security/         # Cryptography and key management
├── ui/               # UI components and screens
└── price/            # Price tracking module
```

---

## 🔧 Building from Source

### Prerequisites

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34+
- Gradle 8.4+

### Clone and Build

```bash
git clone https://github.com/massawallet/massapay.git
cd massapay
./gradlew assembleDebug
```

The APK will be in `app/build/outputs/apk/debug/app-debug.apk`

---

## 🔐 Security

### Cryptography
- **Key Derivation**: BIP-39 (12/24-word seed phrases)
- **Encryption**: AES-256-GCM
- **Signing**: Ed25519 (Massa standard)
- **Hashing**: BLAKE3 (Massa standard)

### Responsible Disclosure
If you discover a security vulnerability, please email: security@massapay.online

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [Massa](https://massa.net/) - The decentralized blockchain
- [DUSA](https://dusa.io/) - Decentralized exchange
- [Material Design 3](https://m3.material.io/) - UI design system
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit

---

## ⚠️ Disclaimer

MassaPay is a self-custodial wallet. **You are solely responsible for**:

- Backing up your seed phrase
- Keeping your seed phrase secure
- Not sharing your private keys

**We cannot recover lost wallets.** If you lose your seed phrase, your funds are **permanently lost**.

---

<div align="center">

**Made with ❤️ for the Massa community**

**Developed by mderramus**

[Website](https://massapay.online) • [GitHub](https://github.com/massawallet/massapay)

</div>
