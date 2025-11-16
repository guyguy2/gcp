import React, { useState, useEffect } from 'react';
import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

function Portfolio() {
  const [links, setLinks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    url: '',
    order: 0,
    category: '',
    icon: '',
    description: ''
  });

  useEffect(() => {
    fetchLinks();
  }, []);

  const fetchLinks = async () => {
    try {
      setLoading(true);
      const response = await axios.get(`${API_URL}/portfolio`);
      setLinks(response.data);
      setError(null);
    } catch (err) {
      setError('Failed to fetch portfolio links: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await axios.post(`${API_URL}/portfolio`, formData);
      setFormData({
        title: '',
        url: '',
        order: 0,
        category: '',
        icon: '',
        description: ''
      });
      setShowForm(false);
      fetchLinks();
    } catch (err) {
      setError('Failed to create portfolio link: ' + err.message);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this link?')) {
      try {
        await axios.delete(`${API_URL}/portfolio/${id}`);
        fetchLinks();
      } catch (err) {
        setError('Failed to delete link: ' + err.message);
      }
    }
  };

  if (loading) {
    return <div className="loading">Loading portfolio...</div>;
  }

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <h1>Portfolio Links</h1>
        <button className="button" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Cancel' : 'Add New Link'}
        </button>
      </div>

      {error && <div className="error">{error}</div>}

      {showForm && (
        <div className="card">
          <h2>Add New Portfolio Link</h2>
          <form onSubmit={handleSubmit}>
            <input
              className="input"
              type="text"
              placeholder="Title"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              required
            />
            <input
              className="input"
              type="url"
              placeholder="URL"
              value={formData.url}
              onChange={(e) => setFormData({ ...formData, url: e.target.value })}
              required
            />
            <input
              className="input"
              type="number"
              placeholder="Display Order"
              value={formData.order}
              onChange={(e) => setFormData({ ...formData, order: parseInt(e.target.value) })}
              required
            />
            <input
              className="input"
              type="text"
              placeholder="Category (e.g., GitHub, LinkedIn)"
              value={formData.category}
              onChange={(e) => setFormData({ ...formData, category: e.target.value })}
            />
            <input
              className="input"
              type="text"
              placeholder="Icon (optional)"
              value={formData.icon}
              onChange={(e) => setFormData({ ...formData, icon: e.target.value })}
            />
            <textarea
              className="textarea"
              placeholder="Description (optional)"
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
            />
            <button className="button" type="submit">Create Link</button>
          </form>
        </div>
      )}

      <div>
        {links.length === 0 ? (
          <div className="card">
            <p>No portfolio links yet. Click "Add New Link" to get started!</p>
          </div>
        ) : (
          links.map((link) => (
            <div key={link.id} className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
                <div style={{ flex: 1 }}>
                  <h3>{link.title}</h3>
                  {link.category && <span style={{ color: '#5f6368', fontSize: '0.9rem' }}>{link.category}</span>}
                  <p style={{ marginTop: '0.5rem' }}>
                    <a href={link.url} target="_blank" rel="noopener noreferrer" style={{ color: '#1a73e8' }}>
                      {link.url}
                    </a>
                  </p>
                  {link.description && <p style={{ marginTop: '0.5rem', color: '#5f6368' }}>{link.description}</p>}
                  <p style={{ fontSize: '0.85rem', color: '#999', marginTop: '0.5rem' }}>Order: {link.order}</p>
                </div>
                <button
                  className="button button-danger"
                  onClick={() => handleDelete(link.id)}
                  style={{ marginLeft: '1rem' }}
                >
                  Delete
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}

export default Portfolio;
