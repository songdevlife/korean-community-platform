import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import SearchResultsPage from './pages/SearchResultsPage';
import BusinessDetailPage from './pages/BusinessDetailPage';
import DirectoryPage from './pages/DirectoryPage';
import AustraliaUpdatesPage from './pages/AustraliaUpdatesPage';
import AustraliaUpdateDetailPage from './pages/AustraliaUpdateDetailPage';
import AdminPage from './pages/AdminPage';
import DashboardPage from './pages/DashboardPage';
import FavouritesPage from './pages/FavouritesPage';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/search" element={<SearchResultsPage />} />
          <Route path="/businesses/:slug" element={<BusinessDetailPage />} />
          <Route path="/directory" element={<DirectoryPage />} />
          <Route path="/australia-updates" element={<AustraliaUpdatesPage />} />
          <Route path="/australia-updates/:updateId" element={<AustraliaUpdateDetailPage />} />
          <Route path="/admin" element={<AdminPage />} />
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/favourites" element={<FavouritesPage />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;