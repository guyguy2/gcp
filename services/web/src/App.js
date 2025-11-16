import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import './App.css';
import Portfolio from './components/Portfolio';
import SnippetManager from './components/SnippetManager';
import Home from './components/Home';

function App() {
  return (
    <Router>
      <div className="App">
        <nav className="navbar">
          <h1>Developer Hub</h1>
          <ul>
            <li><Link to="/">Home</Link></li>
            <li><Link to="/portfolio">Portfolio</Link></li>
            <li><Link to="/snippets">Code Snippets</Link></li>
          </ul>
        </nav>

        <main className="content">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/portfolio" element={<Portfolio />} />
            <Route path="/snippets" element={<SnippetManager />} />
          </Routes>
        </main>

        <footer>
          <p>Developer Hub Platform - Built with React, Spring Boot, and GCP</p>
        </footer>
      </div>
    </Router>
  );
}

export default App;
