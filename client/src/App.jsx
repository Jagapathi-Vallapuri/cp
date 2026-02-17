import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import remarkMath from "remark-math";
import rehypeKatex from "rehype-katex";
import {
    getProblems,
    getProblemDetails,
    getSubmissionStatus,
    logoutUser,
    submitCode,
} from "./api";
import "./App.css";
import { clearStoredAuth, getStoredAuth } from "./auth";

const templates = {
    cpp: `#include <bits/stdc++.h>\n\nint main() {\n    int x;\n    std::cin >> x;\n    std::cout << x + 1;\n    return 0;\n}`,
    python: `x = int(input())\nprint(x + 1)`,
    java: `import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        if (scanner.hasNextInt()) {\n            int x = scanner.nextInt();\n            System.out.println(x + 1);\n        }\n        scanner.close();\n    }\n}`
};

function App() {
    const [language, setLanguage] = useState("cpp");
    const [code, setCode] = useState(templates.cpp);
    const [problems, setProblems] = useState([]);
    const [selectedProblemId, setSelectedProblemId] = useState(null);
    const [selectedProblem, setSelectedProblem] = useState(null);
    const [problemsLoading, setProblemsLoading] = useState(false);
    const [problemsError, setProblemsError] = useState("");
    const [submissionState, setSubmissionState] = useState({
        id: null,
        status: "",
        verdict: "",
        loading: false,
        error: "",
        timeTaken: null,
        memoryUsed: null,
    });
    const [auth, setAuth] = useState(() => getStoredAuth());
    const [sidebarOpen, setSidebarOpen] = useState(true);
    const [showError, setShowError] = useState(false);
    const [copyMessage, setCopyMessage] = useState("");

    useEffect(() => {
        if (!sidebarOpen) return;
        const handleKeyDown = (event) => {
            if (event.key === "Escape") {
                setSidebarOpen(false);
            }
        };

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [sidebarOpen]);

    useEffect(() => {
        setAuth(getStoredAuth());
    }, []);

    useEffect(() => {
        let isMounted = true;
        const loadProblems = async () => {
            setProblemsLoading(true);
            setProblemsError("");
            try {
                const data = await getProblems();
                if (!isMounted) return;
                const list = Array.isArray(data) ? data : [];
                setProblems(list);
                if (list.length > 0) {
                    setSelectedProblemId(list[0].id);
                }
            } catch (error) {
                if (!isMounted) return;
                setProblemsError("Failed to load problems.");
            } finally {
                if (isMounted) setProblemsLoading(false);
            }
        };

        loadProblems();
        return () => {
            isMounted = false;
        };
    }, []);

    useEffect(() => {
        setCode(templates[language] || "");
    }, [language]);

    useEffect(() => {
        if (!selectedProblemId) {
            setSelectedProblem(null);
            return;
        }

        let isMounted = true;
        const fromList = problems.find((problem) => problem.id === selectedProblemId);
        if (fromList) {
            setSelectedProblem(fromList);
        }

        const loadDetails = async () => {
            try {
                const data = await getProblemDetails(selectedProblemId);
                if (!isMounted) return;
                setSelectedProblem(data);
            } catch (error) {
                if (!isMounted) return;
                if (fromList) {
                    setSelectedProblem(fromList);
                }
            }
        };

        loadDetails();
        return () => {
            isMounted = false;
        };
    }, [selectedProblemId, problems]);

    const handleSubmit = async () => {
        if (!selectedProblemId) {
            setSubmissionState((prev) => ({
                ...prev,
                error: "Select a problem first.",
            }));
            return;
        }
        if (!code.trim()) {
            setSubmissionState((prev) => ({
                ...prev,
                error: "Code cannot be empty.",
            }));
            return;
        }
        setSubmissionState((prev) => ({
            ...prev,
            loading: true,
            status: "PENDING",
            verdict: "",
            error: "",
            timeTaken: null,
            memoryUsed: null,
        }));

        try {
            const submission = await submitCode({
                problemId: selectedProblemId,
                language,
                code,
            });
            const id = submission?.id || null;
            setSubmissionState((prev) => ({
                ...prev,
                id,
                status: submission?.status || "PENDING",
                verdict: submission?.verdict || "",
                error: submission?.error || "",
                timeTaken: submission?.timeTaken ?? null,
                memoryUsed: submission?.memoryUsed ?? null,
                loading: id ? prev.loading : false,
            }));
        } catch (error) {
            setSubmissionState((prev) => ({
                ...prev,
                error: "Failed to submit solution.",
                loading: false,
            }));
        }
    };

    useEffect(() => {
        if (!submissionState.id) return;

        let isMounted = true;
        let timeoutId;

        const poll = async () => {
            try {
                const result = await getSubmissionStatus(submissionState.id);
                if (!isMounted) return;
                setSubmissionState((prev) => ({
                    ...prev,
                    status: result?.status || "",
                    verdict: result?.verdict || "",
                    error: result?.error || "",
                    timeTaken: result?.timeTaken ?? null,
                    memoryUsed: result?.memoryUsed ?? null,
                }));

                if (result?.status === "COMPLETED" || result?.status === "FAILED") {
                    setSubmissionState((prev) => ({
                        ...prev,
                        loading: false,
                    }));
                    return;
                }
            } catch (error) {
                if (!isMounted) return;
                setSubmissionState((prev) => ({
                    ...prev,
                    error: "Failed to fetch submission status.",
                    loading: false,
                }));
                return;
            }

            timeoutId = setTimeout(poll, 1500);
        };

        poll();

        return () => {
            isMounted = false;
            if (timeoutId) clearTimeout(timeoutId);
        };
    }, [submissionState.id]);

    const copySubmissionId = async () => {
        if (!submissionState.id) return;
        try {
            await navigator.clipboard.writeText(submissionState.id);
            setCopyMessage("Copied!");
            setTimeout(() => setCopyMessage(""), 2000);
        } catch (e) {
            setCopyMessage("Copy failed");
            setTimeout(() => setCopyMessage(""), 2000);
        }
    };

    const handleLogout = async () => {
        try {
            await logoutUser();
        } catch (error) {
        }
        clearStoredAuth();
        setAuth(null);
    };

    return (
        <div className="app">
            <header className="app-header">
                <div className="brand">
                    <div className="logo-wrap" aria-hidden>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M3 12L9 6L15 12L9 18L3 12Z" fill="white"/>
                            <path d="M15 6L21 12L15 18" stroke="rgba(255,255,255,0.5)" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round"/>
                        </svg>
                    </div>
                    <h1 className="app-title">Online Judge</h1>
                </div>
                <div className="auth-bar">
                    {auth?.username ? (
                        <>
                            <span className="auth-user">Signed in as <strong>{auth.username}</strong></span>
                            <button className="auth-cta" onClick={handleLogout}>Logout</button>
                        </>
                    ) : (
                        <>
                            <Link className="auth-link" to="/login">Login</Link>
                            <Link className="auth-cta" to="/register">Register</Link>
                        </>
                    )}
                </div>
            </header>

            <div className="layout">
                <div className="left-rail">
                    <div className="drawer-toggle-row">
                        <button
                            className={`sidebar-toggle ${sidebarOpen ? "open" : "closed"}`}
                            type="button"
                            onClick={() => setSidebarOpen((prev) => !prev)}
                            aria-expanded={sidebarOpen}
                            aria-label={sidebarOpen ? "Collapse problem list" : "Open problem list"}
                        />
                        <span className="drawer-label">Problems</span>
                    </div>

                    {sidebarOpen && (
                        <>
                            <div className="drawer-backdrop" onClick={() => setSidebarOpen(false)} />
                            <aside className="panel sidebar drawer">
                                <div className="drawer-header">
                                    <h2 className="sidebar-title">Problems</h2>
                                    <button
                                        className="drawer-close"
                                        type="button"
                                        onClick={() => setSidebarOpen(false)}
                                        aria-label="Close problem list"
                                    >
                                        Close
                                    </button>
                                </div>

                                {problemsLoading && <p>Loading problems...</p>}
                                {problemsError && <p className="status-error">{problemsError}</p>}

                                <div className="problem-list">
                                    {problems.map((problem) => (
                                        <div
                                            key={problem.id}
                                            className={`problem-card ${selectedProblemId === problem.id ? "active" : ""}`}
                                            onClick={() => {
                                                setSelectedProblemId(problem.id);
                                                setSidebarOpen(false);
                                            }}
                                            role="button"
                                            tabIndex={0}
                                            onKeyDown={(event) => {
                                                if (event.key === "Enter") {
                                                    setSelectedProblemId(problem.id);
                                                    setSidebarOpen(false);
                                                }
                                            }}
                                        >
                                            <h3>{problem.title}</h3>
                                            <div className="problem-meta">
                                                {problem.difficulty && <span className="pill">{problem.difficulty}</span>}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </aside>
                        </>
                    )}

                    <aside className="panel problem-detail side-panel">
                        <h2>{selectedProblem?.title || "Select a problem"}</h2>
                        <div className="problem-markdown">
                            <ReactMarkdown
                                remarkPlugins={[remarkMath]}
                                rehypePlugins={[rehypeKatex]}
                            >
                                {selectedProblem?.description || "Choose a problem from the list to view its statement."}
                            </ReactMarkdown>
                        </div>
                        <div className="detail-meta">
                            {selectedProblem?.difficulty && <span>Difficulty: {selectedProblem.difficulty}</span>}
                            {selectedProblem?.timeLimitSeconds != null && (
                                <span>Time Limit: {selectedProblem.timeLimitSeconds}s</span>
                            )}
                            {selectedProblem?.memoryLimitMb != null && (
                                <span>Memory: {selectedProblem.memoryLimitMb} MB</span>
                            )}
                            {selectedProblem?.testCaseCount != null && (
                                <span>Test Cases: {selectedProblem.testCaseCount}</span>
                            )}
                        </div>
                    </aside>
                </div>

                <main className="content">
                    <section className="panel submission-panel code-panel">
                        <div className="form-row">
                            <div className="form-field">
                                <label htmlFor="language">Language</label>
                                <select
                                    id="language"
                                    value={language}
                                    onChange={(event) => setLanguage(event.target.value)}
                                >
                                    <option value="cpp">C++</option>
                                    <option value="python">Python 3</option>
                                    <option value="java">Java</option>
                                </select>
                            </div>
                            <div className="form-field">
                                <label>Username</label>
                                <div className="username-chip">
                                    {auth?.username || "guest"}
                                </div>
                            </div>
                        </div>

                        <textarea
                            className="editor"
                            value={code}
                            onChange={(event) => setCode(event.target.value)}
                        />

                        <div className="action-row">
                            <button className="submit-btn" onClick={handleSubmit} disabled={submissionState.loading}>
                                {submissionState.loading ? "Judging..." : "Submit Solution"}
                            </button>
                        </div>

                        {submissionState.status && (
                            <div className="status-card">
                                <div className="status-card-header">
                                    <div>
                                        <div><strong>ID:</strong> {submissionState.id?.substring(0, 8)}...</div>
                                        <div><strong>Status:</strong> {submissionState.status}</div>
                                    </div>
                                    <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
                                        {submissionState.id && (
                                            <button className="copy-btn" onClick={copySubmissionId} aria-label="Copy submission id">
                                                Copy ID
                                            </button>
                                        )}
                                        {copyMessage && <span style={{ fontSize: "0.85rem" }}>{copyMessage}</span>}
                                        {submissionState.error && (
                                            <button className="error-toggle" onClick={() => setShowError((s) => !s)}>
                                                {showError ? "Hide Error" : "Show Error"}
                                            </button>
                                        )}
                                    </div>
                                </div>

                                <div style={{ marginTop: "0.6rem", display: "flex", gap: "1rem", alignItems: "center", flexWrap: "wrap" }}>
                                    {submissionState.verdict && (
                                        <div className={`verdict ${submissionState.verdict === "ACCEPTED" ? "ok" : "bad"}`}>
                                            {submissionState.verdict}
                                        </div>
                                    )}
                                    {submissionState.timeTaken != null && (
                                        <div><strong>Time:</strong> {submissionState.timeTaken} ms</div>
                                    )}
                                    {submissionState.memoryUsed != null && (
                                        <div><strong>Memory:</strong> {submissionState.memoryUsed} KB</div>
                                    )}
                                </div>

                                {submissionState.error && (
                                    <div style={{ marginTop: "0.75rem" }}>
                                        <strong style={{ display: "block", marginBottom: "0.35rem" }}>Error trace</strong>
                                        {showError ? (
                                            <div className="error-preview" role="region">
                                                <pre style={{ whiteSpace: "pre-wrap", margin: 0 }}>{submissionState.error}</pre>
                                            </div>
                                        ) : (
                                            <div className="error-preview" style={{ maxHeight: "3rem" }}>
                                                <pre style={{ whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis", margin: 0 }}>{submissionState.error}</pre>
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>
                        )}
                    </section>
                </main>
            </div>
        </div>
    );
}

export default App;