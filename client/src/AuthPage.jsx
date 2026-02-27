import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { loginUser, registerUser, googleLogin } from "./api";
import { getStoredAuth, setStoredAuth } from "./auth";

function AuthPage({ mode }) {
    const isLogin = mode === "login";
    const navigate = useNavigate();
    const [form, setForm] = useState({
        username: "",
        email: "",
        password: "",
    });
    const [status, setStatus] = useState({ loading: false, error: "", success: "" });

    useEffect(() => {
        const auth = getStoredAuth();
        if (auth?.username) {
            navigate("/");
        }
    }, [navigate]);

    const onChange = (field) => (event) => {
        setForm((prev) => ({ ...prev, [field]: event.target.value }));
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        setStatus({ loading: true, error: "", success: "" });
        try {
            if (isLogin) {
                const data = await loginUser({ email: form.email, password: form.password });
                if (data?.username) {
                    setStoredAuth({ username: data.username });
                    navigate("/");
                    return;
                }
                setStatus({ loading: false, error: "Login succeeded but no user info returned.", success: "" });
                return;
            }

            const data = await registerUser({
                username: form.username,
                email: form.email,
                password: form.password,
            });

            if (data?.username) {
                setStoredAuth({ username: data.username });
                navigate("/");
                return;
            }

            setStatus({ loading: false, error: "Registration succeeded but no user info returned.", success: "" });
        } catch (error) {
            const message = error?.response?.data?.message || error?.message || "Something went wrong.";
            setStatus({ loading: false, error: message, success: "" });
        }
    };

    useEffect(() => {
        const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
        if (!clientId) return;

        const handleCredentialResponse = async (response) => {
            setStatus({ loading: true, error: "", success: "" });
            try {
                const data = await googleLogin(response.credential);
                if (data?.username) {
                    setStoredAuth({ username: data.username });
                    navigate("/");
                    return;
                }
                setStatus({ loading: false, error: "Google login succeeded but no user info returned.", success: "" });
            } catch (err) {
                const message = err?.response?.data?.message || err?.message || "Google login failed.";
                setStatus({ loading: false, error: message, success: "" });
            }
        };

        if (window.google && window.google.accounts && window.google.accounts.id) {
            window.google.accounts.id.initialize({
                client_id: clientId,
                callback: handleCredentialResponse,
            });

            // Render button into our container if present
            const container = document.getElementById("google-signin-button");
            if (container) {
                window.google.accounts.id.renderButton(container, { theme: "outline", size: "large" });
            }
        }
    }, [navigate]);

    return (
        <div className="auth-page">
            <div className="auth-card">
                <div className="auth-header">
                    <h1 className="auth-title">{isLogin ? "Welcome back" : "Create your account"}</h1>
                    <p className="auth-subtitle">
                        {isLogin ? "Sign in to continue to the judge." : "Register to start solving problems."}
                    </p>
                </div>

                <form className="auth-form" onSubmit={handleSubmit}>
                    {!isLogin && (
                        <div className="auth-field">
                            <label htmlFor="username">Username</label>
                            <input
                                id="username"
                                value={form.username}
                                onChange={onChange("username")}
                                placeholder="Your handle"
                                required
                            />
                        </div>
                    )}

                    <div className="auth-field">
                        <label htmlFor="email">Email</label>
                        <input
                            id="email"
                            type="email"
                            value={form.email}
                            onChange={onChange("email")}
                            placeholder="you@example.com"
                            required
                        />
                    </div>

                    <div className="auth-field">
                        <label htmlFor="password">Password</label>
                        <input
                            id="password"
                            type="password"
                            value={form.password}
                            onChange={onChange("password")}
                            placeholder="••••••••"
                            required
                        />
                    </div>

                    {status.error && <p className="auth-error">{status.error}</p>}
                    {status.success && <p className="auth-success">{status.success}</p>}

                    <button className="auth-button" type="submit" disabled={status.loading}>
                        {status.loading ? "Please wait..." : isLogin ? "Login" : "Create account"}
                    </button>
                </form>

                {/* Google Sign-In button (login only) */}
                {isLogin && (
                    <div style={{ display: "flex", justifyContent: "center", marginTop: "1rem" }}>
                        <div id="google-signin-button" />
                    </div>
                )}

                <div className="auth-footer">
                    {isLogin ? (
                        <p>
                            New here? <Link to="/register">Create an account</Link>
                        </p>
                    ) : (
                        <p>
                            Already have an account? <Link to="/login">Log in</Link>
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
}

export default AuthPage;
