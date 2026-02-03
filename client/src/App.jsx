import { useState, useEffect } from "react";
import "./App.css";

function App() {
    const [language, setLanguage] = useState("cpp");
    const [code, setCode] = useState("");
    const [status, setStatus] = useState("");
    const [verdict, setVerdict] = useState("");
    const [subId, setSubId] = useState(null);
    const [loading, setLoading] = useState(false);

    // Boilerplate templates
    const templates = {
        cpp: `#include <iostream>\n\nint main() {\n    int x;\n    std::cin >> x;\n    std::cout << x + 1;\n    return 0;\n}`,
        python: `x = int(input())\nprint(x + 1)`,
        java: `import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        if (scanner.hasNextInt()) {\n            int x = scanner.nextInt();\n            System.out.println(x + 1);\n        }\n        scanner.close();\n    }\n}`
    };

    // Set default code when language changes
    useEffect(() => {
        setCode(templates[language]);
    }, [language]);

    // Polling Logic
    useEffect(() => {
        let interval = null;
        if (subId && status !== "COMPLETED") {
            interval = setInterval(async () => {
                try {
                    const res = await fetch(`http://localhost:8080/api/submissions/${subId}`);
                    const data = await res.json();
                    setStatus(data.status);
                    if (data.status === "COMPLETED") {
                        setVerdict(data.verdict);
                        setLoading(false);
                        clearInterval(interval);
                    }
                } catch (err) {
                    console.error("Polling error:", err);
                }
            }, 1000); // Poll every 1 second
        }
        return () => clearInterval(interval);
    }, [subId, status]);

    const handleSubmit = async () => {
        setLoading(true);
        setStatus("PENDING");
        setVerdict("");

        try {
            const response = await fetch("http://localhost:8080/api/submissions", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    code: code,
                    problemId: 1,
                    username: "test",
                    language: language
                }),
            });

            const data = await response.json();
            setSubId(data.id);

        } catch (error) {
            console.error("Error submitting:", error);
            setLoading(false);
            setStatus("ERROR");
        }
    };

    return (
        <div style={{ padding: "2rem", fontFamily: "Arial", maxWidth: "800px", margin: "0 auto" }}>
            <h1>Online Judge</h1>

            <div style={{ marginBottom: "15px" }}>
                <label style={{ fontWeight: "bold", marginRight: "10px" }}>Language:</label>
                <select
                    value={language}
                    onChange={(e) => setLanguage(e.target.value)}
                    style={{ padding: "8px", fontSize: "16px" }}
                >
                    <option value="cpp">C++</option>
                    <option value="python">Python 3</option>
                    <option value="java">Java</option>
                </select>
            </div>

            <div style={{ display: "flex", gap: "20px" }}>
                <textarea
                    rows="20"
                    style={{
                        flex: 1,
                        fontFamily: "monospace",
                        padding: "15px",
                        fontSize: "14px",
                        backgroundColor: "#1e1e1e",
                        color: "#d4d4d4",
                        borderRadius: "5px",
                        border: "1px solid #333"
                    }}
                    value={code}
                    onChange={(e) => setCode(e.target.value)}
                />

                <div style={{ width: "250px" }}>
                    <button
                        onClick={handleSubmit}
                        disabled={loading}
                        style={{
                            width: "100%",
                            padding: "15px",
                            backgroundColor: loading ? "#666" : "#28a745",
                            color: "white",
                            border: "none",
                            borderRadius: "5px",
                            cursor: "pointer",
                            fontSize: "18px",
                            fontWeight: "bold"
                        }}
                    >
                        {loading ? "Judging..." : "Submit Solution"}
                    </button>

                    {status && (
                        <div style={{ marginTop: "20px", padding: "15px", background: "#f8f9fa", borderRadius: "5px", border: "1px solid #ddd" }}>
                            <p style={{ margin: "5px 0" }}><strong>ID:</strong> {subId?.substring(0, 8)}...</p>
                            <p style={{ margin: "5px 0" }}><strong>Status:</strong> {status}</p>
                            {verdict && (
                                <div style={{
                                    marginTop: "10px",
                                    padding: "10px",
                                    textAlign: "center",
                                    backgroundColor: verdict === "ACCEPTED" ? "#d4edda" : "#f8d7da",
                                    color: verdict === "ACCEPTED" ? "#155724" : "#721c24",
                                    borderRadius: "4px",
                                    fontWeight: "bold"
                                }}>
                                    {verdict}
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

export default App;