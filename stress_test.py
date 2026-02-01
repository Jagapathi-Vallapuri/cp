import requests
import time
import concurrent.futures

# CONFIGURATION
API_URL = "http://localhost:8080/api/submit"  # Change to your actual Spring Boot endpoint
TOTAL_JOBS = 10
SLEEP_TIME = 3  # How long each job takes (seconds)


CODE_PAYLOAD = f"""
import time
time.sleep({SLEEP_TIME})
print("I finished after {SLEEP_TIME} seconds!")
"""

def submit_job(i):
    """Sends a single submission to the backend."""
    try:
        payload = {
            "language": "python",
            "code": CODE_PAYLOAD,
            "input": "" ,
            "problemId": 1
        }
        
        start = time.time()
        response = requests.post(API_URL, json=payload)
        
        
        if response.status_code == 200:
            print(f"Job {i}: Submitted successfully (Status: {response.status_code})")
        else:
            print(f"Job {i}: Failed {response.text}")
            
    except Exception as e:
        print(f"Job {i}: Error - {e}")

def run_test():
    print(f"--- STARTING STRESS TEST: {TOTAL_JOBS} Jobs ---")
    print(f"Each job sleeps for {SLEEP_TIME} seconds.")
    print(f"Theoretical time with 1 worker: {TOTAL_JOBS * SLEEP_TIME} seconds.")
    
    start_time = time.time()
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=TOTAL_JOBS) as executor:
        futures = [executor.submit(submit_job, i) for i in range(1, TOTAL_JOBS + 1)]
        concurrent.futures.wait(futures)
        
    end_time = time.time()
    total_duration = end_time - start_time
    
    print("-" * 30)
    print(f"Total Test Duration: {total_duration:.2f} seconds")
    
    if total_duration < (TOTAL_JOBS * SLEEP_TIME):
        print("✅ SUCCESS: System is processing in PARALLEL.")
    else:
        print("❌ FAIL: System seems SEQUENTIAL (or overhead is huge).")

if __name__ == "__main__":
    run_test()