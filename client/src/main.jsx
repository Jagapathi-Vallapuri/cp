import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import "./index.css";
import "katex/dist/katex.min.css";
import App from "./App.jsx";
import AuthPage from "./AuthPage.jsx";
import { getStoredAuth } from "./auth";

const RequireAuth = ({ children }) => {
    const auth = getStoredAuth();
    if (!auth?.username) {
        return <Navigate to="/login" replace />;
    }
    return children;
};

createRoot(document.getElementById("root")).render(
    <StrictMode>
        <BrowserRouter>
            <Routes>
                <Route
                    path="/"
                    element={
                        <RequireAuth>
                            <App />
                        </RequireAuth>
                    }
                />
                <Route path="/login" element={<AuthPage mode="login" />} />
                <Route path="/register" element={<AuthPage mode="register" />} />
                <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
        </BrowserRouter>
    </StrictMode>
);
