import React, { useRef, useState } from 'react';
import './DocumentUpload.css';

function DocumentUpload({ onUpload, loading }) {
  const fileInputRef = useRef(null);
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState(null);

  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);

    const files = e.dataTransfer.files;
    if (files && files[0]) {
      handleFile(files[0]);
    }
  };

  const handleFileInput = (e) => {
    if (e.target.files && e.target.files[0]) {
      handleFile(e.target.files[0]);
    }
  };

  const handleFile = (file) => {
    const validTypes = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/plain', 'text/markdown'];
    
    if (!validTypes.includes(file.type)) {
      alert('Please upload a PDF, DOCX, TXT, or MD file');
      return;
    }

    if (file.size > 50 * 1024 * 1024) {
      alert('File size must be less than 50MB');
      return;
    }

    setSelectedFile(file);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (selectedFile) {
      onUpload(selectedFile);
      setSelectedFile(null);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  return (
    <div className="upload-container">
      <div className="upload-card">
        <h2>📤 Upload Your Document</h2>
        <p className="upload-info">
          Upload PDF, DOCX, TXT, or Markdown files (max 50MB)
        </p>

        <form onSubmit={handleSubmit}>
          <div
            className={`drop-zone ${dragActive ? 'active' : ''}`}
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
          >
            <input
              ref={fileInputRef}
              type="file"
              onChange={handleFileInput}
              accept=".pdf,.docx,.txt,.md"
              disabled={loading}
              style={{ display: 'none' }}
            />

            {!selectedFile ? (
              <>
                <div className="drop-icon">📁</div>
                <h3>Drag & drop your file here</h3>
                <p>or click to browse</p>
              </>
            ) : (
              <>
                <div className="file-selected">✓</div>
                <h3>{selectedFile.name}</h3>
                <p>{(selectedFile.size / 1024 / 1024).toFixed(2)} MB</p>
              </>
            )}
          </div>

          <button
            type="submit"
            disabled={!selectedFile || loading}
            className="upload-button"
          >
            {loading ? (
              <>
                <span className="spinner"></span>
                Uploading...
              </>
            ) : (
              '🚀 Upload Document'
            )}
          </button>

          {selectedFile && (
            <button
              type="button"
              onClick={() => {
                setSelectedFile(null);
                if (fileInputRef.current) {
                  fileInputRef.current.value = '';
                }
              }}
              className="clear-button"
            >
              Clear Selection
            </button>
          )}
        </form>

        <div className="supported-formats">
          <h4>Supported Formats:</h4>
          <ul>
            <li><strong>PDF</strong> - Best for long documents and reports</li>
            <li><strong>DOCX</strong> - Microsoft Word documents</li>
            <li><strong>TXT</strong> - Plain text files</li>
            <li><strong>MD</strong> - Markdown files</li>
          </ul>
        </div>

        <div className="benefits">
          <h4>How It Works:</h4>
          <ol>
            <li>Upload your document</li>
            <li>System extracts and chunks content intelligently</li>
            <li>Ask natural language questions</li>
            <li>Get AI-powered answers with source references</li>
          </ol>
        </div>
      </div>
    </div>
  );
}

export default DocumentUpload;
