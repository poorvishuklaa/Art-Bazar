import { Route, Routes } from 'react-router-dom'
import Layout from './components/layout/Layout'
import ProtectedRoute from './components/auth/ProtectedRoute'
import Home from './pages/Home'
import Browse from './pages/Browse'
import ArtworkDetail from './pages/ArtworkDetail'
import ArtistProfile from './pages/ArtistProfile'
import Login from './pages/Login'
import Register from './pages/Register'
import ComingSoon from './pages/ComingSoon'
import NotFound from './pages/NotFound'
import ArtistDashboard from './pages/artist/ArtistDashboard'
import ArtworkEditor from './pages/artist/ArtworkEditor'
import ArtistApplications from './pages/admin/ArtistApplications'

const App = () => (
  <Routes>
    <Route element={<Layout />}>
      <Route index element={<Home />} />
      <Route path="artworks" element={<Browse />} />
      <Route path="artworks/:id" element={<ArtworkDetail />} />
      <Route path="artists/:id" element={<ArtistProfile />} />
      <Route path="login" element={<Login />} />
      <Route path="register" element={<Register />} />

      <Route element={<ProtectedRoute />}>
        <Route path="cart" element={<ComingSoon title="Your cart" />} />
      </Route>
      <Route element={<ProtectedRoute roles={['ARTIST']} />}>
        <Route path="artist" element={<ArtistDashboard />} />
        <Route path="artist/artworks/new" element={<ArtworkEditor />} />
        <Route path="artist/artworks/:id/edit" element={<ArtworkEditor />} />
      </Route>
      <Route element={<ProtectedRoute roles={['ADMIN']} />}>
        <Route path="admin/artists" element={<ArtistApplications />} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Route>
  </Routes>
)

export default App
