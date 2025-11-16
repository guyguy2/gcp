import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

function SnippetManager() {
  const [snippets, setSnippets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [filter, setFilter] = useState('all');
  const [formData, setFormData] = useState({
    title: '',
    code: '',
    language: '',
    tags: '',
    category: '',
    isPublic: false,
    description: ''
  });

  useEffect(() => {
    fetchSnippets();
  }, [filter]);

  const fetchSnippets = async () => {
    try {
      setLoading(true);
      let url = `${API_URL}/snippets`;
      if (filter === 'public') {
        url = `${API_URL}/snippets/public`;
      }
      const response = await axios.get(url);
      setSnippets(response.data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch snippets: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const snippetData = {
        ...formData,
        tags: formData.tags.split(',').map(tag => tag.trim()).filter(tag => tag)
      };
      await axios.post(`${API_URL}/snippets`, snippetData);
      setFormData({
        title: '',
        code: '',
        language: '',
        tags: '',
        category: '',
        isPublic: false,
        description: ''
      });
      setShowForm(false);
      fetchSnippets();
    } catch (err) {
      setError('Failed to create snippet: ' + err.message);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this snippet?')) {
      try {
        await axios.delete(`${API_URL}/snippets/${id}`);
        fetchSnippets();
      } catch (err) {
        setError('Failed to delete snippet: ' + err.message);
      }
    }
  };

  const formatDate = (timestamp) => {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp.seconds * 1000);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
  };

  if (loading) {
    return <div className="loading">Loading snippets...</div>;
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h1>Code Snippets</h1>
        <div style={{ display: 'flex', gap: '1rem' }}>
          <select
            className="input"
            style={{ width: 'auto' }}
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          >
            <option value="all">All Snippets</option>
            <option value="public">Public Only</option>
          </select>
          <button className="button" onClick={() => setShowForm(!showForm)}>
            {showForm ? 'Cancel' : 'Add New Snippet'}
          </button>
        </div>
      </div>

      {error && <div className="error">{error}</div>}

      {showForm && (
        <div className="card">
          <h2>Add New Code Snippet</h2>
          <form onSubmit={handleSubmit}>
            <input
              className="input"
              type="text"
              placeholder="Title"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              required
            />
            <textarea
              className="textarea"
              placeholder="Code"
              value={formData.code}
              onChange={(e) => setFormData({ ...formData, code: e.target.value })}
              required
              style={{ minHeight: '200px' }}
            />
            <input
              className="input"
              type="text"
              placeholder="Language (e.g., javascript, python, java)"
              value={formData.language}
              onChange={(e) => setFormData({ ...formData, language: e.target.value })}
              required
            />
            <input
              className="input"
              type="text"
              placeholder="Tags (comma-separated)"
              value={formData.tags}
              onChange={(e) => setFormData({ ...formData, tags: e.target.value })}
            />
            <input
              className="input"
              type="text"
              placeholder="Category (e.g., algorithms, utilities)"
              value={formData.category}
              onChange={(e) => setFormData({ ...formData, category: e.target.value })}
            />
            <textarea
              className="textarea"
              placeholder="Description (optional)"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              style={{ minHeight: '100px' }}
            />
            <div style={{ marginBottom: '1rem' }}>
              <label>
                <input
                  type="checkbox"
                  checked={formData.isPublic}
                  onChange={(e) => setFormData({ ...formData, isPublic: e.target.checked })}
                />
                {' '}Make this snippet public (visible in portfolio)
              </label>
            </div>
            <button className="button" type="submit">Create Snippet</button>
          </form>
        </div>
      )}

      <div>
        {snippets.length === 0 ? (
          <div className="card">
            <p>No snippets found. Click "Add New Snippet" to get started!</p>
          </div>
        ) : (
          snippets.map((snippet) => (
            <div key={snippet.id} className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: '1rem' }}>
                <div style={{ flex: 1 }}>
                  <h3>{snippet.title}</h3>
                  <div style={{ display: 'flex', gap: '1rem', marginTop: '0.5rem', fontSize: '0.9rem', color: '#5f6368' }}>
                    <span>Language: {snippet.language}</span>
                    {snippet.category && <span>Category: {snippet.category}</span>}
                    {snippet.isPublic && <span style={{ color: '#137333' }}>Public</span>}
                  </div>
                  {snippet.tags && snippet.tags.length > 0 && (
                    <div style={{ marginTop: '0.5rem' }}>
                      {snippet.tags.map((tag, index) => (
                        <span
                          key={index}
                          style={{
                            background: '#e8f0fe',
                            color: '#1a73e8',
                            padding: '0.25rem 0.5rem',
                            borderRadius: '4px',
                            marginRight: '0.5rem',
                            fontSize: '0.85rem'
                          }}
                        >
                          {tag}
                        </span>
                      ))}
                    </div>
                  )}
                  {snippet.description && (
                    <p style={{ marginTop: '0.5rem', color: '#5f6368' }}>{snippet.description}</p>
                  )}
                </div>
                <button
                  className="button button-danger"
                  onClick={() => handleDelete(snippet.id)}
                  style={{ marginLeft: '1rem' }}
                >
                  Delete
                </button>
              </div>
              <pre style={{
                background: '#f5f5f5',
                padding: '1rem',
                borderRadius: '4px',
                overflow: 'auto',
                fontSize: '0.9rem'
              }}>
                <code>{snippet.code}</code>
              </pre>
              <div style={{ marginTop: '0.5rem', fontSize: '0.85rem', color: '#999' }}>
                Created: {formatDate(snippet.createdAt)}
                {snippet.updatedAt && ` | Updated: ${formatDate(snippet.updatedAt)}`}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

export default SnippetManager;
