import { Route, Routes } from 'react-router-dom'
import Layout from './components/layout/Layout'
import ProtectedRoute from './components/auth/ProtectedRoute'
import Home from './pages/Home'
import Login from './pages/Login'
import Register from './pages/Register'
import ComingSoon from './pages/ComingSoon'
import NotFound from './pages/NotFound'
import ArtistHome from './pages/artist/ArtistHome'
import ArtistApplications from './pages/admin/ArtistApplications'

const App = () => (
  <Routes>
    <Route element={<Layout />}>
      <Route index element={<Home />} />
      <Route path="login" element={<Login />} />
      <Route path="register" element={<Register />} />

      <Route element={<ProtectedRoute />}>
        <Route path="cart" element={<ComingSoon title="Your cart" />} />
      </Route>
      <Route element={<ProtectedRoute roles={['ARTIST']} />}>
        <Route path="artist" element={<ArtistHome />} />
      </Route>
      <Route element={<ProtectedRoute roles={['ADMIN']} />}>
        <Route path="admin/artists" element={<ArtistApplications />} />
      </Route>

      <Route path="*" element={<NotFound />} />
    </Route>
  </Routes>
)

export default App
