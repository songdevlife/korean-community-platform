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
import GuidesPage from './pages/GuidesPage';
import GuideDetailPage from './pages/GuideDetailPage';
import GuideCreatePage from './pages/GuideCreatePage';
import BusinessCreatePage from './pages/BusinessCreatePage';

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
          <Route path="/businesses/new" element={<BusinessCreatePage />} />
          <Route path="/guides" element={<GuidesPage />} />
          <Route path="/guides/:slug" element={<GuideDetailPage />} />
          <Route path="/admin/guides/new" element={<GuideCreatePage />} />
          
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;