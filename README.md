# Smart Document Q&A - Agentic AI LLM Integration

> A modern full-stack application demonstrating advanced LLM integration with document intelligence, built with Spring Boot and React.

[![Java](https://img.shields.io/badge/Java-17+-blue?style=flat-square&logo=java)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3-green?style=flat-square&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18+-blue?style=flat-square&logo=react)](https://react.dev)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue?style=flat-square&logo=docker)](https://www.docker.com)

## 🎯 Project Overview

Smart Document Q&A is a full-stack application that enables users to upload documents and ask natural language questions about their content. The system leverages Claude/OpenAI APIs to understand documents semantically and provide accurate, context-aware answers.

### Key Features

✨ **Core Features**
- 📄 Multi-format document support (PDF, DOCX, TXT, MD)
- 🤖 AI-powered semantic understanding with Claude/OpenAI
- 💬 Natural language question answering
- 🔄 Intelligent retry logic with exponential backoff
- 📊 Structured JSON responses
- 🎨 Modern React UI with real-time feedback
- 🐳 Docker containerization for easy deployment

### Technical Highlights

🏗️ **Architecture**
- **Backend**: Spring Boot 3 with reactive capabilities
- **AI Integration**: Claude API with fallback to OpenAI
- **Document Processing**: Smart chunking with semantic awareness
- **Frontend**: React 18 with Tailwind CSS
- **Containerization**: Multi-stage Docker builds
- **Error Handling**: Comprehensive retry mechanisms and validation

## 🏃 Quick Start

### Prerequisites
- Docker & Docker Compose (recommended)
- Or: Java 17+, Node.js 18+, npm/yarn
- OpenAI/Claude API key

### Option 1: Docker (Recommended)

```bash
# Clone repository
git clone https://github.com/yourusername/smart-document-qa.git
cd smart-document-qa

# Create .env file
cat > .env << EOF
OPENAI_API_KEY=sk-...
CLAUDE_API_KEY=sk-anthropic-...
EOF

# Start application
docker-compose up -d

# Access application
open http://localhost:3000
```

### Option 2: Manual Setup

**Backend:**
```bash
cd backend
export OPENAI_API_KEY=sk-...
export CLAUDE_API_KEY=sk-anthropic-...
./mvnw spring-boot:run
# Backend available at http://localhost:8080
```

**Frontend:**
```bash
cd frontend
npm install
npm start
# Frontend available at http://localhost:3000
```

## 📚 API Documentation

### Upload Document
```http
POST /api/documents/upload
Content-Type: multipart/form-data

Request:
- file: File (PDF, DOCX, TXT, MD)

Response:
{
  "documentId": "doc_abc123xyz",
  "fileName": "report.pdf",
  "pageCount": 25,
  "chunks": 45,
  "processingTime": 2345,
  "status": "processed"
}
```

### Ask Question
```http
POST /api/qa/ask

Request Body:
{
  "documentId": "doc_abc123xyz",
  "question": "What are the main findings?",
  "maxTokens": 500
}

Response:
{
  "questionId": "q_123abc789",
  "question": "What are the main findings?",
  "answer": "The analysis reveals...",
  "confidence": 0.92,
  "sources": [
    {"chunk": 5, "text": "...", "relevanceScore": 0.95}
  ],
  "processingTime": 1234,
  "model": "claude-3-sonnet-20240229"
}
```

### List Documents
```http
GET /api/documents

Response:
{
  "documents": [
    {
      "documentId": "doc_abc123xyz",
      "fileName": "report.pdf",
      "uploadedAt": "2024-01-15T10:30:00Z",
      "status": "processed"
    }
  ],
  "totalCount": 5
}
```

### Delete Document
```http
DELETE /api/documents/{documentId}

Response:
{
  "success": true,
  "message": "Document deleted successfully"
}
```

## 🏗️ Architecture

### Backend Architecture

```
Backend (Spring Boot 3)
├── Controller Layer
│   ├── DocumentController (Upload, List, Delete)
│   └── QAController (Ask, History)
├── Service Layer
│   ├── DocumentProcessingService (Chunking, Storage)
│   ├── LLMService (Claude/OpenAI Integration)
│   └── RetryService (Exponential Backoff)
├── Model Layer
│   ├── Document
│   ├── DocumentChunk
│   └── QAResponse
└── Configuration
    ├── OpenAI Configuration
    ├── Error Handling
    └── Logging
```

### Document Chunking Strategy

1. **Smart Splitting**: Semantic-aware chunking (500-1000 tokens)
2. **Context Preservation**: Maintain sentence boundaries
3. **Overlap**: 20% overlap between chunks for context
4. **Metadata**: Preserve original position and source

### Retry Logic

```
Initial Request
    ↓
Rate Limit? → Wait 2s → Retry (Max 3)
    ↓
Timeout? → Wait 1s → Retry (Max 3)
    ↓
Error? → Fallback to Alternative API
```

## 💻 Tech Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Language**: Java 17
- **Build**: Maven
- **APIs**: OpenAI SDK, Anthropic SDK
- **Storage**: In-memory (upgradeable to PostgreSQL)
- **Logging**: SLF4J, Logback

### Frontend
- **Framework**: React 18
- **Styling**: Tailwind CSS
- **HTTP**: Axios
- **State**: React Hooks
- **UI Components**: Custom + Tailwind

### DevOps
- **Containerization**: Docker
- **Orchestration**: Docker Compose
- **CI/CD**: GitHub Actions ready

## 🎓 What Recruiters Will See

✅ **Best Practices**
- Clean, well-documented code
- Proper error handling and logging
- Retry logic with exponential backoff
- Structured API responses
- Security considerations (API key management)
- Docker containerization

✅ **Production-Ready Features**
- Rate limit handling
- Fallback mechanisms
- Comprehensive error messages
- Performance monitoring
- API documentation

✅ **Modern Stack**
- Latest Spring Boot 3
- Java 17+ features
- React 18 with hooks
- Docker & containerization

## 📂 Project Structure

```
smart-document-qa/
├── backend/
│   ├── src/
│   │   ├── main/java/com/smartqa/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── model/
│   │   │   ├── exception/
│   │   │   ├── config/
│   │   │   └── Application.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── logback.xml
│   ├── pom.xml
│   └── Dockerfile
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   │   ├── DocumentUpload.jsx
│   │   │   ├── QAInterface.jsx
│   │   │   └── DocumentList.jsx
│   │   ├── pages/
│   │   ├── services/
│   │   └── App.jsx
│   ├── package.json
│   └── Dockerfile
├── docker-compose.yml
├── .env.example
└── README.md
```

## 🚀 Deployment

### Cloud Deployment Examples

**AWS ECS**
```bash
# Build and push to ECR
aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_URI
docker build -t smart-qa:latest .
docker tag smart-qa:latest $ECR_URI/smart-qa:latest
docker push $ECR_URI/smart-qa:latest
```

**Google Cloud Run**
```bash
gcloud run deploy smart-qa \
  --source . \
  --platform managed \
  --region us-central1
```

## 🔒 Security Considerations

- API keys managed via environment variables
- No secrets in version control
- CORS properly configured
- Input validation and sanitization
- Rate limiting ready to implement
- Error messages don't expose sensitive data

## 📊 Performance

- Document processing: < 5s for 25-page PDF
- Question answering: < 3s for most queries
- Concurrent request handling with thread pools
- Memory efficient chunking strategy

## 🤝 Contributing

This is a personal portfolio project. Feel free to fork and customize!

## 📝 License

MIT License - See LICENSE file

## 👤 Author

Created as a portfolio project demonstrating:
- Full-stack development (Spring Boot + React)
- LLM API integration and best practices
- Production-ready code quality
- DevOps and containerization
- System design and architecture

---

**Questions or improvements?** Open an issue or submit a pull request!
