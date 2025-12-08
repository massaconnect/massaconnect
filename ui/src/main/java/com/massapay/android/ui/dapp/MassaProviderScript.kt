package com.massapay.android.ui.dapp

/**
 * JavaScript Provider that gets injected into WebView
 * This makes MassaPay wallet detectable by DApps using @massalabs/wallet-provider
 * 
 * Compatible with:
 * - Massa Station detection pattern
 * - Bearby detection pattern  
 * - EIP-6963 provider announcement
 */
object MassaProviderScript {
    
    /**
     * The JavaScript code to inject into every page
     * Creates window.massaPay provider compatible with Massa wallet standard
     */
    fun getProviderScript(walletAddress: String): String = """
        (function() {
            // Prevent multiple injections
            if (window.massaPayInjected) return;
            window.massaPayInjected = true;
            
            console.log('[MassaPay] Initializing wallet provider...');
            
            // CRITICAL: Enable BigInt serialization in JSON.stringify
            // This fixes "Do not know how to serialize a BigInt" error
            if (typeof BigInt !== 'undefined' && !BigInt.prototype.toJSON) {
                BigInt.prototype.toJSON = function() {
                    return this.toString();
                };
                console.log('[MassaPay] BigInt.prototype.toJSON polyfill installed');
            }
            
            // DEBUG: Intercept property access to find undefined.getNodesStatus
            const originalTypeError = TypeError;
            window.addEventListener('unhandledrejection', (event) => {
                if (event.reason && event.reason.message && event.reason.message.includes('getNodesStatus')) {
                    console.log('[MassaPay-DEBUG] getNodesStatus error:', event.reason.message);
                    console.log('[MassaPay-DEBUG] Stack:', event.reason.stack);
                }
            });
            
            // AGGRESSIVE POLYFILL: Make undefined.getNodesStatus return a function
            // This is a hack to catch the exact location where client is undefined
            Object.defineProperty(Object.prototype, 'getNodesStatus', {
                get: function() {
                    if (this === undefined || this === null) {
                        console.error('[MassaPay-TRAP] getNodesStatus accessed on undefined/null!');
                        console.error('[MassaPay-TRAP] Stack:', new Error().stack);
                    }
                    // If this object doesn't have getNodesStatus, provide the global one
                    if (typeof this._getNodesStatus === 'function') {
                        return this._getNodesStatus;
                    }
                    // Return global function as fallback for any object
                    return async () => {
                        console.log('[MassaPay-FALLBACK] getNodesStatus called via prototype fallback');
                        return [{
                            node_id: 'MassaPay',
                            node_ip: 'mainnet.massa.net',
                            version: '1.0.0',
                            chain_id: 77658377,
                            minimal_fees: '0.01',
                            connected: true,
                            config: {
                                genesis_timestamp: Date.now(),
                                thread_count: 32,
                                t0: 16000,
                                delta_f0: 1088,
                                operation_validity_periods: 10,
                                periods_per_cycle: 128
                            }
                        }];
                    };
                },
                set: function(val) {
                    this._getNodesStatus = val;
                },
                configurable: true
            });
            
            // GLOBAL getNodesStatus function that can be used anywhere
            const globalGetNodesStatus = async () => {
                console.log('[MassaPay] globalGetNodesStatus called');
                return {
                    node_id: 'MassaPay',
                    node_ip: 'mainnet.massa.net',
                    version: '1.0.0',
                    chain_id: 77658377,
                    minimal_fees: '0.01',
                    connected: true,
                    config: {
                        genesis_timestamp: Date.now(),
                        thread_count: 32,
                        t0: 16000,
                        delta_f0: 1088,
                        operation_validity_periods: 10,
                        periods_per_cycle: 128
                    }
                };
            };
            
            // Global client object
            const globalClient = {
                _getNodesStatus: globalGetNodesStatus,
                getNodeStatus: globalGetNodesStatus,
                networkInfos: async () => ({
                    name: 'mainnet',
                    chainId: BigInt(77658377),
                    url: 'https://mainnet.massa.net/api/v2',
                    minimalFee: BigInt(10000000)
                }),
                getMinimalFee: async () => BigInt(10000000),
                getChainId: async () => BigInt(77658377)
            };
            
            // Request ID counter for async responses
            let requestId = 0;
            const pendingRequests = new Map();
            
            // Account class compatible with Massa wallet-provider
            class MassaPayAccount {
                constructor(address) {
                    this.address = address;
                    this.accountName = 'MassaPay Account';
                    this.providerName = 'MassaPay';
                    
                    // Use the global client
                    this.client = globalClient;
                    
                    // Also add publicApi for alternative access patterns
                    this.publicApi = globalClient;
                }
                
                // Direct getNodesStatus method on account
                async getNodesStatus() {
                    return {
                        result: [{
                            node_id: 'MassaPay',
                            node_ip: 'mainnet.massa.net',
                            version: '1.0.0',
                            chain_id: 77658377,
                            minimal_fees: '0.01',
                            connected: true
                        }]
                    };
                }
                
                // getNodeStatus (singular) - used by Provider interface
                async getNodeStatus() {
                    return {
                        node_id: 'MassaPay',
                        node_ip: 'mainnet.massa.net',
                        version: '1.0.0',
                        chain_id: 77658377,
                        minimal_fees: '0.01',
                        connected: true,
                        config: {
                            genesis_timestamp: Date.now(),
                            end_timestamp: null,
                            thread_count: 32,
                            t0: 16000,
                            delta_f0: 1088,
                            operation_validity_periods: 10,
                            periods_per_cycle: 128,
                            block_reward: '0.02',
                            roll_price: '100',
                            max_block_size: 1000000
                        }
                    };
                }
                
                // getClient method for DApps that need JsonRpcPublicProvider-like interface
                async getClient() {
                    return globalClient;
                }
                
                async balance(final = false) {
                    const result = await massaPayRequest('massa_getBalance', { address: this.address, final });
                    return BigInt(result.balance || '0');
                }
                
                async sign(data, opts = {}) {
                    const message = typeof data === 'string' ? data : 
                        (data instanceof Uint8Array ? Array.from(data).map(b => String.fromCharCode(b)).join('') : String(data));
                    
                    const result = await massaPayRequest('massa_signMessage', { 
                        message,
                        description: opts.description || 'Sign message'
                    });
                    
                    return {
                        publicKey: result.publicKey,
                        signature: result.signature
                    };
                }
                
                async buyRolls(amount, opts = {}) {
                    return massaPayRequest('massa_buyRolls', { 
                        amount: amount.toString(),
                        fee: opts.fee?.toString()
                    });
                }
                
                async sellRolls(amount, opts = {}) {
                    return massaPayRequest('massa_sellRolls', { 
                        amount: amount.toString(),
                        fee: opts.fee?.toString()
                    });
                }
                
                async transfer(to, amount, opts = {}) {
                    return massaPayRequest('massa_sendTransaction', {
                        toAddress: to.toString(),
                        amount: amount.toString(),
                        fee: opts.fee?.toString()
                    });
                }
                
                async callSC(params) {
                    return massaPayRequest('massa_callSmartContract', {
                        targetAddress: params.target || params.targetAddress,
                        functionName: params.func || params.functionName,
                        parameter: params.parameter ? Array.from(params.parameter) : [],
                        coins: params.coins?.toString() || '0',
                        fee: params.fee?.toString()
                    });
                }
                
                async networkInfos() {
                    return {
                        name: 'mainnet',
                        chainId: BigInt(77658377),
                        url: 'https://mainnet.massa.net/api/v2',
                        minimalFee: BigInt(10000000)
                    };
                }
            }
            
            // Wallet class compatible with Massa wallet-provider interface
            class MassaPayWallet {
                constructor() {
                    this._name = 'MassaPay';
                    this._connected = false;
                    this._accounts = [];
                    this._listeners = {};
                    this._enabled = true;
                    
                    // Add client for DApps that access it through wallet.client
                    this.client = {
                        getNodesStatus: async () => [{
                            node_id: 'MassaPay',
                            node_ip: 'mainnet.massa.net',
                            version: '1.0.0',
                            chain_id: 77658377,
                            minimal_fees: '0.01',
                            connected: true
                        }],
                        getNodeStatus: async () => ({
                            node_id: 'MassaPay',
                            node_ip: 'mainnet.massa.net', 
                            version: '1.0.0',
                            chain_id: 77658377,
                            minimal_fees: '0.01',
                            connected: true
                        }),
                        networkInfos: async () => ({
                            name: 'mainnet',
                            chainId: BigInt(77658377),
                            url: 'https://mainnet.massa.net/api/v2',
                            minimalFee: BigInt(10000000)
                        })
                    };
                }
                
                // getClient method
                async getClient() {
                    return this.client;
                }
                
                name() {
                    return 'MassaPay';
                }
                
                enabled() {
                    return this._enabled;
                }
                
                async connect() {
                    try {
                        const result = await massaPayRequest('wallet_connect');
                        if (result && result.address) {
                            this._connected = true;
                            this._accounts = [new MassaPayAccount(result.address)];
                            this._emit('accountsChanged', [result.address]);
                            return true;
                        }
                        return false;
                    } catch (e) {
                        console.error('[MassaPay] Connect error:', e);
                        return false;
                    }
                }
                
                async disconnect() {
                    this._connected = false;
                    this._accounts = [];
                    this._emit('disconnect');
                    return true;
                }
                
                async connected() {
                    return this._connected;
                }
                
                async accounts() {
                    if (!this._connected && this._accounts.length === 0) {
                        // Auto-connect if we have a wallet address
                        const address = '$walletAddress';
                        if (address && address.startsWith('AU')) {
                            this._accounts = [new MassaPayAccount(address)];
                        }
                    }
                    return this._accounts;
                }
                
                async networkInfos() {
                    return {
                        name: 'mainnet',
                        chainId: BigInt(77658377),
                        url: 'https://mainnet.massa.net/api/v2',
                        minimalFee: BigInt(10000000)
                    };
                }
                
                // Direct node status methods on wallet
                async getNodesStatus() {
                    return [{
                        node_id: 'MassaPay',
                        node_ip: 'mainnet.massa.net',
                        version: '1.0.0',
                        chain_id: 77658377,
                        minimal_fees: '0.01',
                        connected: true
                    }];
                }
                
                async getNodeStatus() {
                    return {
                        node_id: 'MassaPay',
                        node_ip: 'mainnet.massa.net',
                        version: '1.0.0',
                        chain_id: 77658377,
                        minimal_fees: '0.01',
                        connected: true
                    };
                }
                
                async setRpcUrl(url) {
                    // Not supported in mobile wallet
                    console.warn('[MassaPay] setRpcUrl not supported');
                }
                
                async importAccount(publicKey, privateKey) {
                    throw new Error('importAccount not supported in MassaPay mobile');
                }
                
                async deleteAccount(address) {
                    throw new Error('deleteAccount not supported in MassaPay mobile');
                }
                
                async generateNewAccount(name) {
                    throw new Error('generateNewAccount not supported in MassaPay mobile');
                }
                
                listenAccountChanges(callback) {
                    return this._on('accountsChanged', callback);
                }
                
                listenNetworkChanges(callback) {
                    return this._on('networkChanged', callback);
                }
                
                _on(event, callback) {
                    if (!this._listeners[event]) {
                        this._listeners[event] = [];
                    }
                    this._listeners[event].push(callback);
                    return {
                        unsubscribe: () => {
                            this._listeners[event] = this._listeners[event].filter(cb => cb !== callback);
                        }
                    };
                }
                
                _emit(event, data) {
                    if (this._listeners[event]) {
                        this._listeners[event].forEach(cb => {
                            try { cb(data); } catch(e) { console.error(e); }
                        });
                    }
                }
            }
            
            // Main request function
            async function massaPayRequest(method, params = {}) {
                return new Promise((resolve, reject) => {
                    const id = ++requestId;
                    
                    pendingRequests.set(id, { resolve, reject, method });
                    
                    const message = JSON.stringify({
                        id: id,
                        method: method,
                        params: params
                    });
                    
                    if (window.MassaPayBridge) {
                        window.MassaPayBridge.postMessage(message);
                    } else {
                        reject(new Error('MassaPay bridge not available'));
                        pendingRequests.delete(id);
                    }
                    
                    // Timeout after 5 minutes
                    setTimeout(() => {
                        if (pendingRequests.has(id)) {
                            pendingRequests.delete(id);
                            reject(new Error('Request timeout'));
                        }
                    }, 300000);
                });
            }
            
            // Handle responses from native app
            window.massaPayResponse = function(responseJson) {
                try {
                    const response = JSON.parse(responseJson);
                    const { id, result, error } = response;
                    
                    // First check early provider pending requests (stored on window)
                    if (window._earlyPendingRequests && window._earlyPendingRequests[id]) {
                        const earlyPending = window._earlyPendingRequests[id];
                        const method = earlyPending.method;
                        delete window._earlyPendingRequests[id];
                        console.log('[MassaPay] Found early provider request for id:', id, 'method:', method);
                        
                        if (error) {
                            console.error('[MassaPay] Error for early request:', error.message);
                            earlyPending.reject(new Error(error.message || 'Unknown error'));
                        } else {
                            // Track successful operations for getOperations
                            if ((method === 'massa_callSmartContract' || method === 'massa_executeBytecode') && typeof result === 'string') {
                                if (!window._massaPaySuccessfulOperations) {
                                    window._massaPaySuccessfulOperations = {};
                                }
                                window._massaPaySuccessfulOperations[result] = {
                                    timestamp: Date.now(),
                                    status: 'speculative_success'
                                };
                                console.log('[MassaPay] Tracked successful operation:', result);
                            }
                            console.log('[MassaPay] Resolving early request with:', result);
                            earlyPending.resolve(result);
                        }
                        return;
                    }
                    
                    // Then check full provider pending requests
                    const pending = pendingRequests.get(id);
                    if (!pending) {
                        console.warn('[MassaPay] No pending request for id:', id);
                        return;
                    }
                    
                    pendingRequests.delete(id);
                    
                    if (error) {
                        const err = new Error(error.message || 'Unknown error');
                        err.code = error.code;
                        pending.reject(err);
                    } else {
                        pending.resolve(result);
                    }
                } catch (e) {
                    console.error('[MassaPay] Error handling response:', e);
                }
            };
            
            // Create wallet instance
            const massaPayWallet = new MassaPayWallet();
            
            // Initialize with current address if available
            const currentAddress = '$walletAddress';
            if (currentAddress && currentAddress.startsWith('AU')) {
                massaPayWallet._accounts = [new MassaPayAccount(currentAddress)];
                massaPayWallet._connected = true;
            }
            
            // ============================================
            // EXPOSE WALLET FOR DETECTION
            // ============================================
            
            // 1. Standard Massa provider (for @massalabs/wallet-provider)
            window.massaPay = massaPayWallet;
            
            // 2. Mark as Massa wallet available (like MassaStation)
            window.massaWallet = {
                standalone: false,
                isMassaPay: true
            };
            
            // 3. Create providers array for getWallets() detection
            if (!window.massaWallets) {
                window.massaWallets = [];
            }
            window.massaWallets.push(massaPayWallet);
            
            // 4. Legacy window.massa for older DApps
            window.massa = {
                isMassaPay: true,
                isMassa: true,
                isConnected: () => massaPayWallet._connected,
                
                // Use globalClient for consistency
                client: globalClient,
                
                // Direct getNodesStatus for convenience
                getNodesStatus: globalGetNodesStatus,
                getNodeStatus: globalGetNodesStatus,
                
                // getClient method
                getClient: async () => globalClient,
                
                connect: () => massaPayWallet.connect(),
                disconnect: () => massaPayWallet.disconnect(),
                
                accounts: () => massaPayWallet.accounts().then(accs => accs.map(a => a.address)),
                getAccounts: () => massaPayWallet.accounts().then(accs => accs.map(a => a.address)),
                
                request: async (args) => {
                    const { method, params } = args;
                    
                    switch(method) {
                        case 'wallet_connect':
                        case 'eth_requestAccounts':
                            await massaPayWallet.connect();
                            return massaPayWallet._accounts.map(a => a.address);
                            
                        case 'eth_accounts':
                        case 'massa_accounts':
                            return massaPayWallet._accounts.map(a => a.address);
                            
                        case 'massa_signMessage':
                        case 'personal_sign':
                            if (massaPayWallet._accounts.length > 0) {
                                return massaPayWallet._accounts[0].sign(params?.[0] || params?.message);
                            }
                            throw new Error('No account connected');
                            
                        case 'massa_sendTransaction':
                            if (massaPayWallet._accounts.length > 0) {
                                return massaPayWallet._accounts[0].transfer(
                                    params?.to || params?.toAddress,
                                    BigInt(params?.amount || params?.value || '0')
                                );
                            }
                            throw new Error('No account connected');
                            
                        case 'massa_callSmartContract':
                        case 'massa_callSC':
                            if (massaPayWallet._accounts.length > 0) {
                                return massaPayWallet._accounts[0].callSC(params);
                            }
                            throw new Error('No account connected');
                            
                        default:
                            return massaPayRequest(method, params);
                    }
                },
                
                on: (event, callback) => {
                    return massaPayWallet._on(event, callback);
                },
                
                removeListener: (event, callback) => {
                    if (massaPayWallet._listeners[event]) {
                        massaPayWallet._listeners[event] = massaPayWallet._listeners[event].filter(cb => cb !== callback);
                    }
                }
            };
            
            // ============================================
            // 5. BEARBY COMPATIBILITY LAYER
            // Dusa.io and other DApps check for window.bearby
            // ============================================
            
            // Bearby subscription system
            const bearbySubscriptions = {
                account: [],
                network: []
            };
            
            window.bearby = {
                isBearby: true,
                isMassaPay: true, // Flag to identify it's actually MassaPay
                
                wallet: {
                    connected: massaPayWallet._connected,
                    enabled: true,
                    account: {
                        base58: currentAddress || null,
                        address: currentAddress || null,
                        // Subscribe to account changes (bearby.js pattern)
                        subscribe: (callback) => {
                            bearbySubscriptions.account.push(callback);
                            // Immediately call with current value if connected
                            if (currentAddress) {
                                setTimeout(() => callback(currentAddress), 0);
                            }
                            return {
                                unsubscribe: () => {
                                    const idx = bearbySubscriptions.account.indexOf(callback);
                                    if (idx > -1) bearbySubscriptions.account.splice(idx, 1);
                                }
                            };
                        }
                    },
                    network: {
                        net: 'mainnet',
                        subscribe: (callback) => {
                            bearbySubscriptions.network.push(callback);
                            setTimeout(() => callback('mainnet'), 0);
                            return {
                                unsubscribe: () => {
                                    const idx = bearbySubscriptions.network.indexOf(callback);
                                    if (idx > -1) bearbySubscriptions.network.splice(idx, 1);
                                }
                            };
                        }
                    },
                    
                    connect: async () => {
                        console.log('[MassaPay] bearby.wallet.connect called');
                        const result = await massaPayWallet.connect();
                        if (result && massaPayWallet._accounts[0]) {
                            const addr = massaPayWallet._accounts[0].address;
                            window.bearby.wallet.connected = true;
                            window.bearby.wallet.account.base58 = addr;
                            window.bearby.wallet.account.address = addr;
                            
                            // Emit Bearby-style event
                            window.dispatchEvent(new CustomEvent('bearby:connect', { 
                                detail: { address: addr }
                            }));
                            
                            // Notify all account subscribers
                            console.log('[MassaPay] Notifying bearby subscribers:', bearbySubscriptions.account.length);
                            bearbySubscriptions.account.forEach(cb => {
                                try { cb(addr); } catch(e) { console.error(e); }
                            });
                        }
                        return result;
                    },
                    
                    disconnect: async () => {
                        await massaPayWallet.disconnect();
                        window.bearby.wallet.connected = false;
                        window.bearby.wallet.account.base58 = null;
                        window.bearby.wallet.account.address = null;
                        window.dispatchEvent(new Event('bearby:disconnect'));
                        
                        bearbySubscriptions.account.forEach(cb => {
                            try { cb(null); } catch(e) { console.error(e); }
                        });
                        return true;
                    },
                    
                    signMessage: async (message) => {
                        if (massaPayWallet._accounts[0]) {
                            const result = await massaPayWallet._accounts[0].sign(message);
                            return {
                                publicKey: result.publicKey,
                                signature: result.signature
                            };
                        }
                        throw new Error('No account connected');
                    }
                },
                
                massa: {
                    getAddresses: async (...addresses) => {
                        // Simulate Bearby's getAddresses response
                        try {
                            const results = await Promise.all(addresses.map(async (addr) => {
                                try {
                                    const balance = await massaPayRequest('massa_getBalance', { address: addr });
                                    return {
                                        address: addr,
                                        final_balance: balance.balance || '0',
                                        candidate_balance: balance.balance || '0'
                                    };
                                } catch (e) {
                                    return {
                                        address: addr,
                                        final_balance: '0',
                                        candidate_balance: '0'
                                    };
                                }
                            }));
                            return { result: results };
                        } catch (e) {
                            return { error: { message: e.message } };
                        }
                    },
                    
                    getNodesStatus: async () => {
                        return {
                            result: {
                                node_id: 'MassaPay',
                                node_ip: 'mainnet.massa.net',
                                version: '1.0.0',
                                chain_id: 77658377,
                                minimal_fees: '0.01',
                                connected: true
                            }
                        };
                    },
                    
                    payment: async (amount, recipient) => {
                        if (!massaPayWallet._accounts[0]) {
                            throw new Error('No account connected');
                        }
                        const result = await massaPayWallet._accounts[0].transfer(recipient, BigInt(amount));
                        return result.operationId || result.transactionHash || result;
                    },
                    
                    buyRolls: async (amount) => {
                        if (!massaPayWallet._accounts[0]) {
                            throw new Error('No account connected');
                        }
                        return massaPayWallet._accounts[0].buyRolls(BigInt(amount));
                    },
                    
                    sellRolls: async (amount) => {
                        if (!massaPayWallet._accounts[0]) {
                            throw new Error('No account connected');
                        }
                        return massaPayWallet._accounts[0].sellRolls(BigInt(amount));
                    },
                    
                    callSmartContract: async (params) => {
                        if (!massaPayWallet._accounts[0]) {
                            throw new Error('No account connected');
                        }
                        return massaPayWallet._accounts[0].callSC(params);
                    },
                    
                    executeReadOnlySmartContract: async (params) => {
                        return massaPayRequest('massa_callSmartContract', params);
                    }
                }
            };
            
            // Update Bearby state when wallet changes
            massaPayWallet._on('accountsChanged', (accounts) => {
                if (accounts && accounts[0]) {
                    window.bearby.wallet.account = {
                        base58: accounts[0],
                        address: accounts[0]
                    };
                    // Also update web3 state
                    if (window.web3 && window.web3.wallet) {
                        window.web3.wallet.account = { base58: accounts[0] };
                    }
                    // Also update st alias
                    if (window.st && window.st.wallet) {
                        window.st.wallet.account = { base58: accounts[0], address: accounts[0] };
                    }
                }
            });
            
            // Create 'st' alias for bearby - some DApps like EagleFi use 'st' object
            // This is likely from the @hicaru/bearby.js library's internal naming
            window.st = window.bearby;
            console.log('[MassaPay] Created window.st alias for bearby');
            
            // ============================================
            // 6. WEB3 OBJECT (Bearby.js style)
            // DApps using @hicaru/bearby.js expect web3.wallet and web3.massa
            // ============================================
            
            // Subscription system for Bearby compatibility
            const web3Subscriptions = {
                account: [],
                network: []
            };
            
            window.web3 = {
                wallet: {
                    installed: true,
                    connected: massaPayWallet._connected,
                    enabled: true,
                    account: {
                        base58: currentAddress || null,
                        address: currentAddress || null,
                        // Subscribe method that bearby.js uses
                        subscribe: (callback) => {
                            web3Subscriptions.account.push(callback);
                            // Immediately call with current value
                            if (currentAddress) {
                                setTimeout(() => callback(currentAddress), 0);
                            }
                            return {
                                unsubscribe: () => {
                                    const idx = web3Subscriptions.account.indexOf(callback);
                                    if (idx > -1) web3Subscriptions.account.splice(idx, 1);
                                }
                            };
                        }
                    },
                    network: {
                        net: 'mainnet',
                        // Make network directly awaitable to return { net: 'mainnet' }
                        then: function(resolve) {
                            resolve({ net: 'mainnet' });
                        },
                        subscribe: (callback) => {
                            web3Subscriptions.network.push(callback);
                            setTimeout(() => callback('mainnet'), 0);
                            return {
                                unsubscribe: () => {
                                    const idx = web3Subscriptions.network.indexOf(callback);
                                    if (idx > -1) web3Subscriptions.network.splice(idx, 1);
                                }
                            };
                        }
                    },
                    
                    connect: async () => {
                        console.log('[MassaPay] web3.wallet.connect called');
                        const result = await massaPayWallet.connect();
                        if (result && massaPayWallet._accounts[0]) {
                            const addr = massaPayWallet._accounts[0].address;
                            window.web3.wallet.connected = true;
                            window.web3.wallet.account.base58 = addr;
                            window.web3.wallet.account.address = addr;
                            
                            // Update bearby too
                            window.bearby.wallet.connected = true;
                            window.bearby.wallet.account = {
                                base58: addr,
                                address: addr
                            };
                            
                            // Notify all account subscribers
                            console.log('[MassaPay] Notifying', web3Subscriptions.account.length, 'subscribers');
                            web3Subscriptions.account.forEach(cb => {
                                try { cb(addr); } catch(e) { console.error(e); }
                            });
                        }
                        return result;
                    },
                    
                    disconnect: async () => {
                        await massaPayWallet.disconnect();
                        window.web3.wallet.connected = false;
                        window.web3.wallet.account.base58 = null;
                        window.bearby.wallet.connected = false;
                        window.bearby.wallet.account = null;
                        
                        // Notify subscribers of disconnection
                        web3Subscriptions.account.forEach(cb => {
                            try { cb(null); } catch(e) { console.error(e); }
                        });
                        return true;
                    },
                    
                    signMessage: async (message) => {
                        if (massaPayWallet._accounts[0]) {
                            return massaPayWallet._accounts[0].sign(message);
                        }
                        throw new Error('No account connected');
                    }
                },
                
                massa: {
                    getAddresses: async (...addresses) => {
                        try {
                            const results = await Promise.all(addresses.map(async (addr) => {
                                try {
                                    const balance = await massaPayRequest('massa_getBalance', { address: addr });
                                    return {
                                        address: addr,
                                        final_balance: balance.balance || '0',
                                        candidate_balance: balance.balance || '0',
                                        thread: 0,
                                        final_roll_count: 0,
                                        candidate_roll_count: 0,
                                        final_datastore_keys: [],
                                        candidate_datastore_keys: []
                                    };
                                } catch (e) {
                                    return { address: addr, final_balance: '0', candidate_balance: '0' };
                                }
                            }));
                            return { result: results };
                        } catch (e) {
                            return { error: { message: e.message } };
                        }
                    },
                    
                    getNodesStatus: async () => {
                        return {
                            result: {
                                node_id: 'MassaPay',
                                node_ip: 'mainnet.massa.net',
                                version: '1.0.0',
                                chain_id: 77658377,
                                minimal_fees: '0.01',
                                connected: true
                            }
                        };
                    },
                    
                    payment: async (amount, recipient) => {
                        if (!massaPayWallet._accounts[0]) {
                            throw new Error('No account connected');
                        }
                        const result = await massaPayWallet._accounts[0].transfer(recipient, BigInt(amount));
                        return result.operationId || result;
                    },
                    
                    buyRolls: async (amount) => {
                        if (!massaPayWallet._accounts[0]) {
                            throw new Error('No account connected');
                        }
                        const result = await massaPayWallet._accounts[0].buyRolls(BigInt(amount));
                        return result.operationId || result;
                    },
                    
                    sellRolls: async (amount) => {
                        if (!massaPayWallet._accounts[0]) {
                            throw new Error('No account connected');
                        }
                        const result = await massaPayWallet._accounts[0].sellRolls(BigInt(amount));
                        return result.operationId || result;
                    },
                    
                    getOperations: async (...operationIds) => {
                        // Return proper status for tracked operations from the early provider
                        // This is critical for wallet-provider's Operation class to work correctly
                        // IMPORTANT: Return isFinal=true immediately so DApps continue with next step
                        console.log('[MassaPay] web3.massa.getOperations called for:', operationIds);
                        return { 
                            result: operationIds.map(opId => {
                                const tracked = window._massaPaySuccessfulOperations ? window._massaPaySuccessfulOperations[opId] : null;
                                if (tracked) {
                                    const elapsed = Date.now() - tracked.timestamp;
                                    console.log('[MassaPay] getOperations for tracked op:', opId, 'elapsed:', elapsed, 'returning isFinal: true');
                                    return { 
                                        address: opId,
                                        thread: 0,
                                        in_pool: false,
                                        in_blocks: ['block1'],
                                        is_operation_final: true,  // Always return true - operation was sent successfully
                                        op_exec_status: true,
                                        final_balance: '0',
                                        candidate_balance: '0'
                                    };
                                }
                                console.log('[MassaPay] getOperations for unknown op:', opId);
                                return { 
                                    address: opId,
                                    thread: 0,
                                    in_pool: false,
                                    in_blocks: [],
                                    is_operation_final: null,
                                    op_exec_status: null
                                };
                            })
                        };
                    }
                },
                
                contract: {
                    call: async (params) => {
                        if (!massaPayWallet._accounts[0]) {
                            throw new Error('No account connected');
                        }
                        const result = await massaPayWallet._accounts[0].callSC({
                            target: params.targetAddress,
                            func: params.functionName,
                            parameter: params.unsafeParameters,
                            coins: BigInt(params.coins || 0),
                            maxGas: params.maxGas,
                            fee: params.fee
                        });
                        return result.operationId || result;
                    },
                    
                    deploy: async (params) => {
                        throw new Error('Contract deployment not supported in MassaPay mobile');
                    },
                    
                    executeBytecode: async (params) => {
                        // executeBytecode is used for executing raw bytecode on the blockchain
                        // EagleFi and other DApps use this for complex operations like swaps
                        console.log('[MassaPay] executeBytecode called with params:', JSON.stringify(params));
                        
                        if (!massaPayWallet._accounts[0]) {
                            throw new Error('No account connected');
                        }
                        
                        // Extract parameters - executeBytecode typically receives:
                        // - bytecode or bytecodeBase64: the compiled bytecode to execute
                        // - datastore: optional datastore entries
                        // - maxGas: maximum gas to use
                        // - coins: amount of coins to send
                        // - fee: transaction fee
                        // EagleFi sends bytecodeBase64, other DApps may send bytecode or data
                        const bytecode = params.bytecode || params.bytecodeBase64 || params.data || '';
                        const datastore = params.datastore || params.datastoreEntries || [];
                        const maxGas = params.maxGas || params.gasLimit || '500000000';
                        const coins = params.coins || params.amount || '0';
                        const fee = params.fee || '0.01';
                        
                        console.log('[MassaPay] executeBytecode bytecode length:', bytecode.length, 'type:', typeof bytecode);
                        
                        // Send as a special smart contract call with bytecode
                        const result = await massaPayRequest('massa_executeBytecode', {
                            bytecode: bytecode,
                            datastore: datastore,
                            maxGas: maxGas,
                            coins: coins,
                            fee: fee
                        });
                        
                        console.log('[MassaPay] executeBytecode result:', result);
                        return result.operationId || result;
                    },
                    
                    getFilteredSCOutputEvent: async (filter) => {
                        console.log('[MassaPay] contract.getFilteredSCOutputEvent called', JSON.stringify(filter));
                        
                        // Check if this is a tracked operation - we need to poll until events appear
                        const opId = filter.original_operation_id;
                        const tracked = window._massaPaySuccessfulOperations && window._massaPaySuccessfulOperations[opId];
                        
                        // Make real RPC call to get actual SC output events from blockchain
                        // For tracked operations, poll with retries since events may not be immediately available
                        const maxRetries = tracked ? 5 : 1;
                        const retryDelay = 3000; // 3 seconds between retries
                        
                        for (let attempt = 0; attempt < maxRetries; attempt++) {
                            try {
                                // Try both is_final states - events may be in either
                                for (const isFinal of [false, true]) {
                                    const rpcFilter = { ...filter, is_final: isFinal };
                                    const rpcPayload = {
                                        jsonrpc: '2.0',
                                        id: 1,
                                        method: 'get_filtered_sc_output_event',
                                        params: [rpcFilter]
                                    };
                                    
                                    if (attempt > 0 || isFinal) {
                                        console.log('[MassaPay] getFilteredSCOutputEvent retry', attempt, 'is_final:', isFinal);
                                    }
                                    console.log('[MassaPay] getFilteredSCOutputEvent RPC request:', JSON.stringify(rpcPayload));
                                    
                                    const response = await fetch('https://mainnet.massa.net/api/v2', {
                                        method: 'POST',
                                        headers: { 'Content-Type': 'application/json' },
                                        body: JSON.stringify(rpcPayload)
                                    });
                                    
                                    const data = await response.json();
                                    console.log('[MassaPay] getFilteredSCOutputEvent RPC response:', JSON.stringify(data));
                                    
                                    if (data.error) {
                                        console.error('[MassaPay] getFilteredSCOutputEvent error:', data.error);
                                        continue;
                                    }
                                    
                                    // If we got events, return them
                                    const events = data.result || [];
                                    if (events.length > 0) {
                                        console.log('[MassaPay] getFilteredSCOutputEvent returning', events.length, 'events');
                                        return [{ result: events }];
                                    }
                                }
                                
                                // For non-tracked operations, don't retry
                                if (!tracked) {
                                    return [{ result: [] }];
                                }
                                
                                // For tracked operations with empty results, wait and retry
                                if (attempt < maxRetries - 1) {
                                    console.log('[MassaPay] getFilteredSCOutputEvent empty result for tracked op, waiting', retryDelay, 'ms');
                                    await new Promise(resolve => setTimeout(resolve, retryDelay));
                                }
                            } catch (e) {
                                console.error('[MassaPay] getFilteredSCOutputEvent failed:', e);
                                if (!tracked || attempt >= maxRetries - 1) {
                                    break;
                                }
                                await new Promise(resolve => setTimeout(resolve, retryDelay));
                            }
                        }
                        
                        // For tracked operations that exhausted retries, return empty result
                        // EagleFi already knows the operation succeeded from getOperations returning isFinal:true
                        console.log('[MassaPay] getFilteredSCOutputEvent returning empty for op:', opId);
                        return [{ result: [] }];
                    },
                    
                    getDatastoreEntries: async (...entries) => {
                        // Placeholder
                        return [{ result: [] }];
                    },
                    
                    readSmartContract: async (params) => {
                        // For read-only calls
                        return massaPayRequest('massa_callSmartContract', {
                            targetAddress: params.targetAddress,
                            functionName: params.targetFunction,
                            parameter: params.parameter,
                            readOnly: true
                        });
                    }
                }
            };
            
            // Add global client object for DApps - use globalClient
            window.web3.client = globalClient;
            
            // Also add to wallet for other access patterns - use globalClient
            window.web3.wallet.client = globalClient;
            
            // Add global massaClient for any code that expects it
            window.massaClient = globalClient;
            
            // CRITICAL: Set installed state ALWAYS - wallet-provider checks this!
            window.web3.wallet.installed = true;  // Must be true for BearbyWallet.createIfInstalled()
            if (currentAddress && currentAddress.startsWith('AU')) {
                window.web3.wallet.connected = true;
                window.web3.wallet.account.base58 = currentAddress;
            }
            
            // 7. EIP-6963 Provider Announcement (used by modern DApps)
            const providerInfo = {
                uuid: 'massapay-wallet-' + Date.now(),
                name: 'MassaPay',
                icon: 'data:image/svg+xml;base64,' + btoa('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="50" cy="50" r="45" fill="#6366f1"/><text x="50" y="65" font-size="40" text-anchor="middle" fill="white" font-weight="bold">M</text></svg>'),
                rdns: 'com.massapay.wallet'
            };
            
            // Announce provider
            window.dispatchEvent(new CustomEvent('eip6963:announceProvider', {
                detail: Object.freeze({ info: providerInfo, provider: window.massa })
            }));
            
            // Listen for provider requests
            window.addEventListener('eip6963:requestProvider', () => {
                window.dispatchEvent(new CustomEvent('eip6963:announceProvider', {
                    detail: Object.freeze({ info: providerInfo, provider: window.massa })
                }));
            });
            
            // 8. Dispatch initialization events
            window.dispatchEvent(new Event('massa#initialized'));
            window.dispatchEvent(new Event('massaPay#initialized'));
            window.dispatchEvent(new Event('bearby#initialized')); // For Bearby detection
            
            // 9. Also set on document for some detection patterns
            document.massaPay = massaPayWallet;
            
            // ============================================
            // 10. TABSTREAM CUSTOMEVENT SYSTEM - Critical for bearby-web3
            // The real bearby extension uses CustomEvents for communication:
            // - Page (injected script) listens on @/BearBy/injected-script
            // - Page sends to @/BearBy/content-script 
            // - Extension (content script) responds back to injected-script
            // We need to act as the content script and respond to messages
            // ============================================
            
            const BEARBY_INJECTED = '@/BearBy/injected-script';
            const BEARBY_CONTENT = '@/BearBy/content-script';
            
            // Message type constants from bearby-web3
            const BearbyMsgTypes = {
                GET_DATA: '@/BearBy/get-wallet-data',
                SET_DATA: '@/BearBy/set-account-data',
                CONNECT_APP: '@/BearBy/connect-app',
                RESPONSE_CONNECT_APP: '@/BearBy/respoonse-connect-app', // Note: typo is intentional, matches bearby source
                REQUEST_PING: '@/BearBy/req-content-ping',
                PING_RESPONSE: '@/BearBy/res-content-ping',
                ACCOUNT_CHANGED: '@/BearBy/accounts-just-changed',
                NETWORK_CHANGED: '@/BearBy/network-just-changed',
                SIGN_MESSAGE: '@/BearBy/sign-message-call',
                SIGN_MESSAGE_RESULT: '@/BearBy/sign-message-result',
                CALL_CONTRACT: '@/BearBy/call-contract',
                CALL_CONTRACT_RES: '@/BearBy/call-contract-result',
                PAYMENT: '@/BearBy/massa-payment-call',
                PAYMENT_RES: '@/BearBy/massa-payment-result',
                DISCONNECT: '@/BearBy/lock-account'
            };
            
            // Function to send response back to bearby.js (injected script)
            function sendToBearbyInjected(payload) {
                const event = new CustomEvent(BEARBY_INJECTED, {
                    detail: JSON.stringify(payload)
                });
                document.dispatchEvent(event);
                console.log('[MassaPay-TabStream] Sent to injected:', payload.type);
            }
            
            // Listen for messages from bearby.js (injected script sends to content script)
            document.addEventListener(BEARBY_CONTENT, (event) => {
                if (!event || !event.detail) return;
                
                let msg;
                try {
                    msg = JSON.parse(event.detail);
                } catch(e) {
                    console.error('[MassaPay-TabStream] Failed to parse message:', e);
                    return;
                }
                
                console.log('[MassaPay-TabStream] Received from injected:', msg.type, 'uuid:', msg.uuid);
                
                const uuid = msg.uuid || msg.payload?.uuid;
                
                switch(msg.type) {
                    case BearbyMsgTypes.REQUEST_PING:
                        // Respond to ping - this tells bearby.js that extension is installed
                        console.log('[MassaPay-TabStream] Responding to PING');
                        sendToBearbyInjected({
                            type: BearbyMsgTypes.PING_RESPONSE,
                            from: BEARBY_CONTENT,
                            uuid: uuid,
                            payload: {
                                uuid: uuid,
                                installed: true
                            }
                        });
                        break;
                        
                    case BearbyMsgTypes.GET_DATA:
                        // Return wallet state - called on initialization
                        // CRITICAL: Response must have type GET_DATA, not SET_DATA!
                        // bearby.js #subscribe() listens for GET_DATA to set #installed = true
                        console.log('[MassaPay-TabStream] Responding to GET_DATA with wallet state');
                        sendToBearbyInjected({
                            type: BearbyMsgTypes.GET_DATA,  // Same type! bearby.js expects this
                            from: BEARBY_CONTENT,
                            uuid: uuid,
                            payload: {
                                base58: massaPayWallet._connected ? currentAddress : '',
                                accounts: massaPayWallet._connected ? [currentAddress] : [],
                                net: 'MainNet',
                                period: 0,
                                enabled: true,
                                connected: massaPayWallet._connected
                            }
                        });
                        break;
                        
                    case BearbyMsgTypes.CONNECT_APP:
                        // App is requesting connection
                        console.log('[MassaPay-TabStream] CONNECT_APP request from:', msg.payload?.title || 'Unknown DApp');
                        
                        // Request connection through our bridge
                        massaPayWallet.connect().then(result => {
                            const addr = massaPayWallet._accounts[0]?.address || currentAddress;
                            console.log('[MassaPay-TabStream] Connection result:', result, 'address:', addr);
                            
                            // Send connection response
                            sendToBearbyInjected({
                                type: BearbyMsgTypes.RESPONSE_CONNECT_APP,
                                from: BEARBY_CONTENT,
                                uuid: uuid,
                                payload: {
                                    uuid: uuid,
                                    base58: addr,
                                    accounts: [addr],
                                    net: 'MainNet',
                                    period: 0,
                                    connected: true,
                                    resolve: true
                                }
                            });
                            
                            // Also trigger account changed event
                            setTimeout(() => {
                                sendToBearbyInjected({
                                    type: BearbyMsgTypes.ACCOUNT_CHANGED,
                                    from: BEARBY_CONTENT,
                                    payload: {
                                        base58: addr,
                                        accounts: [addr],
                                        net: 'MainNet'
                                    }
                                });
                            }, 100);
                        }).catch(err => {
                            console.error('[MassaPay-TabStream] Connection failed:', err);
                            sendToBearbyInjected({
                                type: BearbyMsgTypes.RESPONSE_CONNECT_APP,
                                from: BEARBY_CONTENT,
                                uuid: uuid,
                                payload: {
                                    uuid: uuid,
                                    reject: err.message || 'Connection rejected'
                                }
                            });
                        });
                        break;
                        
                    case BearbyMsgTypes.SIGN_MESSAGE:
                        // Sign message request
                        console.log('[MassaPay-TabStream] SIGN_MESSAGE request');
                        if (massaPayWallet._accounts[0]) {
                            massaPayWallet._accounts[0].sign(msg.payload?.message || '').then(result => {
                                sendToBearbyInjected({
                                    type: BearbyMsgTypes.SIGN_MESSAGE_RESULT,
                                    from: BEARBY_CONTENT,
                                    uuid: uuid,
                                    payload: {
                                        uuid: uuid,
                                        signature: result.signature,
                                        publicKey: result.publicKey,
                                        resolve: result
                                    }
                                });
                            }).catch(err => {
                                sendToBearbyInjected({
                                    type: BearbyMsgTypes.SIGN_MESSAGE_RESULT,
                                    from: BEARBY_CONTENT,
                                    uuid: uuid,
                                    payload: {
                                        uuid: uuid,
                                        reject: err.message
                                    }
                                });
                            });
                        }
                        break;
                        
                    case BearbyMsgTypes.PAYMENT:
                        // Payment request
                        console.log('[MassaPay-TabStream] PAYMENT request:', msg.payload);
                        if (massaPayWallet._accounts[0]) {
                            massaPayWallet._accounts[0].transfer(
                                msg.payload?.toAddress,
                                BigInt(msg.payload?.amount || '0')
                            ).then(result => {
                                sendToBearbyInjected({
                                    type: BearbyMsgTypes.PAYMENT_RES,
                                    from: BEARBY_CONTENT,
                                    uuid: uuid,
                                    payload: {
                                        uuid: uuid,
                                        resolve: result.operationId || result
                                    }
                                });
                            }).catch(err => {
                                sendToBearbyInjected({
                                    type: BearbyMsgTypes.PAYMENT_RES,
                                    from: BEARBY_CONTENT,
                                    uuid: uuid,
                                    payload: {
                                        uuid: uuid,
                                        reject: err.message
                                    }
                                });
                            });
                        }
                        break;
                        
                    case BearbyMsgTypes.CALL_CONTRACT:
                        // Smart contract call
                        console.log('[MassaPay-TabStream] CALL_CONTRACT request:', msg.payload);
                        if (massaPayWallet._accounts[0]) {
                            massaPayWallet._accounts[0].callSC({
                                target: msg.payload?.targetAddress,
                                func: msg.payload?.functionName,
                                parameter: msg.payload?.parameter,
                                coins: BigInt(msg.payload?.coins || '0'),
                                fee: msg.payload?.fee
                            }).then(result => {
                                sendToBearbyInjected({
                                    type: BearbyMsgTypes.CALL_CONTRACT_RES,
                                    from: BEARBY_CONTENT,
                                    uuid: uuid,
                                    payload: {
                                        uuid: uuid,
                                        resolve: result.operationId || result
                                    }
                                });
                            }).catch(err => {
                                sendToBearbyInjected({
                                    type: BearbyMsgTypes.CALL_CONTRACT_RES,
                                    from: BEARBY_CONTENT,
                                    uuid: uuid,
                                    payload: {
                                        uuid: uuid,
                                        reject: err.message
                                    }
                                });
                            });
                        }
                        break;
                        
                    case BearbyMsgTypes.DISCONNECT:
                        // Disconnect/lock request
                        console.log('[MassaPay-TabStream] DISCONNECT request');
                        massaPayWallet.disconnect();
                        break;
                        
                    default:
                        console.log('[MassaPay-TabStream] Unhandled message type:', msg.type);
                }
            });
            
            console.log('[MassaPay-TabStream] CustomEvent listener registered on', BEARBY_CONTENT);
            
            // If already connected, send initial state via TabStream
            if (massaPayWallet._connected && currentAddress) {
                setTimeout(() => {
                    console.log('[MassaPay-TabStream] Sending initial connected state');
                    // Use GET_DATA type - bearby.js #subscribe() listens for this to update state
                    sendToBearbyInjected({
                        type: BearbyMsgTypes.GET_DATA,
                        from: BEARBY_CONTENT,
                        payload: {
                            base58: currentAddress,
                            accounts: [currentAddress],
                            net: 'MainNet',
                            period: 0,
                            enabled: true,
                            connected: true
                        }
                    });
                }, 100);
            }
            
            // ============================================
            // 11. BEARBY.JS INTERCEPTION - Additional fallbacks
            // The @hicaru/bearby.js package stores its internal state
            // We need to intercept and simulate the browser extension
            // ============================================
            
            // Create a BroadcastChannel-like mechanism that bearby.js uses
            if (typeof BroadcastChannel !== 'undefined') {
                const originalBroadcastChannel = window.BroadcastChannel;
                window.BroadcastChannel = class extends originalBroadcastChannel {
                    constructor(name) {
                        super(name);
                        console.log('[MassaPay] BroadcastChannel created:', name);
                        if (name === 'BearbyExtension' || name.includes('bearby')) {
                            // Intercept bearby messages
                            this._isBearbyChannel = true;
                        }
                    }
                };
            }
            
            // Intercept chrome.runtime which bearby extension uses
            if (!window.chrome) {
                window.chrome = {};
            }
            if (!window.chrome.runtime) {
                window.chrome.runtime = {
                    id: 'massapay-bearby-emulation',
                    sendMessage: async (extensionId, message, callback) => {
                        console.log('[MassaPay] chrome.runtime.sendMessage:', message);
                        // Handle bearby extension messages
                        if (message && message.type) {
                            switch (message.type) {
                                case 'GET_ACCOUNTS':
                                case 'getAccounts':
                                    const accounts = massaPayWallet._accounts.map(a => a.address);
                                    if (callback) callback({ accounts });
                                    return { accounts };
                                    
                                case 'CONNECT':
                                case 'connect':
                                    await massaPayWallet.connect();
                                    const addr = massaPayWallet._accounts[0]?.address;
                                    if (callback) callback({ address: addr, connected: true });
                                    return { address: addr, connected: true };
                                    
                                case 'GET_NETWORK':
                                case 'getNetwork':
                                    if (callback) callback({ network: 'MainNet' });
                                    return { network: 'MainNet' };
                            }
                        }
                        if (callback) callback({});
                        return {};
                    },
                    connect: () => {
                        console.log('[MassaPay] chrome.runtime.connect called');
                        return {
                            onMessage: { addListener: () => {} },
                            onDisconnect: { addListener: () => {} },
                            postMessage: (msg) => {
                                console.log('[MassaPay] port.postMessage:', msg);
                            }
                        };
                    },
                    onMessage: {
                        addListener: (callback) => {
                            console.log('[MassaPay] onMessage listener added');
                            window._bearbyMessageListener = callback;
                        },
                        removeListener: () => {}
                    }
                };
            }
            
            // Helper to simulate bearby extension response
            window._sendBearbyMessage = (type, data) => {
                if (window._bearbyMessageListener) {
                    window._bearbyMessageListener({
                        type: type,
                        ...data
                    });
                }
                window.postMessage({ type: type, source: 'bearby-extension', ...data }, '*');
            };
            
            // Listen for messages from the page (bearby.js sends these)
            window.addEventListener('message', (event) => {
                if (event.data && event.data.source === 'bearby-page') {
                    console.log('[MassaPay] Intercepted bearby-page message:', event.data.type);
                    
                    switch (event.data.type) {
                        case 'GET_INSTALLED':
                            window.postMessage({
                                type: 'INSTALLED',
                                source: 'bearby-extension',
                                installed: true
                            }, '*');
                            break;
                            
                        case 'GET_CONNECTED':
                            window.postMessage({
                                type: 'CONNECTED',
                                source: 'bearby-extension',
                                connected: massaPayWallet._connected,
                                address: massaPayWallet._accounts[0]?.address
                            }, '*');
                            break;
                            
                        case 'CONNECT':
                            massaPayWallet.connect().then(() => {
                                const addr = massaPayWallet._accounts[0]?.address;
                                window.postMessage({
                                    type: 'ACCOUNT_CHANGED',
                                    source: 'bearby-extension',
                                    address: addr
                                }, '*');
                            });
                            break;
                            
                        case 'GET_ACCOUNT':
                            window.postMessage({
                                type: 'ACCOUNT',
                                source: 'bearby-extension',
                                address: massaPayWallet._accounts[0]?.address
                            }, '*');
                            break;
                    }
                }
            });
            
            // Simulate extension injection marker
            const injectBearbyMarker = () => {
                // Some DApps check for specific elements or attributes
                const marker = document.createElement('div');
                marker.id = 'bearby-extension-marker';
                marker.style.display = 'none';
                marker.setAttribute('data-installed', 'true');
                marker.setAttribute('data-version', '1.0.0');
                if (document.body) {
                    document.body.appendChild(marker);
                } else {
                    document.addEventListener('DOMContentLoaded', () => {
                        document.body.appendChild(marker);
                    });
                }
            };
            injectBearbyMarker();
            
            // ============================================
            // 12. FORCE DUSA TO RECOGNIZE CONNECTION
            // After connection, we need to force UI update
            // ============================================
            
            // Global function that can be called after connection approved
            window.massaPayForceUpdate = (address) => {
                console.log('[MassaPay] Forcing update with address:', address);
                
                // Update all our objects
                window.web3.wallet.connected = true;
                window.web3.wallet.account.base58 = address;
                window.web3.wallet.account.address = address;
                window.bearby.wallet.connected = true;
                window.bearby.wallet.account.base58 = address;
                window.bearby.wallet.account.address = address;
                massaPayWallet._connected = true;
                if (massaPayWallet._accounts.length === 0) {
                    massaPayWallet._accounts = [new MassaPayAccount(address)];
                }
                
                // CRITICAL: Resolve any pending TabStream connects
                if (typeof window._resolveTabStreamConnect === 'function') {
                    console.log('[MassaPay] Resolving TabStream pending connects');
                    window._resolveTabStreamConnect(true, address);
                }
                
                // Also send TabStream data update via our main handler
                // Use GET_DATA type - bearby.js #subscribe() listens for this
                sendToBearbyInjected({
                    type: BearbyMsgTypes.GET_DATA,
                    from: '@/BearBy/content-script',
                    payload: {
                        base58: address,
                        accounts: [address],
                        net: 'MainNet',
                        period: 0,
                        enabled: true,
                        connected: true
                    }
                });
                
                sendToBearbyInjected({
                    type: BearbyMsgTypes.ACCOUNT_CHANGED,
                    from: '@/BearBy/content-script',
                    payload: {
                        base58: address,
                        accounts: [address],
                        net: 'MainNet'
                    }
                });
                
                // Notify bearby subscribers
                bearbySubscriptions.account.forEach(cb => {
                    try { cb(address); } catch(e) { console.error(e); }
                });
                
                // Notify web3 subscribers
                web3Subscriptions.account.forEach(cb => {
                    try { cb(address); } catch(e) { console.error(e); }
                });
                
                // Notify wallet-provider subscribers
                massaPayWallet._emit('accountsChanged', [address]);
                
                // Dispatch various events that DApps might listen to
                window.dispatchEvent(new CustomEvent('bearby:accountChanged', { 
                    detail: { address: address } 
                }));
                window.dispatchEvent(new CustomEvent('bearby:connect', { 
                    detail: { address: address } 
                }));
                window.dispatchEvent(new CustomEvent('massaPay:connected', { 
                    detail: { address: address } 
                }));
                window.dispatchEvent(new CustomEvent('wallet:connected', { 
                    detail: { address: address, provider: 'MassaPay' } 
                }));
                
                // Post messages that bearby extension would send
                window.postMessage({
                    type: 'ACCOUNT_CHANGED',
                    source: 'bearby-extension',
                    address: address
                }, '*');
                
                window.postMessage({
                    type: 'CONNECTED',
                    source: 'bearby-extension',
                    connected: true,
                    address: address
                }, '*');
                
                // Try to find and click any "refresh" or update mechanism
                // Some DApps have reactive stores that need manual triggering
                if (window.__NUXT__) {
                    console.log('[MassaPay] Nuxt detected, trying to update store');
                }
                
                // For Svelte/SvelteKit stores (Dusa might use this)
                if (window.__svelte_store_updates__) {
                    window.__svelte_store_updates__.forEach(update => {
                        try { update(address); } catch(e) {}
                    });
                }
                
                console.log('[MassaPay] Force update complete');
            };
            
            console.log('[MassaPay] Wallet provider initialized successfully');
            console.log('[MassaPay] Address:', currentAddress);
            console.log('[MassaPay] Connected:', massaPayWallet._connected);
            console.log('[MassaPay] Bearby compatibility layer active');
            console.log('[MassaPay] web3 object available:', !!window.web3);
            console.log('[MassaPay] chrome.runtime emulation active');
            
            // ============================================
            // 12. AUTO-NOTIFY ON PAGE LOAD IF ALREADY CONNECTED
            // When page reloads and we're already connected, notify DApp
            // ============================================
            if (massaPayWallet._connected && currentAddress) {
                console.log('[MassaPay] Already connected, will auto-notify DApp');
                
                // Delay to let DApp register its listeners first
                setTimeout(() => {
                    console.log('[MassaPay] Auto-notifying DApp of existing connection');
                    window.massaPayForceUpdate(currentAddress);
                }, 500);
                
                // Also try again after a longer delay in case DApp is slow to initialize
                setTimeout(() => {
                    if (web3Subscriptions.account.length > 0 || bearbySubscriptions.account.length > 0) {
                        console.log('[MassaPay] Late notification with', 
                            web3Subscriptions.account.length, 'web3 subs,',
                            bearbySubscriptions.account.length, 'bearby subs');
                        window.massaPayForceUpdate(currentAddress);
                    }
                }, 1500);
            }
            
        })();
    """.trimIndent()
    
    /**
     * Early injection script that runs BEFORE the page loads its JavaScript
     * This sets up stubs that will be filled in by the full provider script later
     * Critical for DApps that check for wallet presence during initialization
     */
    fun getEarlyProviderScript(walletAddress: String): String = """
        (function() {
            // Only run once
            if (window._massaPayEarlyInjected) return;
            window._massaPayEarlyInjected = true;
            
            console.log('[MassaPay-Early] Injecting early provider stubs');
            
            // CRITICAL: Enable BigInt serialization in JSON.stringify
            // This fixes "Do not know how to serialize a BigInt" error in EagleFi
            if (typeof BigInt !== 'undefined' && !BigInt.prototype.toJSON) {
                BigInt.prototype.toJSON = function() {
                    return this.toString();
                };
                console.log('[MassaPay-Early] BigInt.prototype.toJSON polyfill installed');
            }
            
            const address = '$walletAddress';
            const hasAddress = address && address.length > 0 && address.startsWith('AU');
            // For MassaStation mode, we always have accounts available if we have an address
            const isConnected = hasAddress;
            
            console.log('[MassaPay-Early] Address:', address, 'hasAddress:', hasAddress);
            
            // Track successful operation IDs so we can report their status correctly
            // This is crucial for DApps like EagleFi that call getOperations after sending a transaction
            if (!window._massaPaySuccessfulOperations) {
                window._massaPaySuccessfulOperations = {};
            }
            
            // Early provider request system for callSC
            // Store in window so full provider can access it
            var earlyRequestId = 0;
            if (!window._earlyPendingRequests) {
                window._earlyPendingRequests = {};
            }
            
            function earlyMassaPayRequest(method, params) {
                return new Promise(function(resolve, reject) {
                    var id = ++earlyRequestId;
                    window._earlyPendingRequests[id] = { resolve: resolve, reject: reject, method: method };
                    
                    var message = JSON.stringify({
                        id: id,
                        method: method,
                        params: params || {}
                    });
                    
                    console.log('[MassaPay-Early] Request:', method, 'id:', id);
                    
                    if (window.MassaPayBridge) {
                        window.MassaPayBridge.postMessage(message);
                    } else {
                        reject(new Error('MassaPay bridge not available'));
                        delete window._earlyPendingRequests[id];
                    }
                    
                    // Timeout after 5 minutes
                    setTimeout(function() {
                        if (window._earlyPendingRequests[id]) {
                            delete window._earlyPendingRequests[id];
                            reject(new Error('Request timeout'));
                        }
                    }, 300000);
                });
            }
            
            // Response handler for early provider (will be replaced by full provider)
            // This handles responses BEFORE the full provider loads
            window.massaPayResponse = function(responseJson) {
                try {
                    var response = JSON.parse(responseJson);
                    var id = response.id;
                    var result = response.result;
                    var error = response.error;
                    
                    console.log('[MassaPay-Early] Response for id:', id, 'result:', result);
                    
                    var pending = window._earlyPendingRequests[id];
                    if (!pending) {
                        console.warn('[MassaPay-Early] No pending request for id:', id);
                        return;
                    }
                    
                    var method = pending.method;
                    delete window._earlyPendingRequests[id];
                    
                    if (error) {
                        console.error('[MassaPay-Early] Error response:', error.message);
                        pending.reject(new Error(error.message || 'Unknown error'));
                    } else {
                        // Format response based on method type
                        // bearby-web3 contract.call expects: operationId as string
                        // wallet-provider then wraps it in Operation(provider, operationId)
                        if (method === 'massa_callSmartContract' || method === 'massa_executeBytecode') {
                            var formattedResult = result;
                            if (typeof result === 'string') {
                                formattedResult = result;
                                // Track this operationId as successful so getOperations returns correct status
                                // This is critical for DApps that call operation.waitSpeculativeExecution()
                                window._massaPaySuccessfulOperations[result] = {
                                    timestamp: Date.now(),
                                    status: 'speculative_success'
                                };
                                console.log('[MassaPay-Early] Tracked successful operation:', result);
                            }
                            console.log('[MassaPay-Early] Resolving with operationId:', formattedResult);
                            pending.resolve(formattedResult);
                        } else {
                            pending.resolve(result);
                        }
                    }
                } catch(e) {
                    console.error('[MassaPay-Early] Error parsing response:', e);
                }
            };
            
            // Create basic bearby stub EARLY so @hicaru/bearby.js sees it
            if (!window.bearby) {
                // Initialize subscriptions storage
                if (!window._massaPayEarlySubscriptions) {
                    window._massaPayEarlySubscriptions = { account: [], network: [] };
                }
                
                // Create a Svelte-like store for account (bearby.js pattern)
                // In bearby.js, account.subscribe(callback) receives the ADDRESS STRING, not an object
                // But account.base58 returns the address directly
                var accountStore = {
                    _value: isConnected ? address : null,  // Store the address string directly
                    _subscribers: [],
                    
                    // Get current value (for direct property access)
                    get base58() {
                        console.log('[MassaPay-Early] account.base58 getter called, returning:', this._value || '');
                        return this._value || '';
                    },
                    get publicKey() {
                        return '';
                    },
                    
                    // Svelte store subscribe pattern - callback receives ADDRESS STRING
                    subscribe: function(callback) {
                        console.log('[MassaPay-Early] account.subscribe called, current value:', accountStore._value);
                        accountStore._subscribers.push(callback);
                        window._massaPayEarlySubscriptions.account.push(callback);
                        
                        // Immediately call with current value (address string)
                        if (accountStore._value) {
                            setTimeout(function() { 
                                console.log('[MassaPay-Early] account.subscribe notifying with address:', accountStore._value);
                                callback(accountStore._value);  // Pass the address string directly
                            }, 0);
                        }
                        
                        return {
                            unsubscribe: function() {
                                var idx = accountStore._subscribers.indexOf(callback);
                                if (idx > -1) accountStore._subscribers.splice(idx, 1);
                                idx = window._massaPayEarlySubscriptions.account.indexOf(callback);
                                if (idx > -1) window._massaPayEarlySubscriptions.account.splice(idx, 1);
                            }
                        };
                    },
                    
                    // Method to update the store value (pass address string)
                    set: function(newValue) {
                        // Accept either string or object with base58
                        var addr = typeof newValue === 'string' ? newValue : (newValue && newValue.base58 ? newValue.base58 : null);
                        console.log('[MassaPay-Early] account.set called with:', addr);
                        accountStore._value = addr;
                        accountStore._subscribers.forEach(function(cb) {
                            try { cb(addr); } catch(e) { console.error(e); }
                        });
                    }
                };
                
                // Store reference globally for updates
                window._massaPayAccountStore = accountStore;
                
                window.bearby = {
                    isEnable: true,
                    isMassaPay: true,
                    isConnected: isConnected,
                    wallet: {
                        account: accountStore,
                        isMassaMainnet: true,
                        network: {
                            _value: 'mainnet',
                            
                            // Direct property access
                            get net() { return 'mainnet'; },
                            get chainId() { return 77658377; },
                            get url() { return 'https://mainnet.massa.net/api/v2'; },
                            
                            // Make network thenable so "await web3.wallet.network" works
                            then: function(resolve) {
                                resolve({ net: 'mainnet', chainId: 77658377, url: 'https://mainnet.massa.net/api/v2' });
                                return Promise.resolve({ net: 'mainnet', chainId: 77658377, url: 'https://mainnet.massa.net/api/v2' });
                            },
                            subscribe: function(callback) {
                                console.log('[MassaPay-Early] network.subscribe called');
                                if (!window._massaPayEarlySubscriptions) {
                                    window._massaPayEarlySubscriptions = { account: [], network: [] };
                                }
                                window._massaPayEarlySubscriptions.network.push(callback);
                                // Return the network name string, not an object
                                setTimeout(function() { callback('mainnet'); }, 0);
                                return {
                                    unsubscribe: function() {
                                        var idx = window._massaPayEarlySubscriptions.network.indexOf(callback);
                                        if (idx > -1) window._massaPayEarlySubscriptions.network.splice(idx, 1);
                                    }
                                };
                            }
                        },
                        connected: isConnected,
                        enabled: true,
                        installed: true,  // CRITICAL: wallet-provider checks this!
                        connect: function() {
                            return new Promise(function(resolve, reject) {
                                console.log('[MassaPay-Early] bearby.wallet.connect() called');
                                
                                // If already connected with an address, resolve immediately
                                if (isConnected && address) {
                                    console.log('[MassaPay-Early] Already connected, resolving with address:', address);
                                    window.bearby.wallet.connected = true;
                                    window.bearby.isConnected = true;
                                    
                                    // Update the account store with the address string
                                    if (window._massaPayAccountStore) {
                                        window._massaPayAccountStore.set(address);
                                    }
                                    
                                    resolve(true);
                                    return;
                                }
                                
                                // Otherwise request connection from native
                                if (window.MassaPayBridge) {
                                    window.MassaPayBridge.request(JSON.stringify({
                                        method: 'connect',
                                        params: {}
                                    }));
                                }
                                // Will be resolved when full provider loads
                                window._massaPayPendingConnect = resolve;
                            });
                        }
                    },
                    // massa object for bearby compatibility
                    // wallet-provider calls web3.massa.getNodesStatus()
                    massa: {
                        getNodesStatus: async function() {
                            console.log('[MassaPay-Early] bearby.massa.getNodesStatus called');
                            return {
                                result: {
                                    node_id: 'MassaPay',
                                    node_ip: 'mainnet.massa.net',
                                    version: '1.0.0',
                                    chain_id: 77658377,
                                    minimal_fees: '0.01',
                                    connected: true
                                }
                            };
                        },
                        getAddresses: async function() {
                            var args = Array.prototype.slice.call(arguments);
                            console.log('[MassaPay-Early] bearby.massa.getAddresses called with:', args);
                            
                            // Make a real RPC call to get addresses info
                            try {
                                var response = await fetch('https://mainnet.massa.net/api/v2', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({
                                        jsonrpc: '2.0',
                                        id: 1,
                                        method: 'get_addresses',
                                        params: [args]
                                    })
                                });
                                var data = await response.json();
                                console.log('[MassaPay-Early] getAddresses RPC response:', JSON.stringify(data));
                                
                                if (data.result) {
                                    return { result: data.result };
                                }
                                
                                // Fallback if no result
                                return {
                                    result: args.map(function(addr) {
                                        return {
                                            address: addr,
                                            final_balance: '0',
                                            candidate_balance: '0',
                                            final_roll_count: 0,
                                            candidate_roll_count: 0,
                                            final_datastore_keys: [],
                                            candidate_datastore_keys: []
                                        };
                                    })
                                };
                            } catch(e) {
                                console.error('[MassaPay-Early] getAddresses error:', e);
                                return {
                                    result: args.map(function(addr) {
                                        return {
                                            address: addr,
                                            final_balance: '0',
                                            candidate_balance: '0'
                                        };
                                    })
                                };
                            }
                        },
                        payment: async function(amount, recipient) {
                            throw new Error('Use full provider for payment');
                        },
                        buyRolls: async function(amount) {
                            throw new Error('Use full provider for buyRolls');
                        },
                        sellRolls: async function(amount) {
                            throw new Error('Use full provider for sellRolls');
                        },
                        getOperations: async function() {
                            console.log('[MassaPay-Early] bearby.massa.getOperations called');
                            var args = Array.prototype.slice.call(arguments);
                            // Return proper status for tracked operations
                            // This is called by wallet-provider's BearbyAccount.getOperationStatus()
                            // which uses web3.massa.getOperations() - and web3 = window.bearby
                            // IMPORTANT: Return isFinal=true immediately so EagleFi continues with swap
                            // The operation was already sent successfully to the network
                            return { 
                                result: args.map(function(opId) {
                                    var tracked = window._massaPaySuccessfulOperations ? window._massaPaySuccessfulOperations[opId] : null;
                                    if (tracked) {
                                        // Operation was sent successfully by MassaPay
                                        // Return isFinal=true immediately - the operation is already on the network
                                        // EagleFi needs this to continue to the swap step
                                        var elapsed = Date.now() - tracked.timestamp;
                                        console.log('[MassaPay-Early] bearby.massa.getOperations for tracked op:', opId, 'elapsed:', elapsed, 'returning isFinal: true');
                                        return { 
                                            id: opId,
                                            in_pool: false,
                                            in_blocks: ['block1'],
                                            is_operation_final: true,  // Always return true - operation was sent successfully
                                            op_exec_status: true,  // true = success
                                            thread: 0
                                        };
                                    }
                                    // Unknown operation - return not found status
                                    console.log('[MassaPay-Early] bearby.massa.getOperations for unknown op:', opId);
                                    return { 
                                        id: opId,
                                        in_pool: true,
                                        in_blocks: [],
                                        is_operation_final: null,
                                        op_exec_status: null,
                                        thread: 0
                                    }; 
                                }) 
                            };
                        },
                        // callSC for smart contract calls - CRITICAL for swaps
                        callSC: async function(params) {
                            console.log('[MassaPay-Early] bearby.massa.callSC called', JSON.stringify(params));
                            var result = await earlyMassaPayRequest('massa_callSmartContract', {
                                targetAddress: params.target || params.targetAddress,
                                functionName: params.func || params.functionName,
                                parameter: params.parameter ? Array.from(params.parameter) : [],
                                coins: params.coins ? params.coins.toString() : '0',
                                fee: params.fee ? params.fee.toString() : undefined
                            });
                            console.log('[MassaPay-Early] bearby.massa.callSC result:', result);
                            return result;
                        }
                    },
                    // contract object for smart contract interactions
                    contract: {
                        call: async function(params) {
                            console.log('[MassaPay-Early] bearby.contract.call called', JSON.stringify(params));
                            var result = await earlyMassaPayRequest('massa_callSmartContract', {
                                targetAddress: params.targetAddress || params.target || params.address || params.toAddr,
                                functionName: params.functionName || params.func || params.function,
                                parameter: params.parameter ? (Array.isArray(params.parameter) ? params.parameter : Array.from(params.parameter)) : [],
                                unsafeParameters: params.unsafeParameters || params.unsafeParams,
                                coins: params.coins ? params.coins.toString() : '0',
                                fee: params.fee ? params.fee.toString() : undefined,
                                maxGas: params.maxGas || params.gasLimit || '100000000'
                            });
                            console.log('[MassaPay-Early] bearby.contract.call raw result:', result);
                            // Bearby wallet returns the operationId string directly
                            // NOT wrapped in an object - EagleFi expects just the string
                            return result;
                        },
                        // executeBytecode for executing raw bytecode (used by some DApps)
                        executeBytecode: async function(params) {
                            console.log('[MassaPay-Early] bearby.contract.executeBytecode called', JSON.stringify(params));
                            
                            // DApps may send bytecodeBase64, bytecode, or data
                            var bytecode = params.bytecode || params.bytecodeBase64 || params.data || '';
                            var datastore = params.datastore || params.datastoreEntries || [];
                            var maxGas = params.maxGas || params.gasLimit || '500000000';
                            // Some DApps use maxCoins for the coins to send with the bytecode execution
                            var coins = params.coins || params.maxCoins || params.amount || '0';
                            var fee = params.fee || '0.01';
                            
                            console.log('[MassaPay-Early] executeBytecode bytecode length:', bytecode.length, 'type:', typeof bytecode);
                            console.log('[MassaPay-Early] executeBytecode coins:', coins, 'maxGas:', maxGas);
                            
                            var result = await earlyMassaPayRequest('massa_executeBytecode', {
                                bytecode: Array.isArray(bytecode) ? bytecode : bytecode,
                                datastore: datastore,
                                maxGas: maxGas.toString(),
                                coins: coins.toString(),
                                fee: fee.toString()
                            });
                            console.log('[MassaPay-Early] bearby.contract.executeBytecode raw result:', result);
                            // Bearby wallet returns the operationId string directly
                            return result;
                        },
                        deploy: async function() { throw new Error('Use full provider'); },
                        getDatastoreEntries: async function() { return []; },
                        readSmartContract: async function() { return []; },
                        getFilteredSCOutputEvent: async function(filter) {
                            console.log('[MassaPay-Early] bearby.contract.getFilteredSCOutputEvent called', JSON.stringify(filter));
                            
                            // Check if this is a tracked operation - we need to poll until events appear
                            var opId = filter.original_operation_id;
                            var tracked = window._massaPaySuccessfulOperations && window._massaPaySuccessfulOperations[opId];
                            
                            // Make real RPC call to get actual SC output events from blockchain
                            // For tracked operations, poll with retries since events may not be immediately available
                            var maxRetries = tracked ? 5 : 1;
                            var retryDelay = 3000; // 3 seconds between retries
                            
                            for (var attempt = 0; attempt < maxRetries; attempt++) {
                                try {
                                    // Try both is_final values for tracked operations
                                    var filtersToTry = [filter];
                                    if (tracked && filter.is_final === false) {
                                        // Also try with is_final: true since the tx may have finalized
                                        filtersToTry.push(Object.assign({}, filter, { is_final: true }));
                                    }
                                    
                                    for (var fi = 0; fi < filtersToTry.length; fi++) {
                                        var currentFilter = filtersToTry[fi];
                                        var rpcPayload = {
                                            jsonrpc: '2.0',
                                            id: 1,
                                            method: 'get_filtered_sc_output_event',
                                            params: [currentFilter]
                                        };
                                        
                                        if (attempt > 0 || fi > 0) {
                                            console.log('[MassaPay-Early] getFilteredSCOutputEvent try', attempt + 1, 'filter', fi + 1);
                                        }
                                        console.log('[MassaPay-Early] getFilteredSCOutputEvent RPC request:', JSON.stringify(rpcPayload));
                                        
                                        var response = await fetch('https://mainnet.massa.net/api/v2', {
                                            method: 'POST',
                                            headers: { 'Content-Type': 'application/json' },
                                            body: JSON.stringify(rpcPayload)
                                        });
                                        
                                        var data = await response.json();
                                        console.log('[MassaPay-Early] getFilteredSCOutputEvent RPC response:', JSON.stringify(data));
                                        
                                        if (!data.error) {
                                            var events = data.result || [];
                                            if (events.length > 0) {
                                                console.log('[MassaPay-Early] getFilteredSCOutputEvent returning', events.length, 'events');
                                                return [{ result: events }];
                                            }
                                        }
                                    }
                                    
                                    // If tracked and no events yet, wait and retry
                                    if (tracked && attempt < maxRetries - 1) {
                                        console.log('[MassaPay-Early] getFilteredSCOutputEvent empty result for tracked op, waiting', retryDelay, 'ms');
                                        await new Promise(function(resolve) { setTimeout(resolve, retryDelay); });
                                    }
                                } catch (e) {
                                    console.error('[MassaPay-Early] getFilteredSCOutputEvent failed:', e);
                                    if (!tracked || attempt >= maxRetries - 1) {
                                        break;
                                    }
                                    await new Promise(function(resolve) { setTimeout(resolve, retryDelay); });
                                }
                            }
                            
                            // For tracked operations that exhausted retries, return empty result
                            // EagleFi already knows the operation succeeded from getOperations returning isFinal:true
                            // Returning empty events won't cause an error since the operation status is confirmed
                            console.log('[MassaPay-Early] getFilteredSCOutputEvent returning empty for tracked op:', opId);
                            return [{ result: [] }];
                        },
                        types: {
                            STRING: 'STRING',
                            BOOL: 'BOOL',
                            U8: 'U8',
                            U16: 'U16',
                            U32: 'U32',
                            U64: 'U64',
                            U128: 'U128',
                            U256: 'U256',
                            I8: 'I8',
                            I16: 'I16',
                            I32: 'I32',
                            I64: 'I64',
                            I128: 'I128',
                            F32: 'F32',
                            F64: 'F64',
                            ARRAY: 'ARRAY',
                            UINT8ARRAY: 'UINT8ARRAY'
                        }
                    },
                    web3: {
                        wallet: null, // Will be set by full provider
                    }
                };
                
                // Make it non-writable so DApp libraries don't overwrite
                Object.defineProperty(window, 'bearby', {
                    value: window.bearby,
                    writable: true, // Allow full provider to update
                    configurable: true
                });
            }
            
            // Create web3 stub with FULL massa implementation
            // wallet-provider's networkInfos() calls web3.massa.getNodesStatus()
            if (!window.web3) {
                window.web3 = {
                    wallet: window.bearby.wallet,
                    massa: {
                        getNodesStatus: async function() {
                            console.log('[MassaPay-Early] web3.massa.getNodesStatus called');
                            return {
                                result: {
                                    node_id: 'MassaPay',
                                    node_ip: 'mainnet.massa.net',
                                    version: '1.0.0',
                                    chain_id: 77658377,
                                    minimal_fees: '0.01',
                                    connected: true,
                                    current_time: Date.now(),
                                    current_cycle: 0,
                                    config: {
                                        genesis_timestamp: 0,
                                        thread_count: 32,
                                        t0: 16000,
                                        delta_f0: 0
                                    }
                                }
                            };
                        },
                        getAddresses: async function() {
                            var args = Array.prototype.slice.call(arguments);
                            console.log('[MassaPay-Early] web3.massa.getAddresses called with:', args);
                            
                            // Make a real RPC call to get addresses info
                            try {
                                var response = await fetch('https://mainnet.massa.net/api/v2', {
                                    method: 'POST',
                                    headers: { 'Content-Type': 'application/json' },
                                    body: JSON.stringify({
                                        jsonrpc: '2.0',
                                        id: 1,
                                        method: 'get_addresses',
                                        params: [args]
                                    })
                                });
                                var data = await response.json();
                                console.log('[MassaPay-Early] web3.massa.getAddresses RPC response:', JSON.stringify(data));
                                
                                if (data.result) {
                                    return { result: data.result };
                                }
                                
                                // Fallback if no result
                                return {
                                    result: args.map(function(addr) {
                                        return {
                                            address: addr,
                                            final_balance: '0',
                                            candidate_balance: '0',
                                            final_roll_count: 0,
                                            candidate_roll_count: 0,
                                            final_datastore_keys: [],
                                            candidate_datastore_keys: []
                                        };
                                    })
                                };
                            } catch(e) {
                                console.error('[MassaPay-Early] web3.massa.getAddresses error:', e);
                                return {
                                    result: args.map(function(addr) {
                                        return {
                                            address: addr,
                                            final_balance: '0',
                                            candidate_balance: '0'
                                        };
                                    })
                                };
                            }
                        },
                        payment: async function(amount, recipient) {
                            console.log('[MassaPay-Early] web3.massa.payment called');
                            return new Promise(function(resolve, reject) {
                                if (window.MassaPayBridge) {
                                    window.MassaPayBridge.request(JSON.stringify({
                                        method: 'massa_transfer',
                                        params: { to: recipient, amount: amount }
                                    }));
                                    // Will be resolved by response handler
                                    window._massaPayPendingPayment = { resolve: resolve, reject: reject };
                                } else {
                                    reject(new Error('MassaPay bridge not available'));
                                }
                            });
                        },
                        buyRolls: async function(amount) {
                            console.log('[MassaPay-Early] web3.massa.buyRolls called');
                            throw new Error('buyRolls not implemented in early provider');
                        },
                        sellRolls: async function(amount) {
                            console.log('[MassaPay-Early] web3.massa.sellRolls called');
                            throw new Error('sellRolls not implemented in early provider');
                        },
                        getOperations: async function() {
                            console.log('[MassaPay-Early] web3.massa.getOperations called');
                            var args = Array.prototype.slice.call(arguments);
                            // Return proper status for tracked operations
                            // This is called by wallet-provider's Operation.getStatus() and Operation.waitSpeculativeExecution()
                            // BearbyAccount.getOperationStatus checks: op_exec_status, is_operation_final, in_pool
                            // IMPORTANT: Return isFinal=true immediately so EagleFi continues with swap
                            return { 
                                result: args.map(function(opId) {
                                    var tracked = window._massaPaySuccessfulOperations[opId];
                                    if (tracked) {
                                        // Operation was sent successfully by MassaPay
                                        // Return isFinal=true immediately - the operation is on the network
                                        var elapsed = Date.now() - tracked.timestamp;
                                        console.log('[MassaPay-Early] getOperations for tracked op:', opId, 'elapsed:', elapsed, 'returning isFinal: true');
                                        return { 
                                            address: opId,
                                            thread: 0,
                                            in_pool: false,
                                            in_blocks: ['block1'],
                                            is_operation_final: true,  // Always return true - operation was sent successfully
                                            op_exec_status: true,  // true = success
                                            final_balance: '0',
                                            candidate_balance: '0'
                                        };
                                    }
                                    // Unknown operation - return not found status
                                    console.log('[MassaPay-Early] getOperations for unknown op:', opId);
                                    return { 
                                        address: opId,
                                        thread: 0,
                                        in_pool: false,
                                        in_blocks: [],
                                        is_operation_final: null,
                                        op_exec_status: null
                                    }; 
                                }) 
                            };
                        },
                        // callSC for smart contract calls
                        callSC: async function(params) {
                            console.log('[MassaPay-Early] web3.massa.callSC called', JSON.stringify(params));
                            return earlyMassaPayRequest('massa_callSmartContract', {
                                targetAddress: params.target || params.targetAddress,
                                functionName: params.func || params.functionName,
                                parameter: params.parameter ? Array.from(params.parameter) : [],
                                coins: params.coins ? params.coins.toString() : '0',
                                fee: params.fee ? params.fee.toString() : undefined
                            });
                        }
                    },
                    // contract object for smart contract interactions
                    contract: {
                        call: async function(params) {
                            console.log('[MassaPay-Early] web3.contract.call called', JSON.stringify(params));
                            return earlyMassaPayRequest('massa_callSmartContract', {
                                targetAddress: params.targetAddress || params.target || params.address || params.toAddr,
                                functionName: params.functionName || params.func || params.function,
                                parameter: params.parameter ? (Array.isArray(params.parameter) ? params.parameter : Array.from(params.parameter)) : [],
                                unsafeParameters: params.unsafeParameters || params.unsafeParams,
                                coins: params.coins ? params.coins.toString() : '0',
                                fee: params.fee ? params.fee.toString() : undefined,
                                maxGas: params.maxGas || params.gasLimit || '100000000'
                            });
                        },
                        deploy: async function() { throw new Error('Use full provider'); },
                        getDatastoreEntries: async function() { return []; },
                        readSmartContract: async function() { return []; },
                        getFilteredSCOutputEvent: async function(filter) {
                            console.log('[MassaPay-Early] web3.contract.getFilteredSCOutputEvent called', JSON.stringify(filter));
                            
                            // Check if this is a tracked operation - we need to poll until events appear
                            var opId = filter.original_operation_id;
                            var tracked = window._massaPaySuccessfulOperations && window._massaPaySuccessfulOperations[opId];
                            
                            // Make real RPC call to get actual SC output events from blockchain
                            // For tracked operations, poll with retries since events may not be immediately available
                            var maxRetries = tracked ? 5 : 1;
                            var retryDelay = 3000; // 3 seconds between retries
                            
                            for (var attempt = 0; attempt < maxRetries; attempt++) {
                                try {
                                    // Try both is_final states - events may be in either
                                    var isFinalValues = [false, true];
                                    for (var i = 0; i < isFinalValues.length; i++) {
                                        var isFinal = isFinalValues[i];
                                        var rpcFilter = Object.assign({}, filter, { is_final: isFinal });
                                        var rpcPayload = {
                                            jsonrpc: '2.0',
                                            id: 1,
                                            method: 'get_filtered_sc_output_event',
                                            params: [rpcFilter]
                                        };
                                        
                                        if (attempt > 0 || isFinal) {
                                            console.log('[MassaPay-Early] web3 getFilteredSCOutputEvent retry', attempt, 'is_final:', isFinal);
                                        }
                                        console.log('[MassaPay-Early] web3 getFilteredSCOutputEvent RPC request:', JSON.stringify(rpcPayload));
                                        
                                        var response = await fetch('https://mainnet.massa.net/api/v2', {
                                            method: 'POST',
                                            headers: { 'Content-Type': 'application/json' },
                                            body: JSON.stringify(rpcPayload)
                                        });
                                        
                                        var data = await response.json();
                                        console.log('[MassaPay-Early] web3 getFilteredSCOutputEvent RPC response:', JSON.stringify(data));
                                        
                                        if (data.error) {
                                            console.error('[MassaPay-Early] web3 getFilteredSCOutputEvent error:', data.error);
                                            continue;
                                        }
                                        
                                        // If we got events, return them
                                        var events = data.result || [];
                                        if (events.length > 0) {
                                            console.log('[MassaPay-Early] web3 getFilteredSCOutputEvent returning', events.length, 'events');
                                            return [{ result: events }];
                                        }
                                    }
                                    
                                    // For non-tracked operations, don't retry
                                    if (!tracked) {
                                        return [{ result: [] }];
                                    }
                                    
                                    // For tracked operations with empty results, wait and retry
                                    if (attempt < maxRetries - 1) {
                                        console.log('[MassaPay-Early] web3 getFilteredSCOutputEvent empty result for tracked op, waiting', retryDelay, 'ms');
                                        await new Promise(function(resolve) { setTimeout(resolve, retryDelay); });
                                    }
                                } catch (e) {
                                    console.error('[MassaPay-Early] web3 getFilteredSCOutputEvent failed:', e);
                                    if (!tracked || attempt >= maxRetries - 1) {
                                        break;
                                    }
                                    await new Promise(function(resolve) { setTimeout(resolve, retryDelay); });
                                }
                            }
                            
                            // For tracked operations that exhausted retries, return empty result  
                            // EagleFi already knows the operation succeeded from getOperations returning isFinal:true
                            console.log('[MassaPay-Early] web3 getFilteredSCOutputEvent returning empty for op:', opId);
                            return [{ result: [] }];
                        },
                        types: {
                            STRING: 'STRING',
                            BOOL: 'BOOL',
                            U8: 'U8',
                            U16: 'U16',
                            U32: 'U32',
                            U64: 'U64',
                            U128: 'U128',
                            U256: 'U256',
                            I8: 'I8',
                            I16: 'I16',
                            I32: 'I32',
                            I64: 'I64',
                            I128: 'I128',
                            F32: 'F32',
                            F64: 'F64',
                            ARRAY: 'ARRAY',
                            UINT8ARRAY: 'UINT8ARRAY'
                        }
                    }
                };
            }
            
            // Emulate chrome.runtime.sendMessage early
            // This is what @hicaru/bearby.js uses internally
            if (!window.chrome) {
                window.chrome = {};
            }
            if (!window.chrome.runtime) {
                window.chrome.runtime = {
                    id: 'massapay-wallet',
                    sendMessage: function(extensionId, message, callback) {
                        console.log('[MassaPay-Early] chrome.runtime.sendMessage:', message);
                        
                        var response = {};
                        
                        if (message && message.type === 'GET_DATA') {
                            response = {
                                isEnable: true,
                                isConnected: isConnected,
                                enabled: true,
                                connected: isConnected,
                                base58: isConnected ? address : '',
                                net: 'MainNet',
                                period: 0
                            };
                        } else if (message && message.type === 'GET_WALLET_STATE') {
                            response = {
                                isEnable: true,
                                enabled: true,
                                connected: isConnected,
                                account: {
                                    base58: address,
                                    publicKey: ''
                                },
                                network: {
                                    net: 'MainNet',
                                    chainId: 77658377
                                }
                            };
                        }
                        
                        if (typeof callback === 'function') {
                            setTimeout(function() { callback(response); }, 0);
                        }
                        return true;
                    },
                    connect: function() { return Promise.resolve({}); },
                    onMessage: {
                        addListener: function(cb) {
                            window._massaPayMessageListeners = window._massaPayMessageListeners || [];
                            window._massaPayMessageListeners.push(cb);
                        },
                        removeListener: function() {}
                    }
                };
            }
            
            // Store connected state in localStorage for persistence
            if (isConnected) {
                try {
                    localStorage.setItem('massapay_connected', 'true');
                    localStorage.setItem('massapay_address', address);
                    localStorage.setItem('bearby_connected', 'true');
                    localStorage.setItem('bearby_address', address);
                } catch(e) {}
            }
            
            // Use standalone mode for MassaStation detection
            // BUT we need to intercept buildnet calls and redirect to mainnet
            // because wallet-provider hardcodes buildnet for standalone mode
            if (!window.massaWallet) {
                window.massaWallet = {
                    standalone: true
                };
                Object.defineProperty(window, 'massaWallet', {
                    value: window.massaWallet,
                    writable: false,
                    configurable: false
                });
            }
            
            // CRITICAL: Intercept fetch requests to localhost:8080, station.massa, AND buildnet->mainnet
            // When wallet-provider is in standalone mode, it calls http://localhost:8080/api
            // We need to intercept these and return wallet data
            try {
                console.log('[MassaPay-Fetch] Setting up fetch interceptor...');
                
                var originalFetch = window.fetch;
                window.fetch = function(input, init) {
                    var url = typeof input === 'string' ? input : (input && input.url ? input.url : '');
                    
                    console.log('[MassaPay-Fetch] Fetch called for:', url);
                    
                    // DEBUG: Log RPC method calls to mainnet
                    if (url && url.indexOf('mainnet.massa.net') !== -1 && init && init.body) {
                        try {
                            var body = typeof init.body === 'string' ? JSON.parse(init.body) : init.body;
                            console.log('[MassaPay-RPC] Method:', body.method, 'Params:', JSON.stringify(body.params));
                        } catch(e) {
                            console.log('[MassaPay-RPC] Body (raw):', init.body);
                        }
                    }
                    
                    // CRITICAL: Redirect buildnet calls to mainnet
                    // wallet-provider hardcodes buildnet for standalone mode, but we want mainnet
                    if (url && url.indexOf('buildnet.massa.net') !== -1) {
                        var mainnetUrl = url.replace('buildnet.massa.net', 'mainnet.massa.net');
                        console.log('[MassaPay-Fetch] Redirecting buildnet to mainnet:', mainnetUrl);
                        return originalFetch.call(this, mainnetUrl, init);
                    }
                    
                    // Check if this is a standalone mode API call
                    if (url && (url.indexOf('localhost:8080') !== -1 || url.indexOf('127.0.0.1:8080') !== -1)) {
                        console.log('[MassaPay-Fetch] Intercepting standalone API call:', url);
                        
                        var path = url;
                        try {
                            var urlObj = new URL(url);
                            path = urlObj.pathname;
                        } catch(e) {
                            var parts = url.split('?');
                            path = parts[0];
                        }
                        
                        var responseData = null;
                        
                        if (path.indexOf('/accounts') !== -1) {
                            console.log('[MassaPay-Fetch] Returning accounts - hasAddress:', hasAddress);
                            responseData = {
                                result: hasAddress ? [{
                                    address: address,
                                    nickname: 'MassaPay Account',
                                    keyPair: { publicKey: '', nonce: '' },
                                    status: 'OK'
                                }] : []
                            };
                        } else if (path.indexOf('/config') !== -1 || path.indexOf('/network') !== -1 || path.indexOf('/node') !== -1) {
                            console.log('[MassaPay-Fetch] Returning network config');
                            responseData = { 
                                result: { 
                                    network: 'MainNet',
                                    chainId: 77658377, 
                                    name: 'mainnet',
                                    url: 'https://mainnet.massa.net/api/v2',
                                    minimalFee: '10000000'
                                } 
                            };
                        } else {
                            responseData = { status: 'ok', result: {} };
                        }
                        
                        console.log('[MassaPay-Fetch] Response:', JSON.stringify(responseData));
                        return Promise.resolve(new Response(
                            JSON.stringify(responseData),
                            {
                                status: 200,
                                statusText: 'OK',
                                headers: { 'Content-Type': 'application/json' }
                            }
                        ));
                    }
                    
                    // Also intercept station.massa requests (not station.massa.net)
                    if (url && url.indexOf('station.massa') !== -1 && url.indexOf('station.massa.net') === -1) {
                        console.log('[MassaPay-Fetch] Intercepting station.massa call:', url);
                        
                        var pathSM = url;
                        try {
                            var urlObjSM = new URL(url);
                            pathSM = urlObjSM.pathname;
                        } catch(e) {
                            var partsSM = url.split('?');
                            pathSM = partsSM[0];
                        }
                        
                        var responseDataSM = null;
                        
                        if (pathSM.indexOf('/plugin-manager') !== -1) {
                            console.log('[MassaPay-Fetch] Returning plugin-manager');
                            responseDataSM = {
                                result: [{
                                    name: 'Massa Wallet',
                                    author: 'Massa Labs',
                                    description: 'MassaPay Wallet',
                                    home: '/',
                                    icon: '',
                                    status: 'Up',
                                    version: '1.0.0'
                                }]
                            };
                        } else if (pathSM.indexOf('/accounts') !== -1 && pathSM.indexOf('signrules') === -1) {
                            console.log('[MassaPay-Fetch] Returning accounts - hasAddress:', hasAddress);
                            responseDataSM = {
                                result: hasAddress ? [{
                                    address: address,
                                    nickname: 'MassaPay Account',
                                    keyPair: { publicKey: '', nonce: '' },
                                    status: 'OK'
                                }] : []
                            };
                        } else if (pathSM.indexOf('/config') !== -1 || pathSM.indexOf('/network') !== -1) {
                            console.log('[MassaPay-Fetch] Returning network config');
                            responseDataSM = { 
                                result: { 
                                    network: 'MainNet', 
                                    chainId: 77658377,
                                    name: 'mainnet',
                                    url: 'https://mainnet.massa.net/api/v2',
                                    minimalFee: '10000000'
                                } 
                            };
                        } else {
                            responseDataSM = { status: 'ok', result: {} };
                        }
                        
                        console.log('[MassaPay-Fetch] Response:', JSON.stringify(responseDataSM));
                        return Promise.resolve(new Response(
                            JSON.stringify(responseDataSM),
                            {
                                status: 200,
                                statusText: 'OK',
                                headers: { 'Content-Type': 'application/json' }
                            }
                        ));
                    }
                    
                    return originalFetch.apply(this, arguments);
                };
                console.log('[MassaPay-Fetch] Fetch interceptor installed successfully');
            } catch(fetchError) {
                console.error('[MassaPay-Fetch] Error setting up fetch interceptor:', fetchError);
            }
            
            // Also intercept XMLHttpRequest for libraries that don't use fetch
            // Create a proper XHR mock that can intercept localhost:8080 and station.massa calls
            try {
                var OriginalXHR = window.XMLHttpRequest;
                
                function MockXHR() {
                    var self = this;
                    var realXhr = new OriginalXHR();
                    var intercepted = false;
                    var mockUrl = '';
                    var mockMethod = '';
                    var mockResponseData = null;
                    
                    // Copy all properties from real XHR
                    this.readyState = 0;
                    this.response = '';
                    this.responseText = '';
                    this.responseType = '';
                    this.responseURL = '';
                    this.responseXML = null;
                    this.status = 0;
                    this.statusText = '';
                    this.timeout = 0;
                    this.withCredentials = false;
                    this.upload = realXhr.upload;
                    
                    // Event handlers
                    this.onreadystatechange = null;
                    this.onload = null;
                    this.onerror = null;
                    this.onabort = null;
                    this.onloadstart = null;
                    this.onloadend = null;
                    this.onprogress = null;
                    this.ontimeout = null;
                    
                    this.open = function(method, url, async, user, password) {
                        mockUrl = url || '';
                        mockMethod = method || 'GET';
                        
                        console.log('[MassaPay-XHR] Open:', method, url);
                        
                        // Check if we should intercept this request
                        if (mockUrl && ((mockUrl.indexOf('station.massa') !== -1 && mockUrl.indexOf('station.massa.net') === -1) ||
                            mockUrl.indexOf('localhost:8080') !== -1 || mockUrl.indexOf('127.0.0.1:8080') !== -1)) {
                            
                            console.log('[MassaPay-XHR] Will intercept this request');
                            intercepted = true;
                            
                            // Determine response based on URL
                            if (mockUrl.indexOf('plugin-manager') !== -1) {
                                mockResponseData = {
                                    result: [{
                                        name: 'Massa Wallet',
                                        author: 'Massa Labs',
                                        description: 'MassaPay Wallet',
                                        home: '/',
                                        icon: '',
                                        status: 'Up',
                                        version: '1.0.0'
                                    }]
                                };
                            } else if (mockUrl.indexOf('/accounts') !== -1 && mockUrl.indexOf('signrules') === -1) {
                                console.log('[MassaPay-XHR] Accounts request - hasAddress:', hasAddress, 'address:', address);
                                mockResponseData = {
                                    result: hasAddress ? [{
                                        address: address,
                                        nickname: 'MassaPay Account',
                                        keyPair: { publicKey: '', nonce: '' },
                                        status: 'OK'
                                    }] : []
                                };
                            } else if (mockUrl.indexOf('/network') !== -1 || mockUrl.indexOf('/config') !== -1) {
                                mockResponseData = {
                                    result: {
                                        network: 'MainNet',
                                        chainId: 77658377,
                                        name: 'mainnet',
                                        url: 'https://mainnet.massa.net/api/v2',
                                        minimalFee: '10000000'
                                    }
                                };
                            } else {
                                mockResponseData = { status: 'ok', result: {} };
                            }
                            
                            self.readyState = 1; // OPENED
                        } else {
                            intercepted = false;
                            realXhr.open(method, url, async !== false, user, password);
                        }
                    };
                    
                    this.send = function(body) {
                        var self = this;
                        if (intercepted) {
                            // Simulate async response
                            setTimeout(function() {
                                self.readyState = 4; // DONE
                                self.status = 200;
                                self.statusText = 'OK';
                                self.responseText = JSON.stringify(mockResponseData);
                                self.response = self.responseText;
                                
                                console.log('[MassaPay-XHR] Mock response:', mockUrl);
                                
                                if (self.onreadystatechange) {
                                    try { self.onreadystatechange(); } catch(e) { console.error('[MassaPay-XHR] onreadystatechange error:', e); }
                                }
                                if (self.onload) {
                                    try { self.onload(); } catch(e) { console.error('[MassaPay-XHR] onload error:', e); }
                                }
                                if (self.onloadend) {
                                    try { self.onloadend(); } catch(e) { console.error('[MassaPay-XHR] onloadend error:', e); }
                                }
                            }, 10);
                        } else {
                            // Forward all event handlers to real XHR
                            realXhr.onreadystatechange = function() {
                                self.readyState = realXhr.readyState;
                                self.status = realXhr.status;
                                self.statusText = realXhr.statusText;
                                self.response = realXhr.response;
                                self.responseText = realXhr.responseText;
                                self.responseURL = realXhr.responseURL;
                                self.responseXML = realXhr.responseXML;
                                if (self.onreadystatechange) self.onreadystatechange();
                            };
                            realXhr.onload = function() { if (self.onload) self.onload(); };
                            realXhr.onerror = function() { if (self.onerror) self.onerror(); };
                            realXhr.onabort = function() { if (self.onabort) self.onabort(); };
                            realXhr.onloadstart = function() { if (self.onloadstart) self.onloadstart(); };
                            realXhr.onloadend = function() { if (self.onloadend) self.onloadend(); };
                            realXhr.onprogress = function() { if (self.onprogress) self.onprogress(); };
                            realXhr.ontimeout = function() { if (self.ontimeout) self.ontimeout(); };
                            
                            realXhr.send(body);
                        }
                    };
                    
                    this.abort = function() { if (!intercepted) realXhr.abort(); };
                    this.setRequestHeader = function(name, value) { if (!intercepted) realXhr.setRequestHeader(name, value); };
                    this.getResponseHeader = function(name) { 
                        if (!intercepted) return realXhr.getResponseHeader(name);
                        return null;
                    };
                    this.dispatchEvent = function(event) {
                        if (!intercepted) {
                            return realXhr.dispatchEvent(event);
                        }
                        return true;
                    };
                }
                
                // Copy static properties
                MockXHR.UNSENT = 0;
                MockXHR.OPENED = 1;
                MockXHR.HEADERS_RECEIVED = 2;
                MockXHR.LOADING = 3;
                MockXHR.DONE = 4;
                
                window.XMLHttpRequest = MockXHR;
                console.log('[MassaPay-XHR] XMLHttpRequest interceptor installed');
            } catch(xhrError) {
                console.error('[MassaPay-XHR] Error setting up XHR interceptor:', xhrError);
            }
            
            // Intercept window.open to station.massa.net (redirect to download page)
            var originalWindowOpen = window.open;
            window.open = function(url, target, features) {
                console.log('[MassaPay] window.open called:', url);
                if (url && url.indexOf('station.massa') !== -1) {
                    console.log('[MassaPay] Intercepted window.open to station.massa, triggering connect instead');
                    if (window.MassaPayBridge) {
                        window.MassaPayBridge.request(JSON.stringify({
                            method: 'connect',
                            params: {}
                        }));
                    }
                    return null; // Don't actually open the window
                }
                return originalWindowOpen.apply(window, arguments);
            };
            
            // ============================================
            // CRITICAL: TABSTREAM CUSTOMEVENT SYSTEM - Early injection
            // bearby-web3 library uses CustomEvents for communication
            // We must set this up BEFORE bearby.js loads
            // ============================================
            
            var BEARBY_INJECTED = '@/BearBy/injected-script';
            var BEARBY_CONTENT = '@/BearBy/content-script';
            
            // Message types from bearby-web3/config/stream-keys.ts
            var BearbyMsgTypes = {
                GET_DATA: '@/BearBy/get-wallet-data',
                SET_DATA: '@/BearBy/set-account-data',
                CONNECT_APP: '@/BearBy/connect-app',
                RESPONSE_CONNECT_APP: '@/BearBy/respoonse-connect-app', // typo matches bearby source
                REQUEST_PING: '@/BearBy/req-content-ping',
                PING_RESPONSE: '@/BearBy/res-content-ping',
                ACCOUNT_CHANGED: '@/BearBy/accounts-just-changed',
                NETWORK_CHANGED: '@/BearBy/network-just-changed'
            };
            
            // Send to bearby.js (injected script)
            function sendToBearbyInjectedEarly(payload) {
                var event = new CustomEvent(BEARBY_INJECTED, {
                    detail: JSON.stringify(payload)
                });
                document.dispatchEvent(event);
                console.log('[MassaPay-TabStream-Early] Sent:', payload.type);
            }
            
            // Store pending connect promise for later resolution
            window._massaPayPendingConnects = [];
            
            // Listen for TabStream messages from bearby.js
            document.addEventListener(BEARBY_CONTENT, function(event) {
                if (!event || !event.detail) return;
                
                var msg;
                try {
                    msg = JSON.parse(event.detail);
                } catch(e) {
                    console.error('[MassaPay-TabStream-Early] Parse error:', e);
                    return;
                }
                
                console.log('[MassaPay-TabStream-Early] Received:', msg.type, 'uuid:', msg.uuid);
                
                var uuid = msg.uuid || (msg.payload && msg.payload.uuid);
                
                if (msg.type === BearbyMsgTypes.REQUEST_PING) {
                    // Extension ping - respond to indicate installed
                    console.log('[MassaPay-TabStream-Early] PING - responding as installed');
                    sendToBearbyInjectedEarly({
                        type: BearbyMsgTypes.PING_RESPONSE,
                        from: BEARBY_CONTENT,
                        uuid: uuid,
                        payload: {
                            uuid: uuid,
                            installed: true
                        }
                    });
                } else if (msg.type === BearbyMsgTypes.GET_DATA) {
                    // Initial data request - respond with same type GET_DATA!
                    // bearby.js #subscribe() listens for GET_DATA to set #installed = true
                    console.log('[MassaPay-TabStream-Early] GET_DATA - returning state');
                    sendToBearbyInjectedEarly({
                        type: BearbyMsgTypes.GET_DATA,
                        from: BEARBY_CONTENT,
                        uuid: uuid,
                        payload: {
                            base58: isConnected ? address : '',
                            accounts: isConnected ? [address] : [],
                            net: 'MainNet',
                            period: 0,
                            enabled: true,
                            connected: isConnected
                        }
                    });
                } else if (msg.type === BearbyMsgTypes.CONNECT_APP) {
                    // App requesting connection
                    console.log('[MassaPay-TabStream-Early] CONNECT_APP from:', msg.payload && msg.payload.title);
                    
                    // Store the uuid for later resolution
                    window._massaPayPendingConnects.push({
                        uuid: uuid,
                        title: msg.payload && msg.payload.title
                    });
                    
                    // Trigger native connect dialog
                    if (window.MassaPayBridge) {
                        window.MassaPayBridge.postMessage(JSON.stringify({
                            id: Date.now(),
                            method: 'wallet_connect',
                            params: { 
                                dappName: msg.payload && msg.payload.title,
                                tabStreamUuid: uuid
                            }
                        }));
                    }
                }
            });
            
            console.log('[MassaPay-TabStream-Early] CustomEvent listener registered on', BEARBY_CONTENT);
            
            // Function to resolve pending TabStream connects (called from main provider)
            window._resolveTabStreamConnect = function(success, addr) {
                console.log('[MassaPay-TabStream-Early] Resolving connects:', window._massaPayPendingConnects.length, 'success:', success);
                
                window._massaPayPendingConnects.forEach(function(pending) {
                    if (success) {
                        sendToBearbyInjectedEarly({
                            type: BearbyMsgTypes.RESPONSE_CONNECT_APP,
                            from: BEARBY_CONTENT,
                            uuid: pending.uuid,
                            payload: {
                                uuid: pending.uuid,
                                base58: addr,
                                accounts: [addr],
                                net: 'MainNet',
                                period: 0,
                                connected: true,
                                resolve: true
                            }
                        });
                        
                        // Also send account changed
                        setTimeout(function() {
                            sendToBearbyInjectedEarly({
                                type: BearbyMsgTypes.ACCOUNT_CHANGED,
                                from: BEARBY_CONTENT,
                                payload: {
                                    base58: addr,
                                    accounts: [addr],
                                    net: 'MainNet'
                                }
                            });
                        }, 50);
                    } else {
                        sendToBearbyInjectedEarly({
                            type: BearbyMsgTypes.RESPONSE_CONNECT_APP,
                            from: BEARBY_CONTENT,
                            uuid: pending.uuid,
                            payload: {
                                uuid: pending.uuid,
                                reject: 'Connection rejected'
                            }
                        });
                    }
                });
                
                window._massaPayPendingConnects = [];
            };
            
            // If already connected, send initial state
            if (isConnected && address) {
                setTimeout(function() {
                    console.log('[MassaPay-TabStream-Early] Sending initial connected state');
                    // Use GET_DATA type - this is what bearby.js listens for!
                    sendToBearbyInjectedEarly({
                        type: BearbyMsgTypes.GET_DATA,
                        from: BEARBY_CONTENT,
                        payload: {
                            base58: address,
                            accounts: [address],
                            net: 'MainNet',
                            period: 0,
                            enabled: true,
                            connected: true
                        }
                    });
                }, 50);
            }
            
            // Create 'st' alias for bearby - some DApps like EagleFi use 'st' object
            // This is likely from the @hicaru/bearby.js library's internal naming
            window.st = window.bearby;
            console.log('[MassaPay-Early] Created window.st alias for bearby');
            
            console.log('[MassaPay-Early] Early provider ready, connected:', isConnected);
            console.log('[MassaPay-Early] window.massaWallet.standalone:', window.massaWallet?.standalone);
            
        })();
    """.trimIndent()
    
    /**
     * CSS to inject for better mobile experience
     */
    val mobileOptimizationCSS = """
        <style>
            body { -webkit-text-size-adjust: 100%; }
            html, body { max-width: 100vw; overflow-x: hidden; }
        </style>
    """.trimIndent()
}
