const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').trim();
if (!API_BASE_URL) {
  throw new Error('Missing VITE_API_BASE_URL configuration. Check frontend/.env files.');
}

class ApiClient {
  constructor() {
    this.cache = new Map();
    this.defaultTtl = 0; // 0ms cache TTL so order/delivery status updates have zero stale delay
    this.tokenProvider = null;
  }

  setTokenProvider(providerFn) {
    this.tokenProvider = providerFn;
  }

  async resolveAuthToken(explicitHeaders = {}) {
    if (explicitHeaders['Authorization'] || explicitHeaders['authorization']) {
      return null;
    }
    if (this.tokenProvider) {
      try {
        const freshToken = await this.tokenProvider();
        if (freshToken) {
          localStorage.setItem('qb_token', freshToken);
          return freshToken;
        }
      } catch (err) {
        console.warn('Could not refresh Firebase ID token:', err?.message || err);
      }
    }
    return localStorage.getItem('qb_token');
  }

  clearCache() {
    this.cache.clear();
    try {
      Object.keys(sessionStorage).forEach((k) => {
        if (k.startsWith('qb_c_')) sessionStorage.removeItem(k);
      });
    } catch {}
  }

  getCache(endpoint, maxAgeMs) {
    const token = localStorage.getItem('qb_token') || 'anon';
    const cacheKey = `${token}:${endpoint}`;
    const ttl = maxAgeMs ?? this.defaultTtl;
    if (ttl <= 0) {
      return null;
    }

    // 1. In-memory instant lookup
    const memoryCached = this.cache.get(cacheKey);
    if (memoryCached && Date.now() - memoryCached.timestamp < ttl) {
      return memoryCached.data;
    }

    // 2. Session storage instant lookup
    try {
      const raw = sessionStorage.getItem('qb_c_' + cacheKey);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Date.now() - parsed.timestamp < ttl) {
          this.cache.set(cacheKey, parsed);
          return parsed.data;
        }
      }
    } catch {}

    return null;
  }

  async request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers,
    };

    const token = await this.resolveAuthToken(headers);
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    // Increased timeout to 45s to handle Render free-tier cold starts gracefully
    const timeoutMs = options.timeout || 45000;
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

    const config = {
      ...options,
      headers,
      signal: options.signal || controller.signal,
    };

    try {
      const response = await fetch(url, config);
      clearTimeout(timeoutId);

      if (response.status === 204) {
        return null;
      }

      const data = await response.json().catch(() => null);

      if (!response.ok) {
        const validationDetails = data?.validationErrors
          ? Object.values(data.validationErrors).filter(Boolean).join('. ')
          : '';
        const errorMessage =
          validationDetails ||
          data?.message ||
          (data?.error ? `${data.error}: ${data.message || ''}` : `HTTP Error ${response.status}`);
        const error = new Error(errorMessage);
        error.status = response.status;
        error.data = data;
        throw error;
      }

      return data;
    } catch (err) {
      clearTimeout(timeoutId);
      if (err.name === 'AbortError') {
        const timeoutError = new Error(`Request to ${endpoint} timed out. The server may be waking up.`);
        timeoutError.isTimeout = true;
        throw timeoutError;
      }
      if (err instanceof TypeError && !err.status) {
        const netError = new Error(`Cannot reach QuickBite backend at ${API_BASE_URL}. Please ensure the backend server is running.`);
        netError.isNetworkError = true;
        throw netError;
      }
      if (err.status === 401 && !endpoint.includes('/auth/')) {
        console.warn('Authentication expired or unauthorized');
      }
      throw err;
    }
  }

  async get(endpoint, headers = {}, options = {}) {
    const skipCache = options.skipCache === true;
    const token = localStorage.getItem('qb_token') || 'anon';
    const cacheKey = `${token}:${endpoint}`;

    if (!skipCache) {
      const ttl = options.ttl ?? this.defaultTtl;
      const cached = ttl > 0 ? this.cache.get(cacheKey) : null;
      if (cached && Date.now() - cached.timestamp < ttl) {
        return cached.data;
      }
    }

    const data = await this.request(endpoint, { method: 'GET', headers, ...options });
    
    // Store in instant cache (memory + sessionStorage)
    if (data !== null && data !== undefined) {
      const entry = { data, timestamp: Date.now() };
      this.cache.set(cacheKey, entry);
      try {
        sessionStorage.setItem('qb_c_' + cacheKey, JSON.stringify(entry));
      } catch {}
    }

    return data;
  }

  async post(endpoint, body, headers = {}) {
    this.clearCache(); // Invalidate cache on mutations
    return this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(body),
      headers,
    });
  }

  async put(endpoint, body, headers = {}) {
    this.clearCache();
    return this.request(endpoint, {
      method: 'PUT',
      body: JSON.stringify(body),
      headers,
    });
  }

  async patch(endpoint, body, headers = {}) {
    this.clearCache();
    return this.request(endpoint, {
      method: 'PATCH',
      body: JSON.stringify(body),
      headers,
    });
  }

  async delete(endpoint, headers = {}) {
    this.clearCache();
    return this.request(endpoint, { method: 'DELETE', headers });
  }

  async uploadFile(endpoint, formData, headers = {}) {
    this.clearCache();
    const url = `${API_BASE_URL}${endpoint}`;
    const customHeaders = { ...headers };
    const token = await this.resolveAuthToken(customHeaders);
    if (token) {
      customHeaders['Authorization'] = `Bearer ${token}`;
    }
    const response = await fetch(url, {
      method: 'POST',
      headers: customHeaders,
      body: formData,
    });
    const data = await response.json().catch(() => null);
    if (!response.ok) {
      const errorMessage = data?.message || (data?.error ? `${data.error}: ${data.message || ''}` : `HTTP Error ${response.status}`);
      const error = new Error(errorMessage);
      error.status = response.status;
      error.data = data;
      throw error;
    }
    return data;
  }
}

export const api = new ApiClient();

