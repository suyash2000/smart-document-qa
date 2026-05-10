import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './Components.css';

// DocumentList Component
export function DocumentList({ documents, onSelect, onDelete, loading }) {
  if (documents.length === 0) {
    return (
      <div className="empty-state">
        <div className="empty-icon">📭</div>
        <h3>No documents yet</h3>
        <p>Upload a document to get started</p>
      </div>
    );
  }

  return (
    <div className="document-list-container">
      <h2>Your Documents</h2>
      <div className="document-grid">
        {documents.map((doc) => (
          <div key={doc.documentId} className="document-card">
            <div className="doc-icon">📄</div>
            <h3>{doc.fileName}</h3>
            <p className="doc-status">{doc.status}</p>
            <p className="doc-date">
              {new Date(doc.uploadedAt).toLocaleDateString()}
            </p>
            <div className="doc-actions">
              <button
                className="btn-primary"
                onClick={() => onSelect(doc)}
                disabled={loading}
              >
                Ask Questions
              </button>
              <button
                className="btn-danger"
                onClick={() => {
                  if (window.confirm('Delete this document?')) {
                    onDelete(doc.documentId);
                  }
                }}
                disabled={loading}
              >
                Delete
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// QAInterface Component
function QAInterface({ document, apiBaseUrl, loading, setLoading }) {
  const [question, setQuestion] = useState('');
  const [responses, setResponses] = useState([]);
  const [qaLoading, setQaLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadQAHistory();
  }, [document.documentId]);

  const loadQAHistory = async () => {
    try {
      const response = await axios.get(
        `${apiBaseUrl}/qa/history/${document.documentId}`
      );
      setResponses(response.data.history || []);
    } catch (err) {
      console.error('Error loading history:', err);
    }
  };

  const handleAskQuestion = async (e) => {
    e.preventDefault();

    if (!question.trim()) {
      setError('Please enter a question');
      return;
    }

    try {
      setQaLoading(true);
      setError(null);

      const response = await axios.post(`${apiBaseUrl}/qa/ask`, {
        documentId: document.documentId,
        question: question.trim(),
        maxTokens: 500,
      });

      setResponses([response.data, ...responses]);
      setQuestion('');
    } catch (err) {
      setError(
        err.response?.data?.message || 'Failed to get answer'
      );
    } finally {
      setQaLoading(false);
    }
  };

  return (
    <div className="qa-container">
      <div className="qa-header">
        <h2>💬 Ask Questions</h2>
        <p className="document-name">Document: {document.fileName}</p>
      </div>

      {error && (
        <div className="error-message">
          {error}
          <button onClick={() => setError(null)}>×</button>
        </div>
      )}

      <div className="qa-main">
        <div className="qa-input-section">
          <form onSubmit={handleAskQuestion}>
            <div className="input-group">
              <input
                type="text"
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                placeholder="Ask a question about the document..."
                disabled={qaLoading}
                className="qa-input"
              />
              <button
                type="submit"
                disabled={qaLoading || !question.trim()}
                className="qa-submit-btn"
              >
                {qaLoading ? (
                  <>
                    <span className="spinner"></span>
                    Thinking...
                  </>
                ) : (
                  '🚀 Ask'
                )}
              </button>
            </div>
            <p className="input-hint">
              💡 Ask questions like: "What are the main points?", "Summarize the findings", etc.
            </p>
          </form>
        </div>

        <div className="qa-responses">
          {responses.length === 0 ? (
            <div className="no-responses">
              <p>No questions asked yet. Ask one to get started!</p>
            </div>
          ) : (
            responses.map((response) => (
              <div key={response.questionId} className="qa-item">
                <div className="qa-question">
                  <span className="q-icon">❓</span>
                  <strong>{response.question}</strong>
                </div>
                <div className="qa-answer">
                  <span className="a-icon">✓</span>
                  <p>{response.answer}</p>
                  <div className="qa-metadata">
                    <span className="confidence">
                      Confidence: {(response.confidence * 100).toFixed(0)}%
                    </span>
                    <span className="model">Model: {response.model}</span>
                    <span className="time">
                      {response.processingTimeMs}ms
                    </span>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {responses.length > 0 && (
        <button
          className="clear-history-btn"
          onClick={async () => {
            if (window.confirm('Clear all questions for this document?')) {
              try {
                await axios.delete(
                  `${apiBaseUrl}/qa/history/${document.documentId}`
                );
                setResponses([]);
              } catch (err) {
                setError('Failed to clear history');
              }
            }
          }}
        >
          🗑️ Clear History
        </button>
      )}
    </div>
  );
}

export default QAInterface;
