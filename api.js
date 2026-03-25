/**
 * Prism Survival API Client
 * Base URL: http://208.84.103.249:26095
 */

class PrismAPI {
    constructor() {
        this.baseURL = 'http://208.84.103.249:26095';
        this.apiKey = 'sk-YziIK3AdUmroqmquJrshwJclNQ3dl8qNrSnXBuuBvPOQ1dAf';
    }

    /**
     * Make authenticated API request
     * @param {string} endpoint - API endpoint path
     * @param {string} method - HTTP method (GET, POST)
     * @param {Object} params - Query parameters
     * @returns {Promise<Object>} API response
     */
    async request(endpoint, method = 'GET', params = {}) {
        const url = new URL(endpoint, this.baseURL);

        // Add API key and other parameters to query string
        const queryParams = { key: this.apiKey, ...params };
        Object.entries(queryParams).forEach(([key, value]) => {
            if (value !== undefined && value !== null) {
                url.searchParams.append(key, value);
            }
        });

        const config = {
            method: method,
            headers: {
                'X-API-Key': this.apiKey,
                'Content-Type': 'application/json',
            }
        };

        try {
            console.log(`Making ${method} request to:`, url.toString());
            const response = await fetch(url.toString(), config);

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`HTTP ${response.status}: ${errorText}`);
            }

            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                return await response.json();
            } else {
                return await response.text();
            }
        } catch (error) {
            console.error('API Request failed:', error);
            throw error;
        }
    }



    // ============ PLAYER STATS ============

    /**
     * Get online players list with stats
     * @returns {Promise<Array>} Array of online player data
     */
    async getOnlinePlayers() {
        return await this.request('/players/stats/onlineplayers', 'GET');
    }

    /**
     * Get specific player stats
     * @param {string} playerName - Player name to get stats for
     * @returns {Promise<Object>} Player stats object
     */
    async getPlayerStats(playerName) {
        return await this.request('/players/stats/', 'GET', { player: playerName });
    }

    /**
     * Get player balance
     * @param {string} playerName - Player name to get balance for
     * @returns {Promise<Object>} Player balance data
     */
    async getPlayerMoney(playerName) {
        return await this.request('/players/money', 'GET', { player: playerName });
    }

    /**
     * Get player playtime
     * @param {string} playerName - Player name to get playtime for
     * @returns {Promise<Object>} Player playtime data
     */
    async getPlayerPlaytime(playerName) {
        return await this.request('/players/playtime', 'GET', { player: playerName });
    }

    // ============ LEADERBOARDS ============

    /**
     * Get money leaderboard
     * @returns {Promise<Array>} Money leaderboard data
     */
    async getMoneyLeaderboard() {
        return await this.request('/leaderboard/money/list', 'GET');
    }

    /**
     * Get playtime leaderboard
     * @returns {Promise<Array>} Playtime leaderboard data
     */
    async getPlaytimeLeaderboard() {
        return await this.request('/leaderboard/playtime/list', 'GET');
    }

    /**
     * Get shards leaderboard
     * @returns {Promise<Array>} Shards leaderboard data
     */
    async getShardsLeaderboard() {
        return await this.request('/leaderboard/shards/list', 'GET');
    }

    // ============ ECONOMY ============

    /**
     * Get global economy stats
     * @returns {Promise<Object>} Economy statistics
     */
    async getEconomyStats() {
        return await this.request('/economy', 'GET');
    }
}

// ============ USAGE EXAMPLES ============

// Initialize API client
const api = new PrismAPI();

// Example usage functions
async function examples() {
    try {
        console.log('=== Player Stats Examples ===');

        // Get online players
        const onlinePlayers = await api.getOnlinePlayers();
        console.log('Online players:', onlinePlayers);

        // Get player stats (if there are any online players)
        if (onlinePlayers.length > 0) {
            const playerName = onlinePlayers[0].name;
            const playerStats = await api.getPlayerStats(playerName);
            console.log(`Stats for ${playerName}:`, playerStats);
        }

        console.log('=== Leaderboard Examples ===');

        // Get money leaderboard
        const moneyLeaderboard = await api.getMoneyLeaderboard();
        console.log('Money leaderboard:', moneyLeaderboard);


    } catch (error) {
        console.error('Example execution failed:', error);
    }
}

// Export for use in other files (Node.js)
if (typeof module !== 'undefined' && module.exports) {
    module.exports = PrismAPI;
}

// Make available globally (Browser)
if (typeof window !== 'undefined') {
    window.PrismAPI = PrismAPI;
}

// Quick test function - uncomment to run examples
examples();

// ============ QUICK ACCESS FUNCTIONS ============


// ============ USAGE INSTRUCTIONS ============
console.log(`
🚀 Prism Survival API Client Loaded!

📝 Quick Usage:
- getOnlinePlayers()            - Get online players
- getPlayerStats("PlayerName")  - Get player stats

🔧 Advanced Usage:
const api = new PrismAPI();
await api.getPlayerStats("PlayerName");
await api.getOnlinePlayers();

📊 Available endpoints:
- Player Stats: stats, money, playtime
- Leaderboards: money, playtime, shards  
- Economy: global stats

🔑 API Key: Configured automatically
🌐 Base URL: ${new PrismAPI().baseURL}
`);
