import axios from "axios";

const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL || "http://localhost:8080/api",
    headers: {
        "Content-Type": "application/json",
    },
    withCredentials: true,
});

export const getProblems = async () => {
    try {
        const res = await api.get("/problems/");
        return res.data;
    } catch (error) {
        console.error("Error fetching problems:", error);
        throw error;
    }
};

export const submitCode = async (submission) => {
    try {
        const res = await api.post("/submissions", submission);
        return res.data;

    } catch (error) {
        console.error("Error submitting code:", error);
        throw error;
    }
};

export const getSubmissionStatus = async (submissionId) => {
    try {
        const res = await api.get(`/submissions/${submissionId}`);
        return res.data;
    } catch (error) {
        console.error("Error fetching submission status:", error);
        throw error;
    }
};

export const getProblemDetails = async (problemId) => {
    try {
        const res = await api.get(`/problems/${problemId}`);
        return res.data;
    } catch (error) {
        console.error("Error fetching problem details:", error);
        throw error;
    }
};

export const registerUser = async (payload) => {
    try {
        const res = await api.post("/auth/register", payload);
        return res.data;
    } catch (error) {
        console.error("Error registering user:", error);
        throw error;
    }
};

export const loginUser = async (payload) => {
    try {
        const res = await api.post("/auth/login", payload);
        return res.data;
    } catch (error) {
        console.error("Error logging in:", error);
        throw error;
    }
};

export const logoutUser = async () => {
    try {
        const res = await api.post("/auth/logout");
        return res.data;
    } catch (error) {
        console.error("Error logging out:", error);
        throw error;
    }
};

export const googleLogin = async (token) => {
    try {
        const res = await api.post("/auth/google", { token });
        return res.data;
    } catch (error) {
        console.error("Error with google login:", error);
        throw error;
    }
};
