import argparse
import concurrent.futures
import os
import random
import time
from collections import Counter, deque

import requests

# CONFIGURATION (override via CLI or env)
DEFAULT_API_URL = os.getenv("API_URL", "http://localhost:8080/api/submit")
DEFAULT_PROBLEM_ID = int(os.getenv("PROBLEM_ID", "1"))
DEFAULT_INPUT = os.getenv("TEST_INPUT", "41\n")
DEFAULT_USERS = int(os.getenv("USERS", "5"))
DEFAULT_CONCURRENCY = int(os.getenv("CONCURRENCY", "10"))
DEFAULT_DURATION = int(os.getenv("DURATION_SEC", "30"))
DEFAULT_POLL_INTERVAL = float(os.getenv("POLL_INTERVAL", "0.5"))
DEFAULT_SUBMIT_INTERVAL = float(os.getenv("SUBMIT_INTERVAL", "0.1"))


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


def submit_job(session, api_url, problem_id, username, language, case_name, code, test_input):
    payload = {
        "language": language,
        "code": code,
        "input": test_input,
        "problemId": problem_id,
        "username": username,
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


def run_stress_test(args):
    print("--- STARTING STRESS TEST ---")
    print(f"API: {args.api_url}")
    print(f"Problem ID: {args.problem_id}")
    print(f"Users: {args.users}")
    print(f"Concurrency: {args.concurrency}")
    print(f"Duration: {args.duration_sec}s")
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
    end_time = start_time + args.duration_sec

    with requests.Session() as session:
        with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as executor:
            futures = set()

            while time.time() < end_time or futures:
                while time.time() < end_time and len(futures) < args.concurrency:
                    language = random.choice(list(PROGRAMS.keys()))
                    case_name = pick_case()
                    code = PROGRAMS[language][case_name]
                    username = f"user_{random.randint(1, args.users)}"

                    future = executor.submit(
                        submit_job,
                        session,
                        args.api_url,
                        args.problem_id,
                        username,
                        language,
                        case_name,
                        code,
                        args.test_input,
                    )
                    futures.add(future)
                    submitted += 1
                    time.sleep(args.submit_interval)

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

            poll_deadline = time.time() + args.poll_timeout_sec
            while in_flight and time.time() < poll_deadline:
                submission_id = in_flight.popleft()
                poll = poll_status(session, args.api_url, submission_id, args.poll_interval, args.poll_timeout_sec)
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


def parse_args():
    parser = argparse.ArgumentParser(description="Distributed Code Judge Stress Test")
    parser.add_argument("--api-url", default=DEFAULT_API_URL)
    parser.add_argument("--problem-id", type=int, default=DEFAULT_PROBLEM_ID)
    parser.add_argument("--test-input", default=DEFAULT_INPUT)
    parser.add_argument("--users", type=int, default=DEFAULT_USERS)
    parser.add_argument("--concurrency", type=int, default=DEFAULT_CONCURRENCY)
    parser.add_argument("--duration-sec", type=int, default=DEFAULT_DURATION)
    parser.add_argument("--submit-interval", type=float, default=DEFAULT_SUBMIT_INTERVAL)
    parser.add_argument("--poll-interval", type=float, default=DEFAULT_POLL_INTERVAL)
    parser.add_argument("--poll-timeout-sec", type=int, default=60)
    return parser.parse_args()


if __name__ == "__main__":
    run_stress_test(parse_args())