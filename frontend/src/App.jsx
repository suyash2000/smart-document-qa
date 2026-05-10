import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';
import DocumentUpload from './components/DocumentUpload';
import DocumentList from './components/DocumentList';
import QAInterface from './components/QAInterface';

function App() {
  const [documents, setDocuments] = useState([]);
  const [selectedDocument, setSelectedDocument] = useState(null);
  const [activeTab, setActiveTab] = useState('upload');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

  useEffect(() => {
    loadDocuments();
  }, []);

  const loadDocuments = async () => {
    try {
      setLoading(true);
      const response = await axios.get(`${API_BASE_URL}/documents`);
      setDocuments(response.data.documents || []);
      setError(null);
    } catch (err) {
      setError('Failed to load documents');
      console.error('Error loading documents:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDocumentUpload = async (file) => {
    try {
      setLoading(true);
      const formData = new FormData();
      formData.append('file', file);

      const response = await axios.post(`${API_BASE_URL}/documents/upload`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });

      // Add new document to list
      const newDoc = {
        documentId: response.data.documentId,
        fileName: response.data.fileName,
        uploadedAt: response.data.uploadedAt,
        status: response.data.status,
      };

      setDocuments([...documents, newDoc]);
      setSelectedDocument(newDoc);
      setActiveTab('qa');
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to upload document');
      console.error('Error uploading document:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteDocument = async (documentId) => {
    try {
      setLoading(true);
      await axios.delete(`${API_BASE_URL}/documents/${documentId}`);
      setDocuments(documents.filter(doc => doc.documentId !== documentId));
      if (selectedDocument?.documentId === documentId) {
        setSelectedDocument(null);
      }
      setError(null);
    } catch (err) {
      setError('Failed to delete document');
      console.error('Error deleting document:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app-container">
      <header className="app-header">
        <div className="header-content">
          <h1>🤖 Smart Document Q&A</h1>
          <p className="subtitle">Agentic AI LLM Integration - Ask questions about your documents</p>
        </div>
      </header>

      {error && (
        <div className="error-banner">
          <span>{error}</span>
          <button onClick={() => setError(null)}>×</button>
        </div>
      )}

      <main className="app-main">
        <div className="tabs">
          <button
            className={`tab ${activeTab === 'upload' ? 'active' : ''}`}
            onClick={() => setActiveTab('upload')}
          >
            📄 Upload Document
          </button>
          <button
            className={`tab ${activeTab === 'list' ? 'active' : ''}`}
            onClick={() => setActiveTab('list')}
          >
            📋 My Documents ({documents.length})
          </button>
          {selectedDocument && (
            <button
              className={`tab ${activeTab === 'qa' ? 'active' : ''}`}
              onClick={() => setActiveTab('qa')}
            >
              💬 Ask Questions
            </button>
          )}
        </div>

        <div className="tab-content">
          {activeTab === 'upload' && (
            <DocumentUpload onUpload={handleDocumentUpload} loading={loading} />
          )}

          {activeTab === 'list' && (
            <DocumentList
              documents={documents}
              onSelect={(doc) => {
                setSelectedDocument(doc);
                setActiveTab('qa');
              }}
              onDelete={handleDeleteDocument}
              loading={loading}
            />
          )}

          {activeTab === 'qa' && selectedDocument && (
            <QAInterface
              document={selectedDocument}
              apiBaseUrl={API_BASE_URL}
              loading={loading}
              setLoading={setLoading}
            />
          )}
        </div>
      </main>

      <footer className="app-footer">
        <p>Smart Document Q&A v1.0 | Built with Spring Boot + React | Powered by Claude/OpenAI</p>
      </footer>
    </div>
  );
}

export default App;
