import requests
import time

# CONFIGURATION
API_URL = "http://localhost:8080/api/submit"  # Change to your actual Spring Boot endpoint
PROBLEM_ID = 1
TEST_INPUT = "41\n"
REQUEST_SLEEP = 0.2  # Small delay between submissions


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


def submit_case(language, case_name, code):
    """Sends a single submission to the backend."""
    payload = {
        "language": language,
        "code": code,
        "input": TEST_INPUT,
        "problemId": PROBLEM_ID,
    }

    try:
        response = requests.post(API_URL, json=payload)
        if response.status_code == 200:
            print(f"[{language}][{case_name}] Submitted successfully (Status: {response.status_code})")
        else:
            print(f"[{language}][{case_name}] Failed {response.status_code}: {response.text}")
    except Exception as e:
        print(f"[{language}][{case_name}] Error - {e}")


def run_test_suite():
    print("--- STARTING FUNCTIONAL TEST SUITE ---")
    print("Cases: success, TLE, MLE, compilation error for 3 languages")
    print(f"Problem ID: {PROBLEM_ID}")
    print("-" * 40)

    for language, cases in PROGRAMS.items():
        for case_name, code in cases.items():
            submit_case(language, case_name, code)
            time.sleep(REQUEST_SLEEP)


if __name__ == "__main__":
    run_test_suite()