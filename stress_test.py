import concurrent.futures
import os
import random
import time
from collections import Counter, deque

import requests

# Simple configuration (edit values here)
API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")
API_URL = os.getenv("API_URL", f"{API_BASE_URL}/api/submissions")
AUTH_LOGIN_URL = os.getenv("AUTH_LOGIN_URL", f"{API_BASE_URL}/api/auth/login")
AUTH_REGISTER_URL = os.getenv("AUTH_REGISTER_URL", f"{API_BASE_URL}/api/auth/register")
PROBLEM_ID = int(os.getenv("PROBLEM_ID", "1"))
USERS = int(os.getenv("USERS", "5"))
CONCURRENCY = int(os.getenv("CONCURRENCY", "10"))
DURATION_SEC = int(os.getenv("DURATION_SEC", "30"))
POLL_INTERVAL = float(os.getenv("POLL_INTERVAL", "0.5"))
SUBMIT_INTERVAL = float(os.getenv("SUBMIT_INTERVAL", "0.1"))
POLL_TIMEOUT_SEC = int(os.getenv("POLL_TIMEOUT_SEC", "60"))
AUTH_EMAIL = os.getenv("AUTH_EMAIL", "demo@example.com")
AUTH_PASSWORD = os.getenv("AUTH_PASSWORD", "password123")
AUTH_USERNAME = os.getenv("AUTH_USERNAME", "demo_user")
AUTO_REGISTER = os.getenv("AUTO_REGISTER", "true").lower() == "true"


PROGRAMS = {
    "python": {
        "success": """
x = int(input().strip())
print(x + 1)
""",
        "tle": """
while True:
    pass
""",
        "mle": """
a = [0] * (10**8)
print(len(a))
""",
        "compile_error": """
def main()
    print("missing colon")
""",
    },
    "java": {
        "success": """
import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int x = sc.nextInt();
        System.out.println(x + 1);
    }
}
""",
        "tle": """
public class Main {
    public static void main(String[] args) {
        while (true) {}
    }
}
""",
        "mle": """
public class Main {
    public static void main(String[] args) {
        int[] a = new int[300_000_000];
        System.out.println(a.length);
    }
}
""",
        "compile_error": """
public class Main {
    public static void main(String[] args) {
        System.out.println("missing semicolon")
    }
}
""",
    },
    "cpp": {
        "success": """
#include <bits/stdc++.h>
using namespace std;

int main() {
    long long x;
    if (!(cin >> x)) return 0;
    cout << (x + 1) << "\n";
    return 0;
}
""",
        "tle": """
#include <bits/stdc++.h>
using namespace std;

int main() {
    while (true) {}
    return 0;
}
""",
        "mle": """
#include <bits/stdc++.h>
using namespace std;

int main() {
    vector<int> a(300000000);
    cout << a.size() << "\n";
    return 0;
}
""",
        "compile_error": """
#include <bits/stdc++.h>
using namespace std;

int main() {
    cout << "missing semicolon" << endl
    return 0;
}
""",
    },
}

CASE_WEIGHTS = {
    "success": 0.70,
    "tle": 0.10,
    "mle": 0.10,
    "compile_error": 0.10,
}


def pick_case():
    cases = list(CASE_WEIGHTS.keys())
    weights = list(CASE_WEIGHTS.values())
    return random.choices(cases, weights=weights, k=1)[0]


def submit_job(session, api_url, problem_id, language, case_name, code):
    payload = {
        "language": language,
        "code": code,
        "problemId": problem_id,
    }

    start = time.time()
    response = session.post(api_url, json=payload, timeout=15)
    latency_ms = int((time.time() - start) * 1000)

    if response.status_code != 200:
        return {
            "ok": False,
            "error": f"HTTP {response.status_code}: {response.text}",
            "latency_ms": latency_ms,
        }

    data = response.json()
    return {
        "ok": True,
        "id": data.get("id"),
        "language": language,
        "case": case_name,
        "latency_ms": latency_ms,
    }


def login_and_get_token(session):
    login_payload = {
        "email": AUTH_EMAIL,
        "password": AUTH_PASSWORD,
    }
    resp = session.post(AUTH_LOGIN_URL, json=login_payload, timeout=10)
    if resp.status_code == 200:
        return resp.json().get("token")

    if AUTO_REGISTER:
        register_payload = {
            "username": AUTH_USERNAME,
            "email": AUTH_EMAIL,
            "password": AUTH_PASSWORD,
        }
        reg_resp = session.post(AUTH_REGISTER_URL, json=register_payload, timeout=10)
        if reg_resp.status_code not in (200, 201):
            return None

        resp = session.post(AUTH_LOGIN_URL, json=login_payload, timeout=10)
        if resp.status_code == 200:
            return resp.json().get("token")

    return None


def poll_status(session, api_url, submission_id, poll_interval, timeout_sec):
    deadline = time.time() + timeout_sec
    while time.time() < deadline:
        resp = session.get(f"{api_url}/{submission_id}", timeout=10)
        if resp.status_code != 200:
            return {"ok": False, "error": f"HTTP {resp.status_code}: {resp.text}"}

        data = resp.json()
        if data.get("status") == "COMPLETED":
            return {
                "ok": True,
                "verdict": data.get("verdict"),
                "error": data.get("error"),
            }

        time.sleep(poll_interval)

    return {"ok": False, "error": "Timeout waiting for completion"}


def run_stress_test():
    print("--- STARTING STRESS TEST ---")
    print(f"API: {API_URL}")
    print(f"Auth login: {AUTH_LOGIN_URL}")
    print(f"Problem ID: {PROBLEM_ID}")
    print(f"Users: {USERS}")
    print(f"Concurrency: {CONCURRENCY}")
    print(f"Duration: {DURATION_SEC}s")
    print("Case mix:", CASE_WEIGHTS)
    print("-" * 50)

    random.seed(42)
    submitted = 0
    completed = 0
    submit_failures = 0
    verdicts = Counter()
    latencies = []
    in_flight = deque()

    start_time = time.time()
    end_time = start_time + DURATION_SEC

    with requests.Session() as session:
        token = login_and_get_token(session)
        if not token:
            print("[ERROR] Could not obtain auth token. Check credentials or disable AUTO_REGISTER.")
            return
        session.headers.update({"Authorization": f"Bearer {token}"})

        with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENCY) as executor:
            futures = set()

            while time.time() < end_time or futures:
                while time.time() < end_time and len(futures) < CONCURRENCY:
                    language = random.choice(list(PROGRAMS.keys()))
                    case_name = pick_case()
                    code = PROGRAMS[language][case_name]
                    future = executor.submit(
                        submit_job,
                        session,
                        API_URL,
                        PROBLEM_ID,
                        language,
                        case_name,
                        code,
                    )
                    futures.add(future)
                    submitted += 1
                    time.sleep(SUBMIT_INTERVAL)

                done, futures = concurrent.futures.wait(
                    futures, timeout=0.1, return_when=concurrent.futures.FIRST_COMPLETED
                )

                for f in done:
                    result = f.result()
                    if not result["ok"]:
                        submit_failures += 1
                        continue
                    latencies.append(result["latency_ms"])
                    if result.get("id"):
                        in_flight.append(result["id"])

            poll_deadline = time.time() + POLL_TIMEOUT_SEC
            while in_flight and time.time() < poll_deadline:
                submission_id = in_flight.popleft()
                poll = poll_status(session, API_URL, submission_id, POLL_INTERVAL, POLL_TIMEOUT_SEC)
                if poll["ok"]:
                    completed += 1
                    verdicts[poll.get("verdict") or "UNKNOWN"] += 1
                else:
                    verdicts["POLL_FAILED"] += 1

    total_time = time.time() - start_time
    avg_latency = int(sum(latencies) / len(latencies)) if latencies else 0
    p95_latency = int(sorted(latencies)[int(len(latencies) * 0.95) - 1]) if latencies else 0

    print("-" * 50)
    print(f"Submitted: {submitted}")
    print(f"Submit failures: {submit_failures}")
    print(f"Completed: {completed}")
    print(f"Duration: {total_time:.2f}s")
    print(f"Avg submit latency: {avg_latency} ms")
    print(f"P95 submit latency: {p95_latency} ms")
    print("Verdicts:")
    for k, v in verdicts.items():
        print(f"  {k}: {v}")


if __name__ == "__main__":
    run_stress_test()