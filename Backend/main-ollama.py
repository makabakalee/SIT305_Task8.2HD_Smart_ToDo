from flask import Flask, request, Response
import requests
import argparse
import json

app = Flask(__name__)

# Ollama API endpoint and model configuration
OLLAMA_API_URL = "http://localhost:11434/api/generate"
MODEL = "llama3.2:latest"

# System prompt template for task parsing
SYSTEM_PROMPT = """
You are a helpful assistant that organizes tasks. Your only job is to extract task information from user input.

INSTRUCTIONS:
1. Analyze the user's input about a task they want to add.
2. Extract these THREE pieces of information:
   - Task Name: The main action or task
   - Category: MUST be one of: "Work", "Personal", "Study", "Shopping", "Health", or "Other"
   - Time: IMPORTANT! Pay close attention to time indicators:
     * If "tomorrow" is mentioned, use "Tomorrow" as the time
     * If "today" is mentioned, use "Today" as the time
     * If morning/afternoon/evening/night is mentioned, use that time period
     * Only use "Anytime" if NO time information is provided

RESPONSE FORMAT:
You must respond with ONLY a valid JSON object with this structure:
{
  "task": "extracted task name",
  "category": "extracted category",
  "time": "extracted time"
}

EXAMPLES:
User: "Buy milk from the store"
Response: {"task": "Buy milk from the store", "category": "Shopping", "time": "Anytime"}

User: "Complete math homework by tomorrow evening"
Response: {"task": "Complete math homework", "category": "Study", "time": "Tomorrow evening"}

User: "Call John about the project at 3pm"
Response: {"task": "Call John about the project", "category": "Work", "time": "3pm"}

User: "Cook tomorrow"
Response: {"task": "Cook", "category": "Personal", "time": "Tomorrow"}

Remember: Only output the JSON object with the extracted information, nothing else.
"""

def check_ollama_server():
    """Check if the Ollama server is running."""
    try:
        response = requests.get("http://localhost:11434")
        if response.status_code == 200:
            print("Ollama server is running.")
            return True
        else:
            print("Ollama server is not responding correctly.")
            return False
    except requests.ConnectionError:
        print("Error: Could not connect to Ollama server. Ensure it is running with 'ollama serve'.")
        return False

@app.route('/')
def index():
    return "Welcome to the Smart ToDo API powered by Ollama!"

@app.route('/chat', methods=['POST'])
def chat():
    # Get userMessage from form data or raw body
    user_message = request.form.get('userMessage') or request.get_data(as_text=True).strip()

    # Validate userMessage
    if not user_message:
        return Response("Error: userMessage cannot be empty", status=400, mimetype='text/plain')

    # Print received request
    print("\nReceived Request:")
    print(f"userMessage: {user_message}")

    # Create a full prompt with system instructions
    full_prompt = f"{SYSTEM_PROMPT}\n\nUser: \"{user_message}\"\nResponse:"

    # Prepare payload for Ollama API
    payload = {
        "model": MODEL,
        "prompt": full_prompt,
        "stream": False,
        "options": {
            "temperature": 0.15,
            "top_p": 0.95,
            "num_predict": 300,
            "stop": ["\n\n", "User:"]
        }
    }

    # Send request to Ollama API
    try:
        response = requests.post(OLLAMA_API_URL, json=payload)
        print(f"Ollama Response Status: {response.status_code}")
        print(f"Ollama Response Text: {response.text}")
        response.raise_for_status()
        result = response.json()
        raw_output = result.get("response", "").strip()
    except requests.RequestException as e:
        print(f"Error during Ollama API call: {str(e)}")
        raw_output = ""

    # Print raw output
    print(f"Raw Model Output: {raw_output}")

    # Try to validate the response as JSON
    try:
        # Find JSON object in the response
        json_start = raw_output.find('{')
        json_end = raw_output.rfind('}') + 1
        
        if json_start >= 0 and json_end > json_start:
            json_str = raw_output[json_start:json_end]
            json_response = json.loads(json_str)
            
            # Ensure all required fields are present
            if not all(key in json_response for key in ["task", "category", "time"]):
                raise ValueError("Missing required fields in JSON response")
                
            return Response(json.dumps(json_response), mimetype='application/json')
    except (json.JSONDecodeError, ValueError) as e:
        print(f"Failed to parse JSON response: {str(e)}")
        # Fall back to returning raw text
        pass
        
    # Return plain text response if JSON parsing failed
    return Response(raw_output, mimetype='text/plain')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--port', type=int, default=5000, help='Specify the port number')
    args = parser.parse_args()

    port_num = args.port
    if check_ollama_server():
        print(f"App running on port {port_num}")
        app.run(host='0.0.0.0', port=port_num)
    else:
        print("Exiting due to Ollama server unavailability.")