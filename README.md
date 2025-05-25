# Smart ToDo - Intelligent Task Management App Based on Llama 2

## Project Overview

Smart ToDo is an intelligent task management Android application integrated with the Llama 2 large language model. The app allows users to input tasks through natural language (voice or text), and the system automatically understands and intelligently categorizes, sets priorities, and provides personalized task suggestions.

## Core Features

### 1. Natural Language Task Input
- Supports voice input and text input
- Automatically recognizes task content, category, and time information
- Intelligently parses complex natural language expressions

### 2. Intelligent Classification System
- Automatically categorizes tasks into 6 categories: Work, Personal, Study, Shopping, Health, Other
- Intelligent classification based on keywords and context
- Supports manual category adjustment

### 3. Intelligent Task Suggestions
- Recommends priority tasks based on multi-factor algorithms
- Considers task importance, deadlines, categories, and other factors
- Provides detailed recommendation reasons

### 4. Natural Language Queries
- Supports natural language queries for task information
- Can inquire about today's tasks, important tasks, specific category tasks, etc.
- Intelligently understands user query intentions

### 5. Data Synchronization
- Local SQLite database storage
- MongoDB cloud data synchronization
- Offline mode support

## Technical Architecture

### Frontend (Android Application)
- Development Language: Java
- Minimum SDK Version: 24 (Android 7.0)
- Target SDK Version: 35
- UI Framework: Material Design
- Database: SQLite + MongoDB

### Backend (AI Service)
- Development Language: Python
- Web Framework: Flask
- AI Model: Llama 3.2 or Llama 2 (deployed via Ollama)
- API Interface: RESTful

### Core Components

#### Android Side
- MainActivity: Main interface controller, handles user interactions
- ApiService: Responsible for communication with backend AI service
- TaskSuggestionService: Intelligent recommendation algorithm implementation
- TaskQueryService: Natural language query processing
- MongoDBTaskManager: Cloud data synchronization management
- TaskDatabase: Local database operations

#### Backend
- main-ollama.py: Flask server, integrates Ollama API
- System Prompts: Specially designed task parsing prompts
- JSON Response Processing: Structured task data output

## Installation and Deployment

### Environment Requirements

#### Android Side
- Android Studio Arctic Fox or higher
- Java 11 or higher
- Android SDK 24+

#### Backend
- Python 3.8+
- Ollama
- Llama 2 or Llama 3.2 model

### Installation Steps

#### 1. Backend Deployment

Install Python dependencies:
```
pip install -r BackendTask8.1C/requirements.txt
```

Install and configure Ollama:
```
# Install Ollama
curl -fsSL https://ollama.ai/install.sh | sh

# Download Llama model
ollama pull llama3.2:latest or ollama pull llama2:latest

# Start Ollama service
ollama serve
```

Start Flask server:
```
cd BackendTask8.1C
python main-ollama.py --port 5000
```

#### 2. Android Application

1. Open Smart_ToDo project with Android Studio
2. Sync Gradle dependencies
3. Configure MongoDB connection (optional)
4. Compile and run the application

### Configuration Instructions

#### API Endpoint Configuration
Default API endpoint: http://10.0.2.2:5000/chat (Android emulator)
For actual devices, modify to the server's actual IP address

#### MongoDB Configuration
Configure MongoDB connection string in MongoDBConfig.java:
```java
private static final String CONNECTION_STRING = "your_mongodb_connection_string";
private static final String DATABASE_NAME = "smart_todo";
private static final String COLLECTION_NAME = "tasks";
```

## User Guide

### Basic Operations

#### Adding Tasks
1. Voice input: Click the microphone button and speak the task content
2. Text input: Enter task description in the input field
3. System automatically parses and categorizes tasks

#### Viewing Tasks
- All tasks: View all task lists
- Today's tasks: View today's tasks
- Important tasks: View tasks marked as important

#### Getting Suggestions
Click the suggestion button or ask "Give me some suggestions" to get intelligent recommendations

#### Querying Tasks
Use natural language queries, for example:
- "What tasks do I have today?"
- "How many important tasks do I have?"
- "Show work category tasks"

### Advanced Features

#### Task Management
- Mark tasks as completed
- Edit task content
- Delete tasks
- Set task importance

#### Intelligent Classification
The system automatically classifies based on the following keywords:
- Work: meeting, project, report, client
- Study: homework, study, exam, assignment
- Shopping: buy, shopping, groceries, purchase
- Health: exercise, doctor, gym, medicine
- Personal: family, personal, call, visit

## API Documentation

### Task Processing Interface

#### POST /chat
Process natural language task input

Request format:
```
Content-Type: text/plain
Body: User input task description
```

Response format:
```json
{
  "task": "Task name",
  "category": "Task category",
  "time": "Time information"
}
```

Supported categories:
- Work
- Personal
- Study
- Shopping
- Health
- Other

