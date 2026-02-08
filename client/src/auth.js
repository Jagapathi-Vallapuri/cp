const TOKEN_KEY = "authToken";
const USERNAME_KEY = "authUsername";

export const getStoredAuth = () => {
    if (typeof window === "undefined") return null;
    const token = localStorage.getItem(TOKEN_KEY);
    const username = localStorage.getItem(USERNAME_KEY);
    if (!token || !username) return null;
    return { token, username };
};

export const setStoredAuth = ({ token, username }) => {
    if (typeof window === "undefined") return;
    if (token) localStorage.setItem(TOKEN_KEY, token);
    if (username) localStorage.setItem(USERNAME_KEY, username);
};

export const clearStoredAuth = () => {
    if (typeof window === "undefined") return;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
};
