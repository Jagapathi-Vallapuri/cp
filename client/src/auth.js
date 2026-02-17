const USERNAME_KEY = "authUsername";
const LEGACY_TOKEN_KEY = "authToken";

export const getStoredAuth = () => {
    if (typeof window === "undefined") return null;
    const username = localStorage.getItem(USERNAME_KEY);
    if (!username) return null;
    return { username };
};

export const setStoredAuth = ({ username }) => {
    if (typeof window === "undefined") return;
    if (username) localStorage.setItem(USERNAME_KEY, username);
};

export const clearStoredAuth = () => {
    if (typeof window === "undefined") return;
    localStorage.removeItem(LEGACY_TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
};
